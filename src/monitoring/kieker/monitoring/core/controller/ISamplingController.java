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

package kieker.monitoring.core.controller;

import java.util.concurrent.TimeUnit;

import kieker.monitoring.core.sampler.ISampler;
import kieker.monitoring.core.sampler.ScheduledSamplerJob;

/**
 * @author Andre van Hoorn, Jan Waller
 */
public interface ISamplingController {

	/**
	 * Schedules the given {@link ISampler} with given initial delay, and
	 * period.
	 * 
	 * @param sampler
	 * @param initialDelay
	 * @param period
	 * @param timeUnit
	 * @return a {@link ScheduledSamplerJob} as a handler for removing the
	 *         scheduled sampler later on by using the method {@link #removeScheduledSampler(ScheduledSamplerJob)}.
	 */
	public abstract ScheduledSamplerJob schedulePeriodicSampler(final ISampler sampler, final long initialDelay, final long period, final TimeUnit timeUnit);

	/**
	 * Stops future executions of the given periodic {@link ScheduledSamplerJob} .
	 * 
	 * @param sampler
	 * @return true if the sensor is not registered
	 */
	public abstract boolean removeScheduledSampler(final ScheduledSamplerJob sampler);

}
