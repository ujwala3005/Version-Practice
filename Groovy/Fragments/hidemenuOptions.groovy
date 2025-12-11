//com.kostebekteknoloji.plugins.jira.jira-comment-toolboxdelete-comment
//com.kostebekteknoloji.plugins.jira.jira-comment-toolbox:update-comment

def project = issue.projectObject
return !project.key in ["IT"]
