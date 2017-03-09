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

import org.jdom.*;
import java.util.*;

/**
 * Creates an object wrapper for JDOM XML elements and for accessing
 * their content. The primary purpose of this class is to provide 
 * object mapping between the JDOM tree hierarchy and the HashMap maintained
 * by {@link XMLRecord}. Child and parent relationships in the XML 
 * element hierarchy are mirrored by the <code>XMLNode</code> structure.
 * <p>
 * The path strings created and maintained by this class reflect the path 
 * that would be traversed through the JDOM tree hierarchy to reach the 
 * wrapped element. Path strings are extended as nodes are added, by 
 * appending the element's name and its number of occurrence. The path 
 * strings then serve as keys to the map maintained by <code>XMLRecord</code>. 
 *<p>
 * Here is an example path:
 * <pre>
 * 	record:0.technical:0.requirements:1.type:0.langstring:0
 * </pre>
 * <p>
 * This path identifies the type langstring element belonging to the 2nd
 * technical requirement of the root element record. Here is the
 * corresponding XML:
 * <pre>
 *	&lt;record&gt;
 *	...
 *	  &lt;technical&gt;
 *	  ...
 *		&lt;requirements&gt;
 * 		  &lt;type&gt;
 *			&lt;langstring /&gt;
 *		  &lt;/type&gt;
 *		  ...
 *		&lt;/requirements&gt;
 *		&lt;requirements&gt;
 * 		  &lt;type&gt;
 *			&lt;langstring&gt;THIS IS THE ONE&lt;/langstring&gt;
 *		  &lt;/type&gt;
 *		  ...
 *		&lt;/requirements&gt;
 *	  ...
 *	  &lt;technical&gt;
 *	...
 *	&lt;record&gt;
 * </pre>
 * <p>
 * By knowing the path structure, a calling method may ask <code>XMLRecord</code>
 * for any node anywhere in the hierarchy. The node then provides the caller
 * with convenient access methods to the element and its attributes.
 * @version 1.0	02/01/01
 * @author Dave Deniman
 */

public class XMLNode {

	/**
	 * Identifies the location in the JDOM hierarchy of the wrapped XML element.
	 */
	protected String path;

	/**
	 * List of child nodes.
	 */
	protected ArrayList children;

	/**
	 * Identifies the nth occurence of this node in the parent's children
	 * array. This is not the same as its index.
	 */
	protected int occurs;

	/**
	 * The JDOM XML element this node wraps.
	 */
	protected Element element;

	/**
	 * The parent node of this one.
	 */
	protected XMLNode parent;
	
    /**
     * Constructor wraps a JDOM Element with a new node.
	 * @param element JDOM element to be wrapped.
     */
    public XMLNode(Element element)  {
		this.element = element;
    	occurs = 0;
    	path = null;	
		parent = null;
		children = new ArrayList();
	}

	/**
	 * Constructor wraps a JDOM Element with a new node.
	 * @param element JDOM element to be wrapped.
	 */
	public XMLNode(Element element, boolean setNormalize)  {
		this.element = element;
		if (setNormalize) {
			List list = element.getContent();
			for (int i=list.size()-1; i >=0; i--) {
				Object obj = list.get(i);
				if (obj instanceof String) {
					if (isWhitespace((String)obj)) {
						list.remove(i);
					}
				}
			}
		}
		occurs = 0;
		path = null;	
		parent = null;
		children = new ArrayList();
	}

	/**
	 * Retrieve the node's parent node, which is null if 
	 * this node wraps the root element.
	 * @return The parent node to this node, or null.
	 */
	public XMLNode getParent() {
		return parent;
	}
	
	
	public void clear() {
		element.removeChildren();
		element = null;
		children.clear();
		children = null;
	}
	
	/**
	 * Adds a child node to this one at the correct location, and sets
	 * the path.
	 * @param node The node to add.
	 */
	public void addChild(XMLNode node)  {
		node.parent = this;
		// inserts to the children array at the correct location
		int place = findPlace(node);
		//System.err.println("adding to place: " + String.valueOf(place)); 
		children.add(place, node);
		// sets the new nodes path, using this node's path as the parent path
		node.setPath(this.path);
		//System.err.println("Adding node " + node.getName() + " to path " + path + " at index " + place); 
	}

	/**
	 * Gets the child node that lives at a specific index of this node's
	 * children array.
	 * <p>
	 * NOTE: no bounds checking is done in this implementation.
	 * @param index An integer identifying the index of the node to return.
	 * @return The child XMLNode at the indexed location.
	 */ 
	public XMLNode getChild(int index) {
		// This should make an assertion check to verify the index is in
		// bounds. ArrayList will throw an IndexOutOfBoundsException, which
		// is not being caught!!!!
		if (children.size() > index) {
			return (XMLNode)children.get(index);
		}
		return null;
	}

	/**
	 * Gets the list of children belonging to this node.
	 * @return A <code>List</code> containing all the children of this node. 
	 */
	public List getChildren() { return children; }


	public List getLikeChildren(String name) {
		ArrayList list = new ArrayList();
		for (int i=0; i<children.size(); i++) {
			XMLNode child = (XMLNode)children.get(i);
			String nodeName = child.getName();
			if (nodeName.equals(name)) {
				list.add(child);
			}
		}
		return list;
	}

	/**
	 * Searches the children nodes for an element name and returns
	 * the number found. This is useful when retrieving multiple occurring
	 * elements.
	 * @param nodeName The name to search for.
	 * @return The number of matching element names
	 */
	public int likeChildren(String nodeName) {
		int count = 0;
		for (int i=0; i<children.size(); i++) {
			String name = ((XMLNode)children.get(i)).getName();
			if (name.equals(nodeName))
				count++;
		}
		return count;	
	}

	/**
	 * Finds the place where a new node should be inserted. JDOM does not
	 * provide for inserting elements at a given location, so this helps 
	 * keep track of a node's location, for constructing the key paths and
	 * for inserting like children one after the other.
	 * @param node The XMLNode to find the placement for.
	 * @return An integer representing the index location where to insert.
	 */
	protected int findPlace(XMLNode node) {
		int count = 0;
		int place = children.size();
		for (int i=0; i<children.size(); i++) {
			String name = ((XMLNode)children.get(i)).getName();
			if (name.equals(node.getName())) {
				count++;
				place = i+1;
			}	
		}
		node.occurs = count;
		return place;
	}

	/**
	 * Returns the path which maps this node's element location in the
	 * JDOM tree hierarchy.
	 * @return The path as a string.
	 */
	public String getPath() { return path; }

	/**
	 * Appends this node's name and number of occurrence to a path.
	 * @param parentPath The path of the node which is or will be this node's
	 * parent.
	 */
	public void setPath(String parentPath) {
		StringBuffer str = new StringBuffer();
		if ((parentPath != null) && (parentPath.length() > 0)) {
			str.append(parentPath);
			str.append(".");
		}	
		str.append(getName());
		str.append(":");
		str.append(String.valueOf(occurs));
		
		path = str.toString();
	}

	/**
	 * Adds a comment to the element of this node. If other comments already
	 * exist then this one is added after the other comments.
	 * @param value String comment to be added. 
	 */
	public void addComment(String value) {
		//element.addChild(new Comment(value));
		element.addContent(new Comment(value));
	}

	/**
	 * Gets an identified comment from the element of this node, if it exists.
	 * If a comment has been added with an identifier, this method will
	 * browse the available comments for a match. The identifier can exist
	 * anywhere in the comment.
	 * @param identifier String to match as an identifier.
	 * @return The comment or an empty string.
	 */	
	public String getComment(String identifier) {
		//List list = element.getMixedContent();
		List list = element.getContent();
		Iterator i = list.iterator();
		//System.err.println("searching "+ getName() +"'s comments for " + identifier);
		while (i.hasNext()) {
			Object obj = i.next();
			//System.err.println("obj class = " + obj.getClass() + " : " + obj.toString());
			if (obj instanceof Comment) {
				String str = ((Comment)obj).getText();
				// System.out.println("comment = " + str);
				if (str.indexOf(identifier) >= 0) 
					return str;
			}	
		}
		return "";
	}

	/**
	 * Gets all the comments belonging to the element of this node.
	 * @return List of comments in the order they are read from the element.
	 */	
	public List getComments() {
		ArrayList result = new ArrayList();
		//List list = element.getMixedContent();
		List list = element.getContent();
		Iterator i = list.iterator();
		while (i.hasNext()) {
			Object obj = i.next();
			if (obj instanceof Comment) {
				result.add(((Comment)obj).getText().trim());
			}
		}
		return result;
	}

	/**
	 * Removes the first identified comment from the element of this node,
	 * if one exists. A comment may be added with an identifier, and this
	 * method will browse the available comments for a matching identifier.
	 * The identifier can exist anywhere in the comment. Only the first 
	 * occurrence is removed.
	 * @param identifier String to match as an identifier.
	 * @return <b>true</b> if removed, <b>false</b> otherwise
	 */ 	
	public boolean removeComment(String identifier) {
		ArrayList result = new ArrayList();
		//List list = element.getMixedContent();
		List list = element.getContent();
		for (int i=(list.size()-1); i>=0; i--) {
			Object obj = list.get(i);
			if (obj instanceof Comment) {
				String str = ((Comment)obj).getText();
				if (str.indexOf(identifier) >= 0) {
					list.remove(i);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Tests whether this node wraps the JDOM document root element.
	 * @return <b>true</b> if is root, <b>false</b> otherwise
	 */	
	public boolean isRoot() { return element.isRootElement(); }


	/**
	 * Tests whether this node wraps a leaf element, which is useful for
	 * knowing how to add content. (IMS and DLESE DTDs do not allow text
	 * to be added to non-leaf elements.
	 * @return <b>true</b> if is a leaf node, <b>false</b> otherwise
	 */	
	public boolean isLeaf()  {
		if (children.size() != 0)
			return false;
		return true;	
	}

	/**
	 * Retrieves the name of the element of this node.
	 * @return The name of the wrapped element as a string.
	 */
	public String toString()  {	return element.getName(); }

	/**
	 * Retrieves the name of the element of this node.
	 * @return The name of the wrapped element, as a string.
	 */
	public String getName() { return element.getName(); }

	/**
	 * Retrieves the element of this node.
	 * @return The JDOM element of this node.
	 */
	public Element getElement() { return element; }

	/**
	 * Retrieves the textual content of the element of this node.
	 * <p>
	 * NOTE: This implementation is built on JDOM 4, which provides
	 * weak access for reading and writing element content. JDOM 5
	 * has deprecated this approach, and JDOM 6 is even more flexible
	 * and robust. Future implementations of this class should reflect
	 * the updates in JDOM.
	 * @return The text content of this node's element, or an empty string
	 * if there is no text content.
	 */	
	public String getValue() { return element.getText(); }  //element.getContent();

	/**
	 * Sets the textual content of the element of this node, if it is a
	 * leaf element. (The requirement to be a leaf element is an IMS and
	 * DLESE DTD requirement, which a more general implementation should
	 * not restrict.)
	 * <p>
	 * NOTE: This implementation is built on JDOM 4, which provides
	 * weak access for reading and writing element content. JDOM 5
	 * has deprecated this approach, and JDOM 6 is even more flexible
	 * and robust. Future implementations of this class should reflect
	 * the updates in JDOM.
	 * @param value The text to set as content for the element of this node.
	 */	
	public void setValue(String value) {
		if (isLeaf())
			//element.setContent(value);
			//System.err.println("setting node " + path + " + with value: " + value);
			element.setText(value);
	}

	/**
	 * Retrieves a named attribute from the element of this node, if the
	 * attribute exists.
	 * @param attName The name of the attribute to retrieve.
	 * @return The value of the attribute as a string.
	 */
	public String getAttribute(String attName)  {
		try {
			//Namespace namespace = element.getNamespace();
			// if this doesn't work, we may have to use the namespace argument
			Attribute attribute = element.getAttribute(attName);
			if (attribute != null) {
				return attribute.getValue();
			}
			else {
				//System.out.println("attribute = null");
			}
		} catch (Exception e) {
			//System.err.println("Error: retrieving " + attName + " info for " + element.getName());
		}
		return null;
	}

	/**
	 * Sets the value of an attribute for the element of this node.
	 * @param attName The name of the attribute to set.
	 * @param value The value to assign to the attribute.
	 */
	public void setAttribute(String attName, String value)  {
		try  {
			Namespace namespace = element.getNamespace();
			Attribute attribute = element.getAttribute(attName, namespace);
			if (attribute != null)
				attribute.setValue(value);
		} catch (Exception e) {
			//System.err.println("Error: assigning " + attName + " to " + elementPath);
		}
	}

	private boolean isWhitespace(String s) {
	    char[] c = s.toCharArray();
	    for (int i=0; i<c.length; ++i) {
			//int x = (int)c[i];
			//System.out.println("c = " + String.valueOf(x));
	        //if (" \t\n\r".indexOf(c[i]) == -1) {
			if (c[i] > ' ') {
	            return false;
	        }
	    }
	    return true;
	}
	
/*
	public void removeChild(XMLNode node) {
		int index = children.lastIndexOf(node);
		children.remove(index);
	}

	public Element getElementCopy() {
		Element e = new Element(element.getName(), element.getNamespace());
		e.setAttributes(element.getAttributes());
		e.setMixedContent(element.getMixedContent());
		//System.err.println("parent = " + e.getParent());
		return e;
	}
*/

}
