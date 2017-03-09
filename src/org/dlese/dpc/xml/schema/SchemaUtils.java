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
package org.dlese.dpc.xml.schema;

import org.dlese.dpc.xml.schema.compositor.*;

import org.dlese.dpc.xml.Dom4jUtils;

import org.dom4j.Node;
import com.sun.msv.datatype.xsd.*;
import java.util.*;

/**
 *  NOT YET DOCUMENTED
 *
 * @author    Jonathan Ostwald
 */
public class SchemaUtils {

	private static String NL = "\n";
	private static String NT = "\n\t";
	private static String NTT = "\n\t\t";
	private static String NN = "\n\n";
	private static String NNT = "\n\n\t";

	// --- static methods operating on SchemaHelper

	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showDerivedDataTypes(SchemaHelper schemaHelper) {
		String s = "";
		XSDatatypeManager xsdDatatypeManager = schemaHelper.getXSDatatypeManager();
		Map typeMap = xsdDatatypeManager.getDerivedTypes();
		Iterator i = typeMap.keySet().iterator();
		int cnt = 0;
		while (i.hasNext()) {
			String key = (String) i.next();
			XSDatatype def = (XSDatatype) xsdDatatypeManager.getTypeByName(key);
			/* 			s +=  NT + "key: " + key + " getName(): " + def.getName() + " displayName(): " + def.displayName();
			s += "  getNamespaceUri(): " + def.getNamespaceUri(); */
			s += NT + cnt++ + ": " + key;
		}

		prtlnBox(s, "Simple Derived Data Types");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showSchemaNodeMap(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		String s = "";
		Iterator i = schemaNodeMap.getKeys().iterator();
		while (i.hasNext()) {
			String path = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(path);
			s += NL + "path: " + path + " (" + schemaNode.getDocOrderIndex() + ")";
			GlobalDef typeDef = schemaNode.getTypeDef();
			if (typeDef == null)
				s += NT + " dataType: " + "TYPE DEF NOT FOUND";
			else
				s += NT + " dataType: " + schemaNode.getTypeDef().getQualifiedName();
		}
		prtlnBox(s, "SchemaNodeMap");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  uri           NOT YET DOCUMENTED
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showNSDefs(String uri, SchemaHelper schemaHelper) {
		prtlnBox(nsDefsToString(uri, schemaHelper), "NSDefs for " + uri);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  uri           NOT YET DOCUMENTED
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 * @return               NOT YET DOCUMENTED
	 */
	public static String nsDefsToString(String uri, SchemaHelper schemaHelper) {

		GlobalDefMap globalDefMap = schemaHelper.getGlobalDefMap();
		String s = "";
		Map nsMap = globalDefMap.getNsMap(uri);

		if (nsMap == null) {
			return "No Global Definitions found";
		}

		Iterator i = nsMap.keySet().iterator();
		while (i.hasNext()) {
			String key = (String) i.next();
			GlobalDef def = (GlobalDef) globalDefMap.getValue(key, uri);
			// s +=  NL + def.toString();
			String[] pathSplits = def.getClass().getName().split("\\.");
			String className = def.getClass().getName();
			if (pathSplits.length > 0)
				className = pathSplits[pathSplits.length - 1];
			s += NT + key + "  (" + className + ")";
			s += NTT + " qualified name: " + def.getQualifiedName();
			s += NTT + " qualified instance name: " + def.getQualifiedInstanceName();
			s += NTT + " namespaceURI: " + def.getNamespace().getURI();
			try {
				s += NTT + " location: " + def.getLocation();
				s += NTT + " schemaReader source: " + def.getSchemaReader().getLocation().toString();
			} catch (Throwable t) {}
		}

		return s;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showSubstitutionGroups(SchemaHelper schemaHelper) {
		GlobalDefMap globalDefMap = schemaHelper.getGlobalDefMap();
		String s = "";
		List globalElements = globalDefMap.getDefsOfType(GlobalDef.GLOBAL_ELEMENT);
		s += NT + "(" + globalElements.size() + " global elements found)";
		Iterator i = globalElements.iterator();
		while (i.hasNext()) {
			GlobalElement globalElement = (GlobalElement) i.next();
			List sg = globalElement.getSubstitutionGroup();
			if (sg.size() > 0) {
				s += NL + globalElement.getQualifiedInstanceName() + " subgroup has " + sg.size() + " members";
				for (Iterator ii = sg.iterator(); ii.hasNext(); ) {
					GlobalElement member = (GlobalElement) ii.next();
					s += NT + member.getQualifiedInstanceName();
				}
			}
		}
		prtlnBox(s, "substitution Groups");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showInstanceDoc(SchemaHelper schemaHelper) {
		prtlnBox(schemaHelper.getInstanceDocument(), "Instance Document");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showMinimalDocument(SchemaHelper schemaHelper) {
		prtlnBox(schemaHelper.getMinimalDocument(), "Minimal Document");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showGlobalDefs(SchemaHelper schemaHelper) {
		GlobalDefMap globalDefMap = schemaHelper.getGlobalDefMap();
		String s = "";
		Iterator i = globalDefMap.getNsKeys().iterator();
		while (i.hasNext()) {
			String nsUri = (String) i.next();
			s += NN + nsUri + NL;
			s += nsDefsToString(nsUri, schemaHelper);
		}
		prtlnBox(s, "Global Definitions");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showNodeDocumentation(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		String s = "";
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			// s +=  NL + "xpath: " + key;
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			String nodeDocs = schemaNode.getDocumentation();
			String typeDocs = schemaNode.getTypeDef().getDocumentation();
			if (nodeDocs != null || typeDocs != null) {
				s += NL + NL + key;
				if (nodeDocs != null)
					s += NL + "NODE: " + nodeDocs;
				if (typeDocs != null)
					s += NL + "TYPE: " + typeDocs;
				/* 				if (nodeDocs == null)
					s += NT + " no Node documentation";
				else
					s += NL + "NODE: " + nodeDocs;
				if (typeDocs == null)
					s += NT + " no Type documentation";
				else
					s += NL + "TYPE: " + typeDocs; */
			}
			else {
				// s += NL + key;
			}
		}
		prtlnBox(s, "SchemaNode Documentation");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showTypeDocumentation(SchemaHelper schemaHelper) {
		GlobalDefMap globalDefMap = schemaHelper.getGlobalDefMap();
		String s = "";
		Iterator i = globalDefMap.getNsKeys().iterator();
		while (i.hasNext()) {
			String nsUri = (String) i.next();
			s += NN + nsUri + NL;

			Map nsMap = globalDefMap.getNsMap(nsUri);

			if (nsMap == null) {
				continue;
			}

			Iterator j = nsMap.keySet().iterator();
			while (j.hasNext()) {
				String key = (String) j.next();
				GlobalDef def = (GlobalDef) globalDefMap.getValue(key, nsUri);
				// s +=  NL + def.toString();
				if (def == null) {
					prtln("def is NULL (key=" + key + ", nsUri: " + nsUri + ")");
					continue;
				}
				String doc = def.getDocumentation();
				String[] pathSplits = def.getClass().getName().split("\\.");
				String className = def.getClass().getName();
				if (pathSplits.length > 0)
					className = pathSplits[pathSplits.length - 1];
				s += NT + key + "  (" + className + ")";
				if (doc != null)
					s += NT + doc;
				s += NL;
			}
		}
		prtlnBox(s, "Type Documentation");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showUnboundedSchemaNodes(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		String s = "";
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			if (schemaNode.isUnbounded()) {
				// s += key + " (" + schemaNode.getDataTypeName() + ")";
				s += key + " (" + schemaNode.getTypeDef().getQualifiedName() + ")";
			}
		}
		prtlnBox(s, "Unbounded Schema Nodes");
	}


	/**
	 *  Display schema nodes for which the SchemaHelper.hasRepeatingComplexSingleton
	 *  predicate returns true.
	 *
	 * @param  schemaHelper  schemaHelper instance for particular framework.
	 */
	public static void showNodesHavingRepeatingComplexSingletons(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		String s = "";
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			// s +=  NL + "xpath: " + key;
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			if (schemaNode != null && 
				schemaNode.getTypeDef().isComplexType() &&
				schemaHelper.hasRepeatingComplexSingleton(key)) {
				// s += NL + key + " (" + schemaNode.getDataTypeName() + ")";
				s += NL + key + " (" + schemaNode.getTypeDef().getQualifiedName() + ")";
			}
		}
		prtlnBox(s, "Nodes having complex repeating singleton elements");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showRepeatingComplexSingletons(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		prtln("complex repeating singleton elements");
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			// prtln ("xpath: " + key);
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			if (schemaNode != null && schemaHelper.isRepeatingComplexSingleton(key)) {
				// prtln (key + " (" + schemaNode.getDataTypeName() + ")");
				prtln(key + " (" + schemaNode.getTypeDef().getQualifiedName() + ")");
			}
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showRepeatingElements(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		String s = "";
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			if (schemaHelper.isRepeatingElement(schemaNode)) {
				// s += NL + key + " (" + schemaNode.getDataTypeName() + ")";
				s += NL + key + " (" + schemaNode.getTypeDef().getQualifiedName() + ")";
			}
		}
		prtlnBox(s, "Repeating Elements");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showRequiredBranches(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		String s = "";
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			if (schemaHelper.isRequiredBranch(schemaNode)) {
				// s += NL + key + " (" + schemaNode.getDataTypeName() + ")";
				s += NL + key + " (" + schemaNode.getTypeDef().getQualifiedName() + ")";
			}
		}
		prtlnBox(s, "Required Branches");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showGlobalElements(SchemaHelper schemaHelper) {
		GlobalDefMap globalDefMap = schemaHelper.getGlobalDefMap();
		String s = "";
		List typeList = globalDefMap.getDefsOfType(GlobalDef.GLOBAL_ELEMENT);
		if (typeList.size() > 0) {
			Iterator i = typeList.iterator();
			while (i.hasNext()) {
				GlobalElement c = (GlobalElement) i.next();
				s += NL + c.toString();
			}
		}
		prtlnBox(s, "Global Elements");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showRequiredContentElements(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		prtln("Required Content Elements");

		List myPaths = new ArrayList();

		Iterator SchemaNodePaths = schemaNodeMap.getKeys().iterator();

		Iterator i = myPaths.iterator();

		while (i.hasNext()) {
			String key = (String) i.next();
			prtln("  looking at " + key);
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			if (schemaNode == null)
				prtln("  ...schemaNode not found");
			if (schemaHelper.isRequiredContentElement(schemaNode)) {
				// prtln (key + " (" + schemaNode.getDataTypeName() + ")");
				prtln(key + " (" + schemaNode.getTypeDef().getQualifiedName() + ")");
			}
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showMultiSelectElements(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		String s = "";
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			if (schemaHelper.isMultiSelect(schemaNode)) {
				// s += NL + key + " (" + schemaNode.getDataTypeName() + ")";
				s += NL + key + " (" + schemaNode.getTypeDef().getQualifiedName() + ")";
			}
		}
		prtlnBox(s, "Multiselect Elements");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showEnumerationTypes(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		String s = "";
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			GlobalDef globalDef = schemaNode.getTypeDef();
			if (globalDef != null && globalDef.isTypeDef()) {
				GenericType typeDef = (GenericType) globalDef;
				if (typeDef.isEnumerationType())
					s += NL + key + " (" + schemaNode.getTypeDef().getQualifiedName() + ")";
			}
		}
		prtlnBox(s, "Enumeration Types");
	}


	/**
	 *  Gets the choicePaths attribute of the SchemaUtils class
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 * @return               The choicePaths value
	 */
	public static List getChoicePaths(SchemaHelper schemaHelper) {
		List paths = new ArrayList();
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		String s = "";
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String path = (String) i.next();
			SchemaNode schemaNode = schemaHelper.getSchemaNode(path);
			if (schemaNode.hasChoiceCompositor())
				paths.add(path);
		}
		return paths;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showOptionalChoicePaths(SchemaHelper schemaHelper) {
		List choicePaths = getChoicePaths(schemaHelper);
		String s = "";
		for (Iterator i = choicePaths.iterator(); i.hasNext(); ) {
			String path = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaHelper.getSchemaNode(path);
			if (!schemaNode.isRequired())
				s += NL + schemaNode.getXpath();
		}
		prtlnBox(s, "Optional Choice Paths");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showOptionalChoicePathsOLD(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		String s = "";
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			if (schemaNode.hasChoiceCompositor() && !schemaNode.isRequired())
				s += NL + schemaNode.getXpath();
		}
		prtlnBox(s, "Optional Choice Paths");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showChoiceNodes(SchemaHelper schemaHelper) {
		List choicePaths = getChoicePaths(schemaHelper);
		String s = "";
		for (Iterator i = choicePaths.iterator(); i.hasNext(); ) {
			String path = (String) i.next();
			SchemaNode schemaNode = schemaHelper.getSchemaNode(path);
			Choice choice = (Choice) schemaNode.getCompositor();
			s += NL + schemaNode.getXpath() + " min: " + choice.getMinOccurs() +
				"   max: " + choice.getMaxOccurs();
		}
		prtlnBox(s, "Choice Nodes");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showChoiceNodesOLD(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		String s = "";
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			if (schemaNode.hasChoiceCompositor()) {
				Choice choice = (Choice) schemaNode.getCompositor();
				s += NL + schemaNode.getXpath() + " min: " + choice.getMinOccurs() +
					"   max: " + choice.getMaxOccurs();
			}
		}
		prtlnBox(s, "Choice Nodes");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showSimpleAndComplexContentElements(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		prtln("simple- and complexContent elements");
		List found = new ArrayList();
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			GlobalDef def = schemaHelper.getGlobalDef(schemaNode);
			if (def == null || !def.isComplexType())
				continue;
			ComplexType cType = (ComplexType) def;
			if (cType.isDerivedType())
				found.add(cType);
		}
		prtln(" ..." + found.size() + " elements found\n");
		for (Iterator i = found.iterator(); i.hasNext(); ) {
			ComplexType cType = (ComplexType) i.next();
			prtln(NL + Dom4jUtils.prettyPrint(cType.getElement()));
		}
	}


	/**
	 *  Gets the recursiveNodes attribute of the SchemaUtils class
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 * @return               The recursiveNodes value
	 */
	public static List getRecursiveNodes(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		List found = new ArrayList();
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			GlobalDef def = schemaHelper.getGlobalDef(schemaNode);
			if (schemaNode.isRecursive())
				found.add(schemaNode);
		}
		return found;
	}


	/**
	 *  Gets the derivedContentNodes attribute of the SchemaUtils class
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 * @return               The derivedContentNodes value
	 */
	public static List getDerivedContentNodes(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		List found = new ArrayList();
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			GlobalDef def = schemaHelper.getGlobalDef(schemaNode);
			if (def == null || !def.isComplexType())
				continue;
			ComplexType cType = (ComplexType) def;
			if (cType.hasComplexContent())
				found.add(schemaNode);
		}
		return found;
	}


	/**
	 *  Gets the modelGroups attribute of the SchemaUtils class
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 * @return               The modelGroups value
	 */
	public static List getModelGroups(SchemaHelper schemaHelper) {

		// look in globalDefMap for
		// globalDef.isModelGroup

		GlobalDefMap globalDefMap = schemaHelper.getGlobalDefMap();
		return globalDefMap.getDefsOfType(GlobalDef.MODEL_GROUP);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showDerivedContentModelNodes(SchemaHelper schemaHelper) {
		prtln("Derived Content Model Schema Nodes");
		List nodes = getDerivedContentNodes(schemaHelper);
		prtln(" ..." + nodes.size() + " nodes found");
		for (Iterator i = nodes.iterator(); i.hasNext(); ) {
			SchemaNode schemaNode = (SchemaNode) i.next();
			ComplexType cType = (ComplexType) schemaNode.getTypeDef();
			prtln(NL + schemaNode.getXpath());
			prtln(Dom4jUtils.prettyPrint(cType.getElement()));
		}
	}


	/**
	 *  Gets the derivedTextOnlyNodes attribute of the SchemaUtils class
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 * @return               The derivedTextOnlyNodes value
	 */
	public static List getDerivedTextOnlyNodes(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		List found = new ArrayList();
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			GlobalDef def = schemaHelper.getGlobalDef(schemaNode);
			if (def == null || !def.isComplexType())
				continue;
			ComplexType cType = (ComplexType) def;
			if (cType.hasSimpleContent())
				found.add(schemaNode);
		}
		return found;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showDerivedTextOnlyModelNodes(SchemaHelper schemaHelper) {
		prtln("\nDerived Text Only Model Schema Nodes");
		List nodes = getDerivedTextOnlyNodes(schemaHelper);
		prtln(" ..." + nodes.size() + " nodes found");
		for (Iterator i = nodes.iterator(); i.hasNext(); ) {
			SchemaNode schemaNode = (SchemaNode) i.next();
			ComplexType cType = (ComplexType) schemaNode.getTypeDef();
			prtln(NL + schemaNode.getXpath());
			// prtln (Dom4jUtils.prettyPrint(cType.getElement()));
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showXSDStringExtensionFields(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		String s = "";
		String stringType = NamespaceRegistry.makeQualifiedName(schemaHelper.getSchemaNamespace().getPrefix(), "string");
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			if (schemaNode.isDerivedModel() &&
				schemaNode.getValidatingType().getName().equals(stringType)) {
				s += NL + key + " (" + schemaNode.getTypeDef().getQualifiedName() + ")";
			}
		}
		prtlnBox(s, stringType + " Extension fields");
	}


	/**
	 *  show fields of xxx:string dataType, where xxx refers to the namespace
	 *  prefix for the schemaNamespace.
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showXSDStringFields(SchemaHelper schemaHelper) {
		SchemaNodeMap schemaNodeMap = schemaHelper.getSchemaNodeMap();
		String s = "";
		String stringType = NamespaceRegistry.makeQualifiedName(schemaHelper.getSchemaNamespace().getPrefix(), "string");
		for (Iterator i = schemaNodeMap.getKeys().iterator(); i.hasNext(); ) {
			String key = (String) i.next();
			SchemaNode schemaNode = (SchemaNode) schemaNodeMap.getValue(key);
			if (schemaNode.getValidatingType().getName().equals("stringType")) {
				s += NL + key + " (" + schemaNode.getTypeDef().getQualifiedName() + ")";
			}
		}
		prtlnBox(s, stringType + " elements");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  schemaHelper  NOT YET DOCUMENTED
	 */
	public static void showComboUnionFields(SchemaHelper schemaHelper) {
		GlobalDefMap globalDefMap = schemaHelper.getGlobalDefMap();
		prtln("globalDefMap has " + globalDefMap.getValues().size() + " items");
		prtln("xsd:Combo union elements");
		for (Iterator i = globalDefMap.getValues().iterator(); i.hasNext(); ) {
			GlobalDef def = (GlobalDef) i.next();
			// prtln ("name: " + def.getName() + "  dataType: " + def.getDataType() + "  location: " + def.getLocation());
			if (schemaHelper.isComboUnionType(def)) {
				prtln(NL + "name: " + def.getName() + NL + def.getElement().asXML());
				prtln("enumeration values:");
				// for (Iterator v=schemaHelper.getEnumerationValues(def.getName(), false).iterator();v.hasNext();) {
				for (Iterator v = ((SimpleType) def).getEnumerationValues(false).iterator(); v.hasNext(); ) {
					prtln("\t" + (String) v.next());
				}
			}
		}
	}


	/**
	 *  Based on logic of RendererImpl.renderRepeatingElement, prints out the name
	 *  of the class instantiated to render provided schemaNode. <p>
	 *
	 *  Used to determine which nodes are rendered as which Mde Classes
	 *
	 * @param  schemaNode  NOT YET DOCUMENTED
	 * @param  typeDef     NOT YET DOCUMENTED
	 */
	public void whatKindOfRepeatingElement(SchemaNode schemaNode, GlobalDef typeDef) {
		// prtln ("\whatKindOfRepeatingElement()");

		if (typeDef == null)
			typeDef = schemaNode.getTypeDef();

		if (typeDef.isAnyType()) {
			prtln("RepeatingAnyType");
		}
		else if (typeDef.isSimpleType() || typeDef.isBuiltIn()) {
			prtln("RepeatingSimpleType");
		}
		else if (schemaNode.isDerivedContentModel()) {
			prtln("RepeatingDerivedContentModel");
		}
		else if (schemaNode.isDerivedTextOnlyModel()) {
			prtln("RepeatingDerivedTextOnlyModel");
		}
		else {
			prtln("RepeatingComplexType");
		}
	}

	// -- printing and displaying stuff
	/**
	 *  Description of the Method
	 *
	 * @param  node  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	public static String pp(Node node) {
		return Dom4jUtils.prettyPrint(node);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n  NOT YET DOCUMENTED
	 */
	public static void prtln(Node n) {
		prtln(n, null);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n       NOT YET DOCUMENTED
	 * @param  prefix  NOT YET DOCUMENTED
	 */
	public static void prtln(Node n, String prefix) {
		prtln(pp(n), prefix);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s       NOT YET DOCUMENTED
	 * @param  prefix  NOT YET DOCUMENTED
	 */
	public static void prtln(String s, String prefix) {

		while (s.length() > 0 && s.charAt(0) == '\n') {
			System.out.println("");
			s = s.substring(1);
		}

		if (prefix == null || prefix.trim().length() == 0)
			System.out.println(s);
		else
			System.out.println(prefix + ": " + s);
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	public static void prtln(String s) {
		// System.out.println("SchemEditUtils: " + s);
		// System.out.println(s);
		prtln(s, "");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s       NOT YET DOCUMENTED
	 * @param  header  NOT YET DOCUMENTED
	 */
	public static void prtlnBox(String s, String header) {
		prtln("\n----------------------------------");
		if (header == null || header.trim().length() == 0)
			prtln(s);
		else
			prtln("* " + header + " *\n" + s);
		prtln("----------------------------------\n");
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  n       NOT YET DOCUMENTED
	 * @param  header  NOT YET DOCUMENTED
	 */
	public static void prtlnBox(Node n, String header) {
		prtln("\n----------------------------------");
		if (header == null || header.trim().length() == 0)
			prtln(Dom4jUtils.prettyPrint(n));
		else
			prtln("* " + header + " *\n" + Dom4jUtils.prettyPrint(n));
		prtln("----------------------------------\n");
	}

}

