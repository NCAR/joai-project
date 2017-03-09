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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.logging.XMLFormatter;


/**
 * Provides debug logging by wrapping java.util.logging.Logger.
 * Sample usage:
 * <pre>
 *    import java.util.logging.Level;
 *    import org.dlese.dpc.logging.DebugLogger;
 *    import org.dlese.dpc.logging.DleseLogManager;
 *
 *    DebugLogger log = null;
 *    try {
 *        log = DleseLogManager.getDebugLogger(
 *            DleseLogManager.LOG_STD,
 *            timeType,
 *            outputFormat,
 *            appendFlag,
 *            loggerLevel,
 *            outfilename);
 *    }
 *    catch( LogException lexc) {
 *        ...
 *    }
 *
 *    // Each logging level has a convenience method, so
 *    // the following two calls are equivalent:
 *    log.log( Level.INFO, "some message");
 *    log.info( "some message");
 *
 *    // The 7 levels are:
 *    log.finest("some message");
 *    log.fine("some message");
 *    log.fine("some message");
 *    log.config("some message");
 *    log.info("some message");
 *    log.warning("some message");
 *    log.severe("some message");
 *
 *    // Additionally, the java.util.logging.Logger.log() method
 *    // can accept an array of Objects, also to be printed in the log:
 *    log.log(Level.INFO,
 *        "some message",
 *        new Object[] { new Integer(123), new Double(77.88)});
 *
 *    // The log file is closed automatically by the logging framework.
 * </pre>
 */


public class DebugLogger extends Logger {

private DebugLogger(String name) {
	super(name, null);
}


/**
 * Returns a {@link DebugLogger DebugLogger}.
 * @param outputType   Must always be LOG_STD.  For future flexibility.
 * @param timeType     The type of time stamp:
 *                     Either LOG_UTCTIME or LOG_LOCALTIME
 * @param outputFormat Either LOG_XML or LOG_TEXT.  Normally LOG_TEXT.
 * @param loggerLevel  The minumum level message this logger will accept.
 * @param appendFlag   If true, we append to outfile
 * @param outfilename  The name of output file
 */

static DebugLogger getDebugLogger(
	int outputType,			// For now, always LOG_STD
	int timeType,			// Either LOG_UTCTIME or LOG_LOCALTIME
	int outputFormat,		// Either LOG_XML or LOG_TEXT
	Level loggerLevel,		// minimum level that will be logged
	boolean appendFlag,		// if true, append to outputFile
	String outfilename)		// name of output file
throws LogException
{
	int ii;
	Handler[] handlers;
	if (outputType != DleseLogManager.LOG_STD)
		throw new LogException("DebugLogger: invalid outputType: "
			+ outputType);

	String name = DebugLogger.class.getName();

	LogManager mgr = LogManager.getLogManager();
	DebugLogger lgr = (DebugLogger) mgr.getLogger(name);
	if (lgr == null) {
		lgr = new DebugLogger(name);
		mgr.addLogger(lgr);
	}

	lgr.setLevel( loggerLevel);

	// Delete all existing handlers from log default setup
	handlers = lgr.getHandlers();
	for (ii = 0; ii < handlers.length; ii++) {
		lgr.removeHandler( handlers[ii]);
	}

	// Get rid of annoying propagation to root logger
	//  (Logger.getLogger(""))
	// The root logger always logs to System.out,
	// level = INFO.
	lgr.setUseParentHandlers( false);

	// Add our own handler
	OutputStream outstrm = null;
	if (outfilename.equals("-")) outstrm = System.out;
	else {
		try {
			outstrm = new BufferedOutputStream(
				new FileOutputStream( outfilename, appendFlag));
		}
		catch( IOException ioe) {
			throw new LogException("DebugLogger: cannot open file \""
				+ outfilename + "\"  exc: " + ioe);
		}
	}
	Formatter fmtr;
	if (outputFormat == DleseLogManager.LOG_TEXT)
		fmtr = new DleseFormatter( timeType);
	else if (outputFormat == DleseLogManager.LOG_XML)
		fmtr = new XMLFormatter();
	else throw new LogException("DebugLogger: invalid outputFormat: "
		+ outputFormat);

	StreamHandler hndlr = new StreamHandler( outstrm, fmtr);
	hndlr.setLevel( loggerLevel);
	lgr.addHandler( hndlr);
	return lgr;
}

} // end class DebugLogger





class DleseFormatter extends Formatter {

int timeType;

DleseFormatter( int timeType)
throws LogException
{
	super();
	this.timeType = timeType;
	if (timeType != DleseLogManager.LOG_UTCTIME
		&& timeType != DleseLogManager.LOG_LOCALTIME)
		throw new LogException("DleseFormatter: invalid timeType: "
			+ timeType);
}


static SimpleDateFormat dateformat = new SimpleDateFormat(
	"yyyy/MM/dd HH:mm:ss.SSS");


public String format( LogRecord rec) {
	int ii;
	StringBuffer outbuf = new StringBuffer(200);
	long tmval = rec.getMillis();
	if (timeType == DleseLogManager.LOG_UTCTIME) {
		// convert local time to UTC
		TimeZone tz = TimeZone.getDefault();
		tmval -= tz.getRawOffset();
	}
	outbuf.append( dateformat.format( new Date(tmval)));
	outbuf.append(" ");
	outbuf.append(rec.getLevel());
	outbuf.append(" ");
	String classname = rec.getSourceClassName();
	int ix = classname.lastIndexOf(".");
	if (ix >= 0 && ix < classname.length() - 1)
		classname = classname.substring( ix + 1);
	outbuf.append(classname);
	outbuf.append(".");
	outbuf.append(rec.getSourceMethodName());
	outbuf.append(" ");
	outbuf.append(rec.getMessage());
	outbuf.append("\n");
	Object[] parms = rec.getParameters();
	if (parms != null && parms.length > 0) {
		for (ii = 0; ii < parms.length; ii++) {
			outbuf.append("    parm ");
			outbuf.append(Integer.toString(ii));
			outbuf.append(": ");
			if (parms[ii] == null) outbuf.append("(null)");
			else outbuf.append( parms[ii].toString());
			outbuf.append("\n");
		}
	}
	return new String( outbuf);
}
} // end class DleseFormatter



