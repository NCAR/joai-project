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
 *  Interface that receives an event message whenever an OAI record file is created, modified, exists but not modified, or deleted.
 *
 */
public interface OAIChangeListener {

	/**
	 *  This method is called whenever an OAI record file is deleted.
	 *
	 * @param  recordFilePath   The record file absolute path
	 * @param  identifier  The OAI identifier
	 */
	public void onRecordDelete(final String recordFilePath, final String identifier);


	/**
	 *  This method is called whenever an OAI record file is created.
	 *
	 * @param  recordFilePath   The record file absolute path
	 * @param  identifier  The OAI identifier
	 */
	public void onRecordCreate(final String recordFilePath, final String identifier);


	/**
	 *  This method is called whenever an OAI record file has changed.
	 *
	 * @param  recordFilePath   The record file absolute path
	 * @param  identifier  The OAI identifier
	 */
	public void onRecordChange(final String recordFilePath, final String identifier);
	
	/**
	 *  This method is called whenever an OAI record file previously exists but has not changed.
	 *
	 * @param  recordFilePath   The record file absolute path
	 * @param  identifier  The OAI identifier
	 */
	public void onRecordExistsNoChange(final String recordFilePath, final String identifier);
	
}

