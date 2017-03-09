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

/**
 *  A FileFilter for zip files. Filters for files that end in '.zip' or '.ZIP' and are files, not directories.
 *  Instances of this class may be passed to the {@link java.io.File#listFiles(FileFilter)} method of the
 *  {@link java.io.File} class.
 *
 * @author     John Weatherley
 * @version    $Id: ZipFileFilter.java,v 1.2 2009/03/20 23:34:00 jweather Exp $
 */
public class ZipFileFilter implements FileFilter {
	/**
	 *  A FileFilter for zip files. Filters for files that end in '.zip' or '.ZIP'.
	 *
	 * @param  file  The file in question
	 * @return       True if isFile() is true and the file ends in '.zip' or '.ZIP'
	 */
	public boolean accept(File file) {
		return (file.isFile() && (file.getName().endsWith(".zip") || file.getName().endsWith(".ZIP")));
	}
}


