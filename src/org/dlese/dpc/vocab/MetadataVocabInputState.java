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

import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.Serializable;

/**
 *  Stores user state for selected controlled vocabularies.
 *
 * @author    Ryan Deardorff
 */
public final class MetadataVocabInputState implements Serializable {
	private StringBuffer vocabQueryString = new StringBuffer( "" );// part of request query string that holds vocab settings
	private StringBuffer vocabFeedbackString = new StringBuffer( "" );// store "Your selections" feedback for no Javascript case

	/**
	 *  Constructor for the MetadataVocabInputState object.
	 */
	public MetadataVocabInputState() { }

	/**
	 *  Sets the state attribute of the MetadataVocabInputState object
	 *
	 * @param  req  The new state value
	 */
	public void setState( HttpServletRequest req ) {
		vocabQueryString.setLength( 0 );
		vocabFeedbackString.setLength( 0 );
		ServletContext servletContext = req.getSession().getServletContext();
		MetadataVocab vocab = (MetadataVocab)servletContext.getAttribute(
			(String)servletContext.getInitParameter( "metadataVocabInstanceAttributeName" ) );
		if ( vocab != null ) {
			ArrayList ids = vocab.getVocabFieldIds();
			for ( int i = 0; i < ids.size(); i++ ) {
				String[] vals = req.getParameterValues( (String)ids.get( i ) );
				if ( ( vals != null ) && ( vals.length > 0 ) && ( vals[0].length() > 0 ) ) {
					vocabFeedbackString.append( vocab.getUiFieldLabel(
						servletContext.getInitParameter( "metadataVocabAudience" ),
						servletContext.getInitParameter( "metadataVocabLanguage" ),
						(String)ids.get( i ), true ) + ": " );
					for ( int j = 0; j < vals.length; j++ ) {
						vocabQueryString.append( "&" + ids.get( i ) + "=" + vals[j] );
						vocabFeedbackString.append( vocab.getUiValueLabel(
							servletContext.getInitParameter( "metadataVocabAudience" ),
							servletContext.getInitParameter( "metadataVocabLanguage" ),
							(String)ids.get( i ), vals[j], true ) + ", " );
					}
					vocabFeedbackString.setLength( vocabFeedbackString.length() - 2 );
					vocabFeedbackString.append( " + " );
				}
			}
		}
		vocabQueryString.append( "&" );
		int ind = vocabFeedbackString.lastIndexOf( " + " );
		if ( ind > -1 ) {
			vocabFeedbackString.setLength( ind );
		}
	}

	/**
	 *  Sets the state attribute of the MetadataVocabInputState object
	 *
	 * @param  req     The new state value
	 * @param  system  The new state value
	 * @deprecated     As of MetadataUI v1.0, replaced by <a
	 *      href="#setState(javax.servlet.http.HttpServletRequest)"> setState()</a>
	 */
	public void setState( HttpServletRequest req, String system ) {
		setState( req );
	}


	/**
	 *  Gets the part of the query string associated with the user's vocab
	 *  selections.
	 *
	 * @return    The state value
	 */
	public String getState() {
		return vocabQueryString.toString();
	}

	/**
	 *  Gets a string (placed after "Your selections" in V3.x of the DDS)
	 *  representing the user's selected vocab criteria.
	 *
	 * @return    The stateFeedback value
	 */
	public String getStateFeedback() {
		return vocabFeedbackString.toString();
	}

	/**
	 *  Is the given field/value pair selected currently?
	 *
	 * @param  field  Vocab field encoded ID
	 * @param  value  Vocab value encoded ID
	 * @return        boolean indicating whether the specified vocab node is
	 *      selected
	 */
	public boolean isSelected( String field, String value ) {
		if ( vocabQueryString.indexOf( "&" + field + "=" + value + "&" ) > -1 ) {
			return true;
		}
		return false;
	}

	/**
	 *  Is the given field/value pair selected currently? (for Struts .isSelected
	 *  access)
	 *
	 * @param  field  Vocab field encoded ID
	 * @param  value  Vocab value encoded ID
	 * @return        boolean indicating whether the specified vocab node is
	 *      selected
	 */
	public boolean getIsSelected( String field, String value ) {
		return isSelected( field, value );
	}
}

