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
package org.dlese.dpc.repository;

/**
 *  Indicates an OAI error.
 *
 * @author     John Weatherley
 * @version    $Id: OAIErrorException.java,v 1.2 2009/03/20 23:33:54 jweather Exp $
 * @see        org.dlese.dpc.oai.OAICodes
 */
public class OAIErrorException extends Exception {
	
	OAIErrorException(String message, String oaiErrorCode) {
		super(message);
		errorCode = oaiErrorCode;
	}


	OAIErrorException() { }


	private String errorCode = null;


	/**
	 *  Gets the errorCode attribute of the OAIErrorException object
	 *
	 * @return    The errorCode value
	 */
	public String getErrorCode() {
		return errorCode;
	}
}
