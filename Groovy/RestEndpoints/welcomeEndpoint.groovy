import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

@BaseScript CustomEndpointDelegate delegate

welcomeEndpoint(
    httpMethod: "GET"
) { MultivaluedMap queryParams, String body ->

    // Extract query parameters
    String email = queryParams.getFirst("emailAddress")
    String status = queryParams.getFirst("statusCode")

    // Convert statusCode to int
    int code
    try {
        code = status.toInteger()
    } catch (Exception e) {
        code = 0
    }

    // Decide message
    def message
    if (code == 200) {
        message = "Welcome, ${email}!"
    } else {
        message = "Connection failed for ${email}"
    }

    // Build JSON response
    def json = new JsonBuilder([email: email, statusCode: code, message: message]).toString()
    return Response.ok(json).build()
}
