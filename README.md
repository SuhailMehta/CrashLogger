# CrashLogger

Crash Logger is android project designed to log crash report of the application which run across all versions of Android. When an application crashes log report is generated with device info.

Installation:-

```gradle
implementation 'com.crashlogger.android:app:1.0.0'
```

How to use this:-
-----------------------
Add CrashLogger in Application

```kotlin
class MyApplication : Application(), IPostTask {

    override fun postExceptionExecution(report: String, e: Throwable) {
        Log.d(CrashLogger.TAG, report)
        /*
        * You can send report to the server or kill application here
        */
    }
    
    override fun onCreate() {
        super.onCreate()
        // Make sure the CrashLogger.init() line is after all other 3rd-party SDKs that set an UncaughtExceptionHandler
        CrashLogger.init(this)

    }
}
```
IPostTask postExceptionExecution will be called after evey crash with generated report and Exception. You can send report to your own server or can configure mail attachment. 

You can add username, userIdentity and userEmail with the report

```kotlin
CrashLogger.setUserEmail("xyz@w.com")
CrashLogger.setUserIdentifier("123456")
CrashLogger.setUserName("xyz")
```
