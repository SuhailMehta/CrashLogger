package com.crashlogger

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.crashlogger.model.DeviceInfo
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*


class CrashLogger private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {

    companion object {

        private var userName: String? = null
        private var userID: String? = null
        private var userEmail: String? = null

        val TAG: String? = CrashLogger::class.java.simpleName

        @Volatile
        private var INSTANCE: CrashLogger? = null

        /**
         * Returns the instance of CrashLogger
         *
         * @param context
         * @return CrashLogger
         */
        fun init(context: Context): CrashLogger? {
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: CrashLogger(context).also { INSTANCE = it }
            }
            Thread.setDefaultUncaughtExceptionHandler(INSTANCE)
            return INSTANCE
        }

        /**
         * Set the User name attach to attach with crash
         * @param userName
         * (optional)
         */
        fun setUserName(userName: String) {
            this.userName = userName
        }

        /**
         * Set the User id attach to attach with crash
         * @param userID
         * (optional)
         */
        fun setUserIdentifier(userID: String) {
            this.userID = userID
        }

        /**
         * Set the User email attach to attach with crash
         * @param userEmail
         * (optional)
         */
        fun setUserEmail(userEmail: String) {
            this.userEmail = userEmail
        }

    }

    /**
     * Find the available internal memory
     *
     * @return available internal memory
     */
    private fun getAvailableInternalMemory(): Long {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            stat.blockSizeLong
        } else {
            stat.blockSize.toLong()
        }
        val availableBlocks = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            stat.availableBlocksLong
        } else {
            stat.blockSize.toLong()
        }
        return availableBlocks * blockSize
    }

    /**
     * Find the total internal memory
     *
     * @return total internal memory
     */
    private fun getTotalInternalMemory(): Long {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            stat.blockSizeLong
        } else {
            stat.blockSize.toLong()
        }
        val totalBlocks = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            stat.blockCountLong
        } else {
            stat.blockCount.toLong()
        }
        return totalBlocks * blockSize
    }

    /**
     * It gets the android phone & application details
     *
     * @param context
     */
    private fun getDeviceInformation(context: Context): DeviceInfo? {
        val pm = context.packageManager
        return try {
            val pi: PackageInfo = pm.getPackageInfo(context.packageName, 0)

            DeviceInfo(pi.versionName, pi.packageName, context.filesDir.absolutePath,
                    Build.MODEL, Build.VERSION.RELEASE, Build.BOARD,
                    Build.BRAND, Build.DEVICE, Build.DISPLAY,
                    Build.FINGERPRINT, Build.HOST, Build.ID, Build.MANUFACTURER,
                    Build.MODEL, Build.PRODUCT, Build.TAGS,
                    Build.TIME, Build.TYPE, Build.VERSION.SDK_INT, Build.USER)

        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, e.message)
            null
        }
    }

    /**
     * It creates a formatted string containing device and phone
     * information.
     *
     * @ return String
     */
    private fun formattedInfoString(deviceInfo: DeviceInfo?): String {
        return """
        Version : ${deviceInfo?.versionName}
        Package : ${deviceInfo?.packageName}
        FilePath : ${deviceInfo?.filePath}
        Phone Model${deviceInfo?.phoneModel}
        Android Version : ${deviceInfo?.androidVersion}
        Board : ${deviceInfo?.board}
        Brand : ${deviceInfo?.brand}
        Device : ${deviceInfo?.device}
        Display : ${deviceInfo?.display}
        Finger Print : ${deviceInfo?.fingerPrint}
        Host : ${deviceInfo?.host}
        ID : ${deviceInfo?.ID}
        Model : ${deviceInfo?.model}
        Product : ${deviceInfo?.product}
        Tags : ${deviceInfo?.tags}
        Time : ${deviceInfo?.time}
        Type : ${deviceInfo?.type}
        User : ${deviceInfo?.user}
        SDK_VERSION : ${deviceInfo?.sdkInfo}
        Total Internal memory :  + ${getTotalInternalMemory()}
        Available Internal memory : ${getAvailableInternalMemory()}
        """
    }

    override fun uncaughtException(t: Thread?, exception: Throwable?) {

        val deviceInfo = getDeviceInformation(context)
        val formattedString = formattedInfoString(deviceInfo)

        var report = ""

        val curDate = Date()
        report += "Error Report collected on : " + curDate.toString()
        report += "\n"
        report += "Information's :"
        report += "\n"
        if (null != userName) {
            report += "Username : $userName"
            report += "\n"
        }
        if (null != userID) {
            report += "UserId : $userID"
            report += "\n"
        }
        if (null != userEmail) {
            report += "UserEmail : $userEmail"
            report += "\n"
        }
        report += "\n"
        report += "=============="
        report += "\n"
        report += "\n"

        report += formattedString

        report += "\n\n"
        report += "Stack : \n"
        report += "======= \n"

        val result = StringWriter()
        val printWriter = PrintWriter(result)
        exception?.printStackTrace(printWriter)
        val stacktrace = result.toString()
        report += stacktrace

        // If the exception was thrown in a background thread
        var cause = exception?.cause
        if (cause != null) {
            report += "\n"
            report += "Cause : \n"
            report += "======= \n"
        }

        // find the cause of the crash
        while (cause != null) {
            cause!!.printStackTrace(printWriter)
            report += result.toString()
            cause = cause!!.cause
        }

        printWriter.close()
        report += "****  End of current Report ***"

        if(context is IPostTask){
            context.postExceptionExecution(report, exception!!)
        }
    }
}