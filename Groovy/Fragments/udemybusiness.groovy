//servicedesk.portal.header
import com.onresolve.scriptrunner.canned.util.OutputFormatter

def scriptPath = "/mnt/jira-shared-home/scripts/cute/UdemyBusiness.groovy"

// Load and evaluate one.groovy
def outputHtml = new GroovyShell().evaluate(new File(scriptPath).text)

// Write its output to the panel
writer.write(outputHtml)
