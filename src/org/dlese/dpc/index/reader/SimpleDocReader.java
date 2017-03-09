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
package org.dlese.dpc.index.reader;



import org.apache.lucene.document.*;

import org.dlese.dpc.index.writer.*;

import org.dlese.dpc.index.*;

import org.dlese.dpc.xml.*;

import org.dlese.dpc.webapps.tools.*;

import org.dlese.dpc.util.*;



import javax.servlet.*;

import java.io.*;

import java.text.*;

import java.util.*;



/**

 *  A bean that provides a simple implementataion of the {@link org.dlese.dpc.index.reader.DocReader}

 *  interface. This DocReader is used when no other is available.

 *

 * @author     John Weatherley

 */

public class SimpleDocReader extends DocReader {



	/**  Constructor for the SimpleDocReader object */

	public SimpleDocReader() { }





	/**  Init method does nothing. */

	public void init() { }





	/**

	 *  Constructor that may be used programatically to wrap a reader around a Lucene {@link

	 *  org.apache.lucene.document.Document} created by a {@link org.dlese.dpc.index.writer.DocWriter}. Sets the

	 *  score to 0.

	 *

	 * @param  doc  A Lucene {@link org.apache.lucene.document.Document} created by a {@link

	 *      org.dlese.dpc.index.writer.DocWriter}.

	 */

	public SimpleDocReader(Document doc) {

		super(doc);

	}





	/**

	 *  Gets a String describing the reader type. This may be used in (Struts) beans to determine which type of

	 *  reader is available for a given search result and thus what data is available for display in the UI. The

	 *  reader type implies which getter methods are available.

	 *

	 * @return    The readerType value.

	 */

	public String getReaderType() {

		return "SimpleDocReader";

	}

}



