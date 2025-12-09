// ScriptRunner Groovy — improved userName extraction + email-derived fallback
import javax.servlet.http.HttpServletRequest
import com.atlassian.jira.component.ComponentAccessor
import groovy.json.JsonOutput
import groovy.transform.BaseScript
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import com.atlassian.jira.web.ExecutingHttpRequest

@BaseScript CustomEndpointDelegate delegate

allnew(httpMethod: "GET") { MultivaluedMap queryParams, String body ->

    HttpServletRequest request = ExecutingHttpRequest.get()

    // ==== CONFIG - change path if needed ====
    String filePath = "/mnt/jira-shared-home/scripts/casepoint/dash/custom-job-log.groovy"
    // ========================================

    def f = new File(filePath)
    if (!f.exists()) {
        def err = [ error: "Log file not found", path: filePath ]
        return Response.status(Response.Status.NOT_FOUND).entity(JsonOutput.toJson(err)).build()
    }

    def text = f.text

    // split into raw blocks by separator lines (3+ equals)
    def rawBlocks = text.split(/(?m)^[=]{3,}.*$\r?\n?/).collect { it?.trim() }.findAll { it }

    // helper regexes
    def rJobRunOnly = ~/(?mi)^\s*Job\s+Run:\s*.+\s*$/
    def rJobRun = ~/(?mi)Job\s+Run:\s*(.+)/
    def rExecMs = ~/(?mi)Execution\s+took\s*([0-9]+)\s*ms/

    // First pass: merge any timestamp-only block into the next block
    def merged = []
    for (int i = 0; i < rawBlocks.size(); i++) {
        def blk = rawBlocks[i]
        if (blk == null) continue
        if ((blk ==~ rJobRunOnly) && (i + 1) < rawBlocks.size()) {
            rawBlocks[i+1] = blk.trim() + "\n" + rawBlocks[i+1]
            continue
        } else if ((blk ==~ rJobRunOnly) && (i + 1) >= rawBlocks.size()) {
            continue
        } else {
            merged << blk
        }
    }

    def results = []

    merged.each { block ->
        if (!block || !block.trim()) return

        // quick check: if block contains no useful tokens, drop it
        if (!(block =~ /:/) && !(block =~ /\|/)) return

        def map = [
            timestamp: null,
            connection: null,
            holdName: null,
            holdStatus: null,
            custodianEmail: null,
            employmentStatus: null,
            terminationDate: null,
            iql: null,
            userName: null,
            executionMs: null,
            raw: block
        ]

        // extract Job Run timestamp and execution ms
        def m = (block =~ rJobRun)
        if (m) map.timestamp = m[0][1].trim()
        m = (block =~ rExecMs)
        if (m) map.executionMs = (m[0][1] as Long)

        // Tokenize block by lines and '|' separators
        def lines = block.split(/\r?\n/).collect { it?.trim() }.findAll { it }
        def tokens = []
        lines.each { ln ->
            tokens.addAll( ln.split(/\s*\|\s*/) as List )
        }

        // Map tokens to fields using first ':' occurrence
        tokens.each { tok ->
            if (!tok) return
            if (!tok.contains(':')) {
                def t = tok.trim()
                if (t =~ /(?i)^Connection\s+/) {
                    def parts = t.split(/\s+/,2)
                    map.connection = (parts.length>1 ? parts[1].trim() : null)
                }
                return
            }

            def idx = tok.indexOf(':')
            if (idx < 0) return
            def rawKey = tok.substring(0, idx).trim()
            def rawVal = tok.substring(idx + 1).trim()
            def key = rawKey.toLowerCase().replaceAll(/\s+/, ' ').trim()

            switch(key) {
                case 'connection': map.connection = rawVal; break
                case 'holdname': map.holdName = rawVal; break
                case 'holdstatus': map.holdStatus = rawVal; break
                case 'custodian email':
                case 'custodianemail': map.custodianEmail = rawVal.toLowerCase(); break
                case 'employment status':
                case 'employmentstatus': map.employmentStatus = rawVal; break
                case 'termination date':
                case 'terminationdate': map.terminationDate = rawVal; break
                case 'iql': map.iql = rawVal; break
                case 'username':
                case 'user name': map.userName = rawVal; break
                default: break
            }
        }

        // ===== aggressive userName extraction =====
        if (!map.userName) {
            // 1) common separators: :, =, -
            def userNamePatterns = [
                /(?mi)(?:User\s*Name|UserName|username)\s*[:=\-]\s*["']?\s*([^"\|\r\n]+?)\s*["']?(?=\s*(?:\||Execution|$))/,
                /(?mi)(?:User\s*Name|UserName|username)\s*(?:is)\s*["']?\s*([^"\|\r\n]+?)\s*["']?(?=\s*(?:\||Execution|$))/,
                // fallback: key followed by value until pipe or end
                /(?mi)(?:User\s*Name|UserName|username)\s*[:=\-]?\s*["']?\s*([^|\r\n]+?)\s*["']?(?=\s*(?:\||\r?\n|$))/
            ]
            for (p in userNamePatterns) {
                def mm = (block =~ p)
                if (mm) {
                    map.userName = mm[0][1].trim().replaceAll(/^"|"$/, '').replaceAll(/[,\;]$/, '').trim()
                    break
                }
            }
        }

        // 2) if still not found, try to infer from IQL (email inside IQL)
        if (!map.userName && map.iql) {
            def emailMatch = (map.iql =~ /([A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,})/)
            if (emailMatch) {
                def e = emailMatch[0][1].toLowerCase()
                def local = e.split('@')[0]
                // convert dots/underscores to spaces and titlecase
                def name = local.replaceAll(/[._]/, ' ').split(/\s+/).collect{ it.capitalize() }.join(' ')
                if (name) map.userName = name
            }
        }

        // 3) if still not found, try to infer from custodianEmail (localpart)
        if (!map.userName && map.custodianEmail) {
            def e = map.custodianEmail.toLowerCase()
            def local = e.split('@')[0]
            def name = local.replaceAll(/[._]/, ' ').split(/\s+/).collect{ it.capitalize() }.join(' ')
            if (name) map.userName = name
        }

        // 4) final brute-force: search for typical capitalized name patterns if block contains 2 words with capital first letters
        if (!map.userName) {
            def brute = (block =~ /([A-Z][a-z]+(?:\s+[A-Z][a-z]+){1,2})/)
            if (brute) {
                // pick the first reasonable-looking match that's not 'Job Run' etc.
                brute.each { b ->
                    def cand = b[1]
                    if (!(cand =~ /(?i)Job\s+Run/) && cand.size() > 3) {
                        map.userName = cand.trim()
                        return
                    }
                }
            }
        }

        // normalize username if present
        if (map.userName) {
            map.userName = map.userName.replaceAll(/^"|"$/, '').replaceAll(/[,\;]$/, '').trim()
        }

        // final filter: require at least one meaningful field besides timestamp/raw
        if (!map.holdName && !map.custodianEmail && !map.userName && !map.iql && !map.holdStatus && !map.employmentStatus) {
            return
        }

        results << map
    }

        // ===== CORS HEADERS =====
    String clientOrigin = request ? request.getHeader("Origin") : null
    String allowOrigin = (clientOrigin && !clientOrigin.isEmpty()) ? clientOrigin : "*"

    // Return JSON and also include a note about heuristics (optional)
    def out = [ items: results, note: "username extraction used tokenization + multiple fallbacks (regex, IQL/email, brute-force)" ]


    return Response.ok(JsonOutput.prettyPrint(JsonOutput.toJson(out)))
        .type("application/json")
        .header("Access-Control-Allow-Origin", allowOrigin)
        .header("Access-Control-Allow-Credentials", "true")
        .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        .header("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization, X-Requested-With")
        .build()
    //return Response.ok(JsonOutput.prettyPrint(JsonOutput.toJson(out))).build()
}
