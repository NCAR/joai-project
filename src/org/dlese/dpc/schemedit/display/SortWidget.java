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
package org.dlese.dpc.schemedit.display;

import org.dlese.dpc.schemedit.Constants;
import java.util.*;

/**
 *  SortWidgets are used to display lists of search results, and to render the
 *  controls for changing the field upon which results are sorted, as well as
 *  the order in which the results are sorted. A SortWidget is associated with
 *  each sortable field.
 *
 *@author    ostwald
 */
public class SortWidget {
	private static boolean debug = true;

	private String label;
	private String fieldName;
	private int order = -1;
	private int defaultOrder = -1;


	/**
	 *  Constructor for the SortWidget object
	 *
	 *@param  fieldName  Description of the Parameter
	 *@param  label      Description of the Parameter
	 */
	public SortWidget(String fieldName, String label) {
		this(fieldName, label, -1);
	}


	/**
	 *  Constructor for the SortWidget object, where the sort order is explicitly
	 *  defined.
	 *
	 *@param  fieldName  Description of the Parameter
	 *@param  label      Description of the Parameter
	 *@param  order      Description of the Parameter
	 */
	public SortWidget(String fieldName, String label, int order) {
		this.label = label;
		this.fieldName = fieldName;
		if (order < 0 || order > 1) {
			this.order = getDefaultOrder();
		} else {
			this.order = order;
			this.defaultOrder = order;
		}
	}


	/**
	 *  Gets the label attribute of the SortWidget object
	 *
	 *@return    The label value
	 */
	public String getLabel() {
		return label;
	}


	/**
	 *  Gets the fieldName attribute of the SortWidget object
	 *
	 *@return    The fieldName value
	 */
	public String getFieldName() {
		return fieldName;
	}


	/**
	 *  Specifies the sort order for this wid
	 *
	 *@return    The order value
	 */
	public int getOrder() {
		return order;
	}


	/**
	 *  Sets the order attribute of the SortWidget object
	 *
	 *@param  order  The new order value
	 */
	public void setOrder(int order) {
		if (order < 0 || order > 1) {
			this.order = getDefaultOrder();
		} else {
			this.order = order;
		}
	}


	/**
	 *  Gets the image attribute of the SortWidget object
	 *
	 *@return    The image value
	 */
	public String getImage() {
		if (order == Constants.ASCENDING) {
			return "ascending.gif";
		} else {
			return "descending.gif";
		}
	}


	/**
	 *  Gets the defaultOrder attribute of the SortWidget object.
	 *
	 *@return    The defaultOrder value
	 */
	public int getDefaultOrder() {
		if ((defaultOrder == Constants.DESCENDING) ||
				(defaultOrder == Constants.ASCENDING)) {
			return defaultOrder;
		}
		return Constants.ASCENDING;
	}


	/**
	 *  Gets the otherOrder attribute of the SortWidget object
	 *
	 *@return    The otherOrder value
	 */
	public int getOtherOrder() {
		if (order == Constants.ASCENDING) {
			return Constants.DESCENDING;
		} else {
			return Constants.ASCENDING;
		}
	}
}


