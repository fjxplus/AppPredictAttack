package com.mlprivacy.apppredictattack

import android.content.Context
import androidx.core.content.edit
import java.io.InputStream
import java.io.OutputStream

/**
 * 文件读写接口
 */
object FileUtils {

    /**
     * 写文件
     * @param fileName 文件名
     * @param append 是否为追加写
     */
    fun write(fileName: String, append: Boolean = false): OutputStream {
        val mode = if (append) {
            Context.MODE_APPEND
        } else {
            Context.MODE_PRIVATE
        }
        return BaseApplication.appContext.openFileOutput(fileName, mode)
    }

    /**
     * 读文件
     * @param fileName 文件名
     * @param append 是否为追加写
     */
    fun read(fileName: String): InputStream {
        return BaseApplication.appContext.openFileInput(fileName)
    }

    /**
     * [用于测试/集成开发]读assets目录下文件
     * @param fileName 文件名
     * @return InputStream
     */
    fun readAssetFile(fileName: String): InputStream {
        val assets = BaseApplication.appContext.assets
        return assets.open(fileName)
    }

    /**
     * 存储字符串键值对
     * @param name 文件名称
     * @param key 键值
     * @see android.content.SharedPreferences.Editor.putString
     */
    fun putString(name: String, vararg pairs: Pair<String, String>) {
        val sp =
            BaseApplication.appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        sp.edit {
            pairs.forEach { pair ->
                putString(pair.first, pair.second)
            }
        }
    }

    /**
     * 读取字符串键值对
     * @param name 文件名称
     * @param key 键值
     * @see android.content.SharedPreferences.getString
     */
    fun getString(name: String, key: String, defValue: String): String? {
        val sp =
            BaseApplication.appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        return sp.getString(key, defValue)
    }

    /**
     * 存储字符串Set
     * @param name 文件名称
     * @param key 键值
     * @see android.content.SharedPreferences.Editor.putStringSet
     */
    fun putStringSet(name: String, key: String, value: Set<String>) {
        val sp =
            BaseApplication.appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        sp.edit {
            putStringSet(key, value)
        }
    }

    /**
     * 读取字符串Set
     * @param name 文件名称
     * @param key 键值
     * @see android.content.SharedPreferences.getStringSet
     */
    fun getStringSet(name: String, key: String, defValue: Set<String> = emptySet()): Set<String>? {
        val sp =
            BaseApplication.appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        return sp.getStringSet(key, defValue)
    }

    fun getBoolean(name: String, key: String, defValue: Boolean = false): Boolean {
        val sp =
            BaseApplication.appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        return sp.getBoolean(key, defValue)
    }

    fun putBoolean(name: String, key: String, value: Boolean) {
        val sp =
            BaseApplication.appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        sp.edit {
            putBoolean(key, value)
        }
    }

    fun getInt(name: String, key: String, defValue: Int = 0): Int {
        val sp =
            BaseApplication.appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        return sp.getInt(key, defValue)
    }

    fun putInt(name: String, key: String, value: Int) {
        val sp =
            BaseApplication.appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        sp.edit {
            putInt(key, value)
        }
    }

    fun getFloat(name: String, key: String, defValue: Float = 0f): Float {
        val sp =
            BaseApplication.appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        return sp.getFloat(key, defValue)
    }

    fun putFloat(name: String, key: String, value: Float) {
        val sp =
            BaseApplication.appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        sp.edit {
            putFloat(key, value)
        }
    }

    fun getLong(name: String, key: String, defValue: Long = 0L): Long {
        val sp =
            BaseApplication.appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        return sp.getLong(key, defValue)
    }

    fun putLong(name: String, key: String, value: Long) {
        val sp =
            BaseApplication.appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        sp.edit {
            putLong(key, value)
        }
    }
}