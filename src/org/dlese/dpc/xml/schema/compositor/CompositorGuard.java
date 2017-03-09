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
package org.dlese.dpc.xml.schema.compositor;

import java.util.*;

import org.dlese.dpc.xml.Dom4jUtils;
import org.dlese.dpc.xml.XPathUtils;
import org.dlese.dpc.xml.schema.SchemaUtils;

import org.dom4j.Element;
import org.dom4j.Node;

/**
 *  The CompositorGuard classes is responsible for enforcing the min and max
 *  occurrance schema constraints defined for a Compositor (e.g., All, Choice,
 *  or Sequence). CompositorGuards determine whether there is "room" in a given
 *  instanceElement for any new members, and what members can be added.
 *
 * @author     ostwald
 * @created    June 26, 2006
 */
public abstract class CompositorGuard {

	private static boolean debug = false;

	List occurrences = null;
	int occurrencesCount;
	Compositor compositor;
	OccurrenceCounter occurrenceCounter = null;
	String parsingError = null;
	/**  Description of the Field */
	protected List instanceMembers = null;
	String parentName = "";


	/**
	 *  Constructor for the CompositorGuard object<p>
	 *
	 *
	 *
	 * @param  compositor       NOT YET DOCUMENTED
	 * @param  instanceElement  NOT YET DOCUMENTED
	 */
	public CompositorGuard(Compositor compositor, Element instanceElement) {
		this(compositor, instanceElement.elements());
	}


	/**
	 *  Constructor for the CompositorGuard object
	 *
	 * @param  compositor       Description of the Parameter
	 * @param  instanceMembers  Description of the Parameter
	 */
	public CompositorGuard(Compositor compositor, List instanceMembers) {

		this.instanceMembers = instanceMembers;
		this.compositor = compositor;

		// resolve the instanceMembers into Occurrences
		try {
			occurrences = getOccurrences();
		} catch (Exception e) {
			parsingError = e.getMessage();
			prtln("CompositorGuard error: " + e.getMessage());
			e.printStackTrace();
			occurrences = null;
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return    NOT YET DOCUMENTED
	 */
	public String toString() {
		String s = "\"" + compositor.getName() + "\" CompositorGuard";
		s += "\n\t occurences: " + this.getOccurrencesCount();
		s += "\n\t maxOccurs: " + compositor.getMaxOccurs();
		return s;
	}


	/**
	 *  Will the CompositorGuard allow another member element to be added?<p>
	 *
	 *  A hook to force validation is provided but not used because we most often
	 *  are dealing with instanceElements that are not valid (e.g., user has just
	 *  added a element in the metadata editor). Absent the validity constraint,
	 *  this method returns if the number of occurrences are less then the number
	 *  of occurrances allowed by the schema.
	 *
	 * @return    true if the instance element can accept a new member.
	 */
	public boolean acceptsNewMember() {
		boolean validityEnforced = false;
		// element must be valid to accept a new member
		boolean isValid;
		if (validityEnforced) {
			try {
				isValid = checkValid();
			} catch (Exception e) {
				prtln("CompositorGuard.acceptsNewMember(): compositor element is NOT Valid: " + e.getMessage());
				isValid = false;
			}
		}
		else {
			isValid = true;
		}
		prtln("CompositorGuard.acceptsNewMember:\n\toccurrences: " + getOccurrencesCount() + "\n\tcompositor.maxOccurs: " + compositor.getMaxOccurs());
		return (isValid && parsingError == null && getOccurrencesCount() < compositor.getMaxOccurs());
	}


	/**
	 *  Tests whether the instance element can accept a new member of "memberName"
	 *  at the specified memberIndex.
	 *
	 * @param  memberName   NOT YET DOCUMENTED
	 * @param  memberIndex  NOT YET DOCUMENTED
	 * @return              true if the instance element will accept the specified
	 *      member.
	 */
	public boolean acceptsNewMember(String memberName, int memberIndex) {
		// element must be valid to accept a new member
		prtln("acceptsNewMember ... ");

		boolean requireValidCompositor = false;
		if (requireValidCompositor) {
			boolean isValid;
			try {
				isValid = checkValid();
			} catch (Exception e) {
				prtln(" .. compositor element is NOT Valid: " + e.getMessage());
				return false;
			}

			if (parsingError != null) {
				prtln(" .. inValid compositor: " + parsingError);
				return false;
			}
		}

		// for sequence compositors with a single member, we simply call acceptsNewMember()
		if (compositor.getMembers().size() == 1)
			return acceptsNewMember();

		// if there are already too many occurrences, deny new members.
		if (getOccurrencesCount() > compositor.getMaxOccurs()) {
			prtln("WARNING: instanceElement has more occurrences than allowed by compositor");
			return false;
		}

		// find the occurrence in which memberName, memberIndex resides
		Occurrence occurrence = findOccurrence(memberName, memberIndex);
		if (occurrence != null) {
			prtln("found occurrence");
			for (Iterator i = occurrence.elements.iterator(); i.hasNext(); ) {
				// prtln (((Element)i.next()).asXML());
				prtln("\t" + ((Element) i.next()).getQualifiedName());
			}
			int memberCount = occurrence.getMemberCount(memberName);
			int memberMaxOccurs = compositor.getMember(memberName).maxOccurs;
			prtln("  memberCount for " + memberName + ": " + memberCount);
			prtln("  maxOccurs for " + memberName + ": " + memberMaxOccurs);
			prtln("  -- > returning " + (memberCount < memberMaxOccurs) + "\n\n");
			return (memberCount < memberMaxOccurs);
		}
		else {
			prtln("occurrence not found");
		}
		return false;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public boolean checkValid() throws Exception {
		if (parsingError != null) {
			throw new Exception(parsingError);
		}

		int occurs = getOccurrencesCount();
		int minOccurs = compositor.getMinOccurs();
		int maxOccurs = compositor.getMaxOccurs();
		if (occurs < minOccurs) {
			throw new Exception(compositor.getName() + " must occur at least " + minOccurs + " times");
		}
		if (occurs > maxOccurs) {
			prtln("checkValid() occurs (" + occurs + ") > compositor.maxOccurs (" + maxOccurs + ")");
			throw new Exception(compositor.getName() + " may not occur more than " + maxOccurs + " times");
		}

		return true;
	}


	/**
	 *  Gets the compositor attribute of the CompositorGuard object
	 *
	 * @return    The compositor value
	 */
	public Compositor getCompositor() {
		return compositor;
	}


	/**
	 *  Returns a list of occurrence instances that can be used to determine
	 *  whether an instance document element satisfies the occurrence constraints
	 *  of the schema.
	 *
	 * @return                The occurrences value
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	protected abstract List getOccurrences() throws Exception;



	/**
	 *  Gets the subOccurrenceCount attribute of the CompositorGuard object
	 *
	 * @param  cm               Description of the Parameter
	 * @param  instanceElement  Description of the Parameter
	 * @return                  The subOccurrenceCount value
	 * @exception  Exception    Description of the Exception
	 */
	protected final int getSubOccurrenceCount(CompositorMember cm, Element instanceElement) throws Exception {

		CompositorGuard subCompsitorGuard =
			CompositorGuard.getInstance(cm.getSubCompositor(), instanceElement);
		return subCompsitorGuard.getOccurrencesCount();
	}


	/**
	 *  Gets the instance attribute of the CompositorGuard class
	 *
	 * @param  compositor       Description of the Parameter
	 * @param  instanceElement  Description of the Parameter
	 * @return                  The instance value
	 */
	public static CompositorGuard getInstance(Compositor compositor, Element instanceElement) {
		List elementsList = instanceElement.elements();
		CompositorGuard guard = null;
		int type = compositor.getType();
		if (type == Compositor.ALL) {
			guard = new AllGuard(compositor, elementsList);
		}
		else if (type == Compositor.SEQUENCE) {
			guard = new SequenceGuard(compositor, elementsList);
		}
		else if (type == Compositor.CHOICE) {
			guard = new ChoiceGuard(compositor, elementsList);
		}
		else {
			prtln("WARNING: getInstance could not construct compositor guard for " + type);
		}
		return guard;
	}


	/**
	 *  Gets the occurrencesCount attribute of the CompositorGuard object
	 *
	 * @return    The occurrencesCount value
	 */
	public final int getOccurrencesCount() {
		if (occurrences == null) {
			return 0;
		}
		return occurrences.size();
	}


	/**
	 *  Gets the occurrencesCount attribute of the ChoiceGuard object
	 *
	 * @param  name  NOT YET DOCUMENTED
	 * @return       The occurrencesCount value
	 */
	public final int getOccurrencesCount(String name) {
		prtln("\ngetOccurrencesCount for name: " + name);
		int cnt = 0;
		if (occurrences != null) {
			for (int i = 0; i < occurrences.size(); i++) {
				Occurrence o = (Occurrence) occurrences.get(i);

				if (o.elements != null) {
					for (Iterator elements = o.elements.iterator(); elements.hasNext(); ) {
						Element member = (Element) elements.next();
						String memberName = member.getQualifiedName();
						prtln("\t memberName: " + memberName);
						if (name.equals(memberName))
							cnt++;
					}
				}
			}
		}
		prtln("... getOccurrencesCount for " + name + " returning " + cnt + "\n");
		return cnt;
	}


	/**
	 *  Uses resolveLeafNameToCM to return the instanceQualifiedName of the
	 *  CompositorMember that controls the leafElement of leafName.
	 *
	 * @param  leafName       NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public String resolveLeafNameToCMName(String leafName) throws Exception {
		String cmName = "";
		try {
			CompositorMember cm = resolveLeafNameToCM(leafName);
			return cm.getInstanceQualifiedName();
		} catch (Exception e) {
			throw new Exception("ERROR: could not resolve \"" + leafName + "\" to a compositor member name");
		}
	}


	/**
	 *  Given a leaf element name, find the CompositorMember that ultimately
	 *  controls it
	 *
	 * @param  leafName       NOT YET DOCUMENTED
	 * @return                NOT YET DOCUMENTED
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	public CompositorMember resolveLeafNameToCM(String leafName) throws Exception {

		// sanity check - we MUST must find a member for this child - unless our compository
		// has an "any" element
		if (!compositor.getLeafMemberNames().contains(leafName)) {
			prtln("compositor didn't find \"" + leafName + "\" in the Leaf Member Names");
			List leafMemberNames = compositor.getLeafMemberNames();
			for (Iterator j = leafMemberNames.iterator(); j.hasNext(); ) {
				prtln("\t" + (String) j.next());
			}
			
			if (this.compositor.hasAnyTypeMember()) {
				prtln ("trying to find any compositor ....");
				return this.compositor.getAnyTypeMember();
			}
			else
				throw new Exception("Unexpected child element: \"" + leafName + "\"");
		}

		// Find the CompositorMember to atttach to this bucket
		CompositorMember cm = null;

		// instanceNames are the names of the simple CompositorMembers
		// (as opposed to those having InlineCompositors)

		if (compositor.getInstanceNames().contains(leafName)) {

			// prtln ("\t\t simple Member");

			//getMember must also handle substitutionGroup members!
			cm = compositor.getMember(leafName);
		}
		else {
			prtln("we didn't find \"" + leafName + "\" in the compositor instance names");
			compositor.printInstanceNames();
			// prtln ("\t\t complex Member");

			/*
			 *  find which complex CompositorMembers this element belongs to
			 *  - loop through compositor members and test complex members to see
			 *  if "name" matches to a leafMemberName
			 */
			for (Iterator members = compositor.getMembers().iterator(); members.hasNext(); ) {
				CompositorMember member = (CompositorMember) members.next();
				if (member.hasSubCompositor() && member.getSubCompositor().getLeafMemberNames().contains(leafName)) {
					// prtln ("\t complex Member");
					cm = member;
					break;
				}
			}
		}
		return cm;
	}


	/**  Description of the Method */
	public void printInstanceMembers() {
		prtln("\nCompositorGuard instanceMembers:");
		for (Iterator i = instanceMembers.iterator(); i.hasNext(); ) {
			prtln(((Element) i.next()).asXML());
		}
	}



	/**
	 *  Description of the Class
	 *
	 * @author     ostwald
	 * @created    June 26, 2006
	 */
	class Counter {
		/**  NOT YET DOCUMENTED */
		public String name;
		/**  NOT YET DOCUMENTED */
		public int count;


		/**
		 *  Constructor for the Counter object
		 *
		 * @param  name  NOT YET DOCUMENTED
		 */
		Counter(String name) {
			this.name = name;
		}
	}


	/**
	 *  Description of the Class
	 *
	 * @author     ostwald
	 * @created    June 26, 2006
	 */
	class CMBucket {
		/**  NOT YET DOCUMENTED */
		public List elements;
		/**  Description of the Field */
		public CompositorMember cm;
		/**  Description of the Field */
		public Compositor parentCompositor;
		/**  Description of the Field */
		public int count;
		/**  Description of the Field */
		public String label;


		/**
		 *  Constructor for the CMBucket object
		 *
		 * @param  cm  Description of the Parameter
		 */
		CMBucket(CompositorMember cm) {
			this.cm = cm;
			parentCompositor = cm.getParentCompositor();
			elements = new ArrayList();
			// label = cm.getInstanceName() + " (" + cm.getCMtype() + ")";
			label = parentCompositor.getName() + " (" + cm.getCMtype() + ")";
		}


		/**
		 *  NOT YET DOCUMENTED
		 *
		 * @param  e  NOT YET DOCUMENTED
		 */
		public void add(Element e) {
			elements.add(e);
			count = elements.size();
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  cm           NOT YET DOCUMENTED
	 * @param  memberIndex  NOT YET DOCUMENTED
	 * @return              NOT YET DOCUMENTED
	 */
	public Occurrence findOccurence(CompositorMember cm, int memberIndex) {
		return findOccurrence(cm.getInstanceQualifiedName(), memberIndex);
	}


	/**
	 *  Find the occurrence containing the nth element of memberName. This is used
	 *  to determine which occurence can take a new member of memberName.
	 *  SequenceGuard.acceptsNewMember calls this method to determine whether a
	 *  member can be added at a certain place (one it determines which occurrence,
	 *  then it checks to see if the member can be added to that occurrence
	 *  legally. <p>
	 *
	 *  Caluclated by traversing the elements in each occurrence, keeping track of
	 *  how many elements have memberName. When
	 *
	 * @param  memberIndex  NOT YET DOCUMENTED
	 * @param  leafName     NOT YET DOCUMENTED
	 * @return              NOT YET DOCUMENTED
	 */
	public Occurrence findOccurrence(String leafName, int memberIndex) {
		prtln("findOccurrence looking for \"" + leafName + "\" (" + memberIndex + ")");
		Occurrence found = null;
		int memberCounter = 0;
		String memberName = "";
		try {
			memberName = resolveLeafNameToCMName(leafName);
		} catch (Exception e) {
			prtln("ERROR: could not resolve \"" + memberName + "\" to a compositor member");
			return null;
		}

		prtln("\t resolved leafName: " + memberName);

		// look through the elements of each occurrence
		for (Iterator i = occurrences.iterator(); i.hasNext(); ) {
			Occurrence o = (Occurrence) i.next();
			List elements = o.elements;
			for (int j = 0; j < elements.size(); j++) {
				Element e = (Element) elements.get(j);
				String eName = e.getQualifiedName();
				try {
					eName = resolveLeafNameToCMName(eName);
				} catch (Exception ex) {
					prtln("ERROR: could not resolve \"" + eName + "\" to a compositor member");
					return null;
				}

				prtln("\t occurrence element name: " + eName);
				if (eName.equals(memberName)) {
					memberCounter++;
					prtln("\t\t found one, count is " + memberCounter);
				}
				if (memberCounter >= memberIndex) {
					prtln("\t returning .. memberCounter: " + memberCounter +
						"  memberIndex: " + memberIndex);
					return o;
				}
			}
		}
		return null;
	}



	/**
	 *  An occurrence holds the instance elements contained in a single occurence
	 *  of the compositor. For example, if a compositor has maxOccurs of 3 and
	 *  there are 5 instanceElements, then that compositor would have two instances
	 *  of the Occurrence class.
	 *
	 * @author     ostwald
	 * @created    June 26, 2006
	 */
	class Occurrence {
		/**
		 *  Constructor for the Occurrence object
		 *
		 * @param  name  NOT YET DOCUMENTED
		 */

		public Compositor compositor;
		/**  Description of the Field */
		public List elements;
		/**  Description of the Field */
		public int count;


		/**
		 *  Constructor for the Occurrence object
		 *
		 * @param  compositor  Description of the Parameter
		 */
		Occurrence(Compositor compositor) {
			this.compositor = compositor;
			elements = new ArrayList();
		}


		/**  Constructor for the Occurrence object */
		Occurrence() {
			this(null);
		}


		/**
		 *  Gets the memberCount attribute of the Occurrence object
		 *
		 * @param  cm  NOT YET DOCUMENTED
		 * @return     The memberCount value
		 */
		public int getMemberCount(CompositorMember cm) {
			return getMemberCount(cm.getInstanceQualifiedName());
		}


		/**
		 *  Gets the memberCount attribute of the Occurrence object
		 *
		 * @param  memberName  NOT YET DOCUMENTED
		 * @return             The memberCount value
		 */
		public int getMemberCount(String memberName) {
			// prtln("Occurrence.getMemberCount()");
			int n = 0;
			for (Iterator i = elements.iterator(); i.hasNext(); ) {
				Element e = (Element) i.next();
				// prtln("\t occurrence element name: " + e.getQualfiedName());
				if (e.getQualifiedName().equals(memberName)) {
					n++;
				}
			}
			return n;
		}


		/**
		 *  Adds a feature to the All attribute of the Occurrence object
		 *
		 * @param  l  The feature to be added to the All attribute
		 */
		public void addAll(List l) {
			elements.addAll(l);
			count = elements.size();
		}


		/**
		 *  Description of the Method
		 *
		 * @param  e  Description of the Parameter
		 */
		public void add(Element e) {
			elements.add(e);
			count = elements.size();
		}


		/**
		 *  NOT YET DOCUMENTED
		 *
		 * @return    NOT YET DOCUMENTED
		 */
		public String toString() {
			String s = "Occurence - (" + compositor.getName() + ")";
			if (elements != null) {
				s += "\n  " + count + " elements";
				for (Iterator i = elements.iterator(); i.hasNext(); ) {
					s += "\n\t" + ((Element) i.next()).getQualifiedName();
				}
			}
			else {
				s += "\n   there are no elements";
			}
			return "\n" + s + "\n";
		}
	}


	/**
	 *  Description of the Class
	 *
	 * @author     ostwald
	 * @created    June 26, 2006
	 */
	class OccurrenceCounter {
		HashMap map = new HashMap();


		/**
		 *  Gets the count attribute of the OccurrenceCounter object
		 *
		 * @param  key  NOT YET DOCUMENTED
		 * @return      The count value
		 */
		int getCount(String key) {
			Integer count = (Integer) map.get(key);
			if (count != null) {
				return count.intValue();
			}
			else {
				return 0;
			}
		}


		/**
		 *  NOT YET DOCUMENTED
		 *
		 * @param  key  NOT YET DOCUMENTED
		 */
		void inc(String key) {
			int count = getCount(key);
			map.put(key, new Integer(count + 1));
		}
	}


	/**  debugging */
	public void printOccurrences() {
		if (occurrences == null) {
			return;
		}
		System.out.println(occurrences.size() + " occurrences");
		// for (Iterator i = occurrences.iterator(); i.hasNext(); ) {
		// Occurrence sc = (Occurrence) i.next();

		for (int i = 0; i < occurrences.size(); i++) {
			Occurrence sc = (Occurrence) occurrences.get(i);
			System.out.println("\t" + "Occurrence #" + i);
			for (Iterator e = sc.elements.iterator(); e.hasNext(); ) {
				// System.out.println("\t\t" + ((Element) e.next()).asXML());
				System.out.println("\t\t" + ((Element) e.next()).getQualifiedName());
			}
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  node  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	static String pp(Node node) {
		try {
			return (Dom4jUtils.prettyPrint(node));
		} catch (Exception e) {
			return (e.getMessage());
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			/* 			while (s.length() > 0 && s.charAt(0) == '\n') {
				System.out.println("");
				s = s.substring(1);
			}
			System.out.println("CG: " + s); */
			SchemaUtils.prtln(s, "CG");
		}
	}

}

