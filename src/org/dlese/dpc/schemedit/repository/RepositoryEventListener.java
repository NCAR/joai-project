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

import org.dlese.dpc.schemedit.SchemEditUtils;
import javax.servlet.ServletContext;
import java.util.*;

/**
 *  A Simple, servletContext-aware, listener for RepositoryEvents that prints event info to the log
 *
 *@author    jonathan
 */
public class RepositoryEventListener implements RepositoryEventListenerInterface {

	private static boolean debug = true;
	private ServletContext servletContext = null;


	/**
	 *  Constructor for the RepositoryEventListener object
	 */
	public RepositoryEventListener() { }


	/**
	 *  Constructor for the RepositoryEventListener object
	 *
	 *@param  servletContext  the servletContext
	 */
	public RepositoryEventListener(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	/**
	 *  Sets the servletContext attribute of the RepositoryEventListener object
	 *
	 *@param  servletContext  The new servletContext value
	 */
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	/**
	 *  Invoked when a repositoryEvent has occured.
	 *
	 *@param  repositoryEvent  Description of the Parameter
	 */
	public void handleEvent(RepositoryEvent repositoryEvent) {
		System.out.println("RepositoryEventListener received a repositoryEvent: " + repositoryEvent.getName());
		System.out.println(repositoryEvent.toString());

	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "RepositoryEventListener");
		}
	}
}

