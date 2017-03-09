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
package org.dlese.dpc.util;

public class RWLock {

	/*
	 * The lock variable.
	 * If == 0, there are no readers or writers.
	 * If > 0, there are n readers and no writers;
	 * If < 0, there are -n writers (at most 1).
	 */
	private int numusers;
	private int bugs = 0;

	
	public int getNumusers()
	{
		return numusers;
	}
	
	
	public synchronized void getRead() {
		if (bugs >= 1) prtln("RWLock.getRead: before wait");
		while (numusers < 0) {
			try { wait(); }
			catch( InterruptedException iex) {}
		}
		numusers++;
		if (bugs >= 1) prtln(
			"RWLock.getRead: got it.  New numusers: " + numusers);
	}


	public synchronized void freeRead() {
		numusers--;
		if (bugs >= 1) prtln(
			"RWLock.freeRead: New numusers: " + numusers);
		notifyAll();
	}


	public synchronized void getWrite() {
		if (bugs >= 1) prtln("RWLock.getWrite: before wait");
		while (numusers != 0) {
			try { wait(); }
			catch( InterruptedException iex) {}
		}
		numusers--;
		if (bugs >= 1) prtln(
			"RWLock.getWrite: got it.  New numusers: " + numusers);
	}


	public synchronized void freeWrite() {
		numusers++;
		if (bugs >= 1) prtln(
			"RWLock.freeWrite: New numusers: " + numusers);
		notifyAll();
	}


	private void prtln( String msg) {
		System.out.println( msg);
	}
}
