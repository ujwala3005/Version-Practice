//condition
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.user.UserUtils
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserManager

def customFieldManager = ComponentAccessor.customFieldManager
ApplicationUser loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
CustomField pendingWithCf = customFieldManager.getCustomFieldObjectsByName("Change Pending With")[0]
Collection<ApplicationUser> pendingWith = issue.getCustomFieldValue(pendingWithCf) as Collection<ApplicationUser>
    
CustomField approvedByCf = customFieldManager.getCustomFieldObjectsByName("Change Approved By")[0]
Collection<ApplicationUser> approvedBy = issue.getCustomFieldValue(approvedByCf) as Collection<ApplicationUser>
    
CustomField declinedByCf = customFieldManager.getCustomFieldObjectsByName("Change Declined By")[0]
Collection<ApplicationUser> declinedBy = issue.getCustomFieldValue(declinedByCf) as Collection<ApplicationUser>

boolean isInIssueType = issue.issueType.name in ["Template"]
boolean cabApprovalsPresent = (pendingWith != null && !pendingWith.isEmpty()) || (approvedBy != null && !approvedBy.isEmpty()) || (declinedBy != null && !declinedBy.isEmpty())

boolean result = issue.projectObject.name == "Templates" && isInIssueType && cabApprovalsPresent
result
//script action
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.user.UserUtils
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserManager

def customFieldManager = ComponentAccessor.customFieldManager
ApplicationUser loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser

def avatarService = ComponentAccessor.avatarService
StringBuilder output = new StringBuilder(1000)
Issue issue = context["issue"] as Issue

CustomField pendingWithCf = customFieldManager.getCustomFieldObjectsByName("Change Pending With")[0]
Collection<ApplicationUser> pendingWith = issue.getCustomFieldValue(pendingWithCf) as Collection<ApplicationUser>
    
CustomField approvedByCf = customFieldManager.getCustomFieldObjectsByName("Change Approved By")[0]
Collection<ApplicationUser> approvedBy = issue.getCustomFieldValue(approvedByCf) as Collection<ApplicationUser>
    
CustomField declinedByCf = customFieldManager.getCustomFieldObjectsByName("Change Declined By")[0]
Collection<ApplicationUser> declinedBy = issue.getCustomFieldValue(declinedByCf) as Collection<ApplicationUser>

    if(approvedBy != null && !approvedBy.isEmpty()) {
        output.append("<p style=\"color: #172b4d;font-size: 14px;font-weight: 600;\">Approved</p>")
        output.append("<ul style=\"display: block;list-style: none;padding: 0px\">")
		approvedBy.each {
	        URI avatar = avatarService.getAvatarURL(loggedInUser, it)
            output.append("<li><span style=\"display: inline-flex;display: -webkit-inline-box;\"><span class=\"aui-avatar aui-avatar-small aui-avatar-inner\"><img src=\"${avatar.toString()}\"></span><span class=\"user-hover ${it.active?"active":"inactive"}\" rel=\"${it.username}\" style=\"padding-left: 5px;\">${it.displayName}</span></span></li>")
        }
        output.append("</ul>")
    }

    if(declinedBy != null && !declinedBy.isEmpty()) {
        output.append("<p style=\"color: #172b4d;font-size: 14px;font-weight: 600;\">Declined</p>")
        output.append("<ul style=\"display: block;list-style: none;padding: 0px\">")
		declinedBy.each { 
	        URI avatar = avatarService.getAvatarURL(loggedInUser, it)
            output.append("<li><span style=\"display: inline-flex;display: -webkit-inline-box;\"><span class=\"aui-avatar aui-avatar-small aui-avatar-inner\"><img src=\"${avatar.toString()}\"></span><span class=\"user-hover ${it.active?"active":"inactive"}\" rel=\"${it.username}\" style=\"padding-left: 5px;\">${it.displayName}</span></span></li>")
        }
        output.append("</ul>")
    }

	//Extra check to remove approved and declined users from pending field
    if(pendingWith != null && !pendingWith.isEmpty()) {
        if(approvedBy) {
            pendingWith.removeAll(approvedBy)
        }
        if(declinedBy) {
            pendingWith.removeAll(declinedBy)
        }
    }

	if(pendingWith != null && !pendingWith.isEmpty()) {
        output.append("<p style=\"color: #172b4d;font-size: 14px;font-weight: 600;\">Pending</p>")
        output.append("<ul style=\"display: block;list-style: none;padding: 0px\">")
		pendingWith.each { 
	        URI avatar = avatarService.getAvatarURL(loggedInUser, it)
            output.append("<li><span style=\"display: inline-flex;display: -webkit-inline-box;\"><span class=\"aui-avatar aui-avatar-small aui-avatar-inner\"><img src=\"${avatar.toString()}\"></span><span class=\"user-hover ${it.active?"active":"inactive"}\" rel=\"${it.username}\" style=\"padding-left: 5px;\">${it.displayName}</span></span></li>")
        }
        output.append("</ul>")
    }
writer.write("${output.toString()}")
