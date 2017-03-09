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
 *  Represents one Record retrieved from the DDS,
 *  and its pertinent information.
 *
 * @author     Sonal Bhushan
 * @created    October 13, 2004
 */
class Rec {

	private String id;
	private String collid;
	private String colllabel;
	private String xmlformat;
	private String accessionstatus;
	private String libacc;
	private EmailAdd[] emails;
	private int numemails;
	private boolean personEmail ; 



	/**
	 *Constructor for the Rec object
	 *
	 * @param  Id               Record Id
	 * @param  Collid           Collection Id
	 * @param  Colllabel        Collection Label
	 * @param  Xmlformat        XML Format (e.g. "adn")
	 * @param  Accessionstatus  accession status of the records
	 */
	Rec(String Id, String Collid, String Colllabel,
			String Xmlformat, String Accessionstatus) {
		this.id = Id;
		this.collid = Collid;
		this.colllabel = Colllabel;
		this.xmlformat = Xmlformat;
		this.accessionstatus = Accessionstatus;
		this.emails = null;
		this.numemails = 0;
		this.personEmail = false;
	}


	/**
	 * Constructor for the Rec object with no parameters passed in
	 */
	Rec() {

		this.id = null;
		this.collid = null;
		this.colllabel = null;
		this.xmlformat = null;
		this.accessionstatus = null;
		this.emails = null;
		this.numemails = 0;
		this.personEmail = false;
	}


	/**
	 *  print out the Rec object for debugging purposes
	 */
	public String toString() {

		String ret = "RECORD \n"+ "Rec Id: " + this.id + "\n"
				 + "Coll Id: " + this.collid + "\n"
				 + "Coll Label: " + this.colllabel + "\n"
				 + "XML Format: " + this.xmlformat + "\n"
				 + "Accession Status: " + this.accessionstatus + "\n" ;
		for (int i = 0; i < this.numemails; i++) {
			ret += emails[i].toString() + "\n";
		}
		return ret;
	}


	/**
	 *  Gets the id attribute of the Rec object
	 *
	 * @return    The id value
	 */
	String getId() {
		return this.id;
	}


	/**
	 *  Sets the id attribute of the Rec object
	 *
	 * @param  Id  The new id value
	 */
	void setId(String Id) {
		this.id = Id;
	}


	/**
	 *  Gets the collid attribute of the Rec object
	 *
	 * @return    The collId value
	 */
	String getCollId() {
		return this.collid;
	}


	/**
	 *  Sets the collId attribute of the Rec object
	 *
	 * @param  CollId  The new collId value
	 */
	void setCollId(String CollId) {
		this.collid = CollId;
	}


	/**
	 *  Gets the colllabel attribute of the Rec object
	 *
	 * @return    The colllabel value
	 */
	String getCollLabel() {
		return this.colllabel;
	}


	/**
	 *  Sets the collLabel attribute of the Rec object
	 *
	 * @param  CollLabel  The new collLabel value
	 */
	void setCollLabel(String CollLabel) {
		this.colllabel = CollLabel;
	}


	/**
	 *  Gets the xmlformat attribute of the Rec object
	 *
	 * @return    The xMLFormat value
	 */
	String getXMLFormat() {
		return this.xmlformat;
	}


	/**
	 *  Sets the xMLFormat attribute of the Rec object
	 *
	 * @param  XMLFormat  The new xMLFormat value
	 */
	void setXMLFormat(String XMLFormat) {
		this.xmlformat = XMLFormat;
	}


	/**
	 *  Gets the accessionstatus attribute of the Rec object
	 *
	 * @return    The accessionStatus value
	 */
	String getAccessionStatus() {
		return this.accessionstatus;
	}


	/**
	 *  Sets the accessionStatus attribute of the Rec object
	 *
	 * @param  AccessionStatus  The new accessionStatus value
	 */
	void setAccessionStatus(String AccessionStatus) {
		this.accessionstatus = AccessionStatus;
	}


	/**
	 *  Gets the numemails attribute of the Rec object
	 *
	 * @return    The numEmails value
	 */
	int getNumEmails() {
		return this.numemails;
	}


	/**
	 *  Adds a feature to the Email attribute of the Rec object
	 *
	 * @param  email  The feature to be added to the Email attribute
	 */
	void addEmail(EmailAdd email) {
		if (numemails == 0) {
			emails = new EmailAdd[]{email};
			numemails = 1;
		}
		else {
			EmailAdd[] newemails = new EmailAdd[numemails + 1];
			System.arraycopy(emails, 0, newemails, 0, numemails);
			newemails[numemails] = email;
			emails = newemails;
			numemails++;
		}
	}


	/**
	 *  Gets the emails attribute of the Rec object
	 *
	 * @return    The emailAdds value
	 */
	EmailAdd[] getEmailAdds() {

		if (this.emails == null) {
			return null;
		}
		if (this.numemails != 0) {
			return emails;
		}
		else {
			return null;
		}
	}
	
	boolean getPersonFlag(){
		return this.personEmail;	
	}
	
	void setPersonFlag(boolean p){
		this.personEmail = p;	
	}

}

