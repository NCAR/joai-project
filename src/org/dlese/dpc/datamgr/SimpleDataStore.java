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
package org.dlese.dpc.datamgr;

import java.util.*;
import java.io.*;

/**
 *  A Hashtable-like interface for reading and writing persistent Java objects from and to
 *  disc. Same as SerializedDataManager except the getter and setter methods return null insted
 *  of throwing exceptions. Objects are stored in serialized form on disc - any object
 *  that implements {@link java.io.Serializable} can be stored in this DataManager for
 *  persistent retrieval. The objects can also be held in a RAM cache to provide faster
 *  retrieval. Serialized objects are stored in a directory specified at the time of
 *  construction. Objects stored in a SimpleDataStore can be of mixed type. This class is
 *  safe to use with multiple process and/or JVMs that access the same dataPath
 *  concurrently.
 *
 * @author    John Weatherley
 */
public class SimpleDataStore extends SerializedDataManager implements Serializable {

	/**
	 *  Constructs a new SimpleDataStore that reads and writes data to the given dataPath
	 *  directory. Allows control over whether or not to hold a cache of the objects in RAM
	 *  memory for faster retrieval. Note that if caching is turned on in this
	 *  SimpleDataStore, the objects that are returned from the {@link #get(String)} and
	 *  {@link #get(List)} methods are not safe to modify. Specifically, any changes made to
	 *  the objects will be reflected in subsequent calls to those methods. To get a copy of
	 *  the data items that are safe to modify the caller should use the {@link
	 *  #getCopy(String)} or {@link #getCopy(List)} methods instead. If caching is not turned
	 *  on, the effect of the get methods are the same as the getCopy methods.<p>
	 *
	 *  This class is safe to use with multiple process and/or JVMs that access the same
	 *  dataPath.
	 *
	 * @param  dataPath       The directory where the serialzed objects are stored.
	 * @param  useCache       Indicates whether to cache the objects in RAM memory for faster
	 *      retrievel.
	 * @exception  Exception  If error.
	 */
	public SimpleDataStore(String dataPath, boolean useCache)
		 throws Exception {
		super(dataPath, useCache);
	}


	/**  Constructor for the SimpleDataStore object, restores a serialized SimpleDataStore. */
	public SimpleDataStore() { }


	/**
	 *  Retrieves a single data object, or null if none exists or unable to retrieve. Note
	 *  that if caching is turned on in this SimpleDataStore, the object that is returned is
	 *  not safe to modify. Specifically, if caching is enabled, any changes made to that
	 *  object will be reflected in subsequent calls to this method and the {@link
	 *  #get(List)} method. To get a copy of a data item that is safe to modify use the
	 *  {@link #getCopy(String)} method instead. If caching is not enabled, the effect of
	 *  this method is the same as {@link #getCopy(String)}.
	 *
	 * @param  oid  The data identifier
	 * @return      <code>Object</code> of data or null.
	 * @see         #getCopy(String)
	 */
	public synchronized Object get(String oid) {
		Object o = null;

		try {
			o = restoreSerializedObject(oid, useCache);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return o;
	}


	/**
	 *  Retrieves a copy of a single data object, sutable for modifying, or null if none
	 *  exists or unable to retrieve. The object that is returned is safe for modifying
	 *  without affecting the data that is in this SimpleDataStore.
	 *
	 * @param  oid  The data identifier.
	 * @return      A copy of an <code>Object</code> of data that is in this SimpleDataStore
	 *      or null.
	 * @see         #get(String)
	 */
	public synchronized Object getCopy(String oid) {
		Object o = null;

		try {
			o = restoreSerializedObject(oid, false);
		} catch (Throwable ioe) {}
		return o;
	}


	/**
	 *  Retrieves a <code>List</code> of data objects. Note that if caching is turned on in
	 *  this SimpleDataStore, the objects that are returned are not safe to modify.
	 *  Specifically, if caching is enabled, any changes made to the objects will be
	 *  reflected in subsequent calls to this method and the {@link #get(String)} method. To
	 *  get a copy of a data items that are safe to modify use the {@link #getCopy(List)}
	 *  method instead. If caching is not ebabled, the effect of this method is the same as
	 *  {@link #getCopy(List)}.
	 *
	 * @param  oids  <code>List</code> of data identifiers
	 * @return       <code>List</code> of corresponding data objects
	 * @see          #getCopy(List)
	 */
	public synchronized List get(List oids) {
		List list = new ArrayList(oids.size());

		for (int i = 0; i < oids.size(); i++) {
			Object o = this.get((String) oids.get(i));
			if (o != null)
				list.add(o);
		}

		return list;
	}


	/**
	 *  Retrieves a <code>List</code> of copied data objects, suitable for modifying. The
	 *  objects that are returned are safe for modifying without affecting the data that is
	 *  in this SimpleDataStore.
	 *
	 * @param  oids  <code>List</code> of data identifiers.
	 * @return       <code>List</code> of corresponding data objects copies.
	 * @see          #get(List)
	 */
	public synchronized List getCopy(List oids) {
		List list = new ArrayList(oids.size());

		for (int i = 0; i < oids.size(); i++) {
			Object o = this.getCopy((String) oids.get(i));
			if (o != null)
				list.add(o);
		}

		return list;
	}




	/**
	 *  Removes an existing data object, assuming the caller had requested and recieved the
	 *  necessary lock for the object. After successful completion the object is removed from
	 *  the DataManager and returned to the caller.
	 *
	 * @param  oid      The unique identifier that references the data object to be removed
	 * @param  lockKey  The key corresponding to the lock on this data object
	 * @return          The removed data object iff successful, otherwise null
	 */
	public synchronized Object remove(String oid, String lockKey) {
		if (!isLocked(oid))
			return null;

		if (!isValidLock(oid, lockKey))
			return null;

		Object o = null;
		//String encodedFileName = encodeFileName(oid);
		try {
			o = restoreSerializedObject(oid, useCache);
		} catch (Throwable e) {
			return null;
		}

		File f = new File(dataPath + "/" + encodeFileName(oid));
		f.delete();
		locks.remove(oid);
		num_records--;
		return o;
	}


	/**
	 *  Removes an existing data object iff the object is not locked by another user. If the
	 *  object is locked a LockNotAvailableException is thrown. After successful completion
	 *  the object is removed from the DataManager and returned to the caller.
	 *
	 * @param  oid  The unique identifier that references the data object to be removed
	 * @return      The Object that was removed, or null.
	 */
	public synchronized Object remove(String oid) {
		if (isLocked(oid))
			return null;

		Object o = null;
		//String encodedFileName = encodeFileName(oid);
		try {
			o = restoreSerializedObject(oid, useCache);
		} catch (Throwable e) {
			return null;
		}

		File f = new File(dataPath + "/" + encodeFileName(oid));
		f.delete();
		num_records--;
		return o;
	}


	/**
	 *  Deletes an existing data object, assuming the caller had requested and recieved the
	 *  necessary lock. Similar to <code>remove()</code> except no object is returned and
	 *  thus is more efficient if an object is not required.
	 *
	 * @param  oid      The unique identifier that references the data object to be deleted
	 * @param  lockKey  The key corresponding to the lock on this data object
	 * @return          True if object was successfully deleted, otherwise false.
	 */
	public synchronized boolean delete(String oid, String lockKey) {
		File f = new File(dataPath + "/" + encodeFileName(oid));
		if (!f.isFile())
			return false;

		if (!isValidLock(oid, lockKey))
			return false;

		f.delete();
		locks.remove(oid);
		num_records--;
		return true;
	}


	/**
	 *  Deletes an existing data object iff the object is not locked by another user. If the
	 *  object is locked a LockNotAvailableException is thrown. Similar to <code>remove()</code>
	 *  except no object is returned and thus is more efficient. After successful completion
	 *  the object is deleted from the DataManager.
	 *
	 * @param  oid  The unique identifier that references the data object to be deleted
	 * @return      True if object was successfully deleted, otherwise false.
	 */
	public synchronized boolean delete(String oid) {
		File f = new File(dataPath + "/" + encodeFileName(oid));
		if (!f.isFile())
			return false;
		if (isLocked(oid))
			return false;

		f.delete();
		num_records--;
		return true;
	}


	/**
	 *  Adds a new object of data, replacing the existing one if one exists and is not
	 *  locked. This method is safe for concurrent use among multiple processes and JVMs
	 *  accessing the same SimpleDataStore.
	 *
	 * @param  oid  The unique identifier that references the new data object
	 * @param  obj  The new data object
	 * @return      The added data object iff successful, otherwise null
	 */
	public synchronized Object put(String oid, Object obj) {
		delete(oid);
		String encodedFilePath = dataPath + "/" + encodeFileName(oid);
		File f = new File(encodedFilePath);
		if (f.isFile())
			return null;

		try {
			if (useFileLocks)
				getLock(encodedFilePath);
			serailizeObject(encodedFilePath, obj);
		} catch (Throwable e) {
			return null;
		} finally {
			if (useFileLocks)
				releaseLock(encodedFilePath);
		}

		num_records++;
		return obj;
	}


	/**
	 *  Adds a new object of data, replacing the existing one if one exists and it can be
	 *  unlocked with the given key. The caller retains the lock after this method returns.
	 *  This method is safe for concurrent use among multiple processes and JVMs accessing
	 *  the same SimpleDataStore.
	 *
	 * @param  oid      The unique identifier that references the new data object
	 * @param  obj      The new data object
	 * @param  lockKey  The key corresponding to the lock on this data object
	 * @return          The added data object iff successful, otherwise null
	 */
	public synchronized Object put(String oid, Object obj, String lockKey) {
		delete(oid, lockKey);
		String encodedFilePath = dataPath + "/" + encodeFileName(oid);
		File f = new File(encodedFilePath);
		if (f.isFile())
			return null;

		try {
			if (useFileLocks)
				getLock(encodedFilePath);
			serailizeObject(encodedFilePath, obj);
		} catch (Throwable e) {
			return null;
		} finally {
			if (useFileLocks)
				releaseLock(encodedFilePath);
		}

		num_records++;
		return obj;
	}


	/**
	 *  Updates a new object of data, assuming the caller had requested and recieved the
	 *  necessary lock. The caller retains the lock after this method returns.
	 *
	 * @param  oid      The unique identifier that references the data object to be updated
	 * @param  obj      The new updated data object
	 * @param  lockKey  The key corresponding to the lock on this data object
	 * @return          The updated data object, if successful otherwise null
	 */
	public synchronized Object update(String oid, Object obj, String lockKey) {
		return put(oid, obj, lockKey);
	}


	/**
	 *  Updates a new object of data iff the object is not locked by another user. If the
	 *  object is locked a LockNotAvailableException is thrown.
	 *
	 * @param  oid  The unique identifier that references the data object to be updated
	 * @param  obj  The new updated data object
	 * @return      The updated data object, if successful otherwise null.
	 */
	public synchronized Object update(String oid, Object obj) {
		return put(oid, obj);
	}


	/**
	 *  Returns the time that the object denoted by this oid was last modified.
	 *
	 * @param  oid  The unique identifier that references the data object
	 * @return      A long value representing the time the file was last modified, measured
	 *      in milliseconds since the epoch (00:00:00 GMT, January 1, 1970), or -1 if the oid
	 *      does not exist.
	 */
	public long getLastModifiedDate(String oid) {
		if (!oidExists(oid))
			return -1;

		return new File(dataPath + "/" + encodeFileName(oid)).lastModified();
	}
}

