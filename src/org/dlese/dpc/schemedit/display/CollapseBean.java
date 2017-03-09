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
package org.dlese.dpc.schemedit.display;

import org.dlese.dpc.schemedit.*;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.vocab.*;
import org.dlese.dpc.xml.schema.*;
import org.dlese.dpc.xml.*;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import java.util.*;
import java.io.*;
import java.text.*;
import java.util.regex.*;

import javax.servlet.http.HttpServletRequest;

/**
 *  Maintains the state of collapsible nodes in the metadata Editor. States are
 *  OPENED and CLOSED, and the default is controlled by setDefaultState.
 *
 * @author    ostwald
 */
public class CollapseBean implements Serializable {
	private boolean debug = false;

	private String id = "";

	/**  Description of the Field */
	public static String OPEN = "block";
	/**  Description of the Field */
	public static String CLOSED = "none";
	/**  Description of the Field */
	public static String DEFAULT_STATE = CLOSED;
	private String defaultState = DEFAULT_STATE;

	private Map stateMap = null;



	/**  Constructor for the CollapseBean object */
	public CollapseBean() {
		stateMap = new TreeMap();
	}


	/**  Removes all state information from the CollapseBean. */
	public void clear() {
		getStateMap().clear();
	}


	/**
	 *  Set the state of the current element (the value returned by getId) to
	 *  CLOSED.
	 */
	public void closeElement() {
		getStateMap().put(id, CLOSED);
	}


	/**
	 *  Set the state of the specified element to CLOSED.
	 *
	 * @param  key  String designating an element in the instance document
	 */
	public void closeElement(String key) {
		setDisplayState(key, CLOSED);
	}


	/**
	 *  Sets the id attribute which represets the *default element* to be operated
	 *  upon when a "key" is not specified. Used by jsp pages to designate a
	 *  particular element that will be queried, and then subsequent calls to the
	 *  CollapseBean need not pass a parameter to specify the element.<p>
	 *
	 *  For example:
	 *  <ul>
	 *    <li> &lt;jsp:setProperty name="collapseBean" property="id"
	 *    value="${id}"/&gt;
	 *    <li> &lt;c:when test="${sef.collapseBean.isOpen}"&gt;
	 *  </ul>
	 *
	 *
	 * @param  id  The new id value
	 */
	public void setId(String id) {
		this.id = id;
	}


	/**
	 *  Gets the id of the default element.
	 *
	 * @return    The id value
	 */
	public String getId() {
		return id;
	}


	/**
	 *  Sets the defaultState attribute of the CollapseBean object
	 *
	 * @param  state  The new defaultState value
	 */
	public void setDefaultState(String state) {
		if (state.equalsIgnoreCase(OPEN) ||
			state.equalsIgnoreCase(CLOSED)) {
			defaultState = state;
		}
		else {
			defaultState = DEFAULT_STATE;
		}
	}


	/**
	 *  Opens the element at the given xpath, as well as each ancestor element.
	 *
	 * @param  xpath  NOT YET DOCUMENTED
	 */
	public void exposeElement(String xpath) {
		// prtln ("exposeNode() with " + xpath);
		String path = xpath;

		// set ancestor paths to OPEN
		while (path != null && xpath.length() > 0) {

			String encodedXPath = XPathUtils.encodeXPath(path);
			String id = CollapseUtils.pathToId(encodedXPath);

			openElement(id);
			path = XPathUtils.getParentXPath(path);
		}
	}


	/**
	 *  Gets the stateMap attribute of the CollapseBean object
	 *
	 * @return    The stateMap value
	 */
	public Map getStateMap() {
		if (stateMap == null) {
			stateMap = new TreeMap();
		}
		return stateMap;
	}


	/**
	 *  Sets the displayState for a document node corresponding to the given key. A
	 *  key is an xpath encoded to be acceptable as a javascript var (see {@link
	 *  org.dlese.dpc.schemedit.SchemEditUtils#pathToId(String)})
	 *
	 * @param  key    An encoded xpath
	 * @param  state  The new displayState value
	 */
	public void setDisplayState(String key, String state) {
		// prtln ("setting displayState for " + CollapseUtils.ppKey(key) + " to: " + state);
		if (key != null && key.trim().length() != 0) {
			getStateMap().put(key, state);
		}
	}


	/**
	 *  Sets the displayState attribute of the default element of the CollapseBean
	 *  object
	 *
	 * @param  state  The new displayState value
	 */
	public void setDisplayState(String state) {
		getStateMap().put(getId(), state);
	}


	/**
	 *  Gets the isOpen attribute of the default element
	 *
	 * @return    The isOpen value
	 */
	public boolean getIsOpen() {
		return (getDisplayState().equals(OPEN));
	}


	/**
	 *  Gets the displayState of the default element
	 *
	 * @return    The displayState value
	 */
	public String getDisplayState() {
		if (!getStateMap().containsKey(id)) {
			stateMap.put(id, defaultState);
		}
		return getDisplayState(id);
	}


	/**
	 *  Gets the displayState attribute of the specified element
	 *
	 * @param  key  Description of the Parameter
	 * @return      The displayState value
	 */
	public String getDisplayState(String key) {
		return (String) getStateMap().get(key);
	}


	/**  Opens the default element*/
	public void openElement() {
		getStateMap().put(id, OPEN);
	}


	/**
	 *  Opens the specified element
	 *
	 * @param  key  an identifier for the element to open
	 */
	public void openElement(String key) {
		// prtln ("\t opening: " + key);
		setDisplayState(key, OPEN);
	}


	/**  Debugging utility */
	public void displayStateMap() {
		prtln("** stateMap **");
		if (stateMap == null) {
			prtln("\tstateMap not yet initialized");
			return;
		}
		for (Iterator i = stateMap.keySet().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			String value = (String) stateMap.get(key);
			prtln("\t" + key + ": " + value);
			// prtln("\t" + CollapseUtils.ppKey (key) + ": " + value);
		}
		prtln("* ------- *");
	}


	/**
	 *  Find the request parameters that specify displayStates for the content
	 *  boxes of the metadataEditor's instance document, and update the
	 *  collapseBean so it reflects the displayStates specified by the request.
	 *
	 * @param  request  Description of the Parameter
	 */
	public void update(HttpServletRequest request) {

		String pattern = "_displayState";

		for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); ) {
			String param = (String) e.nextElement();
			if (param.endsWith(pattern)) {
				String key = param.substring(0, param.length() - pattern.length());
				prtln("\nKey: " + key);
				String[] values = request.getParameterValues(param);

				if (values.length == 0) {
					prtln("no value found for " + param + " continuing to next param");
					continue;
				}
				else if (values.length != 1) {
					// prtln(values.length + " values found for " + param);
				}

				String value = values[0];
				prtln("\t" + CollapseUtils.idToPath(key) + ": " + value);
				setDisplayState(key, value);
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	protected final void prtln(String s) {
		if (debug) {
			// SchemEditUtils.prtln(s, "CollapseBean");
			SchemEditUtils.prtln(s, "cb");
		}
	}
}

