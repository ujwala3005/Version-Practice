//condition
import com.atlassian.jira.component.ComponentAccessor

def groupManager = ComponentAccessor.groupManager
def loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser

try {
    final username = loggedInUser?.username

    if (username) {
        def groupNames = groupManager?.getGroupNamesForUser(username)?.toArray()

        if (groupNames) {
            if (groupManager.isUserInGroup(username, "ECHDFWASupport") || 
                groupManager.isUserInGroup(username, "ECHDPASupport") || 
                groupManager.isUserInGroup(username, "ECHDPAYSupport") || 
                groupManager.isUserInGroup(username, "ECHDQISupport") || 
                groupManager.isUserInGroup(username, "ECHDRETSupport") || 
                groupManager.isUserInGroup(username, "ELZSupport") || 
                groupManager.isUserInGroup(username, "HGBSupport") || 
                groupManager.isUserInGroup(username, "RASSupport") || 
                groupManager.isUserInGroup(username, "PPMSupport") ||
                groupManager.isUserInGroup(username, "Unified Project Group") || 
                groupManager.isUserInGroup(username, "UP Group") ||  
                groupManager.isUserInGroup(username, "UPSupport") ||  
                groupManager.isUserInGroup(username, "ECOSL1") ||  
                groupManager.isUserInGroup(username, "ECOSL2") || 
                 groupManager.isUserInGroup(username, "QI SPT Group") || 
                  groupManager.isUserInGroup(username, "PAS SPT Group") ||
                  groupManager.isUserInGroup(username, "HGB SPT Group") || 
                  groupManager.isUserInGroup(username, "Cigna SPT Group") || 
                groupManager.isUserInGroup(username, "ECOS Tech Team")) {
    
                return true
            }
        }
    }
} catch (Exception e) {
    log.error("exceptional handiled: ${e.message}")
}

return false
//script
writer.write("<li><a href='/servicedesk/customer/portal/31?exitRefined=true' class='aui-button aui-button-primary aui-style' id='portal-link-link' target='_blank'>Unified Portal</a></li>")
