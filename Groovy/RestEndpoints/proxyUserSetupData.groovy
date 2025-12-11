import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate

import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import com.atlassian.jira.component.ComponentAccessor

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atlassian.sal.api.pluginsettings.PluginSettings

import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin

@WithPlugin('com.atlassian.sal.jira')
@PluginModule
PluginSettingsFactory pluginSettingsFactory;

@BaseScript CustomEndpointDelegate delegate

proxyUserSetupData( 
    httpMethod: "POST"
) { MultivaluedMap queryParams, String body ->
    
    log.info(body)
    
    def settingKey = "proxy-user-setup"
    
    PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey("proxy-user")
	pluginSettings.put(settingKey, body)
    
    return Response.ok(body).build()
}
