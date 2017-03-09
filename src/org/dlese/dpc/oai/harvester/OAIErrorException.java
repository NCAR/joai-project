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
package org.dlese.dpc.oai.harvester;

/**
 *  Indicates an <a href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ErrorConditions">
 *  OAI protocol error code</a> was returned by the data provider during a harvest.
 *
 * @author    John Weatherley
 */
public class OAIErrorException extends Exception {
	String errorCode = null;
	String errorMsg = null;


	/**
	 *  Constructor for the OAIErrorException object
	 *
	 * @param  errorCode  The OAI error code.
	 * @param  errorMsg   Description of the error.
	 */
	public OAIErrorException(String errorCode, String errorMsg) {
		super("OAI code '" + errorCode + "' was returned by the data provider. Message: " + errorMsg);
		this.errorCode = errorCode;
		this.errorMsg = errorMsg;
	}


	/**
	 *  Gets the oAIErrorCode attribute of the OAIErrorException object
	 *
	 * @return    The oAIErrorCode value
	 */
	public String getOAIErrorCode() {
		return errorCode;
	}


	/**
	 *  Gets the oAIErrorMessage attribute of the OAIErrorException object
	 *
	 * @return    The oAIErrorMessage value
	 */
	public String getOAIErrorMessage() {
		return errorMsg;
	}

}

