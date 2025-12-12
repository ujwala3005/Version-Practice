package com.behaviours.OffboardingBehaviour

import com.onresolve.jira.groovy.user.FormField
import com.atlassian.jira.issue.IssueFieldConstants
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.customfields.manager.OptionsManager
import com.onresolve.jira.groovy.user.FieldBehaviours
import com.atlassian.jira.bc.project.component.ProjectComponent
import com.atlassian.jira.user.UserPropertyManager
import com.atlassian.crowd.embedded.api.User
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.UserPropertyManager
import com.onresolve.jira.groovy.user.FormField
import com.atlassian.jira.user.DelegatingApplicationUser
import com.atlassian.jira.user.util.UserUtil
import com.opensymphony.module.propertyset.PropertySet;
import com.atlassian.jira.user.ApplicationUser;
import static com.atlassian.jira.issue.IssueFieldConstants.*

def delegate = getFieldByName("Name of the Delegate")
def result = delegate.getValue() as String
def resultObj =  (ComponentAccessor.getUserManager().getUserByName(result) as ApplicationUser).getEmailAddress()
Class objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade")
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass)

Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade") 
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass)
def m2manager = getFieldByName("Requestor M2 Manager")
def m1manager = getFieldByName("Requestor M1 Manager")
m1manager.setHidden(true).setRequired(false).setFormValue(result)
m2manager.setHidden(false).setRequired(false)
String attributeUsernamekey = "Email Address"
String attributeUserKey = "Key"
def schemaID = 25

def objects = iqlFacade.findObjectsByIQLAndSchema(schemaID, "ObjectType = Users and \"" + attributeUsernamekey + "\" = \"" + resultObj + "\"")

def objectmanagername = objectFacade.loadObjectAttributeBean(objects[0].getId(), "Manager1")

if( objectmanagername )
{
    def objectAttributeValues = objectmanagername.getObjectAttributeValueBeans()
    def objectAttributeValue = objectAttributeValues[0]
    String managerKey="CMDB-"+objectAttributeValue.getValue()

    def objectsUser = iqlFacade.findObjectsByIQLAndSchema(schemaID, "\"" + attributeUserKey + "\" = \"" + managerKey + "\"")

    def objectsUserFinalDetails = objectFacade.loadObjectAttributeBean(objectsUser[0].getId(), "User Name")

    def userN = objectsUserFinalDetails.getObjectAttributeValueBeans()[0].getValue().toString()
    def userObj = ComponentAccessor.getUserManager().getUserByName(userN) as ApplicationUser
    def email = objectFacade.loadObjectAttributeBean(objectsUser[0].getId(), "Email Address").getObjectAttributeValueBeans()[0].getValue()
    if(!userObj && email) 
    {
        def users = ComponentAccessor.userSearchService.findUsersByEmail(email)
        userObj = (users==null || users.isEmpty())?null:users[0]                
    }
    if(userObj)
    {
        m2manager.setFormValue(userObj.getUsername()) 
        m2manager.setHidden(true)
    }
    else	
        m2manager.setFormValue("")	
}
