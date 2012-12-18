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

package kieker.analysis.plugin.filter.forward;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.OutputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.annotation.Property;
import kieker.analysis.plugin.filter.AbstractFilterPlugin;
import kieker.common.configuration.Configuration;
import kieker.common.logging.Log;
import kieker.common.logging.LogFactory;

/**
 * This filter has exactly one input port and one output port.
 * 
 * A simple message is printed to a configurable stream and all objects are forwarded to the output port.
 * 
 * @author Matthias Rohr, Jan Waller
 */
@Plugin(description = "A filter to print the object to a configured stream",
		outputPorts = @OutputPort(name = TeeFilter.OUTPUT_PORT_NAME_RELAYED_EVENTS, description = "Provides each incoming object", eventTypes = { Object.class }),
		configuration = {
			@Property(name = TeeFilter.CONFIG_PROPERTY_NAME_STREAM, defaultValue = TeeFilter.CONFIG_PROPERTY_VALUE_STREAM_STDOUT,
					description = "The name of the stream used to print the incoming data (special values are STDOUT, STDERR, STDLOG, and NULL; other values are interpreted as filenames)."),
			@Property(name = TeeFilter.CONFIG_PROPERTY_NAME_ENCODING, defaultValue = TeeFilter.CONFIG_PROPERTY_VALUE_DEFAULT_ENCODING,
					description = "The used encoding for the selected stream.")
		})
public final class TeeFilter extends AbstractFilterPlugin {

	public static final String INPUT_PORT_NAME_EVENTS = "receivedEvents";

	public static final String OUTPUT_PORT_NAME_RELAYED_EVENTS = "relayedEvents";

	public static final String CONFIG_PROPERTY_NAME_STREAM = "stream";
	public static final String CONFIG_PROPERTY_NAME_ENCODING = "characterEncoding";

	public static final String CONFIG_PROPERTY_VALUE_STREAM_STDOUT = "STDOUT";
	public static final String CONFIG_PROPERTY_VALUE_STREAM_STDERR = "STDERR";
	public static final String CONFIG_PROPERTY_VALUE_STREAM_STDLOG = "STDLOG";
	public static final String CONFIG_PROPERTY_VALUE_STREAM_NULL = "NULL";
	public static final String CONFIG_PROPERTY_VALUE_DEFAULT_ENCODING = "UTF-8";

	private static final Log LOG = LogFactory.getLog(TeeFilter.class);

	private final PrintStream printStream;
	private final String printStreamName;
	private final boolean active;
	private final String encoding;

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
	public TeeFilter(final Configuration configuration, final IProjectContext projectContext) {
		super(configuration, projectContext);

		// Get the name of the stream.
		final String printStreamNameConfig = this.configuration.getStringProperty(CONFIG_PROPERTY_NAME_STREAM);
		// Get the encoding.
		this.encoding = this.configuration.getStringProperty(CONFIG_PROPERTY_NAME_ENCODING);

		// Decide which stream to be used - but remember the name!
		if (CONFIG_PROPERTY_VALUE_STREAM_STDLOG.equals(printStreamNameConfig)) {
			this.printStream = null; // NOPMD (null)
			this.printStreamName = null; // NOPMD (null)
			this.active = true;
		} else if (CONFIG_PROPERTY_VALUE_STREAM_STDOUT.equals(printStreamNameConfig)) {
			this.printStream = System.out;
			this.printStreamName = null; // NOPMD (null)
			this.active = true;
		} else if (CONFIG_PROPERTY_VALUE_STREAM_STDERR.equals(printStreamNameConfig)) {
			this.printStream = System.err;
			this.printStreamName = null; // NOPMD (null)
			this.active = true;
		} else if (CONFIG_PROPERTY_VALUE_STREAM_NULL.equals(printStreamNameConfig)) {
			this.printStream = null; // NOPMD (null)
			this.printStreamName = null; // NOPMD (null)
			this.active = false;
		} else {
			PrintStream tmpPrintStream;
			try {
				tmpPrintStream = new PrintStream(new FileOutputStream(printStreamNameConfig), false, this.encoding);
			} catch (final UnsupportedEncodingException ex) {
				LOG.error("Failed to initialize " + printStreamNameConfig, ex);
				tmpPrintStream = null; // NOPMD (null)
			} catch (final FileNotFoundException ex) {
				LOG.error("Failed to initialize " + printStreamNameConfig, ex);
				tmpPrintStream = null; // NOPMD (null)
			}
			this.printStream = tmpPrintStream;
			this.printStreamName = printStreamNameConfig;
			this.active = true;
		}
	}

	/**
	 * Creates a new instance of this class using the given parameters.
	 * 
	 * @param configuration
	 *            The configuration for this component.
	 * 
	 * @deprecated
	 */
	@Deprecated
	public TeeFilter(final Configuration configuration) {
		this(configuration, null);
	}

	@Override
	public final void terminate(final boolean error) {
		if ((this.printStream != null) && (this.printStream != System.out) && (this.printStream != System.err)) {
			this.printStream.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see kieker.analysis.plugin.IPlugin#getCurrentConfiguration()
	 */
	public final Configuration getCurrentConfiguration() {
		final Configuration configuration = new Configuration();
		configuration.setProperty(CONFIG_PROPERTY_NAME_ENCODING, this.encoding);
		/* We reverse the if-decisions within the constructor. */
		if (this.printStream == null) {
			if (this.active) {
				configuration.setProperty(CONFIG_PROPERTY_NAME_STREAM, CONFIG_PROPERTY_VALUE_STREAM_STDLOG);
			} else {
				configuration.setProperty(CONFIG_PROPERTY_NAME_STREAM, CONFIG_PROPERTY_VALUE_STREAM_NULL);
			}
		} else if (this.printStream == System.out) {
			configuration.setProperty(CONFIG_PROPERTY_NAME_STREAM, CONFIG_PROPERTY_VALUE_STREAM_STDOUT);
		} else if (this.printStream == System.err) {
			configuration.setProperty(CONFIG_PROPERTY_NAME_STREAM, CONFIG_PROPERTY_VALUE_STREAM_STDERR);
		} else {
			configuration.setProperty(CONFIG_PROPERTY_NAME_STREAM, this.printStreamName);
		}
		return configuration;
	}

	@InputPort(name = INPUT_PORT_NAME_EVENTS, description = "Receives incoming objects to be logged and forwarded", eventTypes = { Object.class })
	public final void inputEvent(final Object object) {
		if (this.active) {
			final StringBuilder sb = new StringBuilder(128);
			sb.append(this.getName());
			sb.append('(').append(object.getClass().getSimpleName()).append(") ").append(object.toString());
			final String record = sb.toString();
			if (this.printStream != null) {
				this.printStream.println(record);
			} else {
				LOG.info(record);
			}
		}
		super.deliver(OUTPUT_PORT_NAME_RELAYED_EVENTS, object);
	}
}
