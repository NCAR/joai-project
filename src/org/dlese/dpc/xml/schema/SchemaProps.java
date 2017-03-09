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
package org.dlese.dpc.xml.schema;

import org.dlese.dpc.xml.*;
// import org.dlese.dpc.xml.schema.compositor.Compositor;

import java.io.*;
import java.util.*;

import java.net.*;
import org.dom4j.Node;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Namespace;

import com.sun.msv.datatype.xsd.*;

/**
 *  Stores XML schema properties defined in the root schema File<p>
 *
 *
 *
 * @author     ostwald <p>
 *

 */
public class SchemaProps {

	private static boolean debug = true;
	private Map map;
	private Element rootElement = null;
	private boolean namespaceEnabled = false;

/* 	private URI rootURI = null;
	private String version;
	private String targetNamespace;
	private String elementFormDefault = "qualified";
	private String attributeFormDefault = "unqualified";
	private Namespace schemaNS; */

	/**
	 *  Constructor for the SchemaProps object for disk-based schema
	 *
	 * @param  schemaFile                 path to root file of schema
	 * @exception  SchemaPropsException  Description of the Exception
	 */
	 public SchemaProps(URI uri) {
		 map = new HashMap();
		 setProp ("schemaLocation", uri.toString());
	}

	public void init (Document  schemaDoc) {
		rootElement = schemaDoc.getRootElement();
		
		if (rootElement == null) {
			prtln ("SchemaProps ERROR: rootElement not found");
			return;
		}
		
		setProp ("namespace", rootElement.getNamespace());
		
		setDefaultProps ();
		
		for (Iterator i=rootElement.attributeIterator();i.hasNext();) {
			Attribute att = (Attribute)i.next();
			setProp (att.getName(), att.getValue());
		}
	}
	
	private void setDefaultProps () {
		setProp ("elementFormDefault", "qualified");
		setProp ("attributeFormDefault", "unqualified");
	}
	
	public void setProp (String name, Object val) {
		map.put (name, val);
	}
	
	public Object getProp (String prop) {
		return map.get (prop);
	}
	
	
	public static void main (String [] args) throws Exception {
		// paths to schema files
		String play = "http://www.dpc.ucar.edu/people/ostwald/Metadata/NameSpacesPlay/cd.xsd";
		String news_opps = "/devel/ostwald/metadata-frameworks/news-opps-project/news-opps.xsd";
		String dlese_collect = "/devel/ostwald/metadata-frameworks/collection-v1.0.00/collection.xsd";
		String local_play = "/devel/ostwald/SchemEdit/NameSpaces/Play-local/cd.xsd";
		String original = "/devel/ostwald/SchemEdit/NameSpaces/Original/cd.xsd";
		String framework_config = "/devel/ostwald/tomcat/tomcat/webapps/schemedit/WEB-INF/metadata-frameworks/framework-config/dcsFrameworkConfig-0.0.2.xsd";
		String nsdl = "http://ns.nsdl.org/schemas/nsdl_dc/nsdl_dc_v1.02.xsd";
		String statusReportSimple = "/devel/ostwald/metadata-frameworks/ProjectReport/statusReportSimple.xsd";
		
		String path = statusReportSimple;
		URI uri = null;
		
		if (args.length > 0)
			path = args[0];
		prtln ("\n-------------------------------------------------");
		prtln ("SchemaProps\n");
		
		Document doc = null;
		if (path.indexOf ("http:") == 0) {
			URL schemaUrl = null;
			try {
				schemaUrl = new URL (path);
			} catch (Exception e) {
				prtln ("bad url: " + e.getMessage());
				return;
			}
			doc = Dom4jUtils.getXmlDocument(schemaUrl);
			uri = schemaUrl.toURI();
		}
		else {
			File file = new File (path);
			doc = Dom4jUtils.getXmlDocument(file);
			uri = file.toURI();
		}
		SchemaProps props = new SchemaProps(uri);
		props.init (doc);
		props.showProps();
	}
	
	public void showProps () {
		Iterator i = map.keySet().iterator();
		prtln ("Schema props map (" + map.size() + ")");
		while (i.hasNext()) {
			String propName = (String)i.next();
			prtln ("\t" + propName + ": " + getProp (propName));
		}
	}
			
	
	/**
	 *  Description of the Method
	 *
	 * @param  o  Description of the Parameter
	 */
	private void pp (Node n) {
		prtln (Dom4jUtils.prettyPrint (n));
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			// System.out.println("SchemaProps: " + s);
			System.out.println(s);
		}
	}

}

