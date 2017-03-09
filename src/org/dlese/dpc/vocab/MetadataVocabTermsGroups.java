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

/*
 *  TO DO:
 *  -Email notification to error handler
 *  -Database indexing (for faster queries, only effects builds hooked to mySQL)
 *  -XML Validation
 *  -Clear member variables used only for parsing purposes when parsing complete (save memory)
 */
/**
 *  Facilitates interface representation of metadata vocabulary. <p>
 *
 *  Controlled vocabulary is stored in XML files and read into the hierarchical
 *  class data structure via a SAX reader. Various methods are provided for
 *  representing the vocabulary through a Web interface (HTML/Javascript) using
 *  JSP scriptlet calls or custom tags (see {@link org.dlese.dpc.vocab.tags}).
 *  <p>
 *
 *  Methods that might be usefull outside of any particular UI are as follows:
 *
 *  <ul>
 *    <li> <tt>getFieldSystemId</tt> </li>
 *    <li> <tt>getFieldValueIdPairExists</tt> </li>
 *    <li> <tt>getFieldValueSystemId</tt> </li>
 *    <li> <tt>getMetaNameOfId</tt> </li>
 *    <li> <tt>getUiLabelOf</tt> </li>
 *    <li> <tt>getUiLabelOfSystemIds</tt> </li>
 *  </ul>
 *  <p>
 *
 *  Method descriptions often use the following terms:
 *  <ul>
 *    <li> <tt><b>Field</b> </tt> - The first half of the field/value vocabulary
 *    scheme </li>
 *    <li> <tt><b>Value</b> </tt> - The second half of the field/value
 *    vocabulary scheme </li>
 *    <li> <tt><b>Metadata name</b> </tt> - The descriptive name given to a
 *    vocabulary field or value</li>
 *    <li> <tt><b>Encoded Id</b> </tt> - The compact unique identifier that the
 *    management system assigns to a particular vocabulary field or value
 *    (sometimes referred to as "system Id")</li>
 *  </ul>
 *  <p>
 *
 *  The last two terms are often used in combination with the first two. For
 *  example, "Encoded field Id" refers to the unique system Id of a vocabulary
 *  field. <p>
 *
 *  Most public methods take an initial parameter called "system", which is a
 *  period-seperated trio that corresponds to the system/interface/language
 *  attributes of the XML files. To retreive values associated with the english
 *  language version of the default DDS interface, for example, one would pass
 *  the string "dds.default.en-us".<p>
 *
 *  Almost all of the public methods that produce lists for UI display take a
 *  parameter named "group". This is a colon-seperated identifier of the spot
 *  within the vocabulary hierarchy which is being requested for display.
 *  Passing "topic:geography" would return a representation of <i>only</i> the
 *  metadata entries that fall under the "geography" sub-heading in the "topic"
 *  category. The colon is not always necessary, however--passing simply "topic"
 *  will return a represenation of <i>all</i> entries in the "topic" category.
 *
 * @author    Ryan Deardorff
 */
public class MetadataVocabTermsGroups implements MetadataVocab, org.xml.sax.ContentHandler, Serializable {
	// Base data structures:
	protected VocabList vocab = new VocabList();
	private VocabList current = vocab;
	private VocabNode currentNode;
	private HashMap luceneQuery = new HashMap();
	private String newNodeName = "";
	private String newNodeId = "";
	private StringUtil stringUtil = new StringUtil();

	// XML parser variables:
	private Locator saxLocator;
	private String currentSAXElementName = "";
	private String currentSAXElementSystem = "";                   // System = system.interface.language
	private String currentSAXElementLanguage = "";                 // i.e. "english"
	private String currentVocabFieldId;
	private String currentVocabFieldName;
	private boolean processingUiGroup = false;
	private boolean encounteredErrorInElement = false;
	// Store XML in case it needs to be re-written with new Ids:
	private StringBuffer outputXML = new StringBuffer();
	private String xmlFile;
	private boolean rewriteXML = false;
	private boolean startSavingXML = false;
	private boolean parsingDefinitions = false;
	private String currentHeader = "";
	private String currentFooter = "";
	private String currentTopLevelAbbrevLabel = "";
	private int uiGroupLevelAt = 0;

	// For tracking changes via database (or other means):
	private DLESEMetadataVocabTracker tracker;
	private HashMap fieldSystemId = new HashMap();
	private HashMap fieldValueSystemId = new HashMap();
	private HashMap uiSystems = new HashMap();                     // keep track of all UI "system" attributes encountered
	private HashMap uiSystemFields = new HashMap();                // keep track of system fields for UIs
	private HashMap abbreviatedLabels = new HashMap();             // store system/fieldId/valueId label abbreviations
	private HashMap abbreviatedLabelsMeta = new HashMap();         // abbreviations keyed by metadata names
	private HashMap uiLabelOf = new HashMap();                     // metadata name -> UI label mapping
	private HashMap uiLabelOfSystemIds = new HashMap();            // encoded value Ids -> UI label mapping
	private HashMap uiLabelOfFieldIds = new HashMap();             // encoded field Ids -> UI label mapping
	private HashMap topUiAbbrevLabelOf = new HashMap();            // top level of hierarchy label
	private HashMap metaNameOfId = new HashMap();                  // encoded Ids -> metadata name
	private HashMap fieldValueIdPairExists = new HashMap();        // does the given field/value Id pair exist in XML?
	private ArrayList vocabFieldIds = new ArrayList();             // list of all defined vocab fields (i.e. 'gr', 're', etc.)

	// Messages/errors
	private ArrayList messages = new ArrayList();                  // messages displayed in admin
	private ArrayList errors = new ArrayList();                    // errors displayed in admin
	private ArrayList vocabDiffs = new ArrayList();                // check vocabTermsGroups return values
	// against vocabOPML values

	private HttpURLConnection httpCon = null;

	public MetadataVocabOPML vocabCompare = null;

	/**
	 *  Constructor for the MetadataVocab object
	 *
	 * @param  sqlDriver
	 * @param  sqlURL
	 * @param  sqlUser
	 * @param  sqlPassword
	 * @param  vocabTextFile
	 */
	public MetadataVocabTermsGroups( String sqlDriver,
	                                 String sqlURL,
	                                 String sqlUser,
	                                 String sqlPassword,
	                                 String vocabTextFile ) {
		tracker = new DLESEMetadataVocabTracker( this, sqlDriver, sqlURL, sqlUser, sqlPassword, vocabTextFile );
	}

	/**
	 *  Constructor for the MetadataVocab object
	 */
	public MetadataVocabTermsGroups() { }


	/**
	 *  Adds a feature to the Error attribute of the MetadataVocabOPML object
	 *
	 * @param  err  The feature to be added to the Error attribute
	 */
	public void addError( String err ) {
		errors.add( err );
		System.out.println( err );
	}

	/**
	 *  Adds a feature to the Message attribute of the MetadataVocabOPML object
	 *
	 * @param  msg  The feature to be added to the Message attribute
	 */
	public void addMessage( String msg ) {
		messages.add( msg );
	}

	/**
	 *  Gets the messages attribute of the MetadataVocab object
	 *
	 * @return    The messages value
	 */
	public synchronized ArrayList getMessages() {
		return messages;
	}

	/**
	 *  Gets the errors attribute of the MetadataVocab object
	 *
	 * @return    The errors value
	 */
	public synchronized ArrayList getErrors() {
		return errors;
	}

	/**
	 *  Gets the set of UI system interfaces (i.e., "dds.descr.en-us") that this
	 *  vocabulary is defined for
	 *
	 * @return    The vocabSystemInterfaces value
	 */
	public synchronized Set getVocabSystemInterfaces() {
		return uiSystems.keySet();
	}

	/**
	 *  Gets the metadata value or field name of the given encoded field and value
	 *  Ids. Pass "" (empty string) for valueId to have it return the metadata
	 *  field name (instead of value).
	 *
	 * @param  system   Vocabulary system/interface/language trio, i.e.
	 *      "dds.default.en-us"
	 * @param  fieldId  Encoded vocabulary field Id
	 * @param  valueId  Encoded vocabulary value Id
	 * @return          The metaNameOfId value
	 */
	public synchronized String getMetaNameOfId( String system, String fieldId, String valueId ) {
		if ( vocabCompare != null ) {
			String newValue = checkNull( vocabCompare.getMetaNameOfId( "adn", fieldId, valueId ) );
			String oldValue = checkNull( (String)metaNameOfId.get( system + fieldId + valueId ) );
			alertVocabDiff( "getMetaNameOfId( " + fieldId + ", " + valueId + " )", oldValue, newValue );
		}
		return (String)metaNameOfId.get( system + fieldId + valueId );
	}

	/**
	 *  Description of the Method
	 *
	 * @param  val
	 * @return
	 */
	private String checkNull( String val ) {
		if ( val == null ) {
			return "null";
		}
		return val;
	}

	/**
	 *  Description of the Method
	 *
	 * @param  meth
	 * @param  newVal
	 * @param  oldVal
	 */
	private void alertVocabDiff( String meth, String oldVal, String newVal ) {
		if ( oldVal.equals( newVal ) ) {
			reportMessage( "Success (" + meth + ")" );
		}
		else {
			reportError( "FAILURE (" + meth + "): " + oldVal + " vs. " + newVal );
		}
	}

	/**
	 *  Does a vocabulary definition exist for the given encoded field/value Ids?
	 *
	 * @param  fieldId  Encoded vocabulary field Id
	 * @param  valueId  Encoded vocabulary value Id
	 * @return          The fieldValueIdPairExists value
	 */
	public synchronized boolean getFieldValueIdPairExists( String fieldId, String valueId ) {
		boolean ret = false;
		if ( fieldValueIdPairExists.get( fieldId + "=" + valueId ) != null ) {
			ret = true;
		}
		if ( vocabCompare != null ) {
			String newValue = new Boolean( vocabCompare.getFieldValueIdPairExists( fieldId, valueId ) ).toString();
			String oldValue = new Boolean( ret ).toString();
			alertVocabDiff( "getFieldValueIdPairExists", oldValue, newValue );
		}
		return ret;
	}

	/**
	 *  Gets the encoded value Id of the given metadata field/value pair
	 *
	 * @param  field          Metadata field name
	 * @param  value          Metadata value name
	 * @return                The encoded value Id
	 * @exception  Exception
	 */
	public synchronized String getFieldValueSystemId( String field,
	                                                  String value ) throws Exception {
		String ret = "";
		if ( ( value != null ) && !value.equals( "" ) ) {
			ret = (String)fieldValueSystemId.get( field + value );
			if ( ret == null ) {
				ret = "";
				String err = "Vocabulary Manager: getFieldValueSystemId is NULL for field/value pair "
					 + field + "=" + value;
				reportError( err );
				throw new Exception( err );
			}
		}
		return ret;
	}

	/**
	 *  Gets the encoded field Id of the given metadata field
	 *
	 * @param  field          Metadata field name
	 * @return                The encoded field Id
	 * @exception  Exception
	 */
	public synchronized String getFieldSystemId( String field ) throws Exception {
		String ret = (String)fieldSystemId.get( field );
		if ( ret == null ) {
			String err = "Vocabulary Manager: getFieldSystemId is NULL for field " + field;
			reportError( err );
			throw new Exception( err );
		}
		return ret;
	}

	/**
	 *  Gets the UI label of the given metadata field/value pair
	 *
	 * @param  system         Vocabulary system/interface/language trio, i.e.
	 *      "dds.default.en-us"
	 * @param  metadataField  Metadata field name
	 * @param  metadataValue  Metadata value name
	 * @param  abbreviated    Return the abbreviated form of the UI label?
	 * @return                The user interface label associated with the given
	 *      vocabulary value
	 */
	public synchronized String getUiLabelOf( String system,
	                                         String metadataField,
	                                         String metadataValue,
	                                         boolean abbreviated ) {
		String ret = null;
		if ( metadataValue == null ) {
			metadataValue = "";
		}
		if ( abbreviated ) {
			// Fudge: abbrevs apply to all system/interfaces in a given language:
			int ind = system.indexOf( "." );
			ind = system.indexOf( ".", ind + 1 );
			String language = system.substring( ind + 1, system.length() );
			ret = (String)abbreviatedLabelsMeta.get( language + metadataField + metadataValue );
		}
		if ( !abbreviated || ( ret == null ) ) {
			ret = (String)uiLabelOf.get( system + metadataField + metadataValue );
		}
		if ( ret == null ) {
			reportError( "Vocabulary Manager: getUiLabelOf is NULL for (" + system + metadataField + metadataValue + ")" );
			ret = " ";
		}
		return ret;
	}

	/**
	 *  <a name="getUiLabelOf"></a> Gets the full (non-abbreviated) UI label of the
	 *  given metadata field/value pair
	 *
	 * @param  system         Vocabulary system/interface/language trio, i.e.
	 *      "dds.default.en-us"
	 * @param  metadataField  Metadata field name
	 * @param  metadataValue  Metadata value name
	 * @return                The user interface label associated with the given
	 *      vocabulary value
	 */
	public synchronized String getUiLabelOf( String system,
	                                         String metadataField,
	                                         String metadataValue ) {
		return getUiLabelOf( system, metadataField, metadataValue, false );
	}

	/**
	 *  Gets the uiLabelOfFieldId attribute of the MetadataVocab object
	 *
	 * @param  fieldId  Description of the Parameter
	 * @return          The uiLabelOfFieldId value
	 */
	public synchronized String getUiLabelOfFieldId( String fieldId ) {
		String ret = (String)uiLabelOfFieldIds.get( fieldId );
		if ( ret == null ) {
			return "";
		}
		return ret;
	}

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
	 */
	public synchronized String getUiLabelOfSystemIds( String system,
	                                                  String systemFieldId,
	                                                  String systemValueId,
	                                                  boolean abbreviated ) {
		String ret = null;
		if ( abbreviated ) {
			// Fudge: abbrevs apply to all system/interfaces in a given language:
			int ind = system.indexOf( "." );
			ind = system.indexOf( ".", ind + 1 );
			String language = system.substring( ind + 1, system.length() );
			ret = (String)abbreviatedLabels.get( language + systemFieldId + systemValueId );
		}
		if ( !abbreviated || ( ret == null ) ) {
			ret = (String)uiLabelOfSystemIds.get( system + systemFieldId + systemValueId );
		}
		if ( ret == null ) {
			reportError( "Vocabulary Manager: getUiLabelOfSystemIds is NULL for (" + system + systemFieldId + systemValueId + ")" );
			ret = " ";
		}
		return ret;
	}

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
	 */
	public synchronized String getUiLabelOfSystemIds( String system,
	                                                  String systemFieldId,
	                                                  String systemValueId ) {
		return getUiLabelOfSystemIds( system, systemFieldId, systemValueId, false );
	}

	/**
	 *  Gets the topLevelAbbrevLabelOf attribute of the MetadataVocab object
	 *
	 * @param  system
	 * @param  metadataField
	 * @param  systemFieldId
	 * @param  systemValueId
	 * @return                The topLevelAbbrevLabelOf value
	 */
	public synchronized String getTopLevelAbbrevLabelOf( String system,
	                                                     String metadataField,
	                                                     String systemFieldId,
	                                                     String systemValueId ) {
		String ret = null;
		ret = (String)topUiAbbrevLabelOf.get( system + "." + metadataField
			 + getMetaNameOfId( system, systemFieldId, systemValueId ) );
		if ( ret == null ) {
			reportError( "Vocabulary Manager: getTopLevelUiLabelOfSystemIds is NULL for (" + system + "." + metadataField
				 + getMetaNameOfId( system, systemFieldId, systemValueId ) + ")" );
			ret = " ";
		}
		return ret;
	}

	/**
	 *  Gets the vocabNodes attribute of the MetadataVocab object
	 *
	 * @param  system
	 * @param  group
	 * @return         The vocabNodes value
	 */
	public synchronized ArrayList getVocabNodes( String system,
	                                             String group ) {
		VocabList current = setCurrent( system, group );
		return current.item;
	}

	/**
	 *  Gets the vocabNodes attribute of the MetadataVocabTermsGroups object
	 *
	 * @param  metaFormat
	 * @param  audience
	 * @param  language
	 * @param  field
	 * @return             The vocabNodes value
	 */
	public ArrayList getVocabNodes( String metaFormat, String audience, String language, String field ) {
		// NEVER SUPPORTED
		return new ArrayList();
	}

	/**
	 *  Gets the vocabNodes attribute of the MetadataVocabTermsGroups object
	 *
	 * @param  metaFormat
	 * @param  audience
	 * @param  language
	 * @param  field
	 * @param  group
	 * @return             The vocabNodes value
	 */
	public ArrayList getVocabNodes( String metaFormat, String audience, String language, String field, String group ) {
		// NEVER SUPPORTED
		return new ArrayList();
	}

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
	public synchronized String getVocabSelectList( String system,
	                                               String group,
	                                               int size,
	                                               MetadataVocabInputState inputState ) {
		StringBuffer ret = new StringBuffer();
		String name = setTopName( system, group );
		VocabList current = setCurrent( system, group );
		if ( size > 0 ) {
			ret.append( "<select multiple name='" )
				.append( name )
				.append( "' size='" )
				.append( Integer.toString( size ) )
				.append( "'>\n" );
		}
		ret.append( vocabSelectList( current, 0, inputState, name ) );
		if ( size > 0 ) {
			ret.append( "</select>\n" );
		}
		return ret.toString();
	}

	/**
	 *  Recursive method invoked by getVocabSelectList()
	 *
	 * @param  list
	 * @param  indent
	 * @param  inputState
	 * @param  name
	 * @return
	 */
	private synchronized String vocabSelectList( VocabList list,
	                                             int indent,
	                                             MetadataVocabInputState inputState,
	                                             String name ) {
		StringBuffer ret = new StringBuffer();
		for ( int i = 0; i < list.item.size(); i++ ) {
			VocabNode node = (VocabNode)list.item.get( i );
			if ( ( inputState != null ) &&
				( inputState.isSelected( node.getFieldId(), node.getId() ) ) ) {
				ret.append( "<option selected value=\"" )
					.append( node.getId() )
					.append( "\">\n" );
			}
			else {
				ret.append( "<option value=\"" )
					.append( node.getId() )
					.append( "\">\n" );
			}
			for ( int j = 0; j < indent; j++ ) {
				ret.append( "&nbsp;&nbsp;&nbsp;&nbsp;" );
			}
			if ( node.getList().item.size() > 0 ) {
				ret.append( node.getLabel().toUpperCase() )
					.append( ":</option>\n" );
			}
			else {
				ret.append( node.getLabel() )
					.append( "</option>\n" );
			}
			if ( node.getList().item.size() > 0 ) {
				ret.append( vocabSelectList( node.getList(), indent + 1, inputState, name ) );
			}
		}
		return ret.toString();
	}

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
	public synchronized String getVocabCheckboxes( String system,
	                                               String group,
	                                               int size,
	                                               String tdWidth,
	                                               boolean skipTopRow,
	                                               MetadataVocabInputState inputState ) {
		String name = setTopName( system, group );
		VocabList current = setCurrent( system, group );
		StringBuffer ret = new StringBuffer( "" );
		ret.append( vocabCheckboxes( current, size, tdWidth, inputState, name, new CBCount( 1 ), skipTopRow ) )
			.append( "</td></table>" );
		StringBuffer ins = new StringBuffer( "" );
		ret.insert( 0, ins )
			.insert( 0, "<table border='0' cellpadding='0' cellspacing='0' width='100%'><td valign='top'>" );
		return ret.toString();
	}

	/**
	 *  Recursive method invoked by getVocabCheckboxes()
	 *
	 * @param  list
	 * @param  size
	 * @param  tdWidth
	 * @param  inputState
	 * @param  name
	 * @param  count
	 * @param  skipTopRow
	 * @return
	 */
	private synchronized String vocabCheckboxes( VocabList list,
	                                             int size,
	                                             String tdWidth,
	                                             MetadataVocabInputState inputState,
	                                             String name,
	                                             CBCount count,
	                                             boolean skipTopRow ) {
		StringBuffer ret = new StringBuffer();
		boolean isHeading = false;
		for ( int i = 0; i < list.item.size(); i++ ) {
			if ( ( count.value != 0 ) && ( ( count.value % size ) == 0 ) ) {
				ret.append( "</td><td width='" + tdWidth + "' valign='top'>" );
				if ( skipTopRow ) {
					ret.append( "<div class='vocabCheckboxLabels'>&nbsp;</div>" );
					count.value++;
				}
			}
			count.value++;
			VocabNode node = (VocabNode)list.item.get( i );
			isHeading = ( node.getList().item.size() > 0 ) ? true : false;
			if ( isHeading ) {
				ret.append( "<div class='subCatHeading'>" );
			}
			else if ( !node.getNoDisplay() ) {
				String chk = "";
				if ( ( inputState != null ) &&
					( inputState.isSelected( node.getFieldId(), node.getId() ) ) ) {
					chk = " checked";
				}
				ret.append( "<div class='vocabCheckboxLabels'><nobr><input type='checkbox'" )
					.append( chk )
					.append( " name='" )
					.append( name )
					.append( "' value=\"" )
					.append( node.getId() )
					.append( "\" id=\"" )
					.append( name )
					.append( node.getId() )
					.append( "\">" );
			}
			if ( isHeading ) {
				ret.append( node.getLabel() + ":</div><div class='subCatBlock'>\n" )
					.append( vocabCheckboxes( node.getList(), size, tdWidth, inputState, name, count, skipTopRow ) )
					.append( "</div>\n" );
			}
			else if ( !node.getNoDisplay() ) {
				ret.append( "<label for=\"" )
					.append( name )
					.append( node.getId() )
					.append( "\">" )
					.append( wrappedLabel( node.getLabel() ) )
					.append( "</label></nobr></div>\n" );
			}
		}
		return ret.toString();
	}

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
	public synchronized String getVocabCheckbox( String groupTop,
	                                             String value,
	                                             String label,
	                                             MetadataVocabInputState inputState ) {
		StringBuffer ret = new StringBuffer();
		String chk = "";
		try {
			if ( ( inputState != null ) &&
				( inputState.isSelected( getFieldSystemId( groupTop ), getFieldValueSystemId( groupTop, value ) ) ) ) {
				chk = " checked";
			}
		}
		catch ( Exception e ) {}
		ret.append( "<span class='vocabCheckboxLabels'><nobr><input type='checkbox'" )
			.append( chk )
			.append( " name='" )
			.append( groupTop )
			.append( "' value=\"" )
			.append( value )
			.append( "\" id=\"" )
			.append( value )
			.append( "\">" )
			.append( "<label for=\"" )
			.append( value )
			.append( "\">" )
			.append( label )
			.append( "</label></nobr></span><br />\n" );
		return ret.toString();
	}

	/**
	 *  Generates a Javascript Tree Menu (collapsable hierarchy) of the specified
	 *  part of the vocabulary
	 *
	 * @param  group     colon-seperated specifier of the part of the vocab
	 *      hierarchy which is to be displayed
	 * @param  system
	 * @param  page
	 * @param  language
	 * @return           the Javascript code defining the menu
	 */
	public synchronized String getVocabTreeMenu( String system,
	                                             String language,
	                                             String group,
	                                             PageContext page ) {
		StringBuffer ret = new StringBuffer();
		String name = setTopName( system, group );
		VocabList current = setCurrent( system, group );
		current.jsVar = "tm_" + name + "0";
		VocabNode node = (VocabNode)vocab.map.get( system + '.' + group );
		ret.append( "<script type='text/javascript'>\n\n" );
		try {
			String abbrevLabel = node.getLabelAbbrev();
			if ( abbrevLabel == null ) {
				abbrevLabel = node.getLabel();
			}
			ret.append( "var tm_" + name + "0 = new VocabList( 'tm_" + name + "0', 0, '"
				 + node.getLabel() + "', '" + abbrevLabel + "' );\n" );
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		ret.append( vocabTreeMenu( current, language, name + "0", name, page ) );
		ret.append( "\nsetList( '" + name + "' );\n//-->\n</script>" );
		return ret.toString();
	}

	/**
	 *  Recursive method invoked by getVocabTreeMenu()
	 *
	 * @param  list
	 * @param  id
	 * @param  fieldId
	 * @param  page
	 * @param  language
	 * @return
	 */
	private synchronized String vocabTreeMenu( VocabList list,
	                                           String language,
	                                           String id,
	                                           String fieldId,
	                                           PageContext page ) {
		StringBuffer ret = new StringBuffer();
		boolean isHeading = false;
		int jsCount = 0;
		HttpSession session = page.getSession();
		String base = page.getServletContext().getRealPath( "/" );
		for ( int i = 0; i < list.item.size(); i++ ) {
			VocabNode node = (VocabNode)list.item.get( i );
			isHeading = ( node.getList().item.size() > 0 ) ? true : false;
			String wrap = "false";
			if ( node.getWrap() ) {
				wrap = "true";
			}
			String labelAbbrev = node.getLabelAbbrev();
			if ( labelAbbrev == null ) {
				labelAbbrev = (String)abbreviatedLabels.get( language + fieldId + node.getId() );
				if ( labelAbbrev == null ) {
					labelAbbrev = node.getLabel();
				}
			}
			String description = node.getDescription( page );
			if ( description != null ) {
				description = stringUtil.replace( description, "\n", " ", false );
				description = stringUtil.replace( description, "\r", " ", false );
				description = stringUtil.replace( description, "'", "\'", false );
				description = ", null, '" + description + "'";
			}
			else {
				description = "";
			}
			if ( isHeading ) {
				String varName = id + new Integer( jsCount++ ).toString();
				String groupType = Integer.toString( node.getList().groupType );
				node.getList().jsVar = "tm_" + varName;
				ret.append( "var tm_" + varName + " = new VocabList( 'tm_" + varName + "', " + groupType + " );\n" );
				ret.append( "AV( tm_" + id + ", \"" + node.getLabel() + "\", \"" + labelAbbrev + "\", '"
					 + fieldId + "', '" + node.getId() + "', false, " + wrap + ", tm_" + varName + description + " );\n" );

				ret.append( vocabTreeMenu( node.getList(), language, varName, fieldId, page ) );
			}
			else if ( !node.getNoDisplay() ) {
				jsCount++;
				ret.append( "AV( tm_" + id + ", \"" + node.getLabel() + "\", \"" + labelAbbrev + "\", '" + fieldId
					 + "', '" + node.getId() + "', false, " + wrap + ", null" + description + " );\n" );
			}
		}
		return ret.toString();
	}


	/**
	 *  Reads the given file and returns its contents as a string
	 *
	 * @param  filename
	 * @param  page
	 * @return           The fileText value
	 */
	private synchronized String getFileText( String filename, PageContext page ) {
		StringBuffer ret = new StringBuffer();
		try {
			BufferedReader in = new BufferedReader( new FileReader( page.getServletContext().getRealPath( filename ) ) );
			String s = null;
			while ( ( s = in.readLine() ) != null ) {
				ret.append( s );
			}
		}
		catch ( FileNotFoundException fnfe ) {
			System.err.println( "File not found - " );
			System.err.println( fnfe.getClass() + " " + fnfe.getMessage() );
		}
		catch ( IOException ioe ) {
			System.err.println( "Exception occurred reading " + filename );
			System.err.println( ioe.getClass() + " " + ioe.getMessage() );
		}
		return ret.toString();
	}

	/**
	 *  Replaces "||" in label values with &lt;br&gt;&amp;nbsp;
	 *
	 * @param  label
	 * @return
	 */
	public synchronized String wrappedLabel( String label ) {
		int ind = label.indexOf( "||" );
		while ( ind > -1 ) {
			label = label.substring( 0, ind ) + "<br />&nbsp;" + label.substring( ind + 2, label.length() );
			ind = label.indexOf( "||" );
		}
		return label;
	}

	/**
	 *  Replaces "||" in label values with spaces
	 *
	 * @param  label
	 * @return
	 */
	public synchronized String nonWrappedLabel( String label ) {
		int ind = label.indexOf( "||" );
		while ( ind > -1 ) {
			label = label.substring( 0, ind ) + " " + label.substring( ind + 2, label.length() );
			ind = label.indexOf( "||" );
		}
		return label;
	}

	/**
	 *  Generates HTML HIDDEN inputs of the specified part of the vocabulary.
	 *
	 * @param  group       colon-seperated specifier of the part of the vocab
	 *      hierarchy which is to be displayed
	 * @param  system
	 * @param  inputState
	 * @return             the HTML code
	 */
	public synchronized String getVocabHiddenInputs( String system,
	                                                 String group,
	                                                 MetadataVocabInputState inputState ) {
		StringBuffer ret = new StringBuffer();
		String name = setTopName( system, group );
		VocabList current = setCurrent( system, group );
		ret.append( vocabHiddenInputs( current, inputState, name ) );
		return ret.toString();
	}

	/**
	 *  Recursive method invoked by getVocabHiddenInputs()
	 *
	 * @param  list
	 * @param  inputState
	 * @param  name
	 * @return
	 */
	private synchronized String vocabHiddenInputs( VocabList list,
	                                               MetadataVocabInputState inputState,
	                                               String name ) {
		StringBuffer ret = new StringBuffer();
		for ( int i = 0; i < list.item.size(); i++ ) {
			VocabNode node = (VocabNode)list.item.get( i );
			try {
				if ( ( inputState != null ) &&
					( inputState.isSelected( node.getFieldId(), node.getId() ) ) ) {
					ret.append( "<input type='hidden' name='" )
						.append( node.getFieldId() )
						.append( "' value='" )
						.append( node.getId() )
						.append( "'>\n" );
				}
			}
			catch ( Exception e ) {}
			if ( node.getList().item.size() > 0 ) {
				ret.append( vocabHiddenInputs( node.getList(), inputState, name ) );
			}
		}
		return ret.toString();
	}

	/**
	 *  Returns the top-level group of a vocab grouping (a substring of the given
	 *  string from index 0 up to but not including the first ':')
	 *
	 * @param  group   the vocab grouping (sub-group)
	 * @param  system  The new topName value
	 * @return
	 */
	private synchronized String setTopName( String system, String group ) {
		// System.out.println( "setTopName = " + system + "/" + group );
		group += ":";
		int ind = 0;
		int ind2 = group.indexOf( ":" );
		return ( (VocabNode)vocab.map.get( system + "." + group.substring( 0, ind2 ) ) ).getId();
	}

	/**
	 *  Sets the topMetaName attribute of the MetadataVocab object
	 *
	 * @param  group  The new topMetaName value
	 * @return
	 */
	private synchronized String setTopMetaName( String group ) {
		group += ":";
		int ind = 0;
		int ind2 = group.indexOf( ":" );
		return group.substring( 0, ind2 );
	}

	/**
	 *  Sets the current VocabList pointer to the specified spot in the vocab
	 *  hierarchy
	 *
	 * @param  group   colon-seperated specifier of the part of the vocab hierarchy
	 *      which is to be displayed
	 * @param  system  The new current value
	 * @return         VocabList pointing to the specified spot
	 */
	private synchronized VocabList setCurrent( String system, String group ) {
		VocabList current = vocab;
		group = system + "." + group + ":";
		int ind = 0;
		int ind2 = group.indexOf( ":" );
		current = vocab;
		while ( ind2 > -1 ) {
			String str = group.substring( ind, ind2 );
			VocabNode node = (VocabNode)current.map.get( str );
			current = node.getList();
			ind = ind2 + 1;
			ind2 = group.indexOf( ":", ind );
		}
		return current;
	}


	/**
	 *  Used by NOSCRIPT vocab pages to carry through selections in other
	 *  categories
	 *
	 * @param  groupTop  top-level vocab group
	 * @param  page      JSP PageContext
	 * @return           the HTML code
	 */
	private synchronized String hiddenState( String groupTop, PageContext page ) {
		return "";
	}

	/**
	 *  When all vocabs are loaded, generate the Lucene queries, register all
	 *  current UI labels into the database, and close any database connection
	 */
	public void doneLoading() {
		tracker.registerUiLabels( uiSystems, uiLabelOfSystemIds );
		tracker.writeDataAsTextFile();
		tracker.closeConnection();
		// Set uiLabelOfSystemIds for UI group headers:
		VocabList list = vocab;
		for ( int i = 0; i < list.item.size(); i++ ) {
			VocabNode node = (VocabNode)list.item.get( i );
			if ( node.getName().lastIndexOf( "." ) > -1 ) {
				String system = node.getName().substring( 0, node.getName().lastIndexOf( "." ) );
				setUiLabelOfSystemIds( node.getList(), system, node.getId() );
			}
		}
		setFieldIds( list );
	}

	/**
	 *  Sets the fieldIds attribute of the MetadataVocab object
	 *
	 * @param  list  The new fieldIds value
	 */
	private void setFieldIds( VocabList list ) {
		for ( int i = 0; i < list.item.size(); i++ ) {
			VocabNode node = (VocabNode)list.item.get( i );
			if ( node.getList().item.size() > 0 ) {
				setNodeFieldIds( node.getList(), node.getId() );
			}
		}
	}

	/**
	 *  Sets the nodeFieldIds attribute of the MetadataVocab object
	 *
	 * @param  list  The new nodeFieldIds value
	 * @param  id    The new nodeFieldIds value
	 */
	private void setNodeFieldIds( VocabList list, String id ) {
		for ( int i = 0; i < list.item.size(); i++ ) {
			VocabNode node = (VocabNode)list.item.get( i );
			node.setFieldId( id );
			if ( node.getList().item.size() > 0 ) {
				setNodeFieldIds( node.getList(), id );
			}
		}
	}

	/**
	 *  Sets the uiLabelOfSystemIds attribute of the MetadataVocab object
	 *
	 * @param  list     The new uiLabelOfSystemIds value
	 * @param  fieldId  The new uiLabelOfSystemIds value
	 * @param  system   The new uiLabelOfSystemIds value
	 */
	private void setUiLabelOfSystemIds( VocabList list, String system, String fieldId ) {
		for ( int i = 0; i < list.item.size(); i++ ) {
			VocabNode node = (VocabNode)list.item.get( i );
			//System.out.println( "setUiLabelOfSystemIds: " + node.getName() + "=" + node.getId() );
			if ( node.getId() == null ) {
				String label = stringUtil.replace( node.getLabel(), " ", "_", false );
				uiLabelOfSystemIds.put( system + fieldId + label, label );
			}
			if ( node.getList().item.size() > 0 ) {
				setUiLabelOfSystemIds( node.getList(), system, fieldId );
			}
		}
	}

	/**
	 *  Sets the definitions attribute of the MetadataVocab object
	 *
	 * @param  system      The new definitions value
	 * @param  nodeId      The new definitions value
	 * @param  definition  The new definitions value
	 */
	private void setDefinitions( String system, String nodeId, String definition ) {
	}

	/**
	 *  Given a cache (Map) of vocab values, this method returns a list of those
	 *  values in the order that they are defined in their groups file.
	 *
	 * @param  cache
	 * @param  system
	 * @param  group
	 * @return         The orderedCacheValues value
	 */
	public ArrayList getCacheValuesInOrder( String system, String group, Map cache ) {
		String name = setTopName( system, group );
		VocabList list = setCurrent( system, group );
		return orderedCacheValues( list, cache );
	}

	/**
	 *  Recursive method invoked by getCacheValuesInOrder()
	 *
	 * @param  list
	 * @param  cache
	 * @return
	 */
	private ArrayList orderedCacheValues( VocabList list, Map cache ) {
		ArrayList ret = new ArrayList();
		for ( int i = 0; i < list.item.size(); i++ ) {
			VocabNode node = (VocabNode)list.item.get( i );
			if ( cache.get( node.getName() ) != null ) {
				if ( !node.getNoDisplay() ) {
					ret.add( node );
				}
			}
			if ( node.getList().item.size() > 0 ) {
				ArrayList subRet = orderedCacheValues( node.getList(), cache );
				if ( subRet.size() > 0 ) {
					if ( !node.getNoDisplay() ) {
						ret.add( node );
					}
					ret.addAll( subRet );
				}
			}
		}
		return ret;
	}

	/**
	 *  Add encoded field ID if it hasn't been added already
	 *
	 * @param  id  The feature to be added to the VocabFieldId attribute
	 */
	private void addVocabFieldId( String id ) {
		boolean doAdd = true;
		for ( int i = 0; i < vocabFieldIds.size(); i++ ) {
			if ( vocabFieldIds.get( i ).equals( id ) ) {
				doAdd = false;
			}
		}
		if ( doAdd ) {
			vocabFieldIds.add( id );
		}
	}

	/**
	 *  Gets the vocabFieldIds attribute of the MetadataVocab object
	 *
	 * @return    The vocabFieldIds value
	 */
	public ArrayList getVocabFieldIds() {
		return vocabFieldIds;
	}

	// Data structures:
	//----------------------------------------------------------------------------------

	/**
	 *  CBCount = CheckBoxesCount, used to render Javascript "All | Clear" links
	 *
	 * @author     Ryan Deardorff
	 * @created    October 15, 2003
	 */
	class CBCount {
		/**
		 *  Description of the Field
		 */
		public int value;

		/**
		 *  Constructor for the CBCount object
		 *
		 * @param  val
		 */
		public CBCount( int val ) {
			value = val;
		}
	}

	// SAX parser implementation:
	//----------------------------------------------------------------------------------

	/**
	 *  (SAX) Not strictly part of the SAX interface, but we use this to figure out
	 *  which type of XML we're parsing (definitions/terms vs. UI/groups)
	 *
	 * @param  bool  The new parsingDefinitions value
	 */
	public void setParsingDefinitions( boolean bool ) {
		parsingDefinitions = bool;
	}

	/**
	 *  (SAX) Sets the SAX locator, which indicates the current position of the
	 *  parser within the document (line number, column number). Could be used to
	 *  indicate the spot where an error occured.
	 *
	 * @param  locator  The new saxLocator value
	 */
	public void setDocumentLocator( Locator locator ) {
		saxLocator = locator;
		xmlFile = saxLocator.getSystemId();
		// Trim "file:///" from file string:
		xmlFile = xmlFile.substring( 8, xmlFile.length() );
		// On Windows, the above will result in C:/... but on UNIX there
		// will be no leading '/', so we must add it:
		if ( xmlFile.indexOf( ':' ) == -1 ) {
			xmlFile = '/' + xmlFile;
		}
	}

	/**
	 *  (SAX) Invoked at the start of any document parse
	 *
	 * @exception  SAXException
	 */
	public void startDocument() throws SAXException {
		current = vocab;
		outputXML.setLength( 0 );
		outputXML.append( "\t" );
		rewriteXML = false;
		currentHeader = "";
		currentFooter = "\n";
		startSavingXML = false;
		uiGroupLevelAt = 0;
		// Get header and footer of file in case of re-write:
		if ( parsingDefinitions ) {
			String filename = saxLocator.getSystemId().substring( 5, saxLocator.getSystemId().length() );
			try {
				BufferedReader in = new BufferedReader( new FileReader( filename ) );
				String s = null;
				boolean parsingHeader = true;
				boolean parsingFooter = false;
				while ( ( s = in.readLine() ) != null ) {
					if ( parsingHeader ) {
						if ( s.indexOf( "<field" ) > -1 ) {
							parsingHeader = false;
						}
						else {
							currentHeader = currentHeader + s + "\n";
						}
					}
					else if ( parsingFooter ) {
						currentFooter = currentFooter + s + "\n";
					}
					else if ( s.indexOf( "</termsRecord>" ) > -1 ) {
						parsingFooter = true;
					}
				}
			}
			catch ( FileNotFoundException fnfe ) {
				System.err.println( "Vocab file not found - " );
				System.err.println( fnfe.getClass() + " " + fnfe.getMessage() );
			}
			catch ( IOException ioe ) {
				System.err.println( "Exception occurred reading " + filename );
				System.err.println( ioe.getClass() + " " + ioe.getMessage() );
			}
		}
	}

	/**
	 *  (SAX) Invoked at the end of parsing. Rewrite the definitions XML if new Ids
	 *  have been assigned.
	 *
	 * @exception  SAXException
	 */
	public void endDocument() throws SAXException {
		if ( parsingDefinitions ) {
			reportMessage( "Loaded definition file: " + xmlFile );
		}
		else {
			reportMessage( "Loaded UI file: " + xmlFile );
		}
		setNodesIsLastInSubList( vocab, true );
		if ( rewriteXML ) {
			try {
				reportMessage( "New vocabulary Id(s) assigned, rewriting file " + xmlFile );
				FileWriter file = new FileWriter( xmlFile );
				String outStr = outputXML.toString();
				file.write( currentHeader, 0, currentHeader.length() );
				file.write( outStr, 0, outStr.length() );
				file.write( currentFooter, 0, currentFooter.length() );
				file.flush();
				file.close();
			}
			catch ( Exception e ) {
				reportError( "Vocabulary Manager: Problem writing XML file: " + e.toString() );
				e.printStackTrace();
			}
		}
	}

	/**
	 *  Sets the isLastInSubList attribute of each vocab node
	 *
	 * @param  list     The new nodesIsLastInSubList value
	 * @param  topList  The new nodesIsLastInSubList value
	 */
	protected synchronized void setNodesIsLastInSubList( VocabList list, boolean topList ) {
		for ( int i = 0; i < list.item.size(); i++ ) {
			VocabNode node = (VocabNode)list.item.get( i );
			if ( !topList && ( i == list.item.size() - 1 ) ) {
				node.setIsLastInSubList( true );
			}
			if ( node.getList().item.size() > 0 ) {
				setNodesIsLastInSubList( node.getList(), false );
			}
		}
	}

	/**
	 *  (SAX) Invoked upon opening tag of an XML element
	 *
	 * @param  namespaceURI
	 * @param  localName
	 * @param  qName
	 * @param  atts
	 * @exception  SAXException
	 */
	public void startElement( String namespaceURI,
	                          String localName,
	                          String qName,
	                          Attributes atts ) throws SAXException {
		if ( parsingDefinitions ) {
			startElementDefinition( namespaceURI, localName, qName, atts );
		}
		else {
			startElementUI( namespaceURI, localName, qName, atts );
		}
	}

	/**
	 *  (SAX) Invoked upon opening tag of a definitions XML element
	 *
	 * @param  namespaceURI
	 * @param  localName
	 * @param  qName
	 * @param  atts
	 * @exception  SAXException
	 */
	public void startElementDefinition( String namespaceURI,
	                                    String localName,
	                                    String qName,
	                                    Attributes atts ) throws SAXException {
		currentSAXElementName = new String( localName );
		if ( !qName.equals( "termsRecord" ) ) {
			outputXML.append( "<" + qName );
		}
		HashMap attributes = new HashMap();
		for ( int i = 0; i < atts.getLength(); i++ ) {
			attributes.put( atts.getLocalName( i ), atts.getValue( i ) );
		}
		if ( localName.equals( "field" ) ) {
			startSavingXML = true;
			currentSAXElementLanguage = (String)attributes.get( "language" );
			String testAtt = (String)attributes.get( "id" );
			if ( testAtt == null ) {
				reportError( "Vocabulary Manager: Missing vocabulary system Id for field " + (String)attributes.get( "name" ) );
				encounteredErrorInElement = true;
			}
			else {
				currentVocabFieldId = testAtt;
				addVocabFieldId( currentVocabFieldId );
				currentVocabFieldName = (String)attributes.get( "name" );
				uiLabelOfFieldIds.put( currentVocabFieldId, (String)uiLabelOfFieldIds.get( currentVocabFieldName ) );
				outputXML.append( " name=\"" + currentVocabFieldName + "\""
					 + " id=\"" + testAtt + "\""
					 + " language=\"" + (String)attributes.get( "language" ) + "\"" );
				fieldSystemId.put( currentVocabFieldName, testAtt );
				// Assign field Id to all system trees:
				Set systemKeys = uiSystems.keySet();
				Iterator i = systemKeys.iterator();
				while ( i.hasNext() ) {
					String sys = (String)i.next();
					// Assign the Id only if this system uses this vocab field (some systems might not):
					if ( uiSystemFields.get( sys + "." + currentVocabFieldName ) != null ) {
						VocabNode node = findVocabNode( sys, currentVocabFieldName, null );
						if ( node != null ) {
							node.setId( currentVocabFieldId );
						}
					}
				}
			}
		}
		else if ( localName.equals( "value" ) ) {
			encounteredErrorInElement = false;
			newNodeName = (String)attributes.get( "vocab" );
			if ( newNodeName == null ) {
				newNodeName = (String)attributes.get( "name" );
			}
			newNodeId = (String)attributes.get( "id" );
			if ( ( newNodeId == null ) || ( newNodeId.equals( "" ) ) ) {
				// Assign new Id:
				rewriteXML = true;
				String dbId = tracker.getId( currentVocabFieldId, newNodeName );
				if ( dbId.equals( "" ) ) {
					newNodeId = tracker.assignNewSystemId( currentVocabFieldId, newNodeName );
				}
				else {
					encounteredErrorInElement = true;
					reportError( "Vocabulary Manager: XML is missing Id for field/value pair already in the database: "
						 + currentVocabFieldId + "/" + newNodeName + ", id='" + dbId + "'" );
				}
			}
			else {
				// Check Id against database:
				if ( tracker.isUsingDatabase() ) {
					String dbId = tracker.getId( currentVocabFieldId, newNodeName );
					if ( dbId.equals( "" ) ) {
						encounteredErrorInElement = true;
						reportError( "Vocabulary Manager: Vocab value not found in database: " + currentVocabFieldId + "=" + newNodeName
							 + ". If this is a new value, then make sure to empty its 'id' attribute!" );
					}
					else if ( !dbId.equals( newNodeId ) ) {
						encounteredErrorInElement = true;
						reportError( "Vocabulary Manager: Vocab Id found in database (" + dbId
							 + ") does not match Id found in XML (" + newNodeId + ") for " + currentVocabFieldId
							 + "=" + newNodeName );
					}
				}
			}
			fieldValueSystemId.put( currentVocabFieldName + newNodeName, newNodeId );
			fieldValueIdPairExists.put( currentVocabFieldId + "=" + newNodeId, new Boolean( true ) );
			// Assign new Id to each system's UI trees and register their labels into the DB:
			StringBuffer systemUiLabels = new StringBuffer( "" );
			Set systemKeys = uiSystems.keySet();
			Iterator i = systemKeys.iterator();
			while ( i.hasNext() ) {
				String sys = (String)i.next();
				// Assign the Id only if this system uses this vocab field (some systems might not):
				if ( uiSystemFields.get( sys + "." + currentVocabFieldName ) != null ) {
					VocabNode node = null;
					String nodeName = (String)attributes.get( "name" );
					if ( nodeName == null ) {
						nodeName = (String)attributes.get( "vocab" );
					}
					node = findVocabNode( sys, currentVocabFieldName, nodeName );
					if ( node == null ) {
						reportError( "Vocabulary Manager: Vocabulary UI for system " + sys + " is missing value "
							 + nodeName );
					}
					else {
						node.setId( newNodeId );
						uiLabelOf.put( sys + currentVocabFieldName + newNodeName, node.getLabel() );
						uiLabelOfSystemIds.put( sys + currentVocabFieldId + newNodeId, node.getLabel() );
						metaNameOfId.put( sys + currentVocabFieldId + newNodeId, nodeName );
					}
				}
			}
			if ( newNodeId == null ) {
				newNodeId = "";
			}
			if ( attributes.get( "name" ) != null ) {
				outputXML.append( " name=\"" + (String)attributes.get( "name" ) + "\"" );
			}
			if ( attributes.get( "vocab" ) != null ) {
				outputXML.append( " vocab=\"" + (String)attributes.get( "vocab" ) + "\"" );
			}
			outputXML.append( " id=\"" + newNodeId + "\"" );
			if ( attributes.get( "abbrv" ) != null ) {
				abbreviatedLabels.put( currentSAXElementLanguage + currentVocabFieldId + newNodeId,
					(String)attributes.get( "abbrv" ) );
				abbreviatedLabelsMeta.put( currentSAXElementLanguage + currentVocabFieldName + newNodeName,
					(String)attributes.get( "abbrv" ) );
				outputXML.append( " abbrv=\"" + (String)attributes.get( "abbrv" ) + "\"" );
			}
		}
		else if ( localName.equals( "definition" ) ) {
			outputXML.append( " system=\"" + (String)attributes.get( "system" ) + "\" interface=\""
				 + (String)attributes.get( "interface" ) + "\"" );
		}
		if ( !qName.equals( "termsRecord" ) ) {
			outputXML.append( ">" );
		}
	}

	/**
	 *  Within a given system (system.interface.language), find the node that
	 *  contains the given value for its 'name' attribute.
	 *
	 * @param  system
	 * @param  fieldName
	 * @param  valueName  pass null if only the field node is desired
	 * @return
	 */
	public VocabNode findVocabNode( String system, String fieldName, String valueName ) {
		VocabNode ret = null;
		VocabList list = vocab;
		String systemField = system + "." + fieldName;
		for ( int i = 0; i < list.item.size(); i++ ) {
			VocabNode node = (VocabNode)list.item.get( i );
			if ( node.getName().equals( systemField ) ) {
				if ( valueName == null ) {
					return node;
				}
				else {
					return findNode( node.getList(), valueName );
				}
			}
		}
		return ret;
	}

	/**
	 *  Recursive method invoked by findVocabNode
	 *
	 * @param  list
	 * @param  valueName
	 * @return
	 */
	private VocabNode findNode( VocabList list, String valueName ) {
		VocabNode ret = null;
		for ( int i = 0; i < list.item.size(); i++ ) {
			VocabNode node = (VocabNode)list.item.get( i );
			if ( node.getList().item.size() > 0 ) {                      // don't return a match for sub-header names, only leaf-nodes!
				ret = findNode( node.getList(), valueName );
				if ( ret != null ) {
					return ret;
				}
			}
			else if ( node.getName().equals( valueName ) ) {
				return node;
			}
		}
		return ret;
	}

	/**
	 *  (SAX) Start element for the UI files
	 *
	 * @param  namespaceURI      XML namespace
	 * @param  localName         local tag name
	 * @param  qName             fully qualified tag name
	 * @param  atts              tag attributes
	 * @exception  SAXException
	 */
	public void startElementUI( String namespaceURI,
	                            String localName,
	                            String qName,
	                            Attributes atts ) throws SAXException {
		currentSAXElementName = new String( localName );
		HashMap attributes = new HashMap();
		for ( int i = 0; i < atts.getLength(); i++ ) {
			attributes.put( atts.getLocalName( i ), atts.getValue( i ) );
		}
		if ( localName.equals( "concept" ) || localName.equals( "group" ) ) {
			if ( localName.equals( "concept" ) ) {
				currentSAXElementSystem = (String)attributes.get( "system" ) + "." + (String)attributes.get( "interface" )
					 + "." + (String)attributes.get( "language" );
				uiSystems.put( currentSAXElementSystem, new Boolean( true ) );
				newNodeName = currentSAXElementSystem + "." + (String)attributes.get( "name" );
				uiSystemFields.put( newNodeName, new Boolean( true ) );
				currentVocabFieldName = newNodeName;
				uiLabelOfFieldIds.put( (String)attributes.get( "name" ), (String)attributes.get( "abbrev" ) );
				processingUiGroup = false;
				uiLabelOf.put( currentSAXElementSystem + (String)attributes.get( "name" ), (String)attributes.get( "label" ) );
			}
			else if ( localName.equals( "group" ) ) {
				uiGroupLevelAt++;
				if ( uiGroupLevelAt == 1 ) {
					currentTopLevelAbbrevLabel = (String)attributes.get( "abbrev" );
				}
				processingUiGroup = true;
				newNodeName = (String)attributes.get( "label" );
			}
			newNodeName = stringUtil.replace( newNodeName, " ", "_", false );
			VocabNodeTermsGroups node = new VocabNodeTermsGroups( newNodeName, false, uiGroupLevelAt );
			current.map.put( newNodeName, node );
			current.item.add( node );
			currentNode = node;
			currentNode.setLabel( (String)attributes.get( "label" ) );
			currentNode.setLabelAbbrev( (String)attributes.get( "abbrev" ) );
			node.getList().parent = current;
			String type = (String)attributes.get( "inline" );
			if ( type != null ) {
				if ( type.equals( "drop" ) ) {
					node.getList().groupType = 1;
				}
				else if ( type.equals( "indent" ) ) {
					node.getList().groupType = 2;
				}
			}
			current = node.getList();
		}
		else if ( localName.equals( "item" ) ) {
			boolean noDisplay = false;
			String newNodeLabel;
			newNodeName = newNodeLabel = (String)attributes.get( "name" );
			if ( newNodeName == null ) {
				newNodeName = newNodeLabel = (String)attributes.get( "vocab" );
				int ind = newNodeLabel.indexOf( ":" );
				while ( ind > -1 ) {
					newNodeLabel = newNodeLabel.substring( ind + 1, newNodeLabel.length() );
					ind = newNodeLabel.indexOf( ":" );
				}
			}
			String testAtt = (String)attributes.get( "noDisplay" );
			if ( ( testAtt != null ) &&
				( testAtt.equalsIgnoreCase( "yes" ) || testAtt.equalsIgnoreCase( "true" ) ) ) {
				noDisplay = true;
			}
			processingUiGroup = false;
			topUiAbbrevLabelOf.put( currentVocabFieldName + newNodeName, currentTopLevelAbbrevLabel );
			//System.out.println( "topUiAbbrevLabelOf.put: " + currentVocabFieldName + newNodeName + "=" + currentTopLevelAbbrevLabel );
			VocabNode node = new VocabNodeTermsGroups( newNodeName, noDisplay, uiGroupLevelAt );
			current.item.add( node );
			currentNode = node;
			currentNode.setLabel( newNodeLabel );
			String srcUrl = (String)attributes.get( "src" );
			if ( srcUrl != null ) {
				currentNode.setSrc( srcUrl );
			}
		}
		else if ( localName.equals( "divider" ) ) {
			currentNode.setDivider( true );
		}
		String wrap = (String)attributes.get( "wrap" );
		if ( wrap != null ) {
			currentNode.setWrap( true );
		}
	}

	/**
	 *  (SAX) Invoked upon closing tag of an XML element
	 *
	 * @param  namespaceURI      XML namespace
	 * @param  localName         local tag name
	 * @param  qName             fully qualified tag name
	 * @exception  SAXException
	 */
	public void endElement( String namespaceURI, String localName,
	                        String qName ) throws SAXException {
		if ( parsingDefinitions ) {
			endElementDefinition( namespaceURI, localName, qName );
		}
		else {
			endElementUI( namespaceURI, localName, qName );
		}
		// NOTE: The following ensures that the characters() methods will not populate node
		// values, and this only works because node values only get populated with LEAF element
		// character data!  When using a DTD, ingorableWhitespace() returns the ignorable
		// whitespace, but using a schema causes that whitespace to be returned in the
		// characters() methods...
		currentSAXElementName = "";
	}

	/**
	 *  (SAX) End element for definitions
	 *
	 * @param  namespaceURI
	 * @param  localName
	 * @param  qName
	 * @exception  SAXException
	 */
	public void endElementDefinition( String namespaceURI, String localName,
	                                  String qName ) throws SAXException {
		outputXML.append( "</" + qName + ">" );
	}

	/**
	 *  (SAX) End element for UIs
	 *
	 * @param  namespaceURI
	 * @param  localName
	 * @param  qName
	 * @exception  SAXException
	 */
	public void endElementUI( String namespaceURI, String localName,
	                          String qName ) throws SAXException {
		if ( chars.length() > 0 ) {
			String label = chars.toString().replaceAll( "[\n\r]", " " ).trim();
			if ( currentSAXElementName.equals( "item" ) && ( label.length() > 0 ) ) {
				currentNode.setLabel( label );
				uiLabelOf.put( currentSAXElementSystem + currentVocabFieldName + newNodeName, label );
			}
			else if ( currentSAXElementName.equals( "definition" ) && ( label.length() > 0 ) ) {
				currentNode.setDefinition( label );
			}
		}
		chars.setLength( 0 );
		if ( localName.equals( "concept" ) || localName.equals( "group" ) ) {
			current = current.parent;
			if ( localName.equals( "group" ) ) {
				uiGroupLevelAt--;
			}
		}
	}

	/**
	 *  (SAX) Element data (characters between tags)
	 *
	 * @param  ch
	 * @param  start
	 * @param  length
	 * @exception  SAXException
	 */
	public void characters( char ch[], int start, int length ) throws SAXException {
		if ( parsingDefinitions ) {
			charactersDefinition( ch, start, length );
		}
		else {
			charactersUI( ch, start, length );
		}
	}

	/**
	 *  (SAX) Element data for definitions
	 *
	 * @param  ch
	 * @param  start
	 * @param  length
	 * @exception  SAXException
	 */
	public void charactersDefinition( char ch[], int start, int length ) throws SAXException {
		String charsValue = new String( ch, start, length );
		if ( currentSAXElementName.equals( "definition" ) ) {
			setDefinitions( currentSAXElementSystem, newNodeId, charsValue );
		}
		if ( startSavingXML ) {
			outputXML.append( charsValue );
		}
	}

	StringBuffer chars = new StringBuffer();

	/**
	 *  (SAX) Element data for UIs
	 *
	 * @param  ch                character array
	 * @param  start             starting index of character data
	 * @param  length            length of character data
	 * @exception  SAXException
	 */
	public void charactersUI( char ch[], int start, int length ) throws SAXException {
		chars.append( new String( ch, start, length ) );
	}

	/**
	 *  (SAX) Reports any whitespace that is ignored because it falls outside of
	 *  the DTD or schema definition--usefull for re-generating the file with
	 *  indents intact.
	 *
	 * @param  ch
	 * @param  start
	 * @param  length
	 * @exception  SAXException
	 */
	public void ignorableWhitespace( char ch[], int start, int length ) throws SAXException {
		if ( parsingDefinitions ) {
			outputXML.append( new String( ch, start, length ) );
		}
	}

	/**
	 *  (SAX) Required by SAX, but not used here
	 *
	 * @param  prefix
	 * @param  uri
	 * @exception  SAXException
	 */
	public void startPrefixMapping( String prefix, String uri ) throws SAXException {
	}

	/**
	 *  (SAX) Required by SAX, but not used here
	 *
	 * @param  prefix
	 * @exception  SAXException
	 */
	public void endPrefixMapping( String prefix ) throws SAXException {
	}

	/**
	 *  (SAX) Required by SAX, but not used here
	 *
	 * @param  target
	 * @param  data
	 * @exception  SAXException
	 */
	public void processingInstruction( String target, String data ) throws SAXException {
	}

	/**
	 *  (SAX) Required by SAX, but not used here
	 *
	 * @param  name
	 * @exception  SAXException
	 */
	public void skippedEntity( String name ) throws SAXException {
	}

	// Messages/Error handlers:
	//----------------------------------------------------------------------------------

	/**
	 *  Log a message
	 *
	 * @param  msg
	 */
	public void reportMessage( String msg ) {
		messages.add( msg );
		System.out.println( msg );
	}

	/**
	 *  Log an error
	 *
	 * @param  err
	 */
	public void reportError( String err ) {
		errors.add( err );
		System.out.println( "ERROR: " + err );
	}

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
	 *  Gets the currentVersion attribute of the MetadataVocabTermsGroups object
	 *
	 * @param  metaFramework
	 * @return                The currentVersion value
	 */
	public String getCurrentVersion( String metaFramework ) {
		System.out.println( "NEVER SUPPORTED" );
		return "NEVER SUPPORTED";
	}

	/**
	 *  Gets the translated FIELD NAME/ID of the given FIELD+VALUE NAMEs/IDs
	 *
	 * @param  field          metadata FIELD encoded ID (i.e. "gr") or metadata
	 *      NAME (i.e. "gradeRange")
	 * @param  metaFormat
	 * @param  metaVersion
	 * @return                The FIELD NAME/ID as translated from input FIELD
	 *      NAME/ID
	 * @exception  Exception
	 */
	public String getTranslatedField( String metaFormat, String metaVersion,
	                                  String field ) throws Exception {
		String ret = null;
		if ( fieldSystemId.get( field ) != null ) {
			ret = getFieldSystemId( field );
		}
		else {
			ret = getMetaNameOfId( "dds.descr.en-us", field, "" );
		}
		if ( ret == null ) {
			// necessary for proper behavior when dlese_collect is not loaded in MUI.
			if ("dlese_collect".equals(metaFormat) && "key".equals(field))
				return "ky";
			
			String err = "Vocab Manager: getTranslatedField is NULL for field " + field;
			reportError( err );
			throw new Exception( err );
		}
		if ( vocabCompare != null ) {
			String retNew = vocabCompare.getTranslatedField( metaFormat, metaVersion, field );
			alertVocabDiff( "getTranslatedField( '" + metaFormat + "', '" + metaVersion + "', '" + field + "' )", ret, retNew );
		}
		return ret;
	}

	/**
	 *  Gets the translatedField attribute of the MetadataVocabTermsGroups object
	 *
	 * @param  metaFormat
	 * @param  field
	 * @return                The translatedField value
	 * @exception  Exception
	 */
	public String getTranslatedField( String metaFormat, String field ) throws Exception {
		return getTranslatedField( metaFormat, "", field );
	}

	/**
	 *  Gets the translatedFieldPath attribute of the MetadataVocabTermsGroups
	 *  object
	 *
	 * @param  metaFormat
	 * @param  metaVersion
	 * @param  field
	 * @return                The translatedFieldPath value
	 * @exception  Exception
	 */
	public String getTranslatedFieldPath( String metaFormat, String metaVersion,
	                                      String field ) throws Exception {
		return "";
	}

	/**
	 *  Gets the translatedFieldPath attribute of the MetadataVocabTermsGroups
	 *  object
	 *
	 * @param  metaFormat
	 * @param  field
	 * @return                The translatedFieldPath value
	 * @exception  Exception
	 */
	public String getTranslatedFieldPath( String metaFormat, String field ) throws Exception {
		return "";
	}

	/**
	 *  Gets the translated VALUE NAME/ID of the given FIELD NAME/ID
	 *
	 * @param  field          metadata FIELD encoded ID (i.e. "gr") or metadata
	 *      NAME (i.e. "gradeRange")
	 * @param  value          metadata VALUE encoded ID (i.e. "04") or metadata
	 *      NAME (i.e. "DLESE:Intermediate elementary")
	 * @param  metaFormat
	 * @param  metaVersion
	 * @return                The VALUE NAME/ID as translated from input
	 *      FIELD+VALUE NAMEs/IDs
	 * @exception  Exception
	 */
	public String getTranslatedValue( String metaFormat, String metaVersion,
	                                  String field, String value ) throws Exception {
		String ret = null;
		if ( fieldValueSystemId.get( field + value ) != null ) {
			ret = getFieldValueSystemId( field, value );
		}
		else {
			ret = getMetaNameOfId( "dds.descr.en-us", field, value );
		}
		if ( ret == null ) {
			String err = "Vocab Manager: getTranslatedValue is NULL for field/value pair " + field
				 + "=" + value;
			reportError( err );
			throw new Exception( err );
		}
		if ( vocabCompare != null ) {
			String retNew = vocabCompare.getTranslatedValue( metaFormat, metaVersion, field, value );
			alertVocabDiff( "getTranslatedField( '" + metaFormat + "', '" + metaVersion + "', '"
				 + field + "', '" + value + "' )", ret, retNew );
		}
		return ret;
	}

	/**
	 *  Gets the translatedValue attribute of the MetadataVocabTermsGroups object
	 *
	 * @param  metaFormat
	 * @param  field
	 * @param  value
	 * @return                The translatedValue value
	 * @exception  Exception
	 */
	public String getTranslatedValue( String metaFormat,
	                                  String field, String value ) throws Exception {
		return getTranslatedValue( metaFormat, "", field, value );
	}

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
	 * @param  language     UI language, ie.e. "en-us"
	 * @return              The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiValueLabel( String metaFormat, String metaVersion,
	                               String audience, String language,
	                               String field, String value, boolean abbrev ) {
		return "NEVER SUPPORTED";
	}

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
	 * @param  language     UI language, ie.e. "en-us"
	 * @return              The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiValueLabel( String metaFormat, String metaVersion,
	                               String audience, String language,
	                               String field, String value ) {
		return "NEVER SUPPORTED";
	}

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
	 * @param  language    UI language, ie.e. "en-us"
	 * @param  abbrev      get the abbreviated version of the label?
	 * @return             The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiValueLabel( String metaFormat,
	                               String audience, String language,
	                               String field, String value, boolean abbrev ) {
		return "NEVER SUPPORTED";
	}

	/**
	 *  Gets the responseGroup attribute of the MetadataVocabOPML object
	 *
	 * @param  context
	 * @param  metaFramework  The new responseGroup value
	 * @param  metaVersion    The new responseGroup value
	 * @param  audience       The new responseGroup value
	 * @param  language       The new responseGroup value
	 * @param  field          The new responseGroup value
	 */
	public void setResponseGroup( PageContext context, String metaFramework, String metaVersion,
	                              String audience, String language, String field ) { }

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
	                              String audience, String language, String field ) { }


	/**
	 *  Caches a response value for rendering within proper order/grouping
	 *
	 * @param  value    the metadata vocab value, i.e. "DLESE:High school"
	 * @param  context  The new response value
	 */
	public void setResponseValue( String value, PageContext context ) { }

	/**
	 *  Gets the responseGroup attribute of the MetadataVocabOPML object
	 *
	 * @param  context
	 * @return          The responseGroup value
	 */
	public String getResponseOPML( PageContext context ) {
		return "NEVER SUPPORTED";
	}

	/**
	 *  Gets the UI label associated with the given FIELD NAME/ID
	 *
	 * @param  metaFormat   metadata format (i.e. "adn")
	 * @param  metaVersion  metadata version (i.e. "0.6.50")
	 * @param  abbrev       get the abbreviated version of the label?
	 * @param  audience     UI audience, i.e. "community" or "cataloger"
	 * @param  language     UI language, ie.e. "en-us"
	 * @param  field        metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @return              The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiFieldLabel( String metaFormat, String metaVersion,
	                               String audience, String language,
	                               String field, boolean abbrev ) {
		return "NEVER SUPPORTED";
	}

	/**
	 *  Gets the non-abbreviated UI label associated with the given FIELD NAME/ID
	 *
	 * @param  metaFormat   metadata format (i.e. "adn")
	 * @param  metaVersion  metadata version (i.e. "0.6.50")
	 * @param  audience     UI audience, i.e. "community" or "cataloger"
	 * @param  language     UI language, ie.e. "en-us"
	 * @param  field        metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @return              The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiFieldLabel( String metaFormat, String metaVersion,
	                               String audience, String language,
	                               String field ) {
		return "NEVER SUPPORTED";
	}

	/**
	 *  Gets the UI label associated with the given FIELD NAME/ID, using the <b>
	 *  current</b> or most recently loaded metadata format version number
	 *
	 * @param  metaFormat  metadata format (i.e. "adn")
	 * @param  field       metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  audience    UI audience, i.e. "community" or "cataloger"
	 * @param  language    UI language, ie.e. "en-us"
	 * @param  abbrev      get the abbreviated version of the label?
	 * @return             The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiFieldLabel( String metaFormat,
	                               String audience, String language,
	                               String field, boolean abbrev ) {
		return "NEVER SUPPORTED";
	}

	/**
	 *  Gets the OPML for a given format/version/audience/language
	 *
	 * @param  metaVersion  metadata version (i.e. "0.6.50")
	 * @param  audience     UI audience, i.e. "community" or "cataloger"
	 * @param  language     UI language, i.e. "en-us"
	 * @param  field        metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  metaFormat   metadata format (i.e. "adn")
	 * @return              OPML for the given format/audience
	 */
	public String getOPML( String metaFormat, String metaVersion, String audience, String language, String field ) {
		return "NEVER SUPPORTED";
	}

	/**
	 *  Gets the oPML attribute of the MetadataVocabTermsGroups object
	 *
	 * @param  metaFormat
	 * @param  audience
	 * @param  language
	 * @param  field
	 * @return             The oPML value
	 */
	public String getOPML( String metaFormat, String audience, String language, String field ) {
		return "NEVER SUPPORTED";
	}

	/**
	 *  Gets the oPML attribute of the MetadataVocabTermsGroups object
	 *
	 * @param  metaFormat
	 * @param  metaVersion
	 * @param  audience
	 * @param  language
	 * @param  field
	 * @param  includeXmlDeclaration
	 * @return                        The oPML value
	 */
	public String getOPML( String metaFormat, String metaVersion, String audience, String language, String field, boolean includeXmlDeclaration ) {
		return "NEVER SUPPORTED";
	}

	/**
	 *  Gets the uiValueLabel attribute of the MetadataVocabTermsGroups object
	 *
	 * @param  audience
	 * @param  language
	 * @param  field
	 * @param  value
	 * @param  abbrev
	 * @return           The uiValueLabel value
	 */
	public String getUiValueLabel( String audience, String language,
	                               String field, String value, boolean abbrev ) {
		return "NEVER SUPPORTED";
	}

	/**
	 *  Gets the uiFieldLabel attribute of the MetadataVocabTermsGroups object
	 *
	 * @param  audience
	 * @param  language
	 * @param  field
	 * @param  abbrev
	 * @return           The uiFieldLabel value
	 */
	public String getUiFieldLabel( String audience, String language,
	                               String field, boolean abbrev ) {
		return "NEVER SUPPORTED";
	}

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
	                                 String field, String value ) {
		return "NEVER SUPPORTED";
	}

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
	                                 String field, String value ) {
		return "NEVER SUPPORTED";
	}

	/**
	 *  Gets the oPML attribute of the MetadataVocabTermsGroups object
	 *
	 * @param  metaFormat
	 * @param  audience
	 * @param  language
	 * @param  field
	 * @param  includeXmlDeclaration
	 * @return                        The oPML value
	 */
	public String getOPML( String metaFormat, String audience, String language, String field, boolean includeXmlDeclaration ) {
		return "NEVER SUPPORTED";
	}

	/**
	 *  Gets the metaFormatOfField attribute of the MetadataVocabTermsGroups object
	 *
	 * @param  field
	 * @return        The metaFormatOfField value
	 */
	public String getMetaFormatOfField( String field ) {
		return "NEVER SUPPORTED";
	}

	/**
	 *  Gets the vocabNode attribute of the MetadataVocabTermsGroups object
	 *
	 * @param  metaFormat
	 * @param  audience
	 * @param  language
	 * @param  fieldName
	 * @param  valueName
	 * @return             The vocabNode value
	 */
	public VocabNode getVocabNode( String metaFormat, String audience, String language, String fieldName, String valueName ) {
		// NEVER SUPPORTED
		return new VocabNodeOPML( "" );
	}

	/**
	 *  Adds an ArrayList of metadata values to the re-ordering/grouping/labeling
	 *  cache
	 *
	 * @param  context  JSP page context
	 * @param  values   List of metadata VALUE encoded ID (i.e. "04") or metadata
	 *      NAME (i.e. "DLESE:Intermediate elementary")
	 * @see             MetadataVocab#setResponseGroup(PageContext,String,String,String,String,String)
	 * @see             MetadataVocab#getResponseOPML(PageContext)
	 */
	public void setResponseList( ArrayList values, PageContext context ) {
	}

	/**
	 *  Adds an ArrayList of metadata values to the re-ordering/grouping/labeling
	 *  cache
	 *
	 * @param  context  JSP page context
	 * @param  values   List of metadata VALUE encoded ID (i.e. "04") or metadata
	 *      NAME (i.e. "DLESE:Intermediate elementary")
	 * @see             MetadataVocab#setResponseGroup(PageContext,String,String,String,String,String)
	 * @see             MetadataVocab#getResponseOPML(PageContext)
	 */
	public void setResponseList( String[] values, PageContext context ) {
	}
}

