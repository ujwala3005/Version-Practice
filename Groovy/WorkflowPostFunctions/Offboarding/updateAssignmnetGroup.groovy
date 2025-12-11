package com.workflows.offboardingSubtask.Create.postfunctions
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.plugin.Plugin
import com.atlassian.plugin.PluginAccessor
import com.atlassian.jira.issue.util.IssueChangeHolder
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.crowd.embedded.api.Group
import groovy.json.*
import groovy.util.*

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
    

    log.warn "*************** Processing Subtask - $issue ***************"
	log.warn("Script Execution Time : " + new Date().toString())
	log.warn "---------------------------------------------------------"

try {
    if (issue.getIssueType().getName() == "Offboarding Subtask") {
        def customFieldManager = ComponentAccessor.getCustomFieldManager()
        def issueManager = ComponentAccessor.getIssueManager()
        def groupManager = ComponentAccessor.getGroupManager()
        def assignedGroup = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Internal Assignment Group")
        def insightAssignmentGroupCf = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Assignment Group")
        def escalationCF = customFieldManager.getCustomFieldObjectByName("Escalation Point")
        def gm = ComponentAccessor.getGroupManager()
        def appValue = issue.getCustomFieldValue(insightAssignmentGroupCf)
        def escalationVal = issue.getCustomFieldValue(escalationCF)
        //Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade")
        //def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass)
        def managerAttribute = null
        def managerAttributeValues = null
        def manager = null
        def managerAttributeName = null
        def managerAttributeValuesName = null
        def managerName = null

        def terminationDateCF = customFieldManager.getCustomFieldObjectByName("Termination Date")
        def parentTerminationDate = issue.parentObject.getCustomFieldValue(terminationDateCF)
        issue.setCustomFieldValue(terminationDateCF, parentTerminationDate)

        String attributeUserID = "Key"

        //Class objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade");
        //def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass)

        def objectsgroup

        def group1

        def issuecontext = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Issue Context")[0])
        
        log.warn("### PROCESSING Subtask context -- " + issuecontext)

        if (issue.getSummary().toString() != "Manager Acknowledgement") {
            if (issuecontext == "Network Ops - Voice Task") {
                group1 = gm.getGroup("Network Ops - Voice")
                objectsgroup = iqlFacade.findObjectsByIQLAndSchema(25, "\"Group Name\" = \"Network Ops - Voice\"")
            } else if (issuecontext == "Desk Side Task") {
                def newlocation = (String) issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Location"))

                if (newlocation != null) {

                    if (newlocation.indexOf('Atlanta') > -1 || newlocation.indexOf('McKinney') > -1 || newlocation.indexOf('Blue Bell') > -1 || newlocation.indexOf('Wilton') > -1 || newlocation.indexOf('Norwalk') > -1) {
                        group1 = gm.getGroup("Desk Side US")
                        objectsgroup = iqlFacade.findObjectsByIQLAndSchema(25, "\"Group Name\" = \"Desk Side US\"")
                    } else if (newlocation.indexOf('Hyderabad') > -1 || newlocation.indexOf('Pune') > -1 || newlocation.indexOf('Bangalore') > -1 || newlocation.indexOf('Chennai') > -1 ||
                        newlocation.indexOf('Coimbatore') > -1 || newlocation.indexOf('Noida') > -1 || newlocation.indexOf('kochi') > -1 ||
                        newlocation.indexOf('Salem') > -1 || newlocation.indexOf('Trichy') > -1 || newlocation.indexOf('Trivandrum') > -1) {
                        group1 = gm.getGroup("Desk Side - India")
                        objectsgroup = iqlFacade.findObjectsByIQLAndSchema(25, "\"Group Name\" = \"Desk Side - India\"")
                    } else if (newlocation.indexOf('Kathmandu') > -1) {
                        group1 = gm.getGroup("Deskside-Nepal")
                        objectsgroup = iqlFacade.findObjectsByIQLAndSchema(25, "\"Group Name\" = \"Deskside-Nepal\"")
                    }else if (newlocation.indexOf('Philippines') > -1) {
                    group1 = gm.getGroup("Desk Side - Philippines")
                    objectsgroup = iqlFacade.findObjectsByIQLAndSchema(25, "\"Group Name\" = \"Desk Side - Philippines\"")
                    }
                     else {
                        group1 = gm.getGroup("Desk Side US")
                        objectsgroup = iqlFacade.findObjectsByIQLAndSchema(25, "\"Group Name\" = \"Desk Side US\"")
                    }
                } else {
                    group1 = gm.getGroup("Desk Side US")
                    objectsgroup = iqlFacade.findObjectsByIQLAndSchema(25, "\"Group Name\" = \"Desk Side US\"")
                }
            } else if (issuecontext == "Data Center Operations Task") {
                group1 = gm.getGroup("Data Center Operations")
                objectsgroup = iqlFacade.findObjectsByIQLAndSchema(25, "\"Group Name\" = \"Data Center Operations\"")
            } else if (issuecontext == "HR coordinators Group Task") {
                group1 = gm.getGroup("HRQueue")
                objectsgroup = iqlFacade.findObjectsByIQLAndSchema(25, "\"Group Name\" = \"HRQueue\"")
            }
            // InfoSec Operations - Please block the devices in security controls 
            else if (issuecontext == "InfoSec Operations Task") {
                group1 = gm.getGroup("InfoSec Operations")
                objectsgroup = iqlFacade.findObjectsByIQLAndSchema(25, "\"Group Name\" = \"InfoSec Operations\"")
            } else if (issuecontext == "Jira Enterprise Task") {
                group1 = gm.getGroup("Jira Enterprise Application Service Desk")
                objectsgroup = iqlFacade.findObjectsByIQLAndSchema(25, "\"Group Name\" = \"Jira Enterprise Application Service Desk\"")
            } //treasury Bank for bank users
              else if (issuecontext == "Bank User Treasury Task") {
                group1 = gm.getGroup("Treasury Team")
                objectsgroup = iqlFacade.findObjectsByIQLAndSchema(25, "\"Group Name\" = \"Treasury Team\"")
                //treasury Bank for bank users -- end
            }
            else if (issuecontext == "Okta Workforce Support Task") {
                group1 = gm.getGroup("Okta Workforce Support")
                objectsgroup = iqlFacade.findObjectsByIQLAndSchema(25, "\"Group Name\" = \"Okta Workforce Support\"")
            }
             else {
                group1 = gm.getGroup("Service Desk")
                objectsgroup = iqlFacade.findObjectsByIQLAndSchema(25, "\"Group Name\" = \"Service Desk\"")
            }
            
            log.warn("### Intrnal Assignment Group : "+ group1)
            log.warn("### Insight Assignment Group : "+ objectsgroup)
                   

            def changeHolder = new DefaultIssueChangeHolder();
            if (issue.getCustomFieldValue(insightAssignmentGroupCf) == null) {
                 log.warn("Insight Assignemt group is NULL/EMPTY")
                issue.setCustomFieldValue(insightAssignmentGroupCf, objectsgroup)
                issue.setCustomFieldValue(assignedGroup, [group1])
                
                try{
                    managerAttribute = objectFacade.loadObjectAttributeBean(objectsgroup[0].id, "Manager")
                	managerAttributeValues = managerAttribute?.getObjectAttributeValueBeans()
               		manager = "CMDB-" + managerAttributeValues[0].value
                	def objectsUser = iqlFacade.findObjectsByIQLAndSchema(25, "\"" + attributeUserID + "\" = \"" + manager + "\"")
                	managerAttributeName = objectFacade.loadObjectAttributeBean(objectsUser[0].id, "Display Name")
                	managerAttributeValuesName = managerAttributeName?.getObjectAttributeValueBeans()
                	managerName = managerAttributeValuesName[0].value  
                } catch (Exception e) {
                    log.error("### Manager is missing for group - "+ objectsgroup)
                    log.error(e.getMessage() +" | " + e)
                }
            } else {
                log.warn("Insight Assignemt group value is PRESENT")
                def groupObject = appValue[0]
                def groupNameAttribute = objectFacade.loadObjectAttributeBean(groupObject.id, "Group Name")
                def groupNameAttributeValues = groupNameAttribute?.getObjectAttributeValueBeans()
                String groupName = groupNameAttributeValues[0].value as String
                Group group = groupManager.getGroup(groupName)
                issue.setCustomFieldValue(assignedGroup, [group]);
                managerAttribute = objectFacade.loadObjectAttributeBean(groupObject.id, "Manager")
                managerAttributeValues = managerAttribute?.getObjectAttributeValueBeans()
                manager = "CMDB-" + managerAttributeValues[0].value
                def objectsUser = iqlFacade.findObjectsByIQLAndSchema(25, "\"" + attributeUserID + "\" = \"" + manager + "\"")
                managerAttributeName = objectFacade.loadObjectAttributeBean(objectsUser[0].id, "Display Name")
                managerAttributeValuesName = managerAttributeName?.getObjectAttributeValueBeans()
                managerName = managerAttributeValuesName[0].value
            }
                       
            // Temporary Fix : Hard Coding - to fix 
            //  issue.setCustomFieldValue(escalationCF, managerName)
            if (issuecontext == "InfoSec Operations Task" && managerName == null) {
                log.warn("### Executing Temporary Fix for isfosec-Operations ...")
                issue.setCustomFieldValue(escalationCF, "Luke Fowler")
            }else{
                issue.setCustomFieldValue(escalationCF, managerName)
            }
            

        } else {
            //Setting fields values required for Manager Acknowledgement Subtask
            def m2ManagerCF = ComponentAccessor.customFieldManager.getCustomFieldObjectByName("M2 Manager")
            def m2Manager = issue.parentObject.getCustomFieldValue(m2ManagerCF)
            if (m2Manager != null) {
                issue.setCustomFieldValue(m2ManagerCF, m2Manager)
            }

            def requestorM1ManagerCF = ComponentAccessor.customFieldManager.getCustomFieldObjectByName("Requestor M1 Manager")
            def requestorM1Manager = issue.parentObject.getCustomFieldValue(requestorM1ManagerCF)
            if (requestorM1Manager != null) {
                issue.setCustomFieldValue(requestorM1ManagerCF, requestorM1Manager)
            }

            def requestorM2ManagerCF = ComponentAccessor.customFieldManager.getCustomFieldObjectByName("Requestor M2 Manager")
            def requestorM2Manager = issue.parentObject.getCustomFieldValue(requestorM2ManagerCF)
            if (requestorM2Manager != null) {
                issue.setCustomFieldValue(requestorM2ManagerCF, requestorM2Manager)
            }

            log.warn("Subtask Manager Ack Updated with Values - requestorM1Manager: " + requestorM1Manager + ", M2Manager: " + m2Manager + " requestorM2Manager: " + requestorM2Manager)
        }

        def jiraAuthenticationContext = ComponentAccessor.jiraAuthenticationContext
        def userManager = ComponentAccessor.userManager
        ApplicationUser adminUser =  userManager.getUserByName("jiraAdmin")
        issueManager.updateIssue(adminUser, issue, EventDispatchOption.ISSUE_UPDATED , false);



        //ComponentAccessor.getIssueManager().updateIssue(null, issue, EventDispatchOption.ISSUE_UPDATED, false)
        log.warn("----------------------")
        log.warn("Subtask " + issue.getKey() + " Updated successfully with values present in Parent Issue")
    }
} catch (Exception e) {
	log.error(e.getMessage() +" | " + e)
}
