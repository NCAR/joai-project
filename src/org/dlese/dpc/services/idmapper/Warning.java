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
package org.dlese.dpc.services.idmapper;

import org.dlese.dpc.util.DpcErrors;


/**
 * Represents a single warning message.
 */

class Warning {



int msgType;
String id;
String filename;
String xpath;
String urllabel;
String url;
String msg;
String auxinfo;

Warning(
	int msgType,
	String id,
	String filename,
	String xpath,
	String urllabel,
	String url,
	String msg,
	String auxinfo)
{
	this.msgType = msgType;
	this.id = id;
	this.filename = filename;
	this.xpath = xpath;
	this.urllabel = urllabel;
	this.url = url;
	this.msg = msg;
	this.auxinfo = auxinfo;
}


public String toString() {
	String res = "msgType: " + msgType;
	res += "  " + DpcErrors.getMessage( msgType);
	res += "  id: " + id
		+ "  filename: " + filename
		+ "  xpath: " + xpath
		+ "  urllabel: " + urllabel
		+ "  url: " + url
		+ "  msg: " + msg
		+ "  auxinfo: " + auxinfo;
	return res;
}



boolean isSevere() {
	boolean bres = false;
	if (msgType < DpcErrors.IDMAP_SEVERE_LIMIT) bres = true;
	return bres;
}

} // end class

