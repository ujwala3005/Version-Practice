package com.jobs.OffboardingJob;

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.user.ApplicationUser
import java.text.SimpleDateFormat
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.user.util.UserUtil
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.customfields.manager.OptionsManager
import com.atlassian.jira.issue.IssueFactory
import java.sql.Timestamp
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;

Class objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().loadClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade");
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass);

Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().loadClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade")
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass)

// Get Insight Object Attribute Facade from plugin accessor
Class objectTypeAttributeFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().loadClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade");
def objectTypeAttributeFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectTypeAttributeFacadeClass);


log.warn "***************** Job Starts ***********************"
log.warn("Script Execution Time : " + new Date().toString())
log.warn "---------------------------------------------------------"


// Checking today profile deletion request
def jqlSearch = "project = IT and issuetype = \"Offboarding\" AND  \"Type of Separation\" != \"Person did not join\"  AND   \"Profile Deletion Date\" = startOfDay() AND Status In (\"Waiting for Deletion\",\"Awaiting Internal Teams\")"
log.warn("JQL : "+ jqlSearch)

def searchService = ComponentAccessor.getComponentOfType(SearchService)
UserUtil userUtil = ComponentAccessor.getUserUtil()
ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
IssueManager issueManager = ComponentAccessor.getIssueManager()

List < Issue > issues = null

SearchService.ParseResult parseResult = searchService.parseQuery(user, jqlSearch)

if (!parseResult.isValid()) {
    log.error("Invalid search");
    return
}

def issueCount = searchService.searchCount(user, parseResult.getQuery())
if (issueCount > 0) {
    log.warn("IssueCount: " + issueCount)
    def results = searchService.search(user, parseResult.query, PagerFilter.unlimitedFilter)
    issues = results.results
    log.warn("Total Issues Found : " +issues)
    log.warn("----------------------------------------------------------------------------------------")

    def i = 0;
    for (issue in issues) {
        log.warn("Currently Processing : " + issue )
        def cFieldLegalHold = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("customfield_12781") //New Legal Hold

        def associatenameObj = issue.getCustomFieldValue(ComponentAccessor.customFieldManager.getCustomFieldObject("customfield_12707")); // Associate Name (Insight)
        log.warn("Fetching Legal Hold from CMDB for - " + associatenameObj)

        def legalHoldBean = objectFacade.loadObjectAttributeBean(associatenameObj[0].getId(), "Legal Hold")
        if (legalHoldBean) {
            def legalHold = legalHoldBean.getObjectAttributeValueBeans()[0].getValue()
            log.warn("Legal Hold Value :" + legalHold)
            if (legalHold != null) {
                cFieldLegalHold.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(cFieldLegalHold), legalHold), new DefaultIssueChangeHolder())

            }
        }else{
            log.error("LEGAL HOLD data is EMPTY in CMDB")
        }

        log.warn("Value Post Updated - "+ issue.getCustomFieldValue(cFieldLegalHold))
        log.warn("-----------------------------------")
    }
}
log.warn "***************** COMPLETE ***********************"
