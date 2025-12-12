package com.behaviours.OffboardingBehaviour

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.util.UserUtil
import  java.util.Date.*
def termDate = getFieldByName("Termination Date")
def typeOfSeparation = getFieldByName("Type of Separation")
def termDateVal = termDate.getValue() as Date
def typeOfSeparationselectedOption = typeOfSeparation.getValue() as String
// get todays date
def today = new Date()


if(termDateVal<(today-1)) {
   termDate.setError("You must not enter a date that is before todays date")
} else {
    termDate.clearError()
}

