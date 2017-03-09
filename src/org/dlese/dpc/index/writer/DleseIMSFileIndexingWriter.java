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
package org.dlese.dpc.index.writer;

import java.io.*;
import java.util.*;
import java.text.*;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.xml.maps.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.services.mmd.MmdRec;
import org.dlese.dpc.util.*;
import org.apache.lucene.document.*;
import org.dlese.dpc.vocab.*;
import org.dlese.dpc.repository.*;

/**
 *  Creates a Lucene {@link org.apache.lucene.document.Document} from a DLESE-IMS XML source file.<p>
 *
 *  <b>The Lucene {@link org.apache.lucene.document.Document} fields that are created by this class are (in
 *  addition the the ones listed for {@link org.dlese.dpc.index.writer.FileIndexingServiceWriter}):</b> <br>
 *  <br>
 *  <code><b>doctype</b> </code> - Set to 'dlese_ims'. Stored. Note: the actual indexing of this field happens
 *  in the superclass {@link org.dlese.dpc.index.writer.FileIndexingServiceWriter}.<br>
 *  <code><b>additional fields</b> </code> - A number of additional fields are defined. See the Java code for
 *  method {@link #addFrameworkFields(Document, Document)} for details. <br>
 *  <br>
 *
 *
 * @author    John Weatherley, Ryan Deardorff
 */
public class DleseIMSFileIndexingWriter extends ItemFileIndexingWriter {
	private DleseXMLReader map;
	private File sourceFile = null;
	private MetadataVocab vocab = null;


	/**  Create a DleseIMSFileIndexingWriter  */
	public DleseIMSFileIndexingWriter() { }



	/**
	 *  Initialize the XML map prior to processing
	 *
	 * @param  source         The source file being indexed.
	 * @param  existingDoc    A Document that previously existed in the index for this item, if present
	 * @exception  Exception  Thrown if error reading the XML map
	 */
	public void initItem(File source, Document existingDoc)
		 throws Exception {
		sourceFile = source;
		map = new DleseXMLReader(source);
		if (!map.init()) {
			throw new Exception(map.getInitErrorMsg());
		}
		map.setMap();
	}


	/**  Release map resources for GC after processing. */
	protected void destroy() {
		map.destroy();
	}


	/**
	 *  Gets the name of the concrete {@link org.dlese.dpc.index.reader.DocReader} class that is used to read
	 *  this type of {@link org.apache.lucene.document.Document}, which is "ItemDocReader".
	 *
	 * @return    The STring "rg.dlese.dpc.index.reader.ItemDocReader".
	 */
	public String getReaderClass() {
		return "org.dlese.dpc.index.reader.ItemDocReader";
	}


	/**
	 *  Gets a report detailing any errors found in the validation of the data, or null if no error was found.
	 *
	 * @return                Null if no data validation errors were found, otherwise a String that details the
	 *      nature of the error.
	 * @exception  Exception  If error in performing the validation.
	 */
	protected String getValidationReport()
		 throws Exception {
		// Do schema validation here...
		return XMLValidator.validateFile(sourceFile);
	}


	/**
	 *  Gets the docType attribute of the DleseIMSFileIndexingWriter, which is 'dlese_ims.'
	 *
	 * @return    The docType, which is 'dlese_ims.'
	 */
	public final String getDocType() {
		return "dlese_ims";
	}


	/**
	 *  Gets the id attribute of the DleseIMSFileIndexingWriter object
	 *
	 * @return                The id value
	 * @exception  Exception  If an error occurs
	 */
	protected final String[] _getIds() throws Exception {
		return new String[]{(String) map.getValue("ID")};
	}


	/**
	 *  Gets the title attribute of the DleseIMSFileIndexingWriter object
	 *
	 * @return                The title value
	 * @exception  Exception  If an error occurs
	 */
	public final String getTitle() throws Exception {
		return (String) map.getValue("title");
	}


	/**
	 *  Gets the description attribute of the DleseIMSFileIndexingWriter object
	 *
	 * @return                The description value
	 * @exception  Exception  If an error occurs
	 */
	public final String getDescription() throws Exception {
		return (String) map.getValue("description");
	}


	/**
	 *  Returns the items keywords. An empty String or null is acceptable. The String is tokenized, stored and
	 *  indexed under the field key 'keywords' and is also indexed in the 'default' field.
	 *
	 * @return                The keywords String
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getKeywords() throws Exception {
		return null;
	}


	/**
	 *  Returns the items creator's last name. An empty String or null is acceptable. The String is tokenized,
	 *  stored and indexed under the field the 'default' field only.
	 *
	 * @return                The creator's last name String
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getCreatorLastName() throws Exception {
		return null;
	}


	/**
	 *  Returns the MmdRecs for records in other collections that catalog the same resource. Does not include
	 *  myMmdRec.
	 *
	 * @return    null
	 */
	protected MmdRec[] getAssociatedMmdRecs() {
		return null;
	}


	/**
	 *  Returns the MmdRecs for all records associated with this resouce, including myMmdRec.
	 *
	 * @return    null
	 */
	protected MmdRec[] getAllMmdRecs() {
		return null;
	}


	/**
	 *  Returns the MmdRec for this record only.
	 *
	 * @return    null
	 */
	protected MmdRec getMyMmdRec() {
		return null;
	}


	/**
	 *  Returns the items creator's full name. An empty String or null is acceptable. The String is tokenized,
	 *  stored and indexed under the field key 'creator' and is also indexed in the 'default' field.
	 *
	 * @return                Creator's full name
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getCreator() throws Exception {
		return null;
		/* list.clear();
		list = (List) map.getValue("catalogers");
		i = list.iterator();
		while (i.hasNext()) {
			DleseBean.Contributor person = (DleseBean.Contributor) i.next();
			newDoc.add(new Field("contributorrole", person.getValue("role"), Field.Store.YES, Field.Index.ANALYZED));
			//addToDefaultField( person.getValue( "role" ) );
			newDoc.add(new Field("contributorname", person.name(), Field.Store.YES, Field.Index.ANALYZED));
			newDoc.add(new Field("contributor", person.getValue("role") + "=" + person.getValue("lastname"), Field.Store.YES, Field.Index.ANALYZED));
			//addToDefaultField( person.name() );
			newDoc.add(new Field("contributororg", person.getValue("org"), Field.Store.YES, Field.Index.ANALYZED));
			//addToDefaultField( person.getValue( "org" ) );
		} */
	}


	/**
	 *  Returns null.
	 *
	 * @return    null
	 */
	protected String getContent() {
		return null;
	}


	/**
	 *  Returns null.
	 *
	 * @return    null
	 */
	protected String getContentType() {
		return null;
	}


	/**
	 *  Returns the accession status of this record, for example 'accessioned'. The String is tokenized, stored
	 *  and indexed under the field key 'accessionstatus'.
	 *
	 * @return                The accession status.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getAccessionStatus() throws Exception {
		return MmdRec.statusNames[MmdRec.STATUS_ACCESSIONED_DISCOVERABLE];
	}


	/**
	 *  Returns false (not implemented).
	 *
	 * @return                False.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected boolean getHasRelatedResource() throws Exception {
		return false;
	}


	/**
	 *  Returns the IDs of related resources that are cataloged by ID, or null if none are present
	 *
	 * @return                Related resource IDs, or null if none are available
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String[] getRelatedResourceIds() throws Exception {
		return null;
	}


	/**
	 *  Returns the URLs of related resources that are cataloged by URL, or null if none are present
	 *
	 * @return                Related resource URLs, or null if none are available
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String[] getRelatedResourceUrls() throws Exception {
		return null;
	}


	/**
	 *  Gets the url attribute of the DleseIMSFileIndexingWriter object
	 *
	 * @return                The url value
	 * @exception  Exception  If an error occurs
	 */
	public final String[] getUrls() throws Exception {
		return new String[]{(String) map.getValue("resourceURL")};
	}


	/**
	 *  Returns the date used to determine "What's new" in the library, which is null (unknown).
	 *
	 * @return                The what's new date for the item
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected Date getWhatsNewDate() throws Exception {
		return null;
	}


	/**
	 *  Returns the accession date, which is null (unknown).
	 *
	 * @return                The what's new date for the item
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected Date getAccessionDate() throws Exception {
		return null;
	}


	/**
	 *  Returns null.
	 *
	 * @return                null
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected Date getCreationDate() throws Exception {
		return null;
	}


	/**
	 *  Returns null (unknown).
	 *
	 * @return                null.
	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error
	 *      occurs.
	 */
	protected String getWhatsNewType() throws Exception {
		return null;
	}


	/**
	 *  Default and stems fields handled here, so do not index full content.
	 *
	 * @return    False
	 */
	public boolean indexFullContentInDefaultAndStems() {
		return false;
	}


	/**
	 *  Adds custom fields to the index that are unique to DLESE-IMS
	 *
	 * @param  newDoc         The feature to be added to the FrameworkFields attribute
	 * @param  existingDoc    The feature to be added to the FrameworkFields attribute
	 * @exception  Exception  If an error occurs
	 */
	protected final void addFrameworkFields(Document newDoc, Document existingDoc) throws Exception {
		if (vocab == null) {
			return;
		}
		newDoc.add(new Field("cost", (String) map.getValue("cost"), Field.Store.YES, Field.Index.ANALYZED));

		newDoc.add(new Field("copyright", (String) map.getValue("copyright"), Field.Store.YES, Field.Index.ANALYZED));
		addToDefaultField((String) map.getValue("copyright"));

		String indexedList;
		indexedList = listToString((List) map.getValue("subjects"), "topic");
		newDoc.add(new Field(vocab.getTranslatedField("adn", "topic"), indexedList, Field.Store.YES, Field.Index.ANALYZED));
		addToDefaultField(indexedList);

		indexedList = listToString((List) map.getValue("audiences"), "learningcontext");
		newDoc.add(new Field(vocab.getTranslatedField("adn", "learningcontext"), indexedList, Field.Store.YES, Field.Index.ANALYZED));
		addToDefaultField(indexedList);

		indexedList = listToString((List) map.getValue("keywords"), "");
		newDoc.add(new Field("keywords", indexedList, Field.Store.YES, Field.Index.ANALYZED));
		addToDefaultField(indexedList);
		//addToStemsField(indexedList);

		indexedList = listToString((List) map.getValue("geographyStds"), "");
		// change to non-empty when XML done
		newDoc.add(new Field("geographystds", indexedList, Field.Store.YES, Field.Index.ANALYZED));
		//addToDefaultField( indexedList );

		indexedList = listToString((List) map.getValue("scienceStds"), "");
		// change to non-empty string
		newDoc.add(new Field("sciencestds", indexedList, Field.Store.YES, Field.Index.ANALYZED));
		//addToDefaultField( indexedList );

		// This is strange--why does resourcetype not use the listToString() method like the other
		// controlled vocabs?  Because the list returned by map.getValue() isn't a list of strings,
		// but rather a list of DleseBean.ResourceType objects.  But, why?  Inconsistency isn't desirable.
		List list = (List) map.getValue("resourceTypes");
		Iterator i = list.iterator();
		StringBuffer buf = new StringBuffer();
		while (i.hasNext()) {
			DleseBean.ResourceType resource = (DleseBean.ResourceType) i.next();
			buf.append(vocab.getTranslatedValue("adn", "resourcetype", resource.getValue("type"))).append(" ");
		}
		newDoc.add(new Field(vocab.getTranslatedField("adn", "resourcetype"), buf.toString(), Field.Store.YES, Field.Index.ANALYZED));
		addToDefaultField(buf.toString());

		list.clear();
		list = (List) map.getValue("technicalReqs");
		i = list.iterator();
		buf.setLength(0);
		while (i.hasNext()) {
			DleseBean.TechnicalReq req = (DleseBean.TechnicalReq) i.next();
			buf.append(req.getValue("typename")).append(' ');
			buf.append(req.getValue("minversion")).append(' ');
		}
		if (buf.toString().endsWith(", ")) {
			buf.setLength(buf.length() - 2);
		}
		newDoc.add(new Field("technicalrequirement", buf.toString(), Field.Store.YES, Field.Index.ANALYZED));
		addToDefaultField(buf.toString());

		list.clear();
		list = (List) map.getValue("creators");
		i = list.iterator();
		while (i.hasNext()) {
			DleseBean.Contributor person = (DleseBean.Contributor) i.next();
			newDoc.add(new Field("contributorrole", person.getValue("role"), Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(person.getValue("role"));
			newDoc.add(new Field("contributorname", person.name(), Field.Store.YES, Field.Index.ANALYZED));
			newDoc.add(new Field("contributor", person.getValue("role") + "=" + person.getValue("lastname"), Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(person.name());
			newDoc.add(new Field("contributororg", person.getValue("org"), Field.Store.YES, Field.Index.ANALYZED));
			addToDefaultField(person.getValue("org"));
		}

		list.clear();
		list = (List) map.getValue("catalogers");
		i = list.iterator();
		while (i.hasNext()) {
			DleseBean.Contributor person = (DleseBean.Contributor) i.next();
			newDoc.add(new Field("contributorrole", person.getValue("role"), Field.Store.YES, Field.Index.ANALYZED));
			//addToDefaultField( person.getValue( "role" ) );
			newDoc.add(new Field("contributorname", person.name(), Field.Store.YES, Field.Index.ANALYZED));
			newDoc.add(new Field("contributor", person.getValue("role") + "=" + person.getValue("lastname"), Field.Store.YES, Field.Index.ANALYZED));
			//addToDefaultField( person.name() );
			newDoc.add(new Field("contributororg", person.getValue("org"), Field.Store.YES, Field.Index.ANALYZED));
			//addToDefaultField( person.getValue( "org" ) );
		}

		list.clear();
		list = (List) map.getValue("geoReferences");
		i = list.iterator();
		while (i.hasNext()) {
			DleseBean.GeoReference geoReference = (DleseBean.GeoReference) i.next();
			String geotext = (geoReference.getValue("begintime_description") + " "
				 + geoReference.getValue("endtime_description") + " "
				 + geoReference.getValue("place_event_name1") + " "
				 + geoReference.getValue("place_event_name2") + " "
				 + geoReference.getValue("place_event_name3") + " "
				 + geoReference.getValue("place_event_name4") + " "
				 + geoReference.getValue("place_event_name5") + " "
				 + geoReference.getValue("place_event_name6") + " "
				 + geoReference.getValue("place_event_name7") + " "
				 + geoReference.getValue("place_event_name8")
				).trim();
			if (geotext.length() > 0) {
				newDoc.add(new Field("geotext", geotext, Field.Store.YES, Field.Index.ANALYZED));
				addToDefaultField(geotext);
			}
		}
	}


	/**
	 *  Takes a list of values and concatenates them into a single string, seperated by spaces. If
	 *  useVocabMapping is specified, then it is used to map the values to their controlled vocabulary IDs.
	 *
	 * @param  list             A list that contains Strings
	 * @param  useVocabMapping  Is this a controlled vocab? If so, map to the encoded ID using this string as the
	 *      field identifier (pass "" otherwise)
	 * @return                  DESCRIPTION
	 * @exception  Exception
	 */
	private String listToString(List list, String useVocabMapping) throws Exception {
		StringBuffer ret = new StringBuffer();
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				String str = ((String) list.get(i)).trim();
				if (str.length() > 0) {
					if (!useVocabMapping.equals("")) {
						str = vocab.getTranslatedValue("adn", useVocabMapping, str);
					}
					ret.append(str).append(" ");
				}
			}
		}
		return ret.toString();
	}
}

