package com.behaviours.OffboardingBehaviour

def typeOfSeparation = getFieldByName("Type of Separation")
def associateName =  getFieldById("customfield_12707") //Associate Name
def terminationDate = getFieldByName("Termination Date")
def timeZone = getFieldByName("Time Zone")
def associateWorkingInNight = getFieldByName("Associate Working in Night Shift?")
def equipmentList = getFieldByName("Equipment List")
def equipmentReturnType = getFieldByName("Equipment Return Type")
def dataOfAssociatebeingoffboarded = getFieldByName("Does Manager require access to Data of Associate being offboarded?")
def mailBoxofAssociatebeingoffboarded = getFieldByName("Does Manager require access to MailBox of Associate being offboarded?")
//def voiceMailofAssociatebeingoffboarded = getFieldByName("Does Manager require access to Voice Mail of Associate being offboarded?")
def nameoftheManager = getFieldByName("Name of the Manager")
def nameoftheDelegate = getFieldByName("Name of the Delegate")
def whoprovidedHRBPApproval = getFieldByName("Who provided HR BP Approval?")
def managerorDelegate = getFieldByName("Manager or Delegate?")
def termDate = getFieldByName("Termination Date")
def typeOfSeparationselectedOption = typeOfSeparation.getValue() as String
def today = new Date()


whoprovidedHRBPApproval.setHidden(true).setRequired(false).setFormValue("")
if(typeOfSeparationselectedOption.equalsIgnoreCase("Person did not join"))
{
     equipmentList.setHidden(true).setRequired(false).setFormValue("")
     equipmentReturnType.setHidden(true).setRequired(false).setFormValue("")
    dataOfAssociatebeingoffboarded.setHidden(true).setRequired(false).setFormValue("")
    mailBoxofAssociatebeingoffboarded.setHidden(true).setRequired(false).setFormValue("")
    //voiceMailofAssociatebeingoffboarded.setHidden(true).setRequired(false).setFormValue("")
    nameoftheManager.setHidden(true).setRequired(false).setFormValue("")
     nameoftheDelegate.setHidden(true).setRequired(false).setFormValue("")
    termDate.clearError()
    managerorDelegate.setHidden(true).setRequired(false).setFormValue("")
    terminationDate.setFormValue(today.format("dd/MMM/yy")).setReadOnly(true)
}
else if(typeOfSeparationselectedOption.equalsIgnoreCase("Immediate"))
{
    terminationDate.setFormValue(today.format("dd/MMM/yy")).setReadOnly(true)
    equipmentList.setHidden(false).setRequired(true)
     equipmentReturnType.setHidden(false).setRequired(true)
    dataOfAssociatebeingoffboarded.setHidden(false).setRequired(true)
    mailBoxofAssociatebeingoffboarded.setHidden(false).setRequired(true)
    //voiceMailofAssociatebeingoffboarded.setHidden(false).setRequired(true)
    nameoftheManager.setHidden(true).setRequired(false).setFormValue("")
    nameoftheDelegate.setHidden(true).setRequired(false).setFormValue("")
     managerorDelegate.setHidden(false).setRequired(true).setFormValue("")
    termDate.clearError()
}
else
{
    equipmentList.setHidden(false).setRequired(true)
     equipmentReturnType.setHidden(false).setRequired(true)
    dataOfAssociatebeingoffboarded.setHidden(false).setRequired(true)
    mailBoxofAssociatebeingoffboarded.setHidden(false).setRequired(true)
    //voiceMailofAssociatebeingoffboarded.setHidden(false).setRequired(true)
     nameoftheManager.setHidden(true).setRequired(false).setFormValue("")
    nameoftheDelegate.setHidden(true).setRequired(false).setFormValue("")
    managerorDelegate.setHidden(false).setRequired(true).setFormValue("")
    terminationDate.setFormValue("").setReadOnly(false) 
}
