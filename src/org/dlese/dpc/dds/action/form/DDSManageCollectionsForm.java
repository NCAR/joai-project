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
package org.dlese.dpc.dds.action.form;

import org.dlese.dpc.index.*;
import org.dlese.dpc.dds.action.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.util.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.repository.*;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.URLEncoder;

/**
 *  A Struts Form bean for managing the items in the DDS collections. This class works in
 *  conjuction with the {@link org.dlese.dpc.dds.action.DDSManageCollectionsAction} Struts
 *  Action class.
 *
 * @author    John Weatherley
 */
public final class DDSManageCollectionsForm extends VocabForm implements Serializable {
	private static boolean debug = true;

	private String statusComment = null;


	/**  Constructor for the DDSManageCollectionsForm object */
	public DDSManageCollectionsForm() { }


	/**
	 *  Gets the statusComment attribute of the DDSManageCollectionsForm object
	 *
	 * @return    The statusComment value
	 */
	public String getStatusComment() {
		return statusComment;
	}


	/**
	 *  Sets the statusComment attribute of the DDSManageCollectionsForm object
	 *
	 * @param  val  The new statusComment value
	 */
	public void setStatusComment(String val) {
		statusComment = val;
	}



	//================================================================

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " " + s);
		}
	}


	/**
	 *  Return a string for the current time and date, sutiable for display in log files and
	 *  output to standout:
	 *
	 * @return    The dateStamp value
	 */
	private final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}


