package com.workflows.Template.Create.Validator

def reporter = issue.reporter
boolean result = true
log.error("Reporter : ${reporter.username}")

def technicalManager = cfValues["Technical Manager"]
log.error("Technical Manager : ${technicalManager.username}")
if(reporter.username.equals(technicalManager.username))
	result =  false

def supportManager = cfValues["Support Manager"]
log.error("Support Manager : ${supportManager.username}")
if(reporter.username.equals(supportManager.username))
	result =  false

def businessOwner = cfValues["Business Owner"]
log.error("Business Owner : ${businessOwner.username}")
if(reporter.username.equals(businessOwner.username))
	result = false

return result
