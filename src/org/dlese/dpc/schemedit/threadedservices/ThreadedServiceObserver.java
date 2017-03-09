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
package org.dlese.dpc.schemedit.threadedservices;

/**
 *  This interface is used by Objects wishing to determine when a background threaded service process (E.g., Export or Validation)
 has completed and to perform additional processing at that time.
 *

 */
public interface ThreadedServiceObserver {

	/**  Indicates that indexing completed normally */
	public final static int SERVICE_COMPLETED_SUCCESS = 1;

	/**  Indicates that indexing was aborted by request */
	public final static int SERVICE_COMPLETED_ABORTED = 2;

	/**  Indicates that indexing completed with a severe error */
	public final static int SERVICE_COMPLETED_ERROR = 3;

	/**  Indicates that one or more of the indexing directories does not exist */
	public final static int SERVICE_COMPLETED_DIR_DOES_NOT_EXIST = 4;
		
	
	/**
	 *  This method is called when the service is complete. This method may then do additional processing that
	 *  is required after indexing and will execute within the same indexing thread, thus blocking all other
	 *  indexing operations until this method is returned.
	 *
	 * @param  status   The status code upon completion
	 * @param  message  A message describing how the indexer completed
	 */
	public void serviceCompleted(int status, String message);

}

