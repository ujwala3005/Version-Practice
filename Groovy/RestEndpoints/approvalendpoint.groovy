import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonOutput
import groovy.transform.BaseScript
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.user.search.UserSearchService
import com.atlassian.jira.service.util.ServiceUtils
import com.atlassian.jira.user.ApplicationUser

import com.atlassian.servicedesk.api.approval.Approval
import com.atlassian.servicedesk.api.approval.ApprovalQuery
import com.atlassian.servicedesk.api.approval.ApprovalService
import com.atlassian.servicedesk.api.util.paging.PagedResponse
import com.atlassian.servicedesk.api.approval.ApprovalDecisionType
 
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import com.atlassian.servicedesk.api.customer.*
 
@BaseScript CustomEndpointDelegate delegate
 
approve(httpMethod: "GET") { MultivaluedMap queryParams ->
 
def issueId = queryParams.getFirst("issueId") as String;
def userEmailAddress = queryParams.getFirst("email") as String ;
def approvalStatus = queryParams.getFirst("issueStatusId") as String; 
def userDecision = queryParams.getFirst("decision") as String;

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def userManager = ComponentAccessor.getUserManager()
def issueManager = ComponentAccessor.getIssueManager()
def issueService = ComponentAccessor.getIssueService()
def commentManager = ComponentAccessor.getCommentManager()

def jiraAdmin = userManager.getUserByName("jiraadmin")
ApplicationUser appUser = userManager.getUserByName(userEmailAddress)

def issue = ServiceUtils.findIssueObjectInString(issueId);

if (appUser != null && issue != null && approvalStatus != null && userDecision != null) {
    def pendingApproval = null
    
    def approvalsField = customFieldManager.getCustomFieldObject(10100)
    def issueApprovals = issue.getCustomFieldValue(approvalsField)?.approvals
    
    // Go through the approvals and find the pending approval
    issueApprovals?.find {
        if (!it.getDecision().isPresent()) {
            pendingApproval = it
            return true
        }
        
        return false
    }
    
    log.warn(appUser);
    log.warn(pendingApproval.getName());

    def approvalService = ComponentAccessor.getOSGiComponentInstanceOfType(ApprovalService)
    // Find if the sender is eligible to answer the current pending approval
    def canAnswerApproval = pendingApproval != null ?
        approvalService.canAnswerApproval(appUser, pendingApproval) : false
    

    if (userDecision && appUser != null && pendingApproval != null && canAnswerApproval) {
        def decision = userDecision == "approved" ?
            ApprovalDecisionType.APPROVED : ApprovalDecisionType.REJECTED
        
        //Set the logged in user to sender user, so that the ticket can be approved by them
        ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(appUser)
        
        def resultApproval = null
        
        try {
            log.warn(new Date());
            CustomerContextService customerContextService = ComponentAccessor.getOSGiComponentInstanceOfType(CustomerContextService.class)
                customerContextService.runInCustomerContext({
                    approvalService.answerApproval(appUser, pendingApproval, decision)
            });
            log.warn(new Date());
        } catch (Exception ex) {
            // Changing logged in user back to Jira admin, for sending error email
            log.warn(ex);
        }
        
        if (resultApproval != null) {
            
            // Changing logged in user back to Jira admin, for adding comments
            //ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(jiraAdmin)
            
            //commentManager.create(issue, jiraAdmin, "[~${appUser?.name}] ${decision} the ticket via Bot", false)

            /*userComment = userComment?.trim()
            if (userComment && userComment != "") {
                commentManager.create(issue, appUser, userComment, false)
            }*/
        }
    }
}



def flag = [
    result : "This issue "+issueId+" has been "+userDecision
]
 
Response.ok(JsonOutput.toJson(flag)).build()
}
