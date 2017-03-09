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
package org.dlese.dpc.schemedit.struts;


import org.apache.struts.config.impl.ModuleConfigImpl;

/**
 * <p>The collection of static configuration information that describes a
 * Struts-based module.  Multiple modules are identified by
 * a <em>prefix</em> at the beginning of the context
 * relative portion of the request URI.  If no module prefix can be
 * matched, the default configuration (with a prefix equal to a zero-length
 * string) is selected, which is elegantly backwards compatible with the
 * previous Struts behavior that only supported one module.</p>
 *
 * @version $Rev: 54929 $ $Date: 2009/03/20 23:33:57 $
 * @since Struts 1.1
 */
public class MyModuleConfig extends ModuleConfigImpl {
    
    /**
     * Construct an ModuleConfigImpl object according to the specified
     * parameter values.
     *
     * @param prefix Context-relative URI prefix for this module
     */
    public MyModuleConfig(String prefix) {
        super(prefix);

        this.actionMappingClass = "org.dlese.dpc.schemedit.struts.HotActionMapping";

    }


}
