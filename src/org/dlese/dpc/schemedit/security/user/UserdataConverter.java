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
package org.dlese.dpc.schemedit.security.user;

import java.io.File;
import java.util.*;

import org.dom4j.*;

import org.dlese.dpc.xml.Dom4jUtils;

import org.dlese.dpc.schemedit.SchemEditUtils;

/**
 *  Converts user.xml files to a directory of username.xml records<p>
 Up to ncs version 2.7.10, all user data was stored in a single "user.xml" file.
 Release 2.7.10 stores user data in individual files. This class is responsible for<ul>
 <li>read a user.xml file, parse into user-based structures (storing info for a single user)
 <li>create a subdirectory in the parent of user.xml
 <li>write user data to individual files within the new user direcotry (named by username)
 <li>rename user.xml to user.xml.imported
 <li>
 *
 *@author    Jonathan Ostwald
 */
public class UserdataConverter {

	static boolean debug = true;

	File usersXmlFile = null;
	File usersDirectory = null;


	public static void convert (File usersXmlFile) throws Exception {
		try {
			new UserdataConverter (usersXmlFile);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new Exception ("convert error: " + t.getMessage());
		}
	}
	
	public static void convert (String usersXmlPath) throws Exception {
		new UserdataConverter (usersXmlPath);
	}
	
	
	/**
	 *  Constructor for the UserdataConverter object
	 *
	 *@param  source         NOT YET DOCUMENTED
	 *@exception  Exception  NOT YET DOCUMENTED
	 */
	private UserdataConverter(String usersXmlPath) throws Exception {
		this (new File (usersXmlPath));
	}

	
	private UserdataConverter(File  usersXmlFile) throws Exception {
		this.usersXmlFile = usersXmlFile;
		prtln ("\nUserdataConverter converting " + usersXmlFile);
		if (!usersXmlFile.exists())
			throw new Exception ("file does not exist at " + usersXmlFile);
		usersDirectory = this.getUsersDirectory ();
		
		Document usersXmlDoc = Dom4jUtils.getXmlDocument(usersXmlFile);
		// usersXmlDoc = Dom4jUtils.localizeXml(usersXmlDoc);
		// pp (doc);
		List userElements = usersXmlDoc.selectNodes ("/users/user");
		prtln (userElements.size() + " users found");
		int cnt = 0;
		for (Iterator i=userElements.iterator();i.hasNext();) {
			Element userElement = (Element) i.next();
			try {
				String username = userElement.attributeValue("username", "").trim();
				if (username.length() == 0)
					throw new Exception ("user name not found");
				Document doc = makeUserDocument (userElement);
				// prtln ("\n" + username);
				// pp (doc);
				
				File newFile = new File (this.usersDirectory, username+".xml");
				Dom4jUtils.writeDocToFile(doc, newFile);
				prtln ("... wrote " + newFile.getName());
			} catch (Exception e) {
				prtln ("Make User ERROR: " + e.getMessage());
			}
			if (++cnt > 100)
				break;
		}
		
		// rename the user.xml file
		File newUsersXmlFile = new File (usersXmlFile.getParentFile(), "users.xml.imported");
		usersXmlFile.renameTo(newUsersXmlFile);
		prtln ("\nRenamed \"user.xml\" as \"" + newUsersXmlFile.getName() + "\"");
		
		prtln ("User Data Converted\n");
	}
	
	Document makeUserDocument (Element userElement) {
		Element root = DocumentHelper.createElement("record");

		 
		root.addAttribute(
			"xmlns:xsi",
			"http://www.w3.org/2001/XMLSchema-instance");
			
		root.addAttribute(
			"xsi:noNamespaceSchemaLocation",
			"http://www.dlese.org/Metadata/dcs/ncs_user/ncsUserFramework.xsd");
		
		root.addElement ("username").
			setText (userElement.attributeValue("username", ""));
		String isAdminUser =  userElement.attributeValue("adminUser", "");
		if (!"true".equals(isAdminUser))
			isAdminUser = "false";
		root.addElement ("isAdminUser").
			setText (isAdminUser);
			
		Element general = root.addElement ("general");
			
		general.addElement ("firstname").
			setText (userElement.attributeValue("firstname", ""));
		general.addElement ("lastname").
			setText (userElement.attributeValue("lastname", ""));
		general.addElement ("department").
			setText (userElement.attributeValue("department", ""));
		general.addElement ("institution").
			setText (userElement.attributeValue("institution", ""));		
		general.addElement ("email").
			setText (userElement.attributeValue("email", ""));	
			
		Element rolesElement = getRolesElement (userElement);
		if (rolesElement != null)
			root.add (rolesElement);
		return DocumentHelper.createDocument(root);
	}
		
	Element getRolesElement (Element userElement) {
		
		Element rolesElement = DocumentHelper.createElement("roles");
		// prtln ("\ngetRolesElement");
		
		String path = "roles/role";
		Iterator i=userElement.selectNodes(path).iterator();
		while (i.hasNext()) {
			Element roleData = (Element)i.next();
			String rolename = roleData.getTextTrim();
			String collection = roleData.attributeValue("collection");
			// prtln ("role: " + rolename + ",  collection: " + collection);

			// now attach a new roleElement
			Element roleElement = rolesElement.addElement("role");
			roleElement.addElement("rolename").setText(rolename);
			roleElement.addElement("collection").setText(collection);	
		}
		
		if (rolesElement.elements().size() > 0)
			return rolesElement;
		else
			return null;
	}
	
	void makeChildElement (Element parent, String tagName, String value) {
		parent.addElement(tagName).setText(value);
	}
	
	

	File getUsersDirectory () throws Exception {
		File parent = usersXmlFile.getParentFile().getParentFile();
		if (!parent.exists())
			throw new Exception ("couldnt find parent at: " + parent);
		File usersDir = new File (parent, "users");
		if (!usersDir.exists())
			usersDir.mkdir();
		else if (usersDir.isFile())
			throw new Exception ("users exists as a file at " + usersDir);
		return usersDir;
	}
	
	
	
	/**
	 *  The main program for the UserdataConverter class
	 *
	 *@param  args           The command line arguments
	 *@exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {
		org.dlese.dpc.schemedit.test.TesterUtils.setSystemProps();
		String userXmlPath = "/Users/ostwald/tmp/auth/users.xml";
		UserdataConverter udc = null;
		try {
			// udc = new UserdataConverter (userXmlPath);
			UserdataConverter.convert (userXmlPath);
		} catch (Exception e) {
			prtln ("Converter caught an Exception; " + e.getMessage());
		} catch (Throwable t) {
			prtln ("Converter had unknown problem: " + t.getMessage());
			t.printStackTrace();
		}


	}

	private void pp (Node node) {
		prtln (Dom4jUtils.prettyPrint(node));
	}
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@param  s  NOT YET DOCUMENTED
	 */
	protected static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "");
			// SchemEditUtils.prtln(s, "convtr");
		}
	}

}

