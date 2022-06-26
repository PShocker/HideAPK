package com.shocker.ui

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.shocker.hideapk.databinding.ActivityMainBinding
import com.shocker.hideapk.hide.HideAPK
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var activity: Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()

        activity=this
        GlobalScope.launch {
            HideAPK.hide(activity,"randomName",getApplicationInfo().sourceDir)
        }
    }

    /**
     * A native method that is implemented by the 'hideapk' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'hideapk' library on application startup.
        init {
            System.loadLibrary("hideapk")
        }
    }
}