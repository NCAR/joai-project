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
package org.dlese.dpc.email;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

/**
 *  Handles sending emails. See <a href="http://java.sun.com/products/javamail/javadocs/overview-summary.html">
 *  Javadocs</a> for more information.
 *
 * @author    John Weatherley
 */
public class SendEmail {

	Properties props = new Properties();


	/**
	 *  Constructor for the SendEmail object
	 *
	 * @param  MAIL_TYPE    The mail type, for example 'mail.smtp.host'
	 * @param  MAIL_SERVER  The mail server, for example 'localhost', 'my-mail-server'
	 */
	public SendEmail(String MAIL_TYPE,
	                 String MAIL_SERVER) {

		props.put(MAIL_TYPE, MAIL_SERVER);
	}


	/**
	 *  Sends email to the given address(es).
	 *
	 * @param  toAddresses             To address(es)
	 * @param  fromAddress             From address
	 * @param  msgSubject              Email subject line
	 * @param  msgBody                 Email content body
	 * @exception  MessagingException  If error
	 */
	public void doSendEmail(
	                         String[] toAddresses,
	                         String fromAddress,
	                         String msgSubject,
	                         String msgBody) throws MessagingException {

		Session mailSession = Session.getDefaultInstance(props, null);
		MimeMessage message = new MimeMessage(mailSession);
		message.setText(msgBody);
		message.setSubject(msgSubject);
		for (int i = 0; i < toAddresses.length; i++) {
			Address address = new InternetAddress(toAddresses[i]);
			message.addRecipient(Message.RecipientType.TO, address);
		}
		Address from = new InternetAddress(fromAddress);
		message.setFrom(from);
		message.setSentDate(new Date());
		Transport.send(message);
	}


	/**
	 *  Sends email to the given address(es). Does not throw Exception if error but prints error message to System.out.
	 *
	 * @param  msgTo       To address(es)
	 * @param  msgFrom     From address
	 * @param  msgSubject  Subject
	 * @param  msgBody     Message body
	 * @return             True if successful, false otherwise
	 */
	public boolean doSend(String[] msgTo,
	                      String msgFrom,
	                      String msgSubject,
	                      String msgBody) {

		try {
			doSendEmail(msgTo, msgFrom, msgSubject, msgBody);
		} catch (MessagingException mex) {
			System.out.println("Mail message exception");
			mex.printStackTrace();
			return false;
		}
		return true;
	}


	/**
	 *  Sends email to the given address. Does not throw Exception if error but prints error message to System.out.
	 *
	 * @param  msgTo       To address
	 * @param  msgFrom     From address
	 * @param  msgSubject  Subject
	 * @param  msgBody     Message body
	 * @return             True if successful, false otherwise
	 */
	public boolean doSend(String msgTo,
	                      String msgFrom,
	                      String msgSubject,
	                      String msgBody) {

		return doSend(new String[]{msgTo}, msgFrom, msgSubject, msgBody);
	}
}

