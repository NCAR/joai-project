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
public class MetadataVocabTopLevelAbbrevLabelTag extends MetadataVocabTag {
	String fieldId = "";
	String valueId = "";
	String fieldName = "";

	/**
	 *  Sets the system attribute of the MetadataVocabGetFieldValueUiLabel object
	 *
	 * @param  fieldId  The new fieldId value
	 */
	public void setFieldId( String fieldId ) {
		this.fieldId = fieldId;
	}

	/**
	 *  Sets the fieldName attribute of the MetadataVocabTopLevelAbbrevLabelTag
	 *  object
	 *
	 * @param  fieldName  The new fieldName value
	 */
	public void setFieldName( String fieldName ) {
		this.fieldName = fieldName;
	}

	/**
	 *  Sets the system attribute of the MetadataVocabGetFieldValueUiLabel object
	 *
	 * @param  valueId  The new valueId value
	 */
	public void setValueId( String valueId ) {
		this.valueId = valueId;
	}

	/**
	 *  Start tag
	 *
	 * @return
	 * @exception  JspException
	 */
	public int doStartTag() throws JspException {
		try {
			setupTag( pageContext );
			StringBuffer outStr = new StringBuffer();
			pageContext.getOut().print( vocab.getTopLevelAbbrevLabelOf( system, fieldName, fieldId, valueId ) );
		}
		catch ( java.io.IOException ex ) {
			throw new JspException( ex.getMessage() );
		}
		return SKIP_BODY;
	}
}

