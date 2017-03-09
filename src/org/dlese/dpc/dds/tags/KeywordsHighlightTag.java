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
package org.dlese.dpc.dds.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.PageContext;
import org.dlese.dpc.dds.KeywordsHighlight;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.*;
import java.io.*;

/**
 *  Provide keyword highlighting of the body content of this tag.
 *
 *@author    Ryan Deardorff
 */
public class KeywordsHighlightTag extends BodyTagSupport {
	boolean truncateString = false;                  // Should the text being highlighted be truncated?
	int maxStringLength = 1100;                      // If truncating, what is the maximum allowed length of the text?
	int minStringLength = 300;                       // What is the minimum length of a truncated string?
	boolean addWbr = false;                          // Should every '/' have '<wbr>' insterted after it? (allows URLs to wrap)

	/**
	 *  Sets the truncateString attribute of the KeywordsHighlightTag object
	 *
	 *@param  truncate  The new truncateString value
	 */
	public void setTruncateString( String truncate ) {
		if ( truncate.toLowerCase().equals( "true" ) ) {
			truncateString = true;
		}
	}

	/**
	 *  Sets the maxStringLength attribute of the KeywordsHighlightTag object
	 *
	 *@param  max  The new maxStringLength value
	 */
	public void setMaxStringLength( String max ) {
		maxStringLength = Integer.parseInt( max );
	}

	/**
	 *  Sets the minStringLength attribute of the KeywordsHighlightTag object
	 *
	 *@param  min  The new minStringLength value
	 */
	public void setMinStringLength( String min ) {
		minStringLength = Integer.parseInt( min );
	}

	/**
	 *  Sets the addWbr attribute of the KeywordsHighlightTag object
	 *
	 *@param  bool  The new addWbr value
	 */
	public void setAddWbr( String bool ) {
		if ( bool.toLowerCase().equals( "true" ) ) {
			addWbr = true;
		}
	}

	/**
	 *  Return EVAL_BODY_BUFFERED for evaluation of body text
	 *
	 *@exception  JspException
	 */
	public int doStartTag() throws JspException {
		return EVAL_BODY_BUFFERED;
	}

	/**
	 *  Highlight keywords in the text of the tag's body
	 *
	 *@exception  JspException
	 */
	public int doEndTag() throws JspException {
		BodyContent body = getBodyContent();
		try {
			JspWriter out = body.getEnclosingWriter();
			KeywordsHighlight keysHighlight = (KeywordsHighlight)pageContext.getSession().getAttribute( "KeywordsHighlight" );
			if ( keysHighlight != null ) {
				String text = body.getString();
				if ( truncateString ) {
					boolean foundPeriod = false;
					int pos = keysHighlight.getFirstHighlightIndex();
					if ( ( pos < maxStringLength ) && ( pos > minStringLength ) ) {
						pos = text.indexOf( '.', pos );
						if ( pos < maxStringLength ) {
							foundPeriod = true;
						}
						else {
							pos = text.indexOf( ' ', maxStringLength );
						}
					}
					else {
						pos = text.indexOf( '.', minStringLength );
						if ( pos < maxStringLength ) {
							foundPeriod = true;
						}
						else {
							pos = text.indexOf( ' ', maxStringLength );
						}
					}
					if ( pos > 0 ) {
						text = text.substring( 0, pos ) + "...";
					}
				}
				text = keysHighlight.highlight( text, addWbr ) + ' ';
				out.print( text );
			}
			else {
				out.print( body.getString() );
			}
		}
		catch ( IOException ioe ) {
			System.out.println( "Error in KeywordsHighlightTag: " + ioe );
		}
		return SKIP_BODY;
	}
}

