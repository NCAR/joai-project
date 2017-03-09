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
 *  Indicates a problem occured when attempting to add, modify or delete a
 *  collection in the repository.
 *
 * @author    John Weatherley
 * @see       RepositoryManager
 */
public class PutCollectionException extends Exception {
	
	public static final String ERROR_CODE_COLLECTION_EXISTS_IN_ANOTHER_FORMAT = "COLLECTION_EXISTS_IN_ANOTHER_FORMAT";
	public static final String ERROR_CODE_BAD_FORMAT_SPECIFIER = "BAD_FORMAT_SPECIFIER";
	public static final String ERROR_CODE_BAD_KEY = "BAD_KEY";
	public static final String ERROR_CODE_BAD_TITLE = "BAD_TITLE";
	public static final String ERROR_CODE_BAD_ADDITIONAL_METADATA = "BAD_ADDITIONAL_METADATA";
	public static final String ERROR_CODE_IO_ERROR = "IO_ERROR";
	public static final String ERROR_CODE_INTERNAL_ERROR = "INTERNAL_ERROR";
	
	private String _errorCode;
	PutCollectionException(String message, String errorCode){
		super(message);
		_errorCode = errorCode; 
	}
	
	public String getErrorCode(){
		return _errorCode;
	}
}

