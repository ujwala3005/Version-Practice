package com.workflows.Template.RequestForApproval.Validator

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser

def peerReviewCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Peer Reviewer")[0]
def peerReviewer = issue.getCustomFieldValue(peerReviewCF) as ApplicationUser
def reporter = issue.reporter
boolean result = true
log.error("Peer Reviewer : ${peerReviewer.username}")

log.error("Reporter : ${reporter.username}")
if(peerReviewer.username.equals(reporter.username))
	result =  false

def technicalManager = cfValues["Technical Manager"]
log.error("Technical Manager : ${technicalManager.username}")
if(peerReviewer.username.equals(technicalManager.username))
	result =  false

def supportManager = cfValues["Support Manager"]
log.error("Support Manager : ${supportManager.username}")
if(peerReviewer.username.equals(supportManager.username))
	result =  false

def businessOwner = cfValues["Business Owner"]
log.error("Business Owner : ${businessOwner.username}")
if(peerReviewer.username.equals(businessOwner.username))
	result = false


return result
