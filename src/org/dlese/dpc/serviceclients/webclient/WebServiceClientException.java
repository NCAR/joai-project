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
package org.dlese.dpc.serviceclients.webclient;

import java.lang.*;

/**
 * Indicates that a WebClient received an error from a DDS Web Service 
 *
 * @author	Jonathan Ostwald
 */
public class WebServiceClientException extends Exception {

	/**
	 * Constructs an <code>Exception</code> with no specified detail message. 
	 */
	public WebServiceClientException() {
		super();
	}
	
	/**
	 * Constructs an <code>Exception</code> with the specified detail message. 
	 *
	 * @param message   the detailed message.
	 */
	public WebServiceClientException(String message) {
		super(message);
	}
	
}
