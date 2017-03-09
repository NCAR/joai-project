/*
	Copyright 2017 Digital Learning Sciences (DLS) at the
	University Corporation for Atmospheric Research (UCAR),
	P.O. Box 3000, Boulder, CO 80307

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package org.dlese.dpc.schemedit.repository;

import java.util.*;

/**
 *  Event indicating that a repository event occurred, exposes an event name (e.g.,
 *  'recordMoved') as well eventData in the form of a map.
 *
 *@author    ostwald
 */
public class RepositoryEvent extends EventObject {

	private String eventName;


	/**
	 *  Contruct a RepositoryEvent
	 *
	 *@param  eventName  Description of the Parameter
	 *@param  eventData  Description of the Parameter
	 */
	public RepositoryEvent(String eventName, Map eventData) {
		super(eventData);
		this.eventName = eventName;
	}


	/**
	 *  Gets the name attribute of the RepositoryEvent object
	 *
	 *@return    The name value
	 */
	public String getName() {
		return this.eventName;
	}


	/**
	 *  Gets the eventData attribute of the RepositoryEvent object
	 *
	 *@return    The eventData value
	 */
	public Map getEventData() {
		return (Map) this.getSource();
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	public String toString() {
		String NL = "\n\t";
		String s = eventName;
		Map eventData = this.getEventData();
		for (Iterator i = eventData.keySet().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			s += NL + "  " + key + ": " + (String) eventData.get(key);
		}
		return s;
	}

}

