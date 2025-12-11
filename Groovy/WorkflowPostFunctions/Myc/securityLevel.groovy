import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.component.ComponentAccessor
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import com.atlassian.jira.issue.security.IssueSecurityLevelManager
import com.atlassian.jira.issue.security.IssueSecuritySchemeManager
import com.atlassian.crowd.embedded.api.Group
import global.GlobalConfigsUnifiedPortal
@WithPlugin('com.riadalabs.jira.plugins.insight')
@PluginModule ObjectFacade objectFacade
@PluginModule ObjectTypeFacade objectTypeFacade
@PluginModule ObjectTypeAttributeFacade objectTypeAttributeFacade
@PluginModule IQLFacade iqlFacade

if(!issue){
    log.warn("No issues Found!!")
    return
}

def reporter = issue.getReporter()?.getEmailAddress()
def groupManager = ComponentAccessor.getGroupManager()

def securityGroups = []
def schemaID = GlobalConfigsUnifiedPortal.MYCOTIVIPLUS_OBJECT_SCHEMA_ID
def issueSecuritySchemeManager = ComponentAccessor.getComponent(IssueSecuritySchemeManager)
def issueSecurityLevelManager = ComponentAccessor.getComponent(IssueSecurityLevelManager)
def businessUnit1 = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("customfield_31603")
def application = issue.getCustomFieldValue("Impacted Application(s)")?.getAt(0)
def repOrgObjects = iqlFacade.findObjectsByIQLAndSchema(schemaID,'"Email" IN (\"' + reporter  + '\")')

def BUObj = iqlFacade.findObjectsByIQLAndSchema(schemaID,'objectType = "MyCotivitiPlus Applications" AND "Application Name" IN ("' + application +'")')?.getAt(0).getAttributeValues("BU1")?.getAt(0)
def repOrgObj = null;
def cmpname = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("customfield_31100")
if(repOrgObjects){
repOrgObjects.each { repObj ->
if(BUObj == repObj.getAttributeValues("Business Unit")?.getAt(0)){
repOrgObj = repObj.getAttributeValues("Company")?.getAt(0)
 def aaa = repOrgObj.getValue()
 String key = "MC-"+ aaa
def bbgroup = iqlFacade.findObjectsByIQLAndSchema(schemaID, "Key = \"" + key + "\"")
// def bbgroup = iqlFacade.findObjectsByIQLAndSchema(schemaID, "Key = 'MC-" + repOrgObj.getValue().toString() + "'")

if(!bbgroup){
    log.warn("repOrgObj not found in schema!!")
    return
}

def secGroup2 = objectFacade.loadObjectAttributeBean(bbgroup[0].getId(), "Security Groups")
def objectAttributeValues123 = secGroup2.getObjectAttributeValueBeans()
log.warn(objectAttributeValues123)
def secschema1 = objectFacade.loadObjectAttributeBean(bbgroup[0].getId(), "Security Level")
def objectAttributeValuesschema1 = secschema1.getObjectAttributeValueBeans()
log.warn(objectAttributeValuesschema1)

for(objectAttributeValue1 in objectAttributeValues123){  

    Group group = groupManager.getGroup(objectAttributeValue1.getValue().toString())
    securityGroups.add(group)
    ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
}
  log.warn(securityGroups)
issue.setCustomFieldValue(businessUnit1, securityGroups);
for(objectAttributeValue2 in objectAttributeValuesschema1){  
    String aaaa = objectAttributeValue2.getValue()
log.warn(securityGroups)
    def schemeFromName = issueSecuritySchemeManager.getSchemeObjects().find { it.name == "SPT Schemas" }
    def schemeFromProject = issueSecuritySchemeManager.getSchemeFor(ComponentAccessor.projectManager.getProjectByCurrentKey("SPT"))
    def securityLvl = issueSecurityLevelManager.getIssueSecurityLevels(schemeFromName.id).find { it ->
        it.name == aaaa
    }?.id
    issue.setSecurityLevelId(securityLvl)
    log.warn(securityLvl)


}
}           
            }
        }
        if(!repOrgObj){
            repOrgObj = repOrgObjects.getAt(0).getAttributeValues("Company").getAt(0)
             def aaa = repOrgObj.getValue()
                 String key = "MC-"+ aaa
            def bbgroup = iqlFacade.findObjectsByIQLAndSchema(schemaID, "Key = \"" + key + "\"")
                    // def bbgroup = iqlFacade.findObjectsByIQLAndSchema(schemaID, "Key = 'MC-" +  repOrgObj.getValue().toString() + "'")
if(!bbgroup){
    log.warn("repOrgObj not found in schema!!")
    return
}
                    
                   def secGroup2 = objectFacade.loadObjectAttributeBean(bbgroup[0].getId(), "Security Groups")
def objectAttributeValues123 = secGroup2.getObjectAttributeValueBeans()

def secschema1 = objectFacade.loadObjectAttributeBean(bbgroup[0].getId(), "Security Level")
def objectAttributeValuesschema1 = secschema1.getObjectAttributeValueBeans()

for(objectAttributeValue1 in objectAttributeValues123){  

    Group group = groupManager.getGroup(objectAttributeValue1.getValue().toString())
    securityGroups.add(group)
    ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
}

issue.setCustomFieldValue(businessUnit1, securityGroups);
for(objectAttributeValue2 in objectAttributeValuesschema1){  
    String aaaa = objectAttributeValue2.getValue()
    def schemeFromName = issueSecuritySchemeManager.getSchemeObjects().find { it.name == "SPT Schemas" }
    def schemeFromProject = issueSecuritySchemeManager.getSchemeFor(ComponentAccessor.projectManager.getProjectByCurrentKey("SPT"))
    def securityLvl = issueSecurityLevelManager.getIssueSecurityLevels(schemeFromName.id).find { it ->
        it.name == aaaa
    }?.id
    issue.setSecurityLevelId(securityLvl)
    log.warn(securityLvl)

}
        }

    

      
