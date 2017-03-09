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
package org.dlese.dpc.schemedit;


/**
 * Schemedit Constants
 */

public final class Constants {

    /**
     * The session scope attribute under which the User object
     * for the currently logged in user is stored.
     */
    public static final String USER_KEY = "user";
	
	public static final String UNKNOWN_USER = "Unknown";
	
	public static final String UNKNOWN_VALIDITY = "Unknown";
	
	public static final String UNKNOWN_EDITOR = "Unknown";

	/**  Used to sort in ascending order. */
	public final static int ASCENDING = 0;
	/**  Used to sort in descending order. */
	public final static int DESCENDING = 1;
	
	public final static String ASN_PURL_BASE = "http://purl.org/ASN/resources/";
	
}
