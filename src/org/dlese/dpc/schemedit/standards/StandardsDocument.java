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
package org.dlese.dpc.schemedit.standards;

import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.schemedit.*;

import java.io.*;
import java.util.*;

import java.net.*;

/**
 *  Interface for classes representing an educational standard (e.g., an ASN
 *  Standard Document) as a heirarchy of standards nodes in support of display
 *  and selection in the metadata editor.
 *
 * @author    ostwald
 */
public interface StandardsDocument {

	public String getAuthor();
	
	public String getTopic();
	
	/**
	 *  Get a StandardNode by id
	 *
	 * @param  id  the standard id
	 * @return     The standard for this id or null if a standard is not found
	 */
	public StandardsNode getStandard(String id);


	/**
	 *  Gets the rootNode attribute of the StandardsDocument object
	 *
	 * @return    The rootNode value
	 */
	public StandardsNode getRootNode();


	/**
	 *  Returns a flat list containing all DleseStandardsNodes in the
	 *  standardsTree.
	 *
	 * @return    The nodeList value
	 */
	public List getNodeList();


	/**
	 *  The number of nodes in this tree.
	 *
	 * @return    the size of the tree
	 */
	public int size();


	/**  NOT YET DOCUMENTED */
	public void destroy();

}

