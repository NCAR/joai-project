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
package org.dlese.dpc.schemedit.security.login;

import java.security.*;

/**
 * Utility methods for com.myjaas.auth.*. All the methods in here are static
 * so Utils should never be instantiated.
 *
 * @author Andy Armstrong, <A HREF="mailto:andy@tagish.com">andy@tagish.com</A>
 * @version 1.0.3
 */
public class Utils
{
	private final static String     ALGORITHM   = "MD5";
	private static MessageDigest    md = null;

	/**
	 * Can't make these: all the methods are static
	 */
	private Utils()
	{
	}

	/**
	 * Turn a byte array into a char array containing a printable
	 * hex representation of the bytes. Each byte in the source array
	 * contributes a pair of hex digits to the output array.
	 *
	 * @param src the source array
	 * @return a char array containing a printable version of the source
	 * data
	 */
	private static char[] hexDump(byte src[])
	{
		char buf[] = new char[src.length * 2];
		for (int b = 0; b < src.length; b++) {
			String byt = Integer.toHexString((int) src[b] & 0xFF);
			if (byt.length() < 2) {
				buf[b * 2 + 0] = '0';
				buf[b * 2 + 1] = byt.charAt(0);
			} else {
				buf[b * 2 + 0] = byt.charAt(0);
				buf[b * 2 + 1] = byt.charAt(1);
			}
		}
		return buf;
	}

	/**
	 * Zero the contents of the specified array. Typically used to
	 * erase temporary storage that has held plaintext passwords
	 * so that we don't leave them lying around in memory.
	 *
	 * @param pwd the array to zero
	 */
	public static void smudge(char pwd[])
	{
		if (null != pwd) {
			for (int b = 0; b < pwd.length; b++) {
				pwd[b] = 0;
			}
		}
	}

	/**
	 * Zero the contents of the specified array.
	 *
	 * @param pwd the array to zero
	 */
	public static void smudge(byte pwd[])
	{
		if (null != pwd) {
			for (int b = 0; b < pwd.length; b++) {
				pwd[b] = 0;
			}
		}
	}

	/**
	 * Perform MD5 hashing on the supplied password and return a char array
	 * containing the encrypted password as a printable string. The hash is
	 * computed on the low 8 bits of each character.
	 *
	 * @param pwd The password to encrypt
	 * @return a character array containing a 32 character long hex encoded
	 * MD5 hash of the password
	 */
	public static char[] cryptPassword(char pwd[]) throws Exception
	{
		if (null == md) { md = MessageDigest.getInstance(ALGORITHM); }
		md.reset();
		byte pwdb[] = new byte[pwd.length];
		for (int b = 0; b < pwd.length; b++) {
			pwdb[b] = (byte) pwd[b];
		}
		char crypt[] = hexDump(md.digest(pwdb));
		smudge(pwdb);
		return crypt;
	}
}
