//location--- atl.jira.view.issue.leftcontext
//issue?.key == 'SPT-3171'
// consotion is issue?.projectObject?.key == 'SPT'
import com.atlassian.jira.component.ComponentAccessor
import groovy.json.JsonSlurper

def issue = context.issue
def customFieldManager = ComponentAccessor.getCustomFieldManager()

def logsField = customFieldManager.getCustomFieldObjects(issue).find { it.name == "Customer email logs" }
def logsRaw = issue.getCustomFieldValue(logsField)?.toString()

if (!logsRaw) {
    writer.write("<p>No email logs found.</p>")
    return
}

def logs = logsRaw.split("---LOG---").findAll { it.trim() }.collect {
    new JsonSlurper().parseText(it.trim())
}

writer.write("<div class='mod-header'><h3>Customer Email Log</h3></div>")
writer.write("""
<style>
    .mail-logs-container {
        max-width: 100%;
        max-height: 600px;
        overflow-y: auto;
        font-family: 'Times New Roman', serif;
        font-size: 14px;
        box-sizing: border-box;
        padding-right: 5px;
    }
    .mail-log-container {
        background-color: #fefefe;
        border: 1px solid #e0e0e0;
        border-radius: 14px;
        box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
        margin: 14px 0;
        font-family: Calibri, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    }
    .mail-log-header {
        background-color: #fefefe;
        color: #5b9bd5;
        padding: 12px 18px;
        cursor: pointer;
        font-weight: 700;
        font-size: 14px;
        user-select: none;
        border-bottom: 1px solid #e0e0e0;
        border-radius: 14px 14px 0 0;
        transition: background-color 0.2s ease;
    }
    .mail-log-body {
        display: none;
        padding: 16px 18px;
        background-color: #ffffff;
        max-height: 300px;
        overflow-y: auto;
        font-size: 13.5px;
        color: #333;
        white-space: normal;
        border-radius: 0 0 14px 14px;
        font-family: Calibri, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    }

    .structured-layout {
        display: flex;
        flex-direction: column;
        gap: 12px;
    }
    .structured-layout .row {
        display: flex;
        border-bottom: 1px solid #ddd;
        padding-bottom: 6px;
    }
    .structured-layout .label {
        width: 140px;
        font-weight: bold;
        color: #333;
    }
    .structured-layout .value {
        flex: 1;
        color: #444;
    }
    .structured-layout .body-row .value {
        padding-top: 10px;
    }

    .header {
        background-color: #30006e;
        padding: 5px;
        text-align: center;
        border-radius: 8px;
    }

    .header .letters {
        color: #fff !important;
    }

    .header .plus {
        color: #ec008c !important;
    }
    .details {
        padding: 10px 0;
    }
    .jsm-message-content {
        margin-bottom: 30px;
        padding-left: 10px;
    }
    .jsm-issue-link {
        color: #ec008c;
        font-weight: bold;
        font-size: 14px;
    }
</style>

<script>
    function toggleMailLog(id) {
        var body = document.getElementById(id);
        if (body.style.display === "none" || body.style.display === '') {
            body.style.display = "block";
        } else {
            body.style.display = "none";
        }
    }
</script>
""")

writer.write("<div class='mail-logs-container'>")

logs.eachWithIndex { log, i ->
    def timestamp = log?.time ?: "Unknown time"
    def commentBody = log?.body?.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;") ?: "No content"
    def sender = log?.sender?.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;") ?: "Unknown sender"
    def issueKey = log?.issueKey ?: issue.key
    def summary = issue.summary?.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;") ?: ""
    def reporter = issue.reporter?.displayName?.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;") ?: "Unknown"
    def status = issue.status?.name?.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;") ?: ""
    def uniqueId = "logBody_${i}"


    def recipients = log?.recipients?.collect { it.toString().replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")}.join(", ")
    if (!recipients) recipients = "Unknown recipients"

    def ccrecipients = log?.ccRecipients?.collect { it.toString().replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")}.join(", ")
    if (!ccrecipients) ccrecipients = "None"

    def subject = log?.subject?.toString()?.replaceAll("&", "&amp;")?.replaceAll("<", "&lt;")?.replaceAll(">", "&gt;") ?: "No subject"

    writer.write("""
    <div class="mail-log-container">
        <div class="mail-log-header" onclick = "toggleMailLog('${uniqueId}')">
            Email from ${sender} on ${timestamp} <em>${subject}</em>
        </div>
        <div class="mail-log-body" id="${uniqueId}">
            <div class="structured-layout">
                <div class="row"><div class="label">From:</div><div class="value">${sender}</div></div>
                <div class="row"><div class="label">To:</div><div class="value">${recipients}</div></div>
                <div class="row"><div class="label">Subject:</div><div class="value">${subject}</div></div>
                <div class="row"><div class="label">CC:</div><div class="value"></div></div>
                <div class="row"><div class="label">BCC:</div><div class="value">${ccrecipients}</div></div>
                <div class="row body-row">
                    <div class="label">Body:</div>
                    <div class="value">
                        <div class="header">
                            <h1><span class="letters">MyCotiviti<span class="plus">+</span> Client Portal </span></h1>
                        </div>

                        <div class="details">
                            <h3>Ticket Details:</h3>
                            Ticket number: ${issueKey}<br/>
                            Reported by: ${reporter}<br/>
                            Ticket summary: ${summary}<br/>
                            Ticket Status: ${status}<br/>
                        </div>
                        <div class="jsm-message-content">
                        <h4>Message From: ${sender}</h4><br/>
                            <div style='white-space: normal;'>${commentBody}</div>
                        </div>
                        <a class="jsm-issue-link" href="https://mydomain/${issueKey}">View ticket</a><br/>
                    </div>
                </div>
            </div>
        </div>
    </div>
    """)
}

writer.write("</div>")
