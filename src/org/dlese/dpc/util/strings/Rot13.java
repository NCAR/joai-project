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
package org.dlese.dpc.util.strings;

/**
 *  Basic Rot13 implementation. Rot13 is a two-way encryption algorithm, so the
 *  same method is used for encrypting and decrypting.
 *
 * @author    Ryan Deardorff
 */
public class Rot13 {
	/**
	 *  Return an encrypted/decrypted version of the given string
	 *
	 * @param  str
	 * @return
	 */
	public static String crypt( String str ) {
		StringBuffer ret = new StringBuffer();
		for ( int i = 0; i < str.length(); i++ ) {
			char c = str.charAt( i );
			if ( c >= 'a' && c <= 'm' ) {
				c += 13;
			}
			else if ( c >= 'n' && c <= 'z' ) {
				c -= 13;
			}
			else if ( c >= 'A' && c <= 'M' ) {
				c += 13;
			}
			else if ( c >= 'A' && c <= 'Z' ) {
				c -= 13;
			}
			ret.append( c );
		}
		return ret.toString();
	}
}

