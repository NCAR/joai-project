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
package org.dlese.dpc.schemedit.test;

import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.xml.schema.SchemaHelper;
import org.dlese.dpc.xml.schema.SchemaNodeMap;
import org.dlese.dpc.xml.schema.SchemaNode;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.autoform.RendererHelper;
import org.dlese.dpc.schemedit.display.CollapseUtils;
import java.util.*;
import java.util.regex.*;
import java.io.File;

/**
 *  Utilities for manipulating XPaths, represented as String
 *
 * @author     ostwald
 * @version    $Id: SchemaPathSorter.java,v 1.3 2009/03/20 23:33:58 jweather Exp $
 */
public class SchemaPathSorter {

	private static boolean debug = true;
	
	String xmlFormat;
	MetaDataFramework framework;
	SchemaHelper sh;
	List pool;
	Comparator schemaOrder;

	public SchemaPathSorter (String xmlFormat) throws Exception {
		this.xmlFormat = xmlFormat;
		String configFileDir = TesterUtils.getFrameworkConfigDir();
		String docRoot = TesterUtils.getDocRoot();

		// make sure the prop file really exists
		File configFile = new File(configFileDir, xmlFormat + ".xml");
		String configFilePath = configFile.toString();
		if (!configFile.exists()) {
			prtln("propfile doesn't exist at " + configFilePath);
			return;
		}
		else {
			framework = new MetaDataFramework(configFilePath, docRoot);
		}

		try {
			framework.loadSchemaHelper();
		} catch (Exception e) {
			prtln("failed to instantiate SchemaHelper: " + e);
			return;
		}
		sh = framework.getSchemaHelper();
		prtln ("RenderTester instantiated\n");
		
		SchemaNodeMap schemaNodeMap = sh.getSchemaNodeMap();
		schemaOrder = schemaNodeMap.new DocOrderComparator();
		
		pool = makePool();
	}
	
	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  name   NOT YET DOCUMENTED
	 * @param  array  NOT YET DOCUMENTED
	 */
	public static void prtList(List list) {
		prtln("List: (" + list.size() + ")");
		for (Iterator i=list.iterator();i.hasNext(); ) {
			prtln ("\t" + (String)i.next());
		}
	}

	static String[] pathPool = {
		"_slash_news_dash_oppsRecord_slash_contributors_slash_contributor_2__slash_organization_slash_instName",
		"_slash_news_dash_oppsRecord_slash_announcementURL",
		"_slash_news_dash_oppsRecord_slash_contributors_slash_contributor_1__slash_organization_slash_instName",
		};

	static String[] pathPoolFOO = {
		"_slash_news_dash_oppsRecord_slash_announcementURL",
		"_slash_news_dash_oppsRecord_slash_announcements_slash_announcement",
		"_slash_news_dash_oppsRecord_slash_audiences_slash_audience",
		"_slash_news_dash_oppsRecord_slash_contributors_slash_contributor_1__slash_organization_slash_instName",
		"_slash_news_dash_oppsRecord_slash_contributors_slash_contributor_2__slash_@role",
		"_slash_news_dash_oppsRecord_slash_contributors_slash_contributor_2__slash_organization_slash_instName",
		"_slash_news_dash_oppsRecord_slash_description",
		"_slash_news_dash_oppsRecord_slash_language_slash_@meta",
		"_slash_news_dash_oppsRecord_slash_language_slash_@resource",
		"_slash_news_dash_oppsRecord_slash_title"
		};


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  urls  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	List makePool() {
		pool = new ArrayList();
		for (int i = 0; i < pathPool.length; i++) {
			pool.add(pathPool[i]);
		}
		return pool;
	}

	List getSortedPool () {
		List sortedPool = pool;
		Collections.sort (sortedPool, new DocOrderComparator());
		return sortedPool;
	}
	
	/**
	 *  Read in a framework (we need the schemaNode map)
	 *
	 * @param  args           The command line arguments
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public static void main(String[] args) throws Exception {

		prtln ("hello from SchemaPathSorter");
		SchemaPathSorter sorter = new SchemaPathSorter ("news_opps");
		
		idMunger ((String)sorter.pool.get(2));
		
		prtList (sorter.pool);
		List sortedPool = sorter.getSortedPool();
		prtList (sortedPool);

	}

	static void idMunger (String id) {
		prtln (id);
		String encodedPath = CollapseUtils.idToPath(id);
		prtln ("encoded: " + encodedPath);
		prtln ("decoded: " + XPathUtils.decodeXPath(encodedPath));
		prtln ("schemaPath: " + SchemaHelper.toSchemaPath(encodedPath));
	}
	
	public class DocOrderComparator implements Comparator {
		/**
		* Compare two paths segment by segment.
		- if a segment only differs by index (e.g., [1] vs [2], then make comparison based on the index
		- if segments are different leafNames, then return a 0 so the paths will be compared by schemaPath.
		*/
		private int pathCmp (String p1, String p2) {
			String [] s1 = p1.split ("/");
			String [] s2 = p2.split ("/");
			int cmp;
			for (int i=0;i<s1.length && i<s2.length;i++) {
				String seg1 = s1[i];
				String seg2 = s2[i];
				prtln ("\tseg1: " + seg1);
				prtln ("\tseg2: " + seg2);
				String name1 = XPathUtils.getNodeName(seg1);
				String name2 = XPathUtils.getNodeName(seg2);
				if (name1.equals(name2)) {
					int i1 = XPathUtils.getIndex(seg1);
					int i2 = XPathUtils.getIndex(seg2);
					if (i1 < i2) {
						prtln ("\t\t" + i1 + " < " + i2);
						return -1;
					}
					if (i1 > i2) {
						prtln ("\t\t" + i1 + " > " + i2);
						return 1;
					}
					prtln ("\t\t ... indexes are the same");
				}
				else {
					// the segment names were different, let schemaOrder compare
					return 0;
				}
			}
			// we haven't resolved order, let schemaOrder compare
			return 0;	
		}
			
		
		/**
		 *  sorts by order in which paths are processed by StructureWalker (and therefore are added to the
		 *  SchemaNodeMap)
		 *
		 * @param  o1  NOT YET DOCUMENTED
		 * @param  o2  NOT YET DOCUMENTED
		 * @return     NOT YET DOCUMENTED
		 */
		public int compare(Object o1, Object o2) {

			String id1 = (String) o1;
			String id2 = (String) o2;
			prtln ("\nCOMPARE with: \n\t 1 - " + id1 + "\n\t 2 - " + id2);

			String path1 = XPathUtils.decodeXPath(CollapseUtils.idToPath(id1));
			String path2 = XPathUtils.decodeXPath(CollapseUtils.idToPath(id2));
			
			prtln ("paths: \n\t 1 - " + path1 + "\n\t 2 - " + path2);
			
			int cmp = pathCmp (path1, path2);
			prtln ("\t pathCmp returned " + cmp);
			if (cmp != 0) {
				return cmp;
			}
			
			cmp = schemaOrder.compare(SchemaHelper.toSchemaPath(path1),
									  SchemaHelper.toSchemaPath(path2));


			
			prtln ("\t ... schemaOrder.compare: " + cmp);
			
			return cmp;

		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println(s);
		}
	}


}

