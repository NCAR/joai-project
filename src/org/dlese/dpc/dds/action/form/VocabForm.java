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
package org.dlese.dpc.dds.action.form;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import org.dlese.dpc.dds.action.form.DDSViewResourceForm;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.index.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.URLEncoder;
import org.dlese.dpc.dds.action.DDSQueryAction;

/**
 *  A Struts Form bean for handling controlled vocabulary displays.
 *
 * @author    Ryan Deardorff
 */
public class VocabForm extends ActionForm implements Serializable {
	/**  NOT YET DOCUMENTED */
	protected MetadataVocab vocab = null;
	/**  NOT YET DOCUMENTED */
	protected String field = ""; // which field are we currently dealing with?
	/**  NOT YET DOCUMENTED */
	protected String value = ""; // which value (of field) are we currently dealing with?
	/**  NOT YET DOCUMENTED */
	protected String metaFormat = "adn"; // which metadata format (i.e. "dlese_collect") are we dealing with?
	/**  NOT YET DOCUMENTED */
	protected String audience = ""; // UI audience
	/**  NOT YET DOCUMENTED */
	protected String language = ""; // UI language


	/**
	 *  Constructor for the VocabForm object
	 *
	 * @param  vocab  The new vocab value
	 */
	public void setVocab(MetadataVocab vocab) {
		init(vocab);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  vocab
	 */
	private void init(MetadataVocab vocab) {
		this.vocab = vocab;
		audience = servlet.getServletContext().getInitParameter("metadataVocabAudience");
		language = servlet.getServletContext().getInitParameter("metadataVocabLanguage");
	}


	/**
	 *  Sets the vocabInterface attribute of the VocabForm object
	 *
	 * @param  system  The new vocabInterface value
	 * @deprecated     As of MetadataUI v1.0, replaced by new constructor
	 */
	public void setVocabInterface(String system) {
		System.err.println("setVocabInterface() in VocabForm is deprecated");
	}


	/**
	 *  Gets the UI audience attribute of the VocabForm object
	 *
	 * @return    The audience value
	 */
	public String getAudience() {
		return audience;
	}


	/**
	 *  Gets the UI language attribute of the VocabForm object
	 *
	 * @return    The language value
	 */
	public String getLanguage() {
		return language;
	}


	/**
	 *  Sets the vocabulary field attribute of the VocabActionForm object
	 *
	 * @param  field       The new field value
	 * @param  metaFormat  The new field value
	 */
	public void setField(String metaFormat, String field) {
		this.metaFormat = metaFormat;
		this.field = field;
	}


	/**
	 *  Sets the field attribute of the VocabForm object
	 *
	 * @param  field  The new field value
	 */
	public void setField(String field) {
		this.field = field;
	}


	/**
	 *  Sets the metaFormat attribute of the VocabForm object
	 *
	 * @param  metaFormat  The new metaFormat value
	 */
	public void setMetaFormat(String metaFormat) {
		this.metaFormat = metaFormat;
	}


	/**
	 *  gets the vocabulary field attribute of the VocabActionForm object
	 *
	 * @return    field The new field value
	 */
	public String getField() {
		return field;
	}


	/**
	 *  Gets the UI label of the current vocabulary
	 *
	 * @return    The fieldLabel value
	 */
	public String getFieldLabel() {
		String ret = "";
		try {
			ret = vocab.getUiFieldLabel(metaFormat, audience, language, field, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}


	/**
	 *  Gets the field encoded ID of the current vocabulary
	 *
	 * @return    The fieldId value
	 */
	public String getFieldId() {
		String ret = "";
		try {
			ret = vocab.getTranslatedField(metaFormat, field);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}


	/**
	 *  Sets the vocabulary value (last half of field/value pair)
	 *
	 * @param  value  The new value value
	 */
	public void setValue(String value) {
		this.value = value;
	}


	/**
	 *  Gets the current vocabulary term as a VocabNode, or if unavailable, an untranslated vocabulary term as a
	 *  VocabNode, which simply echos the vocab ID in the id, name, label and labelabbrev fields.
	 *
	 * @return    The vocabTerm value
	 * @see       #getIsVocabTermAvailable
	 */
	public VocabNode getVocabTerm() {
		if (vocab == null) {
			init((MetadataVocab) servlet.getServletContext().getAttribute("MetadataVocab"));
		}
		VocabNode ret = doGetVocabTerm();
		if (ret == null)
			ret = doGetVocabTermUntranslated();

		return ret;
	}


	/**
	 *  True if there is a vocab entry for this field/value, false if not. If true, {@link #getVocabTerm} will
	 *  return the translated vocabs, if false it will return the untranslated values.
	 *
	 * @return    The isVocabTermAvailable value
	 * @see       #getVocabTerm
	 */
	public boolean getIsVocabTermAvailable() {
		return (doGetVocabTerm() != null);
	}


	/**
	 *  Gets the current vocabulary term as a VocabNode, or null if no vocab entry for this field is available.
	 *
	 * @return    The vocabTerm value
	 * @see       #getVocabTermUntranslated
	 */
	private final VocabNode doGetVocabTerm() {
		if (vocab == null)
			init((MetadataVocab) servlet.getServletContext().getAttribute("MetadataVocab"));
		// Will return null if not available:
		return vocab.getVocabNode(metaFormat, audience, language, field, value);
	}


	/**
	 *  Gets an untranslated vocabulary term as a VocabNode, which simply echos the vocab ID in the id, name,
	 *  label and labelabbrev fields.
	 *
	 * @return    The vocabTerm value
	 */
	private VocabNode doGetVocabTermUntranslated() {

		VocabNode ret = new VocabNodeTermsGroups(value, false, 0);
		ret.setId(value);
		ret.setLabel(value);
		ret.setLabelAbbrev(value);

		return ret;
	}


	private String getVocabNodeString(VocabNode node) {
		if (node == null)
			return "null";
		return "name: " + node.getName() + " id: " + node.getId() + " label: " + node.getLabel();
	}


	/**
	 *  Gets the vocab attribute of the VocabActionForm object
	 *
	 * @return    The vocab value
	 */
	public final MetadataVocab getVocab() {
		return vocab;
	}


	/**
	 *  Gets the vocabList attribute of the VocabActionForm object
	 *
	 * @param  field
	 * @return        The vocabList value
	 */
	public ArrayList getVocabList(String field) {
		return vocab.getVocabNodes(metaFormat, audience, language, field);
	}


	/**
	 *  Gets the vocabList attribute of the VocabActionForm object
	 *
	 * @return    The vocabList value
	 */
	public ArrayList getVocabList() {
		if (vocab == null) {
			System.err.println("Error in VocabForm.getVocabList(): vocab is null");
			return new ArrayList();
		}
		return getVocabList(vocab.getVocabNodes(metaFormat, audience, language, field));
	}


	/**
	 *  Flattens the hierarchy of the given vocab list, and returns it as a single array.
	 *
	 * @param  vocList
	 * @return          The vocabList value
	 */
	private ArrayList getVocabList(ArrayList vocList) {
		ArrayList ret = new ArrayList();
		for (int i = 0; i < vocList.size(); i++) {
			VocabNode addNode = (VocabNode) vocList.get(i);
			ArrayList sublist = addNode.getSubList();
			ret.add(addNode);
			if (sublist.size() > 0) {
				ret.addAll(getVocabList(sublist));
			}
		}
		return ret;
	}
}


