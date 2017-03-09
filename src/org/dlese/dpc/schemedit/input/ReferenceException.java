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
package org.dlese.dpc.schemedit.input;

import java.text.ParseException;

/**
 *  Stores information about a problem trying to resolve a numeric or character
 *  reference within form input. Description of the Class
 *
 *@author    ostwald
 */
public class ReferenceException extends ParseException {
	/**
	 *  A string representation of a character or numerical reference (e.g.,
	 *  &Delta; or &#00a9;)
	 */
	public String ref;


	/**
	 *  Constructor for the ReferenceException object
	 *
	 *@param  ref          The unresolvable reference
	 *@param  msg          The reason the reference could not be resolved
	 *@param  errorOffset  Offset to the unresolvable reference within the input
	 *      string
	 */
	public ReferenceException(String ref, String msg, int errorOffset) {
		super(msg, errorOffset);
		this.ref = ref;
	}


	/**
	 *  Gets an error message describing the unresolvable reference, prividing the
	 *  reference, the reason why it can't be resolved, and the position within the
	 *  text of the reference.
	 *
	 *@return    The errorMessage value
	 */
	public String getErrorMessage() {
		int errorPosition = getErrorOffset() + 1;
		// escape reference so it will display properly in browser
		String entityRef = "&amp;" + ref.substring(1);
		return ("\"" + entityRef + "\" (at position " + errorPosition + "): " + getMessage());
	}
}

