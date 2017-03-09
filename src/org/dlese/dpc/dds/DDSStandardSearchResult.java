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
package org.dlese.dpc.dds;

import org.dlese.dpc.index.*;
import org.apache.lucene.queryParser.ParseException;

/**
 *  Structure that holds the results of a standard DDS text/field search. This Object is returned by {@link
 *  org.dlese.dpc.dds.action.DDSQueryAction#ddsStandardQuery} and is used by {@link
 *  org.dlese.dpc.dds.action.DDSQueryAction} and {@link org.dlese.dpc.services.dds.action.DDSServicesAction}.
 *
 * @author    John Weatherley
 * @see       org.dlese.dpc.dds.action.DDSQueryAction
 * @see       org.dlese.dpc.services.dds.action.DDSServicesAction
 */
public final class DDSStandardSearchResult {
	private String forwardName = null;
	private ResultDocList results = null;
	private Exception ex = null;



	/**  Constructor for the DDSStandardSearchResult object */
	public DDSStandardSearchResult() { }


	/**
	 *  Constructor for the DDSStandardSearchResult object
	 *
	 * @param  results         The search results.
	 * @param  forwardName     The name that describes which page to forward to to render the results.
	 * @param  exception  		An Exception if one occured, or null
	 */
	public DDSStandardSearchResult(ResultDocList results, Exception exception, String forwardName) {
		ex = exception;
		this.results = results;
		this.forwardName = forwardName;
	}


	/**
	 *  Gets the search results.
	 *
	 * @return    The results value
	 */
	public ResultDocList getResults() {
		return results;
	}


	/**
	 *  Gets the Exception if one occured, or null if none.
	 *
	 * @return    An Exception or null
	 */
	public Exception getException() {
		return ex;
	}


	/**
	 *  Gets the name of the page to forward to, which is one of 'simple.query' or 'whats.new.query'.
	 *
	 * @return    The forwardName value
	 */
	public String getForwardName() {
		return forwardName;
	}
}



