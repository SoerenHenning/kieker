/***************************************************************************
 * Copyright 2012 by
 *  + Christian-Albrechts-University of Kiel
 *    + Department of Computer Science
 *      + Software Engineering Group 
 *  and others.
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

package kieker.analysis;

import kieker.common.logging.Log;
import kieker.common.logging.LogFactory;

/**
 * Allows spawn the execution of an {@link AnalysisController} into a separate {@link Thread}. The thread with the {@link AnalysisController} instance
 * provided in the constructor {@link #AnalysisControllerThread(AnalysisController)} is started by calling
 * the {@link #start()} method. The analysis can be terminated by calling the {@link #terminate()} method which delegates the call to the
 * {@link kieker.analysis.AnalysisController#terminate()} method.
 * 
 * @author Andre van Hoorn, Jan Waller
 */
public final class AnalysisControllerThread extends Thread {
	private static final Log LOG = LogFactory.getLog(AnalysisControllerThread.class);

	private final AnalysisController analysisController;

	public AnalysisControllerThread(final AnalysisController analysisController) {
		super();
		this.analysisController = analysisController;
	}

	@Override
	public void start() {
		synchronized (this) {
			super.start();
			this.analysisController.awaitInitialization();
		}
	}

	@Override
	public void run() {
		try {
			this.analysisController.run();
		} catch (final Exception ex) { // NOPMD NOCS (Exception)
			LOG.error("Error running AnalysisCOntroller.", ex);
		}
	}

	/**
	 * Initiates a termination of the executed {@link AnalysisController}.
	 */
	public void terminate() {
		synchronized (this) {
			this.analysisController.terminate();
		}
	}
}
