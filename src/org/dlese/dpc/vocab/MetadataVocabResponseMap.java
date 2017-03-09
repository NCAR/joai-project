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
package org.dlese.dpc.vocab;

import org.dlese.dpc.vocab.MetadataVocab;
import javax.servlet.jsp.PageContext;
import java.io.Serializable;
import java.util.HashMap;

/**
 *  Stores a list of metadata values that come from a services/indexer response,
 *  for reproducing with OPML-defined order/groupings/labels
 *
 * @author    Ryan Deardorff
 */
public class MetadataVocabResponseMap implements Serializable {

	public String audience = "";
	public String metaFormat = "";
	public String metaVersion = "";
	public String language = "";
	public String field = "";
	protected HashMap map = new HashMap();

	/**
	 *  Audience is a user group, i.e. "default", "community", or "cataloger"
	 *
	 * @param  audience     The new audience value
	 * @param  metaFormat
	 * @param  metaVersion
	 * @param  language
	 * @param  field
	 */
	public MetadataVocabResponseMap( String metaFormat, String metaVersion, String audience, String language, String field ) {
		this.metaFormat = metaFormat;
		this.metaVersion = metaVersion;
		this.audience = audience;
		this.language = language;
		this.field = field;
	}

	/**
	 *  Indicates a particular value in a list of response values
	 *
	 * @param  value  The new value value
	 */
	public void setValue( String value ) {
		map.put( value, new Boolean( true ) );
	}

	/**
	 *  Has the specified value been cached in the response?
	 *
	 * @param  value
	 * @return
	 */
	public boolean hasValue( String value ) {
		if ( value == null ) {
			return false;
		}
		if ( map.get( value ) != null ) {
			return true;
		}
		return false;
	}
}

