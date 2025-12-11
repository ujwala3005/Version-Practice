import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.transform.BaseScript
import javax.ws.rs.core.Response
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.web.ExecutingHttpRequest
import javax.servlet.http.HttpServletRequest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.regex.Pattern
import java.nio.charset.StandardCharsets
import java.io.RandomAccessFile

@BaseScript CustomEndpointDelegate delegate

FinalcasepointMetrics(httpMethod: "GET") { queryParams ->

    // 1. GET REQUEST OBJECT (Safe method for Jira 8/9+)
    HttpServletRequest request = ExecutingHttpRequest.get()

    // ===== Configuration =====
    final String LOG_FILE_PATH = "/mnt/jira-shared-home/scripts/casepoint/dash/logs.groovy"
    final long MAX_BYTES = 5L * 1024L * 1024L // 5 MB max read
    def logger = org.apache.log4j.Logger.getLogger("casepoint.metrics")

    // ---- Helper: safe file read ----
    def safeReadLines = { String path ->
        def f = new File(path)
        if (!f.exists()) {
            logger.warn("Log file not found: ${path}")
            return []
        }
        if (!f.canRead()) {
            logger.warn("Cannot read log file: ${path}")
            return []
        }
        if (f.length() > MAX_BYTES) {
            def raf = new RandomAccessFile(f, "r")
            try {
                long start = Math.max(0L, raf.length() - MAX_BYTES)
                raf.seek(start)
                def bytes = new byte[(int)(raf.length() - start)]
                raf.readFully(bytes)
                return new String(bytes, StandardCharsets.UTF_8).readLines()
            } finally {
                raf.close()
            }
        } else {
            return f.getText("UTF-8").readLines()
        }
    }

    // ---- Load lines ----
    def lines = []
    try {
        lines = safeReadLines(LOG_FILE_PATH)
    } catch (ex) {
        logger.error("Failed to read logs: ${ex.message}", ex)
        // Error handling with CORS
        String clientOrigin = request ? request.getHeader("Origin") : null
        String allowOrigin = (clientOrigin && !clientOrigin.isEmpty()) ? clientOrigin : "*"
        return Response.status(500).entity("Failed to read logs")
            .header("Access-Control-Allow-Origin", allowOrigin)
            .header("Access-Control-Allow-Credentials", "true")
            .build()
    }

    // ---- Patterns ----
    def tsPattern = Pattern.compile('^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:,\\d+)?)')
    def compPattern = Pattern.compile('\\[([^\\]]+)\\]')
    def holdPattern = Pattern.compile('HoldName\\s*:\\s*([^|\\s]+)\\s*\\|\\s*HoldStatus\\s*:\\s*([^|]+)\\s*\\|\\s*Custodian Email\\s*:\\s*([^|]+)\\s*\\|\\s*Employment Status\\s*:\\s*([^|]+)\\s*\\|\\s*Termination Date\\s*:\\s*(\\S+)?')
    def objectQueryPattern = Pattern.compile('objectType\\s*=\\s*([^\\n]+)', Pattern.CASE_INSENSITIVE)
    def execPattern = Pattern.compile('Execution took\\s*(\\d+)\\s*ms', Pattern.CASE_INSENSITIVE)
    def userAddPattern = Pattern.compile('User Add\\s*:\\s*([^|\\s]+)', Pattern.CASE_INSENSITIVE)
    def userRemovePattern = Pattern.compile('User Remove\\s*:\\s*([^|\\s]+)', Pattern.CASE_INSENSITIVE)
    def simpleNamePattern = Pattern.compile('^[^\\]]*\\]:\\s*(.+)$')
    def emailPattern = Pattern.compile('[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}')

    def parseTimestamp = { String s ->
        if (!s) return null
        def t = s.replace(',', '.')
        try {
            if (!t.endsWith('Z')) t = t + 'Z'
            return Instant.parse(t)
        } catch (DateTimeParseException ex) {
            try { return Instant.parse(t.substring(0,19) + 'Z') } catch (e) { return null }
        }
    }

    // ---- Parsing Logic ----
    def events = []
    def current = null

    lines.eachWithIndex { rawLine, idx ->
        if (!rawLine?.trim()) return

        def mTs = tsPattern.matcher(rawLine)
        def ts = mTs.find() ? parseTimestamp(mTs.group(1)) : null

        if (ts) {
            if (current) events << current
            current = [
                rawLines: [rawLine],
                timestamp: ts,
                level: null,
                component: null,
                messageType: 'OTHER',
                holdName: null,
                holdStatus: null,
                custodianEmail: null,
                employmentStatus: null,
                terminationDate: null,
                objectQuery: null,
                userFullName: null,
                userEmail: null,
                executionMs: null,
                startLineIndex: idx
            ]
            def parts = rawLine.split('\\s+', 4)
            if (parts.length >= 3) current.level = parts[2]
            def cm = compPattern.matcher(rawLine)
            if (cm.find()) current.component = cm.group(1)
        } else {
            if (!current) return
            current.rawLines << rawLine
        }

        if (!current) return

        try {
            def em = emailPattern.matcher(rawLine)
            if (em.find()) {
                def found = em.group(0)?.trim()
                if (found && !current.userEmail) current.userEmail = found
            }
        } catch (ignore) {}

        def hm = holdPattern.matcher(rawLine)
        if (hm.find()) {
            current.holdName = hm.group(1)?.trim()
            current.holdStatus = hm.group(2)?.trim()
            current.custodianEmail = hm.group(3)?.trim()
            current.employmentStatus = hm.group(4)?.trim()
            current.terminationDate = hm.group(5)?.trim()
            if (current.custodianEmail && !current.userEmail) current.userEmail = current.custodianEmail
            current.messageType = 'HOLD_INFO'
        }

        def om = objectQueryPattern.matcher(rawLine)
        if (om.find()) {
            current.objectQuery = om.group(0).trim()
            def emailExtract = (current.objectQuery =~ /"Email Address"\s*IN\s*\(([^)]+)\)/)
            if (emailExtract.find()) {
                def emails = emailExtract.group(1)
                def first = (emails.split(',')[0] ?: '').replaceAll(/["\s]/, '')
                if (first && !current.userEmail && emailPattern.matcher(first).find()) current.userEmail = first
            }
            if (current.messageType == 'OTHER') current.messageType = 'OBJECT_QUERY'
        }

        def am = userAddPattern.matcher(rawLine)
        if (am.find()) {
            current.messageType = 'USER_ADD'
            def candidate = am.group(1)?.trim()
            if (candidate && !current.userEmail && emailPattern.matcher(candidate).find()) current.userEmail = candidate
            else if (candidate && !current.userEmail) current.userFullName = candidate
        }
        def rm = userRemovePattern.matcher(rawLine)
        if (rm.find()) {
            current.messageType = 'USER_REMOVE'
            def candidate = rm.group(1)?.trim()
            if (candidate && !current.userEmail && emailPattern.matcher(candidate).find()) current.userEmail = candidate
            else if (candidate && !current.userEmail) current.userFullName = candidate
        }

        def exm = execPattern.matcher(rawLine)
        if (exm.find()) {
            try { current.executionMs = Integer.parseInt(exm.group(1)) } catch (e) { current.executionMs = null }
            current.messageType = 'EXECUTION_TIME'
        }

        def un = simpleNamePattern.matcher(rawLine)
        if (un.find()) {
            def nm = un.group(1)?.trim()
            if (nm && nm.contains(' ') && !nm.contains('@') && !nm.toLowerCase().contains('execution') && !nm.toLowerCase().contains('ms') && !nm.matches('.*\\d{2,}.*ms.*')) {
                current.userFullName = nm
                if (current.messageType == 'OTHER') current.messageType = 'USER_NAME'
            }
        }
        if (!ts && rawLine.trim() && rawLine.trim().contains(' ') && !rawLine.contains(':') && !rawLine.contains('@') && !rawLine.toLowerCase().contains('execution') && !rawLine.toLowerCase().contains(' ms') && !current.userFullName) {
            current.userFullName = rawLine.trim()
            if (!current.messageType || current.messageType == 'OTHER') current.messageType = 'USER_NAME'
        }
    }
    if (current) events << current

    // ---- Email extraction (neighbor/context) ----
    events.each { e ->
        if ((!e.userEmail || e.userEmail == '') && e.rawLines) {
            try {
                def joined = e.rawLines.join(' ')
                def em = emailPattern.matcher(joined)
                if (em.find()) {
                    e.userEmail = em.group(0)?.trim()
                } else {
                    def jm = (joined =~ /["']email["']\s*[:=]\s*["']([^"']+)["']/)
                    if (jm.find()) {
                        def cand = jm.group(1)?.trim()
                        if (cand && emailPattern.matcher(cand).find()) e.userEmail = cand
                    } else {
                        def km = (joined =~ /email\s*[:=]\s*([A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,})/)
                        if (km.find()) e.userEmail = km.group(1)?.trim()
                    }
                }
            } catch (ignore) {}
        }
        if ((!e.userEmail || e.userEmail == '') && e.userFullName && e.userFullName.contains('@')) {
            def cand = e.userFullName.trim()
            if (emailPattern.matcher(cand).find()) e.userEmail = cand
        }
    }
    try {
        events.each { e ->
            if ((!e.userEmail || e.userEmail == '') && e.startLineIndex != null) {
                def start = Math.max(0, e.startLineIndex - 3)
                def end = Math.min(lines.size() - 1, e.startLineIndex + 3)
                for (int i = start; i <= end; i++) {
                    try {
                        def ln = lines[i]
                        if (!ln) continue
                        def m = emailPattern.matcher(ln)
                        if (m.find()) {
                            def found = m.group(0)?.trim()
                            if (found) { e.userEmail = found; break }
                        }
                    } catch (ignoreInner) {}
                }
            }
        }
    } catch (ignore) { logger.warn("Neighbor scan failed: ${ignore?.message}") }

    // ---- Aggregation ----
    def byDayUsers = [:].withDefault { new LinkedHashSet() }
    def churnEvents = []
    def execSamples = []
    def slowEntries = []
    def recentRuns = []

    events.each { e ->
        def day = e.timestamp?.atZone(ZoneOffset.UTC)?.toLocalDate()?.toString() ?: 'unknown'
        def userEmailVal = (e.userEmail?.trim()) ?: null
        def userFullNameVal = (e.userFullName?.trim()) ?: null
        def userKeyForSet = userEmailVal ?: userFullNameVal
        if (userKeyForSet) byDayUsers[day].add(userKeyForSet)

        if (e.messageType == 'USER_ADD' || (e.rawLines.toString().toUpperCase().contains("ADD") && e.rawLines.toString().toUpperCase().contains("USER"))) {
            churnEvents << [day: day, type: 'ADD', userEmail: userEmailVal, userFullName: userFullNameVal]
        }
        if (e.messageType == 'USER_REMOVE' || (e.rawLines.toString().toUpperCase().contains("REMOVE") && e.rawLines.toString().toUpperCase().contains("USER"))) {
            churnEvents << [day: day, type: 'REMOVE', userEmail: userEmailVal, userFullName: userFullNameVal]
        }

        if (e.executionMs != null) {
            execSamples << e.executionMs
            slowEntries << [timestamp: e.timestamp.toString(), component: e.component, userFullName: userFullNameVal, userEmail: userEmailVal, executionMs: e.executionMs, rawLines: e.rawLines.take(5)]
            recentRuns << [startTime: e.timestamp.toString(), durationSeconds: (e.executionMs / 1000.0), executionMs: e.executionMs, messageType: e.messageType]
        }
    }

    def dailyActive = byDayUsers.collect { k, set -> [day: k, activeUsers: set.size()] }.sort { it.day }
    def avgExec = execSamples ? (execSamples.sum() / execSamples.size()) as double : null
    def p95Exec = null
    if (execSamples) {
        def sorted = execSamples.sort()
        def idx = Math.ceil(0.95 * sorted.size()).toInteger() - 1
        idx = Math.max(Math.min(idx, sorted.size()-1), 0)
        p95Exec = sorted[idx]
    }

    slowEntries = slowEntries.sort { -it.executionMs }
    try { recentRuns = recentRuns.sort { -Instant.parse(it.startTime).toEpochMilli() } } catch (ex) {}

    def limit = 50
    slowEntries = slowEntries.size() > limit ? slowEntries[0..<limit] : slowEntries
    recentRuns = recentRuns.size() > limit ? recentRuns[0..<limit] : recentRuns

    def lastDay = dailyActive ? dailyActive[-1].day : null
    def activeUsersCount = lastDay ? dailyActive[-1].activeUsers : (byDayUsers.values().sum { it.size() } ?: 0)

    def payload = [
        generatedAt: Instant.now().toString(),
        sourceSummary: [
            totalParsedEvents: events.size(),
            executionSamples: execSamples.size(),
            churnAdds: churnEvents.count { it.type == 'ADD' },
            churnRemoves: churnEvents.count { it.type == 'REMOVE' }
        ],
        activeUsers: activeUsersCount,
        lastDay: lastDay,
        dailyActive: dailyActive,
        churnEvents: churnEvents,
        executionStats: [
            avgExecutionMs: avgExec ? Math.round(avgExec) : null,
            p95ExecutionMs: p95Exec
        ],
        slowEntries: slowEntries,
        recentRuns: recentRuns
    ]

    // ===== CORS HEADERS =====
    String clientOrigin = request ? request.getHeader("Origin") : null
    String allowOrigin = (clientOrigin && !clientOrigin.isEmpty()) ? clientOrigin : "*"

    return Response.ok(JsonOutput.toJson(payload))
        .type("application/json")
        .header("Access-Control-Allow-Origin", allowOrigin)
        .header("Access-Control-Allow-Credentials", "true")
        .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        .header("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization, X-Requested-With")
        .build()
}
