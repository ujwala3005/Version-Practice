package com.workflows.offboarding.Create.postfunctions
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.user.util.UserUtil

import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
@WithPlugin('com.riadalabs.jira.plugins.insight')
@PluginModule ObjectFacade objectFacade
@PluginModule ObjectTypeFacade objectTypeFacade
@PluginModule ObjectTypeAttributeFacade objectTypeAttributeFacade
@PluginModule IQLFacade iqlFacade

int schemaID = 25
long associateNameUserPickerCFId = 11615l
long associateNameInsightCFId = 12707l
String associateNameTextCFId = "Associate name"


log.warn "***************** Process Start for - ${issue} ******************"
log.warn("Script Execution Time : " + new Date().toString())
log.warn("Offboarding Issue : "+ issue.key)
log.warn("--------------------------------------------------------------------------")

//************
//def issue = ComponentAccessor.getIssueManager().getIssueObject("IT-557677")

CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()

def associateNameUserPickerCF = customFieldManager.getCustomFieldObject(associateNameUserPickerCFId)
def associateNameInsightCF = customFieldManager.getCustomFieldObject(associateNameInsightCFId)
def associateNameTextCF = customFieldManager.getCustomFieldObjectsByName(associateNameTextCFId)[0]

def legalhold =  customFieldManager.getCustomFieldObjectsByName("New Legal Hold")[0]
def itcontrolledspace =  customFieldManager.getCustomFieldObjectsByName("IT Controlled Space")[0]
def isustuser =  customFieldManager.getCustomFieldObjectsByName("Is UST User")[0]
def frequent =  customFieldManager.getCustomFieldObjectsByName("Frequent Card Holder")[0]
def infrequent =  customFieldManager.getCustomFieldObjectsByName("Infrequent Card Holder")[0]
def isOffboardingAutomate = customFieldManager.getCustomFieldObjectsByName("IsOffboardingAutomate")[0]
def employeetype =  customFieldManager.getCustomFieldObjectsByName("Offboarding Employee Type")[0]
def typeofseparation = customFieldManager.getCustomFieldObjectsByName("Type of Separation")[0]
def cflocation = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Location")[0]
def bankUserCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Bank User")[0]

//Class objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade")
//def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass)


// Summary and Associate Name
def associateNameInsight = issue.getCustomFieldValue(associateNameInsightCF).get(0)
if(associateNameInsight){
    issue.setCustomFieldValue(associateNameTextCF, associateNameInsight.name)
    issue.setSummary("Offboarding Request for: ${associateNameInsight.name}")
}else{
    log.error("Associate Field is NULL or EMPTY")
	log.error("Nothing to set for Summary of Issue as Associate Value is null. Please check CMDB for Associate Dropdown.")
    //throw  new Exception("Nothing to set for Summary of Issue as Associate Value is null. Please check CMDB for Associate Dropdown.")
}


//Associate ID
//def cfAssociateID  = ComponentAccessor.customFieldManager.getCustomFieldObject(12741l)
def cfAssociateID = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Associate ID")[0]
def objectAssociateIDBean = objectFacade.loadObjectAttributeBean(associateNameInsight.getId(), "Employee ID")
if(objectAssociateIDBean) {
    def objectAssociateID = objectAssociateIDBean.getObjectAttributeValueBeans()[0].getValue()
    if(objectAssociateID != null) {
        issue.setCustomFieldValue(cfAssociateID, objectAssociateID)
    }
    log.warn("Associate ID : " +  objectAssociateID)
}else{
    log.error("Associate ID not found")
}


// Associate Name - UserPicker
def userNameAttributeBean = objectFacade.loadObjectAttributeBean(associateNameInsight.id ,"User Name")
if(userNameAttributeBean) {
    String userName = userNameAttributeBean.getObjectAttributeValueBeans()[0].getValue()
    ApplicationUser associateUser = ComponentAccessor.getUserManager().getUserByName(userName)

    if(!associateUser) {
        def emailAttributeBean =  objectFacade.loadObjectAttributeBean(associateNameInsight.id ,"Email Address")
        if(emailAttributeBean) {
            String email = emailAttributeBean.getObjectAttributeValueBeans()[0].getValue()
            if(email) {
                log.warn("Email : "+ email)
                def users = ComponentAccessor.userSearchService.findUsersByEmail(email)
                associateUser = (users==null || users.isEmpty())?null:users[0]
            }
        }
    }

    if(associateUser) {
        issue.setCustomFieldValue(associateNameUserPickerCF, associateUser)
        log.warn("Associate Name (User Picker) : " + associateUser)
    }else{
        log.error("Associate Name (User Picker) for above email is not found")
    }
}


//Location
def objectlocationBean = objectFacade.loadObjectAttributeBean(associateNameInsight.getId(), "Location")
if(objectlocationBean) {
    def objectlocation = objectlocationBean.getObjectAttributeValueBeans()[0].getValue()
    if(objectlocation != null) {
        issue.setCustomFieldValue(cflocation, objectlocation)
        log.warn("Location:" +  objectlocation)
    }
}else{
    log.error("Location not found in CMDB")
}

log.warn("Type Of Separation : " + issue.getCustomFieldValue(typeofseparation).toString())

if(issue.getCustomFieldValue(typeofseparation).toString() != "Person did not join") {
    log.warn("Not a Person did not Join")

    //isOffboardingAutomate
    if(issue.getCustomFieldValue(isOffboardingAutomate) == null) {
        def fieldConfig = isOffboardingAutomate.getRelevantConfig(issue)
        def newOption = ComponentAccessor.getOptionsManager().getOptions(fieldConfig).find {it.value == "No"}
        issue.setCustomFieldValue(isOffboardingAutomate, newOption)
        log.warn("Value of isOffboardingAutomate post update is : " + issue.getCustomFieldValue(isOffboardingAutomate).toString())
    }

    //System Domain
    def objectdomainBean = objectFacade.loadObjectAttributeBean(associateNameInsight.getId(), "System Domain")
    if(objectdomainBean) {
        def objectdomain = objectdomainBean.getObjectAttributeValueBeans()[0].getValue()
        log.warn("System Domain:" + objectdomain)
        def cfDomain = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Domain")[0]
        def cfConfig = cfDomain.getRelevantConfig(issue)

        def domainValue = null
        if(objectdomain != null) {
            switch(objectdomain) {
                case 'A': domainValue = ComponentAccessor.getOptionsManager().getOptions(cfConfig).find { it.toString() == 'Atlanta'}
                    break;
                case 'V': domainValue = ComponentAccessor.getOptionsManager().getOptions(cfConfig).find { it.toString() == 'Verscend'}
                    break;
                case 'W': domainValue = ComponentAccessor.getOptionsManager().getOptions(cfConfig).find { it.toString() == 'Wilton'}
                    break;
            }
            issue.setCustomFieldValue(cfDomain, domainValue)
            log.warn("Domain Value :" + domainValue)
        }
    }else{
        log.error("'System Domain' not found in CMDB")
    }

    //Legal Hold ------
    def objectlegalholdBean = objectFacade.loadObjectAttributeBean(associateNameInsight.getId(), "Legal Hold")
    if(objectlegalholdBean) {
        def objectlegalhold = objectlegalholdBean.getObjectAttributeValueBeans()[0].getValue()
        log.warn("LegalHold:" + objectlegalhold)
        if(objectlegalhold != null) {
            issue.setCustomFieldValue(legalhold, objectlegalhold)
        }
    }else {
        log.error("'Legal Hold' Detail not Found in CMDB")
    }

    //IT Controlled Space ------
    def objectITControlledSpaceBean = objectFacade.loadObjectAttributeBean(associateNameInsight.getId(), "IT Controlled Space")
    if(objectITControlledSpaceBean) {
        def objectITControlledSpace = objectITControlledSpaceBean.getObjectAttributeValueBeans()[0].getValue()
        log.warn("IT Controlled Space:" + objectITControlledSpace)
        if(objectITControlledSpace != null ) {
            issue.setCustomFieldValue(itcontrolledspace, objectITControlledSpace)
        }
    }else {
        log.error("'IT Controlled Space' Detail not Found in CMDB")
    }

    //UST Global ------
    def objectUSTBean = objectFacade.loadObjectAttributeBean(associateNameInsight.getId(), "UST Global")
    if(objectUSTBean) {
        def objectUST = objectUSTBean.getObjectAttributeValueBeans()[0].getValue()
        log.warn("UST Global:" + objectUST)
        if(objectUST != null ) {
            issue.setCustomFieldValue(isustuser, objectUST.toString())
        }
    }else {
        log.error("'UST Global' Detail not Found in CMDB")
    }

    //Frequent ------
    def objectFrequentBean = objectFacade.loadObjectAttributeBean(associateNameInsight.getId(), "Frequent")
    if(objectFrequentBean) {
        def objectFrequent = objectFrequentBean.getObjectAttributeValueBeans()[0].getValue()
        log.warn("Frequent:" + objectFrequent)
        if(objectFrequent != null) {
            issue.setCustomFieldValue(frequent, objectFrequent)
        }
    }else {
        log.error("'Frequent' Detail not Found in CMDB")
    }

    //InFrequent ----
    def objectInFrequentBean = objectFacade.loadObjectAttributeBean(associateNameInsight.getId(), "Infrequent")
    if(objectInFrequentBean) {
        def objectInFrequent = objectInFrequentBean.getObjectAttributeValueBeans()[0].getValue()
        log.warn("InFrequent:" + objectInFrequent)
        if(objectInFrequent != null) {
            issue.setCustomFieldValue(infrequent, objectInFrequent)
        }
    }else {
        log.error("'InFrequent' Details not Found in CMDB")
    }

    //Offboarding Employee Type
    def objectEmployeeTypeBean = objectFacade.loadObjectAttributeBean(associateNameInsight.getId(), "Employee Type")
    if(objectEmployeeTypeBean) {
        def objectEmployeeType = objectEmployeeTypeBean.getObjectAttributeValueBeans()[0].getValue()
        log.warn("Employee Type:" + objectEmployeeType)
        if(objectEmployeeType != null) {
            issue.setCustomFieldValue(employeetype, objectEmployeeType)
        }
    }else {
        log.error("'Employee Type' not Found in CMDB")
    }

    //Bank User
    def bankUser = objectFacade.loadObjectAttributeBean(associateNameInsight.getId(), "Bank User")
    if(bankUser) {
        def objectbankUser = bankUser.getObjectAttributeValueBeans()[0].getValue()
        log.warn("Bank User :" + objectbankUser)
        if(objectbankUser != null ) {
            issue.setCustomFieldValue(bankUserCF, objectbankUser.toString())
        }
    }else {
        log.error("'Bank User' Detail not Found in CMDB")
    }

    log.warn("Updating Manager Fields ------------------------")

    // Manager Fields
    def nameOfManagerCF = customFieldManager.getCustomFieldObjectsByName("Name of the Manager")[0]
    def nameOfDelegateCF = customFieldManager.getCustomFieldObjectsByName("Name of the Delegate")[0]
    def reqM1ManagerCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Requestor M1 Manager")[0]
    def reqM2ManagerCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Requestor M2 Manager")[0]

    String managerResult = null
    ApplicationUser nameOfManager = null

    nameOfManager = issue.getCustomFieldValue(nameOfManagerCF) as ApplicationUser
    if(nameOfManager != null) {
        managerResult =  nameOfManager.emailAddress
    }

    if(managerResult == null) {
        nameOfManager = issue.getCustomFieldValue(nameOfDelegateCF) as ApplicationUser
        managerResult =  nameOfManager?.emailAddress
    }


    if(nameOfManager != null) {
        issue.setCustomFieldValue(reqM1ManagerCF, nameOfManager)
        log.warn("Req M1 Manager : ${nameOfManager}")

        String managerAttributeUsernamekey = "Email Address"
        String managerAttributeUserKey = "Key"
        def managerInsightObjects = null

        //Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade")
        //def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass)

        if(managerResult){
            managerInsightObjects = iqlFacade.findObjectsByIQLAndSchema(schemaID, "objectType = Users and \"${managerAttributeUsernamekey}\" = \"${managerResult}\"")
            log.warn("CMDB M1 Managers : ${managerInsightObjects}")

            def manager2IdBean = objectFacade.loadObjectAttributeBean(managerInsightObjects[0].getId(), "Manager1")

            if( manager2IdBean ) {
                def manager2Id = manager2IdBean.getObjectAttributeValueBeans()[0].getValue()
                log.warn("CMDB M2 Managers : CMDB-" + manager2Id)

                def m2ManagerEmailBean = objectFacade.loadObjectAttributeBean(manager2Id, "Email Address")
                if(m2ManagerEmailBean) {
                    def m2ManagerEmail = m2ManagerEmailBean.getObjectAttributeValueBeans()[0].getValue()
                    def users = ComponentAccessor.userSearchService.findUsersByEmail(m2ManagerEmail)
                    def m2ManagerApplicationUser = (users==null || users.isEmpty())?null:users[0]

                    issue.setCustomFieldValue(reqM2ManagerCF, m2ManagerApplicationUser)
                    log.warn("Manager M2 Email (AD) : ${m2ManagerApplicationUser}")
                }
            }else{
                log.error("CMDB M2 Manager not Found")
            }
        }else{
            log.error("Manager/Delegate Details not present.")
        }
    }
    log.warn("----Process Complete---")
}
