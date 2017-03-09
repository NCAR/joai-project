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

import java.io.*;
import java.util.Date;
import java.util.*;
import java.text.*;
import java.nio.charset.*;
import org.dlese.dpc.oai.*;

/**
 *  Contains methods for performing common operations on files and directories such as reading, moving,
 *  deleting and copying. Also contains methods used to encode and decode IDs from filenames.
 *
 * @author     John Weatherley, Dave Deniman
 * @version    $Id: Files.java,v 1.22 2009/03/20 23:34:00 jweather Exp $
 */
public final class Files {
	private static boolean debug = false;

	/**  The char used to separate files in the native file system. */
	public static char dirSep = System.getProperty("file.separator").charAt(0);
	/**  The String used to separate files in the native file system. */
	public static String dirSepStr = Character.toString(System.getProperty("file.separator").charAt(0));


	/**
	 *  Gets the char used to separate files in the native file system.
	 *
	 * @return    The fileSeparatorStr value
	 */
	public static String getFileSeparatorStr() {
		return dirSepStr;
	}


	/**
	 *  Gets the String used to separate files in the native file system.
	 *
	 * @return    The fileSeparatorCh value
	 */
	public static char getFileSeparatorCh() {
		return dirSep;
	}


	/**
	 *  Resets the file mod time to the current time.
	 *
	 * @param  filePath  A path to a file.
	 * @return           true if and only if the operation succeeded; false otherwise.
	 */
	public static boolean touch(String filePath) {
		try {
			return touch(new File(filePath));
		} catch (Throwable e) {
			//prtlnErr("Error unable to reset file current time: " + e);
			//e.printStackTrace();
			return false;
		}
	}


	/**
	 *  Resets the file mod time to the current time.
	 *
	 * @param  file  A File.
	 * @return       true if and only if the operation succeeded; false otherwise.
	 */
	public static boolean touch(File file) {
		try {
			return file.setLastModified(System.currentTimeMillis());
		} catch (Throwable e) {
			//prtlnErr("Error unable to reset file current time: " + e);
			//e.printStackTrace();
			return false;
		}
	}


	/**
	 *  Reads a file from a JAR or location in the runtime classpath.
	 *
	 * @param  filePathInJarClasspath  The path to the file inside the JAR file or classpath
	 * @return                         The content of the file
	 */
	public final static StringBuffer readFileFromJarClasspath(String filePathInJarClasspath) {
		Files tmp = new Files();
		InputStream input = tmp.getClass().getResourceAsStream(filePathInJarClasspath);
		if (input == null) {
			input = tmp.getClass().getResourceAsStream("/" + filePathInJarClasspath);
		}
		StringBuffer content = null;
		if (input != null) {
			try {
				content = Files.readInputStream(input);
			} catch (IOException ioe) {} finally {
				try {
					input.close();
				} catch (IOException ioe) {}
			}
		}
		else {
			prtlnErr("File not found in jar \"" + filePathInJarClasspath + "\"");
		}
		return content;
	}


	/**
	 *  Reads a file into a StringBuffer.
	 *
	 * @param  filePath         The path to the file.
	 * @return                  A StringBuffer containing the files content.
	 * @exception  IOException  If error.
	 */
	public final static StringBuffer readFile(String filePath)
		 throws IOException {
		return readFile(new File(filePath));
	}


	/**
	 *  Reads a file into a StringBuffer.
	 *
	 * @param  f                The file.
	 * @return                  A StringBuffer containing the files content.
	 * @exception  IOException  If error.
	 */
	public final static StringBuffer readFile(File f)
		 throws IOException {
		// Note the FileReader is closed in the overloaded readFile method...
		return readFile(new FileReader(f));
	}


	/**
	 *  Reads a file to a StringBuffer that will be in the given character encoding, for example UTF-8. The file
	 *  that is read is assumed to be in the default system character encoding. See <a
	 *  href='http://java.sun.com/docs/books/tutorial/i18n/text/convertintro.html'>Converting Non-Unicode Text
	 *  Tutorial</a> .
	 *
	 * @param  f                The File to read
	 * @param  encoding         The encoding to write to, or null to use the default system encoding
	 * @return                  The content of the File in the given encoding
	 * @exception  IOException  If error
	 */
	public final static StringBuffer readFileToEncoding(File f, String encoding)
		 throws IOException {

		// The following two lines are just for debugging...
		//OutputStreamWriter out1 = new OutputStreamWriter(new ByteArrayOutputStream());
		//System.out.println("readFile() encoding: " + out1.getEncoding() + " file encoding: " + System.getProperty("file.encoding"));

		FileInputStream in = new FileInputStream(f);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int c;
		StringBuffer buf = new StringBuffer();
		IOException ioe = null;
		try {
			while ((c = in.read()) != -1)
				out.write(c);

			if (encoding != null)
				buf.append(out.toString(encoding));
			else
				buf.append(out.toString());
		} catch (IOException e) {
			ioe = e;
		} catch (Throwable t) {
			// do nothing
		} finally {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}
		if (ioe != null)
			throw ioe;
		return buf;
	}



	/**
	 *  Reads an InputStream into a StringBuffer and closes the stream when done.
	 *
	 * @param  is               The InputStream.
	 * @return                  A StringBuffer containing the stream content.
	 * @exception  IOException  If error.
	 */
	public final static StringBuffer readInputStream(InputStream is)
		 throws IOException {
		IOException e = null;
		InputStreamReader ir = null;
		StringBuffer buf = null;
		try {
			ir = new InputStreamReader(is);
			buf = readFile(ir);
		} catch (IOException ioe) {
			e = ioe;
		} catch (Throwable t) {
			// do nothing
		} finally {
			if (is != null)
				is.close();
			if (ir != null)
				ir.close();
		}
		if (e != null)
			throw e;
		return buf;
	}


	/**
	 *  Reads a Reader into a StringBuffer using a BufferedReader and closes the Reader when done.
	 *
	 * @param  rdr              A Reader.
	 * @return                  A StringBuffer containing the Readers content.
	 * @exception  IOException  If error.
	 */
	private final static StringBuffer readFile(Reader rdr)
		 throws IOException {
		if (rdr == null)
			return null;
		StringBuffer out = null;
		BufferedReader bufRdr = null;
		IOException exception = null;
		try {
			bufRdr = new BufferedReader(rdr);
			long c = bufRdr.read();
			out = new StringBuffer("");
			while (c != -1) {
				out.append((char) c);
				c = bufRdr.read();
			}
		} catch (IOException ioe) {
			exception = ioe;
		} catch (Throwable t) {
			// do nothing
		} finally {
			if (bufRdr != null)
				bufRdr.close();
			if (rdr != null)
				rdr.close();
		}
		if (exception != null)
			throw exception;
		return out;
	}


	/**
	 *  Writes a file containing the given content using UTF-8 encoding.
	 *
	 * @param  content          The content to write.
	 * @param  file             The file to write to.
	 * @exception  IOException  If error
	 */
	public final static void writeFile(String content, File file)
		 throws IOException {
		if (content == null || file == null)
			return;
		IOException thrown = null;
		FileOutputStream foStream = null;
		OutputStreamWriter osWriter = null;
		BufferedWriter bufWriter = null;

		try {
			foStream = new FileOutputStream(file);
			osWriter = new OutputStreamWriter(foStream, "UTF-8");
			bufWriter = new BufferedWriter(osWriter);

			bufWriter.write(content);
		} catch (IOException ioe) {
			thrown = ioe;
		} finally {
			if (bufWriter != null)
				bufWriter.close();
			if (osWriter != null)
				osWriter.close();
			if (foStream != null)
				foStream.close();
		}
		if (thrown != null)
			throw thrown;
	}


	/**
	 *  Writes a file containing the given content using UTF-8 encoding.
	 *
	 * @param  content          The content to write.
	 * @param  filePath         The file to write to.
	 * @exception  IOException  If error
	 */
	public final static void writeFile(String content, String filePath)
		 throws IOException {
		if (filePath == null || content == null)
			return;
		writeFile(content, new File(filePath));
	}


	/**
	 *  Writes a file containing the given content using UTF-8 encoding.
	 *
	 * @param  content          The content to write.
	 * @param  file             The file to write to.
	 * @exception  IOException  If error.
	 */
	public final static void writeFile(StringBuffer content, File file)
		 throws IOException {
		writeFile(content.toString(), file);
	}


	/**
	 *  Writes a file containing the given content using UTF-8 encoding.
	 *
	 * @param  content          The content to write.
	 * @param  filePath         The file to write to.
	 * @exception  IOException  If error.
	 */
	public final static void writeFile(StringBuffer content, String filePath)
		 throws IOException {
		writeFile(content.toString(), filePath);
	}


	/**
	 *  Recursively copies the contents of one directory to another. The source and destination File objects must
	 *  be directories and must exist, otherwise nothing is done. Existing files in the destination directory
	 *  with the same name as those in the source directory are overwritten.
	 *
	 * @param  sourceDir       The directory to copy.
	 * @param  destinationDir  The directory to copy to.
	 * @return                 True if successful.
	 * @exception  Exception   If error.
	 */
	public final static boolean copyDir(File sourceDir, File destinationDir) throws Exception {
		if (sourceDir == null || !sourceDir.isDirectory() || destinationDir == null || !destinationDir.isDirectory())
			return false;

		File[] files = sourceDir.listFiles();
		File newDir;
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				newDir = new File(destinationDir, files[i].getName());
				newDir.mkdir();
				if (!copyDir(files[i], newDir))
					return false;
			}
			else {
				if (!copy(files[i], new File(destinationDir, files[i].getName())))
					return false;
			}
		}
		return true;
	}


	/**
	 *  Copys the contents of one file to another, as bytes. E.g., does not convert from and then back to
	 *  characters. May be used for both text and binary files.
	 *
	 * @param  infile         The file to copy.
	 * @param  outfile        The destination of the copied file.
	 * @return                True if successful.
	 * @exception  Exception  If error.
	 */
	public final static boolean copy(File infile, File outfile) throws Exception {
		Exception thrown = null;
		boolean success = false;
		if (!infile.equals(outfile)) {
			FileInputStream in = null;
			FileOutputStream out = null;

			try {
				in = new FileInputStream(infile);
				out = new FileOutputStream(outfile);
				long size = infile.length();
				int bytes = 0;
				while (size > 0) {
					bytes = (size > 65536) ? 65536 : (int) size;
					byte[] b = new byte[bytes];
					in.read(b);
					out.write(b);
					size -= bytes;
					b = null;
				}
				success = true;
			} catch (Exception e) {
				thrown = e;
			} catch (Throwable t) {
				// do nothing
			} finally {
				if (in != null)
					in.close();
				if (out != null)
					out.close();
			}
		}
		if (thrown != null)
			throw thrown;
		return success;
	}


	/**
	 *  Moves a file from one location to another.
	 *
	 * @param  infile   The file to move.
	 * @param  outfile  A file denoting the new location.
	 * @return          True if successful.
	 */
	public static boolean move(File infile, File outfile) {
		return infile.renameTo(outfile);
	}


	/**
	 *  Deletes a directory or file and all files and directories within.
	 *
	 * @param  directory      The directory to delete.
	 * @exception  Exception  If error.
	 */
	public static void deleteDirectory(File directory) throws SecurityException {
		if (directory.isDirectory()) {
			File[] files = directory.listFiles();
			if (files.length > 0) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						deleteDirectory(files[i]);
					}
					else {
						files[i].delete();
					}
				}
			}
			directory.delete();
		}
	}


	// ------------------------ Methods used to encode and decode IDs from filenames -----------------

	/**
	 *  Escapes a Windows directory path by removing the first colon so that it can be used as the end part of
	 *  another directory path. For example, if the path is C:/mydirectory/myfile.txt the output of this method
	 *  will be C/mydirectory/myfile.txt. If the path does not begin with an upper-case character and a colon
	 *  then nothing is done.
	 *
	 * @param  path  The original path.
	 * @return       The escaped path with the first colon removed.
	 */
	public static String escapeWindowsPath(String path) {
		if (path.matches("[A-Z]:.+")) {
			return (path.replaceFirst(":", ""));
		}
		else
			return path;
	}


	/**
	 *  Gets a record ID from its file name. Assumes the file name is of the form ID.xml where ID is the record
	 *  id. For example DLESE-000-000-000-001.xml.
	 *
	 * @param  file  The file.
	 * @return       The record ID of the file.
	 */
	public static String getIDFromFilename(File file) {
		if (file != null) {
			return getIDFromFilename(file.getName());
		}
		return null;
	}


	/**
	 *  Gets a record ID from its file name. Assumes the file name is of the form ID.xml where ID is the record
	 *  id. For example DLESE-000-000-000-001.xml.
	 *
	 * @param  filename  The file.
	 * @return           The record ID of the file.
	 */
	public static String getIDFromFilename(String filename) {
		String id = null;
		if (filename != null) {
			int period = filename.indexOf('.');
			int underscore = filename.indexOf('_');
			if ((period > 0) && (underscore > 0)) {
				int pos = (period > underscore) ? underscore : period;
				id = filename.substring(0, pos);
			}
			else if (period > 0) {
				id = filename.substring(0, period);
			}
			else {
				id = filename;
			}
		}
		return id;
	}


	/**
	 *  Appends a path into a string in such a way that lexicographic sorting gives the same results as a walk of
	 *  the file hierarchy. Thus null (\u0000) is used both to separate directory components. May be decoded back
	 *  to a file path using {@link #fid2path(String fid)}.
	 *
	 * @param  f  The file whoes path will be encoded.
	 * @return    The encoded fid.
	 */
	public static String fid(File f) {

		return f.getPath().replace(dirSep, '\u0000');
		//return f.getPath().replaceAll(dirSepStr, "");
	}


	/**
	 *  Appends a path and date into a string in such a way that lexicographic sorting gives the same results as
	 *  a walk of the file hierarchy. Thus null (\u0000) is used both to separate directory components and to
	 *  separate the path from the date.
	 *
	 * @param  f  The file whoes path will be encoded.
	 * @return    The encoded fuid.
	 */
	public static String fuid(File f) {

		return f.getPath().replace(dirSep, '\u0000') + "\u0000" + Long.toString(f.lastModified());
		//return f.getPath().replaceAll(dirSepStr, "") +  Long.toString(f.lastModified());
	}


	/**
	 *  Converts an fid back to a file path. The reciprocol of {@link #fid(File f)}.
	 *
	 * @param  fid  The fid.
	 * @return      The file path that corresponds to this fid.
	 */
	public static String fid2path(String fid) {
		String path = fid.replace('\u0000', dirSep);
		// replace nulls with slashes
		return path;
	}


	/**
	 *  Encode a file name for file system compatibility.
	 *
	 * @param  msg            A String to encode.
	 * @return                Encoded String
	 * @exception  Exception  If error.
	 */
	public static String encode(String msg)
		 throws Exception {
		return OAIUtils.encode(msg);
	}


	/**
	 *  Decode a file name.
	 *
	 * @param  msg            Message to decode
	 * @return                Decoded String
	 * @exception  Exception  If unable to decode.
	 */
	public static String decode(String msg)
		 throws Exception {
		return OAIUtils.decode(msg);
	}


	// --------------------- Prtln output -------------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	protected final static void prtlnErr(String s) {
		System.err.println(getDateStamp() + " " + s);
	}



	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	protected final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}

}


