package com.den.shak.nds

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CustomListPreference(context: Context, attrs: AttributeSet?) : ListPreference(context, attrs) {
    override fun onClick() {
        // Создаем Material диалог вместо стандартного
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(title) // Устанавливаем заголовок диалога из title

        // Устанавливаем однократный выбор из списка вариантов
        builder.setSingleChoiceItems(entries, findIndexOfValue(value)) { dialog, which ->
            val selectedValue = entryValues[which].toString() // Получаем выбранное значение
            if (callChangeListener(selectedValue)) { // Проверяем, можно ли изменить значение
                value = selectedValue // Устанавливаем новое значение
            }
            dialog.dismiss() // Закрываем диалог после выбора
        }

        // Устанавливаем кнопку отмены
        builder.setNegativeButton(android.R.string.cancel, null)

        // Показываем диалог
        builder.show()
    }
}
