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
package org.dlese.dpc.xml;

import java.io.*;
import java.util.*;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;

/**
 * Creates a wrapper implementation for accessing a JDOM XML document
 * and for mapping the JDOM elements to a local <code>HashMap</code>. JDOM
 * provides the capability to parse, create, edit and write XML documents,
 * however it lacks an efficient method for locating individual elements,
 * as well as needing some help with controlling where new elements get
 * inserted and how comments get processed. It should be noted that
 * only a limited subset of JDOM's capability is actually utilized by 
 * this implementation.
 * <p>
 * Individual JDOM XML elements are wrapped in {@link XMLNode}
 * objects before being stored in the local HashMap. The map key for each
 * <code>XMLNode</code> is a dot delimited string formed from the paths
 * through the JDOM hierarchy, obtained when reading in the XML template.
 * Key paths are constructed and described in the <code>XMLNode</code> details.
 * <p>
 * Currently, <code>XMLRecord</code> is instantiated with an XML
 * file, to provide the mappings from the JDOM tree to the local
 * <code>HashMap</code>. The JDOM document is created, and that
 * document's root element is wrapped in an <code>XMLNode</code>. Then each
 * child element is recursively processed. As each child element is processed,
 * it is likewise wrapped in an <code>XMLNode</code>, its key path is
 * constructed, and then is inserted into the map. Each node maintains the 
 * proper parent-child relations.
 * <p>
 *
 * @version 1.0	02/01/01
 * @author Dave Deniman
 */

public class XMLRecord {

	/**
	 * The root node of this <code>XMLRecord</code>.
	 */
	protected XMLNode root;

	/**
	 * The JDOM document this record maps.
	 */
	protected Document doc;

	/**
	 * Maps reference pointers to elements in the XML hierarchy.
	 */
	protected HashMap map;

	/**
	 * The source XML file used to identify the XML hierarchy.
	 */
	protected File xmlFile;

	/**
	 * A special comment intended as the final comment in the XML file.
	 */
	protected StringBuffer footnote;


	/**
	 * Accepts an XML file and maps its XML structure to a <code>HashMap</code>.
	 * @param file A well-formed XML file.
	 */
	public XMLRecord(File file) throws Exception {
		xmlFile = file;
		root = null;
		map = new HashMap();
		footnote = new StringBuffer();
		
		try {			
			// if we want to validate, use SAXBuilder(true);
			doc = new SAXBuilder().build(new FileInputStream(xmlFile));

			Element rootElement = doc.getRootElement();
			root = new XMLNode(rootElement, true);
			root.setPath(null);
			// map.put(root.path, root);
			process(root);
			processComments();
		}
		catch (Exception e) {
			//System.err.println("throwing exception " + this.getClass());
			//System.err.println(this.getClass() + " threw exception with message: " + e.getMessage());
			//e.printStackTrace();
			throw e;
		}
	}


	public void clear() {
		clearMap(root);
		xmlFile = null;
		root = null;
		doc = null;
		map = null;
	}


	protected void clearMap(XMLNode node) {
		List list = node.getChildren();
		for (int i=0; i<list.size(); i++) {
			clearMap((XMLNode)list.get(i));
		}
		node.clear();
		map.remove(node.getPath());
		node = null;
	}

	/**
	 * Recursively iterates through the children XML elements, wrapping each
	 * in an XMLNode and adding the created node to the local map. Element
	 * parent/child relationships are maintained. 
	 *<p>
	 * If called on the root node, this method effectively builds the entire
	 * map. If called on a subordinate node, then it will build only the local
	 * sub-hierarchy.
	 * @param parentNode The node from which to start processing.
	 */ 		
	protected void process(XMLNode parentNode)  {
	    map.put(parentNode.path, parentNode);

		Element parentElement = parentNode.getElement();
		List children = parentElement.getChildren();
		int size = children.size();
		for (int i=0; i<size; i++) {
			Element childElement = (Element)children.get(i);
			XMLNode childNode = new XMLNode(childElement, true);
			parentNode.addChild(childNode);
			//System.err.println("map path = " + childNode.path);
			process(childNode);
		}
	}

	/**
	 * A convenience method for outputting the current record to a given
	 * filename. Typically, the class is instantiated with an XML file,
	 * and the no argument <code>outputRecord</code> method used to write
	 * the record to that same file. This method, however, reassigns the
	 * protected member <code>xmlFile</code> in order to write to a new 
	 * file. This is done in the case when an XML template file is used,
	 * and the template should be preserved.
	 * @param newfile String representing the filename or absolute filename
	 * of the new file to write.
	 * @return <b>true</b> if succeeded, <b>false</b> otherwise
	 */
	public boolean outputRecord(String newfile) {
		// need some constraints or error checking here???
		xmlFile = new File(newfile);
		return outputRecord();
	}

	/**
	 * Ouputs the current record to <code><b>xmlFile</b></code>, which
	 * is the same file used to create the current record unless 
	 * <code>outputRecord(newfile)</code> has been called. Once that method
	 * is called, <code>xmlFile</code>is changed, and subsequent
	 * calls to this method will output to the new file.
	 * @return <b>true</b> if succeeded, <b>false</b> otherwise
	 */
	public boolean outputRecord() {
		// first we add the footnote back into the document...
		// (see processComments below) 
		//doc.addComment(new Comment("ADDITIONAL INFO: " + footnote));
		doc.addContent(new Comment(" ADDITIONAL INFO: " + footnote + " "));
		try {
			XMLWriter writer = new XMLWriter();
			writer.write(doc, xmlFile);
		}
		catch (Exception e) {
			// XMLOutputter throws this when it encounters problems
			System.err.println(e.getClass() + " threw exception - ");
			System.err.println("message: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Returns the root node of the current record.
	 * @return The <code>XMLNode</code> wrapper for the JDOM document 
	 * root element
	 */
	public XMLNode getRoot() {
		return root;
	}
	
	/**
	 * Returns a node of the current record, or null if the node does not
	 * exist. The node is identified by its path in the JDOM tree. See
	 * <code>XMLNode</code> for details on path construction.
	 * @param name String representing the node path in the JDOM tree.
	 * @return <code>XMLNode</code> wrapper for the element requested
	 */
	public XMLNode getNode(String name) {
		return (XMLNode)map.get(name);
	}

	/**
	 * Adds a JDOM element to the current record. The element is added to
	 * parent XMLNode, so parent is assumed to exist within the current
	 * record. The element may be a complex element with mixed content. The
	 * element is added after like elements if other like elements are already
	 * attached to this parent node, otherwise it is added at the end of the
	 * parent node's list of children elements. After adding the element, the
	 * element is wrapped in an <code>XMLNode</code> and processed (added to
	 * the local map).
	 * <p>
	 * NOTE: Both parent and element are assumed to be non-null in the
	 * current implementation, future implementations will provide
	 * this assertion check.
	 * @param parent XMLNode that this element should be added to.
	 * @param element A new JDOM element to add to this parent node.
	 */
	public void addNode(XMLNode parent, Element element) {
		// should verify parent and element as non-null
		//parent.getElement().addChild(element);
		parent.getElement().addContent(element);
		XMLNode node = new XMLNode(element);
		parent.addChild(node);
		process(node);
	}

	/**
	 * Adds one or more like elements to the current record. The elements
	 * are added to the parent <code>XMLNode</code>, and it is assumed that
	 * there already exists one such element as a child to the parent node.
	 * If no such like element exists, then it should be created and the
	 * <code>addNode</code> method called first. Then invoke this method to
	 * add multiples of the same element as new XMLNodes in the local map. 
	 * Once the elements are added as nodes to the map, their content
	 * may be defined.
	 *<p>
	 * NOTE: New elements are created as copies of existing elements, which
	 * means their text content cannot be assumed empty.
	 * @param name String representing the name of like elements to add.
	 * @param parent XMLNode that the like elements should be added to.
	 * @param num Number of like elements to add.
	 */ 	
	public void addNodes(String name, XMLNode parent, int num) {
		int index = -1;
		// take a look at all the parent's children elements and find
		// the last occurence of the element with the same name.
		Element parentElement = parent.getElement();
		//List list = parentElement.getMixedContent();
		List list = parentElement.getContent();
		for (int i=0; i<list.size(); i++) {
			Object obj = list.get(i);
			//System.err.println("addNodes object " + String.valueOf(i) + " = " + obj.toString());
			if (obj instanceof Element) {
				String str = ((Element)obj).getName();
				if (str.equals(name)) {
					index = i;
				}
			}
		}
		// assuming we found like children...
		//System.err.println("using index: " + String.valueOf(index));
		if (index >= 0) {
			// get that last like child
			Element e = (Element)list.get(index);
			for (int i=0; i<num; i++) {
				// copy it, and add copy into the list
				//Element newElement = e.getCopy(e.getName(), e.getNamespace());
				Element newElement = (Element)e.clone();
				newElement.setName(e.getName());
				newElement.setNamespace(e.getNamespace());
				if (++index >= list.size()) {
					list.add(newElement);
				}
				else {
					list.add(index, newElement);
				}
				// then process the new children as nodes
				// and add to the local map.	
				XMLNode node = new XMLNode(newElement);
				parent.addChild(node);
				//System.err.println("added node " + node.getName() + " to path: " + node.path);
				process(node);
			}
		}
		//for (int i=0; i<list.size(); i++) {
		//	Object obj = list.get(i);
		//	System.err.println("addNodes new list object " + String.valueOf(i) + " = " + obj.toString());
		//}
		
	}

	/**
	 * Removes one or more like elements from the parent <code>XMLNode</code>.
	 * No more elements can be removed than what exists, and no errors are 
	 * generated if an attempt to do so is made. The wrapping nodes are also
	 * removed from the local map, preserving the integrity of the relationship
	 * with the JDOM tree.
	 * @param name String representing the name of the like elements.
	 * @param parent XMLNode that the like elements should be removed from.
	 * @param num Number of like elements to remove.
	 */
	public void removeNodes(String name, XMLNode parent, int num) {
		int counter = num;
		Element parentElement = parent.getElement();
		//List list = parentElement.getMixedContent();
		List list = parentElement.getContent();
		for (int i=(list.size()-1); i>= 0; i--) {
			Object obj = list.get(i);
			if (obj instanceof Element) {
				String str = ((Element)obj).getName();
				if ( str.equals(name) && (counter > 0) ) {
					list.remove(i);
					counter--;
				}
			}
		}
		
		counter = num;
		list = parent.getChildren();
		for (int i=(list.size()-1); i>=0; i--) {
			XMLNode child = (XMLNode)list.get(i);
			if ( child.getName().equals(name) && (counter > 0) ) {
				cleanMap(child);
				list.remove(i);
				counter--;
			}	
		}
	}


	/**
	 * Called by <code>removeNodes</code> to remove a node and its children
	 * from the local map.
	 * @param node XMLNode to be removed.
	 */
	protected void cleanMap(XMLNode node) {
		List list = node.getChildren();
		for (int i=0; i<list.size(); i++) {
			cleanMap((XMLNode)list.get(i));
		}
		map.remove(node.getPath());
	}

	/** 
	 * Getter method for the XML footnote comment. The footnote is read from
	 * and written to the end of the XML file as an XML comment. Look at the 
	 * source for implementation details, because the same strategy could be
	 * applied to include additional comments, such as headers, other footers,
	 * or specifc labeled comments to elements.
	 * @return String representing the footnote comment.
	 */	
	public String getFootnote() {
		return footnote.toString();
	}
	
	/** 
	 * Setter method for the XML footnote comment. The footnote is read from
	 * and written to the end of the XML file as an XML comment. Look at the 
	 * source for implementation details, because the same strategy could be
	 * applied to include additional comments, such as headers, other footers,
	 * or specifc labeled comments to elements.
	 * @param value String representing the footnote comment to add.
	 */	
	public void setFootnote(String value) {
		footnote.delete(0, footnote.length());
		footnote.append(' ').append(value).append(' ');
	}

	/** 
	 * Process the document's (as opposed to an element's) comments for
	 * purposes of extracting the footnote. Also fixes a bug in JDOM which
	 * adds extraneous spaces to comments each time they get processed.
	 */	
	protected void processComments() {
		//List list = doc.getMixedContent();
		List list = doc.getContent();
		for (int i=0; i<list.size(); i++) {
			Object obj = list.get(i);
			if (obj instanceof Comment) {
				String str = ((Comment)obj).getText();
				if (!(str.indexOf("ADDITIONAL INFO:") < 0)) {
					str = str.trim();
					str = str.substring("ADDITIONAL INFO:".length());
					list.remove(i);
					footnote.append(str.trim());
				}
				// we don't need the below trim with jdom7
//				else {
//					// eliminate extraneous spaces - gotta luv JDOM!
//					list.set(i, new Comment(str.trim()));
//				}	
			}	
		}
	}

/*
	public Document getDocument() {
		return doc;
	}
	
	public void setElementValue(String name, String value) {
		XMLNode node = (XMLNode)map.get(name);
		if (node != null)
			node.setValue(value);
	}

	public List getElementList(String name) {
		//
		return null;
	}
	
	public String getName() {
		return xmlFile.getName();
	}
*/	
	
}
