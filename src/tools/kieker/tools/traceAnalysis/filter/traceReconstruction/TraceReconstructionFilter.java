/***************************************************************************
 * Copyright 2012 Kieker Project (http://kieker-monitoring.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package kieker.tools.traceAnalysis.filter.traceReconstruction;

import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.OutputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.annotation.Property;
import kieker.analysis.plugin.annotation.RepositoryPort;
import kieker.common.configuration.Configuration;
import kieker.common.logging.Log;
import kieker.common.logging.LogFactory;
import kieker.tools.traceAnalysis.filter.AbstractTraceAnalysisFilter;
import kieker.tools.traceAnalysis.filter.AbstractTraceProcessingFilter;
import kieker.tools.traceAnalysis.filter.executionRecordTransformation.ExecutionEventProcessingException;
import kieker.tools.traceAnalysis.systemModel.Execution;
import kieker.tools.traceAnalysis.systemModel.ExecutionTrace;
import kieker.tools.traceAnalysis.systemModel.InvalidExecutionTrace;
import kieker.tools.traceAnalysis.systemModel.MessageTrace;
import kieker.tools.traceAnalysis.systemModel.repository.SystemModelRepository;
import kieker.tools.util.LoggingTimestampConverter;

/**
 * @author Andre van Hoorn
 */
@Plugin(description = "Uses the incoming data to enrich the connected repository with the reconstructed traces",
		outputPorts = {
			@OutputPort(name = TraceReconstructionFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE, description = "Reconstructed Message Traces", eventTypes = { MessageTrace.class }),
			@OutputPort(name = TraceReconstructionFilter.OUTPUT_PORT_NAME_EXECUTION_TRACE, description = "Reconstructed Execution Traces", eventTypes = { ExecutionTrace.class }),
			@OutputPort(name = TraceReconstructionFilter.OUTPUT_PORT_NAME_INVALID_EXECUTION_TRACE, description = "Invalid Execution Traces", eventTypes = { InvalidExecutionTrace.class })
		},
		repositoryPorts = {
			@RepositoryPort(name = AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, repositoryType = SystemModelRepository.class)
		},
		configuration = {
			@Property(name = TraceReconstructionFilter.CONFIG_PROPERTY_NAME_MAX_TRACE_DURATION_MILLIS, defaultValue = "2147483647"), // Integer.toString(Integer.MAX_VALUE)
			@Property(name = TraceReconstructionFilter.CONFIG_PROPERTY_NAME_IGNORE_INVALID_TRACES, defaultValue = "true")
		})
public class TraceReconstructionFilter extends AbstractTraceProcessingFilter {

	public static final String INPUT_PORT_NAME_EXECUTIONS = "executions";

	public static final String OUTPUT_PORT_NAME_MESSAGE_TRACE = "messageTraces";
	public static final String OUTPUT_PORT_NAME_EXECUTION_TRACE = "executionTraces";
	public static final String OUTPUT_PORT_NAME_INVALID_EXECUTION_TRACE = "invalidExecutionTraces";

	public static final String CONFIG_PROPERTY_NAME_MAX_TRACE_DURATION_MILLIS = "maxTraceDurationMillis";
	public static final String CONFIG_PROPERTY_NAME_IGNORE_INVALID_TRACES = "ignoreInvalidTraces";

	private static final long CONFIG_PROPERTY_VALUE_MAX_DURATION_NANOS = Long.MAX_VALUE;

	private static final Log LOG = LogFactory.getLog(TraceReconstructionFilter.class);

	/** TraceId x trace. */
	private final Map<Long, ExecutionTrace> pendingTraces = new Hashtable<Long, ExecutionTrace>(); // NOPMD (UseConcurrentHashMap)
	/** We need to keep track of invalid trace's IDs. */
	private final Set<Long> invalidTraces = new TreeSet<Long>();
	private volatile long minTin = -1;
	private volatile long maxTout = -1;
	private volatile boolean terminated;
	private final boolean ignoreInvalidTraces; // false
	private final long maxTraceDurationNanos;
	private final long maxTraceDurationMillis;

	private boolean traceProcessingErrorOccured; // false

	/** Pending traces sorted by tin timestamps. */
	private final SortedSet<ExecutionTrace> timeoutMap = new TreeSet<ExecutionTrace>(new Comparator<ExecutionTrace>() {

		/** Order traces by tins */

		public int compare(final ExecutionTrace t1, final ExecutionTrace t2) {
			if (t1 == t2) { // NOPMD (no equals)
				return 0;
			}
			final long t1LowestTin = t1.getTraceAsSortedExecutionSet().first().getTin();
			final long t2LowestTin = t2.getTraceAsSortedExecutionSet().first().getTin();

			/**
			 * Multiple traces may have an equal tin timestamp value.
			 * In order to provide an absolute ordering of the keys,
			 * we take the traceId as a second ordering key.
			 */
			if (t1LowestTin != t2LowestTin) {
				return t1LowestTin < t2LowestTin ? -1 : 1; // NOCS
			}
			return t1.getTraceId() < t2.getTraceId() ? -1 : 1; // NOCS
		}
	});

	/**
	 * Creates a new instance of this class using the given parameters.
	 * 
	 * @param configuration
	 *            The configuration for this component.
	 * @param projectContext
	 *            The project context for this component.
	 * 
	 * @since 1.7
	 */
	public TraceReconstructionFilter(final Configuration configuration, final IProjectContext projectContext) {
		super(configuration, projectContext);

		/* Load from the configuration. */
		this.maxTraceDurationMillis = configuration.getLongProperty(CONFIG_PROPERTY_NAME_MAX_TRACE_DURATION_MILLIS);
		this.ignoreInvalidTraces = configuration.getBooleanProperty(CONFIG_PROPERTY_NAME_IGNORE_INVALID_TRACES);

		if (this.maxTraceDurationMillis < 0) {
			throw new IllegalArgumentException("value maxTraceDurationMillis must not be negative (found: " + this.maxTraceDurationMillis + ")");
		}
		if (this.maxTraceDurationMillis == AbstractTraceProcessingFilter.MAX_DURATION_MILLIS) {
			this.maxTraceDurationNanos = CONFIG_PROPERTY_VALUE_MAX_DURATION_NANOS;
		} else {
			this.maxTraceDurationNanos = this.maxTraceDurationMillis * (1000 * 1000);
		}
	}

	/**
	 * Creates a new instance of this class using the given parameters. Keep in mind that the Trace-Equivalence-Class-Mode has to be set via the method
	 * <i>setTraceEquivalenceCallMode</i> before using this component!
	 * 
	 * @param configuration
	 *            The configuration for this component.
	 * 
	 * @deprecated To be removed in Kieker 1.8.
	 */
	@Deprecated
	public TraceReconstructionFilter(final Configuration configuration) {
		this(configuration, null);
	}

	/**
	 * Returns a set of the IDs of invalid traces.
	 * 
	 * @return a set of the IDs of invalid traces
	 */
	public Set<Long> getInvalidTraces() {
		return this.invalidTraces;
	}

	/**
	 * Returns the minimum tin timestamp of a processed execution.
	 * 
	 * @return the minimum tin timestamp of a processed execution
	 */
	public final long getMinTin() {
		return this.minTin;
	}

	/**
	 * Returns the maximum tout timestamp of a processed execution.
	 * 
	 * @return the maximum tout timestamp of a processed execution
	 */
	public final long getMaxTout() {
		return this.maxTout;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean init() {
		return true; // no need to do anything here
	}

	@InputPort(
			name = INPUT_PORT_NAME_EXECUTIONS,
			description = "Receives the executions to be processed",
			eventTypes = { Execution.class })
	public void inputExecutions(final Execution execution) {
		synchronized (this) {
			if (this.terminated || (this.traceProcessingErrorOccured && !this.ignoreInvalidTraces)) {
				return;
			}

			final long traceId = execution.getTraceId();

			this.minTin = ((this.minTin < 0) || (execution.getTin() < this.minTin)) ? execution.getTin() : this.minTin; // NOCS
			this.maxTout = execution.getTout() > this.maxTout ? execution.getTout() : this.maxTout; // NOCS

			ExecutionTrace executionTrace = this.pendingTraces.get(traceId);
			if (executionTrace != null) { /* trace (artifacts) exists already; */
				if (!this.timeoutMap.remove(executionTrace)) { /* remove from timeoutMap. Will be re-added below */
					LOG.error("Missing entry for trace in timeoutMap: " + executionTrace
							+ " PendingTraces and timeoutMap are now longer consistent!");
					this.reportError(traceId);
				}
			} else { /* create and add new trace */
				executionTrace = new ExecutionTrace(traceId, execution.getSessionId());
				this.pendingTraces.put(traceId, executionTrace);
			}
			try {
				executionTrace.add(execution);
				if (!this.timeoutMap.add(executionTrace)) { // (re-)add trace to timeoutMap
					LOG.error("Equal entry existed in timeoutMap already:" + executionTrace);
				}
				this.processTimeoutQueue();
			} catch (final InvalidTraceException ex) { // this would be a bug!
				LOG.error("Attempt to add record to wrong trace", ex);
			} catch (final ExecutionEventProcessingException ex) {
				LOG.error("ExecutionEventProcessingException occured while processing the timeout queue.", ex);
			}
		}
	}

	/**
	 * Transforms the execution trace is delivers the trace to the output ports
	 * of this filter (message trace and execution trace output ports, or invalid
	 * execution trace output port respectively).
	 * 
	 * @param executionTrace
	 * @throws ExecutionEventProcessingException
	 *             if the passed execution trace is
	 *             invalid and this filter is configured to fail on the occurrence of invalid
	 *             traces.
	 */
	private void processExecutionTrace(final ExecutionTrace executionTrace) throws ExecutionEventProcessingException {
		final long curTraceId = executionTrace.getTraceId();
		try {
			/*
			 * If the polled trace is invalid, the following method
			 * toMessageTrace(..) throws an exception
			 */
			final MessageTrace mt = executionTrace.toMessageTrace(SystemModelRepository.ROOT_EXECUTION);

			/*
			 * Transformation successful and the trace is for itself valid.
			 * However, this trace may actually contain the [0,0] execution
			 * and thus complete a trace that has timed out before and has
			 * thus been considered an invalid trace.
			 */
			if (!this.invalidTraces.contains(mt.getTraceId())) {
				/* Not completing part of an invalid trace */
				super.deliver(OUTPUT_PORT_NAME_MESSAGE_TRACE, mt);
				super.deliver(OUTPUT_PORT_NAME_EXECUTION_TRACE, executionTrace);
				this.reportSuccess(curTraceId);
			} else {
				/* mt is the completing part of an invalid trace */
				super.deliver(OUTPUT_PORT_NAME_INVALID_EXECUTION_TRACE, new InvalidExecutionTrace(executionTrace));
				// the statistics have been updated on the first
				// occurrence of artifacts of this trace
			}
		} catch (final InvalidTraceException ex) {
			/* Transformation failed (i.e., trace invalid) */
			super.deliver(OUTPUT_PORT_NAME_INVALID_EXECUTION_TRACE, new InvalidExecutionTrace(executionTrace));
			final String transformationError = "Failed to transform execution trace to message trace (ID: " + curTraceId + "). \n"
					+ "Reason: " + ex.getMessage() + "\n Trace: " + executionTrace;
			if (!this.invalidTraces.contains(curTraceId)) {
				// only once per traceID (otherwise, we would report all
				// trace fragments)
				this.reportError(curTraceId);
				this.invalidTraces.add(curTraceId);
				if (!this.ignoreInvalidTraces) {
					this.traceProcessingErrorOccured = true;
					LOG.warn("Note that this filter was configured to terminate at the *first* occurence of an invalid trace \n"
							+ "If this is not the desired behavior, set the configuration property "
							+ CONFIG_PROPERTY_NAME_IGNORE_INVALID_TRACES + " to 'true'");
					throw new ExecutionEventProcessingException(transformationError, ex);
				} else {
					LOG.error(transformationError); // do not pass 'ex' to LOG.error because this makes the output verbose (#584)
				}
			} else {
				LOG.warn("Found additional fragment for trace already marked invalid: " + transformationError);
			}
		}
	}

	/**
	 * Processes the pending traces in the timeout queue: Either those,
	 * that timed out are all, if the filter was requested to terminate.
	 * 
	 * @throws ExecutionEventProcessingException
	 */
	private void processTimeoutQueue() throws ExecutionEventProcessingException {
		synchronized (this.timeoutMap) {
			while (!this.timeoutMap.isEmpty() && (this.terminated || ((this.maxTout - this.timeoutMap.first().getMinTin()) > this.maxTraceDurationNanos))) {
				// Java 1.5 compatibility
				final ExecutionTrace polledTrace = this.timeoutMap.first();
				this.timeoutMap.remove(polledTrace);
				// Java 1.6: final ExecutionTrace polledTrace = this.timeoutMap.pollFirst();
				final long curTraceId = polledTrace.getTraceId();
				this.pendingTraces.remove(curTraceId);
				this.processExecutionTrace(polledTrace);
			}
		}
	}

	/**
	 * Return the number of nanoseconds after which a pending trace isconsidered to have timed out.
	 * 
	 * @return the timeout duration for a pending trace in nanoseconds
	 */
	public final long getMaxTraceDurationNanos() {
		synchronized (this) {
			return this.maxTraceDurationNanos;
		}
	}

	/**
	 * Terminates the filter (internally, all pending traces are processed).
	 * 
	 * @param error
	 *            Determines whether the plugin is terminated due to an error or not.
	 */
	@Override
	public void terminate(final boolean error) {
		synchronized (this) {
			try {
				this.terminated = true;
				if (!error || (this.traceProcessingErrorOccured && !this.ignoreInvalidTraces)) {
					this.processTimeoutQueue();
				} else {
					LOG
							.info("terminate called with error an flag set or a trace processing occurred; won't process timeoutqueue any more.");
				}
			} catch (final ExecutionEventProcessingException ex) {
				this.traceProcessingErrorOccured = true;
				LOG.error("Error processing timeout queue", ex);
			}
		}
	}

	@Override
	public void printStatusMessage() {
		synchronized (this) {
			super.printStatusMessage();
			if ((this.getSuccessCount() > 0) || (this.getErrorCount() > 0)) {
				final String minTinStr = new StringBuilder().append(this.minTin).append(" (")
						.append(LoggingTimestampConverter.convertLoggingTimestampToUTCString(this.minTin)).append(",")
						.append(LoggingTimestampConverter.convertLoggingTimestampLocalTimeZoneString(this.minTin)).append(")").toString();
				final String maxToutStr = new StringBuilder().append(this.maxTout).append(" (")
						.append(LoggingTimestampConverter.convertLoggingTimestampToUTCString(this.maxTout)).append(",")
						.append(LoggingTimestampConverter.convertLoggingTimestampLocalTimeZoneString(this.maxTout)).append(")").toString();
				this.stdOutPrintln("First timestamp: " + minTinStr);
				this.stdOutPrintln("Last timestamp: " + maxToutStr);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Configuration getCurrentConfiguration() {
		final Configuration configuration = new Configuration();

		configuration.setProperty(CONFIG_PROPERTY_NAME_MAX_TRACE_DURATION_MILLIS, Long.toString(this.maxTraceDurationMillis));
		configuration.setProperty(CONFIG_PROPERTY_NAME_IGNORE_INVALID_TRACES, Boolean.toString(this.ignoreInvalidTraces));

		return configuration;
	}

}
