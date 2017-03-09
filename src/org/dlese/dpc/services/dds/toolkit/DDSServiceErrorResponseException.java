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
package org.dlese.dpc.services.dds.toolkit;

/**
 *  Indicates a standard error response was recieved from a DDS service request.
 *
 * @author    John Weatherley
 */
public final class DDSServiceErrorResponseException extends Exception {
	String code = null;
	String msg = null;
	String serviceRequest = null;


	/**
	 *  Constructor for the DDSServiceErrorResponseException object
	 *
	 * @param  code            The error response code
	 * @param  msg             The error resonse message
	 * @param  serviceRequest  The service request made
	 */
	public DDSServiceErrorResponseException(String code, String msg, String serviceRequest) {
		super("The DDS " + serviceRequest + " request returned an error response (code " + code + "): " + msg);
		this.code = code;
		this.msg = msg;
		this.serviceRequest = serviceRequest;
	}


	/**
	 *  Gets the serviceResponseCode. A value of 'noRecordsMatch' indicates there were no matches for the given search.
	 *
	 * @return    The serviceResponseCode value
	 */
	public String getServiceResponseCode() {
		if (code == null)
			return "";
		else
			return code;
	}


	/**
	 *  Gets the serviceResponseMessage attribute of the DDSServiceErrorResponseException object
	 *
	 * @return    The serviceResponseMessage value
	 */
	public String getServiceResponseMessage() {
		if (msg == null)
			return "";
		else
			return msg;
	}


	/**
	 *  Gets the serviceRequestMade attribute of the DDSServiceErrorResponseException object
	 *
	 * @return    The serviceRequestMade value
	 */
	public String getServiceRequestMade() {
		if (serviceRequest == null)
			return "";
		else
			return serviceRequest;
	}
}

