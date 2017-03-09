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
package org.dlese.dpc.schemedit.standards.asn;

import org.dlese.dpc.schemedit.SchemEditUtils;
import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.util.strings.FindAndReplace;
import org.dom4j.*;
import java.util.*;
import java.io.File;

/**
 *  GradeRangeHelper that accomodates the "nsdlEdLevel" vocabs, which are
 *  defined by the nsdl schemas but which are also used by others, including
 *  MSP2, res_qual, commcore, etc. The nsdlEdLevel vocab is a flat controlled
 *  vocabulary that contains values that are interpreted hierarchically. For
 *  example, "Early Elementary" is interpreted as containing "Kindergarten",
 *  "Grade 1" and "Grade 2". The problem is, that the metadata editor cannot
 *  enforce the implicit hierarchical structure without substantial ....". NOTE:
 *  at this point we decided to enforce these semantics in the metadata editor,
 *  which might change everything ..... To Be completed!
 *
 * @author    Jonathan Ostwald
 */
public class NsdlGradeRangeHelper extends GradeRangeHelper {

	private static boolean debug = false;



	/**
	 *  Constructor for the NsdlGradeRangeHelper read from file at specified path
	 */
	public NsdlGradeRangeHelper() {
		super();
	}


	/**
	 *  Return an integer representing the lowest gradeLevel contained in the
	 *  provided gradeRangeVocab values, or -1 if none are provided
	 *
	 * @param  gradeRangeVocabs  NOT YET DOCUMENTED
	 * @return                   The startGrade value
	 */
	public int getStartGrade(String[] gradeRangeVocabs) {
		prtln("\ngetStartGrade");
		return getDerivedGradeRange(gradeRangeVocabs).getMinGrade();
	}


	/**
	 *  Return an integer representing the highest gradeLevel contained in the
	 *  provided gradeRangeVocab values, or -1 if none are provided
	 *
	 * @param  gradeRangeVocabs  NOT YET DOCUMENTED
	 * @return                   The startGrade value
	 */
	public int getEndGrade(String[] gradeRangeVocabs) {
		prtln("\ngetEndGrade");

		return getDerivedGradeRange(gradeRangeVocabs).getMaxGrade();
	}


	/**
	 *  Create a sorted list of gradeRangeItems using GrItemComparator.<P>
	 *
	 *  NOTE: gradeRange values that do not have a corresponding GradeRangeItem are
	 *  ignored.
	 *
	 * @param  values  grade range vocab values
	 * @return         sorted list of GradeRangeItems
	 */
	public List makeSortedGRList(String[] values) {
		List sorted = new ArrayList();
		for (int i = 0; i < values.length; i++) {
			GradeRangeItem grItem = getGradeRangeItem(values[i]);
			if (grItem != null)
				sorted.add(grItem);
		}
		Collections.sort(sorted, new GrItemComparator());
		showList(sorted, "sorted");
		return pruneSortedGRList(sorted);
	}


	/**
	 *  Removes GradeRangeItems that contain the item in front of them.
	 *
	 * @param  sorted  a list of GradeRangeItems sorted by GrItemComparator
	 * @return         list of GradeRangeItems with no containing ranges.
	 */
	public List pruneSortedGRList(List sorted) {
		int max = 0;
		int i = sorted.size() - 1;
		while (i > 0 && max < 20) {
			GradeRangeItem item = (GradeRangeItem) sorted.get(i);
			GradeRangeItem other = (GradeRangeItem) sorted.get(i - 1);
			if (item.contains(other)) {
				sorted.remove(item);
			}
			i--;
			max++;
		}
		showList(sorted, "pruned");
		return sorted;
	}


	/**
	 *  Gets the gradeRange "extents" of a group of gradeRangeVocabs.
	 *
	 * @param  gradeRangeVocabs  vocab values selected in an itemDocument
	 * @return                   the lowest and highest gradeLevels in
	 *      non-containing gradeRanges.
	 */
	public GradeRange getDerivedGradeRange(String[] gradeRangeVocabs) {
		prtln("\ngetEndGrade");
		GradeRange derived = null;
		try {
			if (gradeRangeVocabs == null || gradeRangeVocabs.length == 0)
				return new GradeRange(-1, -1);

			for (Iterator i = makeSortedGRList(gradeRangeVocabs).iterator(); i.hasNext(); ) {
				GradeRangeItem grItem = (GradeRangeItem) i.next();
				if (derived == null)
					derived = new GradeRange(grItem.getMinGrade(), grItem.getMaxGrade());
				if (grItem.contains(derived)) {
					// prtln (grItem.toStr() + " contains " + derived.toStr());
					continue;
				}
				derived.setMinGrade(Math.min(derived.getMinGrade(), grItem.getMinGrade()));
				derived.setMaxGrade(Math.max(derived.getMaxGrade(), grItem.getMaxGrade()));
			}
		} catch (Throwable t) {
			prtlnErr("getDerivedGradeRange: " + t.getMessage());
			t.printStackTrace();
		}
		if (derived == null)
			derived = new GradeRange(-1, -1);

		return derived;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @author    Jonathan Ostwald
	 */
	public class GrItemComparator implements Comparator {

		int getDiffComparison(GradeRangeItem gr1, GradeRangeItem gr2) {
			int diffComparison = 0;

			// put the most specific range (least diff between minGrade and maxGrade) first
			try {
				int diff1 = gr1.getMinGrade() - gr1.getMaxGrade();
				int diff2 = gr2.getMinGrade() - gr2.getMaxGrade();
				diffComparison = Integer.valueOf(diff1).compareTo(Integer.valueOf(diff2));
			} catch (Throwable t) {
				return 0;
			}
			return diffComparison;
		}


		int getMinComparison(GradeRangeItem gr1, GradeRangeItem gr2) {
			int minComparison = 0;

			try {
				minComparison = Integer.valueOf(gr1.getMinGrade()).compareTo(Integer.valueOf(gr2.getMinGrade()));
			} catch (Throwable t) {
				return 0;
			}
			return minComparison;
		}


		int getMaxComparison(GradeRangeItem gr1, GradeRangeItem gr2) {
			int maxComparison = 0;

			try {
				maxComparison = Integer.valueOf(gr1.getMaxGrade()).compareTo(Integer.valueOf(gr2.getMaxGrade()));
			} catch (Throwable t) {
				return 0;
			}
			return maxComparison;
		}


		/**
		 *  Compare two GradeRanges to enable "NSDL graderange sort".
		 *
		 * @param  o1  NOT YET DOCUMENTED
		 * @param  o2  NOT YET DOCUMENTED
		 * @return     NOT YET DOCUMENTED
		 */
		public int compare(Object o1, Object o2) {

			GradeRangeItem gr1 = (GradeRangeItem) o1;
			GradeRangeItem gr2 = (GradeRangeItem) o2;

			int minComp = getMinComparison(gr1, gr2);
			int diffComp = getDiffComparison(gr1, gr2);
			int maxComp = getMaxComparison(gr1, gr2);

			if (gr1.contains(gr2))
				return 1;

			if (gr2.contains(gr1))
				return -1;

			if (minComp == 0)
				return maxComp;
			else
				return minComp;
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "NsdlGradeRangeHelper");
		}
	}


	private static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "NsdlGradeRangeHelper");
	}
}

