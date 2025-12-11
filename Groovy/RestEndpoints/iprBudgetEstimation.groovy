import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder

import groovy.transform.BaseScript
import groovy.json.JsonSlurper

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import com.atlassian.jira.bc.issue.properties.IssuePropertyService
import com.atlassian.jira.component.ComponentAccessor

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.sal.api.pluginsettings.PluginSettings

import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin

import com.atlassian.jira.event.type.EventDispatchOption

import com.atlassian.jira.issue.IssueManager

@WithPlugin('com.atlassian.sal.jira')
@PluginModule
PluginSettingsFactory pluginSettingsFactory;

@BaseScript CustomEndpointDelegate delegate

def customFieldManager = ComponentAccessor.getCustomFieldManager()

def issueManager = ComponentAccessor.getIssueManager() as IssueManager

def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

def totalCapexHW = customFieldManager.getCustomFieldObject(14622)
def totalCapexSW = customFieldManager.getCustomFieldObject(14623)
def totalOpexHW = customFieldManager.getCustomFieldObject(14624)
def totalOpexSW = customFieldManager.getCustomFieldObject(14625)

iprBudgetEstimation( 
    httpMethod: "POST"
) { MultivaluedMap queryParams, String body ->
    log.info(body)
    def slurper = new JsonSlurper()
	def result = slurper.parseText(body)
    
    log.info(body)
    
    def issueKey = queryParams.getFirst("issueKey") as String
    def settingKey = "budget-estimation-" + issueKey
    
    def issue = issueManager.getIssueObject(issueKey)
    
    PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey("IT-IPR")
	pluginSettings.put(settingKey, body)
    
    Double totalCapexHWCalc = 0
    Double totalCapexSWCalc = 0
    Double totalOpexHWCalc = 0
    Double totalOpexSWCalc = 0
    
    (result).each {est -> if (est != null && est.estimate != null) {
        if (est.capexopex == "CapEx Hardware") {
            totalCapexHWCalc = totalCapexHWCalc + Double.parseDouble(est.estimate.toString())
        } else if (est.capexopex == "CapEx Software") {
            totalCapexSWCalc = totalCapexSWCalc + Double.parseDouble(est.estimate.toString())
        } else if (est.capexopex == "OpEx Hardware") {
            totalOpexHWCalc = totalOpexHWCalc + Double.parseDouble(est.estimate.toString())
        } else if (est.capexopex == "OpEx Software") {
            totalOpexSWCalc = totalOpexSWCalc + Double.parseDouble(est.estimate.toString())
        }
        
    }}
    
    issue.setCustomFieldValue(totalCapexHW, totalCapexHWCalc)
    issue.setCustomFieldValue(totalCapexSW, totalCapexSWCalc)
    issue.setCustomFieldValue(totalOpexHW, totalOpexHWCalc)
    issue.setCustomFieldValue(totalOpexSW, totalOpexSWCalc)
    
    issueManager.updateIssue(user, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
    
    return Response.ok(body).build()
}
