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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;



/**
 * This implements the DLESE extended form of the combined log format
 * as documented at
 * <a href="http://httpd.apache.org/docs/logs.html"> Apache logs docs</a>.
 * <p>
 * The combined log format has the same first seven fields
 * as the common log format, and adds two more fields:
 * referrer and user-agent.
 * <p>
 * The DLESE extension to the combined log format adds several
 * more fields, documented below.
 * <p>
 * The fields, with their normal CGI variable names, are:
 * <p>
 * <h4> Common log format:</h4>
 * <dl>
 *		<dt> REMOTE_HOST
 *			<dd> Remote host IP number.<br>
 *			Example: 64.124.140.181<br>
 *		<dt> id
 *			<dd> See rfc1413 (obsoletes rfc931) remote logname of the user.
 *			Unreliable, seldom used.  Usually appears as "-".<br>
 *			Example: <code>-</code><br>
 *		<dt> REMOTE_USER
 *			<dd> The username as which the user has
 *			authenticated via password protection.<br>
 *			Example: <code>-</code><br>
 *			Example: <code>samuel</code><br>
 *		<dt> Date/time
 *			<dd> The date and time of request, UTC (GMT) time. <br>
 *			Note: this is the time when the server wrote the
 *			log record, not the time extracted from the
 *			possibly bogus date header. <br>
 *			Example: <code>[10/Nov/2002:07:48:54 -0700]</code><br>
 *		<dt> Request string
 *			<dd> The request line as it came from the client.
 *			Note: this is reconstructed using HttpServletRequest calls,
 *			so there may be slight differences in formatting from
 *			the original request string.<br>
 *			Example: <code>"GET /testldap/tldap HTTP/1.1"</code><br>
 *		<dt> Status
 *			<dd> The HTTP status code returned to the client.<br>
 *			Example: <code>200</code><br>
 *		<dt> Content length
 *			<dd> The content length of the document returned to the client.<br>
 *			Example: <code>7638</code><br>
 * </dl>
 * <p>
 * <h4> Combined log format extensions:</h4>
 * <dl>
 *		<dt> Referer
 *			<dd> The referer HTTP request header, in quotes.
 *			Someone long ago
 *			ago misspelled "referrer" and it's never been fixed.
 *			If unavailable, it appears quoted as "-", not as -.<br>
 *			Example: <code>"-"</code><br>
 *			Example: <code>"http://quake.dpc.ucar.edu/index.html"</code><br>
 *		<dt> User Agent
 *			<dd> The User_Agent HTTP request header, in quotes.
 *			If unavailable, it appears quoted as "-", not as -.<br>
 *			Example: <code>"-"</code><br>
 *			Example: <code>"Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.0.0) Gecko/20020529"</code><br>
 * </dl>
 * <p>
 * <h4> DLESE extensions:</h4>
 * <dl>
 *		<dt> SERVER_NAME
 *			<dd> The name of the server that received the request.<br>
 *			Example: <code>localhost</code><br>
 *		<dt> SERVER_PORT
 *			<dd> The port number of the server that received the request.<br>
 *			Example: <code>80</code><br>
 *		<dt> requestType
 *			<dd> The DLESE request type:
 *				<ul>
 *					<li> "search":  A search request
 *					<li> "full":    A request for a full record
 *					<li> "other":   Some other request
 *				</ul><br>
 *			Example: <code>search</code><br>
 *		<dt> numSearchResults
 *			<dd> Number of search results for "search" requests;
 *			0 otherwise.<br>
 *			Example: <code>98</code><br>
 *		<dt> numDbRecords
 *			<dd> Total number of records in the database.<br>
 *			Example: <code>9000</code><br>
 *		<dt> rankNum
 *			<dd> If "search": rank of first result in the displayed page.
 *			If "full": rank of this result.
 *			If "other": 0.<br>
 *			Example: <code>1</code><br>
 *		<dt> dleseId
 *			<dd> If "full": the DLESE ID of the record returned.
 *			Otherwise: "-"<br>
 *			Example: <code>"-"</code><br>
 *		<dt> xmlString
 *			<dd> An arbitrary string, generally XML.
 *				Internal backslashes, quotes, double quotes,
 *				newlines, etc. will be escaped.<br>
 *			Example: <code>"&lt;criteria testattr=\"testval\"&gt;&lt;field&gt;Free Text&lt;/field&gt;&lt;textSearchValue&gt;oceanography&lt;/textSearchValue&gt;&lt;textSearchTerm&gt;oceanography&lt;/textSearchTerm&gt;&lt;field&gt;Grade Level&lt;/field&gt;&lt;LearningContext&gt;High school&lt;/LearningContext&gt;&lt;/criteria&gt;"</code><br>
 * </dl>
 * <p>
 * <h4> Sample Usage</h4>
 * <pre>
 * public class TestServlet extends HttpServlet {
 * 
 * ClfLogger logger = null;
 * public void init( ServletConfig conf)
 * throws ServletException
 * {
 *     super.init(conf);
 * 
 *     String logfile = getInitParameter("test.log.file.name");
 *     try {
 *         logger = DleseLogManager.getClfLogger(
 *             Level.FINEST,  // minimum level this logger will accept
 *             true,          // append
 *             logfile);      // output log file
 *     }
 *     catch( LogException dle) {
 *         log("cannot init ClfLogger: " + dle);
 *         logger = null;
 *     }
 * } // end init
 * 
 * 
 * 
 * public void doGet(
 *     HttpServletRequest req,
 *     HttpServletResponse resp)
 * throws IOException, ServletException
 * {
 *     int statusCode = 200;
 *     int contentLen = 0;
 *     int numSearchResults = 98;
 *     int numDbRecs = 9000;
 *     int rankNum = 1;
 *     String xmlString
 *         = "&lt;someTag someAttr="xyz"&gt;some contents&lt;/someTag&gt;";
 *     logger.log(
 *         java.util.logging.Level.SEVERE,  // message importance
 *         req,                // the incoming request
 *         statusCode,         // returned http status code
 *         contentLen,         // returned content len, bytes
 *         "search",           // type: search, full, or other
 *         numSearchResults,   // num search results
 *         numDbRecs,          // total num records in the database
 *         rankNum,            // if search: rank of first rec in page
 *                             // if full: rank of this rec
 *         null,               // dleseId: if full: dlese ID of this rec
 *         xmlString);         // xml stg.  Will appear in double quotes.
 *                             // Internal backslashes, quotes, double quotes,
 *                             // newlines, etc. will be escaped.
 * } // end doGet
 * } // end class TestServlet
 * </pre>
 **/


public class ClfLogger {

private Level loggerLevel;
private String outfilename;
private File outfile,outdir;
private BufferedWriter wtr = null;
private boolean appendFlag = true;


/**
 * Returns a {@link ClfLogger ClfLogger}.
 * @param loggerLevel  The minumum level message this logger will accept.
 * @param appendFlag   If true, we append to outfile
 * @param outfilename  The name of output file
 */

ClfLogger(
	Level loggerLevel,		// one of java.util.logging.Level.*
	boolean appendFlag,		// if true, we append to outfile
	String outfilename)		// name of output file
throws LogException
{
	this.loggerLevel = loggerLevel;
	this.outfilename = outfilename;
	this.appendFlag = appendFlag;

	if (loggerLevel.intValue() < Level.FINEST.intValue()
		|| loggerLevel.intValue() > Level.SEVERE.intValue())
		throw new LogException(
			"ClfLogger.const: invalid loggerLevel: " + loggerLevel);
	
	try {
		outfile = new File(outfilename);
		outdir = outfile.getParentFile();
	}
	catch( Throwable t) {
		throw new LogException(
			"ClfLogger: cannot open output file \""
			+ outfilename + "\": " + t);
	}
}

/**
 * Returns the BufferedWriter to write to and ensures the file
 * being written to exists.
 * @return The BufferedWriter to write to
 */
private BufferedWriter getWriter()
throws Throwable
{
	if(!outfile.exists()){
		if(outdir != null)
			outdir.mkdirs();
		if(wtr != null)
			wtr.close();
		wtr = new BufferedWriter( new FileWriter(
			outfilename, appendFlag));
	}
	if(wtr == null)
		wtr = new BufferedWriter( new FileWriter(
			outfilename, appendFlag));
	return wtr;
}


public void close()
throws LogException
{
	try {
		wtr.close();
	}
	catch( IOException ioe) {
		throw new LogException(
			"ClfLogger: I/O error closing file \""
			+ outfilename + "\": " + ioe);
	}
}




public void log(
	Level level,
	HttpServletRequest req,		// the incoming request
	int statusCode,				// returned http status code
	int contentLen,				// returned content len, bytes
	String requestType,			// type: search, full, or other
	int numSearchResults,		// num search results
	int numDbRecords,			// total num records in the database
	int rankNum,				// if search: rank of first rec in page
								// if full: rank of this rec
	String dleseId,				// if full: dlese ID of this rec
	String xmlString)			// xml stg.  Will appear in double quotes.
								// Internal backslashes, quotes, double quotes,
								// newlines, etc. will be escaped.
{
	if (level.intValue() < Level.FINEST.intValue()
		|| level.intValue() > Level.SEVERE.intValue())
		mkerror("ClfLogger.log: invalid level: " + level);
	if (level.intValue() >= loggerLevel.intValue()) {
		logit( req, statusCode, contentLen, requestType,
			numSearchResults, numDbRecords, rankNum,
			dleseId, xmlString);
	}
}




private void logit(
	HttpServletRequest req,		// the incoming request
	int statusCode,				// returned http status code
	int contentLen,				// returned content len, bytes
	String requestType,			// type: search, full, or other
	int numSearchResults,		// num search results
	int numDbRecords,			// total num records in the database
	int rankNum,				// if search: rank of first rec in page
								// if full: rank of this rec
	String dleseId,				// if full: dlese ID of this rec
	String xmlString)			// xml stg.  Will appear in double quotes.
								// Internal backslashes, quotes, double quotes,
								// newlines, etc. will be escaped.
{
	StringBuffer outbuf = new StringBuffer(1000);
	//outbuf.append(req.getRemoteHost());			// REMOTE_HOST
	outbuf.append(req.getRemoteAddr());		/* getRemoteHost() was returning null
	when run through the apache connector, so using getRemoteAddr instead. */
	
	outbuf.append(' ');
	outbuf.append("- ");						// identity (unreliable)

	outbuf.append(' ');
	if (req.getRemoteUser() == null) outbuf.append('-');	// REMOTE_USER
	else outbuf.append( req.getRemoteUser());

	outbuf.append(' ');					// current date, not the date header
	long tmval = System.currentTimeMillis();
	// convert local time to UTC
	TimeZone tz = TimeZone.getDefault();
	tmval -= tz.getRawOffset();

	SimpleDateFormat sdf = new SimpleDateFormat(
		"[dd/MMM/yyyy:HH:mm:ss +0000]");
	outbuf.append( sdf.format( new Date(tmval)));


	// request string: "GET /apache_pb.gif HTTP/1.0"
	StringBuffer requestbuf = new StringBuffer();
	requestbuf.append(req.getMethod());				// REQUEST_METHOD
	requestbuf.append(' ');
	requestbuf.append(req.getContextPath());
	requestbuf.append(req.getServletPath());		// SCRIPT_NAME
	if (req.getPathInfo() != null)				// PATH_INFO
		requestbuf.append(req.getPathInfo());
	if (req.getQueryString() != null) {			// QUERY_STRING
		requestbuf.append('?');
		requestbuf.append(req.getQueryString());
	}
	requestbuf.append(' ');
	requestbuf.append(req.getProtocol());			// SERVER_PROTOCOL
	outbuf.append(' ');
	outbuf.append('\"');
	outbuf.append( filterString( new String( requestbuf)));
	outbuf.append('\"');


	outbuf.append(' ');							// http status
	outbuf.append(statusCode);

	outbuf.append(' ');							// content len
	outbuf.append(contentLen);


	// Combined log format extensions

	outbuf.append(' ');		// REFERER: "http://www.example.com/start.html"
	outbuf.append('\"');	// apache puts quotes around even "-"
	if (req.getHeader("referer") == null) outbuf.append('-');
	else {
		outbuf.append( filterString( req.getHeader("referer")));
	}
	outbuf.append('\"');

	outbuf.append(' ');		// USER-AGENT: "Mozilla/4.08 [en] (Win98; I ;Nav)"
	outbuf.append('\"');	// apache puts quotes around even "-"
	if (req.getHeader("user-agent") == null) outbuf.append('-');
	else outbuf.append( filterString( req.getHeader("user-agent")));
	outbuf.append('\"');


	// DLESE extensions

	outbuf.append(' ');		// SERVER_NAME
	if (req.getServerName() == null) outbuf.append('-');
	else outbuf.append(req.getServerName());

	outbuf.append(' ');		// SERVER_PORT
	outbuf.append(req.getServerPort());

	outbuf.append(' ');
	outbuf.append( requestType);	// request type: search/full/other

	outbuf.append(' ');
	outbuf.append( numSearchResults);	// num search results

	outbuf.append(' ');
	outbuf.append( numDbRecords);	// total num recs in the database

	outbuf.append(' ');				// rankNum
	outbuf.append( rankNum);		// if search: rank of first one in page
									// if full: rank of this one

	outbuf.append(" \"");			// dleseId
	if (dleseId == null || dleseId.length() == 0) outbuf.append('-');
	else outbuf.append( dleseId);
	outbuf.append('\"');

	outbuf.append(' ');				// xmlString
	outbuf.append('\"');
	if (xmlString == null || xmlString.length() == 0) outbuf.append('-');
	else outbuf.append( filterString( xmlString));
	outbuf.append('\"');

	// Finally write it.
	outbuf.append('\n');
	try {
		BufferedWriter myWriter = getWriter();
		myWriter.write( new String( outbuf), 0, outbuf.length());
		myWriter.flush();
	}
	catch( Throwable t) {
		mkerror("ClfLogger.logit: I/O error on file \""
			+ outfilename + "\": " + t);
	}
} // end logit





StringBuffer filterString( String stg) {
	int ii, jj;
	StringBuffer resbuf = new StringBuffer( stg.length() + 10);
	for (ii = 0; ii < stg.length(); ii++) {
		char cc = stg.charAt( ii);
		if      (cc == '\\') resbuf.append("\\\\");
		else if (cc == '\'') resbuf.append("\\\'");
		else if (cc == '\"') resbuf.append("\\\"");
		else if (cc == '\n') resbuf.append("\\n");
		else if (cc == '\r') resbuf.append("\\r");
		else if (Character.isLetterOrDigit( cc)
			|| " !#$%&()*+,-./:;<=>?@[]^_`{|}~".indexOf(cc) >= 0)
			resbuf.append(cc);
		else {
			// output unicode "\u1234".  Must pad to 4 hex digits.
			resbuf.append("\\u");
			String hexstg = Integer.toHexString( cc);
			for (jj = 0; jj < 4 - hexstg.length(); jj++) {
				resbuf.append('0');
			}
			resbuf.append( hexstg);
		}
	}
	return resbuf;
}




void mkerror( String msg) {
	System.out.println( msg);
}


} // end class ClfLogger

