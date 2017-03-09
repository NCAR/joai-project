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
import java.util.*;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.http.*;

/**
 *  Tag handler for rendering vocabulary select list inputs
 *
 * @author    Ryan Deardorff
 */
public class MetadataVocabSelectListTag extends MetadataVocabTag {
	private int size = 1;                                          // Set default height of 1 for 'drop-down' list

	/**
	 *  Sets the size attribute of the MetadataVocabSelectListTag object
	 *
	 * @param  size  The new size value
	 */
	public void setSize( String size ) {
		this.size = Integer.parseInt( size );
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
			pageContext.getOut().print( vocab.getVocabSelectList( system, subGroup, size, inputState ) );
		}
		catch ( java.io.IOException ex ) {
			throw new JspException( ex.getMessage() );
		}
		return SKIP_BODY;
	}
}

