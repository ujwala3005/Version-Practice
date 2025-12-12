package com.listeners.OnboardingListener

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.crowd.embedded.api.Group
import org.ofbiz.core.entity.GenericValue

//Insight Assignment Group Field
//def iAssignmentGroupCF = customFieldManager.getCustomFieldObject(12613)
def iAssignmentGroupCF = customFieldManager.getCustomFieldObjectsByName("Assignment Group")[0]

//issue value
def iAssignmentGroupValues = issue.getCustomFieldValue(iAssignmentGroupCF)

def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade"));

def groupObject = iAssignmentGroupValues[0]

def groupNameAttribute = objectFacade.loadObjectAttributeBean(groupObject.id, "Group Name")
def groupNameAttributeValues = groupNameAttribute.getObjectAttributeValueBeans()

String groupName = groupNameAttributeValues[0].value as String
def assignmentGroupCF = customFieldManager.getCustomFieldObjectsByName("Internal Assignment Group")[0]

def groupManager = ComponentAccessor.getGroupManager()
Group group = groupManager.getGroup(groupName)
issue.setCustomFieldValue(assignmentGroupCF, [group]);

// Set Group Manager
def managerCF = customFieldManager.getCustomFieldObjectsByName("Group Manager")[0] // "Group Manager"

def managerAttribute = objectFacade.loadObjectAttributeBean(groupObject.id, "Manager")

if(managerAttribute){
    
    def managerAttributeValues = managerAttribute.getObjectAttributeValueBeans()
    def manager = managerAttributeValues[0].value

    def managerNameAttribute = objectFacade.loadObjectAttributeBean(manager, "User Name")
    def managerNameAttributeValues = managerNameAttribute.getObjectAttributeValueBeans()
    String managerName = managerNameAttributeValues[0].value

    if(managerName!=null) {
        def userManager = ComponentAccessor.userManager
        ApplicationUser managerUser = userManager.getUserByName(managerName)

        issue.setCustomFieldValue(managerCF, managerUser);        
    }
}
// Set Assignee null
issue.setAssignee(null);
