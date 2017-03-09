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
package org.dlese.dpc.suggest.action.form;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.LabelValueBean;

import javax.servlet.http.HttpServletRequest;
import java.util.*;


/**
 *  ActionForm bean for handling requests that access a {@link org.dlese.dpc.suggest.SuggestionRecord}
 *  the QA DataBase and Records.
 *
 * @author     ostwald
 * @version    $Id: SuggestCommentForm.java,v 1.3 2009/03/20 23:34:00 jweather Exp $
 */
public class SuggestCommentForm extends SuggestForm {

	private static boolean debug = true;

	private String itemID = null;
	private String itemURL = null;
	private String itemTitle = null;

	private String role = null;
	private String[] roleValues = new String[]{};

	// suggestor info
	private String description = null;
	private String[] roles = new String[0];
	private String share = null;
	private String nameFirst = null;
	private String nameLast = null;
	private String email = null;
	private String instName = null;

	private String creationDate = null;


	/**  Constructor */
	public SuggestCommentForm() { }


	/**
	 *  Necessary to set role to a default value in the case in which no selection
	 *  is made for roles.
	 *
	 * @param  mapping  Description of the Parameter
	 * @param  request  Description of the Parameter
	 */
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		super.reset(mapping, request);
	}


	/**  resets the bean's key attributes */
	public void clear() {
		super.clear();

		itemID = null;
		itemTitle = null;
		itemURL = null;

		role = null;
		share = null;
		description = null;
		nameFirst = null;
		nameLast = null;
		email = null;
		instName = null;
		creationDate = null;
	}


	/**
	 *  Gets the itemID attribute of the SuggestCommentForm object
	 *
	 * @return    The itemID value
	 */
	public String getItemID() {
		return itemID;
	}


	/**
	 *  Sets the itemID attribute of the SuggestCommentForm object
	 *
	 * @param  val  The new itemID value
	 */
	public void setItemID(String val) {
		this.itemID = val;
	}


	/**
	 *  Gets the itemURL attribute of the SuggestCommentForm object
	 *
	 * @return    The itemURL value
	 */
	public String getItemURL() {
		return itemURL;
	}


	/**
	 *  Sets the itemURL attribute of the SuggestCommentForm object
	 *
	 * @param  val  The new itemURL value
	 */
	public void setItemURL(String val) {
		this.itemURL = val;
	}


	/**
	 *  Gets the itemTitle attribute of the SuggestCommentForm object
	 *
	 * @return    The itemTitle value
	 */
	public String getItemTitle() {
		return (itemTitle != null ? itemTitle : "");
	}


	/**
	 *  Sets the itemTitle attribute of the SuggestCommentForm object
	 *
	 * @param  val  The new itemTitle value
	 */
	public void setItemTitle(String val) {
		this.itemTitle = val;
	}


	/**
	 *  Gets the description attribute of the SuggestCommentForm object
	 *
	 * @return    The description value
	 */
	public String getDescription() {
		return (description != null ? description : "");
	}


	/**
	 *  Sets the description attribute of the SuggestCommentForm object
	 *
	 * @param  val  The new description value
	 */
	public void setDescription(String val) {
		this.description = val;
	}


	/**
	 *  Gets the role attribute of the SuggestCommentForm object
	 *
	 * @return    The role value
	 */
	public String getRole() {
		return role;
	}


	/**
	 *  Sets the role attribute of the SuggestCommentForm object
	 *
	 * @param  role  The new role value
	 */
	public void setRole(String role) {
		this.role = role;
	}


	/**
	 *  Gets the roleValues attribute of the SuggestCommentForm object
	 *
	 * @return    The roleValues value
	 */
	public String[] getRoleValues() {
		try {
			if (roleValues.length == 0) {
				String typeName = "roleAnnotationType";
				List valueList = schemaHelper.getEnumerationValues(typeName, false);
				if (valueList == null) {
					// prtln ("valueList was null for " + typeName + " values");
					// prtln ("schemaHelper: " + schemaHelper.getSchemaLocation());
				}
				else {
					roleValues = (String[]) valueList.toArray(new String[]{});
				}
			}
		} catch (Throwable t) {
			prtln(t.getMessage());
			t.printStackTrace();
		}
		return roleValues;
	}


	/**
	 *  Gets the share attribute of the SuggestCommentForm object
	 *
	 * @return    The share value
	 */
	public String getShare() {
		return this.share;
	}


	/**
	 *  Sets the share attribute of the SuggestCommentForm object
	 *
	 * @param  share  The new share value
	 */
	public void setShare(String share) {
		// this.share = ("true".equals(share) ? share : "false");
		this.share = share;
	}

	// ------- information about the suggestor -------------

	/**
	 *  Gets the nameFirst attribute of the SuggestCommentForm object
	 *
	 * @return    The nameFirst value
	 */
	public String getNameFirst() {
		return (nameFirst != null ? nameFirst : "");
	}


	/**
	 *  Sets the nameFirst attribute of the SuggestCommentForm object
	 *
	 * @param  val  The new nameFirst value
	 */
	public void setNameFirst(String val) {
		this.nameFirst = val;
	}


	/**
	 *  Gets the nameLast attribute of the SuggestCommentForm object
	 *
	 * @return    The nameLast value
	 */
	public String getNameLast() {
		return (nameLast != null ? nameLast : "");
	}


	/**
	 *  Sets the nameLast attribute of the SuggestCommentForm object
	 *
	 * @param  val  The new nameLast value
	 */
	public void setNameLast(String val) {
		this.nameLast = val;
	}


	/**
	 *  Gets the email attribute of the SuggestCommentForm object
	 *
	 * @return    The email value
	 */
	public String getEmail() {
		return (email != null ? email : "");
	}


	/**
	 *  Sets the email attribute of the SuggestCommentForm object
	 *
	 * @param  val  The new email value
	 */
	public void setEmail(String val) {
		this.email = val;
	}


	/**
	 *  Gets the instName attribute of the SuggestCommentForm object
	 *
	 * @return    The instName value
	 */
	public String getInstName() {
		return (instName != null ? instName : "");
	}


	/**
	 *  Sets the instName attribute of the SuggestCommentForm object
	 *
	 * @param  val  The new instName value
	 */
	public void setInstName(String val) {
		this.instName = val;
	}


	/**
	 *  Print selected fields of this object for debugging purposes
	 *
	 * @return    String
	 */
	public String toString() {
		String s = "SuggestCommentForm contents:";
		if (email != null) {
			s += "\n\temail: " + email;
		}
		return s;
	}


	/**
	 *  Sets the debug attribute of the SuggestCommentForm object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}

}

