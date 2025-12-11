import com.atlassian.jira.component.ComponentAccessor

def groupManager = ComponentAccessor.groupManager
def loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser

final username = loggedInUser.username

def groupNames = groupManager.getGroupNamesForUser(username).toArray()

if(groupManager.isUserInGroup(username, "Jira Enterprise Application Service Desk")|| groupManager.isUserInGroup(username, "ECHD_JSD")){
    
    return true
}
    return false

//version app--- servicedesk.portal.user.menu.actions
//add link in link section https://jiraet-uat.cotiviti.com/plugins/servlet/desk/category/versionapp
