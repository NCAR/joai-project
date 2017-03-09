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
package org.dlese.dpc.services.dds.action.form;

import org.dlese.dpc.propertiesmgr.*;
import org.dlese.dpc.webapps.tools.*;
import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.oai.*;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.dds.action.form.VocabForm;
import org.dlese.dpc.vocab.*;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.URLEncoder;

/**
 *  A ActionForm bean that holds data for the JavaScript HTML search service.
 *
 * @author     John Weatherley
 * @version    $Id: JSHTMLForm_1_1.java,v 1.9 2010/07/14 00:19:27 jweather Exp $
 * @see        org.dlese.dpc.services.dds.action.JSHTMLAction_1_1
 */
public class JSHTMLForm_1_1 extends ActionForm implements Serializable {

	private static boolean debug = true;
	private String errorMsg = null;
	private ResultDocList results = null;
	private String authorizedFor = null;
	private String recordXml = null;
	private String recordFormat = null;
	private int s = 0, n = 10;
	private List xmlFormats = null;
	private String vocabField = null;
	private String requestElementLabel = null;

	private String vocabFieldId = "";
	private String vocabValueId = "";
	private String vocabInterface = "dds.descr.en-us";
	private MetadataVocab vocab = null;
	private HashMap myMenus = null;
	private ArrayList menuNamesInOrder = null;	
	private HashMap myMenuItemsMap = null;
	private HashMap myMenuLabelsMap = null;
	private HashMap myMenuPositionsMap = null;
	private ArrayList smartLinkParameterNames = null;


	/**
	 *  Gets the vocab attribute of the JSHTMLForm_1_1 object
	 *
	 * @return    The vocab value
	 */
	public final MetadataVocab getVocab() {
		if (vocab == null)
			vocab = (MetadataVocab) servlet.getServletContext().getAttribute("MetadataVocab");
		return vocab;
	}


	/**
	 *  Sets the vocabFieldId attribute of the JSHTMLForm_1_1 object
	 *
	 * @param  id  The new vocabFieldId value
	 */
	public void setVocabFieldId(String id) {
		vocabFieldId = id;
	}


	/**
	 *  Sets the vocabValueId attribute of the JSHTMLForm_1_1 object
	 *
	 * @param  id  The new vocabValueId value
	 */
	public void setVocabValueId(String id) {
		vocabValueId = id;
	}


	/**
	 *  Gets the vocabLabel attribute of the JSHTMLForm_1_1 object
	 *
	 * @return    The vocabLabel value
	 */
	public String getVocabLabel() {
		try {
			return getVocab().getUiLabelOfSystemIds(vocabInterface, vocabFieldId, vocabValueId);
		} catch (Throwable t) {
			prtlnErr("getVocabLabel(): " + t);
			return "Unavailable";
		}
	}

	// Bean properties:

	/**  Constructor for the RepositoryForm object */
	public JSHTMLForm_1_1() { }


	/**
	 *  Reset all bean properties to their default state. This method is called before the properties are
	 *  repopulated by the controller servlet.
	 *
	 * @param  mapping  The ActionMapping
	 * @param  request  The request
	 */
	public void reset(ActionMapping mapping,
	                  HttpServletRequest request) {
		myMenus = null;
		myMenuItemsMap = null;
		smartLinkParameterNames = null;
		myMenuLabelsMap = null;
		myMenuPositionsMap = null;
	}


	/**
	 *  Adds menu and item to the page. If the given menu does not exist it will be created and the item will be
	 *  added, otherwise the item will be added to the given menu.
	 *
	 * @param  menu  The name of the menu
	 * @param  item  The name of the menu item
	 */
	public Object addMenuItem(String menu, String item) {
		if (myMenus == null)
			myMenus = new HashMap();
		
		if(myMenuPositionsMap == null)
			myMenuPositionsMap = new HashMap();
		
		if(menuNamesInOrder == null)
			menuNamesInOrder = new ArrayList();
		
		if(!myMenus.containsKey(menu))
			menuNamesInOrder.add(menu);
		
		Object position = myMenuPositionsMap.get(menu);
		if(position == null){
			position = new Integer(myMenuPositionsMap.size());
			myMenuPositionsMap.put(menu, position);
		}
		
		//Integer position = getMenuPosition(menu);
		String slm = "slm" + position;
		
		//prtln("addMenuItem() - menu: " + menu + " item:" + item + " key:" + slm);
		
		ArrayList items = (ArrayList) myMenus.get(menu);
		if (items == null)
			items = new ArrayList();
		items.add(item);
		myMenus.put(menu, items);

		if (myMenuLabelsMap == null)
			myMenuLabelsMap = new HashMap();
		myMenuLabelsMap.put(slm, menu);
		return position;
	}


	/**
	 *  Gets a Map of the smart link menu labels, for example 'My custom menu', keyed by the menu number, for
	 *  example 'slm0'.
	 *
	 * @return    A Map of menu labels
	 */
	public HashMap getMenuLabelsMap() {
		return myMenuLabelsMap;
	}


	/**
	 *  Gets the number of items in the smart link menu by the given name.
	 *
	 * @param  menu  The menu name, for example 'My custom menu'
	 * @return       The number of items in this menu
	 */
	public int getNumMenuItems(String menu) {
		if (myMenus == null || menu == null)
			return 0;
		List items = (List) myMenus.get(menu);
		if (items == null)
			return 0;
		else
			return items.size();
	}


	/**
	 *  Gets a Map of the menu positions keyed by menu name, for example'My custom menu'.
	 *
	 * @return       A map of positions, corresponding to slm0, slm1 etc, or null if no menu has not been added yet
	 */
	public HashMap getMenuPositions() {
		return myMenuPositionsMap;
	}


	/**
	 *  Gets a Map of all smart link menus keyed by menu name, for example 'My custom menu', with values
	 *  containing Lists of the items in the given menu. This map should be used to create the menu in the UI.
	 *
	 * @return    A Map of smart link menus
	 */
	public Map getMenus() {
		return myMenus;
	}

	public ArrayList getMenuNamesInOrder() {
		return menuNamesInOrder;
	}	

	/**
	 *  Gets a Map of all items in the smart link menus, keyed by their menu and item number of the form slm0-0,
	 *  where slm0 is the ID for the menu and 0 is the ID for the item in that menu.
	 *
	 * @return    The Map of all menu items
	 */
	public Map getMenuItemsMap() {
		return myMenuItemsMap;
	}


	/**
	 *  Sets the Map of all items in the smart link menus, keyed by their menu and item number of the form
	 *  slm0-0, where slm0 is the ID for the menu and 0 is the ID for the item in that menu.
	 *
	 * @param  map  The new menu items map
	 */
	public void setMenuItemsMap(HashMap map) {
		myMenuItemsMap = map;
	}


	/**
	 *  Gets the names of the parameters used to refer to the all current smart links, for example slm0, slm1,
	 *  etc.
	 *
	 * @return    The names of the smart link parameters
	 */
	public ArrayList getSmartLinkParameterNames() {
		if (myMenus == null || myMenus.size() == 0)
			return null;

		if (smartLinkParameterNames == null) {
			smartLinkParameterNames = new ArrayList(myMenus.size());
			for (int i = 0; i < myMenus.size(); i++)
				smartLinkParameterNames.add("slm" + i);
		}
		return smartLinkParameterNames;
	}


	/**
	 *  Gets the xmlFormats attribute of the JSHTMLForm_1_1 object
	 *
	 * @return    The xmlFormats value
	 */
	public List getXmlFormats() {
		return xmlFormats;
	}


	/**
	 *  Sets the xmlFormats attribute of the JSHTMLForm_1_1 object
	 *
	 * @param  var  The new xmlFormats value
	 */
	public void setXmlFormats(List var) {
		xmlFormats = var;
		if (xmlFormats != null)
			Collections.sort(xmlFormats);
	}


	/**
	 *  Gets the localizedRecordXml attribute of the JSHTMLForm_1_1 object
	 *
	 * @return    The localizedRecordXml value
	 */
	public String getLocalizedRecordXml() {
		String xml = recordXml.replaceAll("xmlns.*=\".*\"|xsi:schemaLocation.*=\".*\"", "");
		if (recordFormat == null)
			return xml;
		else if (recordFormat.equals("oai_dc"))
			return xml.replaceAll("oai_dc:dc", "oai_dc").replaceAll("<dc:", "<").replaceAll("</dc:", "</");
		else if (recordFormat.equals("nsdl_dc"))
			return xml.replaceAll("nsdl_dc:nsdl_dc", "ndsl_dc").replaceAll("<dc:", "<").replaceAll("<dct:", "<").replaceAll("</dc:", "</").replaceAll("</dct:", "</");
		return xml;
	}


	/**
	 *  Gets the recordXml attribute of the JSHTMLForm_1_1 object
	 *
	 * @return    The recordXml value
	 */
	public String getRecordXml() {
		return recordXml;
	}


	/**
	 *  Sets the recordXml attribute of the JSHTMLForm_1_1 object
	 *
	 * @param  val  The new recordXml value
	 */
	public void setRecordXml(String val) {
		recordXml = val;
	}


	/**
	 *  Sets the recordFormat attribute of the JSHTMLForm_1_1 object
	 *
	 * @param  val  The new recordFormat value
	 */
	public void setRecordFormat(String val) {
		recordFormat = val;
	}


	/**
	 *  Gets the recordFormat attribute of the JSHTMLForm_1_1 object
	 *
	 * @return    The recordFormat value
	 */
	public String getRecordFormat() {
		return recordFormat;
	}


	/**
	 *  Gets the s attribute of the JSHTMLForm_1_1 object
	 *
	 * @return    The s value
	 */
	public int getS() {
		return s;
	}


	/**
	 *  Sets the s attribute of the JSHTMLForm_1_1 object
	 *
	 * @param  val  The new s value
	 */
	public void setS(int val) {
		s = val;
	}


	/**
	 *  Gets the n attribute of the JSHTMLForm_1_1 object
	 *
	 * @return    The n value
	 */
	public int getN() {
		return n;
	}


	/**
	 *  Sets the n attribute of the JSHTMLForm_1_1 object
	 *
	 * @param  val  The new n value
	 */
	public void setN(int val) {
		n = val;
	}


	/**
	 *  Gets the role name for which this user is authorized for
	 *
	 * @return    The authorizedFor value
	 */
	public String getAuthorizedFor() {
		return authorizedFor;
	}



	/**
	 *  Sets the role name for which this user is authorized for
	 *
	 * @param  val  The new authorizedFor value
	 */
	public void setAuthorizedFor(String val) {
		authorizedFor = val;
	}


	/**
	 *  Sets the errorMsg attribute of the JSHTMLForm_1_1 object
	 *
	 * @param  errorMsg  The new errorMsg value
	 */
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}


	/**
	 *  Gets the errorMsg attribute of the JSHTMLForm_1_1 object
	 *
	 * @return    The errorMsg value
	 */
	public String getErrorMsg() {
		return errorMsg;
	}


	/**
	 *  Gets the results attribute of the JSHTMLForm_1_1 object
	 *
	 * @return    The results value
	 */
	public ResultDocList getResults() {
		return results;
	}


	/**
	 *  Gets the DocReader for the first item in the results. Appropriate for getRecord requests where you're
	 *  accessing a record by ID.
	 *
	 * @return    The DocReader for the first result.
	 */
	public DocReader getDocReader() {
		if (results == null || results.size() == 0)
			return null;
		return ((ResultDoc)results.get(0)).getDocReader();
	}


	/**
	 *  Sets the results attribute of the JSHTMLForm_1_1 object
	 *
	 * @param  results  The new results value
	 */
	public void setResults(ResultDocList results) {
		this.results = results;
	}


	/**
	 *  Gets the number of matching results.
	 *
	 * @return    The numResults value
	 */
	public int getNumResults() {
		if (results == null)
			return 0;
		return results.size();
	}


	boolean hasMenuItemSelected = false;


	/**
	 *  Sets whether one or more itmes in a menu is selected.
	 *
	 * @param  isSelected  True if one or more items in a menu is selected, else false
	 */
	public void setHasMenuItemSelected(boolean isSelected) {
		hasMenuItemSelected = isSelected;
	}


	/**
	 *  Gets whether one or more itmes in a menu is selected.
	 *
	 * @return    True if one or more items in a menu is selected, else false
	 */
	public boolean getHasMenuItemSelected() {
		return hasMenuItemSelected;
	}


	/**
	 *  A list of UTC dates in the past in the following order: one minute, one hour, one day, one week, one
	 *  month, one year.
	 *
	 * @return    A list of UTC dates in the past.
	 */
	public List getUtcDates() {
		long curTime = System.currentTimeMillis();
		long min = 1000 * 60;
		long hour = min * 60;
		long day = hour * 24;
		long week = day * 7;
		long month = day * 30;
		long year = day * 365;

		List dates = new ArrayList();

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'z'");
		df.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
		Date date = new Date(curTime - min);
		dates.add(new DateLabelPair(df.format(date), "one minute ago"));
		date.setTime(curTime - hour);
		dates.add(new DateLabelPair(df.format(date), "one hour ago"));
		date.setTime(curTime - day);
		dates.add(new DateLabelPair(df.format(date), "one day ago"));
		date.setTime(curTime - week);
		dates.add(new DateLabelPair(df.format(date), "one week ago"));
		date.setTime(curTime - month);
		dates.add(new DateLabelPair(df.format(date), "one month ago"));
		date.setTime(curTime - year);
		dates.add(new DateLabelPair(df.format(date), "one year ago"));
		return dates;
	}


	/**
	 *  DESCRIPTION
	 *
	 * @author     John Weatherley
	 * @version    $Id: JSHTMLForm_1_1.java,v 1.9 2010/07/14 00:19:27 jweather Exp $
	 */
	public class DateLabelPair {
		private String date, label;


		/**
		 *  Constructor for the DateLabelPair object
		 *
		 * @param  date   DESCRIPTION
		 * @param  label  DESCRIPTION
		 */
		public DateLabelPair(String date, String label) {
			this.date = date;
			this.label = label;
		}


		/**
		 *  Gets the date attribute of the DateLabelPair object
		 *
		 * @return    The date value
		 */
		public String getDate() {
			return date;
		}


		/**
		 *  Gets the label attribute of the DateLabelPair object
		 *
		 * @return    The label value
		 */
		public String getLabel() {
			return label;
		}
	}

	//================================================================

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDs() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final void prtlnErr(String s) {
		System.err.println(getDs() + " " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug)
			System.out.println(getDs() + " " + s);
	}


	/**
	 *  Sets the debug attribute
	 *
	 * @param  isDebugOuput  The new debug value
	 */
	public static void setDebug(boolean isDebugOuput) {
		debug = isDebugOuput;
	}
}


