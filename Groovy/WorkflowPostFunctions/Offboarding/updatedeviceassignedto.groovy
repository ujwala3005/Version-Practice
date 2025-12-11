package com.workflows.offboarding.Create.postfunctions
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder

import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
@WithPlugin('com.riadalabs.jira.plugins.insight')
@PluginModule ObjectFacade objectFacade
@PluginModule ObjectTypeFacade objectTypeFacade
@PluginModule ObjectTypeAttributeFacade objectTypeAttributeFacade
@PluginModule IQLFacade iqlFacade

def cfuserdevice = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Device Assigned to User")[0]
def resultObj = ((ComponentAccessor.getCustomFieldManager().getCustomFieldObject("customfield_11615").getValue(issue)) as ApplicationUser)

if (resultObj == null) {
    return
}
def result = resultObj.getUsername()
log.warn("User : " + result)
//Class objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade")
//def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass)

//Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade")
//def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass)

String attributeUsernamekey = "Assigned To"
def schemaID = 7
def objects = iqlFacade.findObjectsByIQLAndSchema(schemaID, "ObjectType = Laptops/Desktops and \"" + attributeUsernamekey + "\" = \"" + result + "\"")

if (objects.size() > 0) {

    def objectName, objectAttributeValues, objectAttributeValue, valdevice = ""
    for (int i = 0; i < objects.size(); i++) {
        objectName = objectFacade.loadObjectAttributeBean(objects[i].getId(), "Name")

        if (objectName) {
            objectAttributeValues = objectName.getObjectAttributeValueBeans()
            objectAttributeValue = objectAttributeValues[0]

            if (i > 0) {
                valdevice += ', ';
            }
            valdevice += objectAttributeValue.getValue()
        }
    }
    log.warn("val" + valdevice)
    cfuserdevice.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(cfuserdevice), valdevice), new DefaultIssueChangeHolder())
}else{
    log.warn("No record in CMDB - Laptops/Desktops for the User")
}
