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
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.PageContext;
import org.dlese.dpc.util.strings.StringUtil;
import org.xml.sax.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.dds.action.*;
import org.dlese.dpc.webapps.tools.GeneralServletTools;
import org.dlese.dpc.util.GetURL;
import org.dlese.dpc.gui.OPMLTree;
import org.dlese.dpc.vocab.tags.opml.*;
import java.text.*;

/**
 *  <p>
 *
 *  MetadataVocabOPML is an implementation of MetadataVocab that uses an
 *  OPML-based framework to provide a set of methods for accessing mappings
 *  between metadata vocabulary encoded IDs and their term names, as well as
 *  rendering groups of controlled vocabularies in a variety of ways (such as
 *  simple HTML checkboxes, or dHTML flyouts).</p> <p>
 *
 *  UI renderings and mapping lookups are keyed in one of two ways: </p>
 *  <ul>
 *    <li> metaFormat, version (encoded ID &lt;--&gt; term name mappings)</li>
 *
 *    <li> audiene, language, metaFormat, version (UI renderings and label
 *    lookups)</li>
 *  </ul>
 *  <p>
 *
 *  NOTE: the version parameter is optional, as most methods have a version that
 *  does not take it. In these instances, the <b>current</b> , or most recently
 *  loaded version is assumed.</p> Instances of this class are loaded via
 *  LoadMetadataOPML and are placed into application scope via
 *  MetadataVocabServlet as an attribute named according to the context
 *  parameter "metadataVocabInstanceAttributeName".</p>
 *
 * @author    Ryan Deardorff
 */
public class MetadataVocabOPML implements MetadataVocab, Serializable {

	public final static String OPML_VERSION = "2.0";
	public final static String OPML_GROUPS_NAMESPACE = "xmlns:groups=\"http://www.dlese.org/Metadata/groups/\"";
	public final static String OPML_NAMESPACE_PREFIX = "groups:";
	private boolean debug = false;
	private String currentXmlFile = "";
	private String treeKey = "";                                   // current "framework/version/audience/language" key into trees
	private HashMap trees = new HashMap();                         // map containing one OPMLTree instance per key
	private HashMap vocabSystemInterfaces = new HashMap();         // "format/version/audience/language"
	private HashMap filenameOfFieldPath = new HashMap();           // "format/version/audience/language/field" maps to what OPML file?
	private ArrayList vocabFieldIds = new ArrayList();
	private HashMap seenVocabFieldIds = new HashMap();
	private ArrayList vocabFieldPaths = new ArrayList();
	private HashMap seenVocabFieldPaths = new HashMap();
	private HashMap metaFormatOfField = new HashMap();
	private ArrayList errors = new ArrayList();
	private OPMLTree currentTree = null;                           // one of many trees hashed into the trees maps
	private OPMLTree currentDefaultTree = null;                    // corresponding tree for default audience (has ids)
	private OPMLTree.TreeNode currentNode = null;
	private HashMap descriptions = new HashMap();
	// mappings for quick access w/out tree traversal:
	private static HashMap fieldValueIdPairExists = new HashMap(); // does the given field/value Id pair exist?
	private static HashMap translatedFields = new HashMap();       // NAME/ID FIELD translations
	private static HashMap translatedValues = new HashMap();       // NAME/ID VALUE translations
	private static HashMap translatedFieldPaths = new HashMap();   // PATHNAME/ID FIELD translations
	private static HashMap translatedValuePaths = new HashMap();   // PATHNAME/ID VALUE translations
	private static HashMap uiFieldLabels = new HashMap();          // metadata FIELD NAME/ID -> UI label
	private static HashMap uiValueLabels = new HashMap();          // metadata VALUE NAME/ID -> UI label
	private static HashMap uiValueDisplay = new HashMap();         // metadata VALUE NAME/ID -> UI "display" attribute
	private static HashMap currentVersions = new HashMap();        // stores most recently loaded framework version numbers
	private static HashMap vocabNodes = new HashMap();             // stores VocabNode representations

	private HashMap vocabTreeMenuCache = new HashMap();            // cache the JavaScript vocab flyout code
	private HashMap vocabTreeMenuCacheDate = new HashMap();        // date of current cache
	private final static int VOCAB_TREE_CACHE_TIME = 3000000;      // how long before refreshing cache (5 mins)

	private ArrayList messages = new ArrayList();

	/**
	 *  Constructor for the MetadataVocabOPML object
	 *
	 * @param  debug
	 * @param  loaderFile
	 * @param  servletContext
	 */
	public MetadataVocabOPML( boolean debug, String loaderFile, ServletContext servletContext ) {
		this.debug = debug;
	}

	/**
	 *  Gets the currentTree attribute of the MetadataVocabOPML object
	 *
	 * @return    The currentTree value
	 */
	public OPMLTree getCurrentTree() {
		return currentTree;
	}

	/**
	 *  Sets currentTree using its key (framework/version/audience/language/id)
	 *
	 * @param  key       system (framework/version/audience/language/id) key
	 * @param  subGroup  The new currentTree value
	 * @return           String containing just the metadata id
	 */
	public String setCurrentTree( String key, String subGroup ) {
		if ( key != null ) {
			String defaultKey = key.replaceFirst( "([^/]+/[^/]+)/[^/]+/(.+)", "$1/default/$2" );
			if ( (OPMLTree)trees.get( key ) == null ) {
				return "ERROR: Vocabulary group \"" + key + "\" does not exist";
			}
			treeKey = key;
			currentTree = (OPMLTree)trees.get( key );
			currentDefaultTree = (OPMLTree)trees.get( defaultKey );
			currentNode = currentTree.topMenu;
			if ( subGroup != null ) {
				setNode( subGroup );
			}
			int ind = key.lastIndexOf( "/" );
			if ( ind > -1 ) {
				return key.substring( ind + 1, key.length() );
			}
		}
		else {
			addError( "setCurrentTree() called with NULL String" );
		}
		return "";
	}

	/**
	 *  Sets the node attribute of the MetadataVocabOPML object
	 *
	 * @param  subGroup  The new node value
	 */
	private void setNode( String subGroup ) {
		String groupMatch = subGroup;
		int ind = groupMatch.indexOf( ":" );
		if ( ind > -1 ) {
			groupMatch = groupMatch.substring( ind, groupMatch.length() );
			subGroup = subGroup.substring( ind + 1, subGroup.length() );
		}
		else {
			subGroup = "";
		}
		for ( int i = 0; i < currentNode.treeNodes.size(); i++ ) {
			OPMLTree.TreeNode node = (OPMLTree.TreeNode)currentNode.treeNodes.get( i );
			if ( ( node.getAttribute( "text" ) != null ) &&
				( (String)node.getAttribute( "text" ) ).equals( groupMatch ) ) {
				i = currentNode.treeNodes.size();
				currentNode = node;
				if ( subGroup.length() > 0 ) {
					setNode( subGroup );
				}
			}
		}
	}

	/**
	 *  Sets the currentTree OPMLTree using the system info.
	 *
	 * @param  key  system (framework/version/audience/language/id) key
	 * @return      metadata id
	 */
	public String setCurrentTree( String key ) {
		return setCurrentTree( key, null );
	}

	/**
	 *  Gets the newTree attribute of the MetadataVocabOPML object
	 *
	 * @return    The newTree value
	 */
	public OPMLTree getNewTree() {
		currentTree = new OPMLTree();
		return currentTree;
	}

	/**
	 *  Sets the treeKey attribute of the MetadataVocabOPML object
	 *
	 * @param  key  The new treeKey value
	 */
	public void setTreeKey( String key ) {
		OPMLTree fromKey = (OPMLTree)trees.get( key );
		if ( fromKey != null ) {
			currentTree = fromKey;
		}
		else {
			trees.put( key, currentTree );
		}
	}

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
	 *  Gets the errors attribute of the MetadataUIManager object
	 *
	 * @return    The errors value
	 */
	public ArrayList getErrors() {
		return errors;
	}

	/**
	 *  Gets the vocabSystemInterfaces attribute of the MetadataUIManager object
	 *
	 * @return    The vocabSystemInterfaces value
	 */
	public Set getVocabSystemInterfaces() {
		return vocabSystemInterfaces.keySet();
	}

	/**
	 *  Gets the metadata value or field name of the given encoded field and value
	 *  Ids. Pass "" (empty string) for valueId to have it return the metadata
	 *  field name (instead of value).
	 *
	 * @param  system   Vocabulary framework/version/audience/language, i.e.
	 *      "adn/0.6.50/community/en-us"
	 * @param  fieldId  Encoded vocabulary field Id
	 * @param  valueId  Encoded vocabulary value Id
	 * @return          The metaNameOfId value
	 * @deprecated      As of MetadataUI v1.0, replaced by getTranslatedValue OR
	 *      getTranslatedField
	 * @see             #getTranslatedField(String,String,String,String,String)
	 * @see             #getTranslatedValue(String,String,String,String,String,String)
	 */
	public String getMetaNameOfId( String system, String fieldId, String valueId ) {
		if ( valueId.equals( "" ) ) {
			return (String)translatedFields.get( system + "/" + fieldId );
		}
		else {
			return (String)translatedValues.get( system + "/" + fieldId + valueId );
		}
	}

	/**
	 *  Does a vocabulary definition exist for the given encoded FIELD + VALUE IDs?
	 *
	 * @param  fieldId  Encoded vocabulary field Id
	 * @param  valueId  Encoded vocabulary value Id
	 * @return          The fieldValueIdPairExists value
	 */
	public boolean getFieldValueIdPairExists( String fieldId, String valueId ) {
		if ( fieldValueIdPairExists.get( fieldId + "=" + valueId ) != null ) {
			return true;
		}
		return false;
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
			ret = (String)translatedValues.get( field + value );
			if ( ret == null ) {
				throw new Exception( "Vocabulary Manager: getFieldValueSystemId is NULL for field/value pair "
					 + field + "=" + value );
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
		String ret = (String)translatedFields.get( field );
		if ( ret == null ) {
			throw new Exception( "Vocabulary Manager: getFieldSystemId is NULL for field " + field );
		}
		return ret;
	}

	/**
	 *  Physical filename of the OPML that loaded the given vocabulary field XPath
	 *
	 * @param  fieldPath  field XPath
	 * @param  format
	 * @param  version
	 * @param  audience
	 * @param  language
	 * @return            XML filename
	 */
	public String getFilenameOfFieldPath( String format, String version,
	                                      String audience, String language, String fieldPath ) {
		return (String)filenameOfFieldPath.get( format + "/" + version + "/"
			 + audience + "/" + language + "/" + fieldPath );
	}

	/**
	 *  Gets the fieldId attribute of the MetadataVocabOPML object
	 *
	 * @param  header
	 * @return         The fieldId value
	 */
	private String getFieldId( HashMap header ) {
		String ret = "";
		HashMap concept = (HashMap)header.get( "concept" );
		if ( concept != null ) {
			ret = (String)concept.get( "id" );
			if ( ret == null ) {
				ret = (String)concept.get( "path" );
			}
		}
		return ret;
	}

	/**
	 *  Gets the UI label of the given metadata field/value pair
	 *
	 * @param  system         Vocabulary framework/version/audience/language key,
	 *      i.e. "adn/0.6.50/community/en-us"
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
			ret = (String)uiValueLabels.get( system + metadataField + metadataValue + "abbrev" );
		}
		if ( !abbreviated || ( ret == null ) ) {
			ret = (String)uiValueLabels.get( system + metadataField + metadataValue );
		}
		if ( ret == null ) {
			prtln( "Vocabulary Manager: getUiLabelOf is NULL for (" + system + metadataField + metadataValue + ")" );
			ret = " ";
		}
		return ret;
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
	public String getTopLevelAbbrevLabelOf( String system,
	                                        String metadataField,
	                                        String systemFieldId,
	                                        String systemValueId ) {
		return "";
	}

	/**
	 *  Gets the vocabNodes attribute of the MetadataVocab object
	 *
	 * @param  metaFormat
	 * @param  audience
	 * @param  language
	 * @param  field
	 * @return             The vocabNodes value
	 */
	public ArrayList getVocabNodes( String metaFormat, String audience, String language, String field ) {
		return getVocabNodes( metaFormat + "/" + getCurrentVersion( metaFormat )
			 + "/" + audience + "/" + language + "/" + field, "" );
	}

	/**
	 *  Gets the vocabNodes attribute of the MetadataVocabOPML object
	 *
	 * @param  metaFormat
	 * @param  audience
	 * @param  language
	 * @param  field
	 * @param  group
	 * @return             The vocabNodes value
	 */
	public ArrayList getVocabNodes( String metaFormat, String audience, String language, String field,
	                                String group ) {
		return getVocabNodes( metaFormat + "/" + getCurrentVersion( metaFormat )
			 + "/" + audience + "/" + language + "/" + field, group );
	}

	/**
	 *  Gets the vocabNodes attribute of the MetadataVocabOPML object
	 *
	 * @param  system
	 * @param  group
	 * @return         The vocabNodes value
	 */
	public ArrayList getVocabNodes( String system,
	                                String group ) {
		ArrayList ret = new ArrayList();
		String inputName = setCurrentTree( system, group );
		iterateVocabNodes( currentNode, ret );
		return ret;
	}

	/**
	 *  Description of the Method
	 *
	 * @param  list
	 * @param  ret
	 */
	private void iterateVocabNodes( OPMLTree.TreeNode list,
	                                ArrayList ret ) {
		for ( int i = 0; i < list.treeNodes.size(); i++ ) {
			OPMLTree.TreeNode node = (OPMLTree.TreeNode)list.treeNodes.get( i );
			ret.add( new VocabNodeOPML( node ) );
			// VocabNode does the sub-iteration...
		}
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
	public String getVocabSelectList( String system,
	                                  String group,
	                                  int size,
	                                  MetadataVocabInputState inputState ) {
		return "";
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
		StringBuffer ret = new StringBuffer();
		String inputName = setCurrentTree( system, group );
		if ( inputName.startsWith( "ERROR:" ) ) {
			return errorDisplay( inputName, "getVocabCheckboxes" );
		}
		ret.append( "<table border='0' cellpadding='0' cellspacing='0' class='dlese_checkboxesTable'><td width='" + tdWidth + "' valign='top'>" )
			.append( vocabCheckboxes( currentNode, size, tdWidth, inputState, inputName, new CBCount( 0 ), skipTopRow ) )
			.append( "</td></table>" );
		return ret.toString();
	}

	/**
	 *  Description of the Method
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
	private synchronized String vocabCheckboxes( OPMLTree.TreeNode list,
	                                             int size,
	                                             String tdWidth,
	                                             MetadataVocabInputState inputState,
	                                             String name,
	                                             CBCount count,
	                                             boolean skipTopRow ) {
		StringBuffer ret = new StringBuffer();
		boolean isHeading = false;
		for ( int i = 0; i < list.treeNodes.size(); i++ ) {
			if ( ( count.value != 0 ) && ( ( count.value % size ) == 0 ) ) {
				ret.append( "</td><td width='" + tdWidth + "' valign='top'>" );
				if ( skipTopRow ) {
					ret.append( "<div class='vocabCheckboxLabels'>&nbsp;</div>" );
					count.value++;
				}
			}
			count.value++;
			OPMLTree.TreeNode node = (OPMLTree.TreeNode)list.treeNodes.get( i );
			if ( node.isHr ) {
				ret.append( "<hr/>" );
			}
			else if ( !node.isComment ) {
				String nodeId = getNodeId( node );
				isHeading = ( node.treeNodes.size() > 0 ) ? true : false;
				if ( isHeading ) {
					ret.append( "<div class='subCatHeading'>" );
				}
				else if ( ( node.getAttribute( "display" ) == null ) ||
					( !node.getAttribute( "display" ).equals( "false" ) ) ) {
					String chk = "";
					if ( ( inputState != null ) &&
						( inputState.isSelected( getFieldId( node.getHeader() ),
						(String)node.getAttribute( "id" ) ) ) ) {
						chk = " checked";
					}
					ret.append( "<div class='vocabCheckboxLabels'><input type='checkbox'" )
						.append( chk ).append( " name='" ).append( name ).append( "' value=\"" ).append( nodeId ).append( "\" id=\"" ).append( name ).append( nodeId )
						.append( "\">" );
				}
				if ( isHeading ) {
					ret.append( (String)node.getAttribute( "text" )
						 + ":</div><div class='subCatBlock'>\n" )
						.append( vocabCheckboxes( node, size, tdWidth, inputState, name, count, skipTopRow ) )
						.append( "</div>\n" );
				}
				else if ( ( node.getAttribute( "display" ) == null ) ||
					( !( (String)node.getAttribute( "display" ) ).equals( "false" ) ) ) {
					ret.append( "<label for=\"" )
						.append( name )
						.append( nodeId )
						.append( "\">" )
						.append( (String)node.getAttribute( "text" ) )
						.append( "</label></div>\n" );
				}
			}
		}
		return ret.toString();
	}

	/**
	 *  Gets the nodeId attribute of the MetadataVocabOPML object
	 *
	 * @param  node
	 * @return       The nodeId value
	 */
	public String getNodeId( OPMLTree.TreeNode node ) {
		return (String)node.getAttribute( "id" );
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
	public String getVocabCheckbox( String groupTop,
	                                String value,
	                                String label,
	                                MetadataVocabInputState inputState ) {
		return "";
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
		if ( vocabTreeMenuCache.get( system + language + group ) != null ) {
			Date now = new Date();
			if ( ( now.getTime() - ( (Date)vocabTreeMenuCacheDate.get( system + language + group ) ).getTime() )
				 < ( VOCAB_TREE_CACHE_TIME ) ) {
				return (String)vocabTreeMenuCache.get( system + language + group );
			}
		}
		StringBuffer ret = new StringBuffer();
		String inputName = setCurrentTree( system, group );
		if ( inputName.startsWith( "ERROR:" ) ) {
			return errorDisplay( inputName, "getVocabTreeMenu" );
		}
		try {
			String abbrevLabel = currentNode.getAttribute( "textAbbrev" );
			if ( abbrevLabel == null ) {
				abbrevLabel = (String)currentNode.getAttribute( "text" );
			}
			ret.append( "var tm_" + currentNode.fieldId + "0 = new dlese_vocabList( \"tm_" + currentNode.fieldId + "0\", 0, \""
				 + currentNode.getAttribute( "text" ) + "\", \"" + abbrevLabel + "\" );\n" );
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		String setList = "\ndlese_setList( \"" + currentNode.fieldId + "\" );\n";
		ret.append( vocabTreeMenu( currentNode, currentNode.fieldId + "0", currentNode.fieldId, page ) );
		ret.append( setList );
		vocabTreeMenuCache.put( system + language + group, ret.toString() );
		vocabTreeMenuCacheDate.put( system + language + group, new Date() );
		return ret.toString();
	}

	/**
	 *  Description of the Method
	 *
	 * @param  list
	 * @param  id
	 * @param  fieldId
	 * @param  page
	 * @return
	 */
	private synchronized String vocabTreeMenu( OPMLTree.TreeNode list,
	                                           String id,
	                                           String fieldId,
	                                           PageContext page ) {
		StringBuffer ret = new StringBuffer();
		boolean isHeading = false;
		int jsCount = 0;
		HttpSession session = page.getSession();
		for ( int i = 0; i < list.treeNodes.size(); i++ ) {
			OPMLTree.TreeNode node = (OPMLTree.TreeNode)list.treeNodes.get( i );
			isHeading = ( node.treeNodes.size() > 0 ) ? true : false;
			String wrap = "false";
			if ( ( node.getAttribute( "wrap" ) != null ) &&
				( node.getAttribute( "wrap" ).equals( "true" ) ) ) {
				wrap = "true";
			}
			String labelAbbrev = node.getAttribute( "textAbbrev" );
			if ( labelAbbrev == null ) {
				labelAbbrev = node.getAttribute( "text" );
			}
			String description = getDescription( node, page );
			if ( description != null ) {
				if ( description.length() > 1 ) {
					description = description.substring( 0, 1 ).toUpperCase() + description.substring( 1, description.length() );
				}
				description = ", null, \"" + description + "\"";
			}
			else {
				description = "";
			}
			if ( !node.isHr && !node.isComment
				 && ( ( node.getAttribute( "display" ) == null ) || !( ( (String)node.getAttribute( "display" ) ).equals( "false" ) ) ) ) {
				if ( isHeading ) {
					String varName = id + new Integer( jsCount++ ).toString();
					String inlineType = "0";
					String collapsible = node.getAttribute( "collapsible" );
					if ( ( collapsible != null ) && collapsible.toLowerCase().equals( "false" ) ) {
						collapsible = "1";
					}
					else {
						collapsible = "0";
					}
					ret.append( "var tm_" + varName + " = new dlese_vocabList( \"tm_" + varName + "\", " + collapsible + " );\n" );
					ret.append( "dlese_AV( tm_" + id + ", \"" + node.getAttribute( "text" ) + "\", \"" + labelAbbrev + "\", \""
						 + fieldId + "\", \"" + node.getAttribute( "id" ) + "\", false, " + wrap + ", tm_" + varName + description + " );\n" );
					ret.append( vocabTreeMenu( node, varName, fieldId, page ) );
				}
				else {
					jsCount++;
					ret.append( "dlese_AV( tm_" + id + ", \"" + node.getAttribute( "text" ) + "\", \"" + labelAbbrev + "\", \"" + fieldId
						 + "\", \"" + node.getAttribute( "id" ) + "\", false, " + wrap + ", null" + description + " );\n" );
				}
			}
		}
		return ret.toString();
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
		String inputName = setCurrentTree( system, group );
		if ( inputName.startsWith( "ERROR:" ) ) {
			return errorDisplay( inputName, "getVocabHiddenInputs" );
		}
		ret.append( vocabHiddenInputs( currentNode, inputState, inputName ) );
		return ret.toString();
	}

	/**
	 *  Description of the Method
	 *
	 * @param  list
	 * @param  inputState
	 * @param  name
	 * @return
	 */
	private synchronized String vocabHiddenInputs( OPMLTree.TreeNode list,
	                                               MetadataVocabInputState inputState,
	                                               String name ) {
		StringBuffer ret = new StringBuffer();
		for ( int i = 0; i < list.treeNodes.size(); i++ ) {
			OPMLTree.TreeNode node = (OPMLTree.TreeNode)list.treeNodes.get( i );
			if ( !node.isHr && !node.isComment ) {
				try {
					String fieldId = node.fieldId;
					String valueId = (String)node.getAttribute( "id" );
					if ( ( inputState != null ) && ( inputState.isSelected( fieldId, valueId ) ) ) {
						ret.append( "<input type='hidden' name='" )
							.append( fieldId )
							.append( "' value='" )
							.append( valueId )
							.append( "'/>\n" );
					}
				}
				catch ( Exception e ) {}
				if ( node.treeNodes.size() > 0 ) {
					ret.append( vocabHiddenInputs( node, inputState, name ) );
				}
			}
		}
		return ret.toString();
	}

	/**
	 *  Gets the metaFormatOfField attribute of the MetadataVocabOPML object
	 *
	 * @param  field
	 * @return        The metaFormatOfField value
	 */
	public String getMetaFormatOfField( String field ) {
		return (String)metaFormatOfField.get( field );
	}

	/**
	 *  Gets the vocabFieldIds attribute of the MetadataVocab object
	 *
	 * @return    The vocabFieldIds value
	 */
	public ArrayList getVocabFieldIds() {
		return vocabFieldIds;
	}

	/**
	 *  Gets the vocabFieldPaths attribute of the MetadataVocabOPML object
	 *
	 * @return    The vocabFieldPaths value
	 */
	public ArrayList getVocabFieldPaths() {
		return vocabFieldPaths;
	}

	/**
	 *  Invoked when all OPML files have been loaded
	 */
	public void doneLoading() {
	}

	/**
	 *  Load attributes that are only specified in the "default" audience groupings
	 *
	 * @param  list
	 */
	private void loadDefaultValues( OPMLTree.TreeNode list ) {
		for ( int i = 0; i < list.treeNodes.size(); i++ ) {
			OPMLTree.TreeNode node = (OPMLTree.TreeNode)list.treeNodes.get( i );
			if ( ( node != null ) &&
				( node.getAttribute( "id" ) == null ) &&
				( node.getAttribute( "vocab" ) != null ) ) {
				OPMLTree.TreeNode defaultNode = (OPMLTree.TreeNode)currentDefaultTree.nodesByVocab.get(
					(String)node.getAttribute( "vocab" ) );
				if ( defaultNode != null ) {
					if ( node.getAttribute( "id" ) == null ) {
						node.setAttribute( "id", defaultNode.getAttribute( "id" ) );
					}
					if ( node.getAttribute( "deftn" ) == null ) {
						node.setAttribute( "deftn", defaultNode.getAttribute( "deftn" ) );
					}
					if ( node.getAttribute( "attribution" ) == null ) {
						node.setAttribute( "attribution", defaultNode.getAttribute( "attribution" ) );
					}
				}
			}
			if ( !node.isHr && !node.isComment ) {
				if ( node.treeNodes.size() > 0 ) {
					loadDefaultValues( node );
				}
			}
		}
	}

	/**
	 *  Traverse the tree and load various value mappings, like vocab name to
	 *  encoded ID, vice versa, etc. Also populates VocabNode representations.
	 *
	 * @param  list
	 */
	private void loadValueMappings( OPMLTree.TreeNode list ) {
		HashMap cm = (HashMap)list.topTree.header.get( "concept" );
		if ( cm != null ) {
			String fieldPath = (String)cm.get( "path" );
		}
		for ( int i = 0; i < list.treeNodes.size(); i++ ) {
			OPMLTree.TreeNode node = (OPMLTree.TreeNode)list.treeNodes.get( i );
			if ( node != null ) {
				String system = node.topTree.conceptKey;
				int ind = system.indexOf( "//" );
				if ( ind > -1 ) {
					system = system.substring( 0, ind );
				}
				String valueId = node.getAttribute( "id" );
				String valueName = node.getAttribute( "vocab" );
				String fieldName = "";
				String fieldPath = "";
				String fieldId = "";
				String fieldLabel = "";
				String fieldLabelAbbrev = "";
				String metaFormat = "";
				String metaVersion = "";
				HashMap conceptMap = (HashMap)node.topTree.header.get( "concept" );
				if ( conceptMap != null ) {
					fieldPath = (String)conceptMap.get( "path" );
					if ( fieldPath.matches( ".*/.*" ) ) {
						fieldName = getNameFromPath( fieldPath );
					}
					fieldId = (String)conceptMap.get( "id" );
					fieldLabel = (String)conceptMap.get( "text" );
					fieldLabelAbbrev = (String)conceptMap.get( "text" );
					if ( fieldLabelAbbrev == null ) {
						fieldLabelAbbrev = fieldLabel;
					}
					metaFormat = (String)conceptMap.get( "metaFormat" );
					metaVersion = (String)conceptMap.get( "metaVersion" );
					// NOTE: the very last loaded becomes the "current" version, used so that lookups
					// and mappings can (but don't HAVE to) be requested in a "version agnostic" manner
					currentVersions.put( metaFormat, metaVersion );
				}
				// NOTE: remnant of old lookup w/out metadata format is faulty if/when two different
				// frameworks use the same name/id (still supplied for backwards compatibility):
				fieldValueIdPairExists.put( node.fieldId + "=" + node.getAttribute( "id" ), new Boolean( true ) );
				// Proper lookup:
				fieldValueIdPairExists.put( metaFormat + "/" + node.fieldId + "=" + node.getAttribute( "id" ), new Boolean( true ) );
				fieldId = (String)( (HashMap)node.topTree.getHeader().get( "concept" ) ).get( "id" );
				// NOTE: lookup w/out field *path* and name is faulty if two different formats use the same
				// name and/or ID (still supplied for backwards compatibility):
				translatedFields.put( metaFormat + "/" + metaVersion + "/" + fieldName, fieldId );
				translatedFields.put( metaFormat + "/" + metaVersion + "/" + fieldId, fieldName );
				translatedValues.put( metaFormat + "/" + metaVersion + "/" + fieldName + valueName, valueId );
				translatedValues.put( metaFormat + "/" + metaVersion + "/" + fieldName + valueId, valueName );
				translatedValues.put( metaFormat + "/" + metaVersion + "/" + fieldId + valueName, valueId );
				translatedValues.put( metaFormat + "/" + metaVersion + "/" + fieldId + valueId, valueName );
				translatedFields.put( metaFormat + "/" + metaVersion + "/" + fieldId, fieldName );
				// Proper lookup:
				translatedFieldPaths.put( metaFormat + "/" + metaVersion + "/" + fieldId, fieldPath );
				translatedValuePaths.put( metaFormat + "/" + metaVersion + "/" + fieldPath + valueName, valueId );
				translatedValuePaths.put( metaFormat + "/" + metaVersion + "/" + fieldPath + valueId, valueName );
				translatedValuePaths.put( metaFormat + "/" + metaVersion + "/" + fieldId + valueName, valueId );
				translatedValuePaths.put( metaFormat + "/" + metaVersion + "/" + fieldId + valueId, valueName );
				String label = node.getAttribute( "text" );
				String abbrevLabel = node.getAttribute( "textAbbrev" );
				if ( abbrevLabel == null ) {
					abbrevLabel = label;
				}
				String nodeDisplay = "true";
				if ( ( node.getAttribute( "display" ) != null ) &&
					node.getAttribute( "display" ).equals( "false" ) ) {
					abbrevLabel = label = "";
					nodeDisplay = "false";
				}
				uiValueLabels.put( system + "/" + fieldName + valueName + "abbrev", abbrevLabel );
				String sys = system + "/" + fieldName + valueName;
				uiValueLabels.put( system + "/" + fieldName + valueName, label );
				uiValueLabels.put( system + "/" + fieldName + valueId + "abbrev", abbrevLabel );
				uiValueLabels.put( system + "/" + fieldName + valueId, label );
				uiValueLabels.put( system + "/" + fieldPath + valueName + "abbrev", abbrevLabel );
				uiValueLabels.put( system + "/" + fieldPath + valueName, label );
				uiValueLabels.put( system + "/" + fieldPath + valueId + "abbrev", abbrevLabel );
				uiValueLabels.put( system + "/" + fieldPath + valueId, label );
				uiValueLabels.put( system + "/" + fieldId + valueId + "abbrev", abbrevLabel );
				uiValueLabels.put( system + "/" + fieldId + valueId, label );
				uiValueLabels.put( system + "/" + fieldId + valueName + "abbrev", abbrevLabel );
				uiValueLabels.put( system + "/" + fieldId + valueName, label );

				uiValueDisplay.put( system + "/" + fieldName + valueName, nodeDisplay );
				uiValueDisplay.put( system + "/" + fieldName + valueId, nodeDisplay );
				uiValueDisplay.put( system + "/" + fieldPath + valueName, nodeDisplay );
				uiValueDisplay.put( system + "/" + fieldPath + valueId, nodeDisplay );
				uiValueDisplay.put( system + "/" + fieldId + valueId, nodeDisplay );
				uiValueDisplay.put( system + "/" + fieldId + valueName, nodeDisplay );

				// NOTE: lookup w/out audience and language parameters is faulty if two different
				// formats use the same name and/or ID (still supplied for backwards compatibility):
				uiFieldLabels.put( fieldName, fieldLabel );
				uiFieldLabels.put( fieldPath, fieldLabel );
				uiFieldLabels.put( fieldId, fieldLabel );
				uiFieldLabels.put( fieldName + "abbrev", fieldLabelAbbrev );
				uiFieldLabels.put( fieldPath + "abbrev", fieldLabelAbbrev );
				uiFieldLabels.put( fieldId + "abbrev", fieldLabelAbbrev );
				// Proper lookup:
				uiFieldLabels.put( system + "/" + fieldName + "abbrev", fieldLabelAbbrev );
				uiFieldLabels.put( system + "/" + fieldPath + "abbrev", fieldLabelAbbrev );
				uiFieldLabels.put( system + "/" + fieldId + "abbrev", fieldLabelAbbrev );
				uiFieldLabels.put( system + "/" + fieldName, fieldLabel );
				uiFieldLabels.put( system + "/" + fieldPath, fieldLabel );
				uiFieldLabels.put( system + "/" + fieldId, fieldLabel );
				// VocabNode representations:
				VocabNode vn = new VocabNodeOPML( metaFormat, node );
				vocabNodes.put( system + "/" + fieldName + valueName, vn );
				vocabNodes.put( system + "/" + fieldPath + valueName, vn );
				vocabNodes.put( system + "/" + fieldId + valueName, vn );
				vocabNodes.put( system + "/" + fieldName + valueId, vn );
				vocabNodes.put( system + "/" + fieldPath + valueId, vn );
				vocabNodes.put( system + "/" + fieldId + valueId, vn );
				vocabSystemInterfaces.put( system, new Boolean( true ) );
				filenameOfFieldPath.put( system + "/" + fieldPath, currentXmlFile );
			}
			if ( !node.isHr && !node.isComment && ( node.treeNodes.size() > 0 ) ) {
				loadValueMappings( node );
			}
		}
	}

	/**
	 *  Gets the vocab field name by clipping from the end of a full xpath
	 *
	 * @param  fieldPath
	 * @return            The nameFromPath value
	 */
	public String getNameFromPath( String fieldPath ) {
		int ind = fieldPath.lastIndexOf( "/" );
		String ret = fieldPath.substring( ind + 1, fieldPath.length() );
		// XPath attributes:
		if ( ret.startsWith( "@" ) ) {
			ret = ret.substring( 1, ret.length() );
		}
		return ret;
	}

	/**
	 *  Description of the Method
	 *
	 * @param  system
	 * @param  fieldName
	 * @param  valueName
	 * @return
	 */
	public VocabNode findVocabNode( String system, String fieldName, String valueName ) {
		VocabNode ret = new VocabNodeTermsGroups( "", false, 0 );
		return ret;
	}

	/**
	 *  CBCount = CheckBoxesCount, used to render Javascript "All | Clear" links
	 */
	class CBCount {
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
	 *  (SAX) Sets the SAX locator, which indicates the current position of the
	 *  parser within the document (line number, column number). Could be used to
	 *  indicate the spot where an error occured.
	 *
	 * @param  locator  The new saxLocator value
	 */
	public void setDocumentLocator( Locator locator ) {
		String xmlFile = locator.getSystemId();
		// Trim "file:///" from file string:
		xmlFile = xmlFile.substring( 8, xmlFile.length() );
		// On Windows, the above will result in C:/... but on UNIX there
		// will be no leading '/', so we must add it:
		if ( xmlFile.indexOf( ':' ) == -1 ) {
			xmlFile = '/' + xmlFile;
		}
		if ( currentTree != null ) {
			currentTree.setDocumentLocator( locator );
		}
		// System.out.println( "Parsing OPML -- " + xmlFile );
		currentXmlFile = xmlFile;
	}

	/**
	 *  (SAX) Invoked at the start of any document parse
	 *
	 * @exception  SAXException
	 */
	public void startDocument() throws SAXException {
		getNewTree();
		currentTree.startDocument();
	}

	/**
	 *  (SAX) Invoked at the end of parsing. Rewrite the definitions XML if new Ids
	 *  have been assigned.
	 *
	 * @exception  SAXException
	 */
	public void endDocument() throws SAXException {
		if ( currentTree != null ) {
			currentTree.endDocument();
			setCurrentTree( currentTree.conceptKey );
			if ( currentDefaultTree != null ) {
				loadDefaultValues( currentTree.topMenu );
				loadValueMappings( currentTree.topMenu );
			}
		}
	}

	/**
	 *  (SAX) Invoked upon opening tag of an XML element
	 *
	 * @param  namespaceURI
	 * @param  lName
	 * @param  qName
	 * @param  atts
	 * @exception  SAXException
	 */
	public void startElement( String namespaceURI,
	                          String lName,
	                          String qName,
	                          Attributes atts ) throws SAXException {
		if ( currentTree != null ) {
			currentTree.startElement( namespaceURI, lName, qName, atts );
		}
		if ( lName.equals( "concept" ) ) {
			// provide lookup by full vocab field xpath, i.e. "/itemRecord/educational/resourceTypes/resourceType"
			String key = (String)atts.getValue( "metaFormat" ) + "/" + (String)atts.getValue( "metaVersion" )
				 + "/" + (String)atts.getValue( "audience" ) + "/" + (String)atts.getValue( "language" )
				 + "/" + (String)atts.getValue( "path" );
			setTreeKey( key );
			if ( atts.getValue( "id" ) != null ) {
				// provide lookup by vocab encoded field id, i.e. "re":
				key = (String)atts.getValue( "metaFormat" ) + "/" + (String)atts.getValue( "metaVersion" )
					 + "/" + (String)atts.getValue( "audience" ) + "/" + (String)atts.getValue( "language" )
					 + "/" + (String)atts.getValue( "id" );
				setTreeKey( key );
				if ( seenVocabFieldIds.get( atts.getValue( "id" ) ) == null ) {
					vocabFieldIds.add( atts.getValue( "id" ) );
					seenVocabFieldIds.put( atts.getValue( "id" ), new Boolean( true ) );
				}
				metaFormatOfField.put( atts.getValue( "id" ), atts.getValue( "metaFormat" ) );
				metaFormatOfField.put( atts.getValue( "path" ), atts.getValue( "metaFormat" ) );
			}
			if ( atts.getValue( "path" ) != null ) {
				if ( seenVocabFieldPaths.get( atts.getValue( "path" ) ) == null ) {
					vocabFieldPaths.add( atts.getValue( "path" ) );
					seenVocabFieldPaths.put( atts.getValue( "path" ), new Boolean( true ) );
				}
			}
			// provide lookup by vocab field name, i.e. "resourceType":
			key = (String)atts.getValue( "metaFormat" ) + "/" + (String)atts.getValue( "metaVersion" )
				 + "/" + (String)atts.getValue( "audience" ) + "/" + (String)atts.getValue( "language" )
				 + "/" + getNameFromPath( (String)atts.getValue( "path" ) );
			setTreeKey( key );
			// NOTE: that last lookup by field name is faulty if two different xpaths have the same name!
			// Best to use lookup by xpath or encoded id
		}
	}

	/**
	 *  (SAX) Invoked upon closing tag of an XML element
	 *
	 * @param  namespaceURI      XML namespace
	 * @param  lName             local tag name
	 * @param  qName             fully qualified tag name
	 * @exception  SAXException
	 */
	public void endElement( String namespaceURI, String lName,
	                        String qName ) throws SAXException {
		if ( currentTree != null ) {
			currentTree.endElement( namespaceURI, lName, qName );
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
		if ( currentTree != null ) {
			currentTree.characters( ch, start, length );
		}
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
		if ( currentTree != null ) {
			currentTree.ignorableWhitespace( ch, start, length );
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
		if ( currentTree != null ) {
			currentTree.startPrefixMapping( prefix, uri );
		}
	}

	/**
	 *  (SAX) Required by SAX, but not used here
	 *
	 * @param  prefix
	 * @exception  SAXException
	 */
	public void endPrefixMapping( String prefix ) throws SAXException {
		if ( currentTree != null ) {
			currentTree.endPrefixMapping( prefix );
		}
	}

	/**
	 *  (SAX) Required by SAX, but not used here
	 *
	 * @param  target
	 * @param  data
	 * @exception  SAXException
	 */
	public void processingInstruction( String target, String data ) throws SAXException {
		if ( currentTree != null ) {
			currentTree.processingInstruction( target, data );
		}
	}

	/**
	 *  (SAX) Required by SAX, but not used here
	 *
	 * @param  name
	 * @exception  SAXException
	 */
	public void skippedEntity( String name ) throws SAXException {
		if ( currentTree != null ) {
			currentTree.skippedEntity( name );
		}
	}

	/**
	 *  Display vocab error as HTML
	 *
	 * @param  err
	 * @param  method
	 * @return
	 */
	public String errorDisplay( String err, String method ) {
		return "<div style='font-weight: bold; color: red;'>Error in MetadataVocabOPML."
			 + method + "(): " + err.substring( 7, err.length() ) + "</div>";
	}

	/**
	 *  Gets the description from HTTP request of SRC URL
	 *
	 * @param  page
	 * @param  node
	 * @return       The description value
	 */
	public synchronized String getDescription( OPMLTree.TreeNode node, PageContext page ) {
		String description = (String)descriptions.get( node );
		String src = node.getAttribute( "url" );
		if ( ( description == null ) || ( description.equals( "" ) ) ) {
			if ( src != null ) {
				if ( src.indexOf( "http://" ) == -1 ) {
					src = GeneralServletTools.getContextUrl( (HttpServletRequest)page.getRequest() ) + src;
				}
				description = GetURL.getUrl( src, false );
				int ind = description.indexOf( ". " );
				int ind2 = 0;
				if ( ind > -1 ) {
					ind2 = description.indexOf( ". ", ind + 1 );
					if ( ind2 > -1 ) {
						description = description.substring( 0, ind2 + 1 );
					}
				}
				if ( description.length() > 350 ) {
					ind2 = description.indexOf( " ", 350 );
					if ( ind2 > -1 ) {
						description = description.substring( 0, ind2 ) + "...";
					}
				}
				if ( ( description.indexOf( "<title>" ) > -1 ) ||
					( description.indexOf( "<TITLE>" ) > -1 ) ) {              // 404 not found case
					return null;
				}
				if ( !description.matches( "[\\s\\S]*\\S[\\s\\S]*" ) ) {
					// return null so that the next request will try again
					return null;
				}
			}
			else if ( node.getAttribute( "deftn" ) != null ) {
				description = ( (String)node.getAttribute( "deftn" ) );
			}
			if ( description != null ) {
				description = description.replaceAll( "[\\r\\n]", " " );
				descriptions.put( node, description );
			}
		}
		if ( description == null ) {
			description = "";
		}
		return description;
	}

	/**
	 *  Get the most recently loaded metadata format version number
	 *
	 * @param  metaFormat  metadata format (i.e. "adn")
	 * @return             The current (most recently loaded) version for the given
	 *      format
	 */
	public String getCurrentVersion( String metaFormat ) {
		String ret = (String)currentVersions.get( metaFormat );
		if ( ret == null ) {
			prtln( "Metadata UI Manager: getCurrentVersion is NULL for metaFormat " + metaFormat );
			ret = "1.0.00";
		}
		return ret;
	}

	/**
	 *  Gets a VocabNode for the given field/value pair
	 *
	 * @param  metaFormat   metadata format (i.e. "adn")
	 * @param  audience     UI audience, i.e. "community" or "cataloger"
	 * @param  language     UI language, i.e. "en-us"
	 * @param  fieldName    vocab field
	 * @param  valueName    vocab value
	 * @param  metaVersion
	 * @return              VocabNode for the vocab
	 */
	public VocabNode getVocabNode( String metaFormat, String metaVersion, String audience, String language, String fieldName, String valueName ) {
		return (VocabNode)vocabNodes.get( metaFormat + "/" + metaVersion + "/" + audience + "/" + language + "/" + fieldName + valueName );
	}

	/**
	 *  Gets the vocabNode attribute of the MetadataVocabOPML object
	 *
	 * @param  metaFormat
	 * @param  audience
	 * @param  language
	 * @param  fieldName
	 * @param  valueName
	 * @return             The vocabNode value
	 */
	public VocabNode getVocabNode( String metaFormat, String audience, String language, String fieldName, String valueName ) {
		return getVocabNode( metaFormat, getCurrentVersion( metaFormat ), audience, language, fieldName, valueName );
	}

	/* ===========================================================================================
	 *  v2.0 methods, deprecating the following:
	 *  <ul>
	 *    <li> getFieldValueSystemId()</li>
	 *    <li> getFieldSystemId()</li>
	 *    <li> getMetaNameOfId()</li>
	 *    <li> getUiLabelOf()</li>
	 *    <li> getUiLabelOfSystemIds()</li>
	 *    <li> getUiLabelOfFieldId()</li>
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
	 *      NAME/ID
	 * @exception  Exception
	 */
	public String getTranslatedField( String metaFormat, String metaVersion,
	                                  String field ) throws Exception {
		String ret = (String)translatedFields.get( metaFormat + "/" + metaVersion + "/" + field );
		if ( ret == null ) {
			// necessary for proper behavior when dlese_collect is not loaded in MUI.
			if ("dlese_collect".equals(metaFormat) && "key".equals(field))
				return "ky";
			throw new Exception( "Metadata UI Manager: getTranslatedField is NULL for field " + field + " format " + metaFormat + " version " + metaVersion );
		}
		return ret;
	}

	/**
	 *  Gets the translated FIELD NAME/ID of the given FIELD NAME/ID using the <b>
	 *  current</b> or most recently loaded metadata format version number
	 *
	 * @param  field          metadata FIELD encoded ID (i.e. "gr") or metadata
	 *      NAME (i.e. "gradeRange")
	 * @param  metaFormat     metadata format (i.e. "adn")
	 * @return                The FIELD NAME/ID as translated from input FIELD
	 *      NAME/ID
	 * @exception  Exception
	 */
	public String getTranslatedField( String metaFormat,
	                                  String field ) throws Exception {
		return getTranslatedField( metaFormat, getCurrentVersion( metaFormat ), field );
	}

	/**
	 *  Gets the translated FIELD XPATH of the given FIELD+VALUE NAMES/IDS
	 *
	 * @param  field          metadata FIELD encoded ID (i.e. "gr") or metadata
	 *      NAME (i.e. "gradeRange")
	 * @param  metaFormat     metadata format (i.e. "adn")
	 * @param  metaVersion    metadata version (i.e. "0.6.50")
	 * @return                The FIELD XPATH as translated from input FIELD
	 *      NAME/ID
	 * @exception  Exception
	 */
	public String getTranslatedFieldPath( String metaFormat, String metaVersion,
	                                      String field ) throws Exception {
		String ret = (String)translatedFieldPaths.get( metaFormat + "/" + metaVersion + "/" + field );
		if ( ret == null ) {
			throw new Exception( "Metadata UI Manager: getTranslatedFieldPath is NULL for field " + field + " format " + metaFormat + " version " + metaVersion );
		}
		return ret;
	}

	/**
	 *  Gets the translated FIELD XPATH of the given FIELD+VALUE NAMES/IDS using
	 *  the <b> current</b> or most recently loaded metadata format version number
	 *
	 * @param  field          metadata FIELD encoded ID (i.e. "gr") or metadata
	 *      NAME (i.e. "gradeRange")
	 * @param  metaFormat     metadata format (i.e. "adn")
	 * @return                The FIELD XPATH as translated from input FIELD
	 *      NAME/ID
	 * @exception  Exception
	 */
	public String getTranslatedFieldPath( String metaFormat,
	                                      String field ) throws Exception {
		return getTranslatedFieldPath( metaFormat, getCurrentVersion( metaFormat ), field );
	}

	/**
	 *  Gets the translated VALUE NAME/ID of the given FIELD+VALUE NAMES/IDS
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
	public String getTranslatedValue( String metaFormat, String metaVersion,
	                                  String field, String value ) throws Exception {
		String ret = (String)translatedValues.get( metaFormat + "/" + metaVersion + "/" + field + value );
		if ( ret == null ) {
			throw new Exception( "Metadata UI Manager: getTranslatedValue is NULL for field/value pair " + field
				 + "=" + value + " format " + metaFormat + " version " + metaVersion );
		}
		return ret;
	}

	/**
	 *  Gets the translated VALUE NAME/ID of the given FIELD+VALUE NAMES/IDS using
	 *  the <b> current</b> or most recently loaded metadata format version number
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
	public String getTranslatedValue( String metaFormat,
	                                  String field, String value ) throws Exception {
		return getTranslatedValue( metaFormat, getCurrentVersion( metaFormat ), field, value );
	}


	/**
	 *  Does the given field/value pair exist in this MetadataVocab instance?
	 *
	 * @param  field        metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  value        metadata VALUE encoded ID (i.e. "04") or metadata NAME
	 *      (i.e. "DLESE:Intermediate elementary")
	 * @param  metaFormat   metadata format (i.e. "adn")
	 * @param  metaVersion  metadata version (i.e. "0.6.50")
	 * @return              true if it exists
	 */
	public boolean hasValue( String metaFormat, String metaVersion,
	                         String field, String value ) {
		String val = (String)translatedValues.get( metaFormat + "/" + metaVersion + "/" + field + value );
		if ( val == null ) {
			return false;
		}
		return true;
	}

	/**
	 *  Does the given field/value pair (in the CURRENT version of the given
	 *  framework) exist in this MetadataVocab instance?
	 *
	 * @param  field       metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  value       metadata VALUE encoded ID (i.e. "04") or metadata NAME
	 *      (i.e. "DLESE:Intermediate elementary")
	 * @param  metaFormat  metadata format (i.e. "adn")
	 * @return             true if it exists
	 */
	public boolean hasValue( String metaFormat,
	                         String field, String value ) {
		return hasValue( metaFormat, getCurrentVersion( metaFormat ), field, value );
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
	 * @param  language     UI language, i.e. "en-us"
	 * @return              The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiValueLabel( String metaFormat, String metaVersion,
	                               String audience, String language,
	                               String field, String value, boolean abbrev ) {
		String abbrevAppend = "";
		if ( abbrev ) {
			abbrevAppend = "abbrev";
		}
		String ret = (String)uiValueLabels.get( metaFormat + "/" + metaVersion + "/" + audience + "/" + language + "/" + field + value + abbrevAppend );
		if ( ret == null ) {
			ret = "<!-- MUI: getUiValueLabel is NULL for field/value pair " + field + "=" + value + " -->";
		}
		return ret;
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
	 * @param  language     UI language, i.e. "en-us"
	 * @return              The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiValueLabel( String metaFormat, String metaVersion,
	                               String audience, String language,
	                               String field, String value ) {
		return getUiValueLabel( metaFormat, metaVersion,
			audience, language, field, value, false );
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
	 * @param  language    UI language, i.e. "en-us"
	 * @param  abbrev      get the abbreviated version of the label?
	 * @return             The user interface label associated with the given
	 *      format/version/audience/language value
	 */
	public String getUiValueLabel( String metaFormat,
	                               String audience, String language,
	                               String field, String value, boolean abbrev ) {
		return getUiValueLabel( metaFormat, getCurrentVersion( metaFormat ),
			audience, language, field, value, abbrev );
	}

	/**
	 *  Gets the uiValueLabel attribute of the MetadataVocabOPML object
	 *
	 * @param  audience  UI audience, i.e. "community" or "cataloger"
	 * @param  language  UI language, i.e. "en-us"
	 * @param  field     metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  abbrev    return the "textAbbrev" version of the label?
	 * @param  value
	 * @return           The uiValueLabel value
	 */
	public String getUiValueLabel( String audience, String language,
	                               String field, String value, boolean abbrev ) {
		String metaFormat = (String)metaFormatOfField.get( field );
		return getUiValueLabel( metaFormat, getCurrentVersion( metaFormat ),
			audience, language, field, value, abbrev );
	}

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
	                               String field, boolean abbrev ) {
		String abbrevAppend = "";
		if ( abbrev ) {
			abbrevAppend = "abbrev";
		}
		String ret = (String)uiFieldLabels.get( metaFormat + "/" + metaVersion + "/" + audience + "/" + language + "/" + field + abbrevAppend );
		if ( ret == null ) {
			ret = "<!-- MUI: getUiFieldLabel is NULL for field " + field + " -->";
		}
		return ret;
	}

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
	                               String field ) {
		return getUiFieldLabel( metaFormat, getCurrentVersion( metaFormat ),
			audience, language, field, false );
	}

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
	                               String field, boolean abbrev ) {
		return getUiFieldLabel( metaFormat, getCurrentVersion( metaFormat ),
			audience, language, field, abbrev );
	}

	/**
	 *  Gets the uiFieldLabel attribute of the MetadataVocabOPML object
	 *
	 * @param  audience  UI audience, i.e. "community" or "cataloger"
	 * @param  language  UI language, i.e. "en-us"
	 * @param  field     metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  abbrev    return the "textAbbrev" version of the label?
	 * @return           The uiFieldLabel value
	 */
	public String getUiFieldLabel( String audience, String language,
	                               String field, boolean abbrev ) {
		String metaFormat = (String)metaFormatOfField.get( field );
		return getUiFieldLabel( metaFormat, getCurrentVersion( metaFormat ),
			audience, language, field, abbrev );
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
		String ret = (String)uiValueDisplay.get( metaFormat + "/" + metaVersion + "/" + audience + "/" + language + "/" + field + value );
		if ( ret == null ) {
			ret = "<!-- MUI: getUiValueDisplay is NULL for field/value pair " + field + "=" + value + " -->";
		}
		return ret;
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
		return getUiValueDisplay( metaFormat, getCurrentVersion( metaFormat ),
			audience, language, field, value );
	}

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
	public synchronized void setResponseGroup( PageContext context, String metaFormat, String metaVersion,
	                                           String audience, String language, String field ) {
		MetadataVocabResponseMap responseMap = new MetadataVocabResponseMap( metaFormat, metaVersion,
			audience, language, field );
		context.getServletContext().setAttribute( "metadataVocabResponseMap", responseMap );
	}

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
	public synchronized void setResponseGroup( PageContext context, String metaFormat,
	                                           String audience, String language, String field ) {
		setResponseGroup( context, metaFormat, getCurrentVersion( metaFormat ), audience, language, field );
	}

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
	public synchronized void setResponseValue( String value, PageContext context ) {
		MetadataVocabResponseMap responseMap = (MetadataVocabResponseMap)context.findAttribute( "metadataVocabResponseMap" );
		if ( responseMap != null ) {
			responseMap.setValue( value );
		}
	}

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
	public synchronized void setResponseList( ArrayList values, PageContext context ) {
		MetadataVocabResponseMap responseMap = (MetadataVocabResponseMap)context.findAttribute( "metadataVocabResponseMap" );
		if ( responseMap != null ) {
			for ( int i = 0; i < values.size(); i++ ) {
				responseMap.setValue( (String)values.get( i ) );
			}
		}
	}

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
	public synchronized void setResponseList( String[] values, PageContext context ) {
		MetadataVocabResponseMap responseMap = (MetadataVocabResponseMap)context.findAttribute( "metadataVocabResponseMap" );
		if ( responseMap != null ) {
			for ( int i = 0; i < values.length; i++ ) {
				responseMap.setValue( values[i] );
			}
		}
	}

	/**
	 *  Gets the re-ordered/grouped/labeled OPML tree of metadata values from the
	 *  cache created by setResponseGroup()
	 *
	 * @param  context  JSP page context
	 * @return          OPML for the group specified with setResponseGroup() and
	 *      trimmed to the subset indicated by values passed into setResponse()
	 * @see             MetadataVocab#setResponseValue(String,PageContext)
	 * @see             MetadataVocab#setResponseList(String[],PageContext)
	 * @see             MetadataVocab#setResponseList(ArrayList,PageContext)
	 * @see             MetadataVocab#setResponseGroup(PageContext,String,String,String,String,String)
	 */
	public synchronized String getResponseOPML( PageContext context ) {
		String ret = "";
		MetadataVocabResponseMap responseMap = (MetadataVocabResponseMap)context.findAttribute( "metadataVocabResponseMap" );
		if ( responseMap == null ) {
			ret = "<!-- MUI ERROR: metadataVocabResponseMap is empty -->";
		}
		if ( ( responseMap.metaVersion == null ) || responseMap.metaVersion.equals( "" ) ) {
			ret = getOPML( responseMap.metaFormat, getCurrentVersion( responseMap.metaFormat ),
				responseMap.audience, responseMap.language, responseMap.field, responseMap, false );
		}
		else {
			ret = getOPML( responseMap.metaFormat, responseMap.metaVersion,
				responseMap.audience, responseMap.language, responseMap.field, responseMap, false );
		}
		if ( ret.indexOf( "<outline" ) == -1 ) {
			ret = "";
		}
		return ret;
	}

	/**
	 *  Gets the OPML for a given format/version/audience/language
	 *
	 * @param  metaVersion            metadata version (i.e. "0.6.50")
	 * @param  audience               UI audience, i.e. "community" or "cataloger"
	 * @param  language               UI language, i.e. "en-us"
	 * @param  field                  metadata FIELD encoded ID (i.e. "gr") or
	 *      metadata NAME (i.e. "gradeRange")
	 * @param  metaFormat             metadata format (i.e. "adn")
	 * @param  includeXmlDeclaration
	 * @return                        OPML for the given format/audience
	 */
	public synchronized String getOPML( String metaFormat, String metaVersion, String audience, String language, String field, boolean includeXmlDeclaration ) {
		return getOPML( metaFormat, metaVersion, audience, language, field, null, includeXmlDeclaration );
	}

	/**
	 *  Gets the OPML for a given format/audience/language using the <b>current</b>
	 *  or most recently loaded version
	 *
	 * @param  audience               UI audience, i.e. "community" or "cataloger"
	 * @param  language               UI language, i.e. "en-us"
	 * @param  field                  metadata FIELD encoded ID (i.e. "gr") or
	 *      metadata NAME (i.e. "gradeRange")
	 * @param  metaFormat             metadata format (i.e. "adn")
	 * @param  includeXmlDeclaration
	 * @return                        OPML for the given format/audience
	 */
	public synchronized String getOPML( String metaFormat, String audience, String language, String field, boolean includeXmlDeclaration ) {
		return getOPML( metaFormat, getCurrentVersion( metaFormat ), audience, language, field, null, includeXmlDeclaration );
	}

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
	public synchronized String getOPML( String metaFormat, String metaVersion, String audience, String language, String field ) {
		return getOPML( metaFormat, metaVersion, audience, language, field, null, false );
	}

	/**
	 *  Gets the OPML for a given format/audience/language using the <b>current</b>
	 *  or most recently loaded version <b>without the XML declaration tag</b>
	 *
	 * @param  audience    UI audience, i.e. "community" or "cataloger"
	 * @param  language    UI language, i.e. "en-us"
	 * @param  field       metadata FIELD encoded ID (i.e. "gr") or metadata NAME
	 *      (i.e. "gradeRange")
	 * @param  metaFormat  metadata format (i.e. "adn")
	 * @return             OPML for the given format/audience
	 */
	public synchronized String getOPML( String metaFormat, String audience, String language, String field ) {
		return getOPML( metaFormat, getCurrentVersion( metaFormat ), audience, language, field, null, false );
	}


	/**
	 *  Gets the OPML for a given format/version/audience/language
	 *
	 * @param  metaVersion            metadata version (i.e. "0.6.50")
	 * @param  audience               UI audience, i.e. "community" or "cataloger"
	 * @param  language               UI language, i.e. "en-us"
	 * @param  field                  metadata FIELD encoded ID (i.e. "gr") or
	 *      metadata NAME (i.e. "gradeRange")
	 * @param  metaFormat             metadata format (i.e. "adn")
	 * @param  responseMap            ResponseMap (can be NULL) holding values from
	 *      a response (indexer/service record)
	 * @param  includeXmlDeclaration
	 * @return                        OPML for the given format/audience
	 */
	private synchronized String getOPML( String metaFormat, String metaVersion, String audience, String language, String field,
	                                     MetadataVocabResponseMap responseMap, boolean includeXmlDeclaration ) {
		StringBuffer ret = new StringBuffer();
		String inputName = setCurrentTree( metaFormat + "/" + metaVersion + "/" + audience + "/" + language + "/" + field );
		if ( inputName.startsWith( "ERROR:" ) ) {
			return errorDisplay( inputName, "getOPML" );
		}
		String title = currentNode.topTree.title;
		int matchCount = Integer.MAX_VALUE;
		if ( responseMap != null ) {
			title += " SUBSET";
			matchCount = responseMap.map.keySet().size();
		}
		String conceptTag = "<" + OPML_NAMESPACE_PREFIX + "concept" + iterateAttributes( (HashMap)currentNode.topTree.header.get( "concept" ), false ) + "/>";
		String xmlDeclare = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		if ( !includeXmlDeclaration ) {
			xmlDeclare = "";
		}
		ret.append( xmlDeclare + "<opml version=\"" + OPML_VERSION + "\" " + OPML_GROUPS_NAMESPACE + ">\n\t<head>\n\t\t<title>"
			 + title + "</title>\n\t\t" + conceptTag + "\n\t</head>\n\t<body>\n"
			 + getOPMLOutline( currentNode.topTree.topMenu, responseMap, "\t\t", matchCount, 0 )
			 + "\t</body>\n</opml>\n" );
		return ret.toString();
	}

	/**
	 *  Recurse through the OPML tree and return the nested &lt;outline&gt; tags
	 *
	 * @param  list         tree node
	 * @param  tabs         tabbed output indenting
	 * @param  responseMap
	 * @param  matchCount
	 * @param  matchAt
	 * @return              OPML for the given node
	 */
	private synchronized String getOPMLOutline( OPMLTree.TreeNode list, MetadataVocabResponseMap responseMap, String tabs, int matchCount, int matchAt ) {
		StringBuffer ret = new StringBuffer();
		for ( int i = 0; ( i < list.treeNodes.size() ) && ( matchAt < matchCount ); i++ ) {
			OPMLTree.TreeNode node = (OPMLTree.TreeNode)list.treeNodes.get( i );
			boolean isHeading = ( node.treeNodes.size() > 0 ) ? true : false;
			if ( node.isHr && ( responseMap == null ) ) {
				ret.append( tabs + "<hr/>\n" );
			}
			else if ( !node.isComment &&
				( ( responseMap == null ) ||
				( responseMap.hasValue( node.getAttribute( "vocab" ) )
				 || responseMap.hasValue( node.getAttribute( "id" ) ) ) ) ) {
				ret.append( tabs + "<outline" );
				ret.append( iterateAttributes( node.getAttributes(), true ) );
				matchAt++;                                                  // return as soon as we've found all values in the reponse map
				if ( isHeading && ( matchAt < matchCount ) ) {
					ret.append( ">\n" + getOPMLOutline( node, responseMap, tabs + "\t", matchCount, matchAt ) + tabs + "</outline>\n" );
				}
				else {
					ret.append( "/>\n" );
				}
			}
			else if ( isHeading && ( matchAt < matchCount ) ) {
				String subOPML = getOPMLOutline( node, responseMap, tabs + "\t", matchCount, matchAt );
				if ( subOPML.length() > 0 ) {
					ret.append( tabs + "<outline" + iterateAttributes( node.getAttributes(), true ) + ">\n"
						 + subOPML + tabs + "</outline>\n" );
				}
			}
		}
		return ret.toString();
	}

	/**
	 *  Iterate through a HashMap and return key/value pairs as XML attrubutes
	 *
	 * @param  map                  map to iterate over
	 * @param  useOutlineNamespace
	 * @return                      map as XML attributes
	 */
	private synchronized String iterateAttributes( HashMap map, boolean useOutlineNamespace ) {
		Iterator iter = map.keySet().iterator();
		String atts = "";
		while ( iter.hasNext() ) {
			String attName = (String)iter.next();
			Object attValue = map.get( attName );
			boolean doInclude = true;
			if ( useOutlineNamespace ) {
				if ( !OPMLTree.isInSetOfBaseOpmlParams( attName ) && !attName.startsWith( OPML_NAMESPACE_PREFIX ) ) {
					doInclude = false;
				}
			}
			if ( doInclude && ( attValue != null ) ) {
				atts += " " + attName + "=\"" + attValue.toString() + "\"";
			}
		}
		return atts;
	}

	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln( String s ) {
		if ( debug ) {
			System.out.println( getDateStamp() + " " + s );
		}
	}

	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getDateStamp() {
		return
			new SimpleDateFormat( "MMM d, yyyy h:mm:ss a zzz" ).format( new Date() );
	}

	// Messages/Error handlers:
	//----------------------------------------------------------------------------------

	/**
	 *  Log a message
	 *
	 * @param  msg
	 */
	public void reportMessage( String msg ) {
		prtln( msg );
	}

	/**
	 *  Log an error
	 *
	 * @param  err
	 */
	public void reportError( String err ) {
		prtln( err );
	}

	/**
	 *  Gets the messages attribute of the MetadataVocabOPML object
	 *
	 * @return    The messages value
	 */
	public synchronized ArrayList getMessages() {
		return messages;
	}

	// _____________________________________ NO LONGER SUPPORTED _________________________________

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
	public String getUiLabelOfSystemIds( String system,
	                                     String systemFieldId,
	                                     String systemValueId,
	                                     boolean abbreviated ) {
		return "NOT SUPPORTED";
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
	public String getUiLabelOfSystemIds( String system,
	                                     String systemFieldId,
	                                     String systemValueId ) {
		return "NOT SUPPORTED";
	}

	/**
	 *  Gets the uiLabelOfFieldId attribute of the MetadataVocabOPML object
	 *
	 * @param  fieldId
	 * @return          The uiLabelOfFieldId value
	 */
	public String getUiLabelOfFieldId( String fieldId ) {
		return "NOT SUPPORTED";
	}

	/**
	 *  Gets the uiLabelOf attribute of the MetadataVocabOPML object
	 *
	 * @param  system
	 * @param  metadataField
	 * @param  metadataValue
	 * @return                The uiLabelOf value
	 */
	public String getUiLabelOf( String system,
	                            String metadataField,
	                            String metadataValue ) {
		return "NOT SUPPORTED";
	}

	/**
	 *  Gets the cacheValuesInOrder attribute of the MetadataVocabOPML object
	 *
	 * @param  system
	 * @param  group
	 * @param  cache
	 * @return         The cacheValuesInOrder value
	 */
	public ArrayList getCacheValuesInOrder( String system, String group, Map cache ) {
		ArrayList ret = new ArrayList();
		ret.add( "NEVER SUPPORTED" );
		return ret;
	}
}

