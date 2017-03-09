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
package org.dlese.dpc.util.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 *  Description of the Class
 *
 *@author     ryandear
 */
public class StoreBodyTag extends BodyTagSupport {
	private String save, print;
	private boolean isSave = false;

	/**
	 *  Sets the save attribute of the StoreBodyTag object
	 *
	 *@param  save  The new save value
	 */
	public void setSave( String save ) {
		this.save = save;
		isSave = true;
	}

	/**
	 *  Sets the print attribute of the StoreBodyTag object
	 *
	 *@param  print  The new print value
	 */
	public void setPrint( String print ) {
		this.print = print;
	}

	/**
	 *  Description of the Method
	 *
	 *@return                   Description of the Return Value
	 *@exception  JspException  Description of the Exception
	 */
	public int doAfterBody() throws JspException {
		try {
			if ( isSave ) {
				String body = bodyContent.getString();
				pageContext.setAttribute( "StoreBodyTag" + save, body );
				bodyContent.clearBody();
				bodyContent.print( body );
				bodyContent.writeOut( getPreviousOut() );
			}
			else {
				String body = (String)pageContext.getAttribute( "StoreBodyTag" + print );
				if ( body == null ) {
					body = "";
				}
				bodyContent.clearBody();
				bodyContent.print( body );
				bodyContent.writeOut( getPreviousOut() );
			}
		}
		catch ( java.io.IOException e ) {
			throw new JspException( e.getMessage() );
		}
		return SKIP_BODY;
	}
}

