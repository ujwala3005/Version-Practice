package com.jobs.OnboardingJobs

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.ImportUtils
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.mail.Email;
import com.atlassian.mail.server.MailServerManager;
import com.atlassian.mail.server.SMTPMailServer;
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.issue.search.SearchException

import org.apache.log4j.Level

def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchProvider = ComponentAccessor.getComponent(SearchProvider)
def issueManager = ComponentAccessor.getIssueManager()
def searchService = ComponentAccessor.getComponentOfType(SearchService)

String subject = ""
String body = ""
String emailAddr = ""

StringBuilder emailBodySB = new StringBuilder()

String jqlSearch = "project = IT AND issuetype = 'Onboarding/Rehire' AND created >= startOfWeek(-1w) AND created <= endOfWeek(-1w) AND 'Employment Type' != 'Vendor 3PI' AND cf[10113] = Email  ORDER BY Created ASC"

UserUtil userUtil = ComponentAccessor.getUserUtil()
ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

def parseResult = searchService.parseQuery(user, jqlSearch)
if (!parseResult.isValid()) {
    log.error('Invalid query')
    return null
}
try {
    // Perform the query to get the issues
	def results = searchService.search(user, parseResult.query, PagerFilter.unlimitedFilter)
	def issues = results.results
	def i=0;
	CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
	CustomField cFieldDomain = customFieldManager.getCustomFieldObject("customfield_12706")
	CustomField cFieldAssName = customFieldManager.getCustomFieldObject("customfield_12728")

	emailBodySB.append("<p>&nbsp;</p><table style=\"font-family: arial; font-size: 10pt; height: 440px;\" align=\"center\"><tbody><tr><td style=\"width: 1081px;\"><table style=\"font-family: arial; border-style: hidden; height: 25px;\" width=\"1081\" align=\"center\">")
	emailBodySB.append("<tbody><tr style=\"height: 15px;\"><td style=\"font-family: arial; font-size: 14pt; background: #31006f; color: #ffffff; border-style: none; width: 1075px; text-align: center; height: 25px;\" colspan=\"2\"><strong><span style=\"font-size: 20px; font-family: arial, helvetica, sans-serif;\">&nbsp; &nbsp;Cotiviti Service Desk</span></strong></td>")
	emailBodySB.append("</tr></tbody></table></td></tr><tr style=\"font-color: #31006f;\"><td style=\"color: #31006f; width: 1081px; text-align: left; height: 46px;\"><p style=\"text-align: left;\"><span style=\"font-family: verdana, geneva, sans-serif; font-size: 12px;\"><strong>&nbsp;Hello Team,</strong></span><br/><br/><span style=\"font-family: verdana, geneva, sans-serif; color: #000000;font-size: 12px;\">&nbsp;Please Check the list of all emails processed in last week for New Hire.</span></p>")
	emailBodySB.append("</td></tr><tr style=\"height: 17px;\"><td style=\"font-family: arial; font-size: 10pt; background: #ffffff; width: 1081px; height: 17px; text-align: left;\"></td>")
	emailBodySB.append("</tr><tr style=\"height: 112px;\"><td style=\"width: 1081px; height: 112px;\"><table style=\"border-collapse: collapse; width: 50%;margin-left:2px ;height: 21px;\" border=\"1\"><tbody>")
	emailBodySB.append("<tr style=\"height: 15px;\"><td style=\"font-family: verdana, arial, helvetica, sans-serif; color: white; font-size: 12px; font-weight: normal; border: 1px solid #34669b; width: 100%; background: #31006f; height: 15px; text-align: center;\"><span style=\"font-family: verdana, geneva, sans-serif; font-size: 12px;\"><strong>&nbsp;Weekly report for New Hire Email&#8217s for Cotiviti Employee&#8217s</strong></span></td>")
	emailBodySB.append("</tr></tbody></table><table style=\"border-collapse: collapse; margin-left:2px;font-family: verdana, arial, helvetica, sans-serif; border: 1px solid #34669b; height: 38px; width: 50%;\" cellpadding=\"5\">")
	emailBodySB.append("<tbody><tr style=\"height: 10px;\">")
	emailBodySB.append("<td style=\"font-family: verdana, arial, helvetica, sans-serif; background: #31006f; color: white; font-size: 12px; font-weight: normal; border: 1px solid #34669b; height: 10px; width: 2%;\"><span style=\"font-family: verdana, geneva, sans-serif; font-size: 12px;\"><strong>Sr.No.</strong></span></td>")
	emailBodySB.append("<td style=\"font-family: verdana, arial, helvetica, sans-serif; background: #31006f; color: white; font-size: 12px; font-weight: normal; border: 1px solid #34669b; height: 10px; width: 60%;\"><span style=\"font-family: verdana, geneva, sans-serif; font-size: 12px;\"><strong>Email Subject</strong></span></td>")
	emailBodySB.append("<td style=\"font-family: verdana, arial, helvetica, sans-serif; background: #31006f; color: white; font-size: 12px; font-weight: normal; border: 1px solid #34669b; height: 10px; width: 38%;\"><span style=\"font-family: verdana, geneva, sans-serif; font-size: 12px;\"><strong>Target Record</strong></span></td>")
	emailBodySB.append("</tr>")
	issues.each {
		def issue = ComponentAccessor.getIssueManager().getIssueObject(it.key)

		def cFieldDomainValue = issue.getCustomFieldValue(cFieldDomain).toString()
		def cFieldAssNameValue = issue.getCustomFieldValue(cFieldAssName).toString()
		log.warn(cFieldAssNameValue)
		i=i+1

		emailBodySB.append("<tr style=\"height: 15px;\">")
		emailBodySB.append("<td style=\"border-collapse: collapse; font-family: verdana, arial, helvetica, sans-serif; font-size: 12px; font-weight: normal; border: 1px solid #004080; height: 15px; width: 2%;\"><span style=\"font-family: verdana, geneva, sans-serif; font-size: 12px;\">")
		emailBodySB.append(i)
		emailBodySB.append("</span></td>")
		emailBodySB.append("<td style=\"border-collapse: collapse; font-family: verdana, arial, helvetica, sans-serif; font-size: 12px; font-weight: normal; border: 1px solid #004080; height: 15px; width: 60%;\"><span style=\"font-family: verdana, geneva, sans-serif; font-size: 12px;\">")
		emailBodySB.append(issue.summary)
		emailBodySB.append("</span></td>")
		emailBodySB.append("<td style=\"border-collapse: collapse; font-family: verdana, arial, helvetica, sans-serif; font-size: 12px; font-weight: normal; border: 1px solid #004080; height: 15px; width: 38%;\"><span style=\"font-family: verdana, geneva, sans-serif; font-size: 12px;\">Request: ")
		emailBodySB.append(it.key)
		emailBodySB.append("</span></td>")
		emailBodySB.append("</tr>")
    }
    
} catch (SearchException e) {
    e.printStackTrace()
    return
}

emailBodySB.append("</tbody></table><div>&nbsp;</div></td></tr>")
emailBodySB.append("<tr style=\"font-family: \"Montserrat\",helvetica,arial,sans-serif;\">")
emailBodySB.append("<td style=\"font-size: 10pt; width: 1081px; font-family: Montserrat, helvetica, arial, sans-serif; text-align: left; height: 17px;\"><span style=\"font-family: verdana, geneva, sans-serif; font-size: 12px;\">&nbsp;<strong>Please check the details and take appropriate action.</strong></span></td>")
emailBodySB.append("</tr><tr style=\"font-family: \"Montserrat\",helvetica,arial,sans-serif;\">")
emailBodySB.append("<td style=\"font-size: 10pt; width: 1081px; font-family: Montserrat, helvetica, arial, sans-serif; text-align: left; height: 17px;\">")
emailBodySB.append("<p><span style=\"font-family: verdana, geneva, sans-serif; font-size: 12px;\">&nbsp;Thanks.</span></p>")
emailBodySB.append("<p>&nbsp;</p></td></tr><tr style=\"font-family: \"Montserrat\",helvetica,arial,sans-serif;\"><td style=\"width: 1081px; font-family: Montserrat, helvetica, arial, sans-serif; height: 25px;\"><table style=\"font-family: arial;\">")
emailBodySB.append("<tbody><tr><td style=\"font-family: arial; font-size: 12px; background: #ec008c; color: #ffffff; text-align: center; width: 1063px;\"><span style=\"font-family: verdana, geneva, sans-serif; font-size: 12px; color: #ffffff;\"><strong>US Toll: 203.529.2200 | USA Toll Free: 1.844.301.4132 | UK Toll Free: 08-082342821 | India Toll Free: 000-800-9190289 | <span style=\"color: #ffffff;\">servicedesk@cotiviti.com</span></strong></span></td>")
emailBodySB.append("</tr></tbody></table></td></tr></tbody></table><div><div>&nbsp;</div></div><div>&nbsp;</div>")

String group1 = "HRQueue"
String jiraSupportTeam = "Jira-EnterpriseSD@cotiviti.com"

Collection<ApplicationUser> users = ComponentAccessor.groupManager.getUsersInGroup(group1)
List<String> toEmailsAddresses = users.findAll{ it.active }.collect{ it.emailAddress }
toEmailsAddresses.add(jiraSupportTeam)
String to = String.join(",",toEmailsAddresses) 

body = emailBodySB.toString()
sendEmail(body, to)

def sendEmail(String body, String emailAddresses) {
    def subject = "Weekly New Hire Request Emails for Cotiviti Employees"
    SMTPMailServer mailServer = ComponentAccessor.getMailServerManager().getDefaultSMTPMailServer();
    if (mailServer) {
        Email email = new Email(emailAddresses);
        email.setMimeType("text/html");
        email.setSubject(subject);
        email.setBody(body);
        mailServer.send(email);
    }
}
