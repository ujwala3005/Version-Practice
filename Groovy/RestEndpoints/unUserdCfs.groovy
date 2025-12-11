import okhttp3.Response
import groovy.json.JsonOutput
import groovy.transform.BaseScript
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import javax.ws.rs.core.MultivaluedMap
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.web.bean.PagerFilter
import groovy.json.JsonSlurper
import java.net.HttpURLConnection
import java.net.URL
import javax.ws.rs.core.Response


@BaseScript CustomEndpointDelegate delegate

UnUsedCustomFields(httpMethod: "GET") { MultivaluedMap queryParams ->

def mutableListCf = []
def zeroIssuesFields =[]
def filteredUnUsedFields = []
def cfId
def cfname

// Replace with actual values
def baseUrl = "https://jiraet-uat.cotiviti.com/rest/api/2/customFields?startAt=0&maxResults=2193"
def email = "divya.kukkadapu@cotiviti.com"
def tokenVal = "OTkzMD"

def urlVal = new URL(baseUrl)
log.warn(urlVal)

HttpURLConnection connection = (HttpURLConnection) urlVal.openConnection()
connection.setRequestMethod("GET")
connection.setRequestProperty("Authorization", "Bearer ${tokenVal}")
connection.setRequestProperty("Accept", "application/json")
connection.setDoOutput(true)
connection.setDoInput(true)

// Read response
def responseText = connection.inputStream.text
//log.warn( "Response Body: $responseText")
//log.warn( "HTTP Status: ${connection.responseCode}")

// Parse JSON
def json = new JsonSlurper().parseText(responseText)
//def values = json.values

def values = json["values"] // safely access the 'values' key
//log.warn(values)

log.warn( "Custom Fields with issuesWithValue = 0 and screensCount = 0:")
values.each { field ->
//log.warn(field)

def issuesWithValue = field.issuesWithValue //?.toString()?.trim()?.toInteger() ?: -1
def screensCount = field.screensCount //?.toString()?.trim()?.toInteger() ?: -1

cfId = field.numericId ?: -1
// log.warn(cfId)
cfname = field.name
//log.warn(issuesWithValue == "0" && screensCount == "0")
if (issuesWithValue == 0 && screensCount == 0) {

    mutableListCf << cfId
    filteredUnUsedFields << field

    // return groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(field))
    //log.warn(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(field)))
}
}

log.warn( "Collected Custom Field IDs: $mutableListCf")
log.warn( "Total Unused Fields: ${mutableListCf.size()}")

connection.disconnect()

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def searchProvider = ComponentAccessor.getComponent(SearchProvider)
//def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchService = ComponentAccessor.getComponent(SearchService)

// Replace with your actual custom field ID or name
//def customField = customFieldManager.getCustomFieldObject("customfield_123")
//def cfList = [13521, 26600, 11719, 11677, 10100, 11361, 11674, 29801, 11647, 11675, 13226, 12202, 19115, 11707, 11735, 11750, 11610, 11684, 11686, 12300, 13684, 11628, 11380, 11703, 11708, 11758, 11683, 11612, 11550, 11563, 11564, 11613, 18905, 11650, 11725, 13202, 16809, 18906, 28100, 26206, 24600, 12600, 10200, 11590, 11687, 28700, 11381, 11702, 11709, 11759, 11704, 11710, 17204, 15867, 11305, 13538, 21901, 11565, 10701, 11636, 11691, 12792, 13549, 17200, 12903, 19606, 10400, 11681, 11715, 13006, 13217, 13399, 11656, 11591, 13902, 10300, 13689, 30101]

//for(cfid : cfList){

for(cfid : mutableListCf){
//log.warn(cfid.class.name)
// Sample JQL query that uses the custom field
def jql = "cf[${cfid}] is not EMPTY" // Replace with actual field name
def fieldId = (Long) cfid
//log.warn(fieldId)
def parsedQuery = jqlQueryParser.parseQuery(jql)
def results = searchService.search(user, parsedQuery, PagerFilter.getUnlimitedFilter())

def fname = customFieldManager.getCustomFieldObject(fieldId)

log.warn(cfid)
// log.warn(results.total)

if (results.total == 0) {
log.warn("No issues are related to the field.")

zeroIssuesFields << [(cfid) : fname]

//zeroIssuesFields << cfId
//zeroIssuesFields << cfname

} else {

log.warn("${results.total} issues are related to the field ${cfid}")
}

}
log.warn(zeroIssuesFields)
log.warn("fields with zero issues :  ${zeroIssuesFields.size()}")

// Convert Data to JSON String
def jsonOutput = JsonOutput.prettyPrint(JsonOutput.toJson(filteredUnUsedFields))

//return jsonOutput
return Response.ok(jsonOutput.toString()).build()

}
