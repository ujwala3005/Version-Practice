//atl.jira.view.issue.right.context
//condition
if (issue.getIssueType().getName() == "Infrastructure Project Request") {
    return true
} else {
    return false
}
//script action
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.sal.api.pluginsettings.PluginSettings

import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin

import com.atlassian.jira.issue.Issue

import groovy.json.JsonSlurper

import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade

@WithPlugin('com.atlassian.sal.jira')
@PluginModule
PluginSettingsFactory pluginSettingsFactory;

@WithPlugin('com.riadalabs.jira.plugins.insight')
@PluginModule IQLFacade iqlFacade

def issue = context.issue as Issue

// issue status to check if the issue is in estimation, to enable/disable editing
def issueStatusCurrent = issue.getStatus().getName().toLowerCase()

// Making this always true to make sure this is available at all times for migration data
//def isStatusEstimation = issueStatusCurrent == "estimation"
def isStatusEstimation = true

// To save the input data
PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey("IT-IPR")
String issueEstimation = pluginSettings.get("time-estimation-" + issue.getKey())

// Sub BU for IT Infrastructure BU, to give as a selection in time estimation
def itInfraSubBUs = iqlFacade.findObjectsByIQLAndSchema(25,'"BU Name" IN ("IT Infrastructure") AND objectType IN ("Sub Business Unit")')
def subBUOptionsHtml = ''
itInfraSubBUs.each{it -> subBUOptionsHtml += '<option value="' + it.getName() + '">' + it.getName() + '</option>'}

// Create default edit HTML
def result = ""
def editRows = '<tr><td><select class="select" id="estimate-team" required>' + subBUOptionsHtml + '</select></td>' +
    	'<td><input id="time-estimate-input" class="text short-field" type="text" oninput="validateNumber(this);" required></input></td>' +
        '<td><input type="button" class="aui-button" value="Remove" onclick="removeRow(this)"></input></td></tr>';

// Getting the saved data and converting to objects from json
def slurper = new JsonSlurper()
if (issueEstimation) {
	result = slurper.parseText(issueEstimation)
    editRows = ''
}

// Creating edit html based on saved data
def dataRows = '';

(result).each {estimate ->
    
    def subBUOptionsHtmlSaved = '';
    (itInfraSubBUs).each {it -> if (it.getName() == estimate.team) {
        subBUOptionsHtmlSaved += '<option value="' + it.getName() + '" selected>' + it.getName() + '</option>'
    } else {
        subBUOptionsHtmlSaved += '<option value="' + it.getName() + '">' + it.getName() + '</option>'
    }}
    
    editRows = editRows + '<tr><td><select class="select" id="estimate-team" required>' + subBUOptionsHtmlSaved + '</select></td>' +
        '<td><input id="time-estimate-input" class="text short-field" type="text" value="' + estimate.estimate + '" required oninput="validateNumber(this);"></input></td>' +
        '<td><input class="aui-button" type="button" value="Remove" onclick="removeRow(this)"></input></td></tr>'
    
    dataRows = dataRows + '<tr><td>' + estimate.team + '</td><td>' + estimate.estimate + '</td></tr>'
}

def displayTable = '<h3 style="color: red;">No time estimation provided</h3>'
if (dataRows != '') {
    displayTable = '<table class="aui" id="displayEstTable"><tr><th>Team</th><th>Estimate (Hours)</th></tr>' + dataRows + '</table>'
}

def editButton = '';
if (isStatusEstimation ) {
    editButton += '<input type="button" class="aui-button" value="Edit" onclick="editRow()"></input>'
}
    
writer.write(           
    '<div id="editDataTable" hidden=true><form id="editEstForm" class="aui"><table class="aui" id="addEstTable"><tr><th>Team</th><th>Estimate (Hours)</th><th></th></tr>' + editRows + '</table>' +
    '<input type="button" class="aui-button" value="Add Estimate" onclick="addRow()">' +
    '</input><input type="submit" class="aui-button" id="estSubmit" value="Submit"></input>' +
    '<input type="button" class="aui-button" value="Cancel" onclick="cancelEdit()"></input></form></div>' +
    
    '<div id="displayDataTable">' + displayTable + editButton + '</div>' +
    
    '<script type="text/javascript">' +
    	'function addRow() {var table=document.getElementById("addEstTable");var rowCount=table.rows.length;var addRow = table.insertRow(rowCount);' +
    
        'var cell1 = addRow.insertCell(0);var elementTeam=document.createElement("select");elementTeam.required=true;elementTeam.className="select";' +
        'elementTeam.id="estimate-team";elementTeam.innerHTML=\'' + subBUOptionsHtml + '\';cell1.appendChild(elementTeam);' +
    
		'var cell2 = addRow.insertCell(1);var elementEst=document.createElement("input");' +
        'elementEst.type="text";elementEst.className="text short-field";elementEst.name="estimation";' +
    	'elementEst.setAttribute("oninput", "validateNumber(this)");elementEst.id="time-estimate-input";' +
    	'elementEst.required=true;cell2.appendChild(elementEst);' +
    
        'var cell3 = addRow.insertCell(2);var elementButton=document.createElement("button");' +
        'elementButton.innerHTML="Remove";elementButton.className="aui-button";' +
    	'elementButton.setAttribute("onclick", "removeRow(this)");cell3.appendChild(elementButton);}' +
    
    'function removeRow(r) {let i = r.parentNode.parentNode.rowIndex;document.getElementById("addEstTable").deleteRow(i);}' +
    
    'function cancelEdit() {JIRA.trigger(JIRA.Events.REFRESH_ISSUE_PAGE, [JIRA.Issue.getIssueId()]);}' +
    
    'function editRow() {document.getElementById("editDataTable").hidden=false;' +
    'document.getElementById("displayDataTable").hidden=true;}' +
    
    'var lastValid = document.getElementById("time-estimate-input")? document.getElementById("time-estimate-input").value : "";' +
    'function validateNumber(elem) {let val = elem.value;if (isNaN(val))' +
    '{elem.value = lastValid;} else {lastValid = elem.value;}}' +
    
    'function onlyNumberKey(evt) {var ASCIICode = (evt.which) ? evt.which : evt.keyCode;' +
    'if (ASCIICode > 31 && (ASCIICode < 48 || ASCIICode > 57)) {return false;}return true;}' +
    
    'document.getElementById("editEstForm").addEventListener("submit", function(e) { e.preventDefault(); var table=document.getElementById("addEstTable"); var estData = []; ' + 
    'for(var i=1; i<table.rows.length;i++){var team = table.rows[i].cells[0].firstChild.value; var est = table.rows[i].cells[1].firstChild.value;' + 
    'estData[i - 1] = {"team" : team, "estimate" : est};} console.log(estData);' +
    
    'fetch("https://jiraet.cotiviti.com/rest/scriptrunner/latest/custom/iprTimeEstimation?issueKey=' + issue.getKey() + '", {' +
    'method: "POST",body: JSON.stringify(estData), headers: {"Content-Type": "application/json"}}).then(response => response.json())' +
    '.then(json => console.log(json)).then(cancelEdit())' +
    '});' +

    
    '</script>'
)
