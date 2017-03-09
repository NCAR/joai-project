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
package org.dlese.dpc.index;

import java.util.*;
import org.apache.lucene.document.*;

public class ResultDocs {

	BitSet bits;
	ResultDoc [] docs;

	public ResultDocs(ResultDoc [] docs, BitSet bits) {
		this.docs = docs;
		this.bits = bits;
	}

	public int length() {
		return (docs == null) ? 0 : docs.length;
	}
	
	public ResultDoc [] results() {
		return docs;
	}
	
	public List asList() {
		if (docs != null) {
			return Arrays.asList(docs);
		}
		return new ArrayList();
	}
	
	public BitSet asBitSet() {
		return bits;
	}

}

