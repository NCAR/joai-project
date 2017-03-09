	function toggleVisibility( elementID ) {
		var objElement = document.getElementById( elementID );
		if ( objElement != null ) {
			if ( objElement.style.display == '' )
				objElement.style.display = 'none';
			else
				objElement.style.display = '';
		}
	}

	function checkDef(locn){
		newwindow = window.open("../popup.jsp?default_location="+locn,"help", "scrollbars,resizable,width=500,height=220");
		newwindow.focus();
	}
	
	function popupHelp(path, w){
		
		if (w == 'split')
		{
			newwindow=window.open(path+"/popup.jsp?help="+w, "help", "scrollbars,resizable,width=500,height=500");
			if(newwindow == null)
				alert("Please enable popups for this site. The help window could not be displayed.");
			else
				newwindow.focus();
		}	
		else
		{
			newwindow=window.open(path+"/popup.jsp?help="+w, "help", "scrollbars,resizable,width=500,height=500");
			if(newwindow == null)
				alert("Please enable popups for this site. The help window could not be displayed.");
			else			
				newwindow.focus();
		}
	}
	
	
	function more(url, locn){
		newwindow=window.open("../popup.jsp?more=true&url="+url+"&locn="+locn, "more", "scrollbars,resizable,width=500,height=300");
		newwindow.focus();
	}
	
	function checkReg(){
		if (document.getElementById('reg').checked== true)
			document.getElementById('regularYes').style.display = 'block';
		else {
			document.getElementById('regularYes').style.display = 'none';
			document.getElementById('interval').value = '';
			document.getElementById('runAtTimeTB').value = '';
		}
		
		var granList = document.getElementById('gran');
		var selectedGran = null;
		if(granList != null)
			selectedGran = granList.options[granList.selectedIndex];
		if(selectedGran != null && selectedGran.value == 'days')
			document.getElementById('runAtTime').style.display = 'block';
		else {
			document.getElementById('runAtTime').style.display = 'none';
			document.getElementById('runAtTimeTB').value = '';
		}
	}
	
	
	function checkText(){
	var setText = document.getElementById( 'setName' ).value;
	if( setText != '' )
		document.getElementById('setsplits').style.display='none';
	
	if (setText == '')
		document.getElementById('setsplits').style.display='block';
	}
		
	function checkSets(dir0, dir1, dir2, dir3, dir4){
	
	if (dir0 != '')
	addOption(document.haf.s, dir0, dir0);
	
	if (dir1 != '')
	addOption(document.haf.s, dir1, dir1);
	
	if (dir2 != '' )
	addOption(document.haf.s, dir2, dir2);
	
	if (dir3 != '')
	addOption(document.haf.s, dir3, dir3);
	
	if (dir4 != '')
	addOption(document.haf.s, dir4, dir4);
	
	var setText = document.getElementById('setName').value;
	var customD = document.getElementById('cD').value;
	if( setText != '' )
		document.getElementById('setsplits').style.display='none';
	if (setText == '')
		document.getElementById('setsplits').style.display='block';
	//alert("customD is " + customD + "!"); 
	if (customD != "")
		document.getElementById('hiddenDir').style.display = 'block';
	if (customD == "")
		document.getElementById('hiddenDir').style.display='none';
		
	change(); 
	checkReg();
	}
	
	function change(){
	if (document.getElementById('radioCustom').checked) {
				document.getElementById('hiddenDir').style.display='block';
	}
	else {
					document.getElementById('hiddenDir').style.display='none';
		}
	}

	
	
	function remove(){
	document.getElementById('hiddenDir').style.display='none';
	}
	
	
	function addOption(selectbox,text,value )
	{
	var optn = document.createElement("OPTION");
	optn.text = text;
	optn.value = value;
	selectbox.options.add(optn);
	}
	
	
	// A simple window with no browser buttons.
	function swin(theURL,w,h) {
          window.open(theURL,"newWin", ("width="+w+",height="+h+",status,resizable,scrollbars")).focus()
    }
	function swinNS(theURL) {
          window.open(theURL,"newWin", ("status,resizable,scrollbars")).focus()
    }

	
	function openFullWindow(theURL,w,h) {
          window.open(theURL,"newWin", ("width="+w+",height="+h+",status,resizable,menubar,scrollbars,toolbar,location,directories,titlebar")).focus()
    }
	
	
	function doDelete( id, name ) {
		if ( confirm( "Are you sure you want to delete the settings for \"" + name + "?\"" ) )
			document.location.href = "index.jsp?delete=" + id;
	}
	
	function confirmHarvest( URL ) {	
		if ( confirm( "Harvesting may take a long time to complete. Are you sure you want to continue?" ) )
			document.location.href = URL;
	}

	function confirmHref( URL, msg ) {	
		if ( confirm( msg ) )
			document.location.href = URL;
	}	

	

	
	// -------- OAI-PMH and ODL helper functions ---------
	
	function mkResume(rt) {	
		var fm = document.resumptionForm;
		var verb = fm.verb.options[fm.verb.selectedIndex].value;
		var token = fm.resumptionToken.value;
		if(rt)
			rt = "&" + rt;
		else
			rt = '';	
		window.location = 	BASE_URL + 
							"?verb=" + verb + 
							"&resumptionToken=" + token + rt;
	}

	function mkGetRecord(rt) {	
		var fm = document.getRecordForm;
		var format = fm.formats.options[fm.formats.selectedIndex].value;
		var identifier = fm.identifier.value;
		var verb = "GetRecord";
		if(rt)
			rt = "&" + rt;
		else
			rt = '';
		window.location = 	BASE_URL + 
							"?verb=" + verb + 
							"&metadataPrefix=" + format +
							"&identifier=" + identifier + rt; 
	}

	function mkListRecordsIdentifers(rt) {	
		var fm = document.listRecordsIdentifers;
		var format = fm.formats.options[fm.formats.selectedIndex].value;
		var set = fm.sets.options[fm.sets.selectedIndex].value;
		if(set == " -- All -- ")
			set = "";
		else
			set = "&set=" + set;
		var from = fm.from.options[fm.from.selectedIndex].value;
		if(from == "none")
			from = "";
		else
			from = "&from=" + from;
			
		var until = fm.until.options[fm.until.selectedIndex].value;
		if(until == "none")
			until = "";
		else
			until = "&until=" + until;
		var verb = fm.verb.options[fm.verb.selectedIndex].value;
		if(rt)
			rt = "&" + rt;
		else
			rt = '';		
		window.location = 	BASE_URL + 
							"?verb=" + verb + 
							"&metadataPrefix=" + format +
							set +from + until + rt; 
	}
	
	function mkOdlSearch(rt) {	
		// ODL search format is: dleseodlsearch/[query string]/[set]/[offset]/[length]
		// Use set = null to indicate no set spec.
		var fm = document.odlSearchForm;
		var from = "";
		var until = "";
		var format = fm.formats.options[fm.formats.selectedIndex].value;
		set = fm.sets.options[fm.sets.selectedIndex].value;
		if(set == " -- All -- ")
			set = "null";
				
		// If the advanced search is visible, get setting from the form:
		if(advanced == "on"){
			from = fm.from.options[fm.from.selectedIndex].value;
			if(from == "none")
				from = "";
			else
				from = "&from=" + from;
				
			until = fm.until.options[fm.until.selectedIndex].value;
			if(until == "none")
				until = "";
			else
				until = "&until=" + until;
		}
		var verb = fm.verb.options[fm.verb.selectedIndex].value;
		var query = fm.query.value;
		var temp = new Array();
		temp = query.split(' ');
		if(temp.length > 0){
			query = temp[0];
			for(var i = 1; i < temp.length; i++){
				query += "+" + temp[i];
			}
		}
		if(rt)
			rt = "&" + rt;
		else
			rt = '';
		
		query = query.replace(/\//g,'%2F'); // Escape slashes with %2F in query string
		set = set.replace(/\//g,'%2F'); // Escape slashes with %2F in set
		
		var setParam = "dleseodlsearch/" + query + "/" + set + "/0/10";
		window.location = 	BASE_URL + 
							"?verb=" + verb + 
							"&metadataPrefix=" + format +
							from + until +
							"&set=" + encodeURIComponent(setParam) + rt; 
	}		



