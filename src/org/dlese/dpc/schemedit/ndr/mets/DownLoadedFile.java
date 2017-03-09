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
package org.dlese.dpc.schemedit.ndr.mets;

import org.dlese.dpc.ndr.toolkit.ContentUtils;
import org.dlese.dpc.util.TimedURLConnection;
import org.dlese.dpc.util.Files;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 *  Utility to download a URL and provide information about it, such as it's
 *  contentType, contentLength, binary content, etc<p>
 *
 *  NOTE: getContent fetches binary content encoded to string for binary Files
 *
 * @author    Jonathan Ostwald
 */
public class DownLoadedFile {

	private boolean debug = true;
	private boolean isBinary = false;
	private URL url;
	private String contentType;
	private int contentLength;
	private String content;
	private String fileName;
	private URLConnection uc;
	private byte[] binaryData;


	/**
	 *  Constructor for the DownLoadedFile object
	 *
	 * @param  url            url to be downloaded
	 * @exception  Exception  if url cannot be processed
	 */
	public DownLoadedFile(URL url) throws Exception {
		this.url = url;
		uc = url.openConnection();
		prtln("url: " + url.toString());
		contentType = uc.getContentType();
		prtln("contentType: " + contentType);
		contentLength = uc.getContentLength();
		prtln("contentLength: " + contentLength);
		fileName = url.getFile().substring(url.getFile().lastIndexOf('/') + 1);
		if (contentType.startsWith("text/") || contentLength == -1) {
			// throw new IOException("This is not a binary file.");
			content = fetchContent();
		}
		else {
			content = this.fetchBinaryContent();
		}
	}


	/**
	 *  Gets the content attribute of the DownLoadedFile object
	 *
	 * @return    The content value
	 */
	public String getContent() {
		return this.content;
	}



	/**
	 *  Gets the fileName attribute of the DownLoadedFile object
	 *
	 * @return    The fileName value
	 */
	public String getFileName() {
		return this.fileName;
	}


	/**
	 *  Gets the isBinary attribute of the DownLoadedFile object
	 *
	 * @return    The isBinary value
	 */
	public boolean getIsBinary() {
		return this.isBinary;
	}


	/**
	 *  Gets the contentLength attribute of the DownLoadedFile object
	 *
	 * @return    The contentLength value
	 */
	public int getContentLength() {
		return this.contentLength;
	}


	/**
	 *  Gets the contentType attribute of the DownLoadedFile object
	 *
	 * @return    The contentType value
	 */
	public String getContentType() {
		return this.contentType;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private String fetchContent() throws Exception {
		return TimedURLConnection.importURL(url.toString(), 6000);
	}


	/**
	 *  Gets the content attribute of the DownLoadedFile class
	 *
	 * @return                The content value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	private String fetchBinaryContent() throws Exception {
		this.isBinary = true;
		InputStream raw = uc.getInputStream();
		InputStream in = new BufferedInputStream(raw);
		binaryData = new byte[contentLength];
		int bytesRead = 0;
		int offset = 0;
		while (offset < contentLength) {
			bytesRead = in.read(binaryData, offset, binaryData.length - offset);
			if (bytesRead == -1)
				break;
			offset += bytesRead;
		}
		in.close();

		if (offset != contentLength) {
			throw new IOException("Only read " + offset + " bytes; Expected " + contentLength + " bytes");
		}
		prtln("read " + offset + " bytes");
		return ContentUtils.encodeToString(binaryData);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  args           NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String args[]) throws Exception {
		String url = "http://www.dls.ucar.edu/people/ostwald/ccs/WaterTreatmentStub.pdf";

		if (args.length > 0)
			url = args[0];

		URL u = new URL(url);
		// URL u = new URL("http://ndrtest.nsdl.org/api/get/2200/test.20090501174936496T/content");

		DownLoadedFile dlf = new DownLoadedFile(u);

		prtln(dlf.content.length() + " bytes downloaded");
		// String filename = u.getFile().substring(u.getFile().lastIndexOf('/') + 1);

		String outpath = "C:/tmp/downloaded/" + dlf.fileName;

		if (dlf.isBinary) {
			prtln("BINARY");
			FileOutputStream out = new FileOutputStream(outpath);
			out.write(dlf.binaryData);

			out.flush();
			out.close();
		}
		else {
			prtln("NOT binary");
			Files.writeFile(dlf.content, outpath);
		}
		prtln("wrote to " + outpath);

	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	static void prtln(String s) {
		System.out.println(s);
	}
}

