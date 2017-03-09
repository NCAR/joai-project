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
package org.dlese.dpc.ndr.connection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Stack;
import java.util.Map.Entry;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;

import org.apache.axis.encoding.Base64;
import org.dlese.dpc.ndr.apiproxy.NDRAPIProxy;


public class NDRConnection {
	
	private static DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
	private File keyFile = null;
	
	private URLConnection connection;
	private HashMap<String,String> canonicalHeaderMap = new HashMap<String,String>();
	private boolean useSSL = false;
	
	private String serviceURI	= null;
	private String agentHandle  = "2200/test.20060829130238941T";
	private String privateKey 	= "";

	// request content
	private String requestContent = "";
	
	// TODO : move this to a connection management queue object
	private Stack<Exception> errorStack = new Stack<Exception>();
	
	protected NDRConnection () throws Exception
	{}
	
	public NDRConnection ( String _serviceURI ) throws Exception 
	{
		this(_serviceURI, false);
	}

	public NDRConnection ( String _serviceURI, boolean _sslFlag ) throws Exception
	{		
		try { 

			this.serviceURI = _serviceURI;
			this.connection = new URL ( serviceURI ).openConnection();
			this.useSSL = _sslFlag;
			
		} catch ( Exception e ){
			e.printStackTrace();
			errorStack.push(e);			
			throw new Exception ( "Class could not be created." );
		}
	}
	
	public File getKeyFile () {
		return keyFile;
	}
	
	public void setKeyFile (File f) {
		keyFile = f;
	}
	
	/**
	 * Get the server for this object.
	 * @return - the server for this object
	 */
	public String getServer()
	{
		return this.serviceURI;
	}
	
	/**
	 * Set the server for this object.
	 * @param _server - the server
	 */
	public void setServer ( String _server )
	{
		this.serviceURI = _server;
	}	
	
	/** 
	 * Get the NDR  agentHandle for this connection object.
	 * 
	 * @return - the agent handle for this object.=
	 */
	public String getAgentHandle()
	{
		return this.agentHandle;
	}
	
	/**
	 * Set the NDR connection agentHandle for this object.
	 * 
	 * @param _handle
	 */
	public void setAgentHandle ( String _handle )
	{
		this.agentHandle = _handle;
	}
	
	/**
	 * 
	 * @return
	 */
	public Exception getLastError()
	{
		return errorStack.pop();
	}
	
	/**
	 * Determine if the server for this connection is responding
	 * and operational.
	 *  
	 * @return - true if the connection is OK
	 */
	public boolean isConnectionOK()
	{
		if ( this.serviceURI == null )
			return false;
		else {
			try {
				URL u = new URL( this.serviceURI );
				connection = u.openConnection();
				connection.setRequestProperty("x-nsdl-date", "Tue, 04 Jun 2005 04:21:05 -0400");
				connection.setRequestProperty("x-nsdl-auth", "") ;
				connection.connect();
				return true;
			} catch ( Exception e ) {				
				errorStack.push(e);
				return false;
			}
		}
	}
		
	/**
	 * Get the private key for this connection.
	 * 
	 * @return - the {@link PrivateKey} object for this connection.
	 */
	public PrivateKey getPrivateKeyObject () 
	{
		PrivateKey privateKeyObject = null;

		try {
			KeyFactory rSAKeyFactory = KeyFactory.getInstance("RSA");
		
			// byte[] base64Key = Base64.decode(privateKey);	
			byte[] base64Key = Base64.decode(this.getPrivateKey());	
								
			try {
				privateKeyObject = rSAKeyFactory.generatePrivate(new PKCS8EncodedKeySpec(base64Key));
			} catch ( Exception e ) {
				e.printStackTrace();
				errorStack.push(e);
			}		
		} catch ( Exception e ) {
			e.printStackTrace();
			errorStack.push(e);
		}	

		return privateKeyObject;
	}
	
	/**
	 * Retrieve the URL for this connection.
	 * 
	 * @return - the URL string for this connection
	 */
	public String getConnection()
	{
		return connection.getURL().toString();
	}
	
	/**
	 * Set the connection for this object give a url.
	 * 
	 * @param url
	 */
	public void setConnection ( String _url )
	{
		try {
			connection = new URL ( _url ).openConnection();
		} catch ( Exception e ) {
			e.printStackTrace();
			errorStack.push(e);
		}
	}
	
	public void setTimeout ( int millisecs ) {
		connection.setReadTimeout(millisecs);
		connection.setConnectTimeout(millisecs);
	}
	
	/**
	 * Connect via this NDR connection.
	 *
	 */	
	public NDRAPIProxy connect() 
	{
		try {
			// prepare the NDR canonical header
			setCanonicalHeader ( false );
			
			// make the connection!
			connection.connect();
			
			return new NDRAPIProxy();
		} catch ( Exception e )
		{
			e.printStackTrace();
			errorStack.push(e);
			
			return null;
		}		
	}

	/**
	 * Set the NDR specified canonical header for this connection.  This will
	 * sign the object with the appropriate x-nsdl-auth property as required
	 * by the NDR API specification {@link http://ndr.comm.nsdl.com}.
	 * 
	 * @param signed - set the canonical x-ndsl-auth property or not
	 * @return - the string representation of the canonical header
	 */
	public void setCanonicalHeader( boolean signed )
	{			
		// collect up the existing header to complete the canonical header
		try {
			// set the nsdl date of NOW ( required )
			canonicalHeaderMap.put("x-nsdl-date", df.format ( Calendar.getInstance().getTime() ) );	
			// canonicalHeaderMap.put("x-nsdl-md5", (MessageDigest.getInstance("MD5").digest(requestContent.getBytes()).toString()) );	
			for ( Entry<String,String> e : canonicalHeaderMap.entrySet() )
				connection.setRequestProperty( e.getKey(), e.getValue() );			
			if ( signed ) {
				canonicalHeaderMap.put("x-nsdl-auth", "nsdl-1.0 " + getAgentHandle() + ":" + signHeader( getCanonicalHeader() ) );
				connection.setRequestProperty( "x-nsdl-auth", canonicalHeaderMap.get("x-nsdl-auth"));
			}
		}
		catch ( Exception e ) {			
			e.printStackTrace();
			errorStack.push(e);
		}
	}
	
	/**
	 * Get the canonical header for this connection object.
	 * 
	 * @return - the canonical header
	 */
	public String getCanonicalHeader()
	{
		String header = "";
		
		try {
			int count = canonicalHeaderMap.size() - 1; // convenient printing
			
			for ( Entry<String,String> e : canonicalHeaderMap.entrySet() )
			{		
				String key = e.getKey(), value = e.getValue();
			
				// canonical header does not include the x-nsdl-auth string
				if ( !key.equals("x-nsdl-auth") )
					header = header + key + ": " + value + ( ( count--  > 0 ) ? "\n" : "");
				else count--;
			}
		} catch ( Exception e ) {
			e.printStackTrace();
			errorStack.add(e);
		}

		return header;
	}
	
	/**
	 * Get the signed header ( canonical + x-nsdl-auth ).
	 * 
	 * @return - the signed header
	 */
	public String getSignedHeader()
	{
		String header = "";
		
		try {
			int count = canonicalHeaderMap.size(); // counter for printing

			for ( Entry<String,String> e : canonicalHeaderMap.entrySet() )
			{	
				count--;
				String key = e.getKey(), value = e.getValue(); 
				
				if ( key.equals("x-nsdl-auth") )
					header = header + key + ": nsdl-1.0  " + getAgentHandle() + ":" + value + ( ( count > 0 ) ? "\n" : "" );
				else 
					header = header + key + ": " + value;
			}			
		} catch ( Exception e ) {
			e.printStackTrace();
			errorStack.add(e);
		}
		
		return header;
	}
	
	/**
	 * Get a header value for the given key.
	 * 
	 * @param key - the key of interest
	 * @return - the value for that key
	 */
	public String getHeaderValue ( String key )
	{
		return this.canonicalHeaderMap.get(key);
	}
	
	/**
	 * Sign the current header with the correct private key for this connection.
	 * 
	 * @param header - the header string ( canonical )
	 * @return - the Base64 signed encoding for the header 
	 * @throws Exception
	 */
	private String signHeader( String _header ) throws Exception 
	{		
		Signature signature = null;
		
		try {	
			signature = Signature.getInstance( "SHA1withRSA" );
			
			signature.initSign( getPrivateKeyObject() );
			
			/* IMPORTANT!
			 * The header specified by :
			 * 
			 * POST <request_url>\n
			 * \n
			 * \n
			 * x-nsdl-date : <date>\n
			 * 
			 */
			_header = "POST " + getConnection() + "\n\n\n" +  _header + "\n";
			
			signature.update( _header.getBytes("UTF-8") );
			
			// System.out.println ( "header to be signed : [" + _header + "]" );
			
			return 
				Base64.encode( signature.sign() );
		} catch ( Exception e ) {			
			return 
				""; // NOTE : we'll let the NDR server reject the connection w/ no handle
		}
	}

	// TODO 
	public void setContent( String _requestContent )
	{
		this.requestContent = _requestContent;
	}
	
	// TODO 
	public String getContent()
	{
		return this.requestContent;
	}

	public String requestGET( String _content )
	{
		return requestGET( _content, false );
	}
	//	 TODO 
	public String requestGET( String _content, boolean _withParameters )
	{
		String response = "";
		
		try {
	        connection.setDoOutput( true );
	        connection.setDoInput(true);
	        
	        ((HttpURLConnection)connection).setRequestMethod("GET");
	        
	        if ( _withParameters )
	        	setConnection( getConnection() + _content );
	        else 
	        	setConnection( getConnection() + "/" + _content );
	        	
	        
	        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	        String line;
	        while ((line = rd.readLine()) != null) {
	            response = response + line;
	        }
	        
	        rd.close();     		        

		} catch ( Exception e )
		{
			e.printStackTrace();
			errorStack.push(e);
		}
		
		return response;
	}
	
	// TODO 
	public String request () throws Exception
	{
		if ( this.requestContent != null ) {
			return request ( this.requestContent );
		}
		else {
			return null;
		}
	}
	// TODO 
	public String request( String _content ) throws Exception
	{
		// prtln ("request: " + _content);
		String response = "";
		
		try {
	        connection.setDoOutput( true );
	        connection.setDoInput(true);
	        
	        ((HttpURLConnection)connection).setRequestMethod("POST");
			
			// specify timeout in milliseconds
	        // connection.setReadTimeout(1000);
	                	        
	        OutputStreamWriter wr = new OutputStreamWriter( connection.getOutputStream(), "UTF-8" );
	        wr.write( _content );
	        wr.close();
	      
	        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	        
	        String line;
	        while (( line = rd.readLine()) != null) {
	            response = response + line;
	        }
	        
	        rd.close();     		        
		} catch ( Exception e ) {
			if (e instanceof java.net.SocketTimeoutException)
				prtln ("timeout interval: " + this.connection.getConnectTimeout() + " milliseconds");
			// e.printStackTrace();
			errorStack.push(e);
			throw e;
		}
		
		return response;
	}
	
	static void prtln (String s) {
		org.dlese.dpc.ndr.NdrUtils.prtln (s, "NDRConnection");
	}
	
	private String getPrivateKey () throws Exception {
		// prtln ("getPrivateKey()");
		String key = "";
		if (keyFile == null) {
			throw new Exception ("private key not initialized");
		}
		else if (keyFile.exists()) {
			// prtln (" ... keyFile: " + keyFile);
			BufferedReader keyfileReader = 
				new BufferedReader (
					new FileReader ( keyFile ) 
				);
		
			String input = "";
			while ( ( input = keyfileReader.readLine() ) != null )
			{
				// skip lines beginning with a dash
				if (input.length() > 0 && input.charAt(0) == '-')
					continue;
				if ( key.length() == 0 )
					key = input;
				else 
					key = key + "\n" +  input ;
			}
		}
 		else {
/* 			if (keyFile == null)
				prtln ("keyFile is null");
			else
				prtln ("keyFile does not exist at " + keyFile); */
			throw new Exception ("ndr Private key file not found at \"" + keyFile + "\"");
		}
		return key;
	}
}
