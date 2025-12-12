package com.listeners.ChangeManagement

import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.plugin.Plugin
import com.atlassian.plugin.PluginAccessor
import com.atlassian.jira.issue.util.IssueChangeHolder
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.crowd.embedded.api.Group
import groovy.json.*
import groovy.util.*
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

Issue issue = event.getIssue()
log.warn(issue)
//def issue = ComponentAccessor.getIssueManager().getIssueObject("IT-19395")
def reqm1 =ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Requestor M1 Manager")
  
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()
def groupManager = ComponentAccessor.getGroupManager()
//Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade")
//def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass)

//Class objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade")
//def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass)
def cfVal = issue.reporter.emailAddress;
def issueType = issue.getIssueType()
log.warn(cfVal)
if(cfVal && issueType.name in ["Emergency Change Request", "Normal Change Request", "Standard Change Request"] ){
  
    String attributeUsernamekey = "Email Address"
    String attributeUserKey = "Key"
     
    def schemaID = 25
    def objects = iqlFacade.findObjectsByIQLAndSchema(schemaID, "\"" + attributeUsernamekey + "\" = \"" + cfVal + "\"")
   
 
    //System Domain
    def objectdomain;
    ////////////////
    
    log.warn(objects)
    log.warn(objects.size())
    //System Domain
    if(objects.size() > 1){

        
    objectdomain = objectFacade.loadObjectAttributeBean(objects[1].getId(), "Manager1")
    }else{

    objectdomain = objectFacade.loadObjectAttributeBean(objects[0].getId(), "Manager1")

    }
    
    if( objectdomain != null)
    {
        	def objectAttributeValues = objectdomain.getObjectAttributeValueBeans() 
            def objectAttributeValue = objectAttributeValues[0]        	
            //log.warn("CMDB-"+objectAttributeValue.getValue()) 
            String managerKey="CMDB-"+objectAttributeValue.getValue()
			def objectsUser = iqlFacade.findObjectsByIQLAndSchema(schemaID, "\"" + attributeUserKey + "\" = \"" + managerKey + "\"")
			def objectsUserFinalDetails = objectFacade.loadObjectAttributeBean(objectsUser[0].getId(), "User Name")
       	 def userObj = ComponentAccessor.getUserManager().getUserByName(objectsUser[0].getId().toString())
        def email = objectFacade.loadObjectAttributeBean(objectsUser[0].getId(), "Email Address").getObjectAttributeValueBeans()[0].getValue()

        if(!userObj && email) {
    def users = ComponentAccessor.userSearchService.findUsersByEmail(email)
    userObj = (users==null || users.isEmpty())?null:users[0]
            if(userObj)
            {
                
                   // log.warn(userObj)

                        issue.setCustomFieldValue(reqm1,userObj) 
                

            }
        }
        

ComponentAccessor.getIssueManager().updateIssue(null, issue, EventDispatchOption.ISSUE_UPDATED, false)     
 }
}
