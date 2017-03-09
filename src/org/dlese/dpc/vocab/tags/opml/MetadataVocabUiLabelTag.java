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

import org.dlese.dpc.vocab.MetadataVocab;
import org.dlese.dpc.util.strings.StringUtil;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.http.*;

/**
 *  Tag handler for retreiving a UI label from the given field and value
 *
 * @author    Ryan Deardorff
 */
public class MetadataVocabUiLabelTag extends MetadataVocabTag {
	String field = "";
	String value = "";
	boolean getAbbreviated = false;                                // return abbreviated version of label?

	/**
	 *  Sets the system attribute of the MetadataVocabGetFieldValueUiLabel object
	 *
	 * @param  field  The new fieldId value
	 */
	public void setField( String field ) {
		this.field = field;
	}

	/**
	 *  Sets the system attribute of the MetadataVocabGetFieldValueUiLabel object
	 *
	 * @param  value  The new value value
	 */
	public void setValue( String value ) {
		this.value = value;
	}

	/**
	 *  Sets the getAbbreviated attribute of the MetadataVocabUiLabelTag object
	 *
	 * @param  bool  The new getAbbreviated value
	 */
	public void setGetAbbreviated( String bool ) {
		if ( bool.toLowerCase().equals( "true" ) ) {
			getAbbreviated = true;
		}
	}

	/**
	 *  Start tag
	 *
	 * @return
	 * @exception  JspException
	 */
	public int doStartTag() throws JspException {
		setupTag( pageContext );
		StringBuffer outStr = new StringBuffer();
		try {
			if ( value.equals( "" ) ) {
				pageContext.getOut().print( vocab.getUiFieldLabel( metaFormat, metaVersion,
					audience, language, field, getAbbreviated ) );
			}
			else {
				pageContext.getOut().print( vocab.getUiValueLabel( metaFormat, metaVersion,
					audience, language, field, value, getAbbreviated ) );
			}
		}
		catch ( Exception e ) {
			System.out.println( "Exception in MetadataVocabUiLabelTag: " + e.getMessage() );
			throw new JspException( e.getMessage() );
		}
		return SKIP_BODY;
	}
}

