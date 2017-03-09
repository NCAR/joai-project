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
package org.dlese.dpc.schemedit.ndr.writer;

import org.dlese.dpc.schemedit.repository.RepositoryWriterPluginException;


/**
 *  Indicates a problem occured when attempting to interact with a RepositoryWriterPlugin.
 *
 * @author    Jonathan Ostwald
 * @see       RepositoryWriter
 */
public class NdrRepositoryWriterPluginException extends RepositoryWriterPluginException {
	
	private String pluginName = "";
	private String details = "";
	
	public NdrRepositoryWriterPluginException(String operation){
		this(operation, null, null);	
	}
	
	public NdrRepositoryWriterPluginException(Throwable cause) {
		this(null, cause, null);
	}
	
	public NdrRepositoryWriterPluginException(String operation, String details) {
		this(operation, null, details);
	}
	
	public NdrRepositoryWriterPluginException(String operation, Throwable cause) {
		this (operation, cause, null);
	}
	
	public NdrRepositoryWriterPluginException(String operation, Throwable cause, String details) {
		super("NdrPlugin", operation, cause, details);
	}	

	// public RepositoryWriterPluginException(){}
}

