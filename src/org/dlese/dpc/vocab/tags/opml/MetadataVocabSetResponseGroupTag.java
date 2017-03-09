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
 *  Tag handler for rendering vocabulary responses, re-ordered/grouped by OPML
 *
 * @author    Ryan Deardorff
 */
public class MetadataVocabSetResponseGroupTag extends MetadataVocabTag {

	/**
	 *  Set one of a potential list of response values
	 *
	 * @return
	 * @exception  JspException
	 */
	public int doStartTag() throws JspException {
		setupTag( pageContext );
		vocab.setResponseGroup( pageContext, metaFormat, audience, language, field );
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

