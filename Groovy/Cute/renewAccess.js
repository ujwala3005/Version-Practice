if (window.location.href.indexOf("desk/portal/7/IT") != -1) {
  	
  jQuery(function($) {
    $(document).ready(function() {
      setTimeout(function() {
        const statusContainer = document.getElementsByClassName("rw_request_status_container")[0];
        if (statusContainer && statusContainer.textContent.includes("Request Resolved")) { 
          const parent = document.getElementsByClassName("rw_request_actions")[0];
          //parent.style.backgroundColor = "yellow";
          
          
          const actualRenewButton = document.getElementsByClassName("js-trigger-customer-transition")[0];
          if (actualRenewButton) {
            actualRenewButton.style.display = 'none';
          }
          
		  const img = document.createElement('img');
		  img.src = '@contextPath/plugins/cute/static/resource/PO/Renew_Access_img.png';
          img.style.marginTop = '10px';
		  img.style.opacity = '0.8';
		  img.style.maxWidth = '24px';
          img.style.marginLeft = '12px';
          
          const Renewbtn = document.createElement('button');
          Renewbtn.textContent = 'Renew Access';
          Renewbtn.style.marginLeft = '12px';
          Renewbtn.style.lineHeight = '44px';
          Renewbtn.style.height = '44px';
          Renewbtn.style.color = '#111';
          Renewbtn.style.whiteSpace = 'nowrap';
          Renewbtn.style.overflow = 'hidden';
          Renewbtn.style.textOverflow = 'ellipsis';
          Renewbtn.style.border = 'none';
          Renewbtn.className = "TestRenew";
          
          var li = document.createElement("li");
          li.className = 'listItem'
          li.style.display = 'flex';          
		  li.style.alignItems = 'center';    
		  li.style.listStyleType = 'none';    
		  li.style.padding = '0';             
          li.appendChild(img);
          li.appendChild(Renewbtn);
          parent.appendChild(li);
          
          
          $(Renewbtn).click(function() {
            
            $(this).prop('disabled', true);
            
			const label = document.createElement('label');
            label.textContent = 'Please enter New Expiration date below* :';
            
            const label1 = document.createElement('label');
       		label1.style.marginTop = '14px'
            label1.textContent = 'Please enter the comments* :';
            
            const input = document.createElement('input');
            input.type = 'date';
            input.id = 'date-field';
            input.required = true;
            
            
            const comment = document.createElement('textarea');
            comment.id = 'comment-field';
            comment.required = true;
            //comment.style.resize = 'none';
            
            const Renew = document.createElement('button');
            Renew.textContent = 'Renew';
            Renew.style.marginTop = '14px'
            Renew.style.width = "100px";
            Renew.style.height = "30px";
            Renew.className = "TestRenew2";
            Renew.style.borderRadius = '5px';
           
            
            const Cancel = document.createElement('button');
            Cancel.textContent = 'Cancel';
            Cancel.style.marginTop = '14px'
            Cancel.style.width = "100px";
            Cancel.style.height = "30px";
            Cancel.style.borderRadius = '5px';
            Cancel.style.marginLeft = '100px';
            
            const buttonContainer = document.createElement('div');
            buttonContainer.style.display = 'flex';  
            buttonContainer.style.marginTop = '14px';  
            
            
            buttonContainer.appendChild(Renew);
            buttonContainer.appendChild(Cancel);
           
            
            Cancel.addEventListener('click', () => { 
              box.style.display = 'none';
              $(Renewbtn).prop('disabled', false);
              
            });
            
            
            
            const box = document.createElement('div');
            box.style.border = '2px solid #000';  
            box.style.padding = '20px';           
            box.style.marginTop = '20px';         
            box.style.borderRadius = '10px';      
            box.style.width = '320px';            
            box.style.backgroundColor = '#f9f9f9';  
            
            var li1= document.createElement("li");
            li1.style.display = 'flex';  
            li1.style.flexDirection = 'column';  
            li1.style.alignItems = 'flex-start'; 
            li1.style.listStyleType = 'none';
            li1.style.padding = '0';
            li1.style.fontFamily = 'Arial, sans-serif';
            
            li1.appendChild(label);
            li1.appendChild(input);
            li1.appendChild(label1);
            li1.appendChild(comment);
            li1.appendChild(buttonContainer);
            
            box.appendChild(li1);
            parent.appendChild(box);
            
            
            $(box).click(function(event) {
              event.stopPropagation();  
            });
            
            
            let inputDate = document.getElementById('date-field').value;
            let userComments = document.getElementById('comment-field').value;
            
            
            input.addEventListener('input', function() {
              inputDate = input.value;
            });
            
            comment.addEventListener('input', function() {
              userComments = comment.value;
            });
            
            /*$(document).click(function(event) {
              if (!$(event.target).closest(box).length && !$(event.target).closest(Renewbtn).length ) {
                box.style.display = 'none';
                $(Renewbtn).prop('disabled', false);
                errorMessage.textContent = '';
                
              }
            });*/
            
            Renew.addEventListener('click', () => { 
              
              $(this).prop('disabled', true); 
              
              const errorMessage = document.createElement('label');
              errorMessage.textContent = '*Please fill the mandatory fields';
              errorMessage.style.color = "red";
              errorMessage.marginTop = '10px';
              errorMessage.className = "errorMessageClass";
              
              
              const existingErrorMessages = box.getElementsByClassName('errorMessageClass');
              
              if (existingErrorMessages.length > 0) {
                existingErrorMessages[0].remove();
              }
              
              
              
              if(!inputDate || !userComments){                
                
                errorMessage.textContent = '*Please fill the mandatory fields';
                box.appendChild(errorMessage);
                $(Renew).prop('disabled', false);
                
              }else if(new Date(inputDate) < new Date().setHours(0, 0, 0, 0)){
                
                errorMessage.textContent = '*Renewal Expiration Date should be Future Date!!!';
                box.appendChild(errorMessage);
                $(Renew).prop('disabled', false); 
                
              }
              else{
                
                const token = "MjM1ODQ2N";
                const ticketID = window.location.pathname.split('/').pop();
                const API_url = "https://jiraet-uat.cotiviti.com/rest/api/2/issue/" + ticketID +"/transitions";
                const updatedData = {
                  "transition": {"id": "991"},
                  "fields": {"customfield_13219": inputDate},
                  "update": {"comment": [{"add": {"body": userComments}}]},
                };
                
                
                console.log("++++++++++++++++++++++++++++++++++++++++++++++");
                console.log(API_url);
                console.log("++++++++++++++++++++++++++++++++++++++++++++++");
                console.log(ticketID);
                console.log("++++++++++++++++++++++++++++++++++++++++++++++");
                console.log(inputDate);
                console.log("++++++++++++++++++++++++++++++++++++++++++++++");
                console.log(userComments);
                
                
                
                
                fetch(API_url, {
                method: "POST",
                headers: {
                  Authorization: `Bearer ${token}`,
                  "Content-Type": "application/json"
                },
                body: JSON.stringify(updatedData)
              }).then((response) => response.json())
                .then((data) => console.log(data))
                .catch((error) => console.error(error));
                
                window.location = window.location
              }               
            });
            
          });
        }
      }, 1000);
    });
  });
}


//let comments = ''
//if(userComments){
//  comments = userComments;
//}
//else{
//  comments = 'Please review renewal access request'
//}



//box.appendChild(li1);

//if (!inputDate) {
//  errorMessage.textContent = '*Please fill the New Expiration Date';
//} else if (!userComments) {
//  errorMessage.textContent = '*Please Enter the mandatory comments';
//}

//$(Renew).click(function() {
//  

//});

//li1.style.fontWeight = 'bold';

//IT-1752824
//const Cancel = document.createElement('button');
//Cancel.textContent = 'Cancel';
//Cancel.style.marginTop = '14px'
//Cancel.style.width = "100px";
//Cancel.style.height = "30px";
//Cancel.style.borderRadius = '5px';
//Cancel.style.marginLeft = '90px';
//Cancel.addEventListener('click', () => { 
//box.style.display = 'none';
                          
//});

//const existingErrorMessages = box.getElementsByClassName('errorMessageClass');

// If any error messages exist, remove them
//if (existingErrorMessages.length > 0) {
//  existingErrorMessages[0].remove();
//}
