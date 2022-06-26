package com.shocker.hideapk.hide

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.shocker.hideapk.BuildConfig.APPLICATION_ID
import com.shocker.hideapk.signing.JarMap
import com.shocker.hideapk.signing.SignApk
import com.shocker.hideapk.utils.AXML
import com.shocker.hideapk.utils.Keygen
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.internal.UiThreadHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.SecureRandom

object HideAPK {

    private const val ALPHA = "abcdefghijklmnopqrstuvwxyz"
    private const val ALPHADOTS = "$ALPHA....."
    private const val ANDROID_MANIFEST = "AndroidManifest.xml"



    private fun genPackageName(): String {
        val random = SecureRandom()
        val len = 5 + random.nextInt(15)
        val builder = StringBuilder(len)
        var next: Char
        var prev = 0.toChar()
        for (i in 0 until len) {
            next = if (prev == '.' || i == 0 || i == len - 1) {
                ALPHA[random.nextInt(ALPHA.length)]
            } else {
                ALPHADOTS[random.nextInt(ALPHADOTS.length)]
            }
            builder.append(next)
            prev = next
        }
        if (!builder.contains('.')) {
            // Pick a random index and set it as dot
            val idx = random.nextInt(len - 2)
            builder[idx + 1] = '.'
        }
        return builder.toString()
    }

    fun patch(
        context: Context,
        apk: File, out: OutputStream,
        pkg: String, label: CharSequence
    ): Boolean {
        val info = context.packageManager.getPackageArchiveInfo(apk.path, 0) ?: return false
        val name = info.applicationInfo.nonLocalizedLabel.toString()
        try {
            JarMap.open(apk, true).use { jar ->
                val je = jar.getJarEntry(ANDROID_MANIFEST)
                val xml = AXML(jar.getRawData(je))

                if (!xml.findAndPatch(APPLICATION_ID to pkg, name to label.toString()))
                    return false

                // Write apk changes
                jar.getOutputStream(je).use { it.write(xml.bytes) }
                val keys = Keygen(context)
                SignApk.sign(keys.cert, keys.key, jar, out)
                return true
            }
        } catch (e: Exception) {
            Timber.e(e)
            return false
        }
    }

    private suspend fun patchAndHide(
        activity: Activity,
        label: String,
        onFailure: Runnable,
        path: String
    ): Boolean {
//        val stub = File(activity.cacheDir, "stub.apk")
        val stub = File(path)

        // Generate a new random package name and signature
        val repack = File(activity.cacheDir, "patched.apk")

        val pkg = genPackageName()

        if (!patch(activity, stub, FileOutputStream(repack), pkg, label))
            return false

        // Install
        val cmd = "pm install ${repack.absolutePath}"
        return if(Shell.su(cmd).exec().isSuccess){
            UiThreadHandler.run { Toast.makeText(activity, "随机包名安装成功,应用名:${label}", Toast.LENGTH_LONG).show() }
            true
        }else{
            UiThreadHandler.run { Toast.makeText(activity, "随机包名安装失败", Toast.LENGTH_LONG).show() }
            false
        }

    }

    //label:应用的名称
    //path:原apk安装包路径
    @Suppress("DEPRECATION")
    suspend fun hide(activity: Activity, label: String, path: String) {
        val onFailure = Runnable {

        }
        val success = withContext(Dispatchers.IO) {
            patchAndHide(activity, label, onFailure,path)
        }
        if (!success) onFailure.run()
    }

}
