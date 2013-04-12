/***************************************************************************
 * Copyright 2013 Kieker Project (http://kieker-monitoring.net)
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

package kieker.monitoring.sampler.sigar;

import kieker.monitoring.sampler.sigar.samplers.CPUsCombinedPercSampler;
import kieker.monitoring.sampler.sigar.samplers.CPUsDetailedPercSampler;
import kieker.monitoring.sampler.sigar.samplers.MemSwapUsageSampler;

/**
 * Defines the list of methods to be provided by a factory for {@link org.hyperic.sigar.Sigar}-based
 * {@link kieker.monitoring.sampler.sigar.samplers.AbstractSigarSampler}s.
 * 
 * @author Andre van Hoorn
 * 
 * @since 1.3
 */
public interface ISigarSamplerFactory {

	/**
	 * Creates an instance of {@link MemSwapUsageSampler}.
	 * 
	 * @return the created instance.
	 * 
	 * @since 1.3
	 */
	public MemSwapUsageSampler createSensorMemSwapUsage();

	/**
	 * Creates an instance of {@link CPUsDetailedPercSampler}.
	 * 
	 * @return the created instance.
	 * 
	 * @since 1.3
	 */
	public CPUsDetailedPercSampler createSensorCPUsDetailedPerc();

	/**
	 * Creates an instance of {@link CPUsCombinedPercSampler}.
	 * 
	 * @return the created instance.
	 * 
	 * @since 1.3
	 */
	public CPUsCombinedPercSampler createSensorCPUsCombinedPerc();
}