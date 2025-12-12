package com.behaviours.OffboardingBehaviour

import com.atlassian.jira.component.ComponentAccessor

def optionsManager = ComponentAccessor.getOptionsManager()

def summary = getFieldById("summary")
def justification = getFieldByName("Justification")
def typeOfSeparation = getFieldByName("Type of Separation")
def associateName = getFieldById("customfield_12707") //Associate Name
def terminationDate = getFieldByName("Termination Date")
def timeZone = getFieldByName("Time Zone")
def associateWorkingInNight = getFieldByName("Associate Working in Night Shift?")
def nightShift = getFieldByName("Night Shift")
def equipmentList = getFieldByName("Equipment List")
def equipmentReturnType = getFieldByName("Equipment Return Type")
def dataOfAssociatebeingoffboarded = getFieldByName("Does Manager require access to Data of Associate being offboarded?")
def mailBoxofAssociatebeingoffboarded = getFieldByName("Does Manager require access to MailBox of Associate being offboarded?")
//def voiceMailofAssociatebeingoffboarded = getFieldByName("Does Manager require access to Voice Mail of Associate being offboarded?")
def nameoftheManager = getFieldByName("Name of the Manager")
def nameoftheDelegate = getFieldByName("Name of the Delegate")
def managerorDelegate = getFieldByName("Manager or Delegate?")
def whoprovidedHRBPApproval = getFieldByName("Who provided HR BP Approval?")
//def currentUser = ComponentAccessor.jiraAuthenticationContext?.getLoggedInUser().getName()
//def requestorField = getFieldByName("Requester")
def shippingAddress= getFieldByName("Shipping Address")
def domaininfo = getFieldByName("Domain")
//def IsTaskforJiraCreated	= getFieldByName("IsTaskforJiraCreated")
//def profiledeletiondate = getFieldByName("Profile Deletion Date")
def m1manager = getFieldByName("Requestor M1 Manager")
def m2manager = getFieldByName("Requestor M2 Manager")
//getFieldByName("Eligible for Rehire?").setHidden(true)
//getFieldByName("Does Manager require access to PC Data of Associate being offboarded?").setHidden(true)

if(action?.id == 141) {
   getFieldById("comment")?.setRequired(true)
}

//requestorField.setFormValue(currentUser)
shippingAddress.setHidden(true)
associateWorkingInNight.setHidden(true)
nightShift.setHidden(true)
nameoftheManager.setHidden(true)
nameoftheDelegate.setHidden(true)
managerorDelegate.setHidden(true)
whoprovidedHRBPApproval.setHidden(true)
//voiceMailofAssociatebeingoffboarded.setHidden(false)
domaininfo.setHidden(true)
m1manager.setHidden(true)
m2manager.setHidden(true)
summary.setHidden(true)
equipmentList.setHidden(true)
equipmentReturnType.setHidden(true)
dataOfAssociatebeingoffboarded.setHidden(true)
mailBoxofAssociatebeingoffboarded.setHidden(true)
//voiceMailofAssociatebeingoffboarded.setHidden(true)
associateName.setRequired(true) //Associate Name
summary.setFormValue("Offboarding Request")
