package com.crashlogger.android

interface IPostTask{
    fun postExceptionExecution(report: String, e: Throwable)
}