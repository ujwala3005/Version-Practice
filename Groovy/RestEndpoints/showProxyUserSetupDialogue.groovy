import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.transform.BaseScript
import com.atlassian.jira.component.ComponentAccessor

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.sal.api.pluginsettings.PluginSettings

import com.atlassian.jira.project.ProjectManager

import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin

import groovy.json.JsonSlurper

@WithPlugin('com.atlassian.sal.jira')
@PluginModule
PluginSettingsFactory pluginSettingsFactory;

@BaseScript CustomEndpointDelegate delegate

showProxyUserSetupDialog { MultivaluedMap queryParams ->
    
    def customFieldManager = ComponentAccessor.getCustomFieldManager()
    
    // To save the input data
    PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey("proxy-user")
    String proxyUserSetup = pluginSettings.get("proxy-user-setup")


    // Create default edit HTML
    def result = null

    // Getting the saved data and converting to objects from json
    def slurper = new JsonSlurper()
    if (proxyUserSetup) {
        result = slurper.parseText(proxyUserSetup)
    }
    
    // Issue type selector (will be placed under opt groups of their own projects)
    def projectManager = ComponentAccessor.getProjectManager()
    
    def issueTypeOptions = '<option value="-1">None</option><option value="0">All</option>'
    
    projectManager.getProjectObjects().each {prj ->
        
        if (!prj.name.equals("FGHHSS PROD Deployment")) {
            def projIssueTypes = prj.getIssueTypes()

            if (projIssueTypes) {
                issueTypeOptions += '<optgroup label="' + prj.getName() + '" data-weight="1">'

                def optionsITypes = ''
                projIssueTypes.each {prjITypes ->
                    optionsITypes += '<option value="' + prjITypes.id + '">' + prjITypes.name + '</option>'
                }

                issueTypeOptions += optionsITypes + '</optgroup>'
            }
        }
    }
    
    // Getting all the user and multi user picker custom fields and create options out of them
    def cfOptions = ''
    def customFieldList = customFieldManager.getCustomFieldObjects().stream().filter {it ->
        it.customFieldType.key.contains("userpicker") ||
            it.customFieldType.key.contains("multiuserpicker")
    }.toList().toSorted{a, b -> a.name <=> b.name}
    
    customFieldList.each { cfItems ->
        cfOptions += '<option value="' + cfItems.idAsLong + '">' + cfItems.name + ' (' + cfItems.idAsLong + ')</option>'
    }

    // Creating edit html based on saved data
    def dataRows = ''

    def issueTypeData = ''
    def editIssueTypeData = ''
    
    def fieldsData = ''
    def editFieldsData = ''
    
    // Testing user picker
    def editSelectRows = ''

    def userManager = ComponentAccessor.getUserManager()
    
    if (result != null) {
        
        if (result.applicableIssueTypes) {
            issueTypeData = """
            	<div class="field-group" style="overflow: auto;max-height: 200px;">
                
                    <label style="color: #30006e;" for="applicableIssueTypes-readonly">
                    	Applicable Issue Types
                    	<span class="aui-icon icon-inline-help field-help"><span>Help</span></span>
                        
                        <div class="description field-help-desc hidden">
                            Please Note:
                            <ul>
                                <li>If left empty or "None" selected, all the other selected options will be ignored</li>
                                <li>If "All" selected, this setup will be applicable for entire Jira</li>
                                <li>If not empty, or "None"/"All" not selected, the set up will be applicable to the selected issue types</li>
                            </ul>
                        </div>
                    </label>
                    
                    <select id="applicableIssueTypes-readonly" multiple disabled>
                    	${issueTypeOptions}
                    </select>
                    
                </div>
            """
            editIssueTypeData = result.applicableIssueTypes
        }
        
        (result.userData).each {puSetup ->
            
            def unUser = userManager.getUserByName(puSetup.unavailable_user);
            def proxUser = userManager.getUserByName(puSetup.proxy_user);
            
            editSelectRows += """
            	<tr><td>
                		<select id="unavailable-user" class="single-user-picker unavailable-user" data-show-dropdown-button="true">
                			<option value="${puSetup.unavailable_user}" selected="selected">${unUser?.displayName}</option>
                        <select>
                    </td>
                    <td>
                		<select id="proxy-user" class="single-user-picker proxy-user" data-show-dropdown-button="true">
                			<option value="${puSetup.proxy_user}" selected="selected">${proxUser?.displayName}</option>
                        <select>
                    </td>
                <td><input class="aui-button removeProxyUserRow" type="button" value="Remove"></input></td></tr>
            """

            dataRows += '<tr><td>' + puSetup.unavailable_user + '</td><td>' + puSetup.proxy_user + '</td></tr>'
        }

        if (result.approverFields) {
            fieldsData = """
            	<div class="field-group" style="overflow: auto;max-height: 200px;">
                
                    <label style="color: #30006e;" for="approverFields-readonly">Approver Fields</label>
                    <select id="approverFields-readonly" multiple disabled>
                    	${cfOptions}
                    </select>
                </div>
            """
            editFieldsData = result.approverFields
        }
    }

    if (editSelectRows == '') {
        editSelectRows = """<tr><td>
                            <select id="unavailable-user" class="single-user-picker unavailable-user" data-show-dropdown-button="true"><select>
                        </td>
                        <td>
                            <select id="proxy-user" class="single-user-picker proxy-user" data-show-dropdown-button="true"><select>
                        </td>
                    <td><input class="aui-button removeProxyUserRow" type="button" value="Remove"></input></td></tr>"""
    }
    
    def displayTable = '<h3 style="color: red;">No User Setup</h3>'
    
    if (issueTypeData != '') {
        displayTable = issueTypeData
    }
    
    if (fieldsData != '') {
        if (issueTypeData != '') {
            displayTable += fieldsData
        } else {
        	displayTable = fieldsData
        }
    }

    if (dataRows != '') {
        if (fieldsData != '' || issueTypeData != '') {
            displayTable += """
            					<div class="field-group" style="overflow: auto;max-height: 200px;">
                                	<label style="color: #30006e;" for="displaySetupTable">Unavailable User Mapping</label>
            						<table class="aui" id="displaySetupTable">
                                    	<tr><th style="font-size: 12px;">Unavailable User</th>
                                        	<th style="font-size: 12px;">Proxy User</th>
                                        </tr>
                                        ${dataRows}
                                    </table>
                               	</div>
                            """
        } else {
            displayTable = """
            					<div class="field-group" style="overflow: auto;max-height: 200px;">
                                	<label style="color: #30006e;" for="displaySetupTable">Unavailable User Mapping</label>
            						<table class="aui" id="displaySetupTable">
                                    	<tr><th style="font-size: 12px;">Unavailable User</th>
                                        	<th style="font-size: 12px;">Proxy User</th>
                                        </tr>
                                        ${dataRows}
                                    </table>
                               	</div>
                            """
        }
    }


    def editButton = '<input type="button" class="aui-button" value="Edit" id="editProxySetupData"></input>'
    def cancelButton = '<input type="button" class="aui-button cancel" value="Cancel"></input>'

    // get a reference to the current page...
    // def page = getPage(queryParams)

    def dialog =
        """
        <h2 style="color: #30006e;" title="Proxy User Setup">Proxy User Setup</h2>
        
       	<div id="editProxyUserSetupData" style="display: none; overflow: auto; max-height: 300px; padding: 0px 9px;">
        	<form id="editSetupForm" class="aui top-label" action="#" method="post">
            	<div class="field-group" style="overflow: auto;max-height: 200px;">
                    <label style="color: #30006e;" for="applicableIssueTypes">
                    	Applicable Issue Types
                    	<span class="aui-icon icon-inline-help field-help"><span>Help</span></span>
                        
                        <div class="description field-help-desc hidden">
                            Please Note:
                            <ul>
                                <li>If left empty or "None" selected, all the other selected options will be ignored</li>
                                <li>If "All" selected, this setup will be applicable for entire Jira</li>
                                <li>If not empty, or "None"/"All" not selected, the set up will be applicable to the selected issue types</li>
                            </ul>
                        </div>
                    </label>
                    <select id="applicableIssueTypes" multiple>
                    	${issueTypeOptions}
                    </select>
				</div>
                
				<div class="field-group" style="overflow: auto;max-height: 200px;">
                    <label style="color: #30006e;" for="approverFields">Approver Fields</label>
                    <select id="approverFields" multiple>
                    	${cfOptions}
                    </select>
				</div>
                
                <div class="field-group" style="overflow: auto;max-height: 200px;">
                	<label style="color: #30006e;" for="addSetupTable">Unavailable User Mapping</label>
                    <table class="aui" id="addSetupTable">
                        <tr>
                        	<th style="font-size: 12px;">Unavailable User</th>
                            <th style="font-size: 12px;">Proxy User</th><th></th>
                        </tr>
                        ${editSelectRows}
                    </table>
				</div>
                
                <div class="buttons-container">
                    <div class="buttons">
                        <input type="button" class="aui-button" value="Add User" id="addProxyUserRow"></input>
            			<input type="submit" class="aui-button" id="setupSubmit" value="Save"></input>
                		<input type="button" class="aui-button" value="Cancel Edit" id="cancelProxySetupData"></input>
                    </div>
                </div>
            </form>
        </div>
        <div id="displayProxyUserSetupData" style="overflow: auto;max-height: 300px;padding: 0px 9px;">
        	<form id="viewSetupForm" class="aui top-label" action="#" method="get">
         		${displayTable}
            </form>
        </div>
        <div class="buttons-container">
         	${editButton} ${cancelButton}
        </div>
        <script type="text/javascript">
        	//AJS.\$(document).ready(function(){
            
            AJS.\$("span.field-help").click(function() {
            	AJS.\$("div.field-help-desc").toggleClass("hidden");
            });
            
            // Hiding the bottom borders of the table
            var lastRowCellsInDisplay = AJS.\$('#displaySetupTable > tbody > tr:last-child > td');
            if (lastRowCellsInDisplay) {
            	lastRowCellsInDisplay.css("border-bottom", "none");
            }
            
            var lastRowCellsInEdit = AJS.\$('#addSetupTable > tbody > tr:last-child > td');
            if (lastRowCellsInEdit) {
            	lastRowCellsInEdit.css("border-bottom", "none");
            }
            
            AJS.\$("#applicableIssueTypes").val(${editIssueTypeData});
        	AJS.\$("#applicableIssueTypes").auiSelect2();
            
            AJS.\$("#applicableIssueTypes-readonly").val(${editIssueTypeData});
        	AJS.\$("#applicableIssueTypes-readonly").auiSelect2();
            
        	AJS.\$("#approverFields").val(${editFieldsData});
        	AJS.\$("#approverFields").auiSelect2();
            
            AJS.\$("#approverFields-readonly").val(${editFieldsData});
        	AJS.\$("#approverFields-readonly").auiSelect2();
            
            console.log("Server data is ${result}");
            
            function addRow() {
                var table=document.getElementById("addSetupTable");
                var rowCount=table.rows.length;
                var addRow = table.insertRow(rowCount);
                var cell1 = addRow.insertCell(0);
                var unavailableUser=document.createElement("select");
                //unavailableUser.type="text";
                unavailableUser.className="single-user-picker unavailable-user";
                unavailableUser.name="unavailable-user";
                unavailableUser.id="unavailable-user";
                unavailableUser.required=true;
                cell1.appendChild(unavailableUser);
                var cell2 = addRow.insertCell(1);
                var proxyUser=document.createElement("select");
                //proxyUser.type="text";
                proxyUser.className="single-user-picker proxy-user";
                proxyUser.name="proxy-user";
                proxyUser.id="proxy-user";
                proxyUser.required=true;
                proxyUser.setAttribute("onchange", "stopUnavailableAsProxy(this)");
                cell2.appendChild(proxyUser);
                var cell3 = addRow.insertCell(2);
                var elementButton=document.createElement("button");
                elementButton.innerHTML="Remove";
                elementButton.className="aui-button";
                elementButton.setAttribute("onclick", "removeRow(this)");
                cell3.appendChild(elementButton);
                
                initOneUserPicker(unavailableUser);
               	initOneUserPicker(proxyUser);
            }
            document.getElementById("addProxyUserRow").setAttribute("onclick", "addRow()");
            function removeRow(r) {
            	let i = r.parentNode.parentNode.rowIndex;document.getElementById("addSetupTable").deleteRow(i);
                // Only enable the button if no errors present on the form
                if (document.getElementsByClassName("validationMessage").length <= 0) {
                	\$("#setupSubmit").prop('disabled', false);
                }
            }
            
            // Removing proxy user row
            var itemsToremove = document.getElementsByClassName("removeProxyUserRow");
            for (let item of itemsToremove) {
                item.setAttribute("onclick", "removeRow(this)");
            }
            function cancelEdit() {
                document.getElementById("editProxyUserSetupData").style.display = 'none';
                document.getElementById("displayProxyUserSetupData").style.display = 'block';
                document.getElementById("editProxySetupData").style.display = 'inline-block';
            }
            document.getElementById("cancelProxySetupData").setAttribute("onclick", "cancelEdit()");
            
            function editRow() {
                document.getElementById("editProxyUserSetupData").style.display = 'block';
                document.getElementById("displayProxyUserSetupData").style.display = 'none';
                document.getElementById("editProxySetupData").style.display = 'none';
            }
			document.getElementById("editProxySetupData").setAttribute("onclick", "editRow()");
			
            //.........................Stop user from adding un-available users as proxy..........................
            function isUnavailableAsProxy(value) {
            	var unavailbleToStop = document.getElementsByClassName("unavailable-user");
                for (let item of unavailbleToStop) {
                	if (item.value == value) {
                    	return false;
                    }
                }
                
                return true;
            }
            
            function stopUnavailableAsProxy(element) {
                \$(element).next("span.validationMessage").remove();
                
                // Only enable the button if no errors present on the form
                if (document.getElementsByClassName("validationMessage").length <= 0) {
                	\$("#setupSubmit").prop('disabled', false);
                }
                
            	if (isUnavailableAsProxy(element.value)) {
                	console.log("Valid!!!");
                } else {
                	\$(element).after("<span class='validationMessage' style='color:red;'>This user is not allowed.</span>");
                    \$("#setupSubmit").prop('disabled', true);
                	console.log("Fail!!!");
                }
            }
            
            var proxyToStop = document.getElementsByClassName("proxy-user");
            for (let item of proxyToStop) {
                item.setAttribute("onchange", "stopUnavailableAsProxy(this)");
            }
            //....................................Finished user validation.........................................
            
            function setupDone(ele) {
            	JIRA.Messages.showSuccessMsg("Proxy user setup has been successfully saved.");
                ele.unbind("submit").submit();
                window?.location?.reload();
            }
            
            function initOneUserPicker(element) {
                var \$el = AJS.\$(element);
                var proxyUserPicker = new AJS.SingleSelect({
                    element: element,
                    itemAttrDisplayed: "label",
                    showDropdownButton: false,
                    ajaxOptions:  {
                        data: function (query) {
                            return {
                                query: query,
                                exclude: \$el.val()
                            }
                        },
                        url: AJS.params.baseURL + "/rest/api/2/user/picker",
                        query: true,
                        formatResponse: JIRA.UserPickerUtil.formatResponse
                    }
                });
                
                var descriptor = \$el.find("option:selected").data("descriptor");
        
        		if (descriptor) {
                    proxyUserPicker.\$field.focus();
                    proxyUserPicker.\$dropDownIcon.click();
                    proxyUserPicker.setSelection(descriptor);
                }
            }
            
            AJS.\$("select#unavailable-user").each(function( index, element ) {
                initOneUserPicker(element);
            });
                
            AJS.\$("select#proxy-user").each(function( index, element ) {
                initOneUserPicker(element);
            });
            
            document.getElementById("editSetupForm").addEventListener("submit", function(e) {
            	e.preventDefault();
                
                // Disable the buttons when submitting the form
                //AJS.\$("#editSetupForm > input").prop('disabled', true);
                
                var table=document.getElementById("addSetupTable");
                var setupData = [];
            	for(var i=1; i<table.rows.length;i++){
                    var unavailable_user = table.rows[i].cells[0].children[1].value;
                    var proxy_user = table.rows[i].cells[1].children[1].value;
                    setupData[i - 1] = {"unavailable_user" : unavailable_user, "proxy_user" : proxy_user};
                }
                
                var applicableIssueTypes = AJS.\$("#applicableIssueTypes").val();
                console.log(AJS.\$("#applicableIssueTypes"))
                var approverFields = AJS.\$("#approverFields").val();
                
                var proxySetupData = {"userData" : setupData, "approverFields" : approverFields, "applicableIssueTypes" : applicableIssueTypes};
                
                console.log(proxySetupData);
                fetch("/rest/scriptrunner/latest/custom/proxyUserSetupData", {
                        method: "POST",body: JSON.stringify(proxySetupData), headers: {
                            "Content-Type": "application/json"
                        }
                     }
                ).then(response => response.json()).then(json => console.log(json)).then(setupDone(\$(this)))
            });
			//});
        </script>
        
        """

    Response.ok().type(MediaType.TEXT_HTML).entity(dialog.toString()).build()
}
