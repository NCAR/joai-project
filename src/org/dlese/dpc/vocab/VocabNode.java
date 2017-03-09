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
 *  Interface for a controlled vocabulary tree node
 */
public interface VocabNode {

	/**
	 *  Sets the metaFormat attribute of the VocabNode object
	 *
	 * @param  metaFormat  The new metaFormat value
	 */
	public void setMetaFormat( String metaFormat );

	/**
	 *  Sets the groupLevel attribute of the VocabNode object
	 *
	 * @param  groupLevel  The new groupLevel value
	 */
	public void setGroupLevel( int groupLevel );

	/**
	 *  Gets the groupLevel attribute of the VocabNode object
	 *
	 * @return    The groupLevel value
	 */
	public int getGroupLevel();

	/**
	 *  Sets the fieldId attribute of the VocabNode object
	 *
	 * @param  fieldId  The new fieldId value
	 */
	public void setFieldId( String fieldId );

	/**
	 *  Gets the fieldId attribute of the VocabNode object
	 *
	 * @return    The fieldId value
	 */
	public String getFieldId();

	/**
	 *  Gets the name attribute of the VocabNode object
	 *
	 * @return    The name value
	 */
	public String getName();

	/**
	 *  Sets the name attribute of the VocabNode object
	 *
	 * @param  name  The new name value
	 */
	public void setName( String name );

	/**
	 *  Gets the id attribute of the VocabNode object
	 *
	 * @return    The id value
	 */
	public String getId();

	/**
	 *  Sets the id attribute of the VocabNode object
	 *
	 * @param  id  The new id value
	 */
	public void setId( String id );

	/**
	 *  Gets the label attribute of the VocabNode object
	 *
	 * @return    The label value
	 */
	public String getLabel();

	/**
	 *  Sets the label attribute of the VocabNode object
	 *
	 * @param  label  The new label value
	 */
	public void setLabel( String label );

	/**
	 *  Gets the labelAbbrev attribute of the VocabNode object
	 *
	 * @return    The labelAbbrev value
	 */
	public String getLabelAbbrev();

	/**
	 *  Sets the labelAbbrev attribute of the VocabNode object
	 *
	 * @param  labelAbbrev  The new labelAbbrev value
	 */
	public void setLabelAbbrev( String labelAbbrev );

	/**
	 *  Gets the definition attribute of the VocabNode object
	 *
	 * @return    The definition value
	 */
	public String getDefinition();

	/**
	 *  Sets the definition attribute of the VocabNode object
	 *
	 * @param  definition  The new definition value
	 */
	public void setDefinition( String definition );

	/**
	 *  Gets the description attribute of the VocabNode object
	 *
	 * @param  page
	 * @return       The description value
	 */
	public String getDescription( PageContext page );

	/**
	 *  Sets the description attribute of the VocabNode object
	 *
	 * @param  description  The new description value
	 */
	public void setDescription( String description );

	/**
	 *  Gets the list attribute of the VocabNode object
	 *
	 * @return    The list value
	 */
	public VocabList getList();

	/**
	 *  Sets the list attribute of the VocabNode object
	 *
	 * @param  list  The new list value
	 */
	public void setList( VocabList list );

	/**
	 *  Gets the subList attribute of the VocabNode object
	 *
	 * @return    The subList value
	 */
	public ArrayList getSubList();

	/**
	 *  Gets the hasSubList attribute of the VocabNode object
	 *
	 * @return    The hasSubList value
	 */
	public boolean getHasSubList();

	/**
	 *  Gets the noDisplay attribute of the VocabNode object
	 *
	 * @return    The noDisplay value
	 */
	public boolean getNoDisplay();

	/**
	 *  Sets the noDisplay attribute of the VocabNode object
	 *
	 * @param  noDisplay  The new noDisplay value
	 */
	public void setNoDisplay( boolean noDisplay );

	/**
	 *  Gets the noDisplayOriginal attribute of the VocabNode object
	 *
	 * @return    The noDisplayOriginal value
	 */
	public boolean getNoDisplayOriginal();

	/**
	 *  Sets the noDisplayOriginal attribute of the VocabNode object
	 *
	 * @param  noDisplayOriginal  The new noDisplayOriginal value
	 */
	public void setNoDisplayOriginal( boolean noDisplayOriginal );

	/**
	 *  Gets the wrap attribute of the VocabNode object
	 *
	 * @return    The wrap value
	 */
	public boolean getWrap();

	/**
	 *  Sets the wrap attribute of the VocabNode object
	 *
	 * @param  wrap  The new wrap value
	 */
	public void setWrap( boolean wrap );

	/**
	 *  Gets the divider attribute of the VocabNode object
	 *
	 * @return    The divider value
	 */
	public boolean getDivider();

	/**
	 *  Sets the divider attribute of the VocabNode object
	 *
	 * @param  divider  The new divider value
	 */
	public void setDivider( boolean divider );

	/**
	 *  Sets the isLastInSubList attribute of the VocabNode object
	 *
	 * @param  isLastInSubList  The new isLastInSubList value
	 */
	public void setIsLastInSubList( boolean isLastInSubList );

	/**
	 *  Gets the isLastInSubList attribute of the VocabNode object
	 *
	 * @return    The isLastInSubList value
	 */
	public boolean getIsLastInSubList();

	/**
	 *  Sets the src attribute of the VocabNode object
	 *
	 * @param  src  The new src value
	 */
	public void setSrc( String src );
}


