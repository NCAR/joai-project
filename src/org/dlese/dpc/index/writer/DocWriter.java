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
package org.dlese.dpc.index.writer;



import java.io.*;

import org.dlese.dpc.xml.*;

import org.apache.lucene.document.*;



/**

 *  Abstract class for creating a typed Lucene {@link org.apache.lucene.document.Document}. A {@link

 *  org.dlese.dpc.index.reader.DocReader} should be implemented to read {@link

 *  org.apache.lucene.document.Document}s of this type.

 *

 * @author     John Weatherley

 * @see        org.dlese.dpc.index.reader.DocReader

 * @see        org.dlese.dpc.index.ResultDoc

 */

public interface DocWriter {



	/**

	 *  Returns a unique document type key for this kind of document, corresponding to the format type. For

	 *  example "dleseims". The string is parsed using the Lucene {@link

	 *  org.apache.lucene.analysis.standard.StandardAnalyzer} so it must be lowercase and should not contain any

	 *  stop words.

	 *

	 * @return                The docType String

	 * @exception  Exception  This method should throw and Exception with appropriate error message if an error

	 *      occurs.

	 */

	public abstract String getDocType() throws Exception;





	/**

	 *  Gets the name of the concrete {@link org.dlese.dpc.index.reader.DocReader} class that is used to read

	 *  this type of {@link org.apache.lucene.document.Document}, for example

	 *  "org.dlese.dpc.index.reader.ItemDocReader". The class name is used by the {@link

	 *  org.dlese.dpc.index.ResultDoc} factory to return the appropriate DocReader.

	 *

	 * @return    The name of the {@link org.dlese.dpc.index.reader.DocReader}

	 */

	public abstract String getReaderClass();



}



