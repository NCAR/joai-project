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
import javax.servlet.jsp.tagext.TagSupport;
import java.io.*;

/**
 *  Tag for displaying the contents of a system file
 *
 *@author    ryandear
 */
public class ReadFileTag extends TagSupport {
	private String filename;


	/**
	 *  Sets the filename attribute of the ReadFileTag object
	 *
	 *@param  filename  The new filename value
	 */
	public void setFilename( String filename ) {
		this.filename = filename;
	}

	/**
	 *  Read the file, spit it out
	 *
	 *@exception  JspException  Description of the Exception
	 *@return                   Description of the Return Value
	 */
	public int doStartTag() throws JspException {
		try {
			FileReader file = new FileReader( filename );
			BufferedReader in = new BufferedReader( file );
			String s = null;
			while ( ( s = in.readLine() ) != null ) {
				pageContext.getOut().print( s );
			}
		}
		catch ( java.io.IOException e ) {
			throw new JspException( e.getMessage() );
		}
		return SKIP_BODY;
	}
}

