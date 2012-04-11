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

package kieker.tools.traceAnalysis.systemModel.repository;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import kieker.tools.traceAnalysis.systemModel.AllocationComponent;
import kieker.tools.traceAnalysis.systemModel.AssemblyComponent;
import kieker.tools.traceAnalysis.systemModel.ExecutionContainer;

/**
 * 
 * @author Andre van Hoorn
 */
public class AllocationRepository extends AbstractSystemSubRepository {
	public static final AllocationComponent ROOT_ALLOCATION_COMPONENT =
			new AllocationComponent(AbstractSystemSubRepository.ROOT_ELEMENT_ID, AssemblyRepository.ROOT_ASSEMBLY_COMPONENT,
					ExecutionEnvironmentRepository.ROOT_EXECUTION_CONTAINER);

	private final Map<String, AllocationComponent> allocationComponentInstancesByName = new Hashtable<String, AllocationComponent>(); // NOPMD (UseConcurrentHashMap)
	private final Map<Integer, AllocationComponent> allocationComponentInstancesById = new Hashtable<Integer, AllocationComponent>(); // NOPMD (UseConcurrentHashMap)

	public AllocationRepository(final SystemModelRepository systemFactory) {
		super(systemFactory);
	}

	/**
	 * Returns the instance for the passed factoryIdentifier; null if no instance
	 * with this factoryIdentifier.
	 */
	public final AllocationComponent lookupAllocationComponentInstanceByNamedIdentifier(final String namedIdentifier) {
		return this.allocationComponentInstancesByName.get(namedIdentifier);
	}

	public final AllocationComponent createAndRegisterAllocationComponentInstance(final String namedIdentifier, final AssemblyComponent assemblyComponentInstance,
			final ExecutionContainer executionContainer) {
		AllocationComponent newInst;
		if (this.allocationComponentInstancesByName.containsKey(namedIdentifier)) {
			throw new IllegalArgumentException("Element with name " + namedIdentifier + "exists already");
		}
		final int id = this.getAndIncrementNextId();
		newInst = new AllocationComponent(id, assemblyComponentInstance, executionContainer);
		this.allocationComponentInstancesById.put(id, newInst);
		this.allocationComponentInstancesByName.put(namedIdentifier, newInst);
		return newInst;
	}

	public final Collection<AllocationComponent> getAllocationComponentInstances() {
		return this.allocationComponentInstancesById.values();
	}
}
