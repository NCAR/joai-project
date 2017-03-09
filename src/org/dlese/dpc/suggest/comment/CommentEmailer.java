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
package org.dlese.dpc.suggest.comment;

import org.dlese.dpc.suggest.Emailer;
import org.dlese.dpc.suggest.SuggestHelper;
import org.dlese.dpc.suggest.action.form.SuggestForm;
import org.dlese.dpc.suggest.action.form.SuggestCommentForm;

import org.dlese.dpc.email.*;

import java.util.*;
import java.text.*;
import java.lang.StringBuffer;

/**
 *  Formats and sends email notifying of a newly suggested resource
 *
 * @author    ostwald $Id: CommentEmailer.java,v 1.2 2007/10/23 15:46:23 ostwald
 *      Exp $
 */
public class CommentEmailer extends Emailer {

	private static boolean debug = true;


	/**
	 *  Constructor for the CommentEmailer object
	 *
	 * @param  recId   id of comment record
	 * @param  helper  SuggestCommentHelper instance
	 */
	public CommentEmailer(String recId, SuggestHelper helper) {
		super(recId, helper);
	}


	/**
	 *  Gets the msgSubject attribute of the CommentEmailer object
	 *
	 * @return    The msgSubject value
	 */
	protected String getMsgSubject() {
		return "Suggested URL (" + recId + ")";
	}


	/**
	 *  Gets the msgBody attribute of the CommentEmailer object
	 *
	 * @param  form  NOT YET DOCUMENTED
	 * @return       The msgBody value
	 */
	protected String getMsgBody(SuggestForm form) {

		SuggestCommentForm scf = (SuggestCommentForm) form;

		String manageLink = helper.getViewBaseUrl() + "/view.do?id=" + recId;
		String msgBody = "";

		msgBody = "A new Comment or Teaching Tip has been suggested:";

		msgBody += "\n\n recordID: " + recId;
		msgBody += "\n\n title: " + "Comment on " + scf.getItemTitle();
		msgBody += "\n\n itemID: " + scf.getItemID();
		msgBody += "\n\n description: " + scf.getDescription();
		msgBody += "\n\n role: " + scf.getRole();
		msgBody += "\n\n share: " + scf.getShare();
		msgBody += "\n\n first name: " + scf.getNameFirst();
		msgBody += "\n\n last name: " + scf.getNameLast();
		msgBody += "\n\n email: " + scf.getEmail();
		msgBody += "\n\n institution: " + scf.getInstName();

		msgBody += "\n\n\n";
		msgBody += "Manage this record at " + manageLink;

		return msgBody;
	}


	/**
	 *  The main program for the CommentEmailer class
	 *
	 * @param  args  The command line arguments
	 */
	public static void main(String[] args) {
		SendEmail send = null;
		String mailServer = "mail.dpc.ucar.edu";
		try {
			send = new SendEmail(MAIL_TYPE, mailServer);
		} catch (Throwable e) {
			prtln("SendEmail failed: " + e);
			return;
		}
		String msgTo = "ostwald@comcast.net";
		String msgFrom = "ostwald@testing.com";
		String msgSubject = "a test message";
		String msgBody = "this is the body of the message";
		boolean sent = send.doSend(msgTo, msgFrom, msgSubject, msgBody);
		if (sent) {
			prtln("message sent SUCCESSFULLY");
		}
		else {
			prtln("trouble sending message");
		}

	}


	/**
	 *  Sets the debug attribute of the CommentEmailer object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}


	/**
	 *  Print the string with trailing newline to std output
	 *
	 * @param  s  string to print
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("CommentEmailer: " + s);
		}
	}
}

