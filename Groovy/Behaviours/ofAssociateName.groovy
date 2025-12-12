package com.behaviours.OffboardingBehaviour

import com.atlassian.jira.user.ApplicationUser
import com.onresolve.jira.groovy.user.FormField
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.user.util.UserUtil
import com.atlassian.jira.web.bean.PagerFilter

FormField associateNameInsightFF = getFieldById(getFieldChanged())
String associateNameInsight = associateNameInsightFF.formValue
associateNameInsightFF.clearError()

if(actionName == "Create") {
    def dataOfAssociatebeingoffboarded = getFieldByName("Does Manager require access to Data of Associate being offboarded?")
    def nameoftheManager = getFieldByName("Name of the Manager")
    def nameoftheDelegate = getFieldByName("Name of the Delegate")
    def whoprovidedHRBPApproval = getFieldByName("Who provided HR BP Approval?")
    def managerorDelegate = getFieldByName("Manager or Delegate?")

    dataOfAssociatebeingoffboarded.setFormValue(-1)
    managerorDelegate.setHidden(true).setRequired(false).setFormValue(-1)
   	nameoftheManager.setHidden(true).setRequired(false).setFormValue("")
   	nameoftheDelegate.setHidden(true).setRequired(false).setFormValue("")
   	whoprovidedHRBPApproval.setHidden(true).setRequired(false).setFormValue("")
}

if(associateNameInsight == null || associateNameInsight == "" ) {
    return
}

String jqlSearch = 'project = IT and issuetype = "Offboarding" and "cf[12707]" != null and status not in (Cancelled,Closed) and "cf[12707]" = ' + associateNameInsight

//getFieldByName("Justification").setFormValue(jqlSearch)

SearchService searchService = ComponentAccessor.getComponent(SearchService.class)
UserUtil userUtil = ComponentAccessor.getUserUtil()
ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
IssueManager issueManager = ComponentAccessor.getIssueManager()

List<Issue> issues = null

SearchService.ParseResult parseResult =  searchService.parseQuery(user, jqlSearch)
if (parseResult.isValid()) {
    def issueCount = searchService.searchCount(user, parseResult.getQuery())
   if(issueCount > 0 ) {
        associateNameInsightFF.setError("Offboarding Request for this user has already been created.")
    }
    else {
        associateNameInsightFF.clearError()
    }
} 
else {
    associateNameInsightFF.setError("Error validating Offboardin request creation.")
}
