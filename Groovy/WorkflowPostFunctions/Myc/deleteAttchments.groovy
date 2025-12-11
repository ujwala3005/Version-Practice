import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.AttachmentManager
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.event.type.EventDispatchOption
 
//def issueKey = "SPT-1232"
 import com.atlassian.jira.issue.Issue
 
Issue issue = binding.getVariable("issue") as Issue
def issueManager = ComponentAccessor.getIssueManager()
def attachmentManager = ComponentAccessor.getAttachmentManager()
 
//def issue = issueManager.getIssueByCurrentKey(issueKey)
 
if (!issue) {
    log.error("Issue with key ${issueKey} not found.")
    return
}
 
 
def attachments = attachmentManager.getAttachments(issue)
 
if (attachments.isEmpty()) {
    log.warn("No attachments found for issue ${issueKey}.")
    return
}
 
attachments.each { attachment ->
    try {
        attachmentManager.deleteAttachment(attachment)
        log.warn("Deleted attachment: ${attachment.filename}")
    } catch (Exception e) {
        log.error("Failed to delete attachment: ${attachment.filename}", e)
    }
}
//ComponentAccessor.getIssueManager().updateIssue(null, issue, EventDispatchOption.ISSUE_UPDATED, false)
