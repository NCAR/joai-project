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
package org.dlese.dpc.services.idmapper;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.StringWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.URL;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import org.dlese.dpc.util.DpcErrors;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;

import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;


/**
 *  Handles the retrieval and analysis of a single page. Cannot use ordinary
 *  java.net.URLConnection here, because lame java URLConnection does not allow us to set
 *  the timeout value. It just hangs forever.
 *
 * @author    Sonal Bhushan
 */

class PageDesc {

	private final static int MAXCONTENTLENGTH = 50 * 1024;

	/**  DESCRIPTION */
	public final static int CKSUMTYPE_EXACT = 1;
	/**  DESCRIPTION */
	public final static int CKSUMTYPE_STD = 2;

	ResourceDesc rsd;// The resource ID

	String xpath;// The xpath in the metadata XML record.
	String urllabel;// label describing the url
	String urlstg;// The URL
	int cksumtype;// CKSUMTYPE_EXACT or CKSUMTYPE_STD

	int respcode;// our response code: one of DpcErrors.IDMAP_xxx.
	double resptime;// Time to retrieve page, in seconds.
	Warning pagewarning;
	HashMap hdrmap;// String name -> String value
	String hdrstg;// concat of all header lines
	byte[] contentbuf;// page content

	long pageChecksum = 0;// checksum
	String ftpline;

	InputStream input;
	ScanThread scanThread;

	String pagesummary = null;// extracted summary of the page
	boolean isPrimaryPage;// if this page is the Primary URL page or not
	String PrimaryContent = null;
	String primarycontentType = null;


	/**
	 * @param  urlstg     The URL
	 * @param  rsd        DESCRIPTION
	 * @param  xpath      DESCRIPTION
	 * @param  urllabel   DESCRIPTION
	 * @param  cksumtype  DESCRIPTION
	 */

	PageDesc(
	         ResourceDesc rsd,
	         String xpath,
	         String urllabel,
	         String urlstg,
	         int cksumtype) {
		this.rsd = rsd;
		this.xpath = xpath;
		this.urllabel = urllabel;
		this.urlstg = urlstg;
		this.cksumtype = cksumtype;
		this.isPrimaryPage = false;

	}



	/**
	 *  DESCRIPTION
	 *
	 * @return    DESCRIPTION
	 */
	public String toString() {
		String res = "page: xpath: \"" + xpath + "\"\n";
		res += "  urllabel: \"" + urllabel + "\"\n";
		res += "  urlstg: \"" + urlstg + "\"\n";
		res += "  respcode: " + respcode
			 + " \"" + DpcErrors.getMessage(respcode) + "\"\n";
		res += "  resptime: " + resptime + "\n";
		res += "  pagewarning: " + pagewarning + "\n";
		if (hdrstg == null)
			res += "  hdrstg: null\n";
		else
			res += "  hdrstg len: " + hdrstg.length() + "\n";
		if (contentbuf == null)
			res += "  contentbuf: null\n";
		else
			res += "  contentbuf len: " + contentbuf.length + "\n";
		res += "  pageChecksum: " + pageChecksum;
		return res;
	}


	/**
	 *  This will parse a PDF document.
	 *
	 * @param  input         The input stream for the document.
	 * @return               The document.
	 * @throws  IOException  If there is an error parsing the document.
	 */
	private static PDDocument parseDocument(InputStream input) throws IOException {
		PDFParser parser = new PDFParser(input);
		parser.parse();
		return parser.getPDDocument();
	}



	/**
	 *  Retrieves the page and sets pageChecksum. If any error occurs, including any response
	 *  code other than 200, sets errmsg.
	 *
	 * @param  bugs            debug level
	 * @param  threadId        The index number of the thread we are running in. Used for
	 *      debug messages only.
	 * @param  timeoutSeconds  timeout value for sockets, in seconds
	 */

	void processPage(
	                 int bugs,
	                 int threadId,
	                 int timeoutSeconds) {
		respcode = 0;
		pageChecksum = 0;
		hdrmap = new HashMap();


		if (bugs >= 50)
			prtln("processPage: start page: \"" + urlstg + "\"");
		Exception bugexc = null;
		try {

			this.PrimaryContent = null;
			URL testurl = new URL(urlstg);
			if ((testurl.getProtocol().equals("http")) || (testurl.getProtocol().equals("https")))
				getHttpContent(bugs, timeoutSeconds, urlstg);
			else if (testurl.getProtocol().equals("ftp"))
				getFtpContent(bugs, timeoutSeconds, urlstg);
			else
				throwResponse(DpcErrors.IDMAP_UNKNOWN_PROTOCOL, null, null);

			// If this was HTTP with text/html, we extract a customized summary.
			// Otherwise we just use the contentbuf as is.
			String contentType = (String) hdrmap.get("content-type");
			this.primarycontentType = contentType;
			
			int indexst = -1;
			if (contentType != null)
				indexst = contentType.indexOf("html");

			if ((((urlstg.startsWith("http://")) || ((urlstg.startsWith("https://"))))) && (contentType != null) && (indexst != -1)) {
				if (cksumtype == CKSUMTYPE_EXACT)
					pagesummary = new String(contentbuf);
				else if (cksumtype == CKSUMTYPE_STD)
					pagesummary = extractSummary(bugs, new String(contentbuf), urlstg);
				else
					throwResponse(DpcErrors.IDMAP_MISC,
						"PageDesc: invalid cksumtype", "" + cksumtype);
			}

			if ((rsd.testPrimary() == true) && this.urllabel.equals("primary-url")) {// there IS a primary URL

				this.isPrimaryPage = true;

				if ((((urlstg.startsWith("http://")) || ((urlstg.startsWith("https://"))))) && (contentType != null) && (indexst != -1)) {
					if (contentbuf != null)
						this.PrimaryContent = new String(contentbuf);
					else
						this.PrimaryContent = null;
				}
			}

			if (contentbuf != null) {
				// Calc CRC, even if we have meta refresh tag.
				CRC32 crcobj = new CRC32();

				//crcobj.update(pagesummary.getBytes());
				crcobj.update(contentbuf);

				pageChecksum = crcobj.getValue();
			}
			else
				pageChecksum = 0;
			
			/* quick and dirty to fix false dups because of FRAMESET elements */
			if ((this.PrimaryContent != null) && (indexst != -1)){
				if ((this.PrimaryContent.indexOf("frameset") != -1 ) || (this.PrimaryContent.indexOf("FRAMESET") != -1))
				{
					pageChecksum = 0;
				}
				
			}
			
			//System.out.println("Checksum is " + pageChecksum);
			respcode = DpcErrors.IDMAP_OK;// finally
		} // try
		catch (PageDescException exc) {
			bugexc = exc;
			pagewarning = new Warning(exc.respcode, rsd.getId(),
				rsd.getFileName(), xpath, urllabel, urlstg,
				exc.message, exc.auxinfo);
		} catch (SocketTimeoutException exc) {
			bugexc = exc;
			pagewarning = new Warning(DpcErrors.IDMAP_TIMEOUT, rsd.getId(),
				rsd.getFileName(), xpath, urllabel, urlstg,
				exc.getMessage(), null);
		} catch (IOException exc) {
			bugexc = exc;
			if (exc instanceof ConnectException) {
				pagewarning = new Warning(
					DpcErrors.IDMAP_CONNECT_REFUSED, rsd.getId(),
					rsd.getFileName(), xpath, urllabel, urlstg,
					exc.getMessage(), null);
			}
			else if (exc instanceof UnknownHostException) {
				pagewarning = new Warning(
					DpcErrors.IDMAP_UNKNOWN_HOST, rsd.getId(),
					rsd.getFileName(), xpath, urllabel, urlstg,
					exc.getMessage(), null);
			}
			else {
				pagewarning = new Warning(
					DpcErrors.IDMAP_MISC, rsd.getId(),
					rsd.getFileName(), xpath, urllabel, urlstg,
					exc.getMessage(), null);
			}
		}

		if (bugs >= 50 && bugexc != null) {
			prtln("processPage: caught: " + bugexc);
			prtln("    for urlstg: \"" + urlstg + "\"");
			bugexc.printStackTrace(System.out);
		}


		hdrstg = null;// free memory
		contentbuf = null;// free memory
		pagesummary = null;// free memory
		System.gc();

		
		
		if (bugs >= 50)
			prtln("processPage: final page: " + this);
	}// end processPage






	/**
	 *  Calls {@link #getSingleHttpContent getSingleHttpContent} to retrieve the content of a
	 *  page into contentbuf. If the page has been redirected (302 or 307), again calls
	 *  getSingleHttpContent with the new location.
	 *
	 * @param  bugs                   debug level
	 * @param  timeoutSeconds         timeout value for sockets, in seconds
	 * @param  urlparm                The URL to retrieve.
	 * @exception  PageDescException  DESCRIPTION
	 * @exception  IOException        DESCRIPTION
	 */

	void getHttpContent(
	                    int bugs,
	                    int timeoutSeconds,
	                    String urlparm)
		 throws PageDescException, IOException {
		
		int iredir;
		int httpResponse = 0;
		long timestart = System.currentTimeMillis();
		try {
			// Handle multiple redirects
			// If there are too many, we exit with
			// respcode = IDMAP_REDIRECT_LIMIT
			String redirecturl = urlparm;
			for (iredir = 0; iredir < 10; iredir++) {
				hdrmap = new HashMap();

				// Throws PageDescException unless all OK:
				httpResponse = getSingleHttpContent(bugs,
					timeoutSeconds, redirecturl);

				if (httpResponse == 200)
					break;

				String location = (String) hdrmap.get("location");
				if (location == null)
					throwResponse(DpcErrors.IDMAP_NOT_FOUND,
						"redirect, but no new loc", null);
				String httptag = "http://";
				String httpstag = "https://";
				if (location.startsWith(httptag))
					redirecturl = location;
				else if (location.startsWith(httpstag))
					redirecturl = location;
				else {
					boolean ishttps = false;
					URL testurl = new URL(urlparm);
					if (testurl.getProtocol().equals("https"))
						ishttps = true;
					int ix;
					if (ishttps == true) {
						ix = redirecturl.indexOf("/", httpstag.length());

					}
					else {
						// oldbase = redirecturl up to the path part.
						// Find first "/" after "http://"
						ix = redirecturl.indexOf("/", httptag.length());
					}

					String oldbase;
					if (ix < 0)
						oldbase = redirecturl;
					else
						oldbase = redirecturl.substring(0, ix);

					if (!location.startsWith("/"))
						oldbase += "/";
					redirecturl = oldbase + location;
				}
			}// for iredir

			if (httpResponse != 200)
				throwResponse(DpcErrors.IDMAP_REDIRECT_LIMIT, null, null);
		}// No catch clause.
		// If there's an exception the higher layers will handle the error.
		 finally {
			long timefin = System.currentTimeMillis();
			resptime = 0.001 * (timefin - timestart);
		}
	}// end getHttpContent






	/**
	 *  Retrieves the content of a page into contentbuf; returns the httpResponse if it's
	 *  200, 302, or 307.. If the httpResponse is any other value, throws PageDescException.
	 *  <p>
	 *
	 *  See: <br>
	 *  http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html <br>
	 *  Hypertext Transfer Protocol -- HTTP/1.1 Status Code Definitions <p>
	 *
	 *
	 *
	 * @param  bugs                   debug level
	 * @param  timeoutSeconds         timeout value for sockets, in seconds
	 * @param  urlparm                The URL to retrieve.
	 * @return                        The singleHttpContent value
	 * @exception  PageDescException  DESCRIPTION
	 * @exception  IOException        DESCRIPTION
	 */

	int getSingleHttpContent(
	                         int bugs,
	                         int timeoutSeconds,
	                         String urlparm)
		 throws PageDescException, IOException {
		int httpResponse = 0;

		if (bugs >= 50)
			prtln("getSingleHttpContent: urlparm: \"" + urlparm + "\"");

		// ### kluge: delete this code some day.
		// Replace all " " with "%20" in the URL.
		// Strictly speaking it's illegal to have a space in a URL,
		// but Holly says it's too difficult to correct the spaces
		// in the current records.
		String transUrl = urlparm;
		String request;
		boolean ismodified = false;
		while (true) {
			int ix = transUrl.indexOf(" ");
			if (ix < 0)
				break;
			transUrl = transUrl.substring(0, ix) + "%20"
				 + transUrl.substring(ix + 1);
			ismodified = true;
		}
		if (bugs >= 50) {
			prtln("getSingleHttpContent: urlparm:  \"" + urlparm + "\"");
			if (ismodified)
				prtln("getSingleHttpContent: transUrl: \"" + transUrl + "\"");
		}

		URL url = new URL(transUrl);
		if (!((url.getProtocol().equals("http")) || (url.getProtocol().equals("https"))))
			throwResponse(DpcErrors.IDMAP_MISC, "protocol mismatch", null);
		String hostname = url.getHost();
		int port = url.getPort();
		if (port == -1)
			port = 80;

		// for HTTPS the default port is 443, not 80
		if (url.getProtocol().equals("https"))
			port = 443;

		if (bugs >= 50) {
			prtln("PORT NUMBER is " + port);

		}

		String path = url.getPath();//  /alpha/index.html
		if (url.getQuery() != null)
			path += "?" + url.getQuery();
		if (url.getRef() != null)
			path += "#" + url.getRef();
		if (path.length() == 0)
			path = "/";
		Socket socket = null;
		boolean ishttps = false;

		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		SSLSocket socket2 = null;

		try {
			// See RFC 2616: ftp://ftp.isi.edu/in-notes/rfc2616.txt
			// This is an update to RFC 2068.

			if (url.getProtocol().equals("https"))
				ishttps = true;


			request = "GET " + path + " HTTP/1.0\r\n";

			if ((port == 80) || (port == 443))
				request += "Host: " + hostname + "\r\n";
			else
				request += "Host: " + hostname + ":" + port + "\r\n";

			// ##### if response code 500, try:
			// Or use: "User-Agent: Mozilla/1.1N\r\n"
			// Or use: "User-Agent: Mozilla/4.0 (compatible;MSIE 5.5; Windows 98)"
			// Or use: "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.0.0) Gecko/20020529"
			request += "User-Agent: www.dlese.org,dlesesupport@ucar.edu\r\n"
				 + "Accept: */*\r\n"
				 + "Connection: close\r\n"// "Keep-Alive" or "close"
			 + "\r\n";
			if (bugs >= 50)
				prtln("request: \"" + request + "\"");
			InputStream istm;
			OutputStream ostm;


			if (ishttps == false) {
				socket = new Socket();

				socket.setSoTimeout(timeoutSeconds * 1000);

				InetSocketAddress saddr = new InetSocketAddress(hostname, port);
				socket.connect(saddr, timeoutSeconds * 1000);
				ostm = socket.getOutputStream();
			}
			else {
				socket2 = (SSLSocket) factory.createSocket(hostname, port);
				socket2.startHandshake();
				ostm = socket2.getOutputStream();
			}

			ostm.write(request.getBytes());

			ostm.flush();

			if (ishttps == false) {
				istm = socket.getInputStream();
			}
			else {
				istm = socket2.getInputStream();
			}


			hdrstg = "";

			// Get status line
			String[] statusline = getStatusLine(bugs, istm);
			String statusversion = statusline[0];
			String statuscodestg = statusline[1];
			String statusreason = statusline[2];
			String statusrawline = statusline[3];

			try {
				httpResponse = Integer.parseInt(statuscodestg, 10);
			} catch (NumberFormatException nfe) {
				throwResponse(DpcErrors.IDMAP_HTTP_STATUSLINE,
					"invalid http response", statusrawline);
			}
			hdrstg += statusrawline + "\r\n";

			// Get headers
			while (true) {
				String[] hdr = getHeader(bugs, istm);
				if (hdr == null)
					break;
				String hdrname = hdr[0];// always lower case
				String hdrvalue = hdr[1];// case is "as is"
				String hdrrawline = hdr[2];
				hdrstg += hdrrawline + "\r\n";
				hdrmap.put(hdrname, hdrvalue);
			}
			if (bugs >= 50)
				prtln("getContent: start read content for \""
					 + urlparm + "\"");

			String contentType = (String) hdrmap.get("content-type");
			this.primarycontentType = contentType;
			int indexst = -1;
			
			if (contentType != null) 
				indexst = contentType.indexOf("pdf");
			if (indexst != -1) {// pdf
				/* Commenting out the PDF Code until the bugs in the PDF to Text Stripper are fixed. 
				PDFTextStripper stripper = new PDFTextStripper();
				int startPage = 1;
				int endPage = Integer.MAX_VALUE;
				Writer output = null;
				PDDocument document = null;
				StringWriter st = new StringWriter();
				try {
					document = parseDocument(istm);
					output = new PrintWriter(st);

					stripper.setStartPage(startPage);
					stripper.setEndPage(endPage);
					stripper.writeText(document, output);
				} 
				catch (NullPointerException ne){
					
					
				}
				finally {
					if (output != null) {
						output.close();
					}
					if (document != null) {
						document.close();
					}
					this.PrimaryContent = st.toString();
					
					if (this.PrimaryContent.length() < 500)// could not strip text
						contentbuf = null;
					else
						contentbuf = this.PrimaryContent.getBytes();

				}// finally*/
			}
			else {

				// Get content.  Read to MAXCONTENTLENGTH or eof.
				// Some servers, like: Server: WebSTAR/3.0.2 ID/65666
				// require that if we use HTTP 1.0 and they send a
				// content-length back, we must not attempt to read
				// past the content-length.  If we do, they do not
				// issue eof; instead we see java.net.SocketException:
				// Connection reset by peer.

				int clen = MAXCONTENTLENGTH;// default content-length
				String clenstg = (String) hdrmap.get("content-length");
				if (clenstg != null) {
					try {
						clen = Integer.parseInt(clenstg);
					} catch (NumberFormatException nfe) {
						prtln("PageDesc.getSingleHttpContent: bad content-length");
					}
				}

				byte[] tmpbuf = new byte[clen];
				int contentlen = 0;
				int numread = 0;
				while (contentlen < tmpbuf.length) {
					numread = istm.read(tmpbuf, contentlen,
						tmpbuf.length - contentlen);
					if (numread < 0)
						break;
					contentlen += numread;
				}

				contentbuf = new byte[contentlen];
				System.arraycopy(tmpbuf, 0, contentbuf, 0, contentlen);

			}

			ostm.close();
			istm.close();

		}// No catch clause.
		// If there's an exception the higher layers will handle the error.
		 finally {
			if (ishttps == false) {
				if (socket != null) {
					try {
						socket.close();
					} catch (Exception exc) {}
					socket = null;
				}
			}
			else {
				if (socket2 != null) {
					try {
						socket2.close();
					} catch (Exception exc) {}
					socket2 = null;
				}
			}
		}



		if (httpResponse == 301) {
			String location = (String) hdrmap.get("location");
			if (location != null && location.equals(urlstg + "/")) {
				// Kluge: if it's just an added "/", pretend all is OK.
				httpResponse = 200;
			}
			else
				throwResponse(DpcErrors.IDMAP_PERM_REDIRECT,
					"new location", location);
		}
		else if (httpResponse == 402)
			throwResponse(DpcErrors.IDMAP_AUTHORIZATION, null, null);
		else if (httpResponse == 404)
			throwResponse(DpcErrors.IDMAP_NOT_FOUND, null, null);
		else if (httpResponse == 500)
			throwResponse(DpcErrors.IDMAP_SERVER_ERROR, null, null);
		else if (httpResponse != 200
			 && httpResponse != 302
			 && httpResponse != 307) {
			throwResponse(DpcErrors.IDMAP_HTTP_RESPONSE,
				"invalid response", "" + httpResponse);
		}

		// Else response is OK; return it.

		if (bugs >= 50)
			prtln("getSingleHttpContent: returning httpResponse: " + httpResponse);

		return httpResponse;// 200, 302, or 307.

	}// end getSingleHttpContent



	void printtoString(InputStream istm) {

		int k;
		int aBuffSize = 11000;
		String StringFromWS = "";
		byte buff[] = new byte[aBuffSize];
		OutputStream xOutputStream = new ByteArrayOutputStream(aBuffSize);

		try {
			while ((k = istm.read(buff)) != -1)
				xOutputStream.write(buff, 0, k);

			// I can now grab the string I want
			StringFromWS = StringFromWS + xOutputStream.toString();
			System.out.println("\n\n\n\n\n String Content of the page is:");
			System.out.println(StringFromWS);
			System.out.println("\n\n\n\n\n\n\n");
		} catch (IOException e) {

			System.out.println("Exception in printtoString method. :" + e);

		}
	}



	/**
	 *  Retrieves the content of a page into contentbuf. If there's any error, throws
	 *  PageDescException.
	 *
	 * @param  bugs                   debug level
	 * @param  timeoutSeconds         timeout value for sockets, in seconds
	 * @param  urlparm                The URL to retrieve.
	 * @exception  PageDescException  DESCRIPTION
	 * @exception  IOException        DESCRIPTION
	 */

	void getFtpContent(
	                   int bugs,
	                   int timeoutSeconds,
	                   String urlparm)
		 throws PageDescException, IOException {
		int ii;
		Socket socket = null;
		Socket dsock = null;

		if (bugs >= 50)
			prtln("getFtpContent: urlparm:  \"" + urlparm + "\"");

		URL url = new URL(urlparm);
		if (!url.getProtocol().equals("ftp"))
			throwResponse(DpcErrors.IDMAP_MISC, "protocol mismatch", null);
		String hostname = url.getHost();
		int port = url.getPort();
		if (port == -1)
			port = 21;
		String path = url.getPath();//  /alpha/index.html

		try {
			int ires;
			socket = new Socket();
			socket.setSoTimeout(timeoutSeconds * 1000);
			InetSocketAddress saddr = new InetSocketAddress(hostname, port);
			socket.connect(saddr, timeoutSeconds * 1000);
			InputStream istm = socket.getInputStream();
			OutputStream ostm = socket.getOutputStream();

			ires = getFtpResponse(bugs, istm);
			chkFtpOk(ires, "greeting");

			putFtpRequest(bugs, ostm, "USER anonymous");
			ires = getFtpResponse(bugs, istm);
			chkFtpOk(ires, "user");

			putFtpRequest(bugs, ostm, "PASS dlesesupport@ucar.edu");
			ires = getFtpResponse(bugs, istm);
			if (!isFtpOk(ires))
				// Could use IDMAP_AUTHORIZATION, but this might
				// be simply because the FTP server is too busy.
				throwResponse(DpcErrors.IDMAP_FTP_LOGIN, null, null);
			chkFtpOk(ires, "pass");

			putFtpRequest(bugs, ostm, "TYPE I");
			ires = getFtpResponse(bugs, istm);
			chkFtpOk(ires, "type");

			putFtpRequest(bugs, ostm, "PASV");
			ires = getFtpResponse(bugs, istm);
			chkFtpOk(ires, "pasv");

			// Decode response: ip,ip,ip,ip,porthi,portlo
			int[] dparms = new int[6];
			int iparm;
			int istart;
			int iend;
			istart = 4;// skip over "pasv "
			for (iparm = 0; iparm < 6; iparm++) {
				// Scan for next start of digits
				while (istart < ftpline.length()) {
					char chr = ftpline.charAt(istart);
					if (Character.isDigit(chr))
						break;
					istart++;
				}

				// Scan for end of this string of digits
				iend = istart + 1;
				while (iend < ftpline.length()) {
					char chr = ftpline.charAt(iend);
					if (!Character.isDigit(chr))
						break;
					iend++;
				}
				int sum = 0;
				int imult = 1;
				for (ii = iend - 1; ii >= istart; ii--) {
					sum += imult * (ftpline.charAt(ii) - '0');
					imult *= 10;
				}
				dparms[iparm] = sum;
				istart = iend;
			}// for iparm

			String dhost = "" + dparms[0] + "." + dparms[1] + "."
				 + dparms[2] + "." + dparms[3];
			int dport = 256 * dparms[4] + dparms[5];
			if (bugs >= 50) {
				prtln("getFtpContent: dhost: \"" + dhost + "\"");
				prtln("getFtpContent: dport: " + dport);
			}
			dsock = new Socket();
			dsock.setSoTimeout(timeoutSeconds * 1000);
			InetSocketAddress dsockaddr = new InetSocketAddress(dhost, dport);
			dsock.connect(dsockaddr, timeoutSeconds * 1000);

			putFtpRequest(bugs, ostm, "RETR " + path);
			ires = getFtpResponse(bugs, istm);

			// If retrieval not OK, try to list the directory
			if (!isFtpMark(ires)) {
				putFtpRequest(bugs, ostm, "CWD " + path);
				ires = getFtpResponse(bugs, istm);
				if (!isFtpOk(ires)) {
					// FTP error codes are negative versions of IDMAP_FTP_xxx.
					throwResponse(DpcErrors.IDMAP_NOT_FOUND, null, null);
				}

				putFtpRequest(bugs, ostm, "LIST");
				ires = getFtpResponse(bugs, istm);
				if (!isFtpMark(ires)) {
					// FTP error codes are negative versions of IDMAP_FTP_xxx.
					throwResponse(DpcErrors.IDMAP_NOT_FOUND, null, null);
				}
			}

			// Get content (either file contents or dir listing).
			// Read to MAXCONTENTLENGTH or eof.
			InputStream istmdata = dsock.getInputStream();
			byte[] tmpbuf = new byte[MAXCONTENTLENGTH];
			int contentlen = 0;
			int numread = 0;
			while (contentlen < MAXCONTENTLENGTH && numread != -1) {
				numread = istmdata.read(tmpbuf, contentlen,
					tmpbuf.length - contentlen);
				if (numread < 0)
					break;
				contentlen += numread;
			}
			contentbuf = new byte[contentlen];
			System.arraycopy(tmpbuf, 0, contentbuf, 0, contentlen);
			dsock.close();
			dsock = null;
			if (bugs >= 50) {
				OutputStream bugostm = new BufferedOutputStream(
					new FileOutputStream("temp.page"));
				bugostm.write(contentbuf);
				bugostm.close();
			}

			// We cannot do the normal ftp cleanup and exit,
			// since we may have closed the dsock prematurely
			// when contentlen exceeded MAXCONTENTLENGTH.
			//
			//ires = getFtpResponse( bugs, istm);
			//chkFtpOk( ires, "getdata");
			//
			//putFtpRequest( bugs, ostm, "QUIT");
			//ires = getFtpResponse( bugs, istm);
			//chkFtpOk( ires, "quit");

			ostm.close();
			istm.close();
		}// No catch clause.
		// If there's an exception the higher layers will handle the error.
		 finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception exc) {}
				socket = null;
			}
			if (dsock != null) {
				try {
					dsock.close();
				} catch (Exception exc) {}
				dsock = null;
			}
		}
	}// end getFtpContent




	void putFtpRequest(int bugs, OutputStream ostm, String msg)
		 throws IOException {
		if (bugs >= 50)
			prtln("putFtpRequest: \"" + msg + "\"");
		ostm.write((msg + "\r\n").getBytes());// must all be in one packet
		ostm.flush();
	}


	int getFtpResponse(int bugs, InputStream istm)
		 throws PageDescException, IOException {
		if (bugs >= 50)
			prtln("getFtpResponse: entry");
		// Loop until we find a line with format "ddd .*" where d is a digit.
		int ires = 0;
		while (true) {
			ftpline = getRawLine(bugs, istm);
			if (ftpline == null)
				throwResponse(DpcErrors.IDMAP_NO_SERVICE, null, null);

			if (ftpline.length() > 4
				 && Character.isDigit(ftpline.charAt(0))
				 && Character.isDigit(ftpline.charAt(1))
				 && Character.isDigit(ftpline.charAt(2))
				 && ftpline.charAt(3) == ' ') {
				ires = 100 * (ftpline.charAt(0) - '0')
					 + 10 * (ftpline.charAt(1) - '0')
					 + 1 * (ftpline.charAt(2) - '0');
				break;
			}
		}
		if (bugs >= 50)
			prtln("getFtpResponse: ires: " + ires
				 + "  ftpline: \"" + ftpline + "\"");
		return ires;
	}



	void chkFtpOk(int ires, String msg)
		 throws PageDescException {
		if (!isFtpOk(ires))
			throwResponse(DpcErrors.IDMAP_FTP_MISC,
				"error on: " + msg, "" + ires);
	}


	boolean isFtpOk(int ires) {
		if (ires >= 200 && ires < 400)
			return true;
		else
			return false;
	}



	boolean isFtpMark(int ires) {
		if (ires >= 100 && ires < 200)
			return true;
		else
			return false;
	}





	/**
	 *  Returns a parsed HTTP status line.
	 *
	 * @param  bugs                   debug level
	 * @param  istm                   The InputStream from which to read the status line.
	 * @return                        res[0] = httpVersion<br>
	 *      res[1] = statusCode<br>
	 *      res[2] = reason<br>
	 *      res[3] = entire raw line<br>
	 *
	 * @exception  PageDescException  DESCRIPTION
	 * @exception  IOException        DESCRIPTION
	 */

	String[] getStatusLine(
	                       int bugs,
	                       InputStream istm)
		 throws PageDescException, IOException {
		int ii;
		char cc;
		String rawline = getRawLine(bugs, istm);
		if (rawline == null)
			throwResponse(DpcErrors.IDMAP_HTTP_STATUSLINE,
				"no status line", null);

		String version = "";// http version
		for (ii = 0; ii < rawline.length(); ii++) {
			cc = rawline.charAt(ii);
			if (cc == ' ')
				break;
			version += cc;
		}
		for (ii = ii; ii < rawline.length(); ii++) {// skip spaces
			if (rawline.charAt(ii) != ' ')
				break;
		}

		String status = "";// status code
		for (ii = ii; ii < rawline.length(); ii++) {
			cc = rawline.charAt(ii);
			if (cc == ' ')
				break;
			status += cc;
		}
		for (ii = ii; ii < rawline.length(); ii++) {// skip spaces
			if (rawline.charAt(ii) != ' ')
				break;
		}

		String reason = rawline.substring(ii);// reason

		if (version.length() == 0
			 || status.length() == 0) {
			throwResponse(DpcErrors.IDMAP_HTTP_STATUSLINE,
				"bad http status line", rawline);
		}

		return new String[]{version, status, reason, rawline};
	}// end getStatusLine




	/**
	 *  Returns a parsed HTTP header line. See:
	 *  http://www.w3.org/Protocols/rfc2616/rfc2616.html
	 *
	 * @param  bugs                   debug level
	 * @param  istm                   The InputStream from which to read the status line.
	 * @return                        res[0] = lower case name<br>
	 *      res[1] = value (case "as is")<br>
	 *      res[2] = entire raw line<br>
	 *
	 * @exception  PageDescException  DESCRIPTION
	 * @exception  IOException        DESCRIPTION
	 */

	String[] getHeader(
	                   int bugs,
	                   InputStream istm)
		 throws PageDescException, IOException {
		String[] res;
		int ii;
		String rawline = getRawLine(bugs, istm);
		if (rawline == null) {
			res = null;
			if (bugs >= 50)
				prtln("getHeader: ret null");
		}
		else {
			// Get header name
			char cc = 0;
			String hdrname = "";
			for (ii = 0; ii < rawline.length(); ii++) {
				cc = rawline.charAt(ii);
				// Technically we should use:
				//     if (cc == ' ' || cc == ':') break;
				// However some flakey sites have spaces in their
				// header names.
				if (cc == ':')
					break;
				hdrname += cc;
			}
			if (ii >= rawline.length())
				throwResponse(DpcErrors.IDMAP_HTTP_HEADER,
					"no colon", rawline);

			if (cc != ':')
				throwResponse(DpcErrors.IDMAP_HTTP_HEADER,
					"no colon", rawline);

			ii++;//skip colen
			for (ii = ii; ii < rawline.length(); ii++) {// skip spaces
				if (rawline.charAt(ii) != ' ')
					break;
			}

			// Get header value
			String hdrvalue = rawline.substring(ii);

			hdrname = hdrname.trim().toLowerCase();
			hdrvalue = hdrvalue.trim();
			if (hdrname.length() == 0)
				throwResponse(DpcErrors.IDMAP_HTTP_HEADER,
					"empty name", rawline);

			// Some headers are empty:  "Server:"
			///if (hdrvalue.length() == 0)
			///	throwResponse( DpcErrors.IDMAP_HTTP_HEADER,
			///		"empty value", rawline);

			if (bugs >= 50) {
				prtln("getHeader: \"" + hdrname + "\"    \"" + hdrvalue + "\"");
				///prtln("  rawline: \"" + rawline + "\"");
			}
			res = new String[]{hdrname, hdrvalue, rawline};
		}
		return res;
	}



	/**
	 *  Returns the next input line, or null if we're at eof or we find an immediate "\r\n".
	 *
	 * @param  bugs                   debug level
	 * @param  istm                   The InputStream from which to read the status line.
	 * @return                        The rawLine value
	 * @exception  PageDescException  DESCRIPTION
	 * @exception  IOException        DESCRIPTION
	 */

	String getRawLine(
	                  int bugs,
	                  InputStream istm)
		 throws PageDescException, IOException {
		int ichr;
		StringBuffer bufa = new StringBuffer();
		while (true) {
			ichr = istm.read();
			if (ichr == -1)
				break;
			if (ichr == '\n')
				break;
			if (ichr == '\r') {
				ichr = istm.read();
				if (ichr != '\n')
					throwResponse(DpcErrors.IDMAP_HTTP_HEADER,
						"no eol", bufa.toString());
				break;
			}
			else
				bufa.append((char) ichr);
		}
		String res = null;
		if (bufa.length() > 0)
			res = bufa.toString();
		if (bugs >= 50)
			prtln("getRawLine: res: "
				 + (res == null ? "null" : "\"" + res + "\""));
		return res;
	}




	/**
	 *  Extracts a summary of the specified page contents. <p>
	 *
	 *  <b>Caution:</b> changing the extraction algorithm will invalidate every checksum in
	 *  the database.
	 *
	 * @param  bugs                   debug level
	 * @param  contentstg             The entire page content.
	 * @param  urlstg                 The URL, for debug messages only.
	 * @return                        DESCRIPTION
	 * @exception  PageDescException  DESCRIPTION
	 */

	String extractSummary(
	                      int bugs,
	                      String contentstg,
	                      String urlstg)// for error msgs only
	 throws PageDescException {


		int ii;
		if (bugs >= 50)
			prtln("extractSummary.entry: urlstg: " + urlstg);

		// ========== inner class Sumspec ==========
		class Sumspec {
			String startstg;
			String endstg;
			int maxlen;
			Pattern endpat;


			Sumspec(String startstg, String endstg, int maxlen) {
				this.startstg = startstg;
				this.endstg = endstg;
				this.maxlen = maxlen;
				this.endpat = Pattern.compile(endstg,
					Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
			}
		}// end inner class Sumspec
		// ========== end inner class ==========


		// Tags to include in the summary.
		// No img: many sites use rotating images.
		// Caution on headers: some sites have javascript with
		// "story of the day".
		Sumspec[] sumspecs = new Sumspec[]{
			new Sumspec("<head", "</head>", 1000),
			new Sumspec("<b>", "</b>", 200),
			new Sumspec("<h1", "</h", 200),
			new Sumspec("<h2", "</h", 200),
			new Sumspec("<h3", "</h", 200),
			new Sumspec("<h4", "</h", 200),
			new Sumspec("<table", ">", 200),
			new Sumspec("<th", "</t|<t", 200),
			new Sumspec("<td", "</t|<t", 200),
			new Sumspec("<dt", "</d|<d", 200),
			new Sumspec("<dd", "</d|<d", 200),
			new Sumspec("<applet", "</applet>", 100),
			new Sumspec("title:", "\\.", 200),
			new Sumspec("overview:", "\\.", 200),
			new Sumspec("purpose:", "\\.", 200),
			new Sumspec("font.{0,100}size=[\" ]?[3-7]", "</font>", 200),
			new Sumspec("font.{0,100}size=[\" ]?\\+", "</font>", 200)
			};

		// Patterns that cause us to exclude the extracted substring
		String[] excludestgs = {
			"<img[^>]*>", // skip rotating images
		"<img[^>]*$", // skip rotating images
		"\\d\\d\\d\\d\\d+", // 5 digits may be a session id
		// 4 digits may be year or charset=iso-8859-1
		// 6 digits may be a color spec
		"session[^>]*>",
			"cookie[^>]*>"};
		// Make exclude pattern of the form "pat1|pat2|pat3|pat4"
		String allexcludestg = "";
		for (ii = 0; ii < excludestgs.length; ii++) {
			if (ii > 0)
				allexcludestg += "|";
			allexcludestg += excludestgs[ii];
		}
		if (bugs >= 50)
			prtln("extractSummary: allexcludestg: \"" + allexcludestg + "\"");
		Pattern excludepat = Pattern.compile(allexcludestg,
			Pattern.CASE_INSENSITIVE);

		boolean urlOnlyFlag = false;
		for (ii = 0; ii < rsd.getUrlOnlyTestsLength(); ii++) {
			if (urlstg.indexOf(rsd.getUrlOnlyTests(ii)) >= 0) {
				urlOnlyFlag = true;
				break;
			}
		}

		StringBuffer extractbuf = new StringBuffer();
		if (urlOnlyFlag) {
			extractbuf.append(urlstg);
		}
		else {

			// Make start pattern of the form "(pat1)|(pat2)|(pat3)|(pat4)"
			String allstartstg = "";
			for (ii = 0; ii < sumspecs.length; ii++) {
				if (ii > 0)
					allstartstg += "|";
				allstartstg += "(" + sumspecs[ii].startstg + ")";
			}
			if (bugs >= 50)
				prtln("extractSummary: allstartstg: \"" + allstartstg + "\"");
			Pattern startpat = Pattern.compile(allstartstg,
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

			Matcher startmat = startpat.matcher(contentstg);
			int istart = 0;
			while (true) {
				// Scan for any start string
				if (!startmat.find(istart))
					break;

				// Find which group we matched.
				// Index origin 1.
				int igrp = -1;
				for (ii = 1; ii <= sumspecs.length; ii++) {
					if (startmat.group(ii) != null) {
						igrp = ii;
						break;
					}
				}
				if (igrp < 0)
					throwResponse(DpcErrors.IDMAP_MISC,
						"invalid sumspecs", "" + igrp);

				igrp--;// convert to index origin 0

				istart = startmat.start();

				// Scan for the matching end string
				Matcher endmat = sumspecs[igrp].endpat.matcher(contentstg);
				int iend;
				if (endmat.find(startmat.end()))
					iend = endmat.end();
				else
					iend = Math.min(istart + sumspecs[igrp].maxlen,
						contentstg.length());

				String extract = contentstg.substring(istart, iend);
				if (bugs >= 50) {
					prtln("extractSummary: istart " + istart
						 + "  iend: " + iend);
					prtln("    extract: \"" + extract + "\"");
				}

				// Remove excluded strings from the extracted substring.
				while (true) {
					Matcher excludemat = excludepat.matcher(extract);
					if (!excludemat.find())
						break;
					int excbeg = excludemat.start();
					int excend = excludemat.end();
					if (bugs >= 50)
						prtln("extractSummary: excluded substring: \""
							 + extract.substring(excbeg, excend) + "\"");
					extract = extract.substring(0, excbeg)
						 + extract.substring(excend);
				}
				extractbuf.append(extract);
				istart = iend;
			}
		}

		if (extractbuf.length() == 0) {
			extractbuf.append(urlstg);
			if (bugs >= 50)
				prtln("extractSummary: extract was empty; using url");
		}
		if (bugs >= 50)
			prtln("extractSummary: final extract: \""
				 + extractbuf + "\"");
		return extractbuf.toString();
	}




	void throwResponse(
	                   int respcode,
	                   String message,
	                   String auxinfo)
		 throws PageDescException {
		throw new PageDescException(respcode, message, auxinfo);
	}





	/**
	 *  Prints a String without a trailing newline.
	 *
	 * @param  msg  DESCRIPTION
	 */

	static void prtstg(String msg) {
		System.out.print(msg);
	}



	/**
	 *  Prints a String with a trailing newline.
	 *
	 * @param  msg  DESCRIPTION
	 */

	static void prtln(String msg) {
		System.out.println(msg);
	}

}// end class PageDesc


//===========================================================================

class PageDescException extends Exception {


	int respcode;
	String message;
	String auxinfo;


	PageDescException(
	                  int respcode,
	                  String message,
	                  String auxinfo) {
		this.respcode = respcode;
		this.message = message;
		this.auxinfo = auxinfo;
	}
}

