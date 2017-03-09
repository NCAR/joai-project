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
import org.dlese.dpc.vocab.MetadataVocabInputState;
import org.dlese.dpc.util.strings.StringUtil;
import java.util.*;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.http.*;

/**
 *  Tag handler for rendering vocabulary checkboxes
 *
 * @author    Ryan Deardorff
 */
public class MetadataVocabCheckboxesTag extends MetadataVocabTag {
	private String value = null;
	private String label = null;
	private String tdWidth = "";
	private boolean skipTopRow = false;
	private int wrap = 100000;

	/**
	 *  Sets the wrap attribute of the MetadataVocabCheckboxesTag object
	 *
	 * @param  wrap  The new wrap value
	 */
	public void setWrap( String wrap ) {
		this.wrap = Integer.parseInt( wrap );
	}

	/**
	 *  Sets the value attribute of the MetadataVocabCheckboxesTag object
	 *
	 * @param  value  The new value value
	 */
	public void setValue( String value ) {
		this.value = value;
	}

	/**
	 *  Sets the label attribute of the MetadataVocabCheckboxesTag object
	 *
	 * @param  label  The new label value
	 */
	public void setLabel( String label ) {
		this.label = label;
	}

	/**
	 *  Sets the skipTopRow attribute of the MetadataVocabCheckboxesTag object
	 *
	 * @param  skipTopRow  The new skipTopRow value
	 */
	public void setSkipTopRow( String skipTopRow ) {
		if ( skipTopRow.equals( "true" ) ) {
			this.skipTopRow = true;
		}
		else {
			this.skipTopRow = false;
		}
	}

	/**
	 *  Sets the tdWidth attribute of the MetadataVocabCheckboxesTag object
	 *
	 * @param  tdWidth  The new tdWidth value
	 */
	public void setTdWidth( String tdWidth ) {
		this.tdWidth = tdWidth;
	}

	/**
	 *  Description of the Method
	 *
	 * @return
	 * @exception  JspException
	 */
	public int doStartTag() throws JspException {
		try {
			setupTag( pageContext );
			MetadataVocabInputState inputState = (MetadataVocabInputState)pageContext.getSession().getAttribute( "MetadataVocabInputState" );
			if ( inputState == null ) {
				inputState = new MetadataVocabInputState();
			}
			if ( value != null ) {
				pageContext.getOut().print( vocab.getVocabCheckbox( system, value, label, inputState ) );
			}
			else {
				pageContext.getOut().print( vocab.getVocabCheckboxes( system, subGroup, wrap, tdWidth, skipTopRow, inputState ) );
			}
		}
		catch ( java.io.IOException ex ) {
			throw new JspException( ex.getMessage() );
		}
		return SKIP_BODY;
	}
}

