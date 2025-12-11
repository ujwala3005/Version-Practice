import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.crowd.embedded.api.Group
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import global.GlobalConfigsUnifiedPortal
@WithPlugin('com.riadalabs.jira.plugins.insight')
@PluginModule ObjectFacade objectFacade 
@PluginModule ObjectTypeFacade objectTypeFacade
@PluginModule ObjectTypeAttributeFacade objectTypeAttributeFacade
@PluginModule IQLFacade iqlFacade

def issueManager = ComponentAccessor.getIssueManager()
def groupManager = ComponentAccessor.getGroupManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def schemaID = GlobalConfigsUnifiedPortal.MYCOTIVIPLUS_OBJECT_SCHEMA_ID
def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
if (!issue) {
    log.warn("Issue object is null — exiting.")
    return
}

def reporterEmail = issue.getReporter()?.getEmailAddress()
log.warn("Processing issue: ${issue.key}")

def impactedAppliCF = customFieldManager.getCustomFieldObjectByName("Impacted Application(s)")
def assignedGroupCF = customFieldManager.getCustomFieldObjectByName("Internal Assignment Group")  
def insightAssignmentGroupCF = customFieldManager.getCustomFieldObjectByName("Assignment Group")

def impactedApplication = issue.getCustomFieldValue(impactedAppliCF)?.getAt(0)?.toString()
if (!impactedApplication) {
    log.warn("No impacted application found.")
    return
}

log.warn("Impacted Application: ${impactedApplication}")

def BUObj = iqlFacade.findObjectsByIQLAndSchema(schemaID, 
    'objectType = "MyCotivitiPlus Applications" AND "Application Name" = "' + impactedApplication + '"'
)?.getAt(0)?.getAttributeValues("BU1")?.getAt(0)

if (!BUObj) {
    log.warn("BU object not found for application: ${impactedApplication}")
    return
}

log.warn("BU Object: ${BUObj}")

def repOrgObjects = iqlFacade.findObjectsByIQLAndSchema(schemaID, '"Email" = "' + reporterEmail + '"')

def targetGroup = null
def targetInsightGroupName = null

if (repOrgObjects) {
    for (repObj in repOrgObjects) {
        def repBU = repObj.getAttributeValues("Business Unit")?.getAt(0)
     if (BUObj == repBU) {
            def companyObj = repObj.getAttributeValues("Company")?.getAt(0)
            def companyKey = "MC-" + companyObj?.getValue()
            log.warn("Reporter Company Key: ${companyKey}")

            def compcmdb = iqlFacade.findObjectsByIQLAndSchema(schemaID, '"key" = "' + companyKey + '"')
            log.warn(compcmdb)
             def label = compcmdb ? compcmdb[0]?.label : null
            log.warn(label)
            if (["Cigna-QIS", "Cigna - Medicare-QIS"].contains(label)) {
                targetGroup = "OPs PM"
                targetInsightGroupName = "OPs PM"
                break
            }
               else if(["Test-HGB","HGB-QIS"].contains(label))
            {
               
                 targetGroup = "HGB OPM"
                targetInsightGroupName = "HGB OPM"
                break

            }
                      else if(["Test-TCH"].contains(label))
            {
                 targetGroup = "TCHSupport"
                targetInsightGroupName = "TCHSupport"
                break

            }
        }
    }
}


if (!targetGroup) {
    targetGroup = "ECOSL2"
    targetInsightGroupName = "ECOSL2"
    log.warn("Defaulting to ECOSL2")
}

if (targetGroup && targetInsightGroupName) {
    def insightGroupObjects = iqlFacade.findObjectsByIQLAndSchema(schemaID, "\"Myc Group Name\" = \"${targetInsightGroupName}\"")
    def group = groupManager.getGroup(targetGroup)

    if (insightGroupObjects && group) {
        issue.setCustomFieldValue(insightAssignmentGroupCF, insightGroupObjects)
        issue.setCustomFieldValue(assignedGroupCF, [group])
      //  issueManager.updateIssue(null, issue, EventDispatchOption.ISSUE_UPDATED, false)
      ComponentAccessor.getIssueManager().updateIssue(currentUser, issue, EventDispatchOption.ISSUE_UPDATED, false)
        log.warn("Successfully updated assignment fields to group: ${targetGroup}")
    } else {
        log.warn("Failed to resolve Jira or Insight group: ${targetGroup}")
    }
} else {
    log.warn("No group assignment could be determined.")
}
