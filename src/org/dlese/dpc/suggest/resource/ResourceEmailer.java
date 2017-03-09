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
package org.dlese.dpc.suggest.resource;

import org.dlese.dpc.suggest.Emailer;
import org.dlese.dpc.suggest.SuggestHelper;
import org.dlese.dpc.suggest.action.form.SuggestForm;
import org.dlese.dpc.suggest.action.form.SuggestResourceForm;

import org.dlese.dpc.suggest.resource.urlcheck.*;
import org.dlese.dpc.schemedit.url.DupSim;

import org.dlese.dpc.email.*;

import java.util.*;
import java.text.*;
import java.lang.StringBuffer;

/**
 *  Formats and sends email notifying of a newly suggested resource
 *
 * @author    ostwald
 */
public class ResourceEmailer extends Emailer {

	private static boolean debug = true;


	/**
	 *  Constructor for the ResourceEmailer object
	 *
	 * @param  recId   id of suggested record
	 * @param  helper  SuggestResourceHelper instance
	 */
	public ResourceEmailer(String recId, SuggestHelper helper) {
		super(recId, helper);
	}


	/**
	 *  creates a comma delimitted display of the grade Range values
	 *
	 * @param  gr  an array of grade Ranges values
	 * @return     comma delimited String
	 */
	private String gradeRangeStr(String[] gr) {
		if (gr == null) {
			return "";
		}
		StringBuffer s = new StringBuffer();
		for (int i = 0; i < gr.length; i++) {
			s.append(gr[i]);
			if (i < gr.length - 1) {
				s.append(", ");
			}
		}
		return s.toString();
	}


	/**
	 *  Gets the msgSubject attribute of the ResourceEmailer object
	 *
	 * @return    The msgSubject value
	 */
	protected String getMsgSubject() {
		return "Suggested URL (" + recId + ")";
	}


	/**
	 *  Gets the msgBody attribute of the ResourceEmailer object
	 *
	 * @param  form  NOT YET DOCUMENTED
	 * @return       The msgBody value
	 */
	protected String getMsgBody(SuggestForm form) {

		SuggestResourceForm srf = (SuggestResourceForm) form;

		String manageLink = helper.getViewBaseUrl() + "/view.do?id=" + recId;
		String msgBody = "";
		if (srf.getDupRecord() != null) {
			msgBody = "An existing record has been commented on";
		}
		else {
			msgBody = "A new URL has been suggested:";
		}
		msgBody += "\n\n id: " + recId;
		msgBody += "\n\n url: " + srf.getUrl();
		msgBody += "\n\n title: " + srf.getTitle();
		msgBody += "\n\n description: " + srf.getDescription();
		msgBody += "\n\n grade level: " + gradeRangeStr(srf.getGradeRanges());
		msgBody += "\n\n first name: " + srf.getNameFirst();
		msgBody += "\n\n last name: " + srf.getNameLast();
		msgBody += "\n\n email: " + srf.getEmailPrimary();
		msgBody += "\n\n institution: " + srf.getInstName();

		msgBody += "\n\n\n";
		msgBody += "Manage this record at " + manageLink;

		ValidatorResults validatorResults = srf.getValidatorResults();
		if (validatorResults.hasSimilarUrls()) {
			msgBody += "\n\n" + similarUrlsReport(validatorResults);
		}
		return msgBody;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  vr  NOT YET DOCUMENTED
	 * @return     NOT YET DOCUMENTED
	 */
	private String similarUrlsReport(ValidatorResults vr) {
		String report = "";
		String repositoryURL = this.helper.getViewBaseUrl();
		if (vr.getSimilarPrimaryUrls().size() > 0) {
			report += "Records having similar Primary Urls";
			for (Iterator i = vr.getSimilarPrimaryUrls().iterator(); i.hasNext(); ) {
				DupSim sim = (DupSim) i.next();
				report += "\n\t" + sim.getId() + ",  " + sim.getUrl();
				report += "\n\t\tview at: " + repositoryURL + "/view.do?id=" + sim.getId();
			}
		}
		if (vr.getSimilarMirrorUrls().size() > 0) {
			report += "\n\nRecords having similar Mirror Urls";
			for (Iterator i = vr.getSimilarMirrorUrls().iterator(); i.hasNext(); ) {
				DupSim sim = (DupSim) i.next();
				report += "\n\t" + sim.getId() + ",  " + sim.getUrl();
				report += "\n\t\tview at: " + repositoryURL + "/view.do?id=" + sim.getId();
			}
		}
		return report;
	}


	/**
	 *  The main program for the ResourceEmailer class
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
	 *  Sets the debug attribute of the ResourceEmailer object
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
			System.out.println("ResourceEmailer: " + s);
		}
	}
}

