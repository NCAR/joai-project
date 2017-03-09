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
package org.dlese.dpc.oai;

/**
 *  An internal data structure that holds an individual OAI error code and a
 *  human-readable description that accompanies it.
 *
 * @author    John Weatherley
 */
public class OAIError {
	private String errorCode, message;


	/**
	 *  Constructor for the OAIError object
	 *
	 * @param  message    A human-readable message that describes this error.
	 * @param  errorCode  The OAI ErrorCode for this error.
	 */
	public OAIError(String errorCode, String message) {
		this.errorCode = errorCode;
		this.message = message;
	}


	/**
	 *  Gets the errorCode attribute of the OAIError object
	 *
	 * @return    The errorCode value
	 */
	public String getErrorCode() {
		return errorCode;
	}


	/**
	 *  Gets the message attribute of the OAIError object
	 *
	 * @return    The message value
	 */
	public String getMessage() {
		return message;
	}
}

