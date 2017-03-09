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

import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Iterator;



/**
 * Represents a resource to be scanned, and the results from that scan.
 * Each ResourceDesc represents one resource but may have
 * multiple PageDescs: for the primary URL, mirrors, related sites, etc.
 */


class ResourceDesc {

private String collKey;
private String metastyle;
private String dirPath;
private String fileName;
private String[] urlOnlyTests;

private String id;
private long metaChecksum;
private ResourceDesc duplicateRsd = null;


String PrimaryContent = null ; 
String primarycontentType = null; 
int numpages;
PageDesc[] pages;
WarnBuf warnBuf = null;		// warnings



/**
 * @param id The resource ID.
 * @param collKey  The collection key.
 * @param fileName The full file path of the id's XML file.
 * @param pages The PageDescs associated with this resource.
 */

ResourceDesc(
	String collKey,
	String metastyle,
	String dirPath,
	String fileName,
	String[] urlOnlyTests)
throws IdmapException
{
	int ii;

	this.collKey = collKey;
	this.metastyle = metastyle;

	this.dirPath = dirPath;
	if (this.dirPath.endsWith("/"))			// strip trailing "/"
		this.dirPath = this.dirPath.substring( 0, this.dirPath.length() - 1);

	this.fileName = fileName;
	this.urlOnlyTests = urlOnlyTests;

	// Set default id for error msgs until real id is filled in.
	this.id = "(file: " + fileName + ")";



	metaChecksum = 0;
	numpages = 0;
	pages = null;
	warnBuf = null;
}



public String toString() {
	return "id: " + id + "  primary URL: \"" + getPrimaryUrl()
		+ "\"  num warnings: " + numWarnings();
}


String getCollKey() {
	return collKey;
}


String getFileName() {
	return fileName;
}


String getFullFileName() {
	String res = dirPath + "/" + fileName;
	return res;
}


int getUrlOnlyTestsLength() {
	int ires = 0;
	if (urlOnlyTests != null) ires = urlOnlyTests.length;
	return ires;
}


String getUrlOnlyTests( int ii) {
	return urlOnlyTests[ ii];
}


String getId() {
	return id;
}


void setId( String id) {
	this.id = id;
}


long getMetaChecksum() {
	return metaChecksum;
}


void setMetaChecksum( long metaChecksum) {
	this.metaChecksum = metaChecksum;
}


ResourceDesc getDuplicateRsd() {
	return duplicateRsd;
}


void setDuplicateRsd( ResourceDesc duplicateRsd) {
	this.duplicateRsd = duplicateRsd;
}


boolean testPrimary() {
	boolean bres = false;
	// If there is no primary URL, or it's invalid,
	// but there are other URLs, we don't want to use one
	// of the others as the primary.
	if (numpages > 0
		&& pages[0].urllabel != null
		&& pages[0].urllabel.equals("primary-url"))
		bres = true;
	return bres;                                                               
}


String getPrimaryXpath() {
	String res = null;
	if (testPrimary()) res = pages[0].xpath;
	return res;
}


String getPrimaryUrllabel() {
	String res = null;
	if (testPrimary()) res = pages[0].urllabel;
	return res;
}


String getPrimaryUrl() {
	String res = null;
	if (testPrimary()) res = pages[0].urlstg;
	return res;
}


long getPrimaryChecksum() {
	long res = 0;
	if (testPrimary()) res = pages[0].pageChecksum;
	return res;
}





void addPage( PageDesc page) {
	if (numpages == 0) {
		pages = new PageDesc[] { page};
		numpages = 1;
	}
	else {
		PageDesc[] newpages = new PageDesc[ numpages + 1];
		System.arraycopy( pages, 0, newpages, 0, numpages);
		newpages[ numpages] = page;
		pages = newpages;
		numpages++;
	}
}



void addWarning( Warning warn) {
	if (warnBuf == null) warnBuf = new WarnBuf();
	warnBuf.add( warn);
}




int numWarnings() {
	int ires = 0;
	if (warnBuf != null) ires = warnBuf.length();
	return ires;
}



Iterator warningIterator() {
	Iterator iter = null;
	if (warnBuf != null) iter = warnBuf.iterator();
	return iter;
}



boolean hasSevereError() {
	boolean bres = false;
	if (warnBuf != null && warnBuf.hasSevereError()) bres = true;
	return bres;
}



/**
 * Simply throws an IdmapException.
 */

static void mkerror( String msg)
throws IdmapException
{
	throw new IdmapException( msg);
}


/**
 * Prints a String without a trailing newline.
 */

static void prtstg( String msg) {
	System.out.print( msg);
}


/**
 * Prints a String with a trailing newline.
 */

static void prtln( String msg) {
	System.out.println( msg);
}


} // end class ResourceDesc



