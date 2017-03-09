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
package org.dlese.dpc.ndr.apiproxy;
import java.util.*;
import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dom4j.*;
/**
 * @author     ostwald<p>
 *
 *      $Id $
 * @version    $Id: DataComponent.java,v 1.2 2009/03/20 23:33:53 jweather Exp $
 */

public class DataComponent extends InputXMLComponent {
	private static boolean debug = true;


	/**  Constructor for the DataComponent object */
	public DataComponent() {
		super("data");
	}


	/**
	 *  Adds a DataStream command to the DataComponent with default action.
	 *
	 * @param  format         The feature to be added to the DataStreamCmd
	 *      attribute
	 * @param  content        The feature to be added to the DataStreamCmd
	 *      attribute
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public void addDataStreamCmd(String format, Element content) throws Exception {
		addDataStreamCmd(format, content, null);
	}


	/**
	 *  Gets the datastream designated by format, or null if it doesn't exist.
	 *
	 * @param  format  NOT YET DOCUMENTED
	 * @return         The dataStreamCmd value
	 */
	private Element getDataStreamCmd(String format) {

		/*
			we don't know if the format element will have a command parent
			(e.g., "add", "replace") or if it will be a direct child of "data").
			can't figure out how to construct single xpath to catch both cases, so
			we use both ...
		*/
		XPath xpath = DocumentHelper.createXPath("*//format[@type=\"" + format + "\"]");
		Node data_stream = xpath.selectSingleNode(this.component);
		if (data_stream == null) {
			xpath = DocumentHelper.createXPath("format[@type=\"" + format + "\"]");
			data_stream = xpath.selectSingleNode(this.component);
		}
		return (Element) data_stream;
	}


	/**
	 *  Inserts the provided info stream element into the datastream designated by "format".
	 *
	 * @param  format         data stream in which to insert info
	 * @param  info           the info stream Element
	 * @exception  Exception  if datastream does not exist for "format"
	 */
	public void setInfoStream(String format, Element info) throws Exception {
		Element ds = getDataStreamCmd(format);
		if (ds == null)
			throw new Exception("addInfoStream unable to locate datastream for " + format);
		Element old_info = ds.element("info");
		if (old_info != null)
			ds.remove(old_info);
		ds.add(info.createCopy());
	}


	/**
	 *  Adds a DataStream command to the DataComponent with specified action.
	 *
	 * @param  format         datastream format (e.g., "nsdl_dc")
	 * @param  content        the datastream content
	 * @param  action         the command action (e.g., "add", "delete") or null
	 *      (the default).
	 * @exception  Exception  If datastream element is null.
	 */
	public void addDataStreamCmd(String format, Element content, String action) throws Exception {
		Element ds = DocumentHelper.createElement("format");
		ds.setAttributeValue("type", format);

		if (action == null || !action.equals("delete")) {
			if (content == null)
				throw new Exception("attempting to set null datastream for format=\"" + format + "\"");
			/* 			else {
				prtln ("\n Adding " + format + " datastream");
				pp (content);
				prtln (" -------  end datastream --------\n");
			} */
			Element meta = ds.addElement("meta");

			/*
				7/9/07 KLUDGE: NDR is not recognizing xmlns:xsi from further up the xml doc,
				so try to put it in here as an attribute to end run this bug ...
			*/
			Element stream = content.createCopy();
			stream.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

			meta.add(stream);
			Element info = ds.addElement("info");
		}

		this.addCommand(ds, action);
	}

	/* 	public void addServiceDescriptionCmd (Element content) {
		addServiceDescriptionCmd (content, null);
	}
	public void addServiceDescriptionCmd (Element content, String action) {
		this.addCommand (content, action);
	} */
}

