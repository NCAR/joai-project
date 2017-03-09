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
package org.dlese.dpc.schemedit.security.access;

import java.io.File;
import java.util.*;

import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;

import org.apache.struts.action.ActionMapping;
import org.dlese.dpc.schemedit.struts.HotActionMapping;
import org.apache.struts.util.WildcardHelper;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.schema.DocMap;
import org.dlese.dpc.schemedit.config.AbstractConfigReader;
import org.dlese.dpc.schemedit.security.access.Roles;

/**
 *  NOT YET DOCUMENTED
 *
 * @author     Jonathan Ostwald
 * @version    $Id: AccessManager.java,v 1.6 2009/07/07 02:58:29 ostwald Exp $
 */
public class AccessManager extends AbstractConfigReader {

	static boolean debug = false;
	private List legalRoles = null;
	Map actionPathMap = null;
	Map guardedPathMap = null;

	private static AccessManager instance = null;


	/**
	 *  Gets the instance attribute of the AccessManager class, but only if
	 *  AccessManager has already been instantiated.
	 *
	 * @return    The instance value
	 */
	public static AccessManager getInstance() {
		if (instance == null) {
			prtlnErr("ERROR: AccessManager must be instantiated before this call can succeed");
		}
		return instance;
	}


	/**
	 *  Return a singleton instance of the AccessManager class.
	 *
	 * @param  source          NOT YET DOCUMENTED
	 * @param  actionMappings  NOT YET DOCUMENTED
	 * @return                 The instance value
	 */
	public static AccessManager getInstance(File source, List actionMappings) {
		if (instance == null) {
			try {
				instance = new AccessManager(source, actionMappings);
			} catch (Exception e) {
				prtlnErr("ERROR instantiating role manager: " + e.getMessage());
			}
		}
		return instance;
	}


	/**
	 *  Constructor for the AccessManager object
	 *
	 * @param  source          NOT YET DOCUMENTED
	 * @param  actionMappings  NOT YET DOCUMENTED
	 * @exception  Exception   NOT YET DOCUMENTED
	 */
	public AccessManager(File source, List actionMappings) throws Exception {
		super(source);
		// prtln ("\nconfig document: " + Dom4jUtils.prettyPrint (docMap.getDocument()));
		pathMapInit(actionMappings);
		configureActionPaths();
		configureGuardedPaths();
		try {
			alignActionsToGuardedPaths();
		} catch (Throwable t) {
			prtlnErr("alignActionsToGuardedPaths error: " + t.getMessage());
			t.printStackTrace();
		}
		prtln("accessManager intitialized");
	}


	/**
	 *  Gets the childText attribute of the AccessManager object
	 *
	 * @param  parent     NOT YET DOCUMENTED
	 * @param  childName  NOT YET DOCUMENTED
	 * @return            The childText value
	 */
	private String getChildText(Element parent, String childName) {
		try {
			return parent.element(childName).getText();
		} catch (Throwable t) {
			return "";
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  actionMappings  NOT YET DOCUMENTED
	 */
	private void pathMapInit(List actionMappings) {
		// prtln ("init()");
		actionPathMap = new TreeMap();
		for (Iterator i = actionMappings.iterator(); i.hasNext(); ) {
			HotActionMapping mapping = (HotActionMapping) i.next();
			actionPathMap.put(mapping.getPath(), new ActionPath(mapping));
		}
	}


	/**
	 *  Gets the roles attribute of the AccessManager object
	 *
	 * @return    The roles value
	 */
	public Collection getRoles() {
		return getRoles(Roles.ADMIN_ROLE);
	}


	/**
	 *  Gets the roles attribute of the AccessManager object
	 *
	 * @param  maxRole  NOT YET DOCUMENTED
	 * @return          The roles value
	 */
	public Collection getRoles(Roles.Role maxRole) {
		/* 		List roleValues = new ArrayList ();
		Roles roles = Roles.getInstance();
		for (Iterator i=roles.getRoles().iterator();i.hasNext();) {
			String role = (String)i.next();
			if (roles.satisfies(maxRole, role)) {
				roleValues.add (role);
			}
		}
		return roleValues; */
		return Roles.getSatisfyingRoles(maxRole);
	}


	/**
	 *  Gets the actionPath attribute of the AccessManager object
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       The actionPath value
	 */
	public ActionPath getActionPath(String path) {
		return (ActionPath) actionPathMap.get(path);
	}


	/**
	 *  Gets the guardedPath attribute of the AccessManager object
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       The guardedPath value
	 */
	public GuardedPath getGuardedPath(String path) {
		return (GuardedPath) guardedPathMap.get(path);
	}


	/**
	 *  Gets the actionPaths attribute of the AccessManager object
	 *
	 * @return    The actionPaths value
	 */
	public List getActionPaths() {
		List actionPaths = new ArrayList();
		for (Iterator i = actionPathMap.keySet().iterator(); i.hasNext(); ) {
			String path = (String) i.next();
			actionPaths.add(getActionPath(path));
		}
		return actionPaths;
	}


	/**
	 *  Gets the guardedPaths attribute of the AccessManager object
	 *
	 * @return    The guardedPaths value
	 */
	public List getGuardedPaths() {
		List guardedPaths = new ArrayList();
		for (Iterator i = guardedPathMap.keySet().iterator(); i.hasNext(); ) {
			String path = (String) i.next();
			guardedPaths.add(getGuardedPath(path));
		}
		return guardedPaths;
	}


	/**
	 *  For each action-path defined in the configuration file, update the existing
	 *  ActionPath instance with description and roles.
	 */
	private void configureActionPaths() {
		// prtln ("configureActionPaths()");
		List actionPathNodes = getNodes("/paths/action-paths/action-path");
		// prtln ("\t processing " + actionPathNodes.size() + " actionPathNodes");
		for (Iterator i = actionPathNodes.iterator(); i.hasNext(); ) {
			Element ape = (Element) i.next();
			String path = this.getChildText(ape, "path");
			// prtln ("\t\t path: " + path);

			ActionPath ap = getActionPath(path);
			if (ap == null) {
				prtlnErr("configureActionPaths WARNING: ActionPath not found for \"" + path + "\"");
				continue;
			}
			// prtln ("\t\t description: " + ape.element("description").getText());
			ap.setDescription(getChildText(ape, "description"));
			// prtln (Dom4jUtils.prettyPrint (ap.asElement()));
		}
	}


	/**  NOT YET DOCUMENTED */
	private void configureGuardedPaths() {
		prtln("configureGuardedPaths()");
		guardedPathMap = new TreeMap();

		List pathNodes = getNodes("/paths/guarded-paths/guarded-path");
		prtln("\t processing " + pathNodes.size() + " guardedPathNodes");
		for (Iterator i = pathNodes.iterator(); i.hasNext(); ) {
			Element r = (Element) i.next();
			String path = getChildText(r, "path");
			UrlPatternMatcher matcher = new UrlPatternMatcher(path);
			if (matcher.isEmpty())
				continue;
			GuardedPath gp = new GuardedPath(matcher);
			gp.setDescription(getChildText(r, "description"));
			String roleStr = getChildText(r, "role");
			gp.setRole(Roles.toRole(roleStr));
			guardedPathMap.put(path, gp);
			// prtln (Dom4jUtils.prettyPrint (ap.asElement()));
		}
	}


	/**
	 *  Adds a feature to the GuardedPath attribute of the AccessManager object
	 *
	 * @param  gp  The feature to be added to the GuardedPath attribute
	 */
	public synchronized void addGuardedPath(GuardedPath gp) {
		guardedPathMap.put(gp.getPath(), gp);
		alignActionsToGuardedPaths();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  gp  NOT YET DOCUMENTED
	 */
	public synchronized void deleteGuardedPath(GuardedPath gp) {
		guardedPathMap.remove(gp.getPath());
		alignActionsToGuardedPaths();
	}


	/**  NOT YET DOCUMENTED */
	public synchronized void alignActionsToGuardedPaths() {
		prtln("alignActionsToGuardedPaths");
		Map map = new HashMap();
		List unguardedActions = new ArrayList();
		for (Iterator i = getActionPaths().iterator(); i.hasNext(); ) {
			ActionPath ap = (ActionPath) i.next();
			GuardedPath gp = this.matchGuardedPath(ap.getPath() + ".do");
			if (gp == null) {
				prtlnErr("no guardedPath found for: " + ap.getPath());
				ap.setRole(Roles.NO_ROLE);
				unguardedActions.add(ap);
				continue;
			}
			String key = gp.getPath();
			List apList = map.containsKey(key) ? (List) map.get(key) : new ArrayList();
			ap.setRole(gp.getRole());
			apList.add(ap);
			map.put(key, apList);
		}

		for (Iterator i = getGuardedPaths().iterator(); i.hasNext(); ) {
			GuardedPath gp = (GuardedPath) i.next();
			gp.setActions((List) map.get(gp.getPath()));
		}

		if (!unguardedActions.isEmpty()) {
			GuardedPath defaultPath = new GuardedPath("/*");
			defaultPath.setDescription("Default path - houses unguarded Actions");
			defaultPath.setActions(unguardedActions);
			addGuardedPath(defaultPath);
		}
	}


	/**
	 *  Update DocMap with recent ActionPath and GuardedPaths, and then write it to
	 *  disk.
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public synchronized void flush() throws Exception {
		Element root = df.createElement(rootElementName);
		Document doc = df.createDocument(root);
		try {
			Element guardedPaths = root.addElement("guarded-paths");
			for (Iterator i = guardedPathMap.values().iterator(); i.hasNext(); ) {
				GuardedPath gp = (GuardedPath) i.next();
				guardedPaths.add(gp.asElement());
			}
			Element actionpaths = root.addElement("action-paths");
			for (Iterator i = actionPathMap.values().iterator(); i.hasNext(); ) {
				ActionPath ap = (ActionPath) i.next();
				actionpaths.add(ap.asElement());
			}
			docMap = new DocMap(doc);
		} catch (Throwable t) {
			prtln("flush error: " + t.getMessage());
			t.printStackTrace();
			throw new Exception(t.getCause());
		}
		super.flush();
	}


	/**
	 *  The main program for the AccessManager class
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("Hello World");
		// String path = "/Users/ostwald/Devel/projects/roles/web/WEB-INF/roles-config.xml";
		String path = "/devel/ostwald/projects/roles/web/WEB-INF/actionPaths_config.xml";

		AccessManager rr = new AccessManager(new File(path), null);
		prtln(Dom4jUtils.prettyPrint(rr.getDocMap().getDocument()));
	}


	/**
	 *  Gets the roleForPath attribute of the AccessManager object
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       The roleForPath value
	 */
	public Roles.Role getRoleForPath(String path) {
		return matchGuardedPath(path).getRole();
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  path  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	public GuardedPath matchGuardedPath(String path) {
		// prtln ("\n matchGuardedPath with path: \"" + path + "\"");
		List matches = new ArrayList();
		Iterator gps = this.getGuardedPaths().iterator();
		while (gps.hasNext()) {
			GuardedPath gp = (GuardedPath) gps.next();
			/* UrlPatternMatcher pm = (UrlPatternMatcher)pms.next(); */
			// prtln ("-- " + gp.getPath());
			// return exact matches
			if (gp.getPath().equals(path)) {
				// prtln ("\t exact match");
				return gp;
			}

			// prtln ("matching against " + pm.urlPattern);
			if (gp.match(path)) {
				// prtln ("\t a candidate!");
				matches.add(gp);
			}
			else {
				// prtln ("\t nope");
			}
		}
		// prtln (" found " + matches.size() + " matches");
		return getBestMatch(matches);
	}


	/**
	 *  Gets the bestMatch attribute of the AccessManager object
	 *
	 * @param  matches  NOT YET DOCUMENTED
	 * @return          The bestMatch value
	 */
	GuardedPath getBestMatch(List matches) {
		// prtln ("\nbestMatch");
		int longest = -1;
		GuardedPath bestMatch = null;
		for (Iterator i = matches.iterator(); i.hasNext(); ) {
			GuardedPath gp = (GuardedPath) i.next();
			String path = gp.getPath();
			if ("/*".equals(path))
				path = "/";
			int length = path.split("/").length;
			// prtln ("\t" + path + "(" + path.split("/").length + ")");
			if (length > longest) {
				bestMatch = gp;
				longest = length;
			}
		}
		// prtln ("\t returning " + bestMatch);
		return bestMatch;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	protected static void prtln(String s) {
		if (debug) {
			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}
			System.out.println("AccessManager: " + s);
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	static void prtlnErr(String s) {
		System.out.println("AccessManager: " + s);
	}

}

