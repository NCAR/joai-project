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
import java.text.*;
import java.io.Serializable;
import javax.servlet.http.*;
import javax.servlet.ServletContext;

import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.XMLDocReader;
import org.dlese.dpc.repository.*;
import org.dlese.dpc.schemedit.dcs.*;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.schemedit.action.form.*;
import org.dlese.dpc.schemedit.security.access.AccessManager;
import org.dlese.dpc.schemedit.security.access.Roles;
import org.dlese.dpc.schemedit.ndr.SyncService;
import org.dlese.dpc.util.Utils;

import org.dlese.dpc.schemedit.security.user.User;

import org.apache.struts.util.LabelValueBean;

/**
 *  A Session-scoped Bean for information that needs to be available to
 *  different controllers and their jsp pages. Encapsulates access to servlet
 *  context attributes as well as session scope attributes, and provides other
 *  session-oriented services, such as
 *  <ul>
 *    <li> record locking (e.g., {@link #getLock(String)}), and </li>
 *    <li> navigation support (e.g., {@link #getPaigingParam()})</li>
 *  </ul>
 *  SessionBeans are bound to Session objects when they are created. This is
 *  done in {@link org.dlese.dpc.schemedit.SessionRegistry}. Because they are
 *  bound to the session context, SessionBeans are available to jsp pages.<p>
 *
 *  This class aims as much as possible to be a <b>read-only</b> structure that
 *  gets information from the application's form beans which are accessed via
 *  the session.<p>
 *
 *  A tricky aspect of accessing Session attributes is that the session can be
 *  invalidated at any time (e.g., when a session times out). Thus, accesses to
 *  session attributes must be wrapped in a try statement that can catch a
 *  IllegalStateException exception. When such an exception is encountered, the
 *  SessionBean instance is {@link org.dlese.dpc.schemedit.SessionBean#destroy()
 *  destroyed}. As a HttpSessionBindingListener, receives notification when
 *  attributes are bound to, or removed from, the session context.
 *
 *@author    ostwald
 *
 */
public class SessionBean implements HttpSessionBindingListener, Serializable {

	private static boolean debug = false;
	private HttpSession session = null;
	private ServletContext servletContext = null;
	private String id = "";
	private String ip = "";

	/**
	 *  Specifies the default sort order for display of records. Used in {@link
	 *  org.dlese.dpc.schemedit.action.form.DCSQueryForm#getSortRecsBy()}.
	 */
	public static String DEFAULT_REC_SORT = "idvalue";
	private String recId = null;
	private List sets = null;
	private ArrayList collectionLabelValues = null;

	private LabelValueBean collectionFilter = new LabelValueBean();
	
	private DCSQueryForm queryForm = null;
	private DCSViewForm viewForm = null;
	private SchemEditForm schemEditForm = null;
	private RecordList failedBatchLocks = null;
	private String editor = "";
	private HttpServletRequest request = null;
	
	private SearchHelper searchHelper = null;


	/**
	 *  Constructor for the SessionBean object
	 *
	 *@param  session         Description of the Parameter
	 *@param  servletContext  Description of the Parameter
	 */
	public SessionBean(HttpSession session, ServletContext servletContext) {
		this.servletContext = servletContext;
		this.session = session;
		if (session != null) {
			id = session.getId();
		}
		searchHelper = new SearchHelper (this.getIndex());
	}

	/**
	* The collectionFilter is used to filter UI displays using dcsTables
	*/
	public void updateCollectionFilter (String column, String value) {
		prtln ("updateCollectionFilter: col: " + column + "  val: " + value);
		this.collectionFilter.setLabel (column);
		this.collectionFilter.setValue (value);
	}
	
	public LabelValueBean getCollectionFilter () {
		return this.collectionFilter;
	}

	/**
	 *  Argumentless Constructor for the SessionBean object.
	 */
	public SessionBean() {
		this(null, null);
		prtln("sessionBean created without a session");
	}


	/**
	 *  Gets the session attribute of the SessionBean object
	 *
	 *@return    The session value
	 */
	public HttpSession getSession() {
		return session;
	}

	public User getUser () {
		if (session == null) {
			prtlnErr ("session is null - can't get user");
			return null;
		}
		return (User) session.getAttribute("user");
	}

	/**
	 *  get the last request for this session - NOT currently used
	 *
	 *@return    The request value
	 */
	public HttpServletRequest getRequest() {
		return request;
	}


	/**
	 *  Sets the request attribute of the SessionBean object
	 *
	 *@param  request  The new request value
	 */
	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}


	/**
	 *  Sets the recId attribute of the SessionBean object
	 *
	 *@param  id  The new recId value
	 */
	public void setRecId(String id) {
		recId = id;
	}


	/**
	 *  Gets the lastAccessedTime attribute of the session as a Date.
	 *
	 *@return    The lastAccessedTime value
	 */
	public Date getLastAccessedTime() {
		if (session != null) {
			try {
				return new Date(session.getLastAccessedTime());
			} catch (java.lang.IllegalStateException e) {}
		}
		return new Date(0);
	}


	/**
	 *  Gets the timeSinceLastAccessed of the session as a formated String for use
	 *  in jsp.<p>
	 *
	 *
	 *
	 *@return    The timeSinceLastAccessed value
	 */
	public String getTimeSinceLastAccessed() {
		if (session != null) {
			try {
				long clicks = session.getLastAccessedTime();
				// return SchemEditUtils.getElapsedSeconds(clicks);
				return Utils.convertMillisecondsToTime(new Date().getTime() - clicks);
			} catch (java.lang.IllegalStateException e) {
				destroy();
			}
		}
		return ("session terminated");
	}

	/**
	 *  Returns the number of seconds left until this session times out. Dependent
	 *  on the session's MaxInactiveInterval
	 *
	 *@return    The numSecsToTimeout value
	 */
	public int getNumSecsToTimeout() {
		int secsToTimeout = -1;
		try {
			long lastAccessTime = session.getLastAccessedTime();
			// prtln ("lastAccessTime: " + lastAccessTime);
			long nowSecs = new Date().getTime();
			// prtln ("nowSecs: " + nowSecs);
			long lastAccessSecs = (nowSecs - lastAccessTime) / 1000;
			// prtln ("lastAccessSecs: " + lastAccessSecs);
			int maxInactiveInterval = session.getMaxInactiveInterval();
			// prtln ("maxInactiveInterval: " + maxInactiveInterval);
			secsToTimeout = maxInactiveInterval - new Long(lastAccessSecs).intValue();
			// prtln ("secsToTimeout: " + secsToTimeout);
		} catch (Throwable t) {
			// t.printStackTrace();
			if (session == null) {
				prtln("getNumSecsToTimeout called with non-existant session");
			}
		}
		return secsToTimeout;
	}


	/**
	 *  Gets the new attribute of the SessionBean object
	 *
	 *@return    The new value
	 */
	public boolean isNew() {
		if (session != null) {
			try {
				return session.isNew();
			} catch (java.lang.IllegalStateException e) {
				prtln("isNew() caught exception: " + e.getMessage());
				prtln("destroying session");
				destroy();
			}
		}
		prtln("isNew called with session == null. returning false");
		return false;
	}


	/**
	 *  Gets the inactiveIntervalRemaining attribute of the SessionBean object
	 *
	 *@return    The inactiveIntervalRemaining value
	 */
	public String getInactiveIntervalRemaining() {
		String ret = "Unknown";
		try {
			long lastAccessTime = session.getLastAccessedTime();
			long now = new Date().getTime();
			long lastAccess = now - lastAccessTime;
			long maxInterval = (long) session.getMaxInactiveInterval() * 1000;
			ret = Utils.convertMillisecondsToTime(maxInterval - lastAccess);
		} catch (Throwable t) {
			// t.printStackTrace();
		}
		return ret;
	}


	/**
	 *  Gets the timeSinceCreation attribute of the session as a formatted string.
	 *
	 *@return    The timeSinceCreation value
	 */
	public String getTimeSinceCreation() {
		if (session != null) {
			try {
				long clicks = session.getCreationTime();
				return Utils.convertMillisecondsToTime(new Date().getTime() - clicks);
			} catch (java.lang.IllegalStateException e) {
				destroy();
			}
		}
		return ("session terminated");
	}


	/**
	 *  Gets the recId attribute of the SessionBean object, which keeps track of
	 *  the record the user has last edited so that particular record can be
	 *  highlighted in lists of records.<p>
	 *
	 *@return    The recId value
	 */
	public String getRecId() {
		return recId;
	}

	/**
	 *  Gets the id of this session.
	 *
	 *@return    The sessionId value
	 */
	public String getId() {
		return id;
	}


	/**
	 *  Gets the ip attribute of the SessionBean object
	 *
	 *@return    The ip value
	 */
	public String getIp() {
		return ip;
	}


	/**
	 *  Sets the ip attribute of the SessionBean object
	 *
	 *@param  ip  The new ip value
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}


	/**
	 *  Gets the records locked by this session.
	 *
	 *@return    A List of record Ids
	 */
	public List getLockedRecords() {
		SessionRegistry sr = getSessionRegistry();
		if (sr == null) {
			prtln(" sessionRegistry not found");
			return null;
		}
		List recs = sr.myLockedRecords(id);
		Collections.sort(recs);
		return recs;
	}


	/**
	 *  Method called each time a attribute is bound to this SessionBean's session.
	 *  <P>
	 *
	 *  This method is not currently used ...
	 *
	 *@param  event  Description of the Parameter
	 */
	public void valueBound(HttpSessionBindingEvent event) {
		HttpSession session = event.getSession();
		// prtln(event.getName() + " bound for session " + session.getId());
	}


	/**
	 *  Method called each time any attribute is removed from this SessionBean's
	 *  session.<P>
	 *
	 *  This method is not currently used ...
	 *
	 *@param  event  Description of the Parameter
	 */
	public void valueUnbound(HttpSessionBindingEvent event) {
		HttpSession session = event.getSession();
		// prtln(event.getName() + " removed from session " + session.getId());
	}


	/**
	 *  Get the RepositoryManager from the servletContext
	 *
	 *@return    The repositoryManager value
	 */
	private RepositoryManager getRepositoryManager() {
		RepositoryManager rm = null;
		if (servletContext != null) {
			rm = (RepositoryManager) servletContext.getAttribute("repositoryManager");
		}
		return rm;
	}

	private SimpleLuceneIndex getIndex () {
		SimpleLuceneIndex index = null;
		try {
			index = this.getRepositoryManager().getIndex();
		} catch (Exception e) {
			prtln ("WARNING: repositoryManager not found by getFailedBatchLocks");
		}
		return index;
	}
	

	/**
	 *  Get the DcsDataManager from the servletContext
	 *
	 *@return    The dcsDataManager value
	 */
	private DcsDataManager getDcsDataManager() {
		DcsDataManager mgr = null;
		if (servletContext != null) {
			mgr = (DcsDataManager) servletContext.getAttribute("dcsDataManager");
		}
		return mgr;
	}

	private AccessManager getAccessManager() {
		AccessManager mgr = null;
		if (servletContext != null) {
			mgr = (AccessManager) servletContext.getAttribute("accessManager");
		}
		return mgr;
	}
	
	/**
	 *  Get DcsDataRecord for the given id via the DcsDataManager
	 *
	 *@param  id  Description of the Parameter
	 *@return     The dcsDataRecord value
	 */
	public DcsDataRecord getDcsDataRecord(String id) {
		RepositoryManager rm = getRepositoryManager();
		DcsDataManager dm = getDcsDataManager();
		if (rm != null && dm != null) {
			return dm.getDcsDataRecord(id, rm);
		}
		else {
			prtln ("unable to obtain dcsDataRecord for " + id);
			return null;
		}
	}


	/**
	 *  Gets the AttributeNames defined in the servlet Context.
	 *
	 *@return    A list of attribute names
	 */
	public List getServletContextAttributeNames() {
		List names = new ArrayList();
		if (servletContext != null) {
			for (Enumeration e = servletContext.getAttributeNames(); e.hasMoreElements(); ) {
				String myName = (String) e.nextElement();
				names.add(myName);
			}
		}
		return names;
	}


	/**
	 *  Does this session own the lock for the record?
	 *
	 *@param  recId  Description of the Parameter
	 *@return        Description of the Return Value
	 */
	public boolean ownsLock(String recId) {
		SessionRegistry sr = getSessionRegistry();
		if (sr == null) {
			prtln("ownsLock() sessionRegistry not found");
			return false;
		}
		return sr.ownsLock(recId, id);
	}

	/**
	 *  Release lock for given record.
	 *
	 *@param  recId  Id of record to release
	 *@return        Description of the Return Value
	 */
	public boolean releaseLock(String recId) {
		// prtln ("session " + id + " releasing record " + recId);
		SessionRegistry sr = getSessionRegistry();
		if (sr == null) {
			prtln("releaseLock() sessionRegistry not found");
			return false;
		}
		return sr.releaseLock(recId, id);
	}


	/**
	 *  Release all locks held by this session.
	 *
	 *@return    Description of the Return Value
	 */
	public boolean releaseAllLocks() {
		if (session == null) {
			prtln("releaseAllLocks() session is null");
			return false;
		}
		// prtln ("session " + id + " releasing all locked records");
		SessionRegistry sr = getSessionRegistry();
		if (sr == null) {
			prtln("releaseAllLocks() sessionRegistry not found");
			return false;
		}
		sr.releaseAllLocks(id);
		return true;
	}


	/**
	 *  Locks a record for this session through a call to {@link
	 *  org.dlese.dpc.schemedit.SessionRegistry#getLock(String, String)}.
	 *
	 *@param  recId  Description of the Parameter
	 *@return        The lock value
	 */
	public boolean getLock(String recId) {
		// prtln("session " + id + " attempting to lock record " + recId);
		SessionRegistry sr = getSessionRegistry();
		if (sr == null) {
			prtln(" sessionRegistry not found");
			return false;
		}
		return sr.getLock(recId, id);
	}


	/**
	 *  List of ids to records that could not be locked during getBatchLocks.
	 *
	 *@return    a list of {@link org.dlese.dpc.index.ResultDoc} instances.
	 *@see       #getBatchLocks(ResultDoc[])
	 */
	public RecordList getFailedBatchLocks() {

		if (failedBatchLocks == null) {
			failedBatchLocks = new RecordList();
		}
		return failedBatchLocks;
	}

	public boolean getBatchLocks(RecordList records) {
		List failedLocks = new ArrayList();
		if (!records.isEmpty()) {
			for (Iterator i = records.iterator();i.hasNext();) {
				String id = (String)i.next();
				if (!getLock(id)) {
					failedLocks.add(id);
				}
			}
			
			//String [] failedLocksArray = failedLocks.toArray();
		}
		String [] failedLocksArray = (String[]) failedLocks.toArray(new String[]{});
		failedBatchLocks = new RecordList (failedLocksArray, getIndex());
		
		if (failedBatchLocks.size() > 0) {
			this.releaseAllLocks();
			return false;
		}
		return true;
	}
	
	/**
	 *  Gets this session's DCSQueryForm from the session attribute.
	 *
	 *@return    The queryForm value
	 */
	private DCSQueryForm getQueryForm() {
		if (queryForm == null) {
			if (session == null) {
				prtln("  .. couldn't find session ... returning null");
				return null;
			}
			try {
				queryForm = (DCSQueryForm) session.getAttribute("queryForm");
			} catch (java.lang.IllegalStateException e) {}
		}
		return queryForm;
	}


	/**
	 *  Gets this session's DCSViewForm from the session attribute.
	 *
	 *@return    The viewForm value
	 */
	private DCSViewForm getViewForm() {
		if (viewForm == null) {
			if (session == null) {
				prtln("  .. couldn't find session ... returning null");
				return null;
			}
			try {
				viewForm = (DCSViewForm) session.getAttribute("viewForm");
			} catch (java.lang.IllegalStateException e) {}
		}
		return viewForm;
	}


	/**
	 *  Gets this session's SchemEditForm from the session attribute.
	 *
	 *@return    The schemEditForm value
	 */
	private SchemEditForm getSchemEditForm() {
		if (schemEditForm == null) {
			if (session == null) {
				prtln("  .. couldn't find session ... returning null");
				return null;
			}
			try {
				schemEditForm = (SchemEditForm) session.getAttribute("sef");
			} catch (java.lang.IllegalStateException e) {}
		}
		return schemEditForm;
	}
	
	/**
	 *  Gets the authorized attribute of the SessionBean object
	 *
	 *@param  operation  Description of the Parameter
	 *@return            The authorized value
	 */
	public boolean isAuthorized(String operation) {
		return isAuthorized(operation, null);
	}

	/**
	 *  Gets the authorized attribute of the SessionBean object
	 *
	 *@param  operation   Description of the Parameter
	 *@param  collection  Description of the Parameter
	 *@return             The authorized value
	 */
	public boolean isAuthorized(String operation, String collection) {
		// if "Operation" is a path, then see if sessionUser has permission to for this path
		if (operation.charAt(0) == '/') {
			String path = operation;
			AccessManager am = getAccessManager();
			if (am != null) {
				return isAuthorizedCollection (am.getRoleForPath(path), collection);
			}
			else {
				prtlnErr ("isAuthorized() could not find accessManager - disallowing operation!");
				return false;
			}
		}
		
		RoleManager roleManager = getRoleManager();
		if (roleManager == null) {
			prtln("WARNING: roleManager not found, blindly authorizing operation");
			return true;
		}
		return roleManager.isAuthorized(operation, this, collection);
	}



	public boolean isAuthorizedCollection (Roles.Role role, String collection) {
		User user = getUser();
		if (role == Roles.NO_ROLE)
			return true;
		if (user == null)
			return false;
		return user.hasRole (role, collection);
	}

	/**
	 *  Gets the sessionAttributeNames of this session.
	 *
	 *@return    The sessionAttributeNames value
	 */
	public List getSessionAttributeNames() {
		List list = new ArrayList();
		if (session != null) {
			try {
				for (Enumeration e = session.getAttributeNames(); e.hasMoreElements(); ) {
					String att = (String) e.nextElement();
					list.add(att);
				}
			} catch (java.lang.IllegalStateException e) {}
		}
		return list;
	}


	/**
	 *  Gets the Global {@link org.dlese.dpc.schemedit.SessionRegistry} from the
	 *  servlet context,
	 *
	 *@return    The sessionRegistry value
	 */
	public SessionRegistry getSessionRegistry() {
		if (servletContext == null) {
			prtln("getSessionRegistry: failed to find servletContext");
			return null;
		}
		return (SessionRegistry) servletContext.getAttribute("sessionRegistry");
	}


	/**
	 *  Gets the roleManager attribute of the SessionBean object
	 *
	 *@return    The roleManager value
	 */
	public RoleManager getRoleManager() {
		if (servletContext == null) {
			prtln("getSessionRegistry: failed to find servletContext");
			return null;
		}
		return (RoleManager) servletContext.getAttribute("roleManager");
	}


	/**
	 *  Gets the collectionRegistry attribute of the SessionBean object
	 *
	 *@return    The collectionRegistry value
	 */
	public CollectionRegistry getCollectionRegistry() {
		if (servletContext == null) {
			prtln("getCollectionRegistry: failed to find servletContext");
			return null;
		}

		return (CollectionRegistry) servletContext.getAttribute("collectionRegistry");
	}


	/**
	 *  Gets the collectionConfig attribute of the SessionBean object
	 *
	 *@param  collection  Description of the Parameter
	 *@return             The collectionConfig value
	 */
	public CollectionConfig getCollectionConfig(String collection) {
		if (getCollectionRegistry() != null) {
			return getCollectionRegistry().getCollectionConfig(collection);
		}
		return null;
	}


	/**
	 *  Gets the finalStatusLabel attribute of the SessionBean object
	 *
	 *@param  collection  Description of the Parameter
	 *@return             The finalStatusLabel value
	 */
	public String getFinalStatusLabel(String collection) {
		CollectionRegistry reg = getCollectionRegistry();
		if (reg == null) {
			prtln("getFinalStatusLabel cannot obtain CollectionRegistry");
			return StatusFlags.DEFAULT_FINAL_STATUS;
		}
		return reg.getFinalStatusLabel(collection);
	}

	public SearchHelper getSearchHelper () {
		if (this.searchHelper == null) {
			if (this.getIndex() == null)
				prtlnErr ("WARNING: getSearchHelper could not retrieve Index");
			else
				this.searchHelper = new SearchHelper (this.getIndex());
		}
		return this.searchHelper;
	}
	
	/**
	* Provides access to current set of search results, which is updated by
	* DCSQueryAction, but needed by other actions that need to operate over the results
	* (e.g., {@link org.dlese.dpc.schemedit.action.BatchOperationsAction}).
	*/
 	public RecordList getRecords () {
		prtln ("\ngetRecords() retrieving RecordList");
		return new RecordList (this.getSearchHelper().getResults(), getIndex());
	}

	/**
	 *  A string representation (http request's query parameters) of the last
	 *  search performed by the user.<p>
	 *
	 *  Enables system to take user back to the last search they performed. For
	 *  example,searchParams is used by the "Search" link in the page header, so
	 *  when user returns to search it is as they left it. queryForm
	 *
	 *@return    The part of a url that specifies a search
	 */
	public String getSearchParams() {
		// prtln ("searchParams");
		queryForm = getQueryForm();
		if (queryForm != null) {
			return queryForm.getNonPaigingParams();
		}
		else {
			// prtln ("getSearchParams: queryForm was null");
			return "";
		}
	}


	/**
	 *  Wipes out information about the last search.
	 */
	public void clearSearchParams() {
		queryForm = getQueryForm();
		if (queryForm != null) {
			queryForm.setNonPaigingParams(null);
		}
		else {
			// prtln ("clearSearchParams: queryForm was null");
		}
	}
	
	/**
	 *  Compute the start record index of the page on which the current record
	 *  (recIndex) will be found.
	 *
	 *@return    The paigingParam value
	 */
	public int getPaigingParam() {

		try {
			DCSQueryForm queryForm = this.getQueryForm();
			return queryForm.getPaigingParam ();
		} catch (Throwable t) {
			// prtln ("getPaigingParam() error: " + t);
		}
		return 0;
	}

	/**
	 *  Compute the start record index of the page on which the specified record
	 *  will be found.
	 *
	 *@param  id  Description of the Parameter
	 *@return     The paigingParam value
	 */
	public int getPaigingParam(String id) {

		try {
			DCSQueryForm queryForm = this.getQueryForm();
			return queryForm.getPaigingParam (id);
		} catch (Throwable t) {
			// prtln ("getPaigingParam() error: " + t);
		}
		return 0;
	}

	/**
	 * Returns the url (decoded to preserve query string) that will reproduce the 
	 * last query preformed on the search page.
	 */
	public String getQueryUrl () {
		prtln ("\ngetQueryUrl");
		String queryUrl = null;
		try {
			String searchParams = getSearchParams();
			int paigingParam = getPaigingParam();
			prtln ("\tsearchParams: " + searchParams);
			prtln ("\tpagingParam: " + paigingParam);
			queryUrl = "/browse/query.do?s=" + paigingParam + searchParams;
			prtln ("\tqueryUrl: " + queryUrl);
			queryUrl = java.net.URLDecoder.decode(queryUrl);
			prtln ("decoded: " + queryUrl);
		} catch (Throwable t) {
			prtlnErr ("WARNING: sessionBean unable to compute queryUrl: " + t.getMessage());
			return "/browse/home.do";
		}
		return queryUrl;
	}
	
	/**
	 *  Gets a List of {@link org.dlese.dpc.repository.SetInfo} objects that
	 *  provide information about the collections known to RepositoryManager.<p>
	 *
	 *  NOTE: why isn't the set list encapsulated by CollectionRegistry, and the
	 *  {@link org.dlese.dpc.repository.SetInfo}s themselves by CollectionConfig.
	 *  Then, the CollectionRegistry would be accessed through the servlet context.
	 *
	 *@return    A List of SetInfo objects.
	 */
	 public List getSets() {
		if (sets == null)
			sets = new ArrayList ();
		return sets;
	 }
	 
	/**
	 *  Sets the sets attribute of the SessionBean object
	 *
	 *@param  sets  The new sets value
	 */
	public void setSets(List sets) {
		if (sets == null)
			this.sets = new ArrayList ();
		else
			this.sets = sets;
	}
	
	/**
	 *  Get the keys of the collections the sessionUser is authorized to access.
	 *
	 * @param  sessionBean  NOT YET DOCUMENTED
	 * @return              The authorizedCollections value
	 */
	public List getAuthorizedCollections() {
		List cols = new ArrayList();
		List sets = this.getSets();
		if (sets != null) {
			for (Iterator i = sets.iterator(); i.hasNext(); ) {
				SetInfo set = (SetInfo) i.next();
				cols.add(set.getSetSpec());
			}
		}
		return cols;
	}
	
	/**
	 *  Return a query clause ORing together all the collections the current user
	 *  is authorized to search over.
	 *
	 * @param  sessionBean  NOT YET DOCUMENTED
	 * @return              The collectionsQueryClause value
	 */
	public String getCollectionsQueryClause() {
		String query = "";
		List colList = this.getAuthorizedCollections();
		String[] collections = (String[]) colList.toArray(new String[]{});
		if (collections != null && collections.length > 0) {
			query = "(collection:0" + collections[0];
			for (int i = 1; i < collections.length; i++) {
				query += " OR collection:0" + collections[i];
			}
			query += ")";
		}
		return query;
	}
	
	

	// -------- Query Selector support -------------
	private boolean querySelectorsInitialized = false;
	
	public boolean isQuerySelectorsInitialized () {
		return querySelectorsInitialized;
	}
	
	public void setQuerySelectorsInitialized (boolean b) {
		querySelectorsInitialized = b;
	}
	
	private long indexLastModified = -1;
	
	public long getIndexLastModified () {
		return indexLastModified;
	}
	
	public void setIndexLastModified (long mod) {
		indexLastModified = mod;
	}
	
	private long collectionConfigMod = -1;
	
	public long getCollectionConfigMod () {
		return collectionConfigMod;
	}
	
	public void setCollectionConfigMod (long mod) {
		collectionConfigMod = mod;
	}
	
	/** 
	 *  mapping from unique status labels to a list of all the status 
	 *  values that have that label.
	*/
	private Map statuses = null;
	
	public Map getStatuses () {
		return statuses;
	}
	
	public void setStatuses (Map statusMap) {
		statuses = statusMap;
	}
	
	public List indexedFormats = null;
	
	public List getIndexedFormats () {
		return indexedFormats;
	}
	
	public void setIndexedFormats (List formats) {
		indexedFormats = formats;
	}

	public List editors = null;
	
	public List getEditors () {
		return editors;
	}
	
	public void setEditors (List editors) {
		this.editors = editors;
	}
	
	public List creators = null;
	
	public List getCreators () {
		return creators;
	}
	
	public void setCreators (List creators) {
		this.creators = creators;
	}
	
	/**
	 *  Generate list of collections for use by jsp tags. Note: this could also be
	 done by CollectionRegistry?
	 *
	 *@return    The collectionLabelValues value
	 */
	public List getCollectionLabelValues() {
		collectionLabelValues = new ArrayList();

		// for (Iterator i=getSets().iterator();i.hasNext();) {
		for (Iterator i = getSets().iterator(); i.hasNext(); ) {
			SetInfo setInfo = (SetInfo) i.next();
			String name = setInfo.getName();
			String setSpec = setInfo.getSetSpec();
			collectionLabelValues.add(new LabelValueBean(name, "0" + setSpec));
		}
		return collectionLabelValues;
	}

	private SyncService syncService = null;
	
	public SyncService getSyncService () {
		if (this.session == null) {
			prtlnErr ("getSyncService could not find session");
			return null;
		}
		return (SyncService)this.session.getAttribute("syncService");
	}
	
	public void setSyncService (SyncService svc) {
		if (this.session == null) {
			prtlnErr ("setSyncService could not find session");
			return;
		}
		this.session.setAttribute ("syncService", svc);
	}
	
	/**
	 *  Description of the Method
	 */
	public void destroy() {
		prtln("destroying sessionBean for " + id);
		this.releaseAllLocks();
		this.session = null;
		this.sets = null;
		this.queryForm = null;
		this.viewForm = null;
		this.schemEditForm = null;
		this.failedBatchLocks = null;
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "SessionBean");
		}
	}

	private static void prtlnErr (String s) {
		SchemEditUtils.prtln(s, "SessionBean");
	}
	
	/**
	 *  Implements Comparator to enable sorting by session id.
	 *
	 *@author    ostwald
	 */
	public static class IdComparator implements Comparator {
		/**
		 *  Campares the session id field.
		 *
		 *@param  O1                      A SessionBean Object
		 *@param  O2                      A SessionBean Object
		 *@return                         A negative integer, zero, or a positive
		 *      integer as the first argument is less than, equal to, or greater than
		 *      the second.
		 *@exception  ClassCastException  If Object is not DcsSetInfo
		 */
		public int compare(Object O1, Object O2)
			throws ClassCastException {
			String one = ((SessionBean) O1).getId();
			String two = ((SessionBean) O2).getId();

			return two.compareTo(one);
		}
	}


	/**
	 *  Implements Comparator to enable sorting SessionBeans by their sessions idle
	 *  time
	 *
	 *@author    ostwald
	 */
	public static class IdleTimeComparator implements Comparator {
		/**
		 *  Campares the session id field.
		 *
		 *@param  O1                      A SessionBean Object
		 *@param  O2                      A SessionBean Object
		 *@return                         A negative integer, zero, or a positive
		 *      integer as the first argument is less than, equal to, or greater than
		 *      the second.
		 *@exception  ClassCastException  If Object is not DcsSetInfo
		 */
		public int compare(Object O1, Object O2)
			throws ClassCastException {
			Date one = ((SessionBean) O1).getLastAccessedTime();
			Date two = ((SessionBean) O2).getLastAccessedTime();

			return two.compareTo(one);
		}
	}

}

