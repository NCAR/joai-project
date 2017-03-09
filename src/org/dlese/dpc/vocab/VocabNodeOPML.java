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
import org.dlese.dpc.gui.OPMLTree;

/**
 *  MetadataVocabOPML implementation of a controlled vocabulary tree node
 */
public class VocabNodeOPML implements VocabNode {

	OPMLTree.TreeNode node;
	VocabList list;
	int groupLevel = 0;
	String fieldId;
	String valueId;
	String name;
	String label;
	String labelAbbrev;
	String definition;
	String description;
	String src;
	String metaFormat = "adn";
	boolean noDisplay;
	boolean noDisplayOriginal;
	boolean wrap;
	boolean collapsible;
	boolean divider;
	boolean isLastInSubList;

	/**
	 *  Constructor for the VocabNodeOPML object
	 *
	 * @param  node
	 */
	public VocabNodeOPML( OPMLTree.TreeNode node ) {
		String metaFormat = "adn";
		if ( node != null ) {
			// a TreeNode.topTree.conceptKey holds "metaFormat/metaVersion/audience/language/fieldName"
			// so we use that to grab the format:
			metaFormat = node.topTree.conceptKey.substring( 0, node.topTree.conceptKey.indexOf( "/" ) );
		}
		init( node, null, metaFormat );
	}

	/**
	 *  Constructor for the VocabNodeOPML object
	 *
	 * @param  metaFormat
	 * @param  node
	 */
	public VocabNodeOPML( String metaFormat, OPMLTree.TreeNode node ) {
		init( node, null, metaFormat );
	}

	/**
	 *  Constructor for the VocabNodeOPML object
	 *
	 * @param  node
	 * @param  parent
	 */
	public VocabNodeOPML( OPMLTree.TreeNode node, VocabList parent ) {
		init( node, parent, "adn" );
	}

	/**
	 *  Constructor for the VocabNodeOPML object
	 *
	 * @param  metaFormat
	 * @param  node
	 * @param  parent
	 */
	public VocabNodeOPML( String metaFormat, OPMLTree.TreeNode node, VocabList parent ) {
		init( node, parent, metaFormat );
	}

	/**
	 *  Constructor for the VocabNodeOPML object
	 *
	 * @param  metaFormat
	 */
	public VocabNodeOPML( String metaFormat ) {
		this.metaFormat = metaFormat;
		list = new VocabList();
	}

	/**
	 *  Constructor for the init object
	 *
	 * @param  node
	 * @param  parent
	 * @param  metaFormat
	 */
	private void init( OPMLTree.TreeNode node, VocabList parent, String metaFormat ) {
		this.node = node;
		this.metaFormat = metaFormat;
		fieldId = node.fieldId;
		valueId = node.getAttribute( "id" );
		label = node.getAttribute( "text" );
		name = getNodeAttribute( node, "vocab" );
		if ( ( name == null ) && ( label != null ) ) {
			name = label.replaceAll( "\\s", "_" );
		}
		labelAbbrev = getNodeAttribute( node, "textAbbrev" );
		definition = getNodeAttribute( node, "deftn" );
		src = getNodeAttribute( node, "src" );
		if ( ( getNodeAttribute( node, "display" ) != null ) &&
			( getNodeAttribute( node, "display" ).equals( "false" ) ) ) {
			noDisplay = true;
		}
		else {
			noDisplay = false;
		}
		noDisplayOriginal = noDisplay;
		if ( ( getNodeAttribute( node, "wrap" ) != null ) &&
			( getNodeAttribute( node, "wrap" ).equals( "true" ) ) ) {
			wrap = true;
		}
		else {
			wrap = false;
		}
		if ( ( getNodeAttribute( node, "collapsible" ) != null ) &&
			( getNodeAttribute( node, "collapsible" ).equals( "true" ) ) ) {
			collapsible = true;
		}
		else {
			collapsible = false;
		}
		if ( node.isHr ) {
			divider = true;
		}
		else {
			divider = false;
		}
		OPMLTree.TreeNode nodeParent = node.parentNode;
		if ( nodeParent != null ) {
			if ( nodeParent.treeNodes.get( nodeParent.treeNodes.size() - 1 ) == node ) {
				isLastInSubList = true;
			}
			while ( nodeParent != null ) {
				groupLevel++;
				nodeParent = nodeParent.parentNode;
			}
			if ( node.treeNodes.size() == 0 ) {
				groupLevel--;
			}
		}
		else {
			isLastInSubList = false;
		}
		list = new VocabList();
		if ( parent != null ) {
			list.parent = parent;
		}
		for ( int i = 0; i < node.treeNodes.size(); i++ ) {
			list.item.add( new VocabNodeOPML( (OPMLTree.TreeNode)node.treeNodes.get( i ), list ) );
		}
	}

	/**
	 *  Gets the nodeAttribute attribute of the VocabNodeOPML object
	 *
	 * @param  node
	 * @param  att
	 * @return       The nodeAttribute value
	 */
	private String getNodeAttribute( OPMLTree.TreeNode node, String att ) {
		String ret = node.getAttribute( att );
		if ( ret == null ) {
			ret = node.getAttribute( "groups:" + att );
		}
		return ret;
	}

	/**
	 *  Sets the metaFormat attribute of the VocabNodeOPML object
	 *
	 * @param  metaFormat  The new metaFormat value
	 */
	public void setMetaFormat( String metaFormat ) {
		this.metaFormat = metaFormat;
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
	 *  Sets the fieldId attribute of the VocabNode object
	 *
	 * @param  fieldId  The new fieldId value
	 */
	public void setFieldId( String fieldId ) {
		this.fieldId = fieldId;
	}

	/**
	 *  Gets the fieldId attribute of the VocabNode object
	 *
	 * @return    The fieldId value
	 */
	public String getFieldId() {
		return fieldId;
	}

	/**
	 *  Gets the name attribute of the VocabNode object
	 *
	 * @return    The name value
	 */
	public String getName() {
		return name;
	}

	/**
	 *  Sets the name attribute of the VocabNode object
	 *
	 * @param  name  The new name value
	 */
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 *  Gets the id attribute of the VocabNode object
	 *
	 * @return    The id value
	 */
	public String getId() {
		return valueId;
	}

	/**
	 *  Sets the id attribute of the VocabNode object
	 *
	 * @param  id  The new id value
	 */
	public void setId( String id ) {
		valueId = id;
	}

	/**
	 *  Gets the label attribute of the VocabNode object
	 *
	 * @return    The label value
	 */
	public String getLabel() {
		return label;
	}

	/**
	 *  Sets the label attribute of the VocabNode object
	 *
	 * @param  label  The new label value
	 */
	public void setLabel( String label ) {
		this.label = label;
	}

	/**
	 *  Gets the labelAbbrev attribute of the VocabNode object
	 *
	 * @return    The labelAbbrev value
	 */
	public String getLabelAbbrev() {
		return labelAbbrev;
	}

	/**
	 *  Sets the labelAbbrev attribute of the VocabNode object
	 *
	 * @param  labelAbbrev  The new labelAbbrev value
	 */
	public void setLabelAbbrev( String labelAbbrev ) {
		this.labelAbbrev = labelAbbrev;
	}

	/**
	 *  Gets the definition attribute of the VocabNode object
	 *
	 * @return    The definition value
	 */
	public String getDefinition() {
		return definition;
	}

	/**
	 *  Sets the definition attribute of the VocabNode object
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
	 *  Gets the description attribute of the VocabNodeOPML object
	 *
	 * @param  page
	 * @return       The description value
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
				description = null;
				return null;
			}
			return description;
		}
		return description;
	}

	/**
	 *  Sets the description attribute of the VocabNode object
	 *
	 * @param  description  The new description value
	 */
	public void setDescription( String description ) {
		this.description = description;
	}

	/**
	 *  Gets the list attribute of the VocabNode object
	 *
	 * @return    The list value
	 */
	public VocabList getList() {
		return list;
	}

	/**
	 *  Sets the list attribute of the VocabNode object
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
		if ( ( node.treeNodes != null ) && ( node.treeNodes.size() > 0 ) ) {
			return true;
		}
		return false;
	}

	/**
	 *  Gets the noDisplay attribute of the VocabNode object
	 *
	 * @return    The noDisplay value
	 */
	public boolean getNoDisplay() {
		return noDisplay;
	}

	/**
	 *  Sets the noDisplay attribute of the VocabNode object
	 *
	 * @param  noDisplay  The new noDisplay value
	 */
	public void setNoDisplay( boolean noDisplay ) {
		this.noDisplay = noDisplay;
		if ( ( node != null ) && ( noDisplay == true ) ) {
			node.setAttribute( "display", "false" );
		}
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
	 *  Gets the wrap attribute of the VocabNode object
	 *
	 * @return    The wrap value
	 */
	public boolean getWrap() {
		return wrap;
	}

	/**
	 *  Sets the wrap attribute of the VocabNode object
	 *
	 * @param  wrap  The new wrap value
	 */
	public void setWrap( boolean wrap ) {
		this.wrap = wrap;
	}

		/**
	 *  Gets the collapsible attribute of the VocabNode object
	 *
	 * @return    The collapsible value
	 */
	public boolean getCollapsible() {
		return collapsible;
	}

	/**
	 *  Sets the collapsible attribute of the VocabNode object
	 *
	 * @param  collapsible  The new collapsible value
	 */
	public void setCollapsible( boolean collapsible ) {
		this.collapsible = collapsible;
	}
	
	/**
	 *  Gets the divider attribute of the VocabNode object
	 *
	 * @return    The divider value
	 */
	public boolean getDivider() {
		return divider;
	}

	/**
	 *  Sets the divider attribute of the VocabNode object
	 *
	 * @param  divider  The new divider value
	 */
	public void setDivider( boolean divider ) {
		this.divider = divider;
	}

	/**
	 *  Sets the isLastInSubList attribute of the VocabNode object
	 *
	 * @param  isLastInSubList  The new isLastInSubList value
	 */
	public void setIsLastInSubList( boolean isLastInSubList ) {
		this.isLastInSubList = isLastInSubList;
	}

	/**
	 *  Gets the isLastInSubList attribute of the VocabNode object
	 *
	 * @return    The isLastInSubList value
	 */
	public boolean getIsLastInSubList() {
		return isLastInSubList;
	}
}

