package com.den.shak.nds

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

open class BaseActivity : AppCompatActivity() {
    private var currentTheme: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Применяем тему перед вызовом super.onCreate(), чтобы она была установлена до создания активности
        applyThemeFromPreferences()
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        // Применяем тему при возобновлении активности, чтобы отразить изменения настроек
        super.onResume()
        applyThemeFromPreferences()
    }

    private fun applyThemeFromPreferences() {
        // Получаем объект SharedPreferences для доступа к сохранённым настройкам
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // Читаем значение настройки темы из SharedPreferences, по умолчанию "auto"
        val themePreference = sharedPreferences.getString("list_preference", "auto")

        // Применяем тему в зависимости от значения настройки
        when (themePreference) {
            "dark" -> setTheme(R.style.Base_Theme_NDS_dark)
            "light" -> setTheme(R.style.Base_Theme_NDS_light)
            else -> setTheme(R.style.Base_Theme_NDS) // Если значение не соответствует "dark" или "light", применяем тему по умолчанию
        }

        // Проверяем, изменилось ли значение темы по сравнению с предыдущим
        if (currentTheme != null && currentTheme != themePreference) {
            // Если тема изменилась, перезапускаем MainActivity, чтобы применить новую тему
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        } else {
            // Обновляем сохранённое значение текущей темы
            currentTheme = themePreference
        }
    }
}
