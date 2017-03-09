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

import org.dlese.dpc.services.dds.toolkit.*;

import java.util.*;
import java.net.*;
import java.io.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.ndr.NdrUtils;
import org.dlese.dpc.ndr.toolkit.NDRToolkit;
import org.dlese.dpc.ndr.toolkit.MimeTypes;
import org.nsdl.repository.model.types.*;
import org.nsdl.repository.model.types.Type;
import org.nsdl.repository.model.Datastream;
import org.nsdl.repository.model.Datastream.Name;
import org.nsdl.repository.model.NDRObject;
import org.nsdl.repository.model.NDRObjectInfo;
import org.nsdl.repository.access.Results;
import org.nsdl.repository.util.NDRObjectTemplate;

import org.dlese.dpc.schemedit.test.TesterUtils;
import org.dlese.dpc.standards.asn.NameSpaceXMLDocReader;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.ndr.toolkit.ContentUtils;
import org.dlese.dpc.ndr.apiproxy.NDRConstants;
import org.dom4j.*;

/**
 *  Unpack a METS envelop and place the contents (metadata + content) into the
 *  NDR.<p>
 *
 *  For now, assume the collection (mdp and agg) objects already exist in the
 *  NDR and are provided to this class.
 *
 * @author    ostwald
 */
public class LibraryDCMetsIngester extends MetsIngester {

	private static boolean debug = true;

	/**
	 *  Constructor for the LibraryDCMetsIngester object
	 *
	 * @param  doc            NOT YET DOCUMENTED
	 * @param  aggHandle      NOT YET DOCUMENTED
	 * @param  mdpHandle      NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */

	LibraryDCMetsIngester(Document doc, String aggHandle, String mdpHandle) throws Exception {
		super(doc, aggHandle, mdpHandle);
	}


	String getXmlFormat() {
		return "library_dc";
	}
	
	String getUrlXpath () {
		return "URL";
	}
	

	/**
	 *  The main program for the LibraryDCMetsIngester class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		TesterUtils.setSystemProps();

		String env = "sanluis";

		String propsPath = null;
		String metsPath = null;

		if (env.equals("sanluis")) {
			// propsPath = "C:/Documents and Settings/ostwald/devel/ndrServerProps/ndr.test.properties";
			propsPath = "C:/Documents and Settings/ostwald/devel/ndrServerProps/dls.ndr.properties";
			metsPath = "H:/Documents/Alliance/mets-work/mets-record.xml";
		}
		if (env.equals("taos")) {
			propsPath = "/Users/ostwald/projects/dcs.properties";
			metsPath = "/Users/ostwald/devel/tmp/mets-record.xml";
		}
		NdrUtils.setup(new File(propsPath));

		Document metsDoc = null;
		try {
			metsDoc = Dom4jUtils.getXmlDocument(new File(metsPath));
		} catch (Throwable e) {
			prtln(e.getMessage());
			return;
		}

		String ndrHost = "dls";
		String mdpHandle = null;
		String aggHandle = null;
		if (ndrHost.equals("dls")) {
			aggHandle = "ndr:16";
			mdpHandle = "ndr:17";
		}
		if (ndrHost.equals("ndrtest")) {
			aggHandle = "2200/test.20090821185036799T";
			mdpHandle = "2200/test.20090821185037493T";
		}

		LibraryDCMetsIngester ingester = new LibraryDCMetsIngester(metsDoc, aggHandle, mdpHandle);

	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n  NOT YET DOCUMENTED
	 * @return    NOT YET DOCUMENTED
	 */
	private static String pp(Node n) {
		return Dom4jUtils.prettyPrint(n);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("LibraryDCMetsIngester: " + s);
			System.out.println(s);
		}
	}
	
}

