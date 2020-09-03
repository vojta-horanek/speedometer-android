package eu.vojtechh.speedometer.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences


object SharedPref {
    private var mSharedPref: SharedPreferences? = null
    const val WHEEL_DIAMETER = "NAME"
    fun init(context: Context) {
        if (mSharedPref == null) mSharedPref =
            context.getSharedPreferences(context.packageName, Activity.MODE_PRIVATE)
    }

    fun read(key: String?, defValue: String?): String? {
        return mSharedPref!!.getString(key, defValue)
    }

    fun write(key: String?, value: String?) {
        val prefsEditor = mSharedPref!!.edit()
        prefsEditor.putString(key, value)
        prefsEditor.apply()
    }

    fun read(key: String?, defValue: Boolean): Boolean {
        return mSharedPref!!.getBoolean(key, defValue)
    }

    fun write(key: String?, value: Boolean) {
        val prefsEditor = mSharedPref!!.edit()
        prefsEditor.putBoolean(key, value)
        prefsEditor.apply()
    }

    fun read(key: String?, defValue: Int): Int {
        return mSharedPref!!.getInt(key, defValue)
    }

    fun write(key: String?, value: Int?) {
        val prefsEditor = mSharedPref!!.edit()
        prefsEditor.putInt(key, value!!).apply()
    }

    fun read(key: String?, defValue: Float): Float {
        return mSharedPref!!.getFloat(key, defValue)
    }

    fun write(key: String?, value: Float?) {
        val prefsEditor = mSharedPref!!.edit()
        prefsEditor.putFloat(key, value!!).apply()
    }
}