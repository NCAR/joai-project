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

import org.dlese.dpc.xml.*;
import org.dlese.dpc.util.*;

import javax.xml.transform.Transformer;

import org.dom4j.*;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.*;

import java.io.*;
import java.lang.*;

public class TransformTester {

	String xsl_dir = "C:/Documents and Settings/ostwald/devel/projects/dcs-project/web/WEB-INF/xsl_files";
	String transformerFactoryClass = null;
	String xsl2_tFactory = "net.sf.saxon.TransformerFactoryImpl";
	
	public TransformTester () {

		String defaultClass = "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";
		try {
			Class result = Class.forName( defaultClass );
	
			// If the TransformerFactory class is available, use it:
			if ( result != null ) {
				// System.setProperty( "javax.xml.transform.TransformerFactory", defaultClass );
			}
		} catch (Exception e) {
			prtln (e.getMessage());
		}
	}
	
	public String transformFile (String path, String transform) {
		return transformFile (path, transform, null);
	}
	
	public String transformFile (String path, String transform, String tFactory) {
		String input = null;
		try {
			input = Files.readFile(path).toString();
			return transformString (input, transform, tFactory);
		} catch (Exception e) {
			prtln ("ERROR: " + e.getMessage());
			return "";
		}
	}
	
	public String transformString (String input, String transform) {
		return transformString (input, transform, null);
	}
	
	public String transformString (String input, String transform, String tFactory) {
		try {
			File transform_file = new File (xsl_dir, transform);

			Transformer transformer = XSLTransformer.getTransformer(transform_file.getAbsolutePath(), tFactory);
			String transformed_content = XSLTransformer.transformString(input, transformer);
			
			prtln ("\ntransformer: " + transformer.getClass().getName());
			
			SAXReader reader = new SAXReader();
			Document document = DocumentHelper.parseText(transformed_content);
	
			// Dom4j automatically writes using UTF-8, unless otherwise specified.
			OutputFormat format = OutputFormat.createPrettyPrint();
			StringWriter outputWriter = new StringWriter();
			XMLWriter writer = new XMLWriter(outputWriter, format);
			writer.write(document);
			outputWriter.close();
			writer.close();
			return outputWriter.toString();	
		} catch (Throwable t) {
			prtln (t.getMessage());
			t.printStackTrace();
			return "";
		}
	}
			
	public void nsdl_dc_test () {
		// String src = "C:/tmp/transform/NSDL_DC_v1.02.xml";
		String src = "L:/ostwald/tmp/transform-sandbox/records/nsdl_dc/relation-and-conformsTo-sample.xml";
		String transform = "nsdl_dc-v1.02-to-nsdl_dc-v1.03.xsl";
		String transformed = transformFile(src, transform);
		prtln ("\ntransformed:\n" + transformed);
	}

	
	private String nsdl_dc_1_02_to_ncs_item (String src) {
		String transform = "nsdl_dc-v1.02-to-nsdl_dc-v1.03.xsl";
		String nsdl_v103 = transformFile(src, transform);
		transform = "nsdl-dc-v1.03-to-ncs-item-v1.02.xsl";
		String ncs_item = transformString (nsdl_v103, transform, xsl2_tFactory);
		// prtln ("\ntransformed:\n" + ncs_item);
		return ncs_item;
	}
	
	public void nsdl_item_test2 () {
		String src = "C:/tmp/transform/NSDL_DC_v1.02.xml";
		String transform = "nsdl-dc-v1.03-to-ncs-item-v1.02.xsl";
		String ncs_item = transformFile (src, transform, xsl2_tFactory);
		prtln ("\ntransformed:\n" + ncs_item);
	}
	
	public void adn_test () {
		String src = "L:/ostwald/tmp/transform-sandbox/records/adn/NASA-Edmall-0749.xml";
		String transform = "adn-v0.6.50-to-nsdl_dc-v1.02.xsl";
		String transformed = transformFile(src, transform);
		prtln ("\ntransformed:\n" + transformed);
	}
		
	static void showTransformerFactoryClass () {
		String transformerFactory = System.getProperty("javax.xml.transform.TransformerFactory");
		prtln("\n transformerFactory: " + transformerFactory);
	}
		
	public static void main (String[] args) throws Exception {
		prtln ("\n===============================================");
		prtln ("TransformTester");
		TransformTester tester = new TransformTester();
		
		String src = "C:/tmp/relation-and-conformsTo-sample.xml";
		String transformed = tester.nsdl_dc_1_02_to_ncs_item (src);
		prtln (transformed);
		Files.writeFile(transformed, "C:/tmp/transformed.xml");
	}
		
	private static void prtln (String s) {
		System.out.println (s);
	}
}

