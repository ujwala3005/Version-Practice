package com.listener.Customlisteners
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.crowd.embedded.api.Group
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade


@WithPlugin('com.riadalabs.jira.plugins.insight')
@PluginModule ObjectFacade objectFacade 



def issueType = event.issue.issueType
// issue.creator.emailAddress=="MoveworksSvcAcct@cotiviti.com"

if(issueType.name== "Generic Service Request" ) 
{
    Date date = new Date()
Date DateVal1 = (issue.getCreated() as Date)
//log.warn(DateVal1)    

Date date2=new Date("2021/06/21")
//if(DateVal1>date2)
//{
  
    def changeLog = event.changeLog
    def related = changeLog.getRelated("ChildChangeItem")
    
    def insightAssignmentGroupGV = related.find { it.field == "Assignment Group" }
    if(insightAssignmentGroupGV != null) {

        // Set Assignment Group from Insight Assignment Group
        def customFieldManager = ComponentAccessor.getCustomFieldManager()

        //Insight Assignment Group Field
        //def iAssignmentGroupCF = customFieldManager.getCustomFieldObject(12613)
        def iAssignmentGroupCF = customFieldManager.getCustomFieldObjectByName("Assignment Group")

        //issue value
		def iAssignmentGroupValues = issue.getCustomFieldValue(iAssignmentGroupCF)

        //def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade"));

        def groupObject = iAssignmentGroupValues[0]

        def groupNameAttribute = objectFacade.loadObjectAttributeBean(groupObject.id, "Group Name")
        def groupNameAttributeValues = groupNameAttribute.getObjectAttributeValueBeans()	

		String groupName = groupNameAttributeValues[0].value as String
        def assignmentGroupCF = customFieldManager.getCustomFieldObjectByName("Internal Assignment Group")

        def groupManager = ComponentAccessor.getGroupManager()
        Group group = groupManager.getGroup(groupName)
        issue.setCustomFieldValue(assignmentGroupCF, [group]);
		
		// Set Assignee null
        issue.setAssignee(null);
        
        /*def managerCF = customFieldManager.getCustomFieldObjectsByName("Escalation Point")[0] // "Group Manager"

        def managerAttribute = objectFacade.loadObjectAttributeBean(groupObject.id, "Manager")
        def managerAttributeValues = managerAttribute.getObjectAttributeValueBeans()
        def manager = managerAttributeValues[0].value

        def managerNameAttribute = objectFacade.loadObjectAttributeBean(manager, "Display Name")
        def managerNameAttributeValues = managerNameAttribute.getObjectAttributeValueBeans()
        String managerName = managerNameAttributeValues[0].value
        
        if(managerName!=null) {
           // def userManager = ComponentAccessor.userManager
            //ApplicationUser managerUser = userManager.getUserByName(managerName)

            issue.setCustomFieldValue(managerCF, managerName);        
        }
        */
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
        def issueManager = ComponentAccessor.issueManager
        issueManager.updateIssue(user, issue, EventDispatchOption.ISSUE_UPDATED , true);
	}
}


//}	
