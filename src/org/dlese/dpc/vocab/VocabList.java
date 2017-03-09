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
package org.dlese.dpc.vocab;

import java.util.*;

/**
 *  List/map combination for accessing VocabNodes
 *
 *@author    ryandear
 */
public class VocabList {
	public ArrayList item = new ArrayList();         // List of nodes
	public HashMap map = new HashMap();              // Hash into spots within the list
	public VocabList parent;                         // Parent list of this list
	public String definition;                        // Definition of this list (or sub-list)
	public int groupType = 0;                        // 0 = flyout, 1 = drop-down, 2 = indented
	public String jsVar;                             // treeMenu JS var name
}

