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
package org.dlese.dpc.vocab;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.PageContext;
import org.dlese.dpc.util.strings.StringUtil;
import java.text.*;
import org.dlese.dpc.webapps.tools.GeneralServletTools;

/**
 *  Check the timestamp on the base loader XML, and reload when it changes
 */
public class NewVocabsChecker extends Thread {

	String loaderFile;
	private volatile String lastUpdate = "";
	ServletContext servletContext;
	Socket s = null;
	BufferedReader in = null;
	PrintWriter out = null;
	static volatile boolean keepRunning = true;                    // used to shut down the thread
	String instanceName;
	MetadataVocabServlet loaderServlet;
	int sleepDuration = 60000;

	/**
	 *  Constructor for the NewVocabsChecker object
	 *
	 * @param  loaderFile
	 * @param  servletContext
	 * @param  instanceName
	 * @param  loaderServlet
	 */
	public NewVocabsChecker( MetadataVocabServlet loaderServlet, String loaderFile, ServletContext servletContext, String instanceName ) {
		this.loaderServlet = loaderServlet;
		this.instanceName = instanceName;
		this.loaderFile = loaderFile;
		if ( !loaderFile.startsWith( "http://" ) ) {
			try {
				this.loaderFile = GeneralServletTools.getAbsolutePath( loaderFile, servletContext );
			}
			catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		this.servletContext = servletContext;
		String duration = servletContext.getInitParameter( "metadataGroupsReloadCheckerDuration" );
		if ( duration != null ) {
			sleepDuration = Integer.parseInt( duration );
		}
		lastUpdate = getLastUpdate();
	}

	/**
	 *  Main processing method for the NewVocabsChecker object
	 */
	public void run() {
		while ( keepRunning ) {
			try {
				sleep( sleepDuration );
			}
			catch ( Exception e ) {                                      // thrown upon interrupt when sleeping
				keepRunning = false;
				Thread.currentThread().interrupt();
			}
			String getLast = getLastUpdate();
			if ( keepRunning && ( !lastUpdate.equals( getLast ) ) ) {
				System.out.println( "Reloading vocab instance '" + instanceName + "'" );
				lastUpdate = getLast;
				loaderServlet.loadVocabs();
			}
		}
		closeConnections();
	}

	/**
	 *  Description of the Method
	 */
	public void shutDown() {
		keepRunning = false;
		interrupt();
		System.out.println( "Done shutting down new vocabs checker." );
	}

	/**
	 *  Gets the last modified date of the loader file
	 *
	 * @return    The lastUpdate value
	 */
	private synchronized String getLastUpdate() {
		if ( loaderFile.startsWith( "http://" ) ) {
			try {
				URL url = new URL( loaderFile );
				String hostName = url.getHost();
				int hostPort = ( url.getPort() > 0 ) ? url.getPort() : 80;
				s = new Socket( hostName, hostPort );
				in = new BufferedReader( new InputStreamReader( s.getInputStream() ) );
				String path = loaderFile.substring( loaderFile.indexOf( "/", 8 ),
					loaderFile.length() ).replaceAll( "//", "/" );
				out = new PrintWriter( s.getOutputStream(), true );
				out.print( "HEAD " + path + " HTTP/1.0\r\n" );
				out.print( "Host: " + hostName + ( hostPort != 80 ? ":" + hostPort : "" ) + "\r\n\r\n" );
				out.flush();
				String line;
				while ( ( line = in.readLine() ) != null ) {
					if ( line.matches( ".*Last-Modified:.*" ) ) {
						String lastMod = line.replaceFirst( ".*Last\\-Modified:\\s*(.*)", "$1" );
						closeConnections();
						return lastMod;
					}
				}
			}
			catch ( Exception e ) {
				e.printStackTrace();
			}
			finally {                                                    // Always close the streams!
				closeConnections();
			}
		}
		else {
			return new Long( new File( loaderFile ).lastModified() ).toString();
		}
		return "";
	}

	/**
	 *  Close any/all sockets/streams
	 */
	private void closeConnections() {
		try {
			if ( s != null ) {
				s.close();
			}
			if ( in != null ) {
				in.close();
			}
			if ( out != null ) {
				out.close();
			}
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
	}
}


