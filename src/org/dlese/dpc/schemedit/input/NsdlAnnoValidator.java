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

import org.dlese.dpc.schemedit.action.form.SchemEditForm;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.xml.Dom4jUtils;

import java.util.*;

import org.dom4j.Node;
import org.dom4j.Element;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;


/**
 *  Extension of SchemEditValidator that is only concerned with a few
 *  msp2-specific fields.
 *
 *@author     ostwald
 *@created    June 25, 2009
 */
public class NsdlAnnoValidator extends SchemEditValidator {

	private static boolean debug = false;
	private Map pathMap = null;
	final String ASN_STANDARDS_PATH = "/nsdl_anno/ASNstandard";


	/**
	 *  Constructor for the NsdlAnnoValidator object
	 *
	 *@param  sef        Description of the Parameter
	 *@param  framework  Description of the Parameter
	 *@param  request    Description of the Parameter
	 *@param  mapping    NOT YET DOCUMENTED
	 */
	public NsdlAnnoValidator(SchemEditForm sef,
			MetaDataFramework framework,
			ActionMapping mapping,
			HttpServletRequest request) {
		super(sef, framework, mapping, request);

		if (this.docMap != null)
			prtln(Dom4jUtils.prettyPrint(this.docMap.getDocument()));
	}


	/**
	 *  Suppress validation of all fields under MATERIALS_LIST_PATH
	 *
	 *@param  inputField  Description of the Parameter
	 *@return             Description of the Return Value
	 */
 	protected boolean skipFieldValidation(InputField inputField) {
		return inputField.getNormalizedXPath().equals(this.ASN_STANDARDS_PATH+"/@alignment");
	}


	/**
	* the docMap comes to us un-updated.
	* create a asn:element mapping for docMap elements (cloned)
	* empty the docMap element
	* for each field:
	*  if there is an entry in the map - insert element
	*  if the field has a value - create element
	*/
	private void updateASNStandardsFields () {
		prtln ("updateASNStandardsFields");
		
		if (this.docMap != null) {
			prtln ("---- BEFORE ------");
			prtln(Dom4jUtils.prettyPrint(this.docMap.getDocument()));
			prtln ("--------------");
		}
		
 		String parentPath = XPathUtils.getParentXPath(this.ASN_STANDARDS_PATH);
		Element parent = (Element)this.docMap.selectSingleNode (parentPath);
/*		List children = parent.elements(XPathUtils.getNodeName(this.ASN_STANDARDS_PATH));
		prtln ("\n" + children.size() + " children obtained from parent");
		for (Iterator i = children.iterator();i.hasNext();) {
			Element asnElement = (Element)i.next();
			prtln ("  " + Dom4jUtils.prettyPrint(asnElement));
		} */
		
		prtln ("\n--------------------- \n");
		
		Map asnMap = new HashMap();
		List asnNodes = this.docMap.selectNodes (this.ASN_STANDARDS_PATH);
		prtln ("\n" + asnNodes.size() + " asn nodes selected from docMap");
		for (Iterator i = asnNodes.iterator();i.hasNext();) {
			Element asnElement = (Element)i.next();
			// prtln ("  " + Dom4jUtils.prettyPrint(asnElement));
			asnMap.put (asnElement.getTextTrim(), asnElement.createCopy());
		}
		
		try {
			docMap.removeSiblings(this.ASN_STANDARDS_PATH);
		} catch (Exception e) {
			// this is not always an error ...
			// prtln("error removing siblings for multivalue: " + e.getMessage());
		}
		
		for (Iterator i = im.getMultiValueFields().iterator(); i.hasNext(); ) {
			InputField field = (InputField) i.next();

 			if (!field.getNormalizedXPath().equals("/nsdl_anno/ASNstandard")) {
				prtln("skipping: " + field.getXPath());
				continue;
			}
			prtln("\nprocessing field for: " + field.getXPath());

			String siblingXPath = XPathUtils.getSiblingXPath(field.getXPath());

			// add the value of the current field.
			String value = field.getValue();
			prtln ("FIELD value: " + value);
			if (value != null && value.trim().length() > 0) {
				Element existing = (Element)asnMap.get (value);
				try {
					if (existing != null) {
						prtln ("inserting existing");
						this.docMap.insertElement (existing, parent, this.ASN_STANDARDS_PATH);
					}
					else {
						prtln ("inserting new");
						Node newNode = docMap.createNewSiblingNode(this.ASN_STANDARDS_PATH);
						newNode.setText(value);
					}
				} catch (Exception e) {
					prtln ("failed to add element: " + e.getMessage());
				}
			}
		}
		if (this.docMap != null) {
			prtln ("---- AFTER ------");
			prtln(Dom4jUtils.prettyPrint(this.docMap.getDocument()));
			prtln ("--------------");
		}
	}
	
	/**
	 *  Validate the multivalue parameters managed by the input manager. For each
	 *  different group of multivalue elements found, remove all existing elements,
	 *  then add the NON-EMPTY params from the input, and finally validate,
	 *  returning errors.<p>
	 *
	 *  NOTE: fields having subelements or attributes must NOT be "updated" by this
	 *  method, since it destroys the element before adding a new one with JUST the
	 *  element text and no child nodes.
	 */
	public void updateMultiValueFields() {
		prtln("\nupdateMultiValueFields()");
		// im.displayMultiValueFields();

		updateASNStandardsFields();
		
		String currentElementPath = "";
		List mulitValueGroups = new ArrayList();

		// traverse all multivalue fields in the InputManager
		for (Iterator i = im.getMultiValueFields().iterator(); i.hasNext(); ) {
			InputField field = (InputField) i.next();
/* 			prtln("processing field: " + field.getXPath());
			prtln("   normalized: " + field.getNormalizedXPath()); */

 			if (field.getNormalizedXPath().equals("/nsdl_anno/ASNstandard")) {
				// prtln("skipping: " + field.getXPath());
				continue;
			}

			/*
				we are only concerned with fields that are represented as multiboxes. fields
				that are not unbounded are represented as select objects and therefore will
				always have a value and do not need to be processed here
			*/
			boolean hasVocabLayout = false;
			try {
				hasVocabLayout = this.framework.getVocabLayouts().hasVocabLayout(field.getNormalizedXPath());
			} catch (Throwable t) {}
			if (hasVocabLayout) {
				// prtln ("\t .. hasVocabLayout: " + field.getNormalizedXPath());
			}

			if (!field.getSchemaNode().isUnbounded() && !hasVocabLayout) {
				// prtln("  .. not unbounded: continuing");
				continue;
			}
			String siblingXPath = XPathUtils.getSiblingXPath(field.getXPath());

			// is this a new group? groups are identified by the siblingXPath (which identifies all members)
			if (!siblingXPath.equals(currentElementPath)) {
				currentElementPath = siblingXPath;
				mulitValueGroups.add(field);
				// prtln("   added new group: " + field.getFieldName() + "(" + siblingXPath + ")");

				// delete all siblings at the currentElementPath
				try {
					docMap.removeSiblings(currentElementPath);
				} catch (Exception e) {
					// this is not always an error ...
					// prtln("error removing siblings for multivalue: " + e.getMessage());
				}
			}

			// add the value of the current field.
			String value = field.getValue();
			if ((value != null) && (value.trim().length() > 0)) {
				try {
					// prtln("  ... about to create a new node at " + field.getXPath());
					Node newNode = docMap.createNewSiblingNode(field.getXPath());
					newNode.setText(value);
				} catch (Throwable t) {
					prtln("updateMultiValueFields ERROR: " + t.getMessage());
					// t.printStackTrace();
				}
			}
		}
		multiValueFields = mulitValueGroups;
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			// SchemEditUtils.prtln(s, "NsdlAnnoValidator");
			SchemEditUtils.prtln(s, "Nav");
		}
	}

}

