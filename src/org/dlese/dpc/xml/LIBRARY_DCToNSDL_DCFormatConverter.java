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
package org.dlese.dpc.xml;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.Files;

import javax.xml.transform.Transformer;
import javax.servlet.ServletContext;
import java.io.File;
import org.dom4j.*;

/**
 *  EXPERIMENTAL (and low fidelity) converter for library_dc to nsdl_dc.<p>
 *
 *  NOTE: this converter is a quick and dirty approach for going from library_dc
 *  to nsdl_dc. It first converts library_dc to oai_dc, and then manipuates the
 *  root element to turn the oai_dc into nsdl_dc. The result is a valid nsdl_dc
 *  record, but this process is likely not optimal!
 *
 * @author    Jonathan Ostwald
 */
public class LIBRARY_DCToNSDL_DCFormatConverter implements XMLFormatConverter {

	protected File library_dc_to_oai_dc_transform_file = null;


	/**
	 *  Converts from the libarary_dc format.
	 *
	 * @return    The String "libarary_dc".
	 */
	public String getFromFormat() {
		return "libarary_dc";
	}


	/**
	 *  Converts to the nsdl_dc format
	 *
	 * @return    The String "nsdl_dc".
	 */
	public String getToFormat() {
		return "nsdl_dc";
	}


	/**
	 *  The main program for the LIBRARY_DCToNSDL_DCFormatConverter class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		prtln("\n===============================================");
		/* 		prtln ("TransformTester");
		String xslt = "library_dc-v1.1-to-oai_dc.xsl";
		String xform_path = xsl_dir + "/" + xslt;
		File file = new File(xform_path);
		if (!file.exists())
			throw new Exception ("transform does not exist at: " + xform_path); */
		String src = "C:/Documents and Settings/ostwald/devel/dcs-instance-data/local-ndr/records/library_dc/1249504379560/TECH-NOTE-000-000-000-789.xml ";
		String xml = Files.readFileToEncoding(new File(src), "UTF-8").toString();
		LIBRARY_DCToNSDL_DCFormatConverter tester = new LIBRARY_DCToNSDL_DCFormatConverter();
		// String oai_dc = XSLTransformer.transformFile(src, xform_path);
		String oai_dc = tester.convertXML(xml);
		prtln("\ntransformed:\n" + oai_dc);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  xml  NOT YET DOCUMENTED
	 * @return      NOT YET DOCUMENTED
	 */
	public String convertXML(String xml) {
		return convertXML(xml, null);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  xml      NOT YET DOCUMENTED
	 * @param  context  NOT YET DOCUMENTED
	 * @return          NOT YET DOCUMENTED
	 */
	public String convertXML(String xml, ServletContext context) {
		getXFormFilesAndIndex(context);
		try {
			Transformer format_transformer =
				XSLTransformer.getTransformer(library_dc_to_oai_dc_transform_file.getAbsolutePath());
			String oai_dc = XSLTransformer.transformString(xml, format_transformer);
			return OaiDcToNsdlDC(oai_dc);
		} catch (Throwable e) {
			System.err.println("LIBRARY_DCToNSDL_DCFormatConverter was unable to produce transformed file: " + e);
			e.printStackTrace();
			return "";
		}
	}


	private String OaiDcToNsdlDC(String oai_dc) throws Exception {
		Document doc = Dom4jUtils.getXmlDocument(oai_dc);

		Element root = doc.getRootElement();
		String schemaURI = "http://ns.nsdl.org/schemas/nsdl_dc/nsdl_dc_v1.02.xsd";
		String targetNS = "http://ns.nsdl.org/nsdl_dc_v1.02/";

		QName qname = DocumentHelper.createQName("nsdl_dc", new Namespace("nsdl_dc", targetNS));
		root.setQName(qname);
		// root.setAttributeValue("xmlns:nsdl_dc","http://ns.nsdl.org/nsdl_dc_v1.02/");
		root.addAttribute("xmlns", targetNS);
		root.addAttribute("schemaLocation", null);
		root.addAttribute("xsi:schemaLocation", targetNS + " " + schemaURI);
		root.addAttribute("schemaVersion", "1.02.020");
		return doc.asXML();
	}


	protected void getXFormFilesAndIndex(ServletContext context) {
		String xslt = "library_dc-v1.1-to-oai_dc.xsl";
		if (context == null) {
			String xsl_dir = "C:/Documents and Settings/ostwald/devel/projects/dcs-project/web/WEB-INF/xsl_files";
			this.library_dc_to_oai_dc_transform_file = new File(xsl_dir, xslt);
		}
		else {
			this.library_dc_to_oai_dc_transform_file = new File(((String) context.getAttribute("xslFilesDirecoryPath")) +
				"/" + context.getInitParameter("library_dc-v1.1-to-oai_dc-xsl"));
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  context  NOT YET DOCUMENTED
	 * @return          NOT YET DOCUMENTED
	 */
	public long lastModified(ServletContext context) {
		return -1;
	}


	private static void pp(Node n) {
		prtln(Dom4jUtils.prettyPrint(n));
	}


	private static void prtln(String s) {
		System.out.println(s);
	}
}

