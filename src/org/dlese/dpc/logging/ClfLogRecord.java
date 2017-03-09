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
package org.dlese.dpc.logging;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;



/**
 * Represents a single log record created by {@link ClfLogger ClfLogger}.
 * Log files may be parsed by calling
 * {@link ClfLogRecord#parse ClfLogRecord.parse}.
 */

public class ClfLogRecord {

private String inline;
private int linenum;
private int linelen;
private int ipos;



/** Remote host IP number. */
public String remoteHost;

/** id.  See rfc1413 (obsoletes rfc931) remote logname of the user.
 * Unreliable, seldom used.  Usually appears as "-".<br>
 */
public String identity;

/** The username as which the user has
 * authenticated via password protection.
 */
public String remoteUser;

/** The date and time of request, UTC (GMT) time.
 * Note: this is the time when the server wrote the
 * log record, not the time extracted from the
 * possibly bogus date header.
 */
public Date requestDateUtc;

/**
 *	<dd> The request line as it came from the client.
 *	Note: this is reconstructed using HttpServletRequest calls,
 *	so there may be slight differences in formatting from
 *	the original request string.<br>
 *	Example: "GET /testldap/tldap HTTP/1.1"
 */
public String requestString;

/** The HTTP status code returned to the client. */
int status;

/** The content length of the document returned to the client. */
public int contentLength;

/** The referrer, but long ago was misspelled it as 
 * the "referer" HTTP request header.
 * If unavailable, it appears quoted as "-", not as -.
 */
public String referrer;

/** The User_Agent HTTP request header, in quotes.
 * If unavailable, it appears quoted as "-", not as -.
 */
public String userAgent;

/** The name of the server that received the request. */
public String serverName;

/** The port number of the server that received the request. */
public int serverPort;

/** The DLESE request type: search/full/other. */
public String requestType;

/** Number of search results for search requests; 0 otherwise */
public int numSearchResults;

/** Total number of records in the database */
public int numDbRecords;

/**	If search: rank of first result in the displayed page.
 * If full: rank of this result.
 */
public int rankNum;

/** If full: DLESE ID of the record returned.  Otherwise: "-" */
public String dleseId;

/** An arbitrary string, generally XML. */
public String xmlString;




public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("remoteHost: \"" + remoteHost + "\"\n");
	buf.append("identity: \"" + identity + "\"\n");
	buf.append("remoteUser: \"" + remoteUser + "\"\n");
	buf.append("requestDateUtc: " + requestDateUtc + "\n");
	buf.append("requestString: \"" + requestString + "\"\n");
	buf.append("status: " + status + "\n");
	buf.append("contentLength: " + contentLength + "\n");
	buf.append("referrer: \"" + referrer + "\"\n");
	buf.append("userAgent: \"" + userAgent + "\"\n");
	buf.append("serverName: \"" + serverName + "\"\n");
	buf.append("serverPort: " + serverPort + "\n");
	buf.append("requestType: \"" + requestType + "\"\n");
	buf.append("numSearchResults: " + numSearchResults + "\n");
	buf.append("numDbRecords: " + numDbRecords + "\n");
	buf.append("rankNum: " + rankNum + "\n");
	buf.append("dleseId: \"" + dleseId + "\"\n");
	buf.append("xmlString: \"" + xmlString + "\"\n");
	return new String( buf);
}



/**
 * Creates a ClfLogRecord by parsing the specified input line.
 * Sample use:
 * <pre>
 *        int linenum = 0;
 *        BufferedReader rdr = new BufferedReader( new FileReader( inname));
 *        while (true) {
 *            String inline = rdr.readLine();
 *            if (inline == null) break;
 *            linenum++;
 *            ClfLogRecord rec = ClfLogRecord.parse( inline, linenum);
 *            System.out.println("\nrecord " + linenum + ":\n" + rec);
 *        }
 *        rdr.close();
 * <pre>
 */

public static ClfLogRecord parse(
	String inline,				// input log line
	int linenum)				// line number, for error msgs only
throws LogException
{
	ClfLogRecord rec = new ClfLogRecord();
	rec.parseSub( inline, linenum);
	return rec;
}



private void parseSub(
	String inline,				// input log line
	int linenum)				// line number, for error msgs only
throws LogException
{
	this.inline = inline;
	this.linenum = linenum;
	linelen = inline.length();
	ipos = 0;

	remoteHost = getField( null);
	identity = getField( null);
	remoteUser = getField( null);

	// Foobared SimpleDateFormat.parse  always translates
	// dates to local time, so we must translate back to UTC.
	String datestg = getField( "[]");
	SimpleDateFormat sdf = new SimpleDateFormat( "dd/MMM/yyyy:HH:mm:ss Z");
	Date localDate = null;
	try { localDate = sdf.parse( datestg); }
	catch( ParseException pex) {
		baddata("invalid date: " + pex);
	}
	if (localDate == null) baddata("invalid date");
	TimeZone tz = TimeZone.getDefault();
	requestDateUtc = new Date( localDate.getTime() - tz.getRawOffset());

	requestString = getField( "\"\"");
	status = getInt();
	contentLength = getInt();

	// Combined log format extensions
	referrer = getField( "\"\"");
	userAgent = getField( "\"\"");

	// DLESE extensions
	serverName = getField( null);
	serverPort = getInt();
	requestType = getField( null);
	numSearchResults = getInt();
	numDbRecords = getInt();
	rankNum = getInt();
	dleseId = getField( "\"\"");
	xmlString = getField( "\"\"");
}




int getInt()
throws LogException
{
	int ires = 0;
	String stg = getField( null);
	try { ires = Integer.parseInt( stg, 10); }
	catch( NumberFormatException nfe) {
		baddata("invalid number");
	}
	return ires;
}



String getField(
	String quotes)		// If not quoted, null.  Otherwise, 2 chars:
						// beginning and ending quote chars.
throws LogException
{

	// Skip white space
	while (ipos < linelen && Character.isWhitespace( inline.charAt(ipos))) {
		ipos++;
	}
	if (ipos >= linelen) baddata("rec too short");

	// Find end of field, accumulating chars in buf
	StringBuffer resbuf = new StringBuffer();

	int iend = ipos;
	if (quotes != null) {
		if (inline.charAt(iend) != quotes.charAt(0)) baddata("invalid quotes");
		iend++;
		if (iend >= linelen) baddata("invalid quotes");
	}
	while (iend < linelen) {
		char cc = inline.charAt(iend);
		iend++;
		if (quotes != null) {
			if (cc == quotes.charAt(1)) {
				iend++;
				break;
			}
		}
		else if ( Character.isWhitespace(cc)) break;
		if (cc == '\\') {
			if (iend >= linelen) baddata("invalid backslash");
			char dd = inline.charAt(iend);
			iend++;
			if      (dd == '\\') resbuf.append('\\');
			else if (dd == '\'') resbuf.append('\'');
			else if (dd == '\"') resbuf.append('\"');
			else if (dd == 'n') resbuf.append('\n');
			else if (dd == 'r') resbuf.append('\r');
			else if (dd == 'u') {
				if (iend + 4 - 1 >= linelen) baddata("invalid unicode");
				String hexstg = inline.substring( iend, iend + 4);
				iend += 4;
				dd = (char) Integer.parseInt( hexstg, 16);
				resbuf.append( dd);
			}
		}
		else resbuf.append( cc);
	}

	String res = new String( resbuf);
	ipos = iend;
	return res;
}





void baddata( String msg)
throws LogException
{
	throw new LogException("parseRec: Invalid line: " + msg
		+ ".  Line num: " + linenum
		+ ".  Contents: \"" + inline + "\"");
}


} // end class ClfLogRecord
