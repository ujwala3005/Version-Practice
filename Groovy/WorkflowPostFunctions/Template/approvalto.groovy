package com.workflows.Template.Approve.Postfunction

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.customfields.option.Option

import com.atlassian.jira.issue.customfields.manager.OptionsManager
import com.onresolve.jira.groovy.user.FormField

import com.atlassian.crowd.embedded.api.User
import com.atlassian.jira.user.ApplicationUser

String techMF = "Technical Manager"
String businessOF = "Business Owner"
String supportManagerF = "Support Manager"
Long firstLevelApproversFId = 10103l

def customFieldManager = ComponentAccessor.customFieldManager

CustomField approversCF = customFieldManager.getCustomFieldObject(firstLevelApproversFId)

CustomField techMCF = customFieldManager.getCustomFieldObjectsByName(techMF)[0]
CustomField businessOCF = customFieldManager.getCustomFieldObjectsByName(businessOF)[0]
CustomField supportManagerCF = customFieldManager.getCustomFieldObjectsByName(supportManagerF)[0]

ApplicationUser techM = issue.getCustomFieldValue(techMCF) as ApplicationUser
ApplicationUser businessO = issue.getCustomFieldValue(businessOCF) as ApplicationUser
ApplicationUser supportManager = issue.getCustomFieldValue(supportManagerCF) as ApplicationUser

List approvers  = new ArrayList<>();
approvers.add(techM)
approvers.add(businessO)
approvers.add(supportManager)


issue.setCustomFieldValue(approversCF, approvers)
