
   AJS.$( document ).ready(function() {  

   setTimeout(function()
              { 
     if(AJS.$("#summary-val")[0]){
				if(AJS.$("#summary-val")[0].outerText=="Jira Align") {
	
  				$("a:contains('Approve')").hide()
                $("a:contains('Decline')").hide()
                console.log("JIRA ALIGN")
  
			  }
       }
    			 }, 10);
     });
    
  
  
