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
package org.dlese.dpc.vocab.tags.opml;

import org.dlese.dpc.vocab.*;
import java.util.*;
import java.io.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.http.*;

/**
 *  Tag handler for retreiving a vocabulary format/version/audience/language
 *  grouping as OPML
 *
 * @author    Ryan Deardorff
 */
public class MetadataVocabGetOPMLTag extends MetadataVocabTag {

	String field = "";

	/**
	 *  Sets the field attribute of the MetadataVocabSetResponseGroupTag object
	 *
	 * @param  field  The new field value
	 */
	public void setField( String field ) {
		this.field = field;
	}

	/**
	 *  Set one of a potential list of response values
	 *
	 * @return
	 * @exception  JspException
	 */
	public int doStartTag() throws JspException {
		try {
			setupTag( pageContext );
			pageContext.getOut().print( vocab.getOPML( metaFormat, metaVersion, audience, language, field ) );
		}
		catch ( java.io.IOException ex ) {
			throw new JspException( ex.getMessage() );
		}
		return SKIP_BODY;
	}

	/**
	 *  Description of the Method
	 *
	 * @return
	 * @exception  JspException
	 */
	public int doEndTag() throws JspException {
		return EVAL_PAGE;
	}

	/**
	 *  Description of the Method
	 */
	public void release() {
		super.release();
	}
}

