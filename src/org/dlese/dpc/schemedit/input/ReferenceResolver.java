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
package org.dlese.dpc.schemedit.input;
import java.util.*;
import java.text.ParseException;
import java.util.regex.*;

/**
 *  Resolves numeric and character references (e.g, &#x009; or &delta;) into
 *  their unicode representations.
 *
 *@author    ostwald<p>
 *
 *      $Id $
 */
public class ReferenceResolver {

	static boolean debug = true;
	/**
	 *  Description of the Field
	 */

	public static Map charEntityMap;
	Pattern entityRefPattern = Pattern.compile("&([#0-9a-zA-Z]+?);");
	Pattern numericRefPattern = Pattern.compile("&#([x0-9]+?);");


	/**
	 *  Constructor for the ReferenceResolver object
	 *
	 */
	public ReferenceResolver() {

		charEntityMap = getCharEntityMap();

	}


	/**
	 *  Returned by ReferenceResolver.resolve storing the resolved input string and
	 *  a list of errors describing references that could not be resolved.
	 *
	 *@author    ostwald
	 */
	public class ResolverResults {
		/**
		 *  Description of the Field
		 */
		public List errors;
		/**
		 *  Description of the Field
		 */
		public String content;


		/**
		 *  Constructor for the ResolverResults object
		 *
		 *@param  content  Description of the Parameter
		 *@param  errors   Description of the Parameter
		 */
		public ResolverResults(String content, List errors) {
			this.content = content;
			this.errors = errors;
		}
	}


	/**
	 *  Resolves a string by replacing numeric (e.g., &#169;) and character
	 *  references (e.g., &delta;) with thier equivalent unicode representation.
	 *  Returns results as a ResolverResult instance, which contains the resolved
	 *  string, as well as errors encountered, if any
	 *
	 *@param  in  Description of the Parameter
	 *@return     Description of the Return Value
	 */
	public ResolverResults resolve(String in) {
		in = unescapeAmpersands(in);
		StringBuffer outBuf = new StringBuffer();
		int ind1 = 0;
		Matcher m = entityRefPattern.matcher(in);
		List refs = new ArrayList();
		List errors = new ArrayList();

		while (m.find()) {
			String ref = m.group(1);
			refs.add(ref);
			int ind2 = m.start();

			// append the part of in upto ref
			outBuf.append(in.substring(ind1, ind2));

			// append resolved ref
			try {
				outBuf.append(resolveRef(ref));
			} catch (IllegalArgumentException iae) {
				errors.add(new ReferenceException(m.group(), iae.getMessage(), outBuf.length()));
				outBuf.append(m.group());
			}

			// process remainder of in
			in = in.substring(ind2 + m.group().length());
			ind1 = 0;
			m = entityRefPattern.matcher(in);
		}
		// we're done - append remainder of in
		outBuf.append(in.substring(ind1));

		/* 		prtln("refs found");
		for (Iterator i = rr.refs.iterator(); i.hasNext(); ) {
			prtln("\t" + (String) i.next());
		} */
		return new ResolverResults(outBuf.toString(), errors);
	}


	/**
	 *  Escape all escaped ampersands (&amp;) as a preprocessing step in resolution
	 *  of entity references. For example, "&amp;#169;" and "&amp;amp;#169;" both
	 *  become "&#169". Even "&amp;" is contracted.
	 *
	 *@param  in  String to be processed
	 *@return     processed string
	 */
	public static String unescapeAmpersands(String in) {
		// prtln ("unescapeExtraAmpersands");
		Pattern p = Pattern.compile("&amp;");
		StringBuffer ret = new StringBuffer();
		int ind1 = 0;
		Matcher m = p.matcher(in);

		while (m.find()) {
			int ind2 = m.start();

			ret.append(in.substring(ind1, ind2));

			in = "&" + in.substring(ind2 + m.group().length());
			ind1 = 0;
			m = p.matcher(in);
		}
		ret.append(in.substring(ind1));
		return ret.toString();
	}


	/**
	 *  Convert a single character or numeric reference (with leading & and
	 *  trailing ; stripped) to an equivalent unicode string.
	 *
	 *@param  ref                           A numerical or character reference
	 *@return                               String representation of unicode
	 *      equivalent
	 *@exception  IllegalArgumentException  If unicode equivalent cannot be
	 *      derived.
	 */
	String resolveRef(String ref)
		throws IllegalArgumentException {
		if (ref.charAt(0) == '#') {
			return toUnicode(ref.substring(1));
		}
		else {
			return resolveCharacterRef(ref);
		}
	}


	/**
	 *  Converts HTML character entity references to their equivalent unicode
	 *  representation.<p>
	 *
	 *  See http://www.w3.org/TR/REC-html40/sgml/entities.html.
	 *
	 *@param  ref                           character reference (With leading & and
	 *      trailing ; stripped)
	 *@return                               String representation of unicode
	 *      equivalent.
	 *@exception  IllegalArgumentException  If given reference cannot be mapped to
	 *      a numeric equivalent.
	 */
	String resolveCharacterRef(String ref)
		throws IllegalArgumentException {
		String characterRef = "&" + ref + ";";
		if (ref.equals("lt") ||
				ref.equals("gt") ||
				ref.equals("amp") ||
				ref.equals("quot") ||
				ref.equals("apos")) {
			return characterRef;
		}

		String numericRef = (String) getCharEntityMap().get(characterRef);

		if (numericRef == null) {
			throw new IllegalArgumentException("unknown character reference");
		}
		else {
			Matcher m = numericRefPattern.matcher(numericRef);
			if (m.find()) {
				return toUnicode(m.group(1));
			}
			else {
				throw new IllegalArgumentException("ill-formed numerical reference found in charEntityMap");
			}
		}

	}



	/**
	 *  The main program for the ReferenceResolver class
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  Description of the Exception
	 */
	public static void main(String[] args)
		throws Exception {

		String in = "here is a decimal numerical (&amp;amp;#169;) a hex numeral (&#x00f1;) and a character (&#xfred;) and another one (&Copy;)";
		prtln("in:\n\t" + in);
		ReferenceResolver rr = new ReferenceResolver();

		ResolverResults results = rr.resolve(in);
		prtln("out:\n\t" + results.content);

		List errors = results.errors;
		if (errors.size() > 0) {
			prtln(errors.size() + " parsing errors found");
			for (Iterator i = errors.iterator(); i.hasNext(); ) {
				/* 				ParseException error = (ParseException) i.next();
				prtln("\t" + error.getMessage() + " could not be resolved (at offset " + error.getErrorOffset() + ")"); */
				ReferenceException re = (ReferenceException) i.next();
				prtln("\t" + re.getErrorMessage());
			}
		}
		// toUnicodeTester ();

	}



	/**
	 *  Converts a numeric character reference in either decimal (e.g., "&#169;")
	 *  or hex (e.g., "&x00a9") into an equivalent unicode representation
	 *
	 *@param  s                             Numeric reference (with leading & and
	 *      trailing ; stripped)
	 *@return                               Equivelant unicode representation
	 *@exception  IllegalArgumentException  If given ref cannot be converted to
	 *      unicode
	 */
	static String toUnicode(String s)
		throws IllegalArgumentException {
		int codePoint = 0;
		boolean diagnostics = false;
		try {
			if (s.charAt(0) == 'x') {
				codePoint = Integer.valueOf(s.substring(1), 16).intValue();
				if (diagnostics) {
					prtln("dec equiv: " + Integer.toString(codePoint));
				}
			}
			else {
				codePoint = Integer.valueOf(s).intValue();
				if (diagnostics) {
					prtln("hex equiv: " + Integer.toString(codePoint, 16));
				}
			}

			char codePointChar = (char) codePoint;
			
			// in 1.4.2 isDefined requires a character!
 			if (!Character.isDefined(codePointChar)) {
				// throw new Exception("unicode character undefined at code point (" + codePoint + ")");
				throw new Exception("reference to undefined unicode character");
			}			
			
			return new Character(codePointChar).toString();
		} catch (NumberFormatException nfe) {
			// throw new IllegalArgumentException("Unable to parse input into codePoint: " + nfe.toString());
			throw new IllegalArgumentException("illegal numerical reference");
		} catch (IllegalArgumentException iae) {
			throw iae;
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}


	/**
	 *  Gets a mapping from HTML 4 character entity references to their numeric
	 *  equivalents (see http://www.w3.org/TR/REC-html40/sgml/entities.html#h-24.3.1)
	 *
	 *@return    The charEntityMap value
	 */
	public static Map getCharEntityMap() {
		if (charEntityMap == null) {
			charEntityMap = new HashMap();
			charEntityMap.put("&diams;", "&#9830;");
			charEntityMap.put("&int;", "&#8747;");
			charEntityMap.put("&Ecirc;", "&#202;");
			charEntityMap.put("&rlm;", "&#8207;");
			charEntityMap.put("&Theta;", "&#920;");
			charEntityMap.put("&Eta;", "&#919;");
			charEntityMap.put("&lArr;", "&#8656;");
			charEntityMap.put("&Lambda;", "&#923;");
			charEntityMap.put("&empty;", "&#8709;");
			charEntityMap.put("&raquo;", "&#187;");
			charEntityMap.put("&eth;", "&#240;");
			charEntityMap.put("&zeta;", "&#950;");
			charEntityMap.put("&sum;", "&#8721;");
			charEntityMap.put("&Phi;", "&#934;");
			charEntityMap.put("&divide;", "&#247;");
			charEntityMap.put("&Sigma;", "&#931;");
			charEntityMap.put("&atilde;", "&#227;");
			charEntityMap.put("&sup1;", "&#185;");
			charEntityMap.put("&THORN;", "&#222;");
			charEntityMap.put("&ETH;", "&#208;");
			charEntityMap.put("&frasl;", "&#8260;");
			charEntityMap.put("&nbsp;", "&#160;");
			charEntityMap.put("&Gamma;", "&#915;");
			charEntityMap.put("&Auml;", "&#196;");
			charEntityMap.put("&Ouml;", "&#214;");
			charEntityMap.put("&Nu;", "&#925;");
			charEntityMap.put("&Epsilon;", "&#917;");
			charEntityMap.put("&Egrave;", "&#200;");
			charEntityMap.put("&acute;", "&#180;");
			charEntityMap.put("&supe;", "&#8839;");
			charEntityMap.put("&Icirc;", "&#206;");
			charEntityMap.put("&deg;", "&#176;");
			charEntityMap.put("&middot;", "&#183;");
			charEntityMap.put("&upsilon;", "&#965;");
			charEntityMap.put("&ocirc;", "&#244;");
			charEntityMap.put("&Ugrave;", "&#217;");
			charEntityMap.put("&larr;", "&#8592;");
			charEntityMap.put("&plusmn;", "&#177;");
			charEntityMap.put("&exist;", "&#8707;");
			charEntityMap.put("&rsquo;", "&#8217;");
			charEntityMap.put("&psi;", "&#968;");
			charEntityMap.put("&micro;", "&#181;");
			charEntityMap.put("&gt;", "&#62;");
			charEntityMap.put("&ordf;", "&#170;");
			charEntityMap.put("&lambda;", "&#955;");
			charEntityMap.put("&lowast;", "&#8727;");
			charEntityMap.put("&ne;", "&#8800;");
			charEntityMap.put("&sigmaf;", "&#962;");
			charEntityMap.put("&Kappa;", "&#922;");
			charEntityMap.put("&thetasym;", "&#977;");
			charEntityMap.put("&ge;", "&#8805;");
			charEntityMap.put("&uml;", "&#168;");
			charEntityMap.put("&aring;", "&#229;");
			charEntityMap.put("&Psi;", "&#936;");
			charEntityMap.put("&frac12;", "&#189;");
			charEntityMap.put("&circ;", "&#710;");
			charEntityMap.put("&oline;", "&#8254;");
			charEntityMap.put("&sub;", "&#8834;");
			charEntityMap.put("&iexcl;", "&#161;");
			charEntityMap.put("&Yuml;", "&#376;");
			charEntityMap.put("&Aacute;", "&#193;");
			charEntityMap.put("&szlig;", "&#223;");
			charEntityMap.put("&sim;", "&#8764;");
			charEntityMap.put("&hearts;", "&#9829;");
			charEntityMap.put("&trade;", "&#8482;");
			charEntityMap.put("&igrave;", "&#236;");
			charEntityMap.put("&aelig;", "&#230;");
			charEntityMap.put("&Omega;", "&#937;");
			charEntityMap.put("&there4;", "&#8756;");
			charEntityMap.put("&asymp;", "&#8776;");
			charEntityMap.put("&uarr;", "&#8593;");
			charEntityMap.put("&gamma;", "&#947;");
			charEntityMap.put("&yen;", "&#165;");
			charEntityMap.put("&Rho;", "&#929;");
			charEntityMap.put("&times;", "&#215;");
			charEntityMap.put("&rceil;", "&#8969;");
			charEntityMap.put("&epsilon;", "&#949;");
			charEntityMap.put("&Prime;", "&#8243;");
			charEntityMap.put("&Xi;", "&#926;");
			charEntityMap.put("&egrave;", "&#232;");
			charEntityMap.put("&Atilde;", "&#195;");
			charEntityMap.put("&kappa;", "&#954;");
			charEntityMap.put("&iota;", "&#953;");
			charEntityMap.put("&hellip;", "&#8230;");
			charEntityMap.put("&Igrave;", "&#204;");
			charEntityMap.put("&hArr;", "&#8660;");
			charEntityMap.put("&omega;", "&#969;");
			charEntityMap.put("&ucirc;", "&#251;");
			charEntityMap.put("&bull;", "&#8226;");
			charEntityMap.put("&brvbar;", "&#166;");
			charEntityMap.put("&sigma;", "&#963;");
			charEntityMap.put("&fnof;", "&#402;");
			charEntityMap.put("&rang;", "&#9002;");
			charEntityMap.put("&uArr;", "&#8657;");
			charEntityMap.put("&agrave;", "&#224;");
			charEntityMap.put("&sup;", "&#8835;");
			charEntityMap.put("&notin;", "&#8713;");
			charEntityMap.put("&prod;", "&#8719;");
			charEntityMap.put("&thorn;", "&#254;");
			charEntityMap.put("&rsaquo;", "&#8250;");
			charEntityMap.put("&ensp;", "&#8194;");
			charEntityMap.put("&cong;", "&#8773;");
			charEntityMap.put("&Ucirc;", "&#219;");
			charEntityMap.put("&lceil;", "&#8968;");
			charEntityMap.put("&rfloor;", "&#8971;");
			charEntityMap.put("&lsaquo;", "&#8249;");
			charEntityMap.put("&amp;", "&#38;");
			charEntityMap.put("&uuml;", "&#252;");
			charEntityMap.put("&Scaron;", "&#352;");
			charEntityMap.put("&yuml;", "&#255;");
			charEntityMap.put("&harr;", "&#8596;");
			charEntityMap.put("&ecirc;", "&#234;");
			charEntityMap.put("&rho;", "&#961;");
			charEntityMap.put("&theta;", "&#952;");
			charEntityMap.put("&oplus;", "&#8853;");
			charEntityMap.put("&laquo;", "&#171;");
			charEntityMap.put("&infin;", "&#8734;");
			charEntityMap.put("&dagger;", "&#8224;");
			charEntityMap.put("&not;", "&#172;");
			charEntityMap.put("&sdot;", "&#8901;");
			charEntityMap.put("&zwnj;", "&#8204;");
			charEntityMap.put("&Ograve;", "&#210;");
			charEntityMap.put("&oslash;", "&#248;");
			charEntityMap.put("&yacute;", "&#253;");
			charEntityMap.put("&frac14;", "&#188;");
			charEntityMap.put("&permil;", "&#8240;");
			charEntityMap.put("&perp;", "&#8869;");
			charEntityMap.put("&chi;", "&#967;");
			charEntityMap.put("&lfloor;", "&#8970;");
			charEntityMap.put("&cedil;", "&#184;");
			charEntityMap.put("&emsp;", "&#8195;");
			charEntityMap.put("&piv;", "&#982;");
			charEntityMap.put("&AElig;", "&#198;");
			charEntityMap.put("&loz;", "&#9674;");
			charEntityMap.put("&icirc;", "&#238;");
			charEntityMap.put("&alpha;", "&#945;");
			charEntityMap.put("&auml;", "&#228;");
			charEntityMap.put("&ouml;", "&#246;");
			charEntityMap.put("&Ccedil;", "&#199;");
			charEntityMap.put("&cup;", "&#8746;");
			charEntityMap.put("&spades;", "&#9824;");
			charEntityMap.put("&OElig;", "&#338;");
			charEntityMap.put("&mu;", "&#956;");
			charEntityMap.put("&euml;", "&#235;");
			charEntityMap.put("&frac34;", "&#190;");
			charEntityMap.put("&Delta;", "&#916;");
			charEntityMap.put("&pi;", "&#960;");
			charEntityMap.put("&lt;", "&#60;");
			charEntityMap.put("&phi;", "&#966;");
			charEntityMap.put("&isin;", "&#8712;");
			charEntityMap.put("&iquest;", "&#191;");
			charEntityMap.put("&equiv;", "&#8801;");
			charEntityMap.put("&scaron;", "&#353;");
			charEntityMap.put("&eacute;", "&#233;");
			charEntityMap.put("&ntilde;", "&#241;");
			charEntityMap.put("&le;", "&#8804;");
			charEntityMap.put("&clubs;", "&#9827;");
			charEntityMap.put("&pound;", "&#163;");
			charEntityMap.put("&upsih;", "&#978;");
			charEntityMap.put("&ni;", "&#8715;");
			charEntityMap.put("&otimes;", "&#8855;");
			charEntityMap.put("&zwj;", "&#8205;");
			charEntityMap.put("&sbquo;", "&#8218;");
			charEntityMap.put("&Iuml;", "&#207;");
			charEntityMap.put("&crarr;", "&#8629;");
			charEntityMap.put("&and;", "&#8743;");
			charEntityMap.put("&rArr;", "&#8658;");
			charEntityMap.put("&lsquo;", "&#8216;");
			charEntityMap.put("&Eacute;", "&#201;");
			charEntityMap.put("&Ntilde;", "&#209;");
			charEntityMap.put("&ndash;", "&#8211;");
			charEntityMap.put("&euro;", "&#8364;");
			charEntityMap.put("&rdquo;", "&#8221;");
			charEntityMap.put("&delta;", "&#948;");
			charEntityMap.put("&Iota;", "&#921;");
			charEntityMap.put("&cap;", "&#8745;");
			charEntityMap.put("&sube;", "&#8838;");
			charEntityMap.put("&real;", "&#8476;");
			charEntityMap.put("&sup2;", "&#178;");
			charEntityMap.put("&dArr;", "&#8659;");
			charEntityMap.put("&Chi;", "&#935;");
			charEntityMap.put("&radic;", "&#8730;");
			charEntityMap.put("&tau;", "&#964;");
			charEntityMap.put("&image;", "&#8465;");
			charEntityMap.put("&Acirc;", "&#194;");
			charEntityMap.put("&ccedil;", "&#231;");
			charEntityMap.put("&Zeta;", "&#918;");
			charEntityMap.put("&prop;", "&#8733;");
			charEntityMap.put("&lrm;", "&#8206;");
			charEntityMap.put("&tilde;", "&#732;");
			charEntityMap.put("&nabla;", "&#8711;");
			charEntityMap.put("&forall;", "&#8704;");
			charEntityMap.put("&Iacute;", "&#205;");
			charEntityMap.put("&Dagger;", "&#8225;");
			charEntityMap.put("&ang;", "&#8736;");
			charEntityMap.put("&quot;", "&#34;");
			charEntityMap.put("&Aring;", "&#197;");
			charEntityMap.put("&darr;", "&#8595;");
			charEntityMap.put("&macr;", "&#175;");
			charEntityMap.put("&ordm;", "&#186;");
			charEntityMap.put("&Tau;", "&#932;");
			charEntityMap.put("&Oslash;", "&#216;");
			charEntityMap.put("&Otilde;", "&#213;");
			charEntityMap.put("&alefsym;", "&#8501;");
			charEntityMap.put("&part;", "&#8706;");
			charEntityMap.put("&xi;", "&#958;");
			charEntityMap.put("&Ocirc;", "&#212;");
			charEntityMap.put("&shy;", "&#173;");
			charEntityMap.put("&reg;", "&#174;");
			charEntityMap.put("&Yacute;", "&#221;");
			charEntityMap.put("&omicron;", "&#959;");
			charEntityMap.put("&weierp;", "&#8472;");
			charEntityMap.put("&eta;", "&#951;");
			charEntityMap.put("&nu;", "&#957;");
			charEntityMap.put("&Beta;", "&#914;");
			charEntityMap.put("&iuml;", "&#239;");
			charEntityMap.put("&ugrave;", "&#249;");
			charEntityMap.put("&sup3;", "&#179;");
			charEntityMap.put("&curren;", "&#164;");
			charEntityMap.put("&copy;", "&#169;");
			charEntityMap.put("&ldquo;", "&#8220;");
			charEntityMap.put("&oacute;", "&#243;");
			charEntityMap.put("&para;", "&#182;");
			charEntityMap.put("&Omicron;", "&#927;");
			charEntityMap.put("&Euml;", "&#203;");
			charEntityMap.put("&uacute;", "&#250;");
			charEntityMap.put("&Pi;", "&#928;");
			charEntityMap.put("&lang;", "&#9001;");
			charEntityMap.put("&Alpha;", "&#913;");
			charEntityMap.put("&ograve;", "&#242;");
			charEntityMap.put("&nsub;", "&#8836;");
			charEntityMap.put("&acirc;", "&#226;");
			charEntityMap.put("&prime;", "&#8242;");
			charEntityMap.put("&or;", "&#8744;");
			charEntityMap.put("&aacute;", "&#225;");
			charEntityMap.put("&Agrave;", "&#192;");
			charEntityMap.put("&Oacute;", "&#211;");
			charEntityMap.put("&thinsp;", "&#8201;");
			charEntityMap.put("&sect;", "&#167;");
			charEntityMap.put("&Uuml;", "&#220;");
			charEntityMap.put("&oelig;", "&#339;");
			charEntityMap.put("&Mu;", "&#924;");
			charEntityMap.put("&iacute;", "&#237;");
			charEntityMap.put("&cent;", "&#162;");
			charEntityMap.put("&Upsilon;", "&#933;");
			charEntityMap.put("&Uacute;", "&#218;");
			charEntityMap.put("&mdash;", "&#8212;");
			charEntityMap.put("&minus;", "&#8722;");
			charEntityMap.put("&bdquo;", "&#8222;");
			charEntityMap.put("&otilde;", "&#245;");
			charEntityMap.put("&beta;", "&#946;");
			charEntityMap.put("&rarr;", "&#8594;");
			charEntityMap.put("&apos;", "&#39;");  // added 10/10/08

		}

		return charEntityMap;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}



	/**
	 *  Examples of various conversion methods
	 *
	 *@param  input  Description of the Parameter
	 */
	static void conversions(String input) {
		prtln("hexInt: " + Integer.valueOf(input, 16));
		prtln("octalInt: " + Integer.valueOf(input, 8));
		prtln("dec Int: " + Integer.valueOf(input));
		prtln("decInt: " + Integer.valueOf(input, 10));
		prtln("binaryInt: " + Integer.valueOf(input, 2));

		int decInt = Integer.valueOf(input).intValue();
		prtln("hex to decimal " + decInt);
		prtln("hex string: " + Integer.toHexString(decInt));
		prtln("octal string: " + Integer.toOctalString(decInt));
		prtln("binary string: " + Integer.toBinaryString(decInt));
		Character copyright = new Character ('\u00a9');
		// unicod for decimal 169
		prtln("what is this? " + copyright);
		prtln("what is this? " + '\251');
		// rr.xmlTest();

		char c = new Character('\251').charValue();
		prtln("char for digit: " + c);

		String foo = "here is a copyright: " + '\u00a9' + ", right?";
		prtln(foo);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  input  Description of the Parameter
	 */
	static void toUnicodeTester(String input) {

		String unicode = "";
		try {
			unicode = toUnicode(input);
			prtln("you typed the codepoint for " + unicode + ", right?\n");
		} catch (Exception e) {
			prtln("toUnicode error: " + e.getMessage() + "\n");
		}
	}
}


