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
package org.dlese.dpc.index.document;

import java.util.Date;
import java.text.ParseException;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateField;

/**
 *  Provides support for converting dates to strings and vice-versa using seconds as the default time
 *  granularity. The strings are structured so that lexicographic sorting orders them by date, which makes
 *  them suitable for use as field values and search terms. This class simply wraps the appropriate methods
 *  from {@link org.apache.lucene.document.DateTools} and applies a resolution of seconds.
 *
 * @author    John Weatherley
 * @see       org.apache.lucene.document.DateTools
 */
public class DateFieldTools {

	/**
	 *  Converts a Date to a string suitable for indexing using resolution to seconds.
	 *
	 * @param  date  The Date
	 * @return       A string in format yyyyMMddHHmmss; using UTC as timezone
	 */
	public final static String dateToString(Date date) {
		return DateTools.dateToString(date, DateTools.Resolution.SECOND);
	}


	/**
	 *  Converts a millisecond time to a string suitable for indexing using resolution to seconds.
	 *
	 * @param  time  Time in millisonds
	 * @return       A string in format yyyyMMddHHmmss; using UTC as timezone
	 */
	public final static String timeToString(long time) {
		return DateTools.timeToString(time, DateTools.Resolution.SECOND);
	}


	/**
	 *  Converts a string produced by {@link #timeToString} or {@link #dateToString} back to a time, represented
	 *  as a Date object. Is also able to parse dates encoded in the old Lucene 1.x DateField format, for
	 *  compatibility with old indexes (this functionality will go away in a future release).
	 *
	 * @param  dateString          A string produced by timeToString or dateToString
	 * @return                     The parsed time as a Date object
	 * @exception  ParseException  If parse error
	 */
	public final static Date stringToDate(String dateString) throws ParseException {
		try {
			return DateTools.stringToDate(dateString);
		} catch (ParseException pe) {
			// Handle dates encoded in the Lucene 1.x format, for compatibility with old indexes
			try {
				// This method will go away in a future release of Lucene...
				return DateField.stringToDate(dateString);
			} catch (Throwable t) {
				throw new ParseException("Unable to parse date string: " + t.getMessage(), 0);
			}
		}
	}


	/**
	 *  Converts a string produced by {@link #timeToString} or {@link #dateToString} back to a time, represented
	 *  as the number of milliseconds since January 1, 1970, 00:00:00 GMT. Is also able to parse dates encoded in
	 *  the old Lucene 1.x DateField format, for compatibility with old indexes (this functionality will go away
	 *  in a future release).
	 *
	 * @param  dateString          A string produced by timeToString or dateToString
	 * @return                     The number of milliseconds since January 1, 1970, 00:00:00 GMT
	 * @exception  ParseException  If parse error
	 */
	public final static long stringToTime(String dateString) throws ParseException {
		try {
			return DateTools.stringToTime(dateString);
		} catch (ParseException pe) {
			// Handle dates encoded in the Lucene 1.x format, for compatibility with old indexes
			try {
				// This method will go away in a future release of Lucene...
				return DateField.stringToTime(dateString);
			} catch (Throwable t) {
				throw new ParseException("Unable to parse date string: " + t.getMessage(), 0);
			}
		}
	}
}

