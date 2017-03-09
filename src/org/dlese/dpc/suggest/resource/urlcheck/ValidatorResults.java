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
package org.dlese.dpc.suggest.resource.urlcheck;

import org.dlese.dpc.serviceclients.remotesearch.SearchServiceClient;
import org.dlese.dpc.schemedit.url.DupSim;
// import org.dlese.dpc.schemedit.*;
import java.util.*;

/**
 *  Holds results of a search over a repository or a collection within a repository for similar and/or duplicate records. 
 Results are represented as DupSim instances.
 *
 *@author    ostwald<p>
 *
 *      $Id $
 */
public class ValidatorResults {

	private static boolean debug = true;

	private DupSim duplicate = null;
	private List similarMirrorUrls = null;
	private List similarPrimaryUrls = null;


	/**
	 *  Constructor for the ValidatorResults object
	 */
	ValidatorResults() {
		similarMirrorUrls = new ArrayList();
		similarPrimaryUrls = new ArrayList();
	}


	/**
	 *  Adds a DupSim instance to the SimilarPrimaryUrl attribute of the ValidatorResults
	 *  object
	 *
	 *@param  sim  The feature to be added to the SimilarPrimaryUrl attribute
	 */
	protected void addSimilarPrimaryUrl(DupSim sim) {
		similarPrimaryUrls.add(sim);
	}


	/**
	 *  Adds a DupSim instance to the SimilarMirrorUrl attribute of the ValidatorResults
	 *  object
	 *
	 *@param  sim  The feature to be added to the SimilarMirrorUrl attribute
	 */
	protected void addSimilarMirrorUrl(DupSim sim) {
		similarMirrorUrls.add(sim);
	}


	/**
	 *  Returns true if a duplcate url (either primary or mirror) has been found.
	 *
	 *@return    Description of the Return Value
	 */
	public boolean hasDuplicate() {
		return !(duplicate == null);
	}


	/**
	 *  Gets the duplicate attribute of the ValidatorResults object
	 *
	 *@return    The duplicate value
	 */
	public DupSim getDuplicate() {
		return duplicate;
	}


	/**
	 *  Sets the duplicate attribute of the ValidatorResults object
	 *
	 *@param  dup  The new duplicate value
	 */
	protected void setDuplicate(DupSim dup) {
		duplicate = dup;
	}


	/**
	 *  Gets the similarMirrorUrls attribute of the ValidatorResults object
	 *
	 *@return    The similarMirrorUrls value
	 */
	public List getSimilarMirrorUrls() {
		Collections.sort(similarMirrorUrls, new SimSorter());
		return similarMirrorUrls;
	}


	/**
	 *  Sets the similarMirrorUrls attribute of the ValidatorResults object
	 *
	 *@param  dupSims  The new similarMirrorUrls value
	 */
	protected void setSimilarMirrorUrls(List dupSims) {
		similarMirrorUrls = dupSims;
	}


	/**
	 *  Gets the similarPrimaryUrls attribute of the ValidatorResults object
	 *
	 *@return    The similarPrimaryUrls value
	 */
	public List getSimilarPrimaryUrls() {
		Collections.sort(similarPrimaryUrls, new SimSorter());
		return similarPrimaryUrls;
	}


	/**
	 *  Sets the similarPrimaryUrls attribute of the ValidatorResults object
	 *
	 *@param  dupSims  The new similarPrimaryUrls value
	 */
	protected void setSimilarPrimaryUrls(List dupSims) {
		similarPrimaryUrls = dupSims;
	}

	/**
	* Report the similar urls for inclusion in a dcsStatusNote
	*/
	public String similarUrlReportForDcsStatusNote () {
		String report = "";
		if (getSimilarPrimaryUrls().size() > 0) {
			report += "<p>Records having similar Primary Urls";
			for (Iterator i=getSimilarPrimaryUrls().iterator();i.hasNext();) {
				DupSim sim = (DupSim) i.next();
				report += "<BR>" + sim.getId();
			}
		}
		if (getSimilarMirrorUrls().size() > 0) {
			report += "<p>Records having similar Mirror Urls";
			for (Iterator i=getSimilarMirrorUrls().iterator();i.hasNext();) {
				DupSim sim = (DupSim) i.next();
				report += "<br>" + sim.getId();
			}
		}
		return report;
	}

	
	/**
	 *  Verbose version of reportSims for debugging purposes.
	 *
	 *@return    Description of the Return Value
	 */
	public String toString() {
		String s = "Validator Results:";

		s += "\n\n** Duplicate **\n------------\n";
		if (duplicate == null) {
			s += "none";
		}
		else {
			/* s += "\n" + duplicate.toString(); */
			s += showDupSim(duplicate);
			s += "\n\t(" + duplicate.getField() + ")";
		}

		s += "\n\n** Similar Primary Urls **\n------------\n";
		for (Iterator i = getSimilarPrimaryUrls().iterator(); i.hasNext(); ) {
			DupSim sim = (DupSim) i.next();
			/* s += "\n" + sim.toString(); */
			s += showDupSim(sim);
		}

		s += "\n\n** Similar Mirror Urls **\n------------\n";
		for (Iterator i = getSimilarMirrorUrls().iterator(); i.hasNext(); ) {
			DupSim sim = (DupSim) i.next();
			/* s += "\n" + sim.toString(); */
			s += showDupSim(sim);
		}

		return s;
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	public boolean hasSimilarUrls() {
		return (getSimilarPrimaryUrls().size() > 0 ||
				getSimilarMirrorUrls().size() > 0);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  d  Description of the Parameter
	 *@return    Description of the Return Value
	 */
	String showDupSim(DupSim d) {
		String s = "\n" + d.getId();
		s += "\n\t" + d.getUrl();
		return s;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of the Parameter
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("ValidatorResults: " + s);
		}
	}


}

