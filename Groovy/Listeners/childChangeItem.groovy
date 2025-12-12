package com.listeners.AccountID

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.crowd.embedded.api.Group
import org.ofbiz.core.entity.GenericValue

def issueType = event.issue.issueType
if(issueType.name in["Account & ID Management", "Account & ID Management Sub-Task", "Account & ID Management Okta Sub-Task"] ) {  
    def changeLog = event.changeLog
    def related = changeLog.getRelated("ChildChangeItem")
    
    def insightAssignmentGroupGV = related.find { it.field == "Assignment Group" }
    if(insightAssignmentGroupGV != null) {
        issue.setAssignee(null);
        
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
        def issueManager = ComponentAccessor.issueManager
        issueManager.updateIssue(user, issue, EventDispatchOption.ISSUE_UPDATED , true);
    }
}
