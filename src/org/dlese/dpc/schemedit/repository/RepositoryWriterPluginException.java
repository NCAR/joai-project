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
package org.dlese.dpc.schemedit.repository;


/**
 *  Indicates a problem occured when attempting to interact with a RepositoryWriterPlugin.
 *
 * @author    Jonathan Ostwald
 * @see       RepositoryWriter
 */
public class RepositoryWriterPluginException extends Exception {
	
	private String pluginName = "";
	private String details = "";
	
	public RepositoryWriterPluginException(String pluginName, String operation) {
		this(pluginName, operation, null, null);	
	}
	
	public RepositoryWriterPluginException(String pluginName, String operation, String details){
		this (pluginName, operation, null, details);

	}
	
	public RepositoryWriterPluginException(String pluginName, String operation, Throwable cause){
		this (pluginName, operation, cause, null);
	}
	
	public RepositoryWriterPluginException(String pluginName, String operation, Throwable cause, String details){
		super (operation, cause);
		this.pluginName = pluginName;
		this.details = details;
	}
	
	public String getPluginName () {
		return this.pluginName;
	}
	
	public String getDetails () {
		return this.details;
	}
	
	private String getOperation () {
		String op = super.getMessage();
		return (op != null && op.trim().length() > 0 ? op : "Unkown Operation");
	}
	
	public String getMessage () {
 		String operation = getOperation();
		String cause = null;
		try {
			cause = this.getCause().getMessage();
		} catch (Throwable t) {}
		if (cause == null)
			cause = "Unknown Cause";

		return operation + ": " + cause;
/*		
 		String msg = super.getMessage();
		return (msg != null ? msg + " error" : "Unknown error");
*/
	}
	
	public String getSyncMessage () {
		return this.getPluginName() + ": " + this.getMessage();
	}
	
	public String toString () {
		String s = this.pluginName;
		if (this.getMessage() != null)
			s += ": " + this.getMessage();
		if (this.getCause() != null)
			s += " (" + this.getCause() + ")";
		return s;
	}
	
	// public RepositoryWriterPluginException(){}
}

