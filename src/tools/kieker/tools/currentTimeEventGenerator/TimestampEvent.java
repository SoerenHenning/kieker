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

package kieker.tools.currentTimeEventGenerator;

/**
 * Record type which can be used to store the current time in the field {@link #timestamp}.
 * 
 * @deprecated will be removed in Kieker 1.6; use {@link kieker.common.record.misc.TimestampRecord} instead.
 * 
 * @author Andre van Hoorn
 * 
 */
@Deprecated
public class TimestampEvent {
	private volatile long timestamp = -1;

	/**
	 * Creates a new {@link TimestampEvent} with the given timestamp.
	 * 
	 * @param currenTime
	 */
	public TimestampEvent(final long currenTime) {
		this.timestamp = currenTime;
	}

	/**
	 * Returns the current time.
	 * 
	 * @return the current time.
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Sets the current time to the given value.
	 * 
	 * @param timestamp
	 */
	public void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}
}
