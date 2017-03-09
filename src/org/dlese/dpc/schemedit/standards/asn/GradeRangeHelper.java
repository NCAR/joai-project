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
 *  Helper to translate between controlled vocabs for cataloging grade levels in
 *  a particular framework and the numerical grade ranges implied by the vocabs.
 *
 * @author    Jonathan Ostwald
 */
public class GradeRangeHelper {

	private static boolean debug = false;
	private File source;
	private HashMap gradeRangeMap;


	/**
	 *  Constructor for the GradeRangeHelper read from file at specified path
	 */
	public GradeRangeHelper() {
		gradeRangeMap = new HashMap();
	}


	/**
	 *  Returns unordered set of GradeRangeVocab values
	 *
	 * @return    The gradeRanges value
	 */
	public Collection getGradeRanges() {
		return gradeRangeMap.keySet();
	}


	/**
	 *  Gets the gradeRangeItem for the provided vocab value
	 *
	 * @param  vocab  a grade range vocab value
	 * @return        The gradeRangeItem value or null
	 */
	public GradeRangeItem getGradeRangeItem(String vocab) {
		return (GradeRangeItem) gradeRangeMap.get(vocab);
	}


	/**
	 *  Gets the label (for UI purposes) of a gradeRange vocab value. E.g., for the
	 *  value of "DLESE:Primary elementary", the label is "Primary elementary".
	 *
	 * @param  gradeRangeValue  NOT YET DOCUMENTED
	 * @return                  The gradeRangeLabel value
	 */
	public String getGradeRangeOptionLabel(String gradeRangeValue) {
		GradeRangeItem gr = getGradeRangeItem(gradeRangeValue);
		if (gr == null)
			return "";
		return gr.label;
	}


	/**
	 *  Gets the gradeOptionRangeValue attribute of the GradeRangeHelper object
	 *
	 * @param  gradeRangeValue  NOT YET DOCUMENTED
	 * @return                  The gradeOptionRangeValue value
	 */
	public String getGradeOptionRangeValue(String gradeRangeValue) {
		GradeRangeItem gr = getGradeRangeItem(gradeRangeValue);
		if (gr == null)
			return "";
		return gr.value;
	}


	/**
	 *  * Gets the startGrade of the given gradeRange vocab value. E.g.,
	 *  "DLESE:Primary elementary" would return 0.
	 *
	 * @param  gradeRangeVocab  NOT YET DOCUMENTED
	 * @return                  The startGrade value
	 */
	public int getStartGrade(String gradeRangeVocab) {
		GradeRangeItem gr = getGradeRangeItem(gradeRangeVocab);
		if (gr == null) {
			prtln("gr not found for " + gradeRangeVocab);
			return Integer.MAX_VALUE;
		}
		return gr.getMinGrade();
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
		if (gradeRangeVocabs == null || gradeRangeVocabs.length == 0)
			return -1;
		int startGrade = Integer.MAX_VALUE;
		for (int i = 0; i < gradeRangeVocabs.length; i++) {
			prtln("\t" + gradeRangeVocabs[i].toString());
			prtln("\t  ..." + getStartGrade(gradeRangeVocabs[i]));
			startGrade = Math.min(startGrade, getStartGrade(gradeRangeVocabs[i]));
		}
		if (startGrade == Integer.MAX_VALUE)
			startGrade = -1;
		return startGrade;
	}


	/**
	 *  Gets the endGrade of the given gradeRange vocab value. E.g., "DLESE:Primary
	 *  elementary" would return 2.
	 *
	 * @param  gradeRangeVocab  NOT YET DOCUMENTED
	 * @return                  The endGrade value
	 */
	public int getEndGrade(String gradeRangeVocab) {
		GradeRangeItem gr = getGradeRangeItem(gradeRangeVocab);
		if (gr == null) {
			prtlnErr("WARNING: gr not found for " + gradeRangeVocab);
			return -1;
		}
		return gr.getMaxGrade();
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
		if (gradeRangeVocabs == null || gradeRangeVocabs.length == 0)
			return -1;
		int endGrade = -1;
		for (int i = 0; i < gradeRangeVocabs.length; i++) {
			prtln("\t" + gradeRangeVocabs[i].toString());
			prtln("\t  ..." + getEndGrade(gradeRangeVocabs[i]));
			endGrade = Math.max(endGrade, getEndGrade(gradeRangeVocabs[i]));
		}
		return endGrade;
	}


	/**
	 *  Creates a mapping from a gradeRange vocab values to {@link GradeRangeItem}
	 *  instance which stores the vocab value, the UI label, the UI value, and the
	 *  start and end grades.
	 */
	private void init() {
		gradeRangeMap = new HashMap();
	}


	/**
	 *  Adds a feature to the Item attribute of the GradeRangeHelper object
	 *
	 * @param  vocab  The feature to be added to the Item attribute
	 * @param  grade  The feature to be added to the Item attribute
	 */
	public void addItem(String vocab, int grade) {
		addItem(vocab, grade, grade);
	}


	/**
	 *  Adds a feature to the Item attribute of the GradeRangeHelper object
	 *
	 * @param  vocab       The feature to be added to the Item attribute
	 * @param  startGrage  The feature to be added to the Item attribute
	 * @param  endGrade    The feature to be added to the Item attribute
	 */
	public void addItem(String vocab, int startGrage, int endGrade) {
		addItem(vocab, vocab, vocab, startGrage, endGrade);
	}


	/**
	 *  Adds a feature to the Item attribute of the GradeRangeHelper object
	 *
	 * @param  vocab     The feature to be added to the Item attribute
	 * @param  label     The feature to be added to the Item attribute
	 * @param  value     The feature to be added to the Item attribute
	 * @param  minGrade  The feature to be added to the Item attribute
	 * @param  maxGrade  The feature to be added to the Item attribute
	 */
	public void addItem(String vocab, String label, String value, int minGrade, int maxGrade) {
		GradeRangeItem grItem = new GradeRangeItem(vocab, label, value, minGrade, maxGrade);
		gradeRangeMap.put(grItem.value, grItem);
	}


	/**  NOT YET DOCUMENTED */
	public void report() {
		prtln("\nGradeRangeHelper Items");
		for (Iterator i = this.gradeRangeMap.values().iterator(); i.hasNext(); ) {
			prtln(((GradeRangeItem) i.next()).toString());
		}
	}


	/**
	 *  Utility class to associate a gradeRange vocab value with a label suitable
	 *  for UI, and a range of gradeLevels (min and max)
	 *
	 * @author    Jonathan Ostwald
	 */
	public class GradeRangeItem extends GradeRange {
		private String vocab;
		private String label;
		private String value;


		/**
		 *  Constructor for the GradeRangeItem object
		 *
		 * @param  vocab  NOT YET DOCUMENTED
		 * @param  grade  NOT YET DOCUMENTED
		 */
		GradeRangeItem(String vocab, int grade) {
			this(vocab, grade, grade);
		}


		/**
		 *  Constructor for the GradeRangeItem object
		 *
		 * @param  vocab     NOT YET DOCUMENTED
		 * @param  minGrade  NOT YET DOCUMENTED
		 * @param  maxGrade  NOT YET DOCUMENTED
		 */
		GradeRangeItem(String vocab, int minGrade, int maxGrade) {
			this(vocab, vocab, vocab, minGrade, maxGrade);
		}


		/**
		 *  Constructor for the GradeRangeItem object
		 *
		 * @param  label     a label for this gradeRange suitable for UI
		 * @param  value     a value from the controlled vocab for gradeRange
		 * @param  minGrade  starting grade for this gradeRange
		 * @param  maxGrade  ending grade for this gradeRange
		 * @param  vocab     NOT YET DOCUMENTED
		 */
		GradeRangeItem(String vocab, String label, String value, int minGrade, int maxGrade) {
			super(minGrade, maxGrade);
			this.vocab = vocab;
			this.label = label;
			this.value = value;
		}


		/**
		 *  Gets the vocab attribute of the GradeRangeItem object
		 *
		 * @return    The vocab value
		 */
		public String getVocab() {
			return this.vocab;
		}


		/**
		 *  Gets the label attribute of the GradeRangeItem object
		 *
		 * @return    The label value
		 */
		public String getLabel() {
			return this.label;
		}


		/**
		 *  Gets the value attribute of the GradeRangeItem object
		 *
		 * @return    The value value
		 */
		public String getValue() {
			return this.value;
		}


		/**
		 *  NOT YET DOCUMENTED
		 *
		 * @return    NOT YET DOCUMENTED
		 */
		public String toString() {
			String NL = "\n\t";
			String s = "\n" + vocab;
			s += NL + "label: " + label;
			s += NL + "min: " + getMinGrade();
			s += NL + " max: " + getMaxGrade();
			return s;
		}
	}

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  list  NOT YET DOCUMENTED
	 * @param  name  NOT YET DOCUMENTED
	 */
	public void showList(List list, String name) {
		prtln("\n" + name);
		if (list == null)
			return;
		for (Iterator i = list.iterator(); i.hasNext(); ) {
			GradeRange gr = (GradeRange) i.next();
			prtln(gr.toStr());
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			SchemEditUtils.prtln(s, "GradeRangeHelper");
		}
	}


	private static void prtlnErr(String s) {
		SchemEditUtils.prtln(s, "GradeRangeHelper");
	}
}

