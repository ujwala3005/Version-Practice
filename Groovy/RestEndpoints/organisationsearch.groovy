import org.apache.logging.log4j.core.config.Scheduled
import java.text.SimpleDateFormat
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.net.ssl.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import java.util.concurrent.ConcurrentHashMap
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field
import casepoint.dashboard.CasepointJobMetrics

@BaseScript CustomEndpointDelegate delegate

@Field static List<String> apiLogList = []

organizationSearch(httpMethod: "GET") { queryParams ->
    return buildOrganizationDashboard(queryParams)
}

// Single method that does everything
def buildOrganizationDashboard(MultivaluedMap queryParams) {

    
    // --- Dark mode toggle ---
    def mode = queryParams.getFirst("mode") ?: "light"
    def isDark = mode == "dark"

    // --- Config & globals ---
    def apiToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6IkZYU0lhSGJpMHgyMDlQTkFiZFAwX05hcWFzNCIsImtpZCI6IkZYU0lhSGJpMHgyMDlQTkFiZFAwX05hcWFzNCJ9.eyJpc3MiOiJodHRwczovL2FwaS5jYXNlcG9pbnQuY29tL0lkZW50aXR5IiwiYXVkIjoiaHR0cHM6Ly9hcGkuY2FzZXBvaW50LmNvbS9JZGVudGl0eS9yZXNvdXJjZXMiLCJleHAiOjE3NTQ0NzgwNDYsIm5iZiI6MTc1MTg4NjA0NiwiY2xpZW50X2lkIjoicWFOQUQvcGRteUE0czdUVTJ1Q2FSVXlKclBJaUpjeWxlU3h5eFZLNE1WayIsInNjb3BlIjpbIkxlZ2FsSG9sZCIsIkxvZ3MiLCJNYXR0ZXJzIiwiT3JnYW5pemF0aW9ucyJdfQ.q6jFckjtC1TZilpXJGoN82p_FCgrM8QQErd4Z8FYFVpgONU9VyIn_bqbpVbte3rKasu51O49Ye8JUvdHfYCLx6Gae4oJ5yM13krSpAdIwagnhDfsHB8zElTMI53XwkRLVOuZbj-o9pJjDTx_Mxu8p7lxs39pD6ntBmK6j1RHTV848kltztbYlJ4KiPxWZHD-JL5syU8W94x94VJ6T6Q_rPxeZNwI2cb2dbbb3fRD6dB4ls7bhxu-3DpyPCvTCRm3iADv7cyDHTsDbXKpjLXyn8vrH_l7oRYvky9MXOCoeih4U_9GVLPQQkd7yGhUCfRU0kZHG1VjHRiseEUngm1qSQ"
    def cpToken = "f4a33cd0-a1f3-470f-8389-0348a4c0454e"
    def targetName = "Cotiviti UAT"          // Target org to search
    def organizationNames = []
    def foundOrg = null

    def writeApiLog = { String msg ->
        def timestamp = new Date().format("HH:mm:ss")
        def entry = "[${timestamp}] ${msg}"
        apiLogList.add(0, entry)
        if (apiLogList.size() > 50) {
            apiLogList = apiLogList.take(50)
        }
        log.warn entry
    }

    // --- GraphQL & HTTP ---
    def graphQLQuery = """
    {
        organizationList(currentPage: 0, recordPerPage: 5, sortBy: "organizationname", sortOrder: "desc") {
            pageInfo { currentPage recordPerPage sortBy sortOrder totalPages }
            totalRecordsCount
            data { organizationid organizationname }
        }
    }
    """

    def graphQLVariables = [ page:0, limit:5 ]
    def requestBody = JsonOutput.toJson([ query: graphQLQuery, variables: graphQLVariables ])

    disableSSLValidation()
    def url = new URL("https://api.casepoint.com/api")
    HttpURLConnection connection = (HttpURLConnection) url.openConnection()

    connection.with {
        requestMethod = "POST"
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Authorization", "Bearer ${apiToken}")
        setRequestProperty("cpToken", cpToken)
        setRequestProperty("Accept", "application/json")
        doOutput = true
        outputStream.withWriter("UTF-8") { it << requestBody }
    }

    def responseCode = connection.responseCode
    def responseText = responseCode < 400 ? connection.inputStream.text : connection.errorStream?.text
    log.warn "Response Code: ${responseCode}"
    log.warn "Response Body: ${responseText ?: 'No response body'}"

    // --- Parse & handle response ---
    def jsonResponse = new JsonSlurper().parseText(responseText)
    def orgList = jsonResponse?.organizationList?.data

    if (!orgList) {
        writeApiLog("⚠️ API connected but no organization data found.")
        return Response.ok("<p>No organization data found.</p>").type("text/html").build()
    }

    writeApiLog("✅ API connected. ${orgList.size()} organizations received.")
    orgList.each { org -> organizationNames << org.organizationname }

    def index = orgList.findIndexOf { it.organizationname.equalsIgnoreCase(targetName) }

    if (index != -1) {
        foundOrg = orgList[index]
        writeApiLog("🎯 Found '$targetName'. ID: ${foundOrg.organizationid}")
    } else {
        writeApiLog("🔍 '$targetName' not found in the list.")
    }

    def companyName = foundOrg?.organizationname ?: "Not Found"
    def companyID = foundOrg?.organizationid ?: "Not Found"

    def lastSuccessfulLog = apiLogList.find { it.contains("✅ API connected") }
    def lastSyncTime = lastSuccessfulLog ? "${new Date().format('yyyy-MM-dd')} ${lastSuccessfulLog.find(/\[\d{2}:\d{2}:\d{2}\]/)?.replaceAll(/\[|\]/,'')}" : "Never"
    def errorLogs = apiLogList.findAll { log -> log.contains("❌") || log.contains("⚠️") || log.toLowerCase().contains("failed") }

    // --- Build HTML ---
    def html = new StringBuilder()

    html << "<!DOCTYPE html>\n"
    html << "<html class='${isDark ? "dark-mode" : ""}'>\n"
    html << "<head>\n"
    html << "<title>Casepoint Dashboard</title>\n"
    html << "<style>\n"
    html << "html, body { margin:0; padding:0; height:100%; width:100%; font-family:sans-serif; background-color:#f4f5f7; }\n"
    html << ".container { width:95%; max-width:1200px; margin:40px auto; background:white; padding:30px; border-radius:10px; box-shadow:0 2px 8px rgba(0,0,0,0.1); min-height:calc(100vh - 80px); box-sizing:border-box; }\n"
    html << "h1 { color:#0747A6; margin-top:0; }\n"
    html << ".status.ok { color:green; font-weight:bold; }\n"
    html << ".section { margin-bottom:35px; }\n"
    html << "ul { background:#f0f0f0; padding:12px; border-radius:6px; list-style:none; }\n"
    html << ".rate-bar { background:#eee; height:20px; border-radius:5px; overflow:hidden; }\n"
    html << ".rate-fill { background:#6554C0; height:100%; color:white; padding-left:5px; line-height:20px; }\n"
    html << "button { background:#0052CC; color:white; padding:10px 20px; border:none; border-radius:5px; cursor:pointer; font-size:14px; margin-right:10px; }\n"
    html << "button:hover { background:#0747A6; }\n"
    html << "body.dark-mode { background-color:#121212; color:#e0e0e0; }\n"
    html << "body.dark-mode .container { background:#1e1e1e; box-shadow:0 2px 8px rgba(255,255,255,0.1); }\n"
    html << "body.dark-mode h1 { color:#90caf9; }\n"
    html << "body.dark-mode ul { background:#333; }\n"
    html << "body.dark-mode .rate-bar { background:#444; }\n"
    html << "body.dark-mode .rate-fill { background:#bb86fc; }\n"
    html << "body.dark-mode button { background:#333; color:#fff; }\n"
    html << "body.dark-mode button:hover { background:#555; }\n"
    html << "</style>\n"
    html << "</head>\n"
    html << "<body class='${isDark ? "dark-mode" : ""}'>\n"
    html << "<div class='container'>\n"
    html << "<center><h1>🛡️ Casepoint Integration Dashboard</h1></center>\n"
    html << "<div><a href='?mode=${isDark ? "light" : "dark"}'><button>${isDark ? "☀️ Light Mode" : "🌙 Dark Mode"}</button></a></div>\n"
    html << "<center><h1>${companyName} - ${companyID}</h1></center>\n"
    html << "<div class='section'><h2>✅ Current Sync Status</h2><p>${lastSyncTime}</p><p class='status ok'>Operational</p></div>\n"
    html << "<div class='section'><h2>📜 Recent Sync Logs</h2><ul>\n"
    apiLogList.take(10).each { html << "<li>${it}</li>\n" }
    html << "</ul></div>\n"
    html << "<div class='section'><h2>🚨 Error Notifications</h2>\n"
    if (errorLogs) {
        html << "<ul style='color:red;'>\n"
        errorLogs.each { html << "<li>${it}</li>\n" }
        html << "</ul>\n"
    } else {
        html << "<p style='color:green;'>✅ No recent errors</p>\n"
    }
    html << "</div>\n"
    html << "<div class='section'><h2>📈 API Rate Limit Info</h2><p>143 / 500 (29%)</p><div class='rate-bar'><div class='rate-fill' style='width:29%;'>143</div></div></div>\n"
    html << "<div class='section'><h2>🔄 Manual Trigger</h2><form method='POST' action='/rest/scriptrunner/latest/custom/manualsync'><button>🔁 Trigger Manual Sync</button></form></div>\n"
    html << "<div class='section'><h2>🧪 Environment</h2><p><strong>${companyName}</strong></p></div>\n"
    html << "<div class='section'><h2>🛠️ Admin Actions</h2><form method='POST' action='/rest/scriptrunner/latest/custom/clearlogs'><button>🧹 Clear Logs</button></form><form method='POST' action='/rest/scriptrunner/latest/custom/retryfailed'><button>🔁 Retry Failed</button></form></div>\n"
    html << "<div id='metrics'>Loading job metrics...</div>\n"

    html << "<script>"
    html << "async function loadMetrics() {"
    html << "  try {"
    html << "    const res = await fetch('/rest/scriptrunner/latest/custom/jobStatus');"
    html << "    const data = await res.json();"
    html << "    document.getElementById(\"metrics\").innerHTML = `"
    html << "      <h2>⚙️ Scheduled Job Metrics</h2>"
    html << "      <b>Job enabled:</b> \${data.jobScheduled ? '✅ Yes' : '❌ No'}<br>"
    html << "      <b>Currently running:</b> \${data.isRunning ? '✅ Yes' : '❌ No'}<br>"
    html << "      <b>Last run start:</b> \${data.lastRunStart}<br>"
    html << "      <b>Last run end:</b> \${data.lastRunEnd}<br>"
    html << "      <b>Users processed:</b> \${data.usersProcessed}<br>"
    html << "      <b>Users updated:</b> \${data.usersUpdated}<br>"
    html << "      <b>API calls this run:</b> \${data.apiCalls}<br>"
    html << "      <b>Total errors today:</b> \${data.totalErrorsToday}<br>`;"
    html << "  } catch (e) {"
    html << "    document.getElementById(\"metrics\").innerText = '⚠️ Failed to load metrics.';"
    html << "  }"
    html << "}"
    html << "loadMetrics();"
    html << "setInterval(loadMetrics, 30000);"
    html << "</script>"

    html << "</div></body></html>\n"

    return Response.ok(html.toString()).type("text/html").build()
}

// keep your SSL helper as-is
def disableSSLValidation() {
    def trustAllCerts = [ new X509TrustManager() {
        void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
        void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
        java.security.cert.X509Certificate[] getAcceptedIssuers() { return null }
    }] as TrustManager[]

    def sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, trustAllCerts, new java.security.SecureRandom())
    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
    HttpsURLConnection.setDefaultHostnameVerifier({ hostname, session -> true })
}
