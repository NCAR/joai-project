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
package org.dlese.dpc.services.cas;


/**
 *  Contains info about the email addresses present
 *  in a Record , and the type of email address it is
 *  (person or institute)
 *
 * @author     Sonal Bhushan
 * @created    October 13, 2004
 */
class EmailAdd {



	String email_type;
	//should be an enum
	String email_address;


	/**
	 *Constructor for the EmailAdd object
	 *
	 * @param  type     Person or Institute
	 * @param  address  Email Address
	 */
	EmailAdd(String type, String address) {
		this.email_type = type;
		this.email_address = address;
	}


	/**
	 *  Output the EmailAdd object on System.out for debugging
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {

		String email = "Email Address : " + this.email_address;
		if (this.email_type.equalsIgnoreCase("INST")){
			email += "  Email Type : Institute ";
		}
		else if (this.email_type.equalsIgnoreCase("PERSON")){
			email += "  Email Type : Person ";
		}

		return email;
	}


	/**
	 *  Gets the emailAddress attribute of the EmailAdd object
	 *
	 * @return    The emailAddress value
	 */
	String getEmailAddress() {
		return this.email_address;
	}


	/**
	 *  Gets the emailType attribute of the EmailAdd object
	 *
	 * @return    The emailType value
	 */
	String getEmailType() {
		return this.email_type;
	}
	

}

