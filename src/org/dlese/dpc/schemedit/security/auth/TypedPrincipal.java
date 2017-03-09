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

import java.io.*;
import java.security.Principal;

/**
 * This class is based on the Tagish JAAS Login Modules (http://thekirschners.com/tagish-jaas/),
 * and extended only to add "COLLECTION" and "AUTH" principles.
 *
 * TypedPrincipals are derived from, and can be treated like Principals
 * but they also contain extra information about the type of the Principal
 * which can be USER, GROUP or DOMAIN. I'm not 100% certain that this is a
 * good way of doing things. Suggestions welcome.
 *
 * @author Andy Armstrong
 * @version 1.0.3
 */
public class TypedPrincipal implements Principal, Serializable
{
	/**
	 */
	protected String	name;
	protected int		type;

	/**
	 * This <CODE>TypedPrincipal</CODE> represents a username or SID.
	 */
	public final static int		USER	= 1;

	/**
	 * This <code>TypedPrincipal</code> represents the domain name or SID.
	 */
	public final static int		DOMAIN	= 2;

	/**
	 * This <code>TypedPrincipal</code> represents a group name or SID.
	 */
	public final static int		GROUP	= 3;

	public final static int		COLLECTION	= 4;
	
	/**
	 * This <code>TypedPrincipal</code> represents an authentication method.
	 */
	public final static int		AUTH	= 5;	
	
	/**
	 * This <code>TypedPrincipal</code> represents an item of unknown type.
	 */
	public final static int		UNKNOWN	= 6;
	

	

	protected final static String typeMap[] = { null, "USER", "DOMAIN", "GROUP", "UNKNOWN", "COLLECTION", "AUTH" };

	/**
	 * Create a TypedPrincipal with a name and type.
	 *
	 * <p>
	 *
	 * @param name the name for this principal.
	 * @param type the type for this principal.
	 * @exception NullPointerException if the <code>name</code>
	 * 			is <code>null</code>.
	 */
	public TypedPrincipal(String name, int type)
	{
		if (name == null) {
			throw new NullPointerException("Illegal null name");
		}
		if (type < USER || type > UNKNOWN) {
			throw new IllegalArgumentException("Bad type value");
		}

		this.name = name;
		this.type = type;
	}

	/**
	 * Create a TypedPrincipal with a name.
	 *
	 * <p>
	 *
	 * @param name the name for this Principal.
	 * @exception NullPointerException if the <code>name</code>
	 * 			is <code>null</code>.
	 */
	public TypedPrincipal(String name)
	{
		this(name, UNKNOWN);
	}


	/**
	 * Create a TypedPrincipal with a blank name.
	 *
	 * <p>
	 *
	 */
	public TypedPrincipal()
	{
		this("", UNKNOWN);
	}

	public void setType(int type)
	{
		this.type = type;
	}


	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Return the name for this <code>TypedPrincipal</code>.
	 *
	 * <p>
	 *
	 * @return the name for this <code>TypedPrincipal</code>
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Return the type for this <code>TypedPrincipal</code>.
	 *
	 * <p>
	 *
	 * @return the type for this <code>TypedPrincipal</code>
	 */
	public int getType()
	{
		return type;
	}

	/**
	 * Return the name of the type for this <code>TypedPrincipal</code>.
	 *
	 * <p>
	 *
	 * @return the name of the type for this <code>TypedPrincipal</code>
	 */
	public String getTypeName()
	{
		return typeMap[type];
	}

	/**
	 * Return a string representation of this <code>TypedPrincipal</code>.
	 *
	 * <p>
	 *
	 * @return a string representation of this <code>TypedPrincipal</code>.
	 */
	public String toString()
	{
		String className = getClass().getName();
		return className.substring(className.lastIndexOf('.') + 1) + ": " + name + " [" + getTypeName() + "]";
	}

	/**
	 * Compares the specified Object with this <code>TypedPrincipal</code>
	 * for equality.  Returns true if the given object is also a
	 * <code>TypedPrincipal</code> and the two TypedPrincipals have the
	 * same name and type. If the object is any other kind Principal
	 * its will be considered equal if the name matches.
	 *
	 * <p>
	 *
	 * @param o Object to be compared for equality with this <code>TypedPrincipal</code>.
	 * @return true if the specified Object is equal to this <code>TypedPrincipal</code>.
	 */
	public boolean equals(Object o)
	{
		if (o == null) {
			return false;
		} else if (this == o) {
			return true;
		} else if (o instanceof TypedPrincipal) {
			TypedPrincipal that = (TypedPrincipal) o;
			return	that.getName().equals(getName()) &&
					that.getType() == getType();
		} else if (o instanceof Principal) {
			Principal that = (Principal) o;
			return	that.getName().equals(getName());
		} else {
			return false;
		}
	}

	/**
	 * Return a hash code for this <code>TypedPrincipal</code>.
	 *
	 * <p>
	 *
	 * @return a hash code for this <code>TypedPrincipal</code>.
	 */
	public int hashCode()
	{
		return name.hashCode();
	}

}
