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
package org.dlese.dpc.serviceclients.remotesearch.reader;

import org.dlese.dpc.serviceclients.remotesearch.*;
import org.dlese.dpc.serviceclients.webclient.*;

import java.io.*;
import java.util.*;

import org.dom4j.Node;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import org.dlese.dpc.vocab.*;
import org.dlese.dpc.xml.*;
import org.dlese.dpc.index.ResultDoc;
import org.dlese.dpc.index.ResultDocList;

/**
 *  Description of the Class
 *
 *@author    ostwald
 <p>$Id: ADNItemDocReader.java,v 1.9 2010/07/14 00:19:27 jweather Exp $
 */
public class ADNItemDocReader {

	/**
	 *  Description of the Field
	 */
	protected MetadataVocab vocab = null;
	/**
	 *  Description of the Field
	 */
	protected static boolean debug = true;
	/**
	 *  Description of the Field
	 */
	protected String id = "";
	/**
	 *  Description of the Field
	 */
	protected String collection = "";
	/**
	 *  Description of the Field
	 */
	protected String readerType = "ADNItemDocReader";
	/**
	 *  Description of the Field
	 */
	protected Document doc = null;
	//String content = null;
	/**
	 *  Description of the Field
	 */
	protected String[] EMPTY_ARRAY = new String[]{};
	/**
	 *  Description of the Field
	 */
	protected String DEFAULT = "";
	/**
	 *  Description of the Field
	 */
	protected ArrayList EMPTY_LIST = new ArrayList();

	/*
		the following fields are defined for ItemDocReader and therefore are defined here
		for compatability. at the end of this file are the corresponding accessors
		and utilities for manipulating these fields, although for now they have been deactivated
		by short-circuting initAnnosByType
	*/
	/**
	 *  Description of the Field
	 */
	protected ResultDocList associatedItemResultDocs = null;
	/**
	 *  Description of the Field
	 */
	protected ResultDocList displayableAssociatedItemResultDocs = null;
	/**
	 *  Description of the Field
	 */
	protected ResultDocList allItemResultDocs = null;
	/**
	 *  Description of the Field
	 */
	protected ResultDocList annotationResultDocs = null;
	/**
	 *  Description of the Field
	 */
	protected boolean associatedItemsInitialized = false;
	/**
	 *  Description of the Field
	 */
	protected boolean displayableAssociatedItemsInitialized = false;
	/**
	 *  Description of the Field
	 */
	protected boolean allItemsInitialized = false;
	/**
	 *  Description of the Field
	 */
	protected boolean annotationDocReadersInitialized = false;
	/**
	 *  Description of the Field
	 */
	protected ArrayList missingAssociatedItemIds = null;
	/**
	 *  Description of the Field
	 */
	protected String multiRecordStatus = null;
	/**
	 *  Description of the Field
	 */
	protected Document multiDoc = null;
	/**
	 *  Description of the Field
	 */
	protected HashMap completedAnnosByType = null;
	/**
	 *  Description of the Field
	 */
	protected ArrayList completedAnnos = null;
	/**
	 *  Description of the Field
	 */
	protected HashMap inProgressAnnosByStatus = null;
	/**
	 *  Description of the Field
	 */
	protected int numCompletedAnnos = 0;
	/**
	 *  Description of the Field
	 */
	protected int numInProgressAnnos = 0;


	/**
	 *  ADNItemDocReader constructor requiring an itemRecordDoc
	 *
	 *@param  id             Description of the Parameter
	 *@param  collection     Description of the Parameter
	 *@param  itemRecordDoc  Description of the Parameter
	 *@param  vocab          Description of the Parameter
	 */
	public ADNItemDocReader(String id, String collection, Document itemRecordDoc, MetadataVocab vocab) {
		this.id = id;
		this.collection = collection;
		this.doc = itemRecordDoc;
		String rootXPath = "/DDSWebService/GetRecord/metadata/itemRecord";
		this.vocab = vocab;
	}


	/**
	 *  Constructor for the ADNItemDocReader object
	 *
	 *@param  id        Description of the Parameter
	 *@param  response  Description of the Parameter
	 */
	public ADNItemDocReader(String id, GetRecordResponse response) {
		this(id, response, null);
	}


	/**
	 *  parse a GetRecordResponse (a response from a GetRecord request), to create
	 *  an ADNItemDocReader. This constructor called from RemoteResultDoc and
	 *  SchemEditAction
	 *
	 *@param  id        Description of the Parameter
	 *@param  response  Description of the Parameter
	 *@param  vocab     Description of the Parameter
	 */
	public ADNItemDocReader(String id, GetRecordResponse response, MetadataVocab vocab) {
		prtln("about construct reader for " + id);
		this.id = id;
		this.vocab = vocab;
		try {
			collection = response.getDocument().valueOf("/DDSWebService/GetRecord/head/collection/@key");
			doc = response.getItemRecord();
		} catch (WebServiceClientException wse) {
			prtln("caught WebServiceClientException: " + wse.getMessage());
			return;
		} catch (Exception e) {
			prtln("ADNItemDocReader() unable to parse getRecordResponse header");
			readerType = "";
			// jsp pages should check for readerType = ADNItemDocReader before displaying
			return;
		}

		// now extract the itemRecord element
	}


	// ----------------------- begin getters --------------------

	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	public boolean hasDoc() {
		return (doc != null);
	}


	/**
	 *  Gets the String 'ItemDocReader,' which is the key that describes this
	 *  reader type. This may be used in (Struts) beans to determine which type of
	 *  reader is available for a given search result and thus what data is
	 *  available for display in the UI. The reader type determines which getter
	 *  methods are available.
	 *
	 *@return    The String 'ItemDocReader'.
	 */
	public String getReaderType() {
		return readerType;
	}


	/**
	 *  Sets the readerType attribute of the ADNItemDocReader object
	 *
	 *@param  s  The new readerType value
	 */
	public void setReaderType(String s) {
		readerType = s;
	}


	/**
	 *  Gets the metadataPrefix attribute of the ADNItemDocReader object
	 *
	 *@return    The metadataPrefix value
	 */
	public String getMetadataPrefix() {
		return "adn";
	}


	/**
	 *  gets the ID of the item
	 *
	 *@return    the id value
	 */
	public String getId() {
		return id;
	}


	/**
	 *  Gets the url attribute of the
	 *
	 *@return    The url value
	 */
	public String getUrl() {
		String xpath = "itemRecord/technical/online/primaryURL";
		return getElementText(xpath);
	}


	/**
	 *  Gets the URL for this resource, truncated if it is very long
	 *
	 *@return    The url value
	 */
	public String getUrlTruncated() {
		final int CUTOFF = 80;
		String t = getUrl();

		if (t == null) {
			return DEFAULT;
		}
		else {
			if (t.length() > CUTOFF + 10) {
				int ind = t.indexOf("/", CUTOFF);
				if (ind > -1 && ind < CUTOFF + 10) {
					t = t.substring(0, ind + 1) + "...";
				}
				else {
					t = t.substring(0, CUTOFF) + "...";
				}
			}
			return t;
		}
	}


	/**
	 *  Gets the title attribute of the ADNItemDocReader object
	 *
	 *@return    The title value
	 */
	public String getTitle() {
		String xpath = "/itemRecord/general/title";
		return getElementText(xpath);
	}


	/**
	 *  Gets the description attribute of the ADNItemDocReader object
	 *
	 *@return    The description value
	 */
	public String getDescription() {
		String xpath = "/itemRecord/general/description";
		return getElementText(xpath);
	}


	/**
	 *  Gets the collection attribute of the ADNItemDocReader object
	 *
	 *@return    The collection value
	 */
	public String getCollection() {
		return collection;
	}


	/**
	 *  Gets the collection key associated with this record, for example 01.
	 *  Assumes the set key has been encoded using the vocab manager and that there
	 *  is only one collection associated with this item.
	 *
	 *@return    The collection for which this item belogs.
	 */
	public String getCollectionKey() {
		if (vocab != null &&
			collection != null && 
			collection.trim().length() > 0) {
			try {
				// prtln ("about to call vocab.getFieldValueSystemId with " + collectionKey);
				String useVocabMapping = "key";
				return vocab.getFieldValueSystemId(useVocabMapping, getCollection());
			} catch (Throwable e) {
				prtln("makeSystemCollectionKey failed with " + collection + ":\n" + e);
			}
		}
		return null;
	}


	/**
	 *  Gets the collection keys associated with this record, for example {01,02}.
	 *  Assumes the set key has been encoded using the vocab manager and that there
	 *  are more than one collections associated with this item.
	 *
	 *@return    The collection for which this item belogs.
	 */
	public String[] getCollectionKeys() {
		String t = getCollectionKey().trim();
		if (t == null || t.length() == 0) {
			return null;
		}
		return t.split("\\+");
	}

	public List getMirrorUrls () {
		String xpath = "/itemRecord/technical/online/mirrorURLs/mirrorURL";
		List nodes = getElements (xpath);
		
		List mirrorUrls = new ArrayList ();
		
		for (Iterator i=nodes.iterator();i.hasNext();) {
			Element mElement = (Element)i.next();
			String url = mElement.getText();
			if (url != null && url.trim().length() > 0)
				mirrorUrls.add (url.trim());
		}
		// return (String []) mirrorUrls.toArray(new String [] {});
		return mirrorUrls;
	}

	/**
	 *  Gets the collection keys for all enabled collections that are associated
	 *  with this resource. Associated annotations such as CRS and JESSE that are
	 *  identified as collections are included. Disabled collections are not
	 *  included.
	 *
	 *@return    The associatedCollectionKeys value
	 */
	public ArrayList getAssociatedCollectionKeys() {
		return EMPTY_LIST;
	}


	// -- MultiElementTexts - returning a String [] of values
	// ------------ most (ALL?) of these will have to be converted to system ids

	/**
	 *  Gets the gradeRanges for this item (as vocab keys)
	 *
	 *@return    The gradeRanges value
	 */
	public String[] getGradeRanges() {
		String xpath = "/itemRecord/educational/audiences/audience/gradeRange";
		try {
			return getFieldContent(getMultiElementTexts(xpath), "gradeRange");
		} catch (Exception e) {
			prtln("getGradeRanges() " + e);
			return EMPTY_ARRAY;
		}
	}


	/**
	 *  Gets the multiGradeRanges attribute of the ADNItemDocReader object
	 *
	 *@return    The multiGradeRanges value
	 */
	public String[] getMultiGradeRanges() {
		return getGradeRanges();
	}


	/**
	 *  Gets the resourceTypes attribute of the ADNItemDocReader object
	 *
	 *@return    The resourceTypes value
	 */
	public String[] getResourceTypes() {
		String xpath = "/itemRecord/educational/resourceTypes/resourceType";
		try {
			return getFieldContent(getMultiElementTexts(xpath), "resourceType");
		} catch (Exception e) {
			prtln("getResourceTypes() " + e);
			return EMPTY_ARRAY;
		}
	}


	/**
	 *  Gets the multiResourceTypes attribute of the ADNItemDocReader object
	 *
	 *@return    The multiResourceTypes value
	 */
	public String[] getMultiResourceTypes() {
		return getResourceTypes();
	}


	/**
	 *  Gets the subjects attribute of the ADNItemDocReader object
	 *
	 *@return    The subjects value
	 */
	public String[] getSubjects() {
		String xpath = "/itemRecord/general/subjects/subject";
		try {
			return getFieldContent(getMultiElementTexts(xpath), "subject");
		} catch (Exception e) {
			prtln("getSubjects() " + e);
			return EMPTY_ARRAY;
		}
	}


	/**
	 *  Gets the multiSubjects attribute of the ADNItemDocReader object
	 *
	 *@return    The multiSubjects value
	 */
	public String[] getMultiSubjects() {
		return getSubjects();
	}


	/**
	 *  Gets the contentStandards attribute of the ADNItemDocReader object
	 *
	 *@return    The contentStandards value
	 */
	public String[] getContentStandards() {
		String xpath = "/itemRecord/educational/contentStandards/contentStandard";
		try {
			return getFieldContent(getMultiElementTexts(xpath), "contentStandard");
		} catch (Exception e) {
			prtln("getContentStandards() " + e);
			return EMPTY_ARRAY;
		}
	}


	/**
	 *  Returns true if the item has one or more related resource, false otherwise.
	 *
	 *@return    True if the item has one or more related resource, false
	 *      otherwise.
	 */
	protected boolean getHasRelatedResource() {
		try {
			if (getElements("/itemRecord/relations/relation/idEntry/@entry").size() > 0 ||
					getElements("/itemRecord/relations/relation/urlEntry/@url").size() > 0) {
				return true;
			}
		} catch (Exception e) {
			prtln("getHasRelatedResource() " + e);
		}
		return false;
	}


	// ------------------------------ utility ------------------------------
	/**
	 *  Gets the vocab encoded keys for the given values as a String Array. Keys
	 *  are found by VocabUtils.getFieldContent and converted from '+' delimited
	 *  string into an Array.
	 *
	 *@param  values           The valuse to encode.
	 *@param  useVocabMapping  The mapping to use, for example "contentStandards".
	 *@return                  The encoded vocab keys.
	 */
	protected String[] getFieldContent(String[] values, String useVocabMapping) {
		String fc = null;
		try {
			fc = VocabUtils.getFieldContent("adn", values, useVocabMapping, vocab);
			// prtln ("VocabUtils.getFieldContent() returned\n\t" + fc);
		} catch (Exception e) {
			prtln("getFieldContent() " + e);
			fc = DEFAULT;
		}
		return fc.split("\\+");
	}


	/**
	 *  Gets the encoded vocab key for the given content.
	 *
	 *@param  value            The value to encode.
	 *@param  useVocabMapping  The vocab mapping to use, for example
	 *      "contentStandard".
	 *@return                  The encoded value.
	 */
	protected String getFieldContent(String value, String useVocabMapping) {
		try {
			return VocabUtils.getFieldContent("adn", value, useVocabMapping, vocab);
		} catch (Exception e) {
			prtln("getFieldContent() error: " + e);
		}
		return DEFAULT;
	}


	/**
	 *  get all Elements satisfying the given xpath
	 *
	 *@param  xpath  an XPath
	 *@return        a List of all elements satisfying given XPath, or null
	 */
	protected List getElements(String xpath) {
		try {
			return doc.selectNodes(xpath);
		} catch (Throwable e) {
			prtln("getElements() failed with " + xpath + ": " + e);
		}
		return null;
	}


	/**
	 *  Gets a single Element satisfying given XPath. If more than one Element is
	 *  found, the first is returned (and a msg is printed)
	 *
	 *@param  xpath  an XPath
	 *@return        a dom4j Element
	 */
	protected Element getElement(String xpath) {
		List list = getElements(xpath);
		if ((list == null) || (list.size() == 0)) {
			prtln("getElement() did not find element for " + xpath);
			return null;
		}
		if (list.size() > 1) {
			prtln("getElement() found mulitple elements for " + xpath + " (returning first)");
		}
		return (Element) list.get(0);
	}


	/**
	 *  return the Text of a Element satisfying the given XPath.
	 *
	 *@param  xpath  an XPath\
	 *@return        Text of Element or empty String if no Element is found
	 */
	protected String getElementText(String xpath) {
		Element e = getElement(xpath);
		String val = DEFAULT;
		try {
			val = e.getText();
		} catch (Throwable t) {
			prtln("getElementText() failed with " + xpath + "\n" + e);
		}
		if (val == null) {
			return DEFAULT;
		}
		else {
			return val.trim();
		}
	}


	/**
	 *  get the values of all elements matching an XPath
	 *
	 *@param  xpath  Description of the Parameter
	 *@return        Array of values
	 */
	protected String[] getMultiElementTexts(String xpath) {
		ArrayList vals = new ArrayList();
		List list = getElements(xpath);
		for (Iterator i = list.iterator(); i.hasNext(); ) {
			Element e = (Element) i.next();
			vals.add(e.getText());
		}
		return (String[]) vals.toArray(new String[]{});
	}


	/**
	 *  Gets the valueOf attribute of the ADNItemDocReader object
	 *
	 *@param  xpath  Description of the Parameter
	 *@return        The valueOf value
	 */
	public String getValueOf(String xpath) {
		String val = DEFAULT;
		try {
			val = doc.valueOf(xpath);
		} catch (Throwable e) {
			prtln("getValueOf() failed with " + xpath + "\n" + e);
		}
		if (val == null) {
			return DEFAULT;
		}
		else {
			return val.trim();
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	public static void prtlnArray(String[] s) {
		for (int i = 0; i < s.length; i++) {
			prtln("\t" + s[i]);
		}
		return;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	public static void prtln(String s) {
		if (debug) {
			System.out.println("ADNItemDocReader: " + s);
		}
	}


	/* ---------------- Annotations display, etc. --------------------------
	   these methods are from ItemDocReader, and return the empty list (EMPTY_LIST) or
	   "false" where appropriate. at this point in time the information required to
	   calculate real results for these methods is not available via the DDS Web Services.
	   When the required information becomes available, these methods can be fleshed out
	*/
	/**
	 *  Gets the ResultDocs for all annotations that refer to this resource.
	 *
	 *@return    The ResultDocs value
	 */
	public ResultDocList getAnnotationResultDocs() {
		return null;
		/*
		try {
			if (annotationDocReadersInitialized) {
				return annotationResultDocs;
			}
			annotationDocReadersInitialized = true;
			if (recordDataService == null || !hasAnnotations()) {
				return null;
			}
			String[] ids = getId().split("\\s+");
			ArrayList allIds = new ArrayList();
			for (int i = 0; i < ids.length; i++) {
				allIds.add(ids[i]);
			}
			ids = getAssociatedIds();
			if (ids != null) {
				for (int i = 0; i < ids.length; i++) {
					allIds.add(ids[i]);
				}
			}
			//prtln("getAnnotationResultDocs() index query");
			annotationResultDocs = recordDataService.getDleseAnnoResultDocs((String[]) allIds.toArray(new String[]{}));
			return annotationResultDocs;
		} catch (Throwable e) {
			//e.printStackTrace();
			return null;
		}
*/
	}


	/**
	 *  Gets the IDs of records that refer to the same resource.
	 *
	 *@return    The associated IDs value.
	 */
	public String[] getAssociatedIds() {
		return null;
	}


	/**
	 *  Determines whether this item has at least one completed annotation.
	 *
	 *@return    True if this item has one or more completed annotation, false
	 *      otherwise.
	 */
	public boolean hasCompletedAnno() {
		initAnnosByType();
		return !completedAnnosByType.isEmpty();
	}


	/**
	 *  Gets the hasCompletedAnno attribute of the ItemDocReader object
	 *
	 *@return    The hasCompletedAnno value
	 */
	public String getHasCompletedAnno() {
		if (hasCompletedAnno()) {
			return "true";
		}
		else {
			return "false";
		}
	}


	/**
	 *  Gets the numCompletedAnnos attribute of the ItemDocReader object
	 *
	 *@return    The numCompletedAnnos value
	 */
	public String getNumCompletedAnnos() {
		initAnnosByType();
		return Integer.toString(numCompletedAnnos);
	}


	/**
	 *  Gets the numInProgressAnnos attribute of the ItemDocReader object
	 *
	 *@return    The numInProgressAnnos value
	 */
	public String getNumInProgressAnnos() {
		initAnnosByType();
		return Integer.toString(numInProgressAnnos);
	}


	/**
	 *  Gets the numTextAnnosInProgress attribute of the ItemDocReader object
	 *
	 *@return    The numTextAnnosInProgress value
	 */
	public String getNumTextAnnosInProgress() {
		initAnnosByType();
		return Integer.toString(numTextInProgress);
	}


	/**
	 *  Gets the numAudioAnnosInProgress attribute of the ItemDocReader object
	 *
	 *@return    The numAudioAnnosInProgress value
	 */
	public String getNumAudioAnnosInProgress() {
		initAnnosByType();
		return Integer.toString(numAudioInProgress);
	}


	/**
	 *  Gets the numGraphicalAnnosInProgress attribute of the ItemDocReader object
	 *
	 *@return    The numGraphicalAnnosInProgress value
	 */
	public String getNumGraphicalAnnosInProgress() {
		initAnnosByType();
		return Integer.toString(numGraphicalInProgress);
	}


	/**
	 *  Determines whether this item has a completed annotataion of the given type.
	 *
	 *@param  type  The annotation type
	 *@return       True if this item has a completed annotataion of the given
	 *      type.
	 */
	public boolean hasCompletedAnnoOfType(String type) {
		initAnnosByType();
		return completedAnnosByType.containsKey(type);
	}


	/**
	 *  Gets a list of all completed annotataions for this item of the given type.
	 *
	 *@param  type  The annotation type.
	 *@return       A list of all completed annotataions for this item of the given
	 *      type, or an empty list.
	 */
	public ArrayList getCompletedAnnosOfType(String type) {
		initAnnosByType();
		ArrayList list = (ArrayList) completedAnnosByType.get(type);
		if (list == null) {
			list = new ArrayList();
		}
		return list;
	}


	/**
	 *  Determines whether the item has an annotation in progress.
	 *
	 *@return    'true' if the item has an annotation in progress, otherwise
	 *      'false'.
	 */
	public String getHasInProgressAnno() {
		if (hasInProgressAnno()) {
			return "true";
		}
		else {
			return "false";
		}
	}


	/**
	 *  Determines whether the item has an annotation in progress.
	 *
	 *@return    True if the item has an annotation in progress, otherwise false.
	 */
	public boolean hasInProgressAnno() {
		initAnnosByType();
		return !inProgressAnnosByStatus.isEmpty();
	}


	/**
	 *  Determines whether the item has an annotation in progress with the given
	 *  status.
	 *
	 *@param  status  Annotation status.
	 *@return         True if the item has an annotation in progress of the given
	 *      status, otherwise false.
	 */
	public boolean hasInProgressAnnoOfStatus(String status) {
		initAnnosByType();
		return inProgressAnnosByStatus.containsKey(status);
	}


	/**
	 *  Gets all in-progress annotations for this item that have the given status.
	 *
	 *@param  status  Annotation status.
	 *@return         All annotations for this item that have the given status, or
	 *      empty list.
	 */
	public ArrayList getInProgressAnnosOfStatus(String status) {
		initAnnosByType();
		ArrayList list = (ArrayList) inProgressAnnosByStatus.get(status);
		if (list == null) {
			list = new ArrayList();
		}
		return list;
	}


	/**
	 *  Description of the Field
	 */
	protected int numAudioInProgress = 0;
	/**
	 *  Description of the Field
	 */
	protected int numGraphicalInProgress = 0;
	/**
	 *  Description of the Field
	 */
	protected int numTextInProgress = 0;


	/**
	 *  Description of the Method
	 */
	protected void initAnnosByType() {
		if (completedAnnosByType != null) {
			return;
		}
		completedAnnosByType = new HashMap();
		inProgressAnnosByStatus = new HashMap();
		completedAnnos = new ArrayList();
		ResultDocList annoResults = getAnnotationResultDocs();
		if (annoResults == null) {
			return;
		}
		/*
		ArrayList annoList;
		boolean isCompleted;
		for (int i = 0; i < annoResults.length; i++) {
			DleseAnnoDocReader annoDocReader = (DleseAnnoDocReader) annoResults[i].getDocReader();
			String annoType = annoDocReader.getType();
			String annoStatus = annoDocReader.getStatus();
			isCompleted = annoDocReader.isCompleted();
			if (isCompleted) {
				numCompletedAnnos++;
				annoList = (ArrayList) completedAnnosByType.get(annoType);
				completedAnnos.add(annoDocReader);
			}
			else {
				if (annoDocReader.isTextInProgress()) {
					numTextInProgress++;
				}
				if (annoDocReader.isAudioInProgress()) {
					numAudioInProgress++;
				}
				if (annoDocReader.isGraphicalInProgress()) {
					numGraphicalInProgress++;
				}
				numInProgressAnnos++;
				annoList = (ArrayList) inProgressAnnosByStatus.get(annoStatus);
			}
			if (annoList == null) {
				annoList = new ArrayList();
			}
			annoList.add(annoDocReader);
			if (isCompleted) {
				completedAnnosByType.put(annoType, annoList);
			}
			else {
				inProgressAnnosByStatus.put(annoStatus, annoList);
			}
		}
*/
	}


	/**
	 *  Gets the textAnnosInProgress attribute of the ItemDocReader object
	 *
	 *@return    The textAnnosInProgress value
	 */
	public ArrayList getTextAnnosInProgress() {
		return getInProgressAnnosOfStatus("Text annotation in progress");
	}


	/**
	 *  Gets the audioAnnosInProgress attribute of the ItemDocReader object
	 *
	 *@return    The audioAnnosInProgress value
	 */
	public ArrayList getAudioAnnosInProgress() {
		return getInProgressAnnosOfStatus("Audio annotation in progress");
	}


	/**
	 *  Gets the graphicalAnnosInProgress attribute of the ItemDocReader object
	 *
	 *@return    The graphicalAnnosInProgress value
	 */
	public ArrayList getGraphicalAnnosInProgress() {
		return getInProgressAnnosOfStatus("Graphical annotation in progress");
	}


	/**
	 *  Gets the completedAnnos attribute of the ItemDocReader object
	 *
	 *@return    The completedAnnos value
	 */
	public ArrayList getCompletedAnnos() {
		initAnnosByType();

		return completedAnnos;
	}


	/**
	 *  Gets the completedReviews attribute of the ItemDocReader object
	 *
	 *@return    The completedReviews value
	 */
	public ArrayList getCompletedReviews() {
		return getCompletedAnnosOfType("Review");
	}


	/**
	 *  Gets the completedTeachingTips attribute of the ItemDocReader object
	 *
	 *@return    The completedTeachingTips value
	 */
	public ArrayList getCompletedTeachingTips() {
		return getCompletedAnnosOfType("Teaching tip");
	}


	/**
	 *  Gets the completedEditorSummaries attribute of the ItemDocReader object
	 *
	 *@return    The completedEditorSummaries value
	 */
	public ArrayList getCompletedEditorSummaries() {
		return getCompletedAnnosOfType("Editor's summary");
	}


	/**
	 *  Gets the completedChallengingSituations attribute of the ItemDocReader
	 *  object
	 *
	 *@return    The completedChallengingSituations value
	 */
	public ArrayList getCompletedChallengingSituations() {
		return getCompletedAnnosOfType("Information on challenging teaching and learning situations");
	}


	/**
	 *  Gets the completedAverageScores attribute of the ItemDocReader object
	 *
	 *@return    The completedAverageScores value
	 */
	public ArrayList getCompletedAverageScores() {
		return getCompletedAnnosOfType("Average scores of aggregated indices");
	}


	/**
	 *  Gets the completedAdvice attribute of the ItemDocReader object
	 *
	 *@return    The completedAdvice value
	 */
	public ArrayList getCompletedAdvice() {
		return getCompletedAnnosOfType("Advice");
	}


	/**
	 *  Gets the completedAnnotation attribute of the ItemDocReader object
	 *
	 *@return    The completedAnnotation value
	 */
	public ArrayList getCompletedAnnotation() {
		return getCompletedAnnosOfType("Annotation");
	}


	/**
	 *  Gets the completedBias attribute of the ItemDocReader object
	 *
	 *@return    The completedBias value
	 */
	public ArrayList getCompletedBias() {
		return getCompletedAnnosOfType("Bias");
	}


	/**
	 *  Gets the completedChange attribute of the ItemDocReader object
	 *
	 *@return    The completedChange value
	 */
	public ArrayList getCompletedChange() {
		return getCompletedAnnosOfType("Change");
	}


	/**
	 *  Gets the completedComment attribute of the ItemDocReader object
	 *
	 *@return    The completedComment value
	 */
	public ArrayList getCompletedComment() {
		return getCompletedAnnosOfType("Comment");
	}


	/**
	 *  Gets the completedEducationalStandard attribute of the ItemDocReader object
	 *
	 *@return    The completedEducationalStandard value
	 */
	public ArrayList getCompletedEducationalStandard() {
		return getCompletedAnnosOfType("Educational standard");
	}


	/**
	 *  Gets the completedExample attribute of the ItemDocReader object
	 *
	 *@return    The completedExample value
	 */
	public ArrayList getCompletedExample() {
		return getCompletedAnnosOfType("Example");
	}


	/**
	 *  Gets the completedExplanation attribute of the ItemDocReader object
	 *
	 *@return    The completedExplanation value
	 */
	public ArrayList getCompletedExplanation() {
		return getCompletedAnnosOfType("Explanation");
	}


	/**
	 *  Gets the completedQuestion attribute of the ItemDocReader object
	 *
	 *@return    The completedQuestion value
	 */
	public ArrayList getCompletedQuestion() {
		return getCompletedAnnosOfType("Question");
	}


	/**
	 *  Gets the completedSeeAlso attribute of the ItemDocReader object
	 *
	 *@return    The completedSeeAlso value
	 */
	public ArrayList getCompletedSeeAlso() {
		return getCompletedAnnosOfType("See also");
	}

}

