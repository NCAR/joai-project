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
package org.dlese.dpc.schemedit.url;

import org.dlese.dpc.schemedit.url.DupSim;
import org.dlese.dpc.schemedit.Constants;
import org.dlese.dpc.schemedit.FrameworkRegistry;
import org.dlese.dpc.schemedit.MetaDataFramework;
import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.schemedit.config.*;
import org.dlese.dpc.repository.RepositoryManager;
import org.dlese.dpc.index.SimpleLuceneIndex;
import org.dlese.dpc.index.ResultDoc;
import org.dlese.dpc.index.ResultDocList;
import org.dlese.dpc.index.reader.XMLDocReader;
import org.dlese.dpc.index.reader.NewsOppsDocReader;
import org.dlese.dpc.index.reader.ItemDocReader;

import java.util.*;
import javax.servlet.ServletContext;
import org.dom4j.Document;

/**
 *  Checks for duplicate and similar urls within a specific collection.
 *
 *@author     ostwald <p>
 *
 *
 *@created    March 19, 2009
 */
public class DupSimUrlChecker {

	static boolean debug = false;
	String referenceUrl;
	String collection;
	ServletContext servletContext;
	RepositoryManager rm = null;
	SimpleLuceneIndex index = null;

	static int SEARCH_LEVELS = 3;
	static int MAX_DELTA = 3;


	/**
	 *  Constructor for the DupSimUrlChecker object, requiring ServletContext.
	 *
	 *@param  servletContext
	 *@param  referenceUrl    The url for which we look for duplicates and similar
	 *      urls
	 *@param  collection      The collection we look in
	 *@exception  Exception   if required helper objects cannot be found in the
	 *      servlet context initialized.
	 */
	public DupSimUrlChecker(String referenceUrl, String collection, ServletContext servletContext) throws Exception {
		this.referenceUrl = referenceUrl;
		this.collection = collection;
		this.servletContext = servletContext;
		rm = (RepositoryManager) servletContext.getAttribute("repositoryManager");
		if (rm == null) {
			throw new Exception("RepositoryManger not found");
		}

		index = rm.getIndex();
		if (index == null) {
			throw new Exception("Index not found");
		}
	}


	// ======================================================

	/**
	 *  Returns a list records in "collection" that contain either a PrimaryURL or
	 *  MirrorURL that is a <b>dupliate</b> of the provided url.
	 *
	 *@return    List of DupSim instances
	 */
	public List getDups() {
		prtln("\ngetDups");
		List dups = new ArrayList();

		dups.addAll(getPrimaryDups());
		dups.addAll(getMirrorDups());
		if (dups.size() > 1) {
			DupSim first = (DupSim) dups.get(0);
			Collections.sort(dups, first.getComparator());
		}
		// prtln ("   ... getDups returning " + dups.size() + " items");
		return dups;
	}


	/**
	 *  Returns a list of DupSim instances that represent records in "collection"
	 *  that contain either a PrimaryURL or MirrorURL <b>similar</b> to the
	 *  provided url.
	 *
	 *@return    List of DupSim instances
	 */
	public List getSims() {
		prtln("getSims ...");
		List sims = new ArrayList();

		sims.addAll(getPrimarySims());
		sims.addAll(getMirrorSims());

		if (sims.size() > 1) {
			DupSim first = (DupSim) sims.get(0);
			Collections.sort(sims, first.getComparator());
		}
		prtln(" ... returning " + sims.size() + " sims");
		return sims;
	}


	// ============== Private Helper methods ========================


	/**
	 *  Returns a list records in "collection" that contain a PrimaryURL that is a
	 *  <b>dupliate</b> of the provided url.<p>
	 *
	 *  NOTE: A url will only be indexed if the framework configuration defines a
	 *  schemaPath named "url".
	 *
	 *@return    List of DupSim instances
	 */
	private List getPrimaryDups() {
		prtln("getPrimaryDups ... ");
		List dups = new ArrayList();
		SimpleLuceneIndex index = rm.getIndex();
		if (index == null) {
			// prtln("WARNING: getPrimaryDups() index not found ... returning emtpy list");
			return dups;
		}
		String query = "((collection:0*) AND collection:0" + this.collection + ") AND ";

		String urlenc = SimpleLuceneIndex.encodeToTerm(this.referenceUrl);
		query += SchemEditUtils.formatQuery("urlenc:" + urlenc);

		prtln("dup url query: " + query);

		// search for results having the specified url. This record has not been indexed yet,
		// so any results are duplicates.
		ResultDocList results = index.searchDocs(query);
		prtln(results.size() + " results found");
		if (results != null) {
			for (int i = 0; i < results.size(); i++) {
				XMLDocReader docReader = (XMLDocReader) ((ResultDoc)results.get(i)).getDocReader();
				String url = getPrimaryUrl(docReader);
				// dups.add(new DupSim(results[i], "primary", this));
				dups.add(new DupSim(docReader.getId(), url, "sim", "primary", docReader.getNativeFormat()));
			}
		}
		prtln("  ...returning " + dups.size() + " dups");
		return dups;
	}


	/**
	 *  Returns a list DupSim instances that represent records in "collection" that
	 *  contain a MirrorURL that is a <b>duplicate</b> of the provided url.
	 *
	 *@return    List of DupSim instances
	 */
	private List getMirrorDups() {
		// prtln ("getMirrorDups ...");
		List mirs = new ArrayList();

		String urlEnc = SimpleLuceneIndex.encodeToTerm(this.referenceUrl);
		String query = "((collection:0*) AND collection:0" + this.collection + ") AND ";
		query += SchemEditUtils.formatQuery("urlMirrorsEncoded:" + urlEnc);
		// prtln("mirror url query: " + query);
		ResultDocList results = index.searchDocs(query);
		mirs = makeMirrorDupSims(results);
		// prtln ("  ... returning " + mirs.size() + " mirs");
		return mirs;
	}


	/**
	 *  Returns a list records in "collection" that contain a PrimaryURL <b>similar
	 *  </b> to the provided url.
	 *
	 *@return    List of DupSim instances
	 */
	private List getPrimarySims() {
		// prtln ("getPrimarySims ...");
		String similarUrlPath = UrlHelper.getSimilarUrlPath(this.referenceUrl, SEARCH_LEVELS);

		String simUrlEnc = SimpleLuceneIndex.encodeToTerm(similarUrlPath, false);

		String query = "((collection:0*) AND collection:0" + this.collection + ") AND ";
		query += SchemEditUtils.formatQuery("urlenc:" + simUrlEnc);

		prtln("url used to check for similar items: " + similarUrlPath);

		ResultDocList results = index.searchDocs(query);
		List sims = new ArrayList();
		if (results != null) {
			// prtln (results.length + " found for " + SEARCH_LEVELS + "-level search");

			// prune for similar urls
			for (int i = 0; i < results.size(); i++) {
				try {
					XMLDocReader docReader = (XMLDocReader) ((ResultDoc)results.get(i)).getDocReader();
					String url = getPrimaryUrl(docReader);
					// String s = (String) results[i].getDocMap().get("url");
					if (UrlHelper.isSimilar(url, this.referenceUrl, MAX_DELTA)) {
						String id = docReader.getId();
						String type = "sim";
						String field = "primary";
						String xmlFormat = docReader.getNativeFormat();

						// sims.add(new DupSim(results[i], "primary", this));
						sims.add(new DupSim(id, url, type, "primary", xmlFormat));
					} else {
						// prtln ("pruning " + s + "-- exceeds max_delta (" + MAX_DELTA + ")");
					}
				} catch (Throwable t) {
					prtln("pruning error: " + t.getMessage());
				}
			}
		}
		// prtln (" ... returning " + sims.size() + " sims");
		return sims;
	}


	/**
	 *  Returns a list records in "collection" that contain a MirrorURL <b>similar
	 *  </b> to the provided url.
	 *
	 *@return    List of DupSim instances
	 */
	private List getMirrorSims() {
		String similarUrlPath = UrlHelper.getSimilarUrlPath(this.referenceUrl, SEARCH_LEVELS);

		String urlEnc = SimpleLuceneIndex.encodeToTerm(similarUrlPath, false);
		String query = "((collection:0*) AND collection:0" + collection + ") AND ";
		query += SchemEditUtils.formatQuery("urlMirrorsEncoded:" + urlEnc);
		// prtln("gitMirrorSims query: " + query);

		ResultDocList results = rm.getIndex().searchDocs(query);
		List allMirrors = makeMirrorDupSims(results);
		List mirrors = new ArrayList();
		// prtln (allMirrors.size() + " candidates before pruning");
		for (Iterator i = allMirrors.iterator(); i.hasNext(); ) {
			DupSim dupSim = (DupSim) i.next();
			try {
				String url = (String) dupSim.getUrl();
				if (UrlHelper.isSimilar(url, this.referenceUrl, MAX_DELTA)) {
					mirrors.add(dupSim);
				} else {
					// prtln ("pruning " + s + "-- exceeds max_delta");
				}
			} catch (Throwable t) {
				prtln("pruning error: " + t.getMessage());
			}
		}
		// prtln ("getMirrorSims returning " + mirrors.size() + " items");
		return mirrors;
	}


	/**
	 *  Create a list of DupSim instances representing each of the MirrorUrls for
	 *  each of the provided ResultDocs.
	 *
	 *@param  results  An array of ResultDocs from which to extract the MirrorUrl
	 *      values
	 *@return          A list of DupSims containing all the MirrorUrls found in
	 *      results.
	 */
	public static List makeMirrorDupSims(ResultDocList results) {
		List list = new ArrayList();
		if (results != null) {
			// prtln ("makeMirrorDupSims with " + results.length + " results");
			for (int i = 0; i < results.size(); i++) {
				XMLDocReader docReader = (XMLDocReader) ((ResultDoc)results.get(i)).getDocReader();
				if (docReader instanceof ItemDocReader) {
					List mirrorUrls = SchemEditUtils.getMirrorUrls(docReader.getXmlLocalized());
					if (mirrorUrls != null && mirrorUrls.size() > 0) {
						String id = docReader.getId();
						String xmlFormat = docReader.getNativeFormat();
						String type = "dup";
						String field = "mirror";

						for (Iterator u = mirrorUrls.iterator(); u.hasNext(); ) {
							String mirrorUrl = (String) u.next();
							DupSim dupSim = new DupSim(id, mirrorUrl, type, field, xmlFormat);
							list.add(dupSim);
						}
					}
				}
			}
		}
		return list;
	}


	// ====================== utilities ===================================

	/**
	 *  Extracts the primaryUrl from a ResultDoc instance, handling "adn" and
	 *  "news_opps" frameworks
	 *
	 *@param  xmlDocReader  NOT YET DOCUMENTED
	 *@return               The primaryUrl, or an empty string if primaryUrl not
	 *      found
	 */
	private String getPrimaryUrl(XMLDocReader xmlDocReader) {
		String url = null;

		String xmlFormat = xmlDocReader.getNativeFormat();
		// prtln ("getPrimaryUrl() format = " + xmlFormat);
		if (xmlFormat.equals("news_opps")) {
			NewsOppsDocReader docReader = (NewsOppsDocReader) xmlDocReader;
			url = docReader.getAnnouncementUrl();
		} else if (xmlFormat.equals("adn")) {
			ItemDocReader docReader = (ItemDocReader) xmlDocReader;
			url = docReader.getUrl();
		} else {
			try {
				MetaDataFramework framework = getMetaDataFramework(xmlFormat);
				url = framework.getRecordUrl(xmlDocReader.getXmlDoc());
			} catch (Throwable t) {
				prtln("WARNING: getPrimaryUrl not found for " + xmlDocReader.getId() + ": " + t.getMessage());
				url = "";
			}
		}
		//prtln (" .. returning " + url);
		return url;
	}



	/*
	 *  =====================================================
	 *  Utilities to get servlet context attributes
	 */
	/**
	 *  Gets the collectionRegistry attribute of the DupSimUrlChecker class
	 *
	 *@return                The collectionRegistry value
	 *@exception  Exception  NOT YET DOCUMENTED
	 */
	private CollectionRegistry getCollectionRegistry() throws Exception {
		CollectionRegistry cr =
				(CollectionRegistry) servletContext.getAttribute("collectionRegistry");
		if (cr == null) {
			throw new Exception("CollectionRegistry not found");
		}
		return cr;
	}


	/**
	 *  Gets the frameworkRegistry attribute of the DupSimUrlChecker class
	 *
	 *@return                The frameworkRegistry value
	 *@exception  Exception  NOT YET DOCUMENTED
	 */
	private FrameworkRegistry getFrameworkRegistry() throws Exception {
		FrameworkRegistry fr =
				(FrameworkRegistry) servletContext.getAttribute("frameworkRegistry");
		if (fr == null) {
			throw new Exception("FrameworkRegistry not found in servlet context");
		}
		return fr;
	}


	/**
	 *  Gets the metaDataFramework associated with given xmlFormat.
	 *
	 *@param  xmlFormat      for example, "adn"
	 *@return                The metaDataFramework value
	 *@exception  Exception  NOT YET DOCUMENTED
	 */
	private MetaDataFramework getMetaDataFramework(String xmlFormat) throws Exception {
		FrameworkRegistry fr = getFrameworkRegistry();
		MetaDataFramework framework = fr.getFramework(xmlFormat);
		if (framework == null) {
			throw new Exception("framework not found for: " + xmlFormat);
		}
		return framework;
	}


	// -----------------------------------------------------------------------

	/**
	 *  Gets the dateString attribute of the DupSimUrlChecker class
	 *
	 *@return    The dateString value
	 */
	public static String getDateString() {
		return SchemEditUtils.fullDateString(new Date());
	}


	/**
	 *  Print a line to standard out.
	 *
	 *@param  s  The String to print.
	 */
	static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "DupSimUrlChecker");
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 *@param  s  NOT YET DOCUMENTED
	 */
	static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "DupSimUrlChecker");
	}
}

