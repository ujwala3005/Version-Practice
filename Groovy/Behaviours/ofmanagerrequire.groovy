package com.behaviours.OffboardingBehaviour

import com.onresolve.jira.groovy.user.FormField
import com.atlassian.jira.issue.IssueFieldConstants
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.customfields.manager.OptionsManager
import com.onresolve.jira.groovy.user.FieldBehaviours
import com.atlassian.jira.bc.project.component.ProjectComponent
import com.atlassian.jira.user.UserPropertyManager
import com.atlassian.crowd.embedded.api.User
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.UserPropertyManager
import com.onresolve.jira.groovy.user.FormField
import com.atlassian.jira.user.DelegatingApplicationUser
import com.atlassian.jira.user.util.UserUtil
import com.opensymphony.module.propertyset.PropertySet;
import com.atlassian.jira.user.ApplicationUser;
import static com.atlassian.jira.issue.IssueFieldConstants.*

def associatename = getFieldById("customfield_12707")  //Associae Name
def dataOfAssociatebeingoffboarded = getFieldByName("Does Manager require access to Data of Associate being offboarded?")
def dataOfAssociatebeingoffboardedselectedOption = dataOfAssociatebeingoffboarded.getValue() as String
def nameoftheManager = getFieldByName("Name of the Manager")
def nameoftheDelegate = getFieldByName("Name of the Delegate")
def managerorDelegate = getFieldByName("Manager or Delegate?")
def whoprovidedHRBPApproval = getFieldByName("Who provided HR BP Approval?")
nameoftheDelegate.setHidden(true).setRequired(false).setFormValue("")
whoprovidedHRBPApproval.setHidden(true).setRequired(false).setFormValue("")

if(dataOfAssociatebeingoffboardedselectedOption.equalsIgnoreCase("Full Access"))
{
	managerorDelegate.setHidden(false).setRequired(true)
 //   nameoftheManager.setHidden(true).setRequired(false).setFormValue("") 
    
    //code Added
    if(managerorDelegate.getValue() == "Manager") {
    nameoftheManager.setHidden(false).setRequired(true)     
}
else
    nameoftheManager.setHidden(true).setRequired(false).setFormValue("")   
    //End of code
}
else
{
    managerorDelegate.setHidden(true).setRequired(false).setFormValue("Manager") // managerorDelegate.setHidden(true).setRequired(false).setFormValue("")
    nameoftheManager.setHidden(false).setRequired(true)
  
	if(associatename.getValue() != null) {
        //Setting Name of the Manager value         
        String result = associatename.getValue() as String
        
        Class objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade")
        def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass)

        Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade") 
        def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass)

        String attributeUsernamekey = "Email Address"
        String attributeUserKey = "Key"
        def schemaID = 25

        def object = objectFacade.loadObjectBean(result)

        def objectmanagername = objectFacade.loadObjectAttributeBean(object.id, "Manager1")

        if( objectmanagername )
        {
            def objectAttributeValues = objectmanagername.getObjectAttributeValueBeans()
            def objectAttributeValue = objectAttributeValues[0]
            int managerKey = objectAttributeValue.getValue()
            def objectsUser =  objectFacade.loadObjectBean(managerKey)
            
            def objectsUserFinalDetails = objectFacade.loadObjectAttributeBean(objectsUser.id ,"User Name")
            def userN = objectsUserFinalDetails.getObjectAttributeValueBeans()[0].getValue().toString()
            def userObj = ComponentAccessor.getUserManager().getUserByName(userN) as ApplicationUser
            def email = objectFacade.loadObjectAttributeBean(objectsUser.id ,"Email Address").getObjectAttributeValueBeans()[0].getValue()
            if(!userObj && email)
            {
                def users = ComponentAccessor.userSearchService.findUsersByEmail(email)
                userObj = (users==null || users.isEmpty())?null:users[0]                
            }
            if(userObj)
            {
                nameoftheManager.setFormValue(userObj.getUsername()) 
            }
            
        }
     }
}
