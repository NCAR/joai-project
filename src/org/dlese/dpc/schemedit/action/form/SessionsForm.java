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
package org.dlese.dpc.schemedit.action.form;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.util.*;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;
import java.text.*;

/**
 *  This class uses the getter methods of the ProviderBean and then adds setter methods
 *  for editable fields.
 *
 * @author    Jonathan Ostwald
 */
public final class SessionsForm extends ActionForm implements Serializable {

	private Map lockedRecords = null;
	private List sessionBeans = null;
	private boolean showAnonymousSessions = false;

	public SessionsForm () {}
	
	public List getSessionBeans () {
		return sessionBeans;
	}
	
	public void setSessionBeans (List sessionBeans) {
		this.sessionBeans = sessionBeans;
	}
	
	public void setLockedRecords (Map lockedRecords) {
		this.lockedRecords = lockedRecords;
	}
	
	public Map getLockedRecords () {
		return lockedRecords;
	}
	
	public void setShowAnonymousSessions (boolean show) {
		showAnonymousSessions = show;
	}
	
	public boolean getShowAnonymousSessions () {
		return showAnonymousSessions;
	}
}


