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
package org.dlese.dpc.services.idmapper;


/**
 * A Thread that simply invokes PageDesc.processPage.
 */

class ScanThread extends Thread {

int bugs;
ThreadGroup threadgrp;
int threadId;
int timeoutSeconds;
PageDesc page;


ScanThread(
	int bugs,
	ThreadGroup thdgrp,
	int threadId,
	int timeoutSeconds,
	PageDesc page)
{
	super( thdgrp, "" + threadId);
	this.bugs = bugs;
	this.threadgrp = threadgrp;
	this.threadId = threadId;
	this.timeoutSeconds = timeoutSeconds;
	this.page = page;
}

public void run() {
	page.processPage( bugs, threadId, timeoutSeconds);
}


} // end class ScanThread

