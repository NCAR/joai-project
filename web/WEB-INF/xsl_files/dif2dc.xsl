<?xml version="1.0" encoding="UTF-8" ?>

<!--
    Document    : dif2dc
    Created on  : September 19, 2006, 3:20 PM
    Author      : Michel.Larour@ifremer.fr
    Description : XSL style sheet for DIF to oai_dc transformation.
                  DIF    = Directory Interchange Format
                  oai_dc = Dublin Core for OAI (Open Archives Initiative)
                  
    Comments    : dif:Role='Principal Investigator' test is added only for a bug
                  in some DIF entry files used at Ifremer (dif:Role='Principal Investigator' used
                  instead of dif:Role='Investigator') 
-->

<!-- Stylesheet added to jOAI software June 2008
	Contact: John Weatherley jweather@ucar.edu -->

   <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                   xmlns:dif="http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/"
                   xmlns:dc="http://purl.org/dc/elements/1.1/"
                   xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                   exclude-result-prefixes="dif"
                   version="1.0">
    
   <xsl:output method="xml" indent="yes" />    
    
   <!-- Dublin Core Metadata from dif:DIF-->
   <xsl:template match="/">
<oai_dc:dc xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
           <xsl:apply-templates select="dif:DIF"/>              
</oai_dc:dc>                    
   </xsl:template>
    
   <!-- Dublin Core Metadata apply-templates from dif:DIF -->
   <xsl:template match="dif:DIF" >
       <xsl:apply-templates select="dif:Entry_Title" mode="dif-Entry_Title_2_dc-title"/>        
       <xsl:apply-templates select="dif:Data_Set_Citation/dif:Dataset_Creator"        mode="dif-Data_Set_Citation_dif-Dataset_Creator_2_dc-creator"/>
       <xsl:apply-templates select="dif:Personnel[dif:Role='Principal Investigator' or dif:Role='Investigator']" mode="dif-Personnel_2_dc-creator"/>
       <xsl:apply-templates select="dif:Keyword"     mode="dif-Keyword_2_dc-subject"/>        
       <xsl:apply-templates select="dif:Parameters"  mode="dif-Parameters_2_dc-subject"/>       
       <xsl:apply-templates select="dif:Source_Name" mode="dif-Source_Name_2_dc-subject"/>
       <xsl:apply-templates select="dif:Sensor_Name" mode="dif-Sensor_2_dc-subject"/>
       <xsl:apply-templates select="dif:Project"     mode="dif-Project_2_dc-subject"/>
       <xsl:apply-templates select="dif:Location"    mode="dif-Location_2_dc-subject"/>
       <xsl:apply-templates select="dif:Summary"     mode="dif-Summary_2_dc-description"/>        
       <xsl:apply-templates select="dif:Data_Set_Citation/dif:Dataset_Publisher" mode="dif-Data_Set_Citation_dif-Dataset_Publisher_2_dc-publisher"/>
       <xsl:apply-templates select="dif:Data_Center" mode="dif-Data_Center_2_dc-publisher"/>        
       <xsl:apply-templates select="dif:Personnel"   mode="dif-Personnel_2_dc-contributor"/>        
       <xsl:apply-templates select="dif:Data_Set_Citation/dif:Dataset_Release_Date"     mode="dif-Data_Set_Citation_dif-Dataset_Release_Date_2_dc-date"/>        
       <xsl:apply-templates select="dif:Data_Set_Citation/dif:Data_Presentation_Form"   mode="dif-Data_Set_Citation_dif-Data_Presentation_Form_2_dc-type"/>        
       <xsl:apply-templates select="dif:Distribution"   mode="dif-Distribution_2_dc-format"/>        
       <xsl:apply-templates select="dif:Data_Center/dif:Data_Set_ID" mode="dif-Data_Center_dif-Data_Set_ID_2_dc-identifier"/>        
       <xsl:apply-templates select="dif:Data_Set_Citation/dif:Online_Resource" mode="dif-Data_Set_Citation_dif-Online_Resource_2_dc-identifier"/>        
       <xsl:apply-templates select="dif:Related_URL" mode="dif-Related_URL_2_dc-identifier"/>        
       <xsl:apply-templates select="dif:Related_URL" mode="dif-Related_URL_2_dc-source"/>        
       <xsl:apply-templates select="dif:Source_Name" mode="dif-Source_Name_2_dc-source"/>        
       <xsl:apply-templates select="dif:Data_Set_Language" mode="dif-Data_Set_Language_2_dc-language"/>        
       <xsl:apply-templates select="dif:Parent_DIF" mode="dif-Parent_DIF_2_dc-relation"/>        
       <xsl:apply-templates select="dif:Data_Set_Citation/dif:Online_Resource" mode="dif-dif-Data_Set_Citation_dif-Online_Resource_2_dc-relation"/>        
       <xsl:apply-templates select="dif:Related_URL" mode="dif-Related_URL_2_dc-relation"/>        
       <xsl:apply-templates select="dif:Reference"   mode="dif-Reference_2_dc-relation"/>        
       <xsl:apply-templates select="dif:Location"    mode="dif-Location_2_dc-coverage"/>
       <xsl:apply-templates select="dif:Spatial_Coverage"    mode="dif-Spatial_Coverage_2_dc-coverage"/>
       <xsl:apply-templates select="dif:Temporal_Coverage"   mode="dif-Temporal_Coverage_2_dc-coverage"/>
       <xsl:apply-templates select="dif:Paleo_Temporal_Coverage"   mode="dif-Paleo_Temporal_Coverage_2_dc-coverage"/>
       <xsl:apply-templates select="dif:Use_Constraints"   mode="dif-Use_Constraints_2_dc-right"/>
       <xsl:apply-templates select="dif:Access_Constraints"   mode="dif-Access_Constraints_2_dc-right"/>       
   </xsl:template>    
   
   <!-- dif-Entry_Title_2_dc-title -->       
   <xsl:template match="dif:Entry_Title" mode="dif-Entry_Title_2_dc-title" >
      <dc:title>
         <xsl:value-of select="." />
      </dc:title>
   </xsl:template>
    
   <!-- dif-Data_Set_Citation_dif-Dataset_Creator_2_dc-creator -->       
   <xsl:template match="dif:Data_Set_Citation/dif:Dataset_Creator" mode="dif-Data_Set_Citation_dif-Dataset_Creator_2_dc-creator" >
      <dc:creator>
         <xsl:value-of select="." />
      </dc:creator>
   </xsl:template>
    
   <!-- dif:Personnel_2_dc-creator -->
   <xsl:template match="dif:Personnel" mode="dif-Personnel_2_dc-creator" >
      <dc:creator>
         <xsl:value-of select="dif:Last_Name" />
         <xsl:if test="dif:First_Name">
            <xsl:text> </xsl:text>
            <xsl:value-of select="dif:First_Name" />
         </xsl:if>         
         <xsl:if test="dif:Middle_Name">
            <xsl:text>,</xsl:text>
            <xsl:value-of select="dif:Middle_Name" />
         </xsl:if>
      </dc:creator>
   </xsl:template>
   
   <!-- dif-Keyword_2_dc-subject -->       
   <xsl:template match="dif:Keyword" mode="dif-Keyword_2_dc-subject" >
      <dc:subject>
         <xsl:value-of select="." />
      </dc:subject>
   </xsl:template>
    
   <!-- dif-Parameters_2_dc-subject -->
   <xsl:template match="dif:Parameters" mode="dif-Parameters_2_dc-subject">
      <dc:subject>
         <xsl:value-of select="dif:Category" />
         <xsl:text>/</xsl:text>
         <xsl:value-of select="dif:Topic" />
         <xsl:text>/</xsl:text>
         <xsl:value-of select="dif:Term" />
         <xsl:if test="dif:Variable">
            <xsl:text>/</xsl:text>
            <xsl:value-of select="dif:Variable" />
         </xsl:if>   
         <xsl:if test="dif:Detailed_Variable">
            <xsl:text> (</xsl:text>
            <xsl:value-of select="dif:Detailed_Variable" />
            <xsl:text>)</xsl:text>
         </xsl:if>
      </dc:subject>
   </xsl:template>
   
   <!-- dif-Source_Name_2_dc-subject -->
   <xsl:template match="dif:Source_Name" mode="dif-Source_Name_2_dc-subject">
       <dc:subject>
         
         <xsl:value-of select="dif:Short_Name" />
         <xsl:if test="dif:Long_Name">
            <xsl:text> (</xsl:text>
            <xsl:value-of select="dif:Long_Name" />
            <xsl:text>)</xsl:text>
         </xsl:if>
      </dc:subject>
   </xsl:template>
    
   <!-- dif-Sensor_2_dc-subject -->
   <xsl:template match="dif:Sensor_Name" mode="dif-Sensor_2_dc-subject">
      <dc:subject>
         <xsl:value-of select="dif:Short_Name" />
         <xsl:if test="dif:Long_Name">
            <xsl:text> (</xsl:text>
            <xsl:value-of select="dif:Long_Name" />
            <xsl:text>)</xsl:text>
          </xsl:if>
      </dc:subject>
   </xsl:template>
    
   <!-- dif-Project_2_dc-subject -->
   <xsl:template match="dif:Project" mode="dif-Project_2_dc-subject">
      <dc:subject>
         <xsl:value-of select="dif:Short_Name" />
         <xsl:if test="dif:Long_Name">
            <xsl:text> (</xsl:text>
            <xsl:value-of select="dif:Long_Name" />
            <xsl:text>)</xsl:text>
          </xsl:if>
      </dc:subject>
   </xsl:template>
   
   <!-- dif-Location_2_dc-subject -->
   <xsl:template match="dif:Location" mode="dif-Location_2_dc-subject">
      <dc:subject>
         <xsl:value-of select="dif:Location_Name" />
         <xsl:if test="dif:Detailed_Location">
            <xsl:text> (</xsl:text>
            <xsl:value-of select="dif:Detailed_Location" />
            <xsl:text>)</xsl:text>
         </xsl:if>
      </dc:subject>
   </xsl:template>
   
   <!-- dif-Summary_2_dc-description -->       
   <xsl:template match="dif:Summary" mode="dif-Summary_2_dc-description" >
      <dc:description>
         <xsl:value-of select="." />
      </dc:description>
   </xsl:template>
   
   <!-- dif-Data_Set_Citation_dif-Dataset_Publisher_2_dc-Publisher -->
   <xsl:template match="dif:Data_Set_Citation/dif:Dataset_Publisher" 
                  mode="dif-Data_Set_Citation_dif-Dataset_Publisher_2_dc-publisher" >
      <dc:publisher>
         <xsl:value-of select="." />         
      </dc:publisher>
   </xsl:template>
   
   <!-- dif-Data_Center_2_dc-publisher -->
   <xsl:template match="dif:Data_Center" mode="dif-Data_Center_2_dc-publisher"> 
      <dc:publisher>
         <xsl:value-of select="Data_Center_Name/dif:Short_Name" />
         <xsl:if test="Data_Center_Name/dif:Long_Name">
            <xsl:if test="Data_Center_Name/dif:Short_Name">
               <xsl:text> (</xsl:text>
            </xsl:if>
               <xsl:value-of select="Data_Center_Name/dif:Long_Name" />
            <xsl:if test="Data_Center_Name/dif:Short_Name">
               <xsl:text>) </xsl:text>
            </xsl:if>
         </xsl:if>
         <xsl:if test="dif:Data_Center_URL">
            <xsl:if test="Data_Center_Name/dif:Short_Name or Data_Center_Name/dif:Long_Name">
               <xsl:text> </xsl:text>
            </xsl:if>        
            <xsl:value-of select="dif:Data_Center_URL" />
         </xsl:if>
         <xsl:apply-templates select="dif:Personnel" mode="dif-Personnel_2_dc-publisher"/>
      </dc:publisher>
   </xsl:template>      
   
    
   <!-- dif-Personnel_2_dc-publisher -->
   <xsl:template match="dif:Personnel" mode="dif-Personnel_2_dc-publisher" >
        <xsl:text> - </xsl:text>
        <xsl:value-of select="dif:Last_Name" />
        <xsl:if test="dif:First_Name">
            <xsl:if test="dif:Last_Name">
                <xsl:text> </xsl:text>
            </xsl:if>
            <xsl:value-of select="dif:First_Name" />
        </xsl:if>         
        <xsl:if test="dif:Middle_Name">
            <xsl:if test="dif:First_Name">
               <xsl:text>,</xsl:text>
            </xsl:if>
            <xsl:value-of select="dif:Middle_Name" />
        </xsl:if>
   </xsl:template>   
   
   <!-- dif-Personnel_2_dc-contributor -->
   <xsl:template match="dif:Personnel" mode="dif-Personnel_2_dc-contributor" >         
      <dc:contributor>     
         <xsl:apply-templates select="dif:Role" />
         <xsl:if test="dif:Role">
            <xsl:text> : </xsl:text>
         </xsl:if>
         <xsl:value-of select="dif:Last_Name" />
         <xsl:if test="dif:First_Name">
             <xsl:if test="dif:Last_Name">
                 <xsl:text> </xsl:text>
             </xsl:if>
             <xsl:value-of select="dif:First_Name" />
         </xsl:if>         
         <xsl:if test="dif:Middle_Name">
             <xsl:if test="dif:First_Name">
                <xsl:text>,</xsl:text>
             </xsl:if>
             <xsl:value-of select="dif:Middle_Name" />
         </xsl:if>          
      </dc:contributor>
   </xsl:template>
    
   <!-- Dublin Core Metadata Contributor from dif:Role -->
   <xsl:template match="dif:Role">                
      <xsl:if  test="position() > 1">
         <xsl:text>, </xsl:text>
      </xsl:if>
      <xsl:value-of select="." />       
   </xsl:template>
   
   <!-- dif-Data_Set_Citation_dif-Dataset_Release_Date_2_dc-date -->       
   <xsl:template match="dif:Data_Set_Citation/dif:Dataset_Release_Date"
                  mode="dif-Data_Set_Citation_dif-Dataset_Release_Date_2_dc-date" >
      <dc:date>
         <xsl:value-of select="." />
      </dc:date>
   </xsl:template>
 
   <!-- dif-Data_Set_Citation_dif-Data_Presentation_Form_2_dc-type -->       
   <xsl:template match="dif:Data_Set_Citation/dif:Data_Presentation_Form"
                  mode="dif-Data_Set_Citation_dif-Data_Presentation_Form_2_dc-type" >
      <dc:type>
         <xsl:value-of select="." />
      </dc:type>
   </xsl:template>
   
   <!-- dif-Distribution_2_dc-format -->
   <xsl:template match="dif:Distribution" mode="dif-Distribution_2_dc-format" >
      <dc:format>
         <xsl:value-of select="dif:Distribution_Media" />
         <xsl:if test="dif:Distribution_Size">
            <xsl:if test="dif:Distribution_Media">
               <xsl:text>, </xsl:text>
            </xsl:if>
            <xsl:value-of select="dif:Distribution_Size" />
         </xsl:if>
         <xsl:if test="dif:Distribution_Format">
            <xsl:if test="dif:Distribution_Media or dif:Distribution_Size">
                <xsl:text>, </xsl:text>
            </xsl:if>            
            <xsl:value-of select="dif:Distribution_Format" />
         </xsl:if>
         <xsl:if test="dif:Fees">
            <xsl:if test="dif:Distribution_Media or dif:Distribution_Size or dif:Distribution_Format">
                <xsl:text>, </xsl:text>
            </xsl:if>
            <xsl:value-of select="dif:Fees" />
         </xsl:if>
      </dc:format>
   </xsl:template>
   
   <!-- dif-Data_Center_dif-Data_Set_ID_2_dc-identifier -->       
   <xsl:template match="dif:Data_Center/dif:Data_Set_ID" mode="dif-Data_Center_dif-Data_Set_ID_2_dc-identifier" >
      <dc:identifier>
         <xsl:value-of select="." />
      </dc:identifier>
   </xsl:template>
   
   <!-- dif-Data_Set_Citation_dif-Online_Resource_2_dc-identifier -->       
   <xsl:template match="dif:Data_Set_Citation/dif:Online_Resource"
                  mode="dif-Data_Set_Citation_dif-Online_Resource_2_dc-identifier" >
      <dc:identifier>
         <xsl:value-of select="." />
      </dc:identifier>
   </xsl:template>
   
   <!-- dif-Related_URL_2_dc-identifier -->
   <xsl:template match="dif:Related_URL" mode="dif-Related_URL_2_dc-identifier">
      <dc:identifier>
         <xsl:value-of select="dif:URL_Content_Type" />
         <xsl:if test="dif:URL">
            <xsl:if test="dif:URL_Content_Type">
                <xsl:text> </xsl:text>
            </xsl:if>            
            <xsl:value-of select="dif:URL" />
         </xsl:if>
      </dc:identifier>
   </xsl:template>
   
   <!-- dif-Related_URL_2_dc-source -->
   <xsl:template match="dif:Related_URL" mode="dif-Related_URL_2_dc-source">
      <dc:source>
         <xsl:value-of select="dif:URL_Content_Type" />
         <xsl:if test="dif:URL">
            <xsl:if test="dif:URL_Content_Type">
                <xsl:text> </xsl:text>
            </xsl:if>            
            <xsl:value-of select="dif:URL" />
         </xsl:if>        
      </dc:source>
   </xsl:template> 
   
   <!-- dif-Source_Name_2_dc-source -->
   <xsl:template match="dif:Source_Name" mode="dif-Source_Name_2_dc-source">
       <dc:source>
         <xsl:value-of select="dif:Short_Name" />
         <xsl:if test="dif:Long_Name">
            <xsl:text> (</xsl:text>
            <xsl:value-of select="dif:Long_Name" />
            <xsl:text>)</xsl:text>
         </xsl:if>
      </dc:source>
   </xsl:template>
   
   <!-- dif-Data_Set_Language_2_dc-language -->       
   <xsl:template match="dif:Data_Set_Language" mode="dif-Data_Set_Language_2_dc-language" >
      <dc:language>
         <xsl:value-of select="." />
      </dc:language>
   </xsl:template>
   
   <!-- dif-Parent_DIF_2_dc-relation -->       
   <xsl:template match="dif:Parent_DIF" mode="dif-Parent_DIF_2_dc-relation" >
      <dc:relation>
         <xsl:value-of select="." />
      </dc:relation>
   </xsl:template>
   
    <!-- dif-dif-Data_Set_Citation_dif-Online_Resource_2_dc-relation -->       
   <xsl:template match="dif:Data_Set_Citation/dif:Online_Resource" 
                  mode="dif-dif-Data_Set_Citation_dif-Online_Resource_2_dc-relation" >
      <dc:relation>
         <xsl:value-of select="." />
      </dc:relation>
   </xsl:template>
   
   <!-- dif-Related_URL_2_dc-relation -->
   <xsl:template match="dif:Related_URL" mode="dif-Related_URL_2_dc-relation">
      <dc:relation>
         <xsl:value-of select="dif:URL_Content_Type" />
         <xsl:if test="dif:URL">
            <xsl:if test="dif:URL_Content_Type">
                <xsl:text> </xsl:text>
            </xsl:if>            
            <xsl:value-of select="dif:URL" />
         </xsl:if>        
      </dc:relation>
   </xsl:template> 
   
   <!-- dif-Reference_2_dc-relation -->       
   <xsl:template match="dif:Reference" mode="dif-Reference_2_dc-relation" >
      <dc:relation>
         <xsl:value-of select="." />
      </dc:relation>
   </xsl:template>
   
   <!-- dif-Location_2_dc-coverage -->
   <xsl:template match="dif:Location" mode="dif-Location_2_dc-coverage">
      <dc:coverage>
         <xsl:value-of select="dif:Location_Name" />
         <xsl:if test="dif:Detailed_Location">
            <xsl:text> (</xsl:text>
            <xsl:value-of select="dif:Detailed_Location" />
            <xsl:text>)</xsl:text>
         </xsl:if>
      </dc:coverage>
   </xsl:template>
   
   <!-- dif:Spatial_Coverage_2_dc-coverage -->
   <xsl:template match="dif:Spatial_Coverage" mode="dif-Spatial_Coverage_2_dc-coverage" >
      <dc:coverage>
         <xsl:text>WEST: </xsl:text>
         <xsl:value-of select="dif:Westernmost_Longitude" />
         <xsl:text> EAST: </xsl:text>
         <xsl:value-of select="dif:Easternmost_Longitude" />
         <xsl:text> SOUTH: </xsl:text>
         <xsl:value-of select="dif:Southernmost_Latitude" />
         <xsl:text> NORTH: </xsl:text>
         <xsl:value-of select="dif:Northernmost_Latitude" />         
      </dc:coverage>
   </xsl:template>
   
   <!-- dif-Temporal_Coverage_2_dc-coverage -->
   <xsl:template match="dif:Temporal_Coverage" mode="dif-Temporal_Coverage_2_dc-coverage" >
      <dc:coverage>
         <xsl:if test="dif:Start_Date">
             <xsl:text>FROM </xsl:text>
             <xsl:value-of select="dif:Start_Date" />
         </xsl:if>
         <xsl:if test="dif:End_Date">
             <xsl:if test="dif:Start_Date">
                <xsl:text> </xsl:text>
             </xsl:if>
             <xsl:text>TO </xsl:text>
             <xsl:value-of select="dif:End_Date" />
         </xsl:if>                          
      </dc:coverage>
   </xsl:template>
   
    <!-- dif-Paleo_Temporal_Coverage_2_dc-coverage -->
   <xsl:template match="dif:Paleo_Temporal_Coverage" mode="dif-Paleo_Temporal_Coverage_2_dc-coverage" >
      <dc:coverage>
         <xsl:if test="dif:Start_Date">
             <xsl:text>FROM </xsl:text>
             <xsl:value-of select="dif:Start_Date" />
         </xsl:if>
         <xsl:if test="dif:End_Date">
             <xsl:if test="dif:Start_Date">
                <xsl:text> </xsl:text>
             </xsl:if>
             <xsl:text>TO </xsl:text>
             <xsl:value-of select="dif:End_Date" />
         </xsl:if>
         <xsl:if test="dif:Chronostratigraphic_Unit">
            <xsl:if test="dif:Start_Date or dif:End_Date">
               <xsl:text> </xsl:text>
            </xsl:if>
            <xsl:text>(</xsl:text>
               <xsl:value-of select="dif:Chronostratigraphic_Unit" />
            <xsl:text>)</xsl:text>
         </xsl:if>
      </dc:coverage>
   </xsl:template>
   
   <!-- dif-Use_Constraints_2_dc-right -->       
   <xsl:template match="dif:Use_Constraints" mode="dif-Use_Constraints_2_dc-right" >
      <dc:rights>
         <xsl:value-of select="." />
      </dc:rights>
   </xsl:template>
   
    <!-- dif-Access_Constraints_2_dc-right -->       
   <xsl:template match="dif:Access_Constraints" mode="dif-Access_Constraints_2_dc-right" >
      <dc:rights>
         <xsl:value-of select="." />
      </dc:rights>
   </xsl:template>
   
</xsl:stylesheet>
