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
package org.dlese.dpc.services.cas;


/**
 *  Represents one email address and the records that it is associated with *
 * @author     Sonal Bhushan
 * @created    October 21, 2004
 */
class EmailRecs {

		private String address;
		private String type;
		Rec[] recs;
		private int numRecs;




	/**
	 * Constructor 
	 */
	EmailRecs() {
		this.address = null;
		this.type = null;
		this.recs = null;
		this.numRecs = 0;
	}



	String getAddress() {
		return this.address;
	}


	void setAddress(String add) {
		this.address = add;
	}



	String getType() {
		return this.type;
	}



	void setType(String t) {
		this.type = t;
	}



	void addRec(Rec r) {
		if (numRecs == 0) {
			recs = new Rec[]{r};
			numRecs = 1;
		}
		else {
			Rec[] newrecs = new Rec[numRecs + 1];
			System.arraycopy(recs, 0, newrecs, 0, numRecs);
			newrecs[numRecs] = r;
			recs = newrecs;
			numRecs++;
		}
	}



	
	int getNumRecs(){
		return this.numRecs;	
	}
	


}

