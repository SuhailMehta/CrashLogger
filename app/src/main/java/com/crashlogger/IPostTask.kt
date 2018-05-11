package com.crashlogger

interface IPostTask{
    fun postExceptionExecution(report: String, e: Throwable)
}