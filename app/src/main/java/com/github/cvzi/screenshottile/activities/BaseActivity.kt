package com.github.cvzi.screenshottile.activities

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.github.cvzi.screenshottile.utils.setUserLanguage

open class BaseActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setUserLanguage()
        super.onCreate(savedInstanceState)
    }
}

open class BaseAppCompatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setUserLanguage()
        super.onCreate(savedInstanceState)
    }
}

open class BaseFragmentActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setUserLanguage()
        super.onCreate(savedInstanceState)
    }
}