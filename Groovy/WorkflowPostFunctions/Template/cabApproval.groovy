package com.workflows.Template.PrsesentToCAB.Postfunction

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.groups.GroupManager
import com.atlassian.crowd.embedded.api.Group

GroupManager grpManager = ComponentAccessor.groupManager
def users = grpManager.getUsersInGroup("CAB") 

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def usersCf = customFieldManager.getCustomFieldObjectsByName("CAB")[0]
issue.setCustomFieldValue(usersCf, users)

//def pendingWithCf = customFieldManager.getCustomFieldObjectsByName("Change Pending With")[0]
//issue.setCustomFieldValue(pendingWithCf, users)
