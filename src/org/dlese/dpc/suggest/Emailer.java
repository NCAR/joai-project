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
package org.dlese.dpc.suggest;

import org.dlese.dpc.suggest.action.form.SuggestForm;
import org.dlese.dpc.suggest.SuggestHelper;

import org.dlese.dpc.email.*;

/**
 *  Formats and sends an email message notifying of a suggestion.
 *
 * @author    ostwald
 */
public abstract class Emailer {

	/**  NOT YET DOCUMENTED */
	protected static boolean debug = true;

	/**  NOT YET DOCUMENTED */
	protected static String MAIL_TYPE = "mail.smtp.host";

	/**  NOT YET DOCUMENTED */
	protected String recId;
	/**  NOT YET DOCUMENTED */
	protected SuggestHelper helper;


	/**
	 *  Constructor for the Emailer object
	 *
	 * @param  recId          id if the suggestion record
	 * @param  helper         SuggestHelper instance
	 */
	public Emailer(String recId, SuggestHelper helper) {
		this.recId = recId;
		this.helper = helper;
	}


	/**
	 *  Gets the subject line of the the notification email message.
	 *
	 * @return    The msgSubject value
	 */
	protected abstract String getMsgSubject();


	/**
	 *  Gets the body of the the notification email message.
	 *
	 * @param  form  NOT YET DOCUMENTED
	 * @return       The msgBody value
	 */
	protected abstract String getMsgBody(SuggestForm form);


	/**
	 *  Construct and send email notification of a suggestion.
	 *
	 * @param  form  The SuggestResourceForm which provides info about the
	 *      suggestion
	 * @return       true if send was successuful
	 */
	public boolean sendNotification(SuggestForm form) {
		SendEmail send = null;
		try {
			send = new SendEmail(MAIL_TYPE, helper.getMailServer());
		} catch (Throwable e) {
			prtln("SendEmail failed: " + e);
			return false;
		}

		String[] msgTo = this.helper.getEmailTo();
		String msgFrom = this.helper.getEmailFrom();
		String msgSubject = this.getMsgSubject();

		String msgBody = this.getMsgBody(form);

		if (debug) {
			String msgToStr = "";
			for (int i = 0; i < msgTo.length; i++) {
				msgToStr += msgTo[i];
				// msgToStr += "(" + i + "," + msgTo.length + ")";
				if (msgTo.length > 1 && i < msgTo.length - 1)
					msgToStr += ", ";
			}

			prtln("The following message would have been sent to " + msgToStr);
			prtln("--------");
			prtln("Subject: " + msgSubject);
			prtln(msgBody);
			prtln("--------");
			return true;
		}
		else
			return send.doSend(msgTo, msgFrom, msgSubject, msgBody);
	}


	/**
	 *  Sets the debug attribute of the Emailer object
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
			System.out.println("Emailer: " + s);
		}
	}
}

