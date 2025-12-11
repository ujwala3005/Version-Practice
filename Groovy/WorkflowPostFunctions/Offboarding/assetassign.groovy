package com.workflows.offboardingSubtask.Create.postfunctions
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.customfields.option.Option
import com.atlassian.jira.issue.customfields.option.Options
import com.atlassian.jira.issue.customfields.manager.OptionsManager
 
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
 
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
@WithPlugin('com.riadalabs.jira.plugins.insight')
@PluginModule ObjectFacade objectFacade
@PluginModule ObjectTypeFacade objectTypeFacade
@PluginModule ObjectTypeAttributeFacade objectTypeAttributeFacade
@PluginModule IQLFacade iqlFacade
 

//def cfuserdevice = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Assets Serial Number")[0]
def resultObj = ((ComponentAccessor.getCustomFieldManager().getCustomFieldObject("customfield_11615").getValue(issue)) as ApplicationUser)
def assetAssignCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Asset Assignment")[0]
 
 
// log.warn(cfuserdevice)
// if (assetCF == null) {
//      return
def result = resultObj.getEmailAddress()
//log.warn("User : " + result)
log.warn(result)
//Class objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade")
//def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass)
 
//Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade")
//def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass)
 
String attributeUsernamekey = "Assigned To"
def schemaID = 7
log.warn("ObjectType = Laptops/Desktops and \"" + attributeUsernamekey + "\" = \"" + result + "\"")
def objects = iqlFacade.findObjectsByIQLAndSchema(schemaID, "ObjectType = Laptops/Desktops and \"" + attributeUsernamekey + "\" = \"" + result + "\"" )
log.warn("Object value "+objects)
   // assetAssignCF.updateValue(null, issue, new ModifiedValue(assetAssignCF), new DefaultIssueChangeHolder())
 
//assetAssignCF.updateValue(null, issue, new ModifiedValue(assetAssignCF), new DefaultIssueChangeHolder())
issue.setCustomFieldValue(assetAssignCF, objects)
ComponentAccessor.getIssueManager().updateIssue(null, issue, EventDispatchOption.ISSUE_UPDATED, false)
