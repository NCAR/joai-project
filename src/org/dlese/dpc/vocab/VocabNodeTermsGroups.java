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

import java.util.*;
import java.io.*;
import java.net.*;
import javax.servlet.http.*;
import javax.servlet.jsp.PageContext;
import org.dlese.dpc.webapps.tools.GeneralServletTools;
import org.dlese.dpc.util.GetURL;

/**
 *  VocabNode is a node within the vocab hierarchy of a MetadataVocab instance
 *
 * @author    Ryan Deardorff
 */
public class VocabNodeTermsGroups implements VocabNode, Serializable {
	private String name;                                           // Metadata name
	private String id;                                             // System Id (indexed & used in search queries)
	private String label = "";                                     // User interface label
	private String labelAbbrev = null;                             // Abbreviated interface label
	private String definition = "";                                // Definition
	private String description = null;                             // Brief description (for "tooltip" rollovers)
	private String src = null;                                     // Source URL of brief description
	private VocabList list = new VocabList();                      // Sub-list, if this is a sub-header node
	private boolean noDisplay = false;                             // Don't display this value in this UI
	private boolean noDisplayOriginal = false;                     // Don't display this value in this UI (per groups file attribute)
	private boolean wrap = false;                                  // Start a new column after displaying this item
	private boolean divider = false;                               // Render a divider line after displaying this item
	private String fieldId = null;                                 // Encoded system Id of this term's field.
	private int groupLevel = 0;                                    // Level in the "groups" hierarchy, 0 = top
	private boolean isLastInSubList = false;                       // Is this the last item in a sub-list?

	/**
	 *  Constructor for the VocabNode object
	 *
	 * @param  name        metadata name
	 * @param  noDisplay
	 * @param  groupLevel
	 */
	public VocabNodeTermsGroups( String name, boolean noDisplay, int groupLevel ) {
		this.name = name;
		this.noDisplay = this.noDisplayOriginal = noDisplay;
		this.groupLevel = groupLevel;
	}

	/**
	 *  Sets the metaFormat attribute of the VocabNodeTermsGroups object
	 *
	 * @param  metaFormat  The new metaFormat value
	 */
	public void setMetaFormat( String metaFormat ) {
		System.err.println( "setMetaFormat() NEVER SUPPORTED in VocabNodeTermsGroups" );
	}

	/**
	 *  Sets the groupLevel attribute of the VocabNode object
	 *
	 * @param  groupLevel  The new groupLevel value
	 */
	public void setGroupLevel( int groupLevel ) {
		this.groupLevel = groupLevel;
	}

	/**
	 *  Gets the groupLevel attribute of the VocabNode object
	 *
	 * @return    The groupLevel value
	 */
	public int getGroupLevel() {
		return groupLevel;
	}

	/**
	 *  Each vocab term has a field and a value. This sets the encoded system Id of
	 *  this term's field.
	 *
	 * @param  fieldId  The new fieldId value
	 */
	public void setFieldId( String fieldId ) {
		this.fieldId = fieldId;
	}

	/**
	 *  Each vocab term has a field and a value. This gets the encoded system Id of
	 *  this term's field.
	 *
	 * @return    The fieldId value
	 */
	public String getFieldId() {
		return fieldId;
	}

	/**
	 *  Gets the Metadata name
	 *
	 * @return    The name value
	 */
	public String getName() {
		return name;
	}

	/**
	 *  Sets the Metadata name
	 *
	 * @param  name  The new name value
	 */
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 *  Gets the encoded system Id (indexed & used in search queries)
	 *
	 * @return    The id value
	 */
	public String getId() {
		return id;
	}

	/**
	 *  Sets the encoded system Id (indexed & used in search queries)
	 *
	 * @param  id  The new id value
	 */
	public void setId( String id ) {
		this.id = id;
	}

	/**
	 *  This is needed because Struts &lt;logic:equal&gt; compares strings that
	 *  look like numbers without regard to leading 0s, and our ID convention
	 *  causes matches between '05' and '005' to return true (they are "equal").
	 *  Solution: turn comparison into non-digit strings ('05Compare' vs.
	 *  '005Compare') and then Struts will use alphabetical instead of numerical
	 *  comparison.
	 *
	 * @return    The idCompare value
	 */
	public String getIdCompare() {
		return id + "Compare";
	}

	/**
	 *  Gets the user interface label
	 *
	 * @return    The label value
	 */
	public String getLabel() {
		return label;
	}

	/**
	 *  Sets the user interface label
	 *
	 * @param  label  The new label value
	 */
	public void setLabel( String label ) {
		this.label = label;
	}

	/**
	 *  Gets the abbreviated user interface label
	 *
	 * @return    The labelAbbrev value
	 */
	public String getLabelAbbrev() {
		return labelAbbrev;
	}

	/**
	 *  Sets the abbreviated user interface label
	 *
	 * @param  labelAbbrev  The new labelAbbrev value
	 */
	public void setLabelAbbrev( String labelAbbrev ) {
		this.labelAbbrev = labelAbbrev;
	}

	/**
	 *  Gets the vocab node definition
	 *
	 * @return    The definition value
	 */
	public String getDefinition() {
		return definition;
	}

	/**
	 *  Sets the vocab node definition
	 *
	 * @param  definition  The new definition value
	 */
	public void setDefinition( String definition ) {
		this.definition = definition;
	}

	/**
	 *  Sets the src attribute of the VocabNode object
	 *
	 * @param  src  The new src value
	 */
	public void setSrc( String src ) {
		this.src = src;
	}

	/**
	 *  Gets a truncated (first two sentences or first 300 characters, whichever
	 *  comes first) version of a vocab descrition
	 *
	 * @param  page
	 * @return       The src value
	 */
	public synchronized String getDescription( PageContext page ) {
		if ( ( ( description == null ) || ( description.equals( "" ) ) ) && src != null ) {
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
				( description.indexOf( "<TITLE>" ) > -1 ) ) {               // 404 not found case
				return null;
			}
			if ( !description.matches( "\\S" ) ) {
				// return null so that the next request will try again
				return null;
			}
			return description;
		}
		return description;
	}

	/**
	 *  Sets the vocab node src (file for information panel)
	 *
	 * @param  description  The new description value
	 */
	public void setDescription( String description ) {
		this.description = description;
	}

	/**
	 *  Gets the vocab node sub-list
	 *
	 * @return    The list value
	 */
	public VocabList getList() {
		return list;
	}

	/**
	 *  Sets the vocab node sub-list
	 *
	 * @param  list  The new list value
	 */
	public void setList( VocabList list ) {
		this.list = list;
	}

	/**
	 *  Struts equivalent of getList()--JPSs work by iterating over ArrayLists
	 *  (Collections)
	 *
	 * @return    The list value
	 */
	public ArrayList getSubList() {
		return list.item;
	}

	/**
	 *  Gets the hasSubList attribute of the VocabNode object
	 *
	 * @return    The hasSubList value
	 */
	public boolean getHasSubList() {
		if ( list.item.size() > 0 ) {
			return true;
		}
		return false;
	}

	/**
	 *  Gets the noDisplay attribute of the vocab node (for UI suppression)
	 *
	 * @return    The noDisplay value
	 */
	public boolean getNoDisplay() {
		return noDisplay;
	}

	/**
	 *  Sets the noDisplay attribute of the vocab node (for UI suppression)
	 *
	 * @param  noDisplay  The new noDisplay value
	 */
	public void setNoDisplay( boolean noDisplay ) {
		this.noDisplay = noDisplay;
	}

	/**
	 *  Gets the noDisplayOriginal attribute of the VocabNode object
	 *
	 * @return    The noDisplayOriginal value
	 */
	public boolean getNoDisplayOriginal() {
		return noDisplayOriginal;
	}

	/**
	 *  Sets the noDisplay attribute of the vocab node (for UI suppression)
	 *
	 * @param  noDisplayOriginal  The new noDisplayOriginal value
	 */
	public void setNoDisplayOriginal( boolean noDisplayOriginal ) {
		this.noDisplayOriginal = noDisplayOriginal;
	}

	/**
	 *  Gets the wrap attribute of the vocab node (for UI table column wrapping)
	 *
	 * @return    The wrap value
	 */
	public boolean getWrap() {
		return wrap;
	}

	/**
	 *  Sets the wrap attribute of the vocab node (for UI table column wrapping)
	 *
	 * @param  wrap  The new wrap value
	 */
	public void setWrap( boolean wrap ) {
		this.wrap = wrap;
	}

	/**
	 *  Gets the divider attribute of the vocab node (for UI list dividing)
	 *
	 * @return    The divider value
	 */
	public boolean getDivider() {
		return divider;
	}

	/**
	 *  Sets the divider attribute of the vocab node (for UI list dividing)
	 *
	 * @param  divider  The new divider value
	 */
	public void setDivider( boolean divider ) {
		this.divider = divider;
	}

	/**
	 *  Sets the isLastInSublist attribute of the VocabNode object
	 *
	 * @param  isLastInSubList  The new isLastInSubList value
	 */
	public void setIsLastInSubList( boolean isLastInSubList ) {
		this.isLastInSubList = isLastInSubList;
	}

	/**
	 *  Gets the isLastInSublist attribute of the VocabNode object
	 *
	 * @return    The isLastInSublist value
	 */
	public boolean getIsLastInSubList() {
		return isLastInSubList;
	}
}


