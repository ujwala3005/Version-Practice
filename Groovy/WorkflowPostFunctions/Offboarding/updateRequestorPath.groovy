package com.workflows.offboarding.Create.postfunctions
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.crowd.manager.directory.DirectoryManager
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder


def resultObj =  ((ComponentAccessor.getCustomFieldManager().getCustomFieldObject("customfield_11615").getValue(issue)) as ApplicationUser)
if(resultObj == null) {
    return
}

def result =  resultObj.getUsername()
log.warn("result" + result)

Collection loggedUserGroupNames = ComponentAccessor.getGroupManager().getGroupNamesForUser(result);
log.warn("loggedUserGroupNames " +loggedUserGroupNames)

List adGroups = new ArrayList()

def dirMngr = ComponentAccessor.getComponentOfType(DirectoryManager.class)
for(group in loggedUserGroupNames)  
{       
    try
    {
        dirMngr.findGroupByName(1, group)            
    }        
    catch(Exception e)
    {
        adGroups.add(group.toString())
    }
}
log.warn("Refined Groups:  " +adGroups)
def cfIsSLTUser = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Requestor Path and Groups")[0]
cfIsSLTUser.updateValue(null,issue,new ModifiedValue(issue.getCustomFieldValue(cfIsSLTUser), adGroups.toString()),new DefaultIssueChangeHolder())



/*
def associateUser = ((ComponentAccessor.getCustomFieldManager().getCustomFieldObject("customfield_11615").getValue(issue)) as ApplicationUser).getUsername()
if(associateUser) {
    log.warn("loggeduser" +associateUser)
    Collection loggedUserGroupNames = ComponentAccessor.getGroupManager().getGroupNamesForUser(associateUser);
    log.warn("loggedUserGroupNames" +loggedUserGroupNames)
    
   List adGroups = new ArrayList()
   
    def dirMngr = ComponentAccessor.getComponentOfType(DirectoryManager.class)
    for(group in loggedUserGroupNames)  
    {       
        try
        {
            dirMngr.findGroupByName(1, group)            
        }        
        catch(Exception e)
        {
            adGroups.add(group.toString())
        }
    }
    log.warn("Refined Groups:  " +adGroups)
    def cfIsSLTUser = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Requestor Path and Groups")[0]
    cfIsSLTUser.updateValue(null,issue,new ModifiedValue(issue.getCustomFieldValue(cfIsSLTUser), adGroups.toString()),new DefaultIssueChangeHolder())
}
    
*/
