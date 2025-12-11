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
def totalHours = customFieldManager.getCustomFieldObject(14621)

iprTimeEstimation( 
    httpMethod: "POST"
) { MultivaluedMap queryParams, String body ->
    log.info(body)
    def slurper = new JsonSlurper()
	def result = slurper.parseText(body)
    
    log.info(body)
    
    def issueKey = queryParams.getFirst("issueKey") as String
    def settingKey = "time-estimation-" + issueKey
    
    def issue = issueManager.getIssueObject(issueKey)
    
    PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey("IT-IPR")
	pluginSettings.put(settingKey, body)
    
    Double totalHoursCalculation = 0
    (result).each {est -> if (est != null && est.estimate != null) {totalHoursCalculation = totalHoursCalculation + Double.parseDouble(est.estimate.toString())}}
    
    //def testSetting = pluginSettingsFactory.createSettingsForKey("IT").get(settingKey);
    
    issue.setCustomFieldValue(totalHours, totalHoursCalculation)
    issueManager.updateIssue(user, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
    
    return Response.ok(body).build()
}
