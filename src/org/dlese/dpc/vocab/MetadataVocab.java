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
package org.dlese.dpc.vocab;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.servlet.http.*;
import javax.servlet.jsp.PageContext;
import org.dlese.dpc.util.strings.StringUtil;
import org.xml.sax.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.dds.action.*;

/**
 *  Interface for classes that manage audience-based metadata controlled
 *  vocabulary UI displays and encoded ID translation to/from metadata
 *  field/value names/xpaths. Current implementations:
 *  <ul>
 *    <li> MetadataVocabOPML</li>
 *    <li> MetadataVocabTermsGroups (<b>Deprecated</b> )</li>
 *  </ul>
 *
 *
 * @author    Ryan Deardorff
 */
public interface MetadataVocab extends org.xml.sax.ContentHandler {

	/**
	 *  Get the list of any errors that have occured
	 *
	 * @return    The errors value
	 */
	public ArrayList getErrors();

	/**
	 *  Adds a feature to the Message attribute of the MetadataVocab object
	 *
	 * @param  msg  The feature to be added to the Message attribute
	 */
	public void addMessage( String msg );

	/**
	 *  Adds a feature to the Error attribute of the MetadataVocab object
	 *
	 * @param  err  The feature to be added to the Error attribute
	 */
	public void addError( String err );

	/**
	 *  Initiate the re-ordering/grouping/labeling of a flat list of metadata
	 *  values in a search response (Services or otherwise) by indicating an
	 *  audience grouping (OPML tree)
	 *
	 * @param  context      JSP page context
	 * @param  metaVersion  metadata version (i.e. "0.6.50")
	 * @param  audience     UI audience, i.e. "community" or "cataloger"
	 * @param  language     UI language, i.e. "en-us"
	 * @param  field        metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  metaFormat   metadata format (i.e. "adn")
	 * @see                 MetadataVocab#setResponseValue(String,PageContext)
	 * @see                 MetadataVocab#setResponseList(String[],PageContext)
	 * @see                 MetadataVocab#setResponseList(ArrayList,PageContext)
	 * @see                 MetadataVocab#getResponseOPML(PageContext)
	 */
	public void setResponseGroup( PageContext context, String metaFormat, String metaVersion,
	                              String audience, String language, String field );

	/**
	 *  Initiate the re-ordering/grouping/labeling of a flat list of metadata
	 *  values in a search response (Services or otherwise) by indicating an
	 *  audience grouping (OPML tree) <b>sans version</b>
	 *
	 * @param  context     JSP page context
	 * @param  audience    UI audience, i.e. "community" or "cataloger"
	 * @param  language    UI language, i.e. "en-us"
	 * @param  field       metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  metaFormat  metadata format (i.e. "adn")
	 * @see                MetadataVocab#setResponseValue(String,PageContext)
	 * @see                MetadataVocab#setResponseList(String[],PageContext)
	 * @see                MetadataVocab#setResponseList(ArrayList,PageContext)
	 * @see                MetadataVocab#getResponseOPML(PageContext)
	 */
	public void setResponseGroup( PageContext context, String metaFormat,
	                              String audience, String language, String field );


	/**
	 *  Adds a single metadata value to the re-ordering/grouping/labeling cache
	 *
	 * @param  value    metadata VALUE encoded ID (i.e. "04") or metadata NAME
	 *      (i.e. "DLESE:Intermediate elementary")
	 * @param  context  JSP page context
	 * @see             MetadataVocab#setResponseGroup(PageContext,String,String,String,String,String)
	 * @see             MetadataVocab#setResponseList(String[],PageContext)
	 * @see             MetadataVocab#setResponseList(ArrayList,PageContext)
	 * @see             MetadataVocab#getResponseOPML(PageContext)
	 */
	public void setResponseValue( String value, PageContext context );

	/**
	 *  Adds an ArrayList of metadata values to the re-ordering/grouping/labeling
	 *  cache
	 *
	 * @param  context  JSP page context
	 * @param  values   List of metadata VALUE encoded ID (i.e. "04") or metadata
	 *      NAME (i.e. "DLESE:Intermediate elementary")
	 * @see             MetadataVocab#setResponseGroup(PageContext,String,String,String,String,String)
	 * @see             MetadataVocab#setResponseValue(String,PageContext)
	 * @see             MetadataVocab#setResponseList(String[],PageContext)
	 * @see             MetadataVocab#getResponseOPML(PageContext)
	 */
	public void setResponseList( ArrayList values, PageContext context );

	/**
	 *  Adds an ArrayList of metadata values to the re-ordering/grouping/labeling
	 *  cache
	 *
	 * @param  context  JSP page context
	 * @param  values   List of metadata VALUE encoded ID (i.e. "04") or metadata
	 *      NAME (i.e. "DLESE:Intermediate elementary")
	 * @see             MetadataVocab#setResponseGroup(PageContext,String,String,String,String,String)
	 * @see             MetadataVocab#setResponseValue(String,PageContext)
	 * @see             MetadataVocab#setResponseList(ArrayList,PageContext)
	 * @see             MetadataVocab#getResponseOPML(PageContext)
	 */
	public void setResponseList( String[] values, PageContext context );


	/**
	 *  Gets the re-ordered/grouped/labeled tree of metadata values from the cache
	 *  created by setResponseGroup()
	 *
	 * @param  context  JSP page context
	 * @return          OPML for the group specified with setResponseGroup() and
	 *      trimmed to the subset indicated by values passed into setResponse()
	 * @see             MetadataVocab#setResponseValue(String,PageContext)
	 * @see             MetadataVocab#setResponseList(String[],PageContext)
	 * @see             MetadataVocab#setResponseList(ArrayList,PageContext)
	 * @see             MetadataVocab#setResponseGroup(PageContext,String,String,String,String,String)
	 */
	public String getResponseOPML( PageContext context );

	/**
	 *  Gets the set of interfaces defined in this instance of the vocabs
	 *
	 * @return    The vocabSystemInterfaces value
	 */
	public Set getVocabSystemInterfaces();

	/**
	 *  Does a vocabulary definition exist for the given encoded field/value Ids?
	 *
	 * @param  fieldId  Encoded vocabulary field Id
	 * @param  valueId  Encoded vocabulary value Id
	 * @return          The fieldValueIdPairExists value
	 */
	public boolean getFieldValueIdPairExists( String fieldId, String valueId );

	/**
	 *  Gets the vocabNodes attribute of the MetadataVocab object
	 *
	 * @param  system
	 * @param  group
	 * @return         The vocabNodes value
	 */
	public ArrayList getVocabNodes( String system,
	                                String group );

	/**
	 *  Returns an HTML SELECT list of the specified part of the vocabulary.
	 *
	 * @param  group       colon-seperated specifier of the part of the vocab
	 *      hierarchy which is to be displayed
	 * @param  size        size (height) of the SELECT list
	 * @param  system
	 * @param  inputState
	 * @return             The vocabSelectList value
	 */
	public String getVocabSelectList( String system,
	                                  String group,
	                                  int size,
	                                  MetadataVocabInputState inputState );

	/**
	 *  Returns an HTML TABLE of CHECKBOX inputs of the specified part of the
	 *  vocabulary.
	 *
	 * @param  group       colon-seperated specifier of the part of the vocab
	 *      hierarchy which is to be displayed
	 * @param  size        how many inputs to display before starting a new column
	 *      in the table
	 * @param  tdWidth     value inserted into the width param of the TD tag
	 * @param  skipTopRow  Should the top row of checkboxes (next to All | Clear)
	 *      be skipped?
	 * @param  system
	 * @param  inputState
	 * @return             the HTML code
	 */
	public String getVocabCheckboxes( String system,
	                                  String group,
	                                  int size,
	                                  String tdWidth,
	                                  boolean skipTopRow,
	                                  MetadataVocabInputState inputState );

	/**
	 *  Returns a SINGLE HTML CHECKBOX input of the specified part of the
	 *  vocabulary.
	 *
	 * @param  groupTop    Top-level vocab group
	 * @param  value       vocab value
	 * @param  label       UI label
	 * @param  inputState
	 * @return             the HTML code
	 */
	public String getVocabCheckbox( String groupTop,
	                                String value,
	                                String label,
	                                MetadataVocabInputState inputState );

	/**
	 *  Generates an absolutely-positioned JavaScript Tree Menu (collapsable flyout
	 *  hierarchy) of the specified part of the vocabulary
	 *
	 * @param  group     colon-seperated specifier of the part of the vocab
	 *      hierarchy which is to be displayed
	 * @param  system
	 * @param  page
	 * @param  language
	 * @return           the Javascript code defining the menu
	 */
	public String getVocabTreeMenu( String system,
	                                String language,
	                                String group,
	                                PageContext page );


	/**
	 *  Generates HTML HIDDEN inputs of the specified part of the vocabulary.
	 *
	 * @param  group       colon-seperated specifier of the part of the vocab
	 *      hierarchy which is to be displayed
	 * @param  system
	 * @param  inputState
	 * @return             the HTML code
	 */
	public String getVocabHiddenInputs( String system,
	                                    String group,
	                                    MetadataVocabInputState inputState );

	/**
	 *  Given a cache (Map) of vocab values, this method returns a list of those
	 *  values in the order that they are defined in their groups file.
	 *
	 * @param  cache
	 * @param  system
	 * @param  group
	 * @return         The orderedCacheValues value
	 */
	public ArrayList getCacheValuesInOrder( String system, String group, Map cache );

	/**
	 *  Gets the vocabFieldIds attribute of the MetadataVocab object
	 *
	 * @return    The vocabFieldIds value
	 */
	public ArrayList getVocabFieldIds();

	/**
	 *  Invoked when XML parsing completes
	 */
	public void doneLoading();

	/**
	 *  Description of the Method
	 *
	 * @param  system
	 * @param  fieldName
	 * @param  valueName
	 * @return
	 * @deprecated        As of MetadataUI v1.0, replaced by <a
	 *      href="#getVocabNode(java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String)">
	 *      getVocabNode()</a>
	 */
	public VocabNode findVocabNode( String system, String fieldName, String valueName );

	/**
	 *  Gets a VocabNode for the given field/value pair
	 *
	 * @param  metaFormat  metadata format (i.e. "adn")
	 * @param  audience    UI audience, i.e. "community" or "cataloger"
	 * @param  language    UI language, i.e. "en-us"
	 * @param  fieldName   vocab field
	 * @param  valueName   vocab value
	 * @return             VocabNode for the vocab
	 */
	public VocabNode getVocabNode( String metaFormat, String audience, String language, String fieldName, String valueName );

	/**
	 *  Gets the vocabNodes attribute of the MetadataVocab object
	 *
	 * @param  metaFormat
	 * @param  audience
	 * @param  language
	 * @param  field
	 * @return             The vocabNodes value
	 */
	public ArrayList getVocabNodes( String metaFormat, String audience, String language, String field );

	/**
	 *  Gets the vocabNodes attribute of the MetadataVocab object
	 *
	 * @param  metaFormat
	 * @param  audience
	 * @param  language
	 * @param  field
	 * @param  group
	 * @return             The vocabNodes value
	 */
	public ArrayList getVocabNodes( String metaFormat, String audience, String language, String field, String group );

	/**
	 *  Get the most recently loaded metadata format version number
	 *
	 * @param  metaFormat  metadata format (i.e. "adn")
	 * @return             The current (most recently loaded) version for the given
	 *      format
	 */
	public String getCurrentVersion( String metaFormat );

	/*
	 *  ===========================================================================================
	 *  v2.0 methods, deprecating the following:
	 *  <ul>
	 *  <li> getFieldValueSystemId()</li>
	 *  <li> getFieldSystemId()</li>
	 *  <li> getMetaNameOfId()</li>
	 *  <li> getUiLabelOf()</li>
	 *  <li> getUiLabelOfSystemIds()</li>
	 *  <li> getUiLabelOfFieldId()</li>
	 *  </ul>
	 *
	 */
	/**
	 *  Gets the translated FIELD NAME/ID of the given FIELD NAME/ID
	 *
	 * @param  field          metadata FIELD encoded ID (i.e. "gr") or metadata
	 *      NAME (i.e. "gradeRange")
	 * @param  metaFormat     metadata format (i.e. "adn")
	 * @param  metaVersion    metadata version (i.e. "0.6.50")
	 * @return                The FIELD NAME/ID as translated from input FIELD
	 *      NAME/ID, i.e. "gradeRange"
	 * @exception  Exception
	 */
	public String getTranslatedField( String metaFormat, String metaVersion, String field ) throws Exception;

	/**
	 *  Gets the translated FIELD NAME/ID of the given FIELD NAME/ID
	 *
	 * @param  field          metadata FIELD encoded ID (i.e. "gr") or metadata
	 *      NAME (i.e. "gradeRange")
	 * @param  metaFormat     metadata format (i.e. "adn")
	 * @return                The FIELD NAME/ID as translated from input FIELD
	 *      NAME/ID, i.e. "gradeRange"
	 * @exception  Exception
	 */
	public String getTranslatedField( String metaFormat, String field ) throws Exception;

	/**
	 *  Gets the translated FIELD XPATH of the given FIELD NAME/ID
	 *
	 * @param  field          metadata FIELD encoded ID (i.e. "gr") or metadata
	 *      NAME (i.e. "gradeRange")
	 * @param  metaFormat     metadata format (i.e. "adn")
	 * @param  metaVersion    metadata version (i.e. "0.6.50")
	 * @return                The FIELD XPATH as translated from input FIELD
	 *      NAME/ID
	 * @exception  Exception
	 */
	public String getTranslatedFieldPath( String metaFormat, String metaVersion, String field ) throws Exception;

	/**
	 *  Gets the translated FIELD XPATH of the given FIELD NAME/ID
	 *
	 * @param  field          metadata FIELD encoded ID (i.e. "gr") or metadata
	 *      NAME (i.e. "gradeRange")
	 * @param  metaFormat     metadata format (i.e. "adn")
	 * @return                The FIELD XPATH as translated from input FIELD
	 *      NAME/ID
	 * @exception  Exception
	 */
	public String getTranslatedFieldPath( String metaFormat, String field ) throws Exception;

	/**
	 *  Gets the translated VALUE NAME/ID of the given FIELD+VALUE NAMEs/IDs
	 *
	 * @param  field          metadata FIELD encoded ID (i.e. "gr") or metadata
	 *      NAME (i.e. "gradeRange")
	 * @param  value          metadata VALUE encoded ID (i.e. "04") or metadata
	 *      NAME (i.e. "DLESE:Intermediate elementary")
	 * @param  metaFormat     metadata format (i.e. "adn")
	 * @param  metaVersion    metadata version (i.e. "0.6.50")
	 * @return                The VALUE NAME/ID as translated from input
	 *      FIELD+VALUE NAMEs/IDs
	 * @exception  Exception
	 */
	public String getTranslatedValue( String metaFormat, String metaVersion, String field, String value ) throws Exception;

	/**
	 *  Gets the translated VALUE NAME/ID of the given FIELD+VALUE NAMEs/IDs
	 *
	 * @param  field          metadata FIELD encoded ID (i.e. "gr") or metadata
	 *      NAME (i.e. "gradeRange")
	 * @param  value          metadata VALUE encoded ID (i.e. "04") or metadata
	 *      NAME (i.e. "DLESE:Intermediate elementary")
	 * @param  metaFormat     metadata format (i.e. "adn")
	 * @return                The VALUE NAME/ID as translated from input
	 *      FIELD+VALUE NAMEs/IDs
	 * @exception  Exception
	 */
	public String getTranslatedValue( String metaFormat, String field, String value ) throws Exception;

	/**
	 *  Gets the UI label associated with the given FIELD+VALUE NAMES/IDS
	 *
	 * @param  metaFormat   metadata format (i.e. "adn")
	 * @param  metaVersion  metadata version (i.e. "0.6.50")
	 * @param  field        metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  value        metadata VALUE encoded ID (i.e. "04") or metadata NAME
	 *      (i.e. "DLESE:Intermediate elementary")
	 * @param  abbrev       get the abbreviated version of the label?
	 * @param  audience     UI audience, i.e. "community" or "cataloger"
	 * @param  language     UI language, i.e. "en-us"
	 * @return              The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiValueLabel( String metaFormat, String metaVersion,
	                               String audience, String language,
	                               String field, String value, boolean abbrev );

	/**
	 *  Gets the non-abbreviated UI label associated with the given FIELD+VALUE
	 *  NAMES/IDS
	 *
	 * @param  metaFormat   metadata format (i.e. "adn")
	 * @param  metaVersion  metadata version (i.e. "0.6.50")
	 * @param  field        metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  value        metadata VALUE encoded ID (i.e. "04") or metadata NAME
	 *      (i.e. "DLESE:Intermediate elementary")
	 * @param  audience     UI audience, i.e. "community" or "cataloger"
	 * @param  language     UI language, i.e. "en-us"
	 * @return              The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiValueLabel( String metaFormat, String metaVersion,
	                               String audience, String language,
	                               String field, String value );

	/**
	 *  Gets the UI label associated with the given FIELD+VALUE NAMES/IDS, using
	 *  the <b> current</b> or most recently loaded metadata format version number
	 *
	 * @param  metaFormat  metadata format (i.e. "adn")
	 * @param  field       metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  value       metadata VALUE encoded ID (i.e. "04") or metadata NAME
	 *      (i.e. "DLESE:Intermediate elementary")
	 * @param  audience    UI audience, i.e. "community" or "cataloger"
	 * @param  language    UI language, i.e. "en-us"
	 * @param  abbrev      get the abbreviated version of the label?
	 * @return             The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiValueLabel( String metaFormat,
	                               String audience, String language,
	                               String field, String value, boolean abbrev );

	/**
	 *  Gets the uiValueLabel attribute of the MetadataVocab object
	 *
	 * @param  audience
	 * @param  language
	 * @param  field
	 * @param  value
	 * @param  abbrev
	 * @return           The uiValueLabel value
	 */
	public String getUiValueLabel( String audience, String language,
	                               String field, String value, boolean abbrev );

	/**
	 *  Gets the UI label associated with the given FIELD NAME/ID
	 *
	 * @param  metaFormat   metadata format (i.e. "adn")
	 * @param  metaVersion  metadata version (i.e. "0.6.50")
	 * @param  abbrev       get the abbreviated version of the label?
	 * @param  audience     UI audience, i.e. "community" or "cataloger"
	 * @param  language     UI language, i.e. "en-us"
	 * @param  field        metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @return              The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiFieldLabel( String metaFormat, String metaVersion,
	                               String audience, String language,
	                               String field, boolean abbrev );

	/**
	 *  Gets the non-abbreviated UI label associated with the given FIELD NAME/ID
	 *
	 * @param  metaFormat   metadata format (i.e. "adn")
	 * @param  metaVersion  metadata version (i.e. "0.6.50")
	 * @param  audience     UI audience, i.e. "community" or "cataloger"
	 * @param  language     UI language, i.e. "en-us"
	 * @param  field        metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @return              The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiFieldLabel( String metaFormat, String metaVersion,
	                               String audience, String language,
	                               String field );

	/**
	 *  Gets the UI label associated with the given FIELD NAME/ID, using the <b>
	 *  current</b> or most recently loaded metadata format version number
	 *
	 * @param  metaFormat  metadata format (i.e. "adn")
	 * @param  field       metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  audience    UI audience, i.e. "community" or "cataloger"
	 * @param  language    UI language, i.e. "en-us"
	 * @param  abbrev      get the abbreviated version of the label?
	 * @return             The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiFieldLabel( String metaFormat,
	                               String audience, String language,
	                               String field, boolean abbrev );

	/**
	 *  Gets the uiFieldLabel attribute of the MetadataVocab object
	 *
	 * @param  audience
	 * @param  language
	 * @param  field
	 * @param  abbrev
	 * @return           The uiFieldLabel value
	 */
	public String getUiFieldLabel( String audience, String language,
	                               String field, boolean abbrev );

	/**
	 *  Gets the "display" attribute value for the given field/value vocab
	 *
	 * @param  metaFormat   metadata format (i.e. "adn")
	 * @param  metaVersion  metadata version (i.e. "0.6.50")
	 * @param  field        metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  value        metadata VALUE encoded ID (i.e. "04") or metadata NAME
	 *      (i.e. "DLESE:Intermediate elementary")
	 * @param  audience     UI audience, i.e. "community" or "cataloger"
	 * @param  language     UI language, i.e. "en-us"
	 * @return              The uiValueDisplay value
	 */
	public String getUiValueDisplay( String metaFormat, String metaVersion,
	                                 String audience, String language,
	                                 String field, String value );

	/**
	 *  Gets the "display" attribute value for the given field/value vocab using
	 *  the CURRENT metadata framework version
	 *
	 * @param  metaFormat  metadata format (i.e. "adn")
	 * @param  field       metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  value       metadata VALUE encoded ID (i.e. "04") or metadata NAME
	 *      (i.e. "DLESE:Intermediate elementary")
	 * @param  audience    UI audience, i.e. "community" or "cataloger"
	 * @param  language    UI language, i.e. "en-us"
	 * @return             The uiValueDisplay value
	 */
	public String getUiValueDisplay( String metaFormat,
	                                 String audience, String language,
	                                 String field, String value );

	/**
	 *  Gets the OPML for a given format/version/audience/language
	 *
	 * @param  metaVersion            metadata version (i.e. "0.6.50")
	 * @param  audience               UI audience, i.e. "community" or "cataloger"
	 * @param  language               UI language, i.e. "en-us"
	 * @param  field                  metadata FIELD encoded ID (i.e. "gr") or
	 *      metadata NAME (i.e. "gradeRange")
	 * @param  metaFormat             metadata format (i.e. "adn")
	 * @param  includeXmlDeclaration  should the &lt;?xml...?&gt; declaration tag
	 *      be included with the output?
	 * @return                        OPML for the given format/audience
	 */
	public String getOPML( String metaFormat, String metaVersion, String audience, String language, String field, boolean includeXmlDeclaration );

	/**
	 *  Gets the OPML for a given format/audience/language, using the <b>current
	 *  </b> or most recently loaded version
	 *
	 * @param  audience               UI audience, i.e. "community" or "cataloger"
	 * @param  language               UI language, i.e. "en-us"
	 * @param  field                  metadata FIELD encoded ID (i.e. "gr") or
	 *      metadata NAME (i.e. "gradeRange")
	 * @param  metaFormat             metadata format (i.e. "adn")
	 * @param  includeXmlDeclaration  should the &lt;?xml...?&gt; declaration tag
	 *      be included with the output?
	 * @return                        OPML for the given format/audience
	 */
	public String getOPML( String metaFormat, String audience, String language, String field, boolean includeXmlDeclaration );

	/**
	 *  Gets the OPML for a given format/version/audience/language <b>without the
	 *  XML declaration tag</b>
	 *
	 * @param  metaVersion  metadata version (i.e. "0.6.50")
	 * @param  audience     UI audience, i.e. "community" or "cataloger"
	 * @param  language     UI language, i.e. "en-us"
	 * @param  field        metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  metaFormat   metadata format (i.e. "adn")
	 * @return              OPML for the given format/audience
	 */
	public String getOPML( String metaFormat, String metaVersion, String audience, String language, String field );

	/**
	 *  Gets the OPML for a given format/audience/language, using the <b>current
	 *  </b> or most recently loaded version <b>without the XML declaration tag</b>
	 *
	 * @param  audience    UI audience, i.e. "community" or "cataloger"
	 * @param  language    UI language, i.e. "en-us"
	 * @param  field       metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  metaFormat  metadata format (i.e. "adn")
	 * @return             OPML for the given format/audience
	 */
	public String getOPML( String metaFormat, String audience, String language, String field );

	// _____________________________________ NO LONGER SUPPORTED _________________________________

	/**
	 *  Gets the metadata value or field name of the given encoded field and value
	 *  Ids. Pass "" (empty string) for valueId to have it return the metadata
	 *  field name (instead of value).
	 *
	 * @param  system   Vocabulary framework/version/audience/language, i.e.
	 *      "adn/0.6.50/community/en-us" in v2.x (or "dds.descr.en-us" in v1.x)
	 * @param  fieldId  Encoded vocabulary field Id
	 * @param  valueId  Encoded vocabulary value Id
	 * @return          The metaNameOfId value
	 * @deprecated      As of MetadataUI v1.0, replaced by <a
	 *      href="#getTranslatedValue(java.lang.String,%20java.lang.String,%20java.lang.String)">
	 *      getTranslatedValue()</a>
	 * @see             MetadataVocab#getTranslatedField(String,String,String,String,String)
	 * @see             MetadataVocab#getTranslatedValue(String,String,String,String,String,String)
	 */
	public String getMetaNameOfId( String system, String fieldId, String valueId );

	/**
	 *  Gets the encoded value Id of the given metadata field/value pair
	 *
	 * @param  field          Metadata field name
	 * @param  value          Metadata value name
	 * @return                The encoded value Id
	 * @exception  Exception
	 * @deprecated            As of MetadataUI v1.0, replaced by <a
	 *      href="#getTranslatedValue(java.lang.String,%20java.lang.String,%20java.lang.String)">
	 *      getTranslatedValue()</a>
	 */
	public String getFieldValueSystemId( String field,
	                                     String value ) throws Exception;

	/**
	 *  Gets the encoded field Id of the given metadata field
	 *
	 * @param  field          Metadata field name
	 * @return                The encoded field Id
	 * @exception  Exception
	 * @deprecated            As of MetadataUI v1.0, replaced by <a
	 *      href="#getTranslatedField(java.lang.String,%20java.lang.String)">
	 *      getTranslatedField()</a>
	 */
	public String getFieldSystemId( String field ) throws Exception;

	/**
	 *  Gets the UI label of the given metadata field/value pair
	 *
	 * @param  system         Vocabulary system/interface/language trio, i.e.
	 *      "adn/0.6.50/community/en-us"
	 * @param  metadataField  Metadata field name
	 * @param  metadataValue  Metadata value name
	 * @param  abbreviated    Return the abbreviated form of the UI label?
	 * @return                The user interface label associated with the given
	 *      vocabulary value
	 * @deprecated            As of MetadataUI v1.0, replaced by <a
	 *      href="#getUiValueLabel(java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String,%20boolean)">
	 *      getUiValueLabel()</a>
	 */
	public String getUiLabelOf( String system,
	                            String metadataField,
	                            String metadataValue,
	                            boolean abbreviated );

	/**
	 *  Gets the full (non-abbreviated) UI label of the given metadata field/value
	 *  pair
	 *
	 * @param  system         Vocabulary system/interface/language trio, i.e.
	 *      "adn/0.6.50/community/en-us"
	 * @param  metadataField  Metadata field name
	 * @param  metadataValue  Metadata value name
	 * @return                The user interface label associated with the given
	 *      vocabulary value
	 * @deprecated            As of MetadataUI v1.0, replaced by <a
	 *      href="#getUiValueLabel(java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String,%20boolean)">
	 *      getUiValueLabel()</a>
	 */
	public String getUiLabelOf( String system,
	                            String metadataField,
	                            String metadataValue );

	/**
	 *  Gets the uiLabelOfFieldId attribute of the MetadataVocab object
	 *
	 * @param  fieldId  Description of the Parameter
	 * @return          The uiLabelOfFieldId value
	 * @deprecated      As of MetadataUI v1.0, replaced by <a
	 *      href="#getUiFieldLabel(java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String,%20boolean)">
	 *      getUiFieldLabel()</a>
	 */
	public String getUiLabelOfFieldId( String fieldId );

	/**
	 *  Gets the UI label of the given encoded field/value Id pair
	 *
	 * @param  system         Vocabulary system/interface/language trio, i.e.
	 *      "dds.default.en-us"
	 * @param  systemFieldId  Encoded field Id
	 * @param  systemValueId  Encoded value Id
	 * @param  abbreviated    Return the abbreviated form of the UI label?
	 * @return                The user interface label associated with the given
	 *      vocabulary value
	 * @deprecated            As of MetadataUI v1.0, replaced by <a
	 *      href="#getUiValueLabel(java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String,%20boolean)">
	 *      getUiValueLabel()</a>
	 */
	public String getUiLabelOfSystemIds( String system,
	                                     String systemFieldId,
	                                     String systemValueId,
	                                     boolean abbreviated );

	/**
	 *  Gets the full (non-abbreviated) UI label of the given encoded field/value
	 *  Id pair
	 *
	 * @param  system         Vocabulary system/interface/language trio, i.e.
	 *      "dds.default.en-us"
	 * @param  systemFieldId  Encoded field Id
	 * @param  systemValueId  Encoded value Id
	 * @return                The user interface label associated with the given
	 *      vocabulary value
	 * @deprecated            As of MetadataUI v1.0, replaced by <a
	 *      href="#getUiValueLabel(java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String,%20boolean)">
	 *      getUiValueLabel()</a>
	 */
	public String getUiLabelOfSystemIds( String system,
	                                     String systemFieldId,
	                                     String systemValueId );

	/**
	 *  Gets the metadata format associated with the given field identifier (either
	 *  encoded ID or PATH)
	 *
	 * @param  field
	 * @return        The metaFormatOfField value
	 */
	public String getMetaFormatOfField( String field );

	/**
	 *  Gets the topLevelAbbrevLabelOf attribute of the MetadataVocab object
	 *
	 * @param  system
	 * @param  metadataField
	 * @param  systemFieldId
	 * @param  systemValueId
	 * @return                The topLevelAbbrevLabelOf value
	 */
	public String getTopLevelAbbrevLabelOf( String system,
	                                        String metadataField,
	                                        String systemFieldId,
	                                        String systemValueId );

	/**
	 *  Return stored messages
	 *
	 * @return    The messages value
	 */
	public ArrayList getMessages();

	/**
	 *  Log a message
	 *
	 * @param  msg
	 */
	public void reportMessage( String msg );

	/**
	 *  Log an error
	 *
	 * @param  err
	 */
	public void reportError( String err );
}


