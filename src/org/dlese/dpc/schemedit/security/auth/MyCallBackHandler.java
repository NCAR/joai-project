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
package org.dlese.dpc.schemedit.security.auth;

import javax.security.auth.callback.*;
import javax.servlet.http.*;

/** 
* Callback handler used by {@link org.dlese.dpc.schemedit.security.auth.SchemEditAuth}
*/
public class MyCallBackHandler implements CallbackHandler {
	
    public void handle(Callback[] callbacks) {
        for (int i = 0; i< callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback nc = (NameCallback)callbacks[i];
                nc.setName(username);
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback pc = (PasswordCallback)callbacks[i];
                pc.setPassword(password.toCharArray());
            }
        }
    }
    public MyCallBackHandler(HttpSession sess) {
        // yet to be implemented
    }

    private String username;
    private String password;

    public MyCallBackHandler(String username, String password) {
        this.username=username;
        this.password=password;
    }

}
