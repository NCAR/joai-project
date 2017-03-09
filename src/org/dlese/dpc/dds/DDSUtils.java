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
package org.dlese.dpc.dds;

import org.dlese.dpc.index.*;
import org.dlese.dpc.index.reader.*;
import org.dlese.dpc.vocab.MetadataVocab;
import java.util.*;

/**
 *  Utility class for working with DDS resources and collections
 *
 *@author    Ryan Deardorff
 */
public final class DDSUtils {
	ResultDoc currentResultDoc = null;
	MetadataVocab vocab;

	/**
	 *  Constructor for the DDSUtils object
	 */
	public DDSUtils() { }

	/**
	 *  Sets the metadataVocab attribute of the DDSUtils object
	 *
	 *@param  vocab  The new metadataVocab value
	 */
	public void setMetadataVocab( MetadataVocab vocab ) {
		this.vocab = vocab;
	}

	/**
	 *  Sets the currentResultDoc attribute of the DDSQueryForm object
	 *
	 *@param  result  The new currentResultDoc value
	 */
	public void setCurrentResultDoc( ResultDoc result ) {
		currentResultDoc = result;
	}

	/**
	 *  Gets the contentStandards attribute of the DDSQueryForm object
	 */
	public String[] getContentStandards() {
		HashMap seenStandard = new HashMap();
		ItemDocReader reader = (ItemDocReader)currentResultDoc.getDocReader();
		String[] standards = reader.getContentStandards();
		for ( int i = 0; i < standards.length; i++ ) {
			String uiLabel = vocab.getTopLevelAbbrevLabelOf( "dds.descr.en-us", "contentStandard", "cs", standards[i] );
			if ( seenStandard.get( uiLabel ) == null ) {
				seenStandard.put( uiLabel, new Boolean( true ) );
			}
		}
		Set s = seenStandard.keySet();
		Iterator iter = s.iterator();
		String[] ret = new String[s.size()];
		int i = 0;
		while ( iter.hasNext() ) {
			ret[i++] = (String)iter.next();
		}
		return ret;
	}
}


