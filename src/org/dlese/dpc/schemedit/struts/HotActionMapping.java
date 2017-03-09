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

import org.apache.struts.action.ActionMapping;
import java.util.*;


/**
 * <p>Subclass of <code>ActionMapping</code> that allows roles to be set (by default they are
 frozen).</p>
 *
 * @version $Rev: 54929 $ $Date: 2009/03/20 23:33:57 $
 */

public class HotActionMapping extends ActionMapping {


    public void setRoles(String roles) {
		// System.out.println ("\tHotActionMapping (" + this.getPath() + ") role set to \"" + roles + "\"");
        this.roles = roles;
        if (roles == null) {
            roleNames = new String[0];
            return;
        }
        ArrayList list = new ArrayList();
        while (true) {
            int comma = roles.indexOf(',');
            if (comma < 0)
                break;
            list.add(roles.substring(0, comma).trim());
            roles = roles.substring(comma + 1);
        }
        roles = roles.trim();
        if (roles.length() > 0)
            list.add(roles);
        roleNames = (String[]) list.toArray(new String[list.size()]);
    }
	
    /**
     * <p>Construct a new instance of this class with the desired default
     * form bean scope.</p>
     */
    public HotActionMapping() {

        super();

    }

}
