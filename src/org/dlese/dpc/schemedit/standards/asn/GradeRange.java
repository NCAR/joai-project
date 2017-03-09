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

/**
 *  Bean to store the min and max grade levels associated with a controlled
 *  vocab expressing a grade range.
 *
 * @author    Jonathan Ostwald
 */
public class GradeRange {
	private int maxGrade;
	private int minGrade;


	/**
	 *  Constructor for the GradeRangeItem object
	 *
	 * @param  grade  NOT YET DOCUMENTED
	 */
	public GradeRange(int grade) {
		this(grade, grade);
	}


	/**
	 *  Constructor for the GradeRangeItem object
	 *
	 * @param  minGrade  NOT YET DOCUMENTED
	 * @param  maxGrade  NOT YET DOCUMENTED
	 */
	public GradeRange(int minGrade, int maxGrade) {
		this.minGrade = minGrade;
		this.maxGrade = maxGrade;
	}


	/**
	 *  Determines if the extents of this GradeRange contain those of provided gradeRange.
	 *
	 * @param  other  other GradeRange
	 * @return        true if this GradeRange contains other
	 */
	public boolean contains(GradeRange other) {
		return (this.minGrade <= other.minGrade &&
			this.maxGrade >= other.maxGrade);
	}


	/**
	 *  Sets the minGrade attribute of the GradeRange object
	 *
	 * @param  grade  The new minGrade value
	 */
	public void setMinGrade(int grade) {
		this.minGrade = grade;
	}


	/**
	 *  Gets the minGrade attribute of the GradeRange object
	 *
	 * @return    The minGrade value
	 */
	public int getMinGrade() {
		return this.minGrade;
	}


	/**
	 *  Sets the maxGrade attribute of the GradeRange object
	 *
	 * @param  grade  The new maxGrade value
	 */
	public void setMaxGrade(int grade) {
		this.maxGrade = grade;
	}


	/**
	 *  Gets the maxGrade attribute of the GradeRange object
	 *
	 * @return    The maxGrade value
	 */
	public int getMaxGrade() {
		return this.maxGrade;
	}



	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String toStr() {
		return "min: " + minGrade + " max: " + maxGrade;
	}
}

