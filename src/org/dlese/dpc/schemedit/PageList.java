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
package org.dlese.dpc.schemedit;

import java.util.*;
import java.io.Serializable;

/**
 *  Manages information about the editor pages in support of SchemaEdit.
 *  PageList is created {@link org.dlese.dpc.schemedit.config.FrameworkConfigReader},
 *  which reads from a config file for a particular framework (e.g., "adn"). The
 *  PageList is used to build the menu of pages within the metadata editor, as
 *  well as to aid in navigation within the SchemEdit controllers.<p>
 *
 *  NOTE: this class belongs in the config package.
 *
 * @author    ostwald
 */
public class PageList implements Serializable {

	private static boolean debug = false;
	private ArrayList pages = new ArrayList();
	private String firstPage = null;
	private String homePage = null;


	/**  Constructor for the PageList object  */
	public PageList() { }


	/**
	 *  Constructor for the PageList object
	 *
	 * @param  pageData  Description of the Parameter
	 */
	public PageList(String[][] pageData) {
		prtln("about to add " + pageData.length + " items");
		for (int i = 0; i < pageData.length; i++) {
			addPage(pageData[i]);
			prtln("added page: " + pageData[i][0] + ", " + pageData[i][1]);
		}
		prtln("PageList: ");
		// prtln(toString());
	}


	/**
	 *  Adds a feature to the Page attribute of the PageList object
	 *
	 * @param  page  The feature to be added to the Page attribute
	 */
	public void addPage(Page page) {
		if (!pages.contains(page)) {
			pages.add(page);
		}
	}


	/**
	 *  Adds a feature to the Page attribute of the PageList object
	 *
	 * @param  vals  The feature to be added to the Page attribute
	 */
	public void addPage(String[] vals) {
		Page page = new Page(vals[0], vals[1]);
		if (!pages.contains(page)) {
			pages.add(page);
		}
	}


	/**
	 *  Adds a feature to the Page attribute of the PageList object
	 *
	 * @param  mapping  The feature to be added to the Page attribute
	 * @param  name     The feature to be added to the Page attribute
	 */
	public void addPage(String mapping, String name) {
		Page page = new Page(mapping, name);
		if (!pages.contains(page)) {
			pages.add(page);
		}
	}


	/**
	 *  Sets the firstPage attribute of the PageList object
	 *
	 * @param  page  The new firstPage value
	 */
	public void setFirstPage(String page) {
		firstPage = page;
	}


	/**
	 *  Gets the firstPage attribute of the PageList object
	 *
	 * @return    The firstPage value
	 */
	public String getFirstPage() {
		return firstPage;
	}


	/**
	 *  Sets the homePage attribute of the PageList object
	 *
	 * @param  page  The new homePage value
	 */
	public void setHomePage(String page) {
		homePage = page;
	}


	/**
	 *  Gets the homePage attribute of the PageList object
	 *
	 * @return    The homePage value
	 */
	public String getHomePage() {
		return homePage;
	}


	/**
	 *  Gets the pages attribute of the PageList object
	 *
	 * @return    The pages value
	 */
	public ArrayList getPages() {
		return pages;
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (Iterator i = pages.iterator(); i.hasNext(); ) {
			Page page = (Page) i.next();
			buf.append("\n" + page.toString());
		}
		buf.append("\n\nhomePage: " + getHomePage());
		buf.append("\nfirstPage: " + getFirstPage());
		return buf.toString();
	}


	/**
	 *  Information about the pages/form in an editor. This information supports
	 *  navigation (in the navBar and in the ActionForm
	 *
	 * @author    ostwald
	 *
	 */
	public class Page {

		private String name = "";
		private String mapping = "";


		/**
		 *  Constructor for the Page object
		 *
		 * @param  mapping  the key into the action mapping to display this page
		 * @param  name     pretty name used for display
		 */
		public Page(String mapping, String name) {
			this.name = name;
			this.mapping = mapping;
		}


		/**
		 *  Gets the name attribute of the Page object, defaulting to mapping
		 *  attribute if name is not defined.
		 *
		 * @return    The name value
		 */
		public String getName() {
			if (name == null || name.trim().length() == 0)
				return this.mapping;
			return name;
		}


		/**
		 *  Gets the mapping attribute of the Page object
		 *
		 * @return    The mapping value
		 */
		public String getMapping() {
			return mapping;
		}


		/**
		 *  Description of the Method
		 *
		 * @return    Description of the Return Value
		 */
		public String toString() {
			return "name: " + name + ", mapping: " + mapping;
		}

	}


	/**
	 *  Print a line to standard out.
	 *
	 * @param  s  The String to print.
	 */
	private void prtln(String s) {
		if (debug) {
			System.out.println("PageList: " + s);
		}
	}
}

