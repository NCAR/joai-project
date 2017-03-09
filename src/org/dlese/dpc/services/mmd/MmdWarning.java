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
package org.dlese.dpc.services.mmd;

import org.dlese.dpc.util.DpcErrors;



/**
 * Describes a single warning message for a metadata record.
 */

public class MmdWarning {

/** Message type: see org.dlese.dpc.util.DpcErrors */
private int msgType;

/** The metadata XML file name */
private String filename;

/** The xpath within the metadata XML file */
private String xpath;

/** The url type: "primary-url", etc. */
private String urllabel;

/** The url itself */
private String url;

/** Additional error description */
private String msg;

/** Additional error info */
private String auxinfo;


/** Creates an MmdWarning with the specified parms */

MmdWarning(
	int msgType,			// See org.dlese.dpc.util.DpcErrors
	String filename,		// metadata XML file
	String xpath,			// xpath within the metadata XML file
	String urllabel,		// url type: "primary-url", etc.
	String url,				// url itself
	String msg,				// additional error description
	String auxinfo)			// additional info
{
	this.msgType = msgType;
	this.filename = filename;
	this.xpath = xpath;
	this.urllabel = urllabel;
	this.url = url;
	this.msg = msg;
	this.auxinfo = auxinfo;
}


public String toString() {
	String res = "MmdWarning:\n"
		+ "    msgType: " + msgType + "\""
		+ DpcErrors.getMessage( msgType) + "\"\n"
		+ "    filename: \"" + filename + "\n"
		+ "    xpath: \"" + xpath + "\n"
		+ "    urllabel: \"" + urllabel + "\n"
		+ "    url: \"" + url + "\n"
		+ "    msg: \"" + msg + "\n"
		+ "    auxinfo: \"" + auxinfo + "\n";
	return res;
}


/** Returns the message type: see org.dlese.dpc.util.DpcErrors */
public int getMsgType() { return msgType; }

/** Returns the metadata XML file name */
public String getFilename() { return filename; }

/** Returns the xpath within the metadata XML file */
public String getXpath() { return xpath; }

/** Returns the url type: "primary-url", etc. */
public String getUrllabel() { return urllabel; }

/** Returns the url itself */
public String getUrl() { return url; }

/** Returns the additional error description */
public String getMsg() { return msg; }

/** Returns the additional error info */
public String getAuxinfo() { return auxinfo; }


} // end class MmdWarning

