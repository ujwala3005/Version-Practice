import com.atlassian.jira.event.issue.IssueEventManager
import com.atlassian.jira.issue.comments.Comment
import groovy.json.JsonSlurper
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.component.ComponentAccessor
import groovy.json.JsonOutput
import groovy.transform.BaseScript
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import com.atlassian.servicedesk.api.customer.*
import com.atlassian.jira.issue.comments.CommentManager
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.mail.Email 

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.JSON
import groovy.json.JsonOutput

@BaseScript CustomEndpointDelegate delegate
portalComment(httpMethod: "POST") { MultivaluedMap queryParams, String body ->

    def slurper = new JsonSlurper()
    def bodytext = slurper.parseText(body)
    def author = bodytext["author"]
    String commentBody = bodytext["comment"]

    def issuekey = bodytext["issuekey"]
    def commentManager = ComponentAccessor.getCommentManager()
    def issueEventManager = ComponentAccessor.getIssueEventManager()
    def customFieldManager = ComponentAccessor.getCustomFieldManager()
 
 
 
    if(!author){
        return Response.status(400).entity("Missing userKey " + bodytext["author"]).build()
    }
    if(!issuekey){
        return Response.status(400).entity("Missing issueKey " + bodytext["issuekey"]).build()
    }
    if(!commentBody){
        return Response.status(400).entity("Missing comment " + bodytext["comment"]).build()
    }
 
    ApplicationUser user = Users.getByName(author.toString())
    Issue issue = Issues.getByKey(issuekey.toString())
 
    if(!user){
        return Response.status(400).entity("Missing userKey " + bodytext["author"]).build()
    }
 
    if(!issue){
        return Response.status(400).entity("Missing issueKey " + bodytext["issuekey"]).build()
    }
 
    Comment comment = commentManager.create(issue, user, commentBody, false) // 'true' makes it public

    log.warn(issue.getKey())
    log.warn(issue.getReporter())
    log.warn(issue.getCreated())

    if(true){
        def commentId = comment.getId()
        user = comment.getAuthorApplicationUser()
        def now = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        def commentResponse = [
            self            : "https://jiraet-uat.domain/rest/api/2/issue/${issuekey}/comment/${commentId}",
            id: commentId,
            author          : [
                self        : "https://jiraet-uat.domain/rest/api/2/user?username=" + user.getEmailAddress(),
                name        : user.getName(),
                key         : user.getKey(),
                emailAddress: user.getEmailAddress(),
                displayName : user.getDisplayName(),
                active      : user.isActive()
            
            ],
            body            : comment.getBody().toString(),
            updateAuthor    : [
                self        : "https://jiraet.domain/rest/api/2/user?username=" + user.toString(),
                name        : user.getName(),
                key         : user.getKey(),
                emailAddress: user.getEmailAddress(),
                displayName : user.getDisplayName(),
                active      : user.isActive()
            
            ],
            created         : now,
            updated         : now
        
        ]

        def webhookUrl = "https://jiraet-uat.cotiviti.com/rest/cb-automation/latest/hooks/5a7e49a805848eed9c531ef5b672985c1c5e038a?issue=${issuekey}"
        def bearerToken = "NzQ3OTA1OTU"
        
        def webhookPayload = [
            issueKey: issue.getKey(),
            commentId: comment.getId(),
            commentBody: comment.getBody(),
            author: user.getName(),
            triggeredBy: 'customer',
            events : ["jira:issue_commented"]
        ]

        def jsonPayload = JsonOutput.toJson(webhookPayload)

        def http = new HTTPBuilder(webhookUrl)
        http.request(POST) {
            requestContentType = JSON
            headers.'Authorization' = "Bearer ${bearerToken}"
            headers.'Content-Type' = 'application/json'
            body = jsonPayload

            response.success = { resp, reader ->
                log.warn("Webhook triggered successfully. Response: ${resp.statusLine}")
            }

            response.failure = { resp ->
                log.warn("Failed to trigger webhook. Status: ${resp.statusLine}")
            }
        }


        //mail ---------------------------------------------------------------------------------------------------------------------------------------
        def key = issue.getKey()
        def reporter = issue.getReporter()
        def status = issue.getStatus().getName()

        def emailBody = """
        
<html>
<head>
    <style>
        body {
            font-family: Times New Roman, sans-serif;
            font-size: 14px;
        }
        .jsd-message-content, .jsm-message-content {
            margin-bottom: 30px;
            padding-left: 20px;
        }
        .jsd-issue-link, .jsd-servicedesk-link, .jsd-unsubscribe-link, .jsm-issue-link {
            color: #ec008c;
            font-weight: bold;
            font-size: 20px;
            padding-left: 20px;
        }
        .jsm-servicedesk-link, .jsm-unsubscribe-link {
            color: #0052cc;
            text-decoration: none;
        }
        .jsd-link-separator, .jsm-link-separator {
            padding: 0 10px;
        }
        .jsd-help-center-footer, .jsd-request-sharedwith,
        .jsm-help-center-footer, .jsm-request-sharedwith {
            color: #7a869a;
            font-size: 12px;
        }
        .jsd-reply-marker, .jsm-reply-marker {
            color: #999;
        }
        .jsd-reply-marker-hint, .jsm-reply-marker-hint {
            color: #999;
            margin-bottom: 30px;
        }
        .jsd-activity-item-separator, .jsm-activity-item-separator {
            border: none;
            border-bottom: 1px solid #ccc;
        }
        .header {
            background-color: #30006e;
            color: #fff;
            padding: 5px;
            text-align: center;
            border-radius: 8px;
        }
        .header .plus {
            color: #ec008c;
        }
        .details {
            padding: 20px;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>MyCotiviti<span class="plus">+</span> Client Portal</h1>
    </div>

        <div class="details">
        <h2>Ticket Details:</h2>
			<div style="display: flex;">
				<span style="width: 115px; font-weight: bold;">Ticket Number:</span><span>${issue.key}</span>
			</div>
			<div style="display: flex;">
				<span style="width: 115px; font-weight: bold;">Reported by:</span><span>${reporter?.displayName ?: 'Unknown'}</span>
			</div>
			<div style="display: flex;">
				<span style="width: 115px; font-weight: bold;">Ticket Summary:</span><span>${issue.summary}</span>
			</div>
			<div style="display: flex;">
				<span style="width: 115px; font-weight: bold;">Ticket Status:</span><span>${status}</span>
			</div>
	</div>
            <div class="jsm-message-content">
                <h3>Message From: ${user?.displayName}</h3> 

                <span>${comment.getBody().toString()}</span>
            </div>

                <a class="jsm-issue-link" href="https://portal-dev.cotiviti.com/ticket/read/${issue.key}">View ticket</a>
   
            </body>
            </html>
            """
            
            def requestParticipantsField = customFieldManager.getCustomFieldObjectByName("Request participants")
            def recipients = []
            def ccRecipients = []
            if (reporter?.emailAddress) recipients << reporter.emailAddress
            if (requestParticipantsField) {
                def participants = issue.getCustomFieldValue(requestParticipantsField) as Collection<ApplicationUser>
                participants?.each { users ->
                    if (users?.emailAddress){ 
                        ccRecipients << users.emailAddress 
                    }

                }
            }




def customerEmailLogs = customFieldManager.getCustomFieldObjectByName("Customer email logs")
def currentmaillog = issue.getCustomFieldValue(customerEmailLogs)?.toString() ?: ""

def mailnowTime = new Date().format("yyyy-MM-dd HH:mm") 
def issueKey = issue.key.toString()
def mailbody = comment.getBody().toString()
def sender = user?.displayName
def subject = "New Comment ${issue.key} - ${issue.summary}"


def newLogEntry = [
    issueKey: issueKey,
    time: mailnowTime,
    body: mailbody,
    subject: subject,
    recipients: recipients,
    ccRecipients: ccRecipients,
    sender: sender
]

def newLogEntryJson = JsonOutput.toJson(newLogEntry)
def newmaillog = currentmaillog + "---LOG---" + newLogEntryJson



def issueCreatedTime = issue.getCreated().getTime()
def nowTime = new Date().getTime()
def timeDifferenceInSeconds = (nowTime - issueCreatedTime) / 1000
if (timeDifferenceInSeconds > 15) {
            if (!recipients.isEmpty()) {
                def mailServer = ComponentAccessor.mailServerManager.defaultSMTPMailServer
                if (mailServer) {
                    def email = new Email(recipients.unique().join(","))
                    email.setSubject("New Comment ${issue.key} - ${issue.summary}")
                    email.setMimeType("text/html")
                    email.setBody(emailBody)
                    email.setFrom("doNotReplyMydomain@domain.com")
                    if (!ccRecipients.isEmpty()) {
                        email.setCc(ccRecipients.unique().join(","))
                    }
                    mailServer.send(email)
                    log.info("Email sent to: ${recipients}")

                    issue.setCustomFieldValue(customerEmailLogs, newmaillog)
                    ComponentAccessor.getIssueManager().updateIssue(ComponentAccessor.jiraAuthenticationContext.loggedInUser, issue, com.atlassian.jira.event.type.EventDispatchOption.ISSUE_UPDATED,false)

                    //trigger manual event
                   
                    def customEventId = 6 
                    def params = [:]  
                    issueEventManager.dispatchEvent(customEventId, issue, params + ["comment": commentBody], user, false)
                    //commentManager.create(issue, user, commentBody, true)
}
                } else {
                    log.warn("Mail server not configured.")
                }
            } else {
                log.warn("No recipients found.")
            }
            return Response.ok(JsonOutput.toJson(commentResponse)).build() // Response similar to jira comment api
            

    }
    else{
        log.warn("Failed to creae comment for" + issuekey.toString() )
    }
 
//issueEventManager.dispatchRedundantEvent(IssueEventManager.ISSUE_COMMENTED_ID, issue, user, comment, false)
}


/*
def issueKey = issue.getKey().toString()
        def webhookUrl = "https://jiraet-uat.cotiviti.com/rest/api/2/issue/${issueKey}/transitions"
        def bearerToken = "Nz"
        int transitionID = 101
        
        def webhookPayload = [
            transition : [
                id : transitionID
            ]
        ]
        
        
        def jsonPayload = JsonOutput.toJson(webhookPayload)
        
        def http = new HTTPBuilder(webhookUrl)
        log.error("Sending payload: " + JsonOutput.prettyPrint(JsonOutput.toJson(webhookPayload)))

        try{
            http.request(POST) {
                requestContentType = JSON
                headers.'Authorization' = "Bearer ${bearerToken}"
                headers.'Content-Type' = 'application/json'
                body = jsonPayload

                response.success = { resp, reader ->
                    log.error("Webhook triggered successfully. Response: ${resp.statusLine}")
                }
            
                response.failure = { resp ->
                    log.error("Failed to trigger webhook.")
                    log.error("Status: ${resp.statusLine}")
                    log.error("Status Code: ${resp.status}")
                    log.error("Response Content: ${resp.entity?.content?.text}")
                }
            
            }
        }
        catch(Exception e){
            log.warn(e)
        }
        

*/
