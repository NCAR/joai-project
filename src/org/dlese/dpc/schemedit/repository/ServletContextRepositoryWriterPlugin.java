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

import javax.servlet.*;

/**
 *  This abstract class implements RepositoryWriterPlugin to provide access to the {@link
 *  javax.servlet.ServletContext} during the indexing process. This class should be used
 *  when using a RepositoryWriterPlugin in a Servlet environment.
 *
 * @author     John Weatherley, Jonathan Ostwald
 * @see        RepositoryWriterServiceWriter
 * <p>
 *
 *
 */
public abstract class ServletContextRepositoryWriterPlugin implements RepositoryWriterPlugin {
	private static ServletContext servletContext = null;


	/**
	 *  Sets the ServletContext to make it available to this plugin during the indexing
	 *  process.
	 *
	 * @param  context  The ServletContext
	 */
	public static void setServletContext(ServletContext context) {
		servletContext = context;
	}


	/**
	 *  Gets the ServletContext for use during the indexing process.
	 *
	 * @return    The ServletContext
	 */
	public static ServletContext getServletContext() {
		return servletContext;
	}
}

