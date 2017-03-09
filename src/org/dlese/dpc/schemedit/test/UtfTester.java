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
package org.dlese.dpc.schemedit.test;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.vocab.FieldInfoReader;
import java.util.*;
import java.util.regex.*;
import java.net.*;
import java.io.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.util.Files;
import org.dom4j.*;
import org.dom4j.io.*;


/**
 *  Class for testing dom manipulation with help from {@link org.dlese.dpc.xml.schema.SchemaHelper}
 *
 *@author     ostwald<p>
 $Id $
 */
public class UtfTester {

	static boolean debug = true;
	DocumentFactory df = null;
	XMLWriter writer = null;
	
	UtfTester () {
		df = DocumentFactory.getInstance();
		OutputFormat format = new OutputFormat();
		try {
			writer = new XMLWriter(format);
			writer.setEscapeText (true);
		} catch (Exception e) {
			prtln ("error setting up writer: " + e.getMessage());
		}
	}
	
	static void showBytes (String path) throws Exception {
		StringBuffer buf = Files.readFile (path);
		String string = buf.toString();
		
		prtln ("contents of " + path);
		prtln (string);
		
		for (int i=0;i<string.length();i++) {
			
			String s = new Character (string.charAt(i)).toString();
			byte [] bytes = s.getBytes ("UTF-8");
			String hexString = toHexString (bytes);
			char firstChar = hexString.charAt(0);
			prtln (s + " : " + hexString + "  " + hexString.length() / 2 + " bytes, first char: " + firstChar);
			try {
				isUtf8 (s.charAt(0));
			} catch (Exception e) {
				prtln ("\t" + e.getMessage());
			}
		}
	}
	
	public static boolean isUtf8 (File file) throws Exception {
		// String content = Files.readFile (file).toString();
		String content = Files.readFileToEncoding (file, "UTF-8").toString();
		
		prtln ("content\n" + content);
		return isUtf8 (content);
	}
		
	
	public static boolean isUtf8 (String s) {
		for (int i=0;i<s.length();i++) {
			try {
				isUtf8 (s.charAt(i));
			} catch (Exception e) {
				prtln ("char at position " + i + " is not utf8: " + e.getMessage());
				return false;
			}
		}
		return true;
	}
	
	public static boolean isUtf8 (char ch) throws Exception {
		String s = new Character (ch).toString();
		byte [] bytes = s.getBytes ("UTF-8");
		// byte [] bytes = s.getBytes ();
		int bytesLen = bytes.length;
		String hexString = toHexString (bytes);
		int hexLen = hexString.length();
		int byteLen = hexString.length() / 2;
		char firstChar = hexString.charAt(0);
		
		prtln (s + " : " + hexString + "  " + byteLen + " bytes (" + bytesLen + "), first char: " + firstChar);
		
		if (byteLen == 0) {
			throw new Exception ("byteLen is zero!");
		}
		
		if (byteLen == 1) {
			if (firstChar >= '0' && firstChar <= '7')
				return true;
			else {
				throw new Exception ("illegal firstChar: " + firstChar);
			}
		}
		
		//  all bytes except first must begin with a hex code between 8 through B
		for (int i=1;i<byteLen;i++) {
			char c = hexString.charAt (i*2);
			// prtln ("\t" + i + ": " + c);
			if (c < '8' || c > 'b') {
				throw new Exception ("char at " + i + "(" + c + ") is not between 8 and B");
			}
		}
		
		if (byteLen == 2) {
			if (firstChar == 'c' || firstChar == 'd')
				return true;
			else {
				throw new Exception ("first Char is not \'c\' or \'d\'");
			}
		}

		if (byteLen == 3) {
			if (firstChar == 'e')
				return true;
			else {
				throw new Exception ("first Char is not \'e\'");
			}
		}

		if (byteLen == 4) {
			if (firstChar == 'f')
				return true;
			else {
				throw new Exception ("first Char is not \'f\'");
			}
		}
		
		return true;
	}
	
	static void showChars () {
		prtln ("----");
		prtln (new String(fromHexString ("ceb2")));
		char foo = '\u0081';
		for (int i=0;i<100;i++) {
			prtln (new Character(foo++).toString());
		}
	}
	
	public static void main (String [] args) throws Exception {
		OutputStreamWriter out = new OutputStreamWriter(new ByteArrayOutputStream());
		System.out.println("default encoding: " + out.getEncoding());	

		String path = "/home/ostwald/non-utf-frag.txt";
		path = "/devel/ostwald/records/adn/1102611887199/MyCol-000-000-000-030.xml";
		if (isUtf8 (new File (path)))
			prtln ("File is UTF8");

	}

	// Fast convert a byte array to a hex string
	// with possible leading zero.

	public static String toHexString ( byte[] b )
	   {
	   StringBuffer sb = new StringBuffer( b.length * 2 );
	   for ( int i=0; i<b.length; i++ )
		  {
		  // look up high nibble char
		  sb.append( hexChar [( b[i] & 0xf0 ) >>> 4] );
	
		  // look up low nibble char
		  sb.append( hexChar [b[i] & 0x0f] );
		  }
	   return sb.toString();
	   }
	
	// table to convert a nibble to a hex char.
	static char[] hexChar = {
	   '0' , '1' , '2' , '3' ,
	   '4' , '5' , '6' , '7' ,
	   '8' , '9' , 'a' , 'b' ,
	   'c' , 'd' , 'e' , 'f'};
	
	/**
	* Convert a hex string to a byte array.
	* Permits upper or lower case hex.
	*
	* @param s String must have even number of characters.
	* and be formed only of digits 0-9 A-F or
	* a-f. No spaces, minus or plus signs.
	* @return corresponding byte array.
	*/
	public static byte[] fromHexString ( String s )
	   {
	   int stringLength = s.length();
	   if ( (stringLength & 0x1) != 0 )
		  {
		  throw new IllegalArgumentException ( "fromHexString requires an even number of hex characters" );
		  }byte[] b = new byte[stringLength / 2];
	
	   for ( int i=0,j=0; i<stringLength; i+=2,j++ )
		  {
		  int high = charToNibble( s.charAt ( i ) );
		  int low = charToNibble( s.charAt ( i+1 ) );
		  b[j] = (byte)( ( high << 4 ) | low );
		  }
	   return b;
	   }
	
	/**
	* convert a single char to corresponding nibble.
	*
	* @param c char to convert. must be 0-9 a-f A-F, no
	* spaces, plus or minus signs.
	*
	* @return corresponding integer
	*/
	private static int charToNibble ( char c )
	   {
	   if ( '0' <= c && c <= '9' )
		  {
		  return c - '0';
		  }
	   else if ( 'a' <= c && c <= 'f' )
		  {
		  return c - 'a' + 0xa;
		  }
	   else if ( 'A' <= c && c <= 'F' )
		  {
		  return c - 'A' + 0xa;
		  }
	   else
		  {
		  throw new IllegalArgumentException ( "Invalid hex character: " + c );
		  }
	   }
	
	
	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			PrintStream out;
			try {
				out = new PrintStream (System.out, true, "UTF-8");
			} catch (Exception e) {
				prtln (e.getMessage());
				return;
			}
			out.println(s);
		}
	}
	
	static String getKeyBoardInput () {
		InputStreamReader unbuffered;
		try {
			unbuffered = new InputStreamReader( System.in, "UTF-8" );
		} catch (Exception e) {
			prtln (e.getMessage());
			return "";
		}
		BufferedReader keyboard = new BufferedReader( unbuffered );
		String inputLine = null;
		try {
			System.out.print ("type something here: ");
			inputLine = keyboard.readLine();
			if (inputLine == null ) {
				System.err.println( "No more keyboard input." );
			}
		}
		catch ( IOException error ) {
			System.err.println( "Error reading keyboard: " + error );
		}
		
		try {
			keyboard.close();
		}
		catch ( IOException error ) {
			System.err.println( "Couldn't close keyboard reader: " + error );
		}
		return inputLine;
	}
	
}

