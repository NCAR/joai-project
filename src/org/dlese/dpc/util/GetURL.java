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
package org.dlese.dpc.util;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 *  URL is a utility class for retreiving the contents of a given URL as a
 *  string
 *
 * @author    Ryan Deardorff
 */
public final class GetURL {

	private String address = "";

	/**
	 *  Gets the url as a string
	 *
	 * @param  address          URL
	 * @param  reportException  if an exception is thrown, should it be reported?
	 * @return                  The url value
	 */
	public static synchronized String getURL( String address, boolean reportException ) {
		String urlname = address;
		InputStream in = null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			URL url = new URL( urlname );                                // Create the URL
			URLConnection uc = url.openConnection();
			uc.setDefaultUseCaches( false );                             // Set no cacheing
			uc.setUseCaches( false );
			uc.setRequestProperty( "Cache-Control", "max-age=0,no-cache" );
			uc.setRequestProperty( "Pragma", "no-cache" );
			in = uc.getInputStream();
			// Now copy bytes from the URL to the output stream
			byte[] buffer = new byte[4096];
			int bytes_read;
			while ( ( bytes_read = in.read( buffer ) ) != -1 ) {
				out.write( buffer, 0, bytes_read );
			}
		}
		catch ( Exception e ) {
			if ( reportException ) {
				System.err.println( "Exception for " + urlname );
				e.printStackTrace();
				byte[] bytes = new String( "<!-- Error retreiving URL: " + address + " -->" ).getBytes();
				out.write( bytes, 0, bytes.length );
			}
		}
		finally {                                                     // Always close the stream
			try {
				in.close();
				out.close();
			}
			catch ( Exception e ) {}
		}
		return out.toString();
	}

	/**
	 *  Gets the url attribute of the GetURL class
	 *
	 * @param  address
	 * @param  reportException
	 * @return                  The url value
	 */
	public static synchronized String getUrl( String address, boolean reportException ) {
		return getURL( address, reportException );
	}

	// The following are for JSP bean access:

	/**
	 *  Gets the url attribute of the GetURL class
	 *
	 * @return    The url value
	 */
	public synchronized String getUrl() {
		return getURL( address, false );
	}

	/**
	 *  Sets the address attribute of the GetURL object
	 *
	 * @param  address  The new address value
	 */
	public synchronized void setAddress( String address ) {
		this.address = address;
	}
}

