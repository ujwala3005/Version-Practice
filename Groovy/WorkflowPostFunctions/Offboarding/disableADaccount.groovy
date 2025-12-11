package com.workflows.offboarding.DisableADAccount.postfunctions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.servicedesk.api.comment.ServiceDeskCommentService
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
@WithPlugin("com.atlassian.servicedesk")

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;



// Get Issue Details
//def issue = ComponentAccessor.issueManager.getIssueByCurrentKey("IT-208080")
//MutableIssue issue = ComponentAccessor.issueManager.getIssueObject(issue.getKey())

log.warn "****************************** Processing Issue - $issue ******************************"
log.warn("Script Execution Time : " + new Date().toString())
log.warn ""

def loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def credentials = "jiraet-offboarding@cotiviti.com:Welcome#123"


// Fetch Offboarded User Email
def associateNameCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("customfield_11615"); // Associate Name
def associateName = issue.getCustomFieldValue(associateNameCF)
log.warn("Associate Email : " + associateName)

// Create JSON Payload
String aname = associateName.toString();
def index = aname.indexOf('(');
String email = aname.substring(0,index);
log.warn("Email Value : " + email)

def jsonInput = "{ \"login\" : \"${email}\" }"
log.warn("JSON Payload : " + email)

// Modification needed based on Environment
def url = "https://cotiviti-workflows.workflows.okta.com/api/flo/53ff480015c9bba24b99a79100976cab/invoke"
def token = "c5ab10d8309cda273fa212d52e3ea8f0fbbf169575e1fc9d72376c7c780f0b66"

// Send HTTP POST 
HttpClient client = new HttpClient();
PostMethod post = new PostMethod(url);
post.addRequestHeader("Content-Type", "application/json");
post.addRequestHeader("accept", "application/json");
post.addRequestHeader("x-api-client-token", token) 
post.addRequestHeader("Authorization", "Basic ${credentials.bytes.encodeBase64()}");
post.setRequestBody(jsonInput);
int httpCode = client.executeMethod(post);

def output = post.getResponseBodyAsString( );
post.releaseConnection( );

log.warn("Output : " + output);
log.warn("Response Code : " + httpCode);

if(httpCode == 200 || httpCode == 201){
    log.warn "User Disabled Successfully in OKTA"
    final String commentBody = """User Disabled Successfully in OKTA."""
    addComment(issue, true, loggedInUser, commentBody)
}else{
    log.warn "#### Error in User Disablement"
    final String commentBody = """Could not lock user. Parameters or configuration invalid."""
    addComment(issue, true, loggedInUser, commentBody)
}

// Add Jira Comment
void addComment(MutableIssue issueToComment, Boolean internal, ApplicationUser author, String text) {
	def serviceDeskCommentService = ComponentAccessor.getOSGiComponentInstanceOfType(ServiceDeskCommentService)
    def createCommentParameters = serviceDeskCommentService.newCreateBuilder()
            .author(author)
            .body(text)
            .issue(issueToComment)
            .publicComment(!internal)
            .build()
    serviceDeskCommentService.createServiceDeskComment(author, createCommentParameters)
    log.warn("Comment Added Successfully")
}

log.warn "********************************* Processing Complete ************************************"
