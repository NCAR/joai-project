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
package org.dlese.dpc.surveys;

import org.dlese.dpc.util.*;
import java.util.*;
import java.io.*;

/**
 *  Parse the HTML form found (hopefully) at the given URL, and turn it into a
 *  Javascript-rendered version that can easily be included in the DLESE site.
 *  This assumes a Perseus processor as the form submission handler.
 *
 * @author    Ryan Deardorff
 */
public class CreateJavascriptSurveyFromHTMLForm {

	/**
	 *  Constructor for the CreateJavascriptSurveyFromHTMLForm object
	 *
	 * @param  URL
	 * @param  saveFile
	 */
	public CreateJavascriptSurveyFromHTMLForm( String URL, String saveFile ) {
		String html = GetURL.getURL( URL, true );
		html = html.replaceAll( "[\\s\\S]*(<[Ff][Oo][Rr][Mm][\\s\\S]*</[Ff][Oo][Rr][Mm]>)[\\s\\S]*", "\n$1" );
		html = html.replaceAll( "'", "\\\\'" );
		html = html.replaceAll( "\n([^\n]+)", "\n\t\thtm += '$1';" );
		String jsOut = "function renderSurvey() {\n\tvar htm = '';" + html + "\n\t"
			 + "var obj = document.getElementById( 'dleseSurvey' ); if ( obj != null ) obj.innerHTML = htm; \n}\n";
		jsOut = jsOut.replaceFirst( "(<form[^>]+)>", "$1 onSubmit=\"dlese_surveySubmitted()\">" );
		jsOut = jsOut.replaceFirst( "value=\\\"Location:http:[^\\\"]+\\\"", "value=\"Location:**DLESE_REDIRECT**\"" );
		try {
			Files.writeFile( jsOut, saveFile );
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	/**
	 *  Description of the Method
	 *
	 * @param  args
	 */
	public static void main( String[] args ) {
		if ( args.length < 2 ) {
			System.out.println( "Format: CreateJavascriptSurveyFromHTMLForm [inputUrl] [outputFilename]" );
		}
		else {
			new CreateJavascriptSurveyFromHTMLForm( args[0], args[1] );
		}
	}
}

