package com.workflows.Template.Create.Postfunction

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.crowd.embedded.api.Group

def customFieldManager= ComponentAccessor.getCustomFieldManager()
//Insight Assignment Group Field
def iAssignmentGroupCF = customFieldManager.getCustomFieldObjectsByName("Assignment Group")[0]
//issue value
def iAssignmentGroupValues = issue.getCustomFieldValue(iAssignmentGroupCF)
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade"));
def groupObject = iAssignmentGroupValues[0]

def groupNameAttribute = objectFacade.loadObjectAttributeBean(groupObject.id, "Group Name")
def groupNameAttributeValues = groupNameAttribute.getObjectAttributeValueBeans()
String groupName = groupNameAttributeValues[0].value as String

def assignmentGroupCF = customFieldManager.getCustomFieldObjectsByName("Assigned Group")[0]

def groupManager = ComponentAccessor.getGroupManager()
Group group = groupManager.getGroup(groupName)
issue.setCustomFieldValue(assignmentGroupCF, [group]);

def managerCF = customFieldManager.getCustomFieldObjectsByName("Group Manager")[0]

def managerAttribute = objectFacade.loadObjectAttributeBean(groupObject.id, "Manager")
def managerAttributeValues = managerAttribute.getObjectAttributeValueBeans()
def manager = managerAttributeValues[0].value

def managerNameAttribute = objectFacade.loadObjectAttributeBean(manager, "User Name")
def managerNameAttributeValues = managerNameAttribute.getObjectAttributeValueBeans()
String managerName = managerNameAttributeValues[0].value

if(managerName!=null) {
    def userManager = ComponentAccessor.userManager
    def managerUser = userManager.getUserByName(managerName)
    issue.setCustomFieldValue(managerCF, managerUser);    
}

def user=ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def cf=customFieldManager.getCustomFieldObjectByName("Request Raised By")
issue.setCustomFieldValue(cf,user)
