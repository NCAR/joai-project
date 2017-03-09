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
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.PageContext;
import org.dlese.dpc.dds.KeywordsHighlight;

/**
 *  Provide keyword highlighting of the body content of this tag.
 *
 *@author    Ryan Deardorff
 */
public class SetKeywordsHighlightTag extends TagSupport {
	String keywords;
	String highlightColor = null;
	String cssClassName = null;

	/**
	 *  Sets the keywords attribute of the KeywordsHighlightTag object
	 *
	 *@param  keywords  The new keywords value
	 */
	public void setKeywords( String keywords ) {
		this.keywords = keywords;
	}

	/**
	 *  Sets the highlightColor attribute of the SetKeywordsHighlightTag object
	 *
	 *@param  highlightColor  The new highlightColor value
	 */
	public void setHighlightColor( String highlightColor ) {
		this.highlightColor = highlightColor;
	}

	/**
	 *  Sets the cssClassName attribute of the SetKeywordsHighlightTag object
	 *
	 *@param  cssClassName  The new cssClassName value
	 */
	public void setCssClassName( String cssClassName ) {
		this.cssClassName = cssClassName;
	}	
	
	/**
	 *  Description of the Method
	 *
	 *@exception  JspException
	 */
	public int doStartTag() throws JspException {
		KeywordsHighlight keysHighlight = new KeywordsHighlight( keywords, highlightColor, cssClassName );
		pageContext.getSession().setAttribute( "KeywordsHighlight", keysHighlight );
		return SKIP_BODY;
	}
}

