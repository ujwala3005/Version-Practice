import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.transform.BaseScript
import javax.ws.rs.core.Response
import groovy.json.JsonOutput
import com.atlassian.jira.web.ExecutingHttpRequest
import javax.servlet.http.HttpServletRequest

@BaseScript CustomEndpointDelegate delegate

// Simple allowed-users endpoint for the dashboard to fetch pre-authorized emails
FinalcasepointAllowed(httpMethod: "GET") { params ->
    HttpServletRequest request = ExecutingHttpRequest.get()

    // TODO: add approved mails here
    def allowed = [
        "user1@domain.com",
        "user2@domain.com",
        "user3@domain.com",
        "user4@domain.com"
    ]

    // ===== CORS HEADERS =====
    String clientOrigin = request ? request.getHeader("Origin") : null
    String allowOrigin = (clientOrigin && !clientOrigin.isEmpty()) ? clientOrigin : "*"

    def payload = [ allowedUsers: allowed ]
    return Response.ok(JsonOutput.toJson(payload))
        .type("application/json")
        .header("Access-Control-Allow-Origin", allowOrigin)
        .header("Access-Control-Allow-Credentials", "true")
        .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        .header("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization, X-Requested-With")
        .build()
}
