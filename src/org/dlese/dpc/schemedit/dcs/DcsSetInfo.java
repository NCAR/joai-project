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
package org.dlese.dpc.schemedit.dcs;

import org.dlese.dpc.schemedit.config.StatusFlag;
import org.dlese.dpc.repository.*;
import java.util.*;

/**
 *  Extends SetInfo to provide DCS-specific information about sets (aka collections), such as the number of valid records. 
 *
 *@author    Jonathan Ostwald
 */
public class DcsSetInfo extends SetInfo {
	int numDone = -1, numWorking = -1, numValid = -1, numNotValid = -1, 
		numSyncErrors = -1, numFinalAndNotValid = -1;
	static String FIELD_NS = DcsDataFileIndexingPlugin.FIELD_NS;
	String exampleId = null;
	String idPrefix = null;
	String authority = null;
	StatusFlag finalStatusFlag = null;
	long uniqueID = 0;


	/**
	 *  Constructor for the DcsSetInfo object
	 *
	 *@param  setInfo  Description of the Parameter
	 */
	public DcsSetInfo(SetInfo setInfo) {
		super(setInfo.getName(), setInfo.getSetSpec(), setInfo.getDescription(),
				setInfo.getEnabled(), setInfo.getDirectory(), setInfo.getFormat(), setInfo.getId());
		// constructor for SetInfo generates a new uniqueID for THIS instance, so we have to explicitly
		// set it back to the uniqueID of the provided SetInfo instance.
		setUniqueID(setInfo.getUniqueID());
	}


	public void setUniqueID (String id) {
		uniqueID = Long.parseLong(id);
	}
	
	/**
	* Overide method from SetInfo
	*/
	public String getUniqueID () {
		return Long.toString(uniqueID);
	}
	
	public int getNumValid () {
		return numValid;
	}
	
	public int getNumNotValid () {
		return numNotValid;
	}
	
	public int getNumSyncErrors () {
		return numSyncErrors;
	}
	
	/**
	 *  Gets the numFiles attribute of the SetInfo object
	 *
	 *@return    The numFiles value
	 */
	public int getNumDoneInt() {
		return numDone;
	}


	/**
	 *  Gets the numDone attribute of the DcsSetInfo object
	 *
	 *@return    The numDone value
	 */
	public String getNumDone() {
		return Integer.toString(numDone);
	}

	/**
	 *  Gets the numFiles attribute of the SetInfo object
	 *
	 *@return    The numFiles value
	 */
	public int getNumWorkingInt() {
		return numWorking;
	}
	
	public int getNumFinalAndNotValid () {
		return numFinalAndNotValid;
	}
	
	/**
	 *  Gets the numWorking attribute of the DcsSetInfo object
	 *
	 *@return    The numWorking value
	 */
	public String getNumWorking() {
		return Integer.toString(numWorking);
	}

	public String getIdPrefix () {
		return idPrefix;
	}
	
	public void setIdPrefix (String id) {
		idPrefix = id;
	}

	public String getAuthority () {
		return authority;
	}
	
	public void setAuthority (String auth) {
		authority = auth;
	}
	
	public StatusFlag getFinalStatusFlag () {
		return finalStatusFlag;
	}
	
	public void setFinalStatusFlag (StatusFlag flag) {
		finalStatusFlag = flag;
	}
	
	public String getExampleId () {
		return exampleId;
	}
	
	public void setExampleId (String id) {
		exampleId = id;
	}
	
	/**
	 *  Adds DcsSetInfo properties to those of repository.SetInfo for this DcsSetInfo instance.
	 *
	 *@param  rm  The new setInfoData value
	 */
	public void setSetInfoData(RepositoryManager rm) {
		super.setSetInfoData(rm);
		numDone = rm.getIndex().getNumDocs("collection:0" + getSetSpec() + " AND " + FIELD_NS + "status:Accessioned");
		String q = "collection:0" + getSetSpec() + " NOT " + FIELD_NS + "status:Accessioned";
		numWorking = rm.getIndex().getNumDocs(q);
		numValid = rm.getIndex().getNumDocs("collection:0" + getSetSpec() + " AND " + FIELD_NS + "isValid:true");
		numNotValid = rm.getIndex().getNumDocs("collection:0" + getSetSpec() + " NOT " + FIELD_NS + "isValid:true");
		numFinalAndNotValid = rm.getIndex().getNumDocs("collection:0" + getSetSpec() + 
			" NOT " + FIELD_NS + "isValid:true" + " AND " + FIELD_NS + "isFinalStatus:true");
		this.numSyncErrors = rm.getIndex().getNumDocs("collection:0" + getSetSpec() + " AND " + FIELD_NS + "hasSyncError:true");
	}


	/**
	 *  Gets the comparator attribute of the DcsSetInfo class
	 *
	 *@param  type  Description of the Parameter
	 *@return       The comparator value
	 */
	public static Comparator getComparator(String type) {
		if (type.equals("numValid")) {
			return new NumValidComparator();
		}
		if (type.equals("numNotValid")) {
			return new NumNotValidComparator();
		}		
		if (type.equals("numWorking")) {
			return new NumWorkingComparator();
		}
		else if (type.equals("numDone")) {
			return new NumDoneComparator();
		}
		else if (type.equals("numSyncErrors")) {
			return new NumSyncErrorsComparator();
		}	
		else {
			return SetInfo.getComparator(type);
		}
	}
	
	/**
	 *  Implements Comparator to enable sorting by numDone.
	 *
	 *@author    John Weatherley
	 */
	public static class NumDoneComparator implements Comparator {
		/**
		 *  Campares the numDone field.
		 *
		 *@param  O1                      A SetInfo Object
		 *@param  O2                      A SetInfo Object
		 *@return                         A negative integer, zero, or a positive
		 *      integer as the first argument is less than, equal to, or greater than
		 *      the second.
		 *@exception  ClassCastException  If Object is not SetInfo
		 */
		public int compare(Object O1, Object O2)
			throws ClassCastException {
			int one = ((DcsSetInfo) O1).getNumDoneInt();
			int two = ((DcsSetInfo) O2).getNumDoneInt();
			if (one == two) {
				return 0;
			}
			if (one > two) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}

	/**
	 *  Implements Comparator to enable sorting by numDone.
	 *
	 *@author    John Weatherley
	 */
	public static class NumSyncErrorsComparator implements Comparator {
		/**
		 *  Campares the numDone field.
		 *
		 *@param  O1                      A SetInfo Object
		 *@param  O2                      A SetInfo Object
		 *@return                         A negative integer, zero, or a positive
		 *      integer as the first argument is less than, equal to, or greater than
		 *      the second.
		 *@exception  ClassCastException  If Object is not SetInfo
		 */
		public int compare(Object O1, Object O2)
			throws ClassCastException {
			int one = ((DcsSetInfo) O1).getNumSyncErrors();
			int two = ((DcsSetInfo) O2).getNumSyncErrors();
			if (one == two) {
				return 0;
			}
			if (one > two) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}

	
	public static class NumValidComparator implements Comparator {
		/**
		 *  Campares the numDone field.
		 *
		 *@param  O1                      A SetInfo Object
		 *@param  O2                      A SetInfo Object
		 *@return                         A negative integer, zero, or a positive
		 *      integer as the first argument is less than, equal to, or greater than
		 *      the second.
		 *@exception  ClassCastException  If Object is not SetInfo
		 */
		public int compare(Object O1, Object O2)
			throws ClassCastException {
			int one = ((DcsSetInfo) O1).getNumValid();
			int two = ((DcsSetInfo) O2).getNumValid();
			if (one == two) {
				return 0;
			}
			if (one > two) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}

	public static class NumNotValidComparator implements Comparator {
		/**
		 *  Campares the numDone field.
		 *
		 *@param  O1                      A SetInfo Object
		 *@param  O2                      A SetInfo Object
		 *@return                         A negative integer, zero, or a positive
		 *      integer as the first argument is less than, equal to, or greater than
		 *      the second.
		 *@exception  ClassCastException  If Object is not SetInfo
		 */
		public int compare(Object O1, Object O2)
			throws ClassCastException {
			int one = ((DcsSetInfo) O1).getNumNotValid();
			int two = ((DcsSetInfo) O2).getNumNotValid();
			if (one == two) {
				return 0;
			}
			if (one > two) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}


	
	/**
	 *  Implements Comparator to enable sorting by numWorking.
	 *
	 *@author    John Weatherley
	 */
	public static class NumWorkingComparator implements Comparator {
		/**
		 *  Campares the numWorking field.
		 *
		 *@param  O1                      A DcsSetInfo Object
		 *@param  O2                      A DcsSetInfo Object
		 *@return                         A negative integer, zero, or a positive
		 *      integer as the first argument is less than, equal to, or greater than
		 *      the second.
		 *@exception  ClassCastException  If Object is not DcsSetInfo
		 */
		public int compare(Object O1, Object O2)
			throws ClassCastException {
			int one = ((DcsSetInfo) O1).getNumWorkingInt();
			int two = ((DcsSetInfo) O2).getNumWorkingInt();
			if (one == two) {
				return 0;
			}
			if (one > two) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}
}

