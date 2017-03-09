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
package org.dlese.dpc.ndr.dds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.dlese.dpc.ndr.apiproxy.NDRAPIProxy;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.dom4j.Text;
import org.dom4j.io.SAXReader;
import org.dom4j.Element;
import org.dom4j.XPath;

/**
 * @author kmaull
 *
 */
public class CollectionImporter {
	
	private int collectionType = 1; // default = 1 ( local ) 
	private Document document;
    private HashMap<String, ArrayList<String>> metaObjects = null;  
	
	
	public CollectionImporter()
	{}
	
	public void setCollectionType ( int _type )
	{
		collectionType = _type;
	}
	
	public int getCollectionType ()
	{
		return collectionType;
	}
	
	
	public String getCollectionKeyFromDDS( String _collectionName )
	{
		String collectionKey = null;

		// make the call to DDS and get ListCollections 
		try { 
			URLConnection connection = new URL ("http://www.dlese.org/dds/services/ddsws1-1?verb=ListCollections" ).openConnection();			
	        connection.setDoOutput( true );
	        connection.setDoInput(true);
	        
	        ((HttpURLConnection)connection).setRequestMethod("GET");
	        
	        Map<String,String> uris = new HashMap<String,String>();
		    uris.put( "ddsws", "http://www.dlese.org/Metadata/ddsws" );	       
	        uris.put( "ddswsnews", "http://www.dlese.org/Metadata/ddswsnews" );
		    uris.put( "groups", "http://www.dlese.org/Metadata/groups/" );
		    uris.put( "adn", "http://adn.dlese.org" );
		    uris.put( "annotation", "http://www.dlese.org/Metadata/annotation" );
		    
		    XPath xpath = DocumentHelper.createXPath( "//collections/collection[vocabEntry=\"" + _collectionName + "\"]/searchKey/text()" );

		    xpath.setNamespaceURIs( uris );
		    
		    SAXReader xmlReader = new SAXReader();
		    
		    this.document = xmlReader.read(connection.getInputStream());	
		            
		    Text t = ((Text)xpath.selectSingleNode(this.document));
		    
			collectionKey = t.getStringValue();
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
		return collectionKey;
	}
	
	
	public long importCollection ( String _vocabEntry, boolean _serializeMetaObjects )
	{
		long importTime = -1;
		
		if ( _vocabEntry == null ) // default = comet.xml test 
		{
			// load local collection 
			String collection = "C:\\ndr\\dds\\comet.xml";
			try {
				File collectionFile = new File(collection);
				
			    SAXReader xmlReader = new SAXReader();
		
			    this.document = xmlReader.read(collectionFile);	
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}	else {
			
				String key = getCollectionKeyFromDDS(_vocabEntry);
				
				// load the WS document
				this.document = getCollectionDocumentFromWebService ( key );
			
			    // load the collection into the importer structures
			    loadCollection();
			
			    if ( metaObjects.size() > 0 )
			    {
			    	
			    	// TEST ONLY : TIMER START
			        long startTimer = 0, endTimer = 0, runningTime = 0; 
			    	startTimer = System.currentTimeMillis();
			    	
			    	System.out.println ( "<><><><>START TIME = " + startTimer );
			    	System.out.println ( "<><><><>Total Objects Ingesting = " + metaObjects.size() );
			    	
					NDRAPIProxy proxy = new NDRAPIProxy ();
					proxy.DEBUG_ON = true;
					
					// create the metadatProvider for this collection
					String dcDesc = 
						"<dc:title>Comet Test Collection</dc:title>\n" +
						"<dc:description>TEST COLLECTION IMPORT FROM DLESE</dc:description>\n" +
						"<dc:identifier>http://dlese.org/</dc:identifier>\n" 
						;

				/* TODO : update MDP signature
					String metadataProviderHandle = proxy.addMetaDataProvider( 
							dcDesc,
							new String[][] {
										{"metadataProviderFor", proxy.getAgentHandle() }
								}
							 );				
				*/
					
					// iterate over the loaded collection and bang, bang go!
					int i = 0;
					for ( Entry<String,ArrayList<String>> m : metaObjects.entrySet() )
					{
						//	FOR TESTING ONLY!  JUST TIME THE ADD/RELS prior
						String resourceURL = m.getValue().get(0);
						String resourceMetadata = m.getValue().get(1);						

						// TEST : start timer
						startTimer = System.currentTimeMillis();
						
/* TODO : update the resource and metatadata signatures
 						String resHandle = proxy.addResource(
											resourceURL, 
											new String[][]{} 
											);
						
						String metadataHandle = proxy.addMetadata(
								resourceMetadata,
								null,
								new String[][] { 
										{"metadataFor",resHandle},
										{"metadataProvidedBy",metadataProviderHandle}
									}
								);	
*/						// TEST : end timer and update running time
						endTimer = System.currentTimeMillis();
						importTime = importTime + (endTimer-startTimer);

						// DEBUG ONLY
						/*System.out.println ( 
								"<" + i + "\n[key = " + m.getKey() + "]\n" + 
								"[rH  = " + resHandle + "]\n" +
								"[mdH = " + metadataHandle + "]\n"+
								"call_time = " + (endTimer-startTimer) +"ms>\n"
						);*/
						i++;
					}					
		    
			    	// DEBUG ONLY : TIMER START
			    	endTimer = System.currentTimeMillis();
			    	System.out.println ( "<><><><>RUNNING TIME = " + importTime );
					
			    	// Serialize the metaObjects
			    	if ( _serializeMetaObjects )
					{
			    		try {
							FileOutputStream fileOut = new FileOutputStream("c:\\ndr\\serialized\\metaObjects."+System.currentTimeMillis()/Math.pow(10, 8)+".ser");
							ObjectOutputStream out = new ObjectOutputStream(fileOut);
							out.writeObject(this.metaObjects);
			    		} catch ( Exception e ) {
			    			e.printStackTrace();
			    		}
					}
			    }
		}

		return importTime;
	}
	
	private Document getCollectionDocumentFromWebService ( String _key )
	{
		Document collectionDocument = null;

		// make the call to DDS and get ListCollections 
		try {
			URLConnection connection = new URL ("http://www.dlese.org/dds/services/ddsws1-1?verb=Search&ky=" + _key + "&n=200&s=0" ).openConnection();
			
	        connection.setDoOutput( true );
	        connection.setDoInput(true);
	        
	        ((HttpURLConnection)connection).setRequestMethod("GET");
	        		    
		    SAXReader xmlReader = new SAXReader();
		    
		    collectionDocument = xmlReader.read(connection.getInputStream());	
		            
		} catch ( Exception e ) {
			e.printStackTrace();
		}		
		
		return collectionDocument;
	}
	
	public int getRecordCount () 
	{				
	    Map<String,String> uris = new HashMap<String,String>();
	    uris.put( "ddsws", "http://www.dlese.org/Metadata/ddsws" );
	    uris.put( "groups", "http://www.dlese.org/Metadata/groups/" );
	    uris.put( "adn", "http://adn.dlese.org" );
	    uris.put( "annotation", "http://www.dlese.org/Metadata/annotation" );
	    
	    XPath xpath = DocumentHelper.createXPath( "//ddsws:results/ddsws:record" );

	    xpath.setNamespaceURIs( uris );
	    
		List recordNodes = xpath.selectNodes(this.document);
	    
	    return recordNodes.size();		
	}
	
	public HashMap<String,ArrayList<String>> getMetadataObjects()
	{
		if ( metaObjects == null )
		{
			loadCollection();
		}
		
		return metaObjects;
	}
	
	public void loadCollection()
	{
		if ( metaObjects == null ) {
		    metaObjects = new HashMap<String, ArrayList<String>>();  
			
			Map<String,String> uris = new HashMap<String,String>();
		    uris.put( "ddsws", "http://www.dlese.org/Metadata/ddsws" );
		    uris.put( "groups", "http://www.dlese.org/Metadata/groups/" );
		    uris.put( "adn", "http://adn.dlese.org" );
		    uris.put( "annotation", "http://www.dlese.org/Metadata/annotation" );
		    
		    XPath xpath = DocumentHelper.createXPath( "//ddsws:results/ddsws:record" );
	
		    xpath.setNamespaceURIs( uris );
		    
			List recordNodes = xpath.selectNodes(this.document);
		    
		    for ( Iterator iter = recordNodes.iterator(); iter.hasNext(); ) 
		    {
		        Element element = (Element) iter.next();
		        
		        XPath meta = DocumentHelper.createXPath( "./ddsws:metadata" ); 
		        XPath resURL = DocumentHelper.createXPath( "./ddsws:metadata/adn:itemRecord/adn:technical/adn:online/adn:primaryURL" );
		        XPath resId = DocumentHelper.createXPath("./ddsws:head/ddsws:additionalMetadata/ddsws:adn/ddsws:annotatedBy/ddsws:record/ddsws:head/ddsws:id");//adn:adn/adn:annotatedBy/adn:record/adn:head/adn:id");
		        
		        meta.setNamespaceURIs( uris );
		        resId.setNamespaceURIs( uris );
		        resURL.setNamespaceURIs( uris );
		        
		        Node metadata = meta.selectSingleNode(element);
		        Element resourceId = (Element)resId.selectSingleNode(element);
		        Element resourceURL = (Element)resURL.selectSingleNode(element);
		        
		        if ( resourceId != null && resourceURL != null && metadata != null  )
		        {
		        	// add the resId as the hash key
		        	metaObjects.put(
	        			resourceId.getTextTrim(), 
	        			new ArrayList<String>(Arrays.asList( resourceURL.getTextTrim(), metadata.asXML() ))
        			);		        		        
		        }		        
		    }
		}		    
	}
}
