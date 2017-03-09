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
 *  disc. Objects are stored in serialized form on disc - any object that implements
 *  {@link java.io.Serializable} can be stored in this DataManager for persistent
 *  retrieval. The objects can also be held in a RAM cache to provide faster retrieval.
 *  Serialized objects are stored in a directory specified at the time of construction.
 *  Objects stored in a SerializedDataManager can be of mixed type. This class is safe to
 *  use with multiple process and/or JVMs that access the same dataPath concurrently.
 *
 * @author    John Weatherley
 */
public class SerializedDataManager extends DataManager implements Serializable {

	/**  The path to the data store */
	protected String dataPath = "";
	/**  DESCRIPTION */
	protected static boolean debug = false;

	/**  DESCRIPTION */
	protected Hashtable locks = new Hashtable(50);
	/**  DESCRIPTION */
	protected Hashtable objectCache = null;

	/**  DESCRIPTION */
	protected long num_records = 0;

	// Should file locking be used to support concurrent access across multiple
	// processes or JVMs?
	/**  DESCRIPTION */
	protected final boolean useFileLocks = true;

	/**  DESCRIPTION */
	protected boolean useCache = false;


	/**
	 *  Constructs a new SerializedDataManager that reads and writes data to the given
	 *  dataPath directory. Allows control over whether or not to hold a cache of the objects
	 *  in RAM memory for faster retrieval. Note that if caching is turned on in this
	 *  SerializedDataManager, the objects that are returned from the {@link #get(String)}
	 *  and {@link #get(List)} methods are not safe to modify. Specifically, any changes made
	 *  to the objects will be reflected in subsequent calls to those methods. To get a copy
	 *  of the data items that are safe to modify the caller should use the {@link
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
	public SerializedDataManager(String dataPath, boolean useCache)
		 throws Exception {
		File f = new File(dataPath);
		if (!f.isDirectory())
			throw new Exception("Directory " + dataPath + " does not exist.");

		this.dataPath = dataPath;

		this.useCache = useCache;
		if (useCache)
			objectCache = new Hashtable(getNumFiles() + 11);
		num_records = getNumFiles();
	}


	/**  Constructor for the SerializedDataManager object, restores a serialized SerializedDataManager. */
	public SerializedDataManager() { }


	/**
	 *  Retrieves a single data object. Note that if caching is turned on in this
	 *  SerializedDataManager, the object that is returned is not safe to modify.
	 *  Specifically, if caching is enabled, any changes made to that object will be
	 *  reflected in subsequent calls to this method and the {@link #get(List)} method. To
	 *  get a copy of a data item that is safe to modify use the {@link #getCopy(String)}
	 *  method instead. If caching is not enabled, the effect of this method is the same as
	 *  {@link #getCopy(String)}.
	 *
	 * @param  oid                           The data identifier
	 * @return                               <code>Object</code> of data
	 * @exception  OIDDoesNotExistException  If no object exists for the given uid.
	 * @see                                  #getCopy(String)
	 */
	public synchronized Object get(String oid)
		 throws OIDDoesNotExistException {
		Object o = null;

		try {
			o = restoreSerializedObject(oid, useCache);
			if (o == null)
				throw new OIDDoesNotExistException(oid);
		} catch (IOException ioe) {
			throw new OIDDoesNotExistException("Problem retrieving OID " + oid + ": " + ioe.toString());
		} catch (ClassNotFoundException ce) {
			throw new OIDDoesNotExistException("Problem retrieving OID " + oid + ": " + ce.toString());
		}
		return o;
	}


	/**
	 *  Retrieves a copy of a single data object, sutable for modifying. The object that is
	 *  returned is safe for modifying without affecting the data that is in this
	 *  SerializedDataManager.
	 *
	 * @param  oid                           The data identifier.
	 * @return                               A copy of an <code>Object</code> of data that is
	 *      in this SerializedDataManager.
	 * @exception  OIDDoesNotExistException  If no object exists for the given uid.
	 * @see                                  #get(String)
	 */
	public synchronized Object getCopy(String oid)
		 throws OIDDoesNotExistException {
		Object o = null;

		try {
			o = restoreSerializedObject(oid, false);
			if (o == null)
				throw new OIDDoesNotExistException(oid);
		} catch (IOException ioe) {
			throw new OIDDoesNotExistException("Problem retrieving OID " + oid + ": " + ioe.toString());
		} catch (ClassNotFoundException ce) {
			throw new OIDDoesNotExistException("Problem retrieving OID " + oid + ": " + ce.toString());
		}
		return o;
	}


	/**
	 *  Retrieves a <code>List</code> of data objects. Note that if caching is turned on in
	 *  this SerializedDataManager, the objects that are returned are not safe to modify.
	 *  Specifically, if caching is enabled, any changes made to the objects will be
	 *  reflected in subsequent calls to this method and the {@link #get(String)} method. To
	 *  get a copy of a data items that are safe to modify use the {@link #getCopy(List)}
	 *  method instead. If caching is not ebabled, the effect of this method is the same as
	 *  {@link #getCopy(List)}.
	 *
	 * @param  oids                          <code>List</code> of data identifiers
	 * @return                               <code>List</code> of corresponding data objects
	 * @exception  OIDDoesNotExistException  If no object exists for the given uid.
	 * @see                                  #getCopy(List)
	 */
	public synchronized List get(List oids)
		 throws OIDDoesNotExistException {
		List list = new ArrayList(oids.size());

		for (int i = 0; i < oids.size(); i++)
			list.add(this.get((String) oids.get(i)));

		return list;
	}


	/**
	 *  Retrieves a <code>List</code> of copied data objects, suitable for modifying. The
	 *  objects that are returned are safe for modifying without affecting the data that is
	 *  in this SerializedDataManager.
	 *
	 * @param  oids                          <code>List</code> of data identifiers.
	 * @return                               <code>List</code> of corresponding data objects
	 *      copies.
	 * @exception  OIDDoesNotExistException  If no object exists for the given uid.
	 * @see                                  #get(List)
	 */
	public synchronized List getCopy(List oids)
		 throws OIDDoesNotExistException {
		List list = new ArrayList(oids.size());

		for (int i = 0; i < oids.size(); i++)
			list.add(this.getCopy((String) oids.get(i)));

		return list;
	}


	/**
	 *  Retrieves a <code>String []</code> of all IDs that exist in this
	 *  SerializedDataManager. Results are not guaranteed to be in lexagraphical order,
	 *  however the order is guaranteed to be in the same order from one invocation to the
	 *  next.
	 *
	 * @return    <code>String []</code> of the IDs in this SerializedDataManager
	 */
	public synchronized String[] getIDs() {
		String[] IDs = new File(dataPath).list();

		for (int i = 0; i < IDs.length; i++)
			IDs[i] = decodeFileName(IDs[i]);

		return IDs;
	}


	/**
	 *  Retrieves a <code>String []</code> of all IDs that exists in this
	 *  SerializedDataManager sorted lexagraphically.
	 *
	 * @return    A lexagraphically sorted <code>String []</code> of the IDs in this
	 *      SerializedDataManager
	 */
	public synchronized String[] getIDsSorted() {
		String[] IDs = new File(dataPath).list();

		for (int i = 0; i < IDs.length; i++)
			IDs[i] = decodeFileName(IDs[i]);
		Arrays.sort(IDs);

		return IDs;
	}


	/**
	 *  Get the number of records in this SerializedDataManager.
	 *
	 * @return    The number of records in this SerializedDataManager.
	 */
	public synchronized long getNumRecords() {
		return num_records;
	}


	/**
	 *  Adds a new object of data. This method is safe for concurrent use among multiple
	 *  processes and JVMs accessing the same SerializedDataManager.
	 *
	 * @param  oid                              The unique identifier that references the new
	 *      data object
	 * @param  obj                              The new data object
	 * @return                                  The added data object iff successful,
	 *      otherwise null
	 * @exception  OIDAlreadyExistsException    If an object with given oid already exists in
	 *      the data store.
	 * @exception  ObjectNotSupportedException  If the object type is not suppored.
	 */
	public synchronized Object put(String oid, Object obj)
		 throws OIDAlreadyExistsException, ObjectNotSupportedException {
		String encodedFilePath = dataPath + "/" + encodeFileName(oid);
		File f = new File(encodedFilePath);
		if (f.isFile())
			throw new OIDAlreadyExistsException("OID " + oid + " already exists.");

		try {
			if (useFileLocks)
				getLock(encodedFilePath);
			serailizeObject(encodedFilePath, obj);
		} catch (InvalidClassException ce) {
			throw new ObjectNotSupportedException("OID " + oid + " object type not supported: " + ce);
		} catch (NotSerializableException se) {
			throw new ObjectNotSupportedException("OID " + oid + " object type not supported: " + se);
		} catch (IOException e) {
			prtln("SerializedDataManager: Error serializing object " + oid + ": " + e);
			return null;
		} finally {
			if (useFileLocks)
				releaseLock(encodedFilePath);
		}

		num_records++;
		return obj;
	}


	/**
	 *  Gets an internal read/write lock for a given object in the this
	 *  SerializedDataManager. This method, together with {@link #releaseLock(String
	 *  objectPath)}, provide reliable locking method for use across multiple processes or
	 *  JVMs that may access the SDM data concurrently.
	 *
	 * @param  objectPath  The absolute path to the data object being stored.
	 * @return             True if the lock was obtained.
	 */
	protected boolean getLock(String objectPath) {
		try {
			int time = 0;
			File lock = new File(objectPath + ".sdm_lck");
			lock.deleteOnExit();
			while (!lock.createNewFile()) {
				try {
					time += 10;
					Thread.sleep(10);

					/*
					 *  Check if writelock might not have been released upon last shutdown, and release it.
					 *  Note: in an environment where multiple apps are accessing the same index, this
					 *  may cause problems
					 */
					if (time > 6000) {
						prtln("SerializedDataManager.getWriteLock() waited 6 seconds. Assuming lock is hung. Removing lock...");
						lock.delete();
					}
				} catch (InterruptedException ie) {}
			}
			//prtln("getLock() for id:" + objectPath);
			return true;
		} catch (Exception e) {}
		return false;
	}


	/**
	 *  Release the read/write lock.
	 *
	 * @param  objectPath  The absolute path to the data object being stored.
	 */
	protected void releaseLock(String objectPath) {
		File lock = new File(objectPath + ".sdm_lck");
		lock.delete();
		//prtln("releaseLock() for id:" + objectPath);
	}


	/**
	 *  Removes an existing data object, assuming the caller had requested and recieved the
	 *  necessary lock for the object. After successful completion the object is removed from
	 *  the DataManager and returned to the caller.
	 *
	 * @param  oid                           The unique identifier that references the data
	 *      object to be removed
	 * @param  lockKey                       The key corresponding to the lock on this data
	 *      object
	 * @return                               The removed data object iff successful,
	 *      otherwise null
	 * @exception  OIDDoesNotExistException  If no object exists for the given uid.
	 * @exception  InvalidLockException      If the lock provided for the object is not
	 *      valid.
	 */
	public synchronized Object remove(String oid, String lockKey)
		 throws OIDDoesNotExistException, InvalidLockException {
		if (!isLocked(oid))
			throw new InvalidLockException("OID " + oid + " must be locked prior to removal");

		if (!isValidLock(oid, lockKey))
			throw new InvalidLockException("Invalid lock key for OID " + oid);

		Object o = null;
		//String encodedFileName = encodeFileName(oid);
		try {
			o = restoreSerializedObject(oid, useCache);
			if (o == null)
				throw new OIDDoesNotExistException(oid);
		} catch (IOException ioe) {
			throw new OIDDoesNotExistException("Problem retrieving OID " + oid + ": " + ioe.toString());
		} catch (ClassNotFoundException ce) {
			throw new OIDDoesNotExistException("Problem retrieving OID " + oid + ": " + ce.toString());
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
	 * @param  oid                            The unique identifier that references the data
	 *      object to be removed
	 * @return                                The Object that was removed.
	 * @exception  OIDDoesNotExistException   If no object exists for the given uid.
	 * @exception  LockNotAvailableException  If another user currently has a lock on this
	 *      object.
	 */
	public synchronized Object remove(String oid)
		 throws OIDDoesNotExistException, LockNotAvailableException {
		if (isLocked(oid))
			throw new LockNotAvailableException("OID " + oid + " is locked and cannot be removed");

		Object o = null;
		//String encodedFileName = encodeFileName(oid);
		try {
			o = restoreSerializedObject(oid, useCache);
			if (o == null)
				throw new OIDDoesNotExistException(oid);
		} catch (IOException ioe) {
			throw new OIDDoesNotExistException("Problem retrieving OID " + oid + ": " + ioe.toString());
		} catch (ClassNotFoundException ce) {
			throw new OIDDoesNotExistException("Problem retrieving OID " + oid + ": " + ce.toString());
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
	 * @param  oid                           The unique identifier that references the data
	 *      object to be deleted
	 * @param  lockKey                       The key corresponding to the lock on this data
	 *      object
	 * @return                               DESCRIPTION
	 * @exception  OIDDoesNotExistException  If no object exists for the given uid.
	 * @exception  InvalidLockException      If the lock provided for the object is not
	 *      valid.
	 */
	public synchronized boolean delete(String oid, String lockKey)
		 throws OIDDoesNotExistException, InvalidLockException {
		File f = new File(dataPath + "/" + encodeFileName(oid));
		if (!f.isFile())
			throw new OIDDoesNotExistException(oid);

		if (!isValidLock(oid, lockKey))
			throw new InvalidLockException("Invalid lock key for OID " + oid);

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
	 * @param  oid                            The unique identifier that references the data
	 *      object to be deleted
	 * @return                                DESCRIPTION
	 * @exception  OIDDoesNotExistException   If no object exists for the given uid.
	 * @exception  LockNotAvailableException  If another user currently has a lock on this
	 *      object.
	 */
	public synchronized boolean delete(String oid)
		 throws OIDDoesNotExistException, LockNotAvailableException {
		File f = new File(dataPath + "/" + encodeFileName(oid));
		if (!f.isFile())
			throw new OIDDoesNotExistException(oid);
		if (isLocked(oid))
			throw new LockNotAvailableException("OID " + oid + " is locked and cannot be deleted");

		f.delete();
		num_records--;
		return true;
	}


	/**
	 *  Updates a new object of data, assuming the caller had requested and recieved the
	 *  necessary lock. The caller retains the lock after this method returns.
	 *
	 * @param  oid                              The unique identifier that references the
	 *      data object to be updated
	 * @param  obj                              The new updated data object
	 * @param  lockKey                          The key corresponding to the lock on this
	 *      data object
	 * @return                                  The updated data object, if successful
	 *      otherwise null
	 * @exception  OIDDoesNotExistException     If no object exists for the given uid.
	 * @exception  ObjectNotSupportedException  If the object type is not suppored.
	 * @exception  InvalidLockException         If the lock provided for the object is not
	 *      valid.
	 */
	public synchronized Object update(String oid, Object obj, String lockKey)
		 throws OIDDoesNotExistException, ObjectNotSupportedException, InvalidLockException {
		delete(oid, lockKey);
		try {
			put(oid, obj);
		} // Since we just deleted this oid, this exception should never occur.
		catch (OIDAlreadyExistsException e) {
			prtln("ERROR: update() " + e);
		}

		locks.put(oid, lockKey);
		// Caller retains the lock after method returns...
		return obj;
	}


	/**
	 *  Updates a new object of data iff the object is not locked by another user. If the
	 *  object is locked a LockNotAvailableException is thrown.
	 *
	 * @param  oid                              The unique identifier that references the
	 *      data object to be updated
	 * @param  obj                              The new updated data object
	 * @return                                  The updated data object, if successful
	 *      otherwise null
	 * @exception  OIDDoesNotExistException     If no object exists for the given uid.
	 * @exception  ObjectNotSupportedException  If the object type is not suppored.
	 * @exception  LockNotAvailableException    If another user currently has a lock on this
	 *      object.
	 */
	public synchronized Object update(String oid, Object obj)
		 throws OIDDoesNotExistException, ObjectNotSupportedException, LockNotAvailableException {
		delete(oid);
		try {
			put(oid, obj);
		} // Since we just deleted this oid, this exception should never occur.
		catch (OIDAlreadyExistsException e) {
			prtln("ERROR: update() " + e);
		}
		return obj;
	}


	/**
	 *  Get a user-requested lock for a data object, preventing it from being written by
	 *  other threads that are using this SerializedDataManager. Note that this type of
	 *  locking only works within a single instance of the SerializedDataManager. Other
	 *  instances of SerializedDataManager will not know about this lock.
	 *
	 * @param  oid                            The unique identifier that references the data
	 *      object to be locked
	 * @return                                The key used to unlock this object.
	 * @exception  OIDDoesNotExistException   If no object exists for the given uid.
	 * @exception  LockNotAvailableException  If another user currently has a lock on this
	 *      object.
	 */
	public synchronized String lock(String oid)
		 throws OIDDoesNotExistException, LockNotAvailableException {
		if (!oidExists(oid))
			throw new OIDDoesNotExistException("OID not found: " + oid);

		if (isLocked(oid))
			throw new LockNotAvailableException("OID " + oid);

		String lock = getNextLockKey();
		locks.put(oid, lock);

		return lock;
	}


	/**
	 *  Returns the time that the object denoted by this oid was last modified.
	 *
	 * @param  oid                           The unique identifier that references the data
	 *      object
	 * @return                               A long value representing the time the file was
	 *      last modified, measured in milliseconds since the epoch (00:00:00 GMT, January 1,
	 *      1970)
	 * @exception  OIDDoesNotExistException  If no object exists for the given uid.
	 */
	public long getLastModifiedDate(String oid)
		 throws OIDDoesNotExistException {
		if (!oidExists(oid))
			throw new OIDDoesNotExistException("OID not found: " + oid);

		return new File(dataPath + "/" + encodeFileName(oid)).lastModified();
	}


	/**
	 *  Determines whether an object with the given oid exists in the DataManager.
	 *
	 * @param  oid  The unique identifier that references the data object
	 * @return      True iff the given object exists in this DataManager
	 */
	public boolean oidExists(String oid) {
		if (oid == null)
			return false;

		// If the file exists then the oid exists
		return new File(dataPath + "/" + encodeFileName(oid)).isFile();
	}


	/**
	 *  Determine whether a given object is locked by another user.
	 *
	 * @param  oid  The unique identifier that references the data object
	 * @return      True iff the object referred to by this oid is locked
	 */
	public synchronized boolean isLocked(String oid) {
		return (oid != null && locks.containsKey(oid));
	}


	/**
	 *  Determine whether a given object is locked with the given key.
	 *
	 * @param  oid      The unique identifier that references the data object
	 * @param  lockKey  The lock key
	 * @return          True iff the object referred to by this oid is locked with the given
	 *      key
	 */
	public synchronized boolean isValidLock(String oid, String lockKey) {
		String storedKey = (String) locks.get(oid);
		return (storedKey != null && lockKey != null && storedKey.equals(lockKey));
	}


	/**
	 *  Remove the lock on a data object, if it exists.
	 *
	 * @param  oid                           The unique identifier that references the locked
	 *      data object
	 * @param  lockKey                       The key corresponding to the lock on this data
	 *      object
	 * @return                               <b>true</b> iff the lock has been removed for
	 *      the given object regardless of whether it had been locked prior to calling this
	 *      method.
	 * @exception  OIDDoesNotExistException  If no object exists for the given uid.
	 * @exception  InvalidLockException      If the lock provided for the object is not
	 *      valid.
	 */
	public synchronized boolean unlock(String oid, String lockKey)
		 throws OIDDoesNotExistException, InvalidLockException {
		if (!oidExists(oid))
			throw new OIDDoesNotExistException("OID not found: " + oid);

		if (!isLocked(oid))
			return true;

		if (!isValidLock(oid, lockKey))
			throw new InvalidLockException("Invalid lock key for OID " + oid);

		locks.remove(oid);
		return true;
	}


	/**
	 *  Serializes an object to the given file path, placing it in a RAM cache if caching
	 *  indicated at construction time.
	 *
	 * @param  path                          DESCRIPTION
	 * @param  obj                           DESCRIPTION
	 * @exception  IOException               DESCRIPTION
	 * @exception  InvalidClassException     DESCRIPTION
	 * @exception  NotSerializableException  DESCRIPTION
	 */
	protected void serailizeObject(String path, Object obj)
		 throws IOException, InvalidClassException, NotSerializableException {

		// Place a copy in a RAM cache, if indicated. May throw nullPointerException
		if (useCache) {
			synchronized (objectCache){
				objectCache.put(path, obj);
			}
		}

		FileOutputStream os = new FileOutputStream(path);
		ObjectOutputStream o = new ObjectOutputStream(os);
		o.writeObject(obj);
		o.flush();
		o.close();
		os.close();
	}


	/**
	 *  Restores a Serialized object from the given file path, pulling if from a RAM cache if
	 *  caching indicated at construction time. Returns null if no file was found for the
	 *  given path.
	 *
	 * @param  oid                         DESCRIPTION
	 * @param  fromCache                   DESCRIPTION
	 * @return                             DESCRIPTION
	 * @exception  IOException             DESCRIPTION
	 * @exception  ClassNotFoundException  DESCRIPTION
	 */
	protected Object restoreSerializedObject(String oid, boolean fromCache)
		 throws IOException, ClassNotFoundException {

		Object obj = null;
		String encodedPath = dataPath + "/" + encodeFileName(oid);
		try {
			File f = new File(encodedPath);
			if (!f.isFile())
				return null;
			if (useFileLocks)
				getLock(encodedPath);

			// Return the cached version, if available.
			if (useCache && fromCache) {
				synchronized (objectCache){
					if(objectCache.containsKey(encodedPath)) {
						return objectCache.get(encodedPath);
					}
				}
			}

			FileInputStream is = new FileInputStream(f);
			ObjectInputStream o = new ObjectInputStream(is);
			obj = o.readObject();
			o.close();
			is.close();
		} catch (ClassNotFoundException cnfe) {
			throw new ClassNotFoundException(cnfe.getMessage());
		} catch (Throwable t) {
			throw new IOException(t.getMessage());
		} finally {
			if (useFileLocks)
				releaseLock(encodedPath);
		}

		if (useCache && encodedPath != null && obj != null) {
			synchronized (objectCache) {
				objectCache.put(encodedPath, obj);
			}
		}

		return obj;
	}



	/**
	 *  Encodes a String so that it is suitable for use as a file name by encoding all non
	 *  letter or digit chars such as "/" and ":" into escaped hex values of the form _HEX.
	 *  Note: Unix commands cannot include the following chars: * ? ! | \ / ' " { } < > ; , ^
	 *  ( ) $ ~ Windows file names may not contain: \ / : * ? " < > | nor does Win 2000 like
	 *  it when a . is at the end of the filename (it removes it when returning the file
	 *  name).
	 *
	 * @param  name  The String to encode.
	 * @return       An encoded String.
	 */
	protected String encodeFileName(String name) {
		int ii;
		StringBuffer outbuf = new StringBuffer(name.length() * 3);
		for (ii = 0; ii < name.length(); ii++) {
			char chr = name.charAt(ii);
			if (Character.isLetterOrDigit(chr) || chr == '-')
				outbuf.append(chr);
			else {
				outbuf.append("_");
				outbuf.append(Integer.toHexString(chr));
			}
		}
		return outbuf.toString();
	}


	/**
	 *  Converts a string that contains escaped hex encoding of the form %HEX back to plain
	 *  text.Provides the inverse operation of the method encodeFileName()
	 */
	protected static Hashtable codes = null;


	/**
	 *  Decodes a String that was encoded using the method {@link #encodeFileName(String
	 *  name)}.
	 *
	 * @param  name  The string to decods.
	 * @return       A decoded String.
	 */
	protected String decodeFileName(String name) {
		// Initialize the Hashtable only once:
		if (codes == null) {
			codes = new Hashtable();
			for (int j = 0; j < 256; j++) {
				char ch = (char) (j);
				if (!Character.isLetterOrDigit(ch) || ch == '-' || !Character.isISOControl(ch))
					codes.put(("_" + Integer.toHexString(ch)), new Character(ch));
			}
		}

		if (name.length() < 3)
			return name;

		StringBuffer decoded = new StringBuffer(name.length());
		Character c;
		String h;
		int i;
		int length = name.length();
		for (i = 0; i < (length - 2); ) {
			h = name.substring(i, i + 3);
			c = (Character) codes.get(h);
			if (c == null) {
				decoded.append(name.charAt(i));
				i++;
			}
			else {
				decoded.append(c);
				i += 3;
			}
		}
		if (i != length)
			decoded.append(name.substring(i, length));

		return decoded.toString();
	}


	/**
	 *  Return the number of files in this DataManager as reported by the file system.
	 *
	 * @return    int The number of files currently in the DataManager
	 */
	protected int getNumFiles() {
		return (new File(dataPath).list()).length;
	}


	/**  A unique id for locking purposes  */
	protected long nextLockKey = 0;
	final static long MAX = 1000000000;


	/**
	 *  Gets the nextLockKey attribute of the SerializedDataManager object
	 *
	 * @return    The nextLockKey value
	 */
	protected String getNextLockKey() {
		nextLockKey++;
		if (nextLockKey >= MAX)
			nextLockKey = 0;
		return Long.toString(nextLockKey);
	}


	/**
	 *  Sets the debug attribute of the SerializedDataManager class
	 *
	 * @param  isDebugOutput  The new debug value
	 */
	public static void setDebug(boolean isDebugOutput) {
		debug = isDebugOutput;
	}


	/**
	 *  Print a line to standard output.
	 *
	 * @param  s  DESCRIPTION
	 */
	protected void prtln(String s) {
		if (debug)
			System.out.println(s);
	}
}

