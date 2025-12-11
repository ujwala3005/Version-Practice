package com.workflows.Template.Create.Validator

def psd = cfValues["Planned Start Date"]
def ped = cfValues["Planned End Date"]

return ped.after(psd)
