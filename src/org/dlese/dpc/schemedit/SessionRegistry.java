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

import java.io.*;
import java.util.*;
import java.lang.IllegalStateException;

import org.dlese.dpc.xml.*;
import org.dlese.dpc.schemedit.security.user.User;
import org.dlese.dpc.util.strings.FindAndReplace;
import javax.servlet.http.*;
import javax.servlet.*;

/**
 *  Maintains a registry of sessions and also manages record locking.<p>
 *
 *  As ServletContextListener, receives "contextInitialized" event, and sets servletContext attribute.<p>
 *
 *  As HttpSessionListener, receives "sessionCreated" and "sessionDestroyed" events. {@link
 *  org.dlese.dpc.schemedit.SessionBean}s are created when the session is created, and destroyed when the
 *  session is destroyed.
 *
 * @author     ostwald 
 */
public class SessionRegistry implements ServletContextListener, HttpSessionListener {
	private static boolean debug = false;

	private HashMap activeSessions = null;
	private HashMap sessionBeanMap = null;
	private HashMap lockedRecords = null;
	private ServletContext servletContext = null;


	/**
	 *  Method called when a context is destroyed. Unregisters all sessions and empties lockedRecords.
	 *
	 * @param  event  Description of the Parameter
	 * @see           #unregisterSessionBean(String)
	 */
	public void contextDestroyed(ServletContextEvent event) {
		System.out.println("contextDestroyed()");
		synchronized (sessionBeanMap) {
			for (Iterator i = sessionBeanMap.keySet().iterator(); i.hasNext(); ) {
				String key = (String) i.next();
				unregisterSessionBean(key);
			}
		}
		sessionBeanMap = null;
		destroy();
	}


	/**
	 *  Called when context is initialized, sets ServletContext as class attribute.
	 *
	 * @param  event  Description of the Parameter
	 */
	public void contextInitialized(ServletContextEvent event) {
		ServletContext context = event.getServletContext();
		context.setAttribute("sessionRegistry", this);
		this.servletContext = context;
		System.out.println("sessionRegistry set as context attribute");
	}


	/**
	 *  Constructor for the SessionRegistry object.<p>
	 *
	 *  <b>activeSessionsMap</b> map maintains the set of ALL active sessions, and is only modified by {@link
	 *  #sessionCreated(HttpSessionEvent)} and {@link #sessionDestroyed(HttpSessionEvent)} listeners.<p>
	 *
	 *  <b>sessionBeanMap</b> maintains the set of sessionBean instances, which are created only for <i>
	 *  interactive sessions</i> - which excludes sessions created by webservice requests.
	 */
	public SessionRegistry() {
		activeSessions = new HashMap();
		sessionBeanMap = new HashMap();
		// activeSessions = (HashMap) Collections.synchronizedMap (new HashMap());
		lockedRecords = new HashMap();
	}


	/**
	 *  Gets the lockedRecords attribute of the SessionRegistry object, which stores all locked records and the
	 *  sessions that owns the lock.
	 *
	 * @return    The lockedRecords value
	 */
	public Map getLockedRecords() {
		return lockedRecords;
	}


	/**
	 *  Called when a session is created with sole effect of putting the created session into the activeSessions
	 *  map. Items are removed from the activeSessionsMap wehn sessions are destroyed.
	 *
	 * @param  se  notification event
	 */
	public void sessionCreated(HttpSessionEvent se) {
		HttpSession session = se.getSession();
		// prtln ("sessionCreated: " + session.getId());

		// here is where we can set session attributes, like inactivity timeout interval
		// BUT this particular session attribute is set in web.xml
		int timeOutMins = 120;
		session.setMaxInactiveInterval(timeOutMins * 60); // in seconds

		synchronized (activeSessions) {
			activeSessions.put(session.getId(), session);
		}
	}


	/**
	 *  Notified when a session is about to be destroyed - removes the session from the activeSessions map and
	 *  unregisteres sessionBean.
	 *
	 * @param  se  notification event
	 * @see        #unregisterSessionBean(String)
	 */
	public void sessionDestroyed(HttpSessionEvent se) {
		HttpSession session = se.getSession();
		session.setAttribute ("user", null);
		String sessionId = session.getId();

		synchronized (activeSessions) {
			activeSessions.remove(sessionId);
		}
		unregisterSessionBean(sessionId);
	}


	/**
	 *  Returns true if there is a lock for the given record.<p>
	 *
	 *
	 *
	 * @param  recId  Description of the Parameter
	 * @return        The locked value
	 */
	public boolean isLocked(String recId) {
		return (lockingSession(recId) != null);
	}


	/**
	 *  Return the sessionId that has lock on given record. If an inactive session has the lock, unregister the
	 *  sessionBean and return null to signify that the record has no locking session.
	 *
	 * @param  recId  A record Id
	 * @return        Id of locking session, or null if there is no locking session
	 */
	private String lockingSession(String recId) {
		String sessionId = (String) lockedRecords.get(recId);
		if (sessionId != null) {
			// is the session inActive or timedOut??
			SessionBean sessionBean = getSessionBean(sessionId);
			int secsToTimeOut = 1;
			if (sessionBean != null) {
				secsToTimeOut = sessionBean.getNumSecsToTimeout();
				// prtln ("locking session: secsToTimeOut: " + secsToTimeOut);
			}
			if (!sessionIsActive(sessionId) || secsToTimeOut < 0) {
				unregisterSessionBean(sessionId);
				sessionId = null;
			}
		}
		return sessionId;
	}


	/**
	 *  Is a record locked by the given session?
	 *
	 * @param  recId      Description of the Parameter
	 * @param  sessionId  Id of given session
	 * @return            true if session owns lock to record
	 */
	public boolean ownsLock(String recId, String sessionId) {
		String lockingSession = lockingSession(recId);
		if (lockingSession == null) {
			return false;
		}
		else {
			return (lockingSession.equals(sessionId));
		}
	}


	/**
	 *  Find records locked by a given session. Should never be more than one!
	 *
	 * @param  sessionId  Description of the Parameter
	 * @return            list of record Ids
	 */
	public List myLockedRecords(String sessionId) {
		List recList = new ArrayList();
		for (Iterator i = lockedRecords.keySet().iterator(); i.hasNext(); ) {
			String recId = (String) i.next();
			String mySessionId = (String) lockedRecords.get(recId);
			if (sessionId.equals(mySessionId)) {
				recList.add(recId);
			}
		}
		return recList;
	}


	/**  Release all locked records. */
	public synchronized void releaseAllLocks() {
		lockedRecords.clear();
	}


	/**
	 *  Release all the locks owned by a particluar session.
	 *
	 * @param  sessionId  Id of session that is releasing records.
	 */
	public synchronized void releaseAllLocks(String sessionId) {
		List recs = myLockedRecords(sessionId);
		for (Iterator i = recs.iterator(); i.hasNext(); ) {
			String id = (String) i.next();
			releaseLock(id, sessionId);
		}
	}


	/**
	 *  Gets the lock attribute of the SessionRegistry object.
	 *
	 * @param  recId      Description of the Parameter
	 * @param  sessionId  Description of the Parameter
	 * @return            true if lock is obtained by requesting session
	 */
	public synchronized boolean getLock(String recId, String sessionId) {
		// String lockingSession = (String) lockedRecords.get(recId);
		String lockingSession = lockingSession(recId);
		String logMsg = "session " + sessionId + " attempting to lock record " + recId;
		if (lockingSession == null) {
			lockedRecords.put(recId, sessionId);
			prtln(logMsg + " ... Success");
			return true;
		}

		if (lockingSession.equals(sessionId)) {
			prtln(logMsg + " ... Success\n\t session already owns lock");
			return true;
		}

		prtln(logMsg + " ... FAILURE\n\tlock held by session " + lockingSession);
		return false;
	}


	/**
	 *  A version of getLock that only allows a single record to be locked by a single session at a time
	 *
	 * @param  recId      Description of the Parameter
	 * @param  sessionId  Description of the Parameter
	 * @return            The uniqueLock value
	 */
	public synchronized boolean getUniqueLock(String recId, String sessionId) {
		List recs = myLockedRecords(sessionId);
		if (recs.size() == 1) {
			String myRecId = (String) recs.get(0);
			if (!myRecId.equals(recId)) {
				prtln("session " + sessionId + " already has a lock on a different record: " + recId);
				return false;
			}
		}
		if (recs.size() > 1) {
			prtln("session " + sessionId + " already has a lock on " + recs.size() + " records");
			return false;
		}

		if (!lockedRecords.containsKey(recId)) {
			lockedRecords.put(recId, sessionId);
			return true;
		}
		else {
			prtln("record " + recId + " is locked by another session");
			return false;
		}
	}


	/**
	 *  Releases the lock for given record, regardless of what session owns the lock.
	 *
	 * @param  recId  Description of the Parameter
	 * @return        true if there was a lock for given record ID
	 */
	public synchronized boolean releaseLock(String recId) {
		prtln("releasing lock for record " + recId + " (session unknown)");
		return (lockedRecords.remove(recId) == null);
	}


	/**
	 *  Release lock held by specified sesssion
	 *
	 * @param  recId      Description of the Parameter
	 * @param  sessionId  Description of the Parameter
	 * @return            true if specified session owned lock on record
	 */
	public synchronized boolean releaseLock(String recId, String sessionId) {
		String recordSession = (String) lockedRecords.get(recId);
		String logMsg = "session " + sessionId + " attempting to release lock on record " + recId;
		if (recordSession == null) {
			return true;
		}

		if (recordSession.equals(sessionId)) {
			lockedRecords.remove(recId);
			prtln(logMsg + " ... Success");
			return true;
		}
		else {
			prtln(logMsg + " ... FAILURE\n\trequesting session " + sessionId + " does not hold lock.\n\tlock held by " + recordSession);
			return false;
		}
	}


	/**
	 *  Register a sessionBean by putting it into the sessionBeanMap.
	 *
	 * @param  sessionBean  Description of the Parameter
	 */
	public void registerSessionBean(SessionBean sessionBean) {
		synchronized (sessionBeanMap) {
			sessionBeanMap.put(sessionBean.getId(), sessionBean);
		}
		prtln("registered sessionBean: " + sessionBean.getId());
	}


	/**
	 *  Unregister a sessionBean by calling its {@link org.dlese.dpc.schemedit.SessionBean#destroy() destroy}
	 *  method (releasing all held locks), and removing its entry from the sessionBeanMap.
	 *
	 * @param  sessionId  Description of the Parameter
	 */
	public void unregisterSessionBean(String sessionId) {
		prtln("unregistering Session Bean for " + sessionId);
		SessionBean sessionBean = null;
		try {
			sessionBean = getSessionBean(sessionId);
		} catch (IllegalStateException ise) {
			prtlnErr("ERROR: getSessionBean (" + sessionId + "): " + ise.getMessage());
		}
		if (sessionBean != null) {
			sessionBean.destroy();
		}
		else {
			// if the session has been timed-out, sessionBean will be null, so we must explicitly release locks ...
			releaseAllLocks(sessionId);
		}
		synchronized (sessionBeanMap) {
			sessionBeanMap.remove(sessionId);
		}
	}


	/**
	 *  Returns true if id refers to a session contained in the activeSessions map
	 *
	 * @param  id  Session id
	 * @return     true if id refers to an active session.
	 */
	private boolean sessionIsActive(String id) {
		return activeSessions.containsKey(id);
	}


	/**
	 *  Returns active session corresponding to given Id.
	 *
	 * @param  id  session id
	 * @return     a HttpSession instance, or null if none was found for given id.
	 */
	public HttpSession getSession(String id) {
		// prtln ("getFramework with " + frameworkName);
		return (HttpSession) activeSessions.get(id);
	}


	/**
	 *  Return the SessionBean associated with the HttpSession for the provided request.
	 *  <p>
	 *
	 *  If a sessionBean does not yet exist for a valid session, then it is created and bound to the session
	 *  context. This method is the standard way of obtaining of obtaining a sessionBean from within a Controller
	 *  or FormBean.
	 *
	 * @param  request  A request instance that is associated with a session
	 * @return          The sessionBean value for the request's session.
	 */
	public SessionBean getSessionBean(HttpServletRequest request) {
		// prtln("getSessionBean()");
		if (request == null) {
			prtln(" ...  REQUEST is NULL");
		}

		HttpSession session = request.getSession();
		if (session == null) {
			prtln(" ...  SESSION is NULL");
		}

		String sessionId = session.getId();
		SessionBean sessionBean = getSessionBean(sessionId);

		if (sessionBean == null && sessionIsActive(sessionId)) {
			prtln("sessionBean is null && session is active for id: " + sessionId);
			sessionBean = new SessionBean(session, servletContext);
			sessionBean.setIp(request.getRemoteAddr());
			registerSessionBean(sessionBean);
			session.setAttribute("sessionBean", sessionBean);
		}

		if (sessionBean == null) {
			/* here we have no sessionBean, and yet we have sessionID for an inActive session! 
				invalidate the sessionto get us out of the jam
			*/
			prtln("WARNING: getSessionBean() returning null");
			prtln("  ... sessionId: " + sessionId + "  - sessionIsActive: " + sessionIsActive(sessionId));
			prtln("  ... invalidating session ...");
			session.invalidate();
		}
		return sessionBean;
	}


	/**
	 *  Gets the sessionBean associated with the given sessionID from the sessionBeanMap.<p>
	 *
	 *  Retrieves a sessionBean from the sessionBeanMap after checking to ensure that the session is in the activeSessionMap. Does
	 *  NOT create a new SessionBean if one is not found.
	 *
	 * @param  sessionId  a session id
	 * @return            The sessionBean value or null if a sessionBean is not found or the session is not
	 *      active.
	 */
	private SessionBean getSessionBean(String sessionId) {
		if (sessionIsActive(sessionId)) {
			return (SessionBean) sessionBeanMap.get(sessionId);
		}
		else {
			return null;
		}
	}

	
	public List getUserSessionBeans (User user) {
		List usbs = new ArrayList();
		if (user == null || user.getUsername() == null)
			return usbs;
		
		for (Iterator i=getSessionBeans().iterator();i.hasNext();) {
			SessionBean sb = (SessionBean) i.next();
			User sbUser = sb.getUser();
			if (sbUser == null)
				continue;
			if (user.getUsername().equals (sbUser.getUsername()))
				usbs.add (sb);
		}
		return usbs;
	}

	/**
	 *  Returns a list of all active SessionBean instances.<p>
	 *
	 *  Unregisters sessionBeans corresponding to sessions that have been invalidated.
	 *
	 * @return    list of {@link org.dlese.dpc.schemedit.SessionBean}
	 */
	public List getSessionBeans() {
		// prtln("getSessionBeans()");

		// traverse the sessionBeanMap and verify that each has an active session
		ArrayList sbList = new ArrayList();
		ArrayList deadBeans = new ArrayList();
		synchronized (sessionBeanMap) {
			for (Iterator i = sessionBeanMap.keySet().iterator(); i.hasNext(); ) {
				String id = (String) i.next();
				SessionBean sb = getSessionBean(id);
				if (sb != null && sb.getNumSecsToTimeout() > -1) {
					sbList.add(sb);
				}
				else {
					deadBeans.add(id);
				}
			}
			// prtln("  ... " + sbList.size() + " live beans, " + deadBeans.size() + " dead ones");
			
			// unregister SessionBeans whose sessions have been destroyed
			for (Iterator i = deadBeans.iterator(); i.hasNext(); ) {
				String deadId = (String) i.next();
				unregisterSessionBean(deadId);
			}
		}
		Collections.sort(sbList, new SessionBean.IdleTimeComparator());
		return sbList;
	}


	/**  Release locked records */
	public void destroy() {
		prtln("destroy");
		lockedRecords.clear();
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("SessionRegistry: " + s);
		}
	}

	private static void prtlnErr(String s) {
			System.out.println("SessionRegistry: " + s);
	}
	
}

