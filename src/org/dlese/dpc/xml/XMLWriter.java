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
package org.dlese.dpc.xml;

import java.io.*;
import java.util.*;

import org.jdom.*;
import org.jdom.output.*;

/**
 * Subclassed from JDOM's <code>XMLOutputter</code>, in order to tweak
 * output. JDOM documentation should be consulted for actual details.
 *
 * @author	Dave Deniman (as revised)
 * @version	1.0,	01/01/02
 *
 */
public class XMLWriter extends XMLOutputter {

	static String encoding = "UTF-8";
	static String STANDARD_INDENT = "\t";
	static String newline = "\r\n";


	/** Should we preserve whitespace or not in text nodes */
	private boolean textNormalize = true;


	static XMLOutputter.NamespaceStack namespaces;
		
	public XMLWriter() {
		super();
		setEncoding(encoding);
		setIndent(STANDARD_INDENT);
		namespaces = createNamespaceStack();
	}


	public void write(org.jdom.Document doc, File out) {
		try {
			
			//FileOutputStream fstr = new FileOutputStream(out);
			//FileWriter fileWriter = new FileWriter(out);
			//System.err.println("using encoding: " + fileWriter.getEncoding());
			
			OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(out), encoding); 
			//System.err.println("using encoding: " + fileWriter.getEncoding());

			printDeclaration(doc, fileWriter, encoding);
			printDocType(doc.getDocType(), fileWriter);
			
			List list = doc.getContent();
			Iterator i = list.iterator();
			while (i.hasNext()) {
				//System.err.println("list object = " + i.next().toString());
				Object object = i.next();
				if (object instanceof Comment) {
					printComment((Comment)object, fileWriter);
				}
				else if (object instanceof Element) {
					//System.err.println("printing element: " + object.toString());
					setTextNormalize(true);
					setNewlines(true);
					printElement((Element)object, fileWriter, 0, namespaces);
					//printElements((Element)object, 0);
				}
				else {
					//System.err.println("unchecked type not printed: " + object.toString());
				}
				setTextNormalize(false);
				setNewlines(false);
				printString(newline, fileWriter);
			}
			fileWriter.close();
		} 
		catch (Exception e) {
			System.err.println(e.getClass() + " threw test exception - ");
			System.err.println("message: " + e.getMessage());
			e.printStackTrace();
			//System.exit(1);
		}
	}



	protected void printElementContent(Element element, Writer out,
	                                   int indentLevel,
	                                   NamespaceStack namespaces,
	                                   List eltContent) throws IOException {
	    // get same local flags as printElement does
	    // a little redundant code-wise, but not performance-wise
		element.setText(element.getTextNormalize());
	    boolean empty = eltContent.size() == 0;

	    // Calculate if the content is String/CDATA only
	    boolean stringOnly = true;
	    if (!empty) {
	        stringOnly = isStringOnly(eltContent);
	    }

	    if (stringOnly) {
	        //printString("strOnly",  out);
	        Class justOutput = null;
	        boolean endedWithWhite = false;
	        Iterator itr = eltContent.iterator();
	        while (itr.hasNext()) {
	            Object content = itr.next();
	            if (content instanceof String) {
	                String scontent = (String) content;
	                if (justOutput == CDATA.class && 
	                      textNormalize &&
	                      startsWithWhite(scontent)) {
	                    out.write(" ");
	                }
					if (!isWhitespace((String)content)) {
		                printString(scontent, out);
					}
		            endedWithWhite = endsWithWhite(scontent);
		            justOutput = String.class;
	            }
	            else {
	                // We're in a CDATA section
	                if (justOutput == String.class &&
	                      textNormalize &&
	                      endedWithWhite) {
	                    out.write(" ");  // padding
	                }
					//System.err.println("not CDATA idiot!");
	                printCDATA((CDATA)content, out);
	                justOutput = CDATA.class;
	            }
	        }
	    }
	    else {
	        // Iterate through children
	        Object content = null;
	        Class justOutput = null;
	        boolean endedWithWhite = false;
	        boolean wasFullyWhite = false;
	        Iterator itr = eltContent.iterator();
	        while (itr.hasNext()) {
	            content = itr.next();
	            // See if text, an element, a PI or a comment
	            if (content instanceof Comment) {
	                if (!(justOutput == String.class && wasFullyWhite)) {
	                    maybePrintln(out);
	                    indent(out, indentLevel);
	                }
	                printComment((Comment) content, out);
	                justOutput = Comment.class;
	            }
	            else if (content instanceof String) {
					if (!isWhitespace((String)content)) {
						//printString("+",  out);
		                String scontent = (String) content;
		                if (justOutput == CDATA.class && 
		                      textNormalize &&
		                      startsWithWhite(scontent)) {
		                    out.write(" ");
		                }
		                else if (justOutput != CDATA.class && 
		                         justOutput != String.class) {
		                    //printString("@",  out);
		                    maybePrintln(out);
		                    indent(out, indentLevel);
		                }
	                	//if (!isWhitespace((String)scontent)) {
	                	printString(scontent, out);
	                	//}
	                	//printString(scontent, out);
	                	endedWithWhite = endsWithWhite(scontent);
	                	justOutput = String.class;
	                	wasFullyWhite = (scontent.trim().length() == 0);
						//if (wasFullyWhite) printString("@", out);
					}
	            }
	            else if (content instanceof Element) {
	                if (!(justOutput == String.class && wasFullyWhite)) {
	                    //printString(":", out);
	                    maybePrintln(out);
	                    indent(out, indentLevel);
	                }
	                printElement((Element) content, out,
	                             indentLevel, namespaces);
	                justOutput = Element.class;
	            }
	            else if (content instanceof EntityRef) {
	                if (!(justOutput == String.class && wasFullyWhite)) {
	                    maybePrintln(out);
	                    indent(out, indentLevel);
	                }
	                printEntityRef((EntityRef) content, out);
	                justOutput = EntityRef.class;
	            }
	            else if (content instanceof ProcessingInstruction) {
	                if (!(justOutput == String.class && wasFullyWhite)) {
	                    maybePrintln(out);
	                    indent(out, indentLevel);
	                }
	                printProcessingInstruction((ProcessingInstruction) content,
	                                           out);
	                justOutput = ProcessingInstruction.class;
	            }
	            else if (content instanceof CDATA) {
	                if (justOutput == String.class &&
	                      textNormalize &&
	                      endedWithWhite) {
	                    out.write(" ");  // padding
	                }
	                else if (justOutput != String.class &&
	                         justOutput != CDATA.class) {
	                    maybePrintln(out);
	                    indent(out, indentLevel);
	                }
	                printCDATA((CDATA)content, out);
	                justOutput = CDATA.class;
	            }
	            // Unsupported types are *not* printed, nor should they exist
	        }
	        maybePrintln(out);
	        indent(out, indentLevel - 1);
	    }
	}  // printElementContent



	// Return true if the element's content list consists only of
	// String or CDATA nodes (or is empty)
	private boolean isStringOnly(List eltContent) {
	    // Calculate if the contents are String/CDATA only
	    Iterator itr = eltContent.iterator();
	    while (itr.hasNext()) {
	        Object o = itr.next();
	        if (!(o instanceof String) && !(o instanceof CDATA)) {
	            return false;
	        }
	    }
	    return true;
	}


	private boolean startsWithWhite(String s) {
	    return (s.length() > 0 && s.charAt(0) <= ' ');
	}


	private boolean endsWithWhite(String s) {
	    return (s.length() > 0 && s.charAt(s.length() - 1) <= ' ');
	}


	// true if string is all whitespace (space, tab, cr, lf only)
	private boolean isWhitespace(String s) {
	    char[] c = s.toCharArray();
	    for (int i=0; i<c.length; ++i) {
			int x = (int)c[i];
			if (c[i] > ' ') {
	            return false;
	        }
	    }
	    return true;
	}

/*
	void printElements(Element element, int indent) {
		// Print out elements, starting with no indention
		try {
		    Iterator i = element.getContent().iterator();
		    while (i.hasNext()) {
		        for (int j=0; j<indent; j++) {
		        	System.out.print(" ");
		        }				
		        Object obj = i.next();
		        if (obj instanceof Element) {
			        System.out.println(obj.toString());
			        printElements((Element)obj, indent + 5);
				}
				else if (obj instanceof String) {
					//String tmp = ((String)obj).trim();
					//if (tmp.length() > 0) {
						System.out.println("[String: " + (String)obj + "]");
					//}
				}
				else if (obj instanceof Comment) {
					System.out.println(obj.toString());
				}
				else {
					System.out.println("Object class = " + obj.getClass());
				}
				
//		            // 0 is indentation
//					
//		            printElement(doc.getRootElement(), writer, 0);
//		        } else if (obj instanceof Comment) {
//		        	maybePrintln(writer);
//		            writer.print(((Comment)obj).getSerializedForm());
//		        }
		    }
		} catch (NoSuchElementException e) {
		    // No elements to print
		}
	}
*/
}
