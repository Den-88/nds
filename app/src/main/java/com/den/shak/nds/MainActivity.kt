package com.den.shak.nds

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import com.den.shak.nds.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private var bannerAd: BannerAdView? = null
    private lateinit var choosePercent: MaterialAutoCompleteTextView
    private lateinit var sumWithNDS: EditText
    private lateinit var sumWithoutNDS: EditText
    private lateinit var sumNDS: EditText
    private lateinit var sharedPreferences: SharedPreferences
    private var isSaveExit: Boolean = false
    private lateinit var spinnerItems: Array<String>
    private lateinit var adapter: ArrayAdapter<String>
    private var percent: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация рекламного SDK Yandex
        MobileAds.initialize(this) {}

        // Настройка привязки вида
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация полей ввода
        sumWithNDS = findViewById(R.id.SumWithNDS)
        sumWithoutNDS = findViewById(R.id.SumWithoutNDS)
        sumNDS = findViewById(R.id.SumNDS)
        choosePercent = findViewById(R.id.autoCompleteTextView)

        // Загружаем массив строк из ресурсов (arrays.xml) в переменную spinnerItems
        spinnerItems = resources.getStringArray(R.array.spinner_values)
        // Создание адаптера для выпадающего списка, используя массив spinnerItems
        adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, spinnerItems)
        // Применение созданного адаптера к элементу MaterialAutoCompleteTextView (choosePercent)
        choosePercent.setAdapter(adapter)

        // Установка отступов для главного View с учетом системных панелей
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Устанавливаем слушатели для различных элементов интерфейса
        setListeners()

        // Отслеживание изменения размера контейнера для рекламы и загрузка баннера
        binding.adContainerView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Удаляем слушатель после первого вызова
                binding.adContainerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                // Загружаем баннерную рекламу с вычисленным размером
                bannerAd = loadBannerAd(adSize)
            }
        })

        // Настройка SharedPreferences для сохранения состояния
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        isSaveExit = sharedPreferences.getBoolean("checkbox_preference", false)
        if (isSaveExit) {
            sumWithNDS.setText(sharedPreferences.getString("SumWithNDS", ""))
            choosePercent.setText(sharedPreferences.getString("autoCompleteTextView", getString(R.string.snip1)), false)
        } else {
            // Устанавливаем значение по умолчанию
            choosePercent.setText(getString(R.string.snip1), false)
        }
    }

    // Этот метод вызывается, когда активность теряет фокус
    override fun onPause() {
        super.onPause()
        // Сохраняем текущее значение текста из поля ввода "SumWithNDS" в SharedPreferences.
        sharedPreferences.edit().putString("SumWithNDS", binding.SumWithNDS.text.toString()).apply()
    }

    // Восстановление состояния активности после её пересоздания (например, при повороте экрана)
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        // Создание адаптера для выпадающего списка, используя массив spinnerItems
        adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, spinnerItems)
        // Применение созданного адаптера к элементу MaterialAutoCompleteTextView (choosePercent)
        choosePercent.setAdapter(adapter)
    }


    // Создание меню
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Обработка нажатий на элементы меню
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Переход к экрану с инструкцией
            R.id.instr -> {
                val intent = Intent(this, ManualActivity::class.java)
                startActivity(intent)
                return true
            }
            // Переход к экрану с настройками
            R.id.setting -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            // Открытие диалогового окна "О приложении"
            R.id.about -> {
                // Получаем информацию о пакете
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                // Чтение версии приложения
                val versionName = packageInfo.versionName
                val text = getString(R.string.AboutText1) + " " + versionName + getString(R.string.AboutText2)

                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.MenuAbout)
                    .setMessage(text)
                    .setNegativeButton(R.string.AboutButton) { dialog, _ -> dialog.cancel() }
                    .show()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    // Вычисление размера баннера на основе ширины контейнера и плотности экрана
    private val adSize: BannerAdSize
        get() {
            // Получаем ширину контейнера
            var adWidthPixels = binding.adContainerView.width
            if (adWidthPixels == 0) {
                adWidthPixels = resources.displayMetrics.widthPixels
            }
            // Преобразуем ширину в dp для баннера
            val adWidth = (adWidthPixels / resources.displayMetrics.density).roundToInt()
            return BannerAdSize.stickySize(this, adWidth)
        }

    // Метод для загрузки баннерной рекламы с обработкой событий
    private fun loadBannerAd(adSize: BannerAdSize): BannerAdView {
        return binding.adContainerView.apply {
            setAdSize(adSize)
            // Получаем ID рекламного блока
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val installSourceInfo = packageManager.getInstallSourceInfo(packageName)
                val installerPackageName = installSourceInfo.installingPackageName

                when (installerPackageName) {
                    "com.android.vending" -> setAdUnitId(ConfigReader.getAdUnitId(this@MainActivity))
                    "ru.vk.store" -> setAdUnitId(ConfigReader.getAdRuStoreUnitId(this@MainActivity))
                    else -> setAdUnitId(ConfigReader.getAdUnitId(this@MainActivity))
                }
            } else {
                // Используем устаревший метод для API ниже 30
                @Suppress("DEPRECATION")
                val installerPackageName = packageManager.getInstallerPackageName(packageName)

                when (installerPackageName) {
                    "com.android.vending" -> setAdUnitId(ConfigReader.getAdUnitId(this@MainActivity))
                    "ru.vk.store" -> setAdUnitId(ConfigReader.getAdRuStoreUnitId(this@MainActivity))
                    else -> setAdUnitId(ConfigReader.getAdUnitId(this@MainActivity))
                }
            }

            // Установка слушателя событий баннерной рекламы
            setBannerAdEventListener(object : BannerAdEventListener {
                override fun onAdLoaded() {
                    // Удаление баннера, если активность уничтожена
                    if (isDestroyed) {
                        bannerAd?.destroy()
                        return
                    }
                }

                // Обработка ошибки загрузки рекламы
                override fun onAdFailedToLoad(error: AdRequestError) {}

                // Обработка клика на рекламу
                override fun onAdClicked() {}

                // Событие при уходе пользователя из приложения
                override fun onLeftApplication() {}

                // Событие при возврате в приложение
                override fun onReturnedToApplication() {}

                // Событие при показе рекламы
                override fun onImpression(impressionData: ImpressionData?) {
                }
            })
            // Загружаем рекламный запрос
            loadAd(AdRequest.Builder().build())
        }
    }

    // Метод для вычисления значений на основе введенного процента
    private fun doCalculation() {
        val textPercent = binding.autoCompleteTextView.text.toString()
        // Извлекаем строку до знака процента
        val percentIndex = textPercent.indexOf('%')
        // Извлекаем строку до знака процента
        val numberString = textPercent.substring(0, percentIndex).trim()
        // Преобразуем строку в число
        percent = numberString.toDouble()

        if (binding.SumWithoutNDS.isFocused) {
            if (binding.SumWithoutNDS.text.toString().isEmpty()) {
                binding.SumWithNDS.setText("")
                binding.SumNDS.setText("")
            } else {
                val x = binding.SumWithoutNDS.getText().toString().toDouble()
                binding.SumWithNDS.setText(String.format(Locale.US, "%.2f", (x * (100 + percent) / 100)))
                binding.SumNDS.setText(String.format(Locale.US, "%.2f", (x * percent / 100)))

            }

        } else if (binding.SumNDS.isFocused) {
            if (binding.SumNDS.text.toString().isEmpty()) {
                binding.SumWithNDS.setText("")
                binding.SumWithoutNDS.setText("")
            } else {
                val x = binding.SumNDS.getText().toString().toDouble()
                binding.SumWithNDS.setText(String.format(Locale.US, "%.2f", (x + (x * 100 / percent ))))
                binding.SumWithoutNDS.setText(String.format(Locale.US, "%.2f", (x * 100 / percent )))

            }
        } else {
            if (binding.SumWithNDS.text.toString().isEmpty()) {
                binding.SumWithoutNDS.setText("")
                binding.SumNDS.setText("")
            } else {
                val x = binding.SumWithNDS.getText().toString().toDouble()
                binding.SumWithoutNDS.setText(String.format(Locale.US, "%.2f", (x - (x * percent)/(100 + percent))))
                binding.SumNDS.setText(String.format(Locale.US, "%.2f", (x * percent)/(100 + percent)))

            }
        }
    }

    // Установка слушателей
     private fun setListeners() {
        // Установить TextWatcher для EditText
        binding.SumWithNDS.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.SumWithNDS.isFocused) {
                    doCalculation()
                }
            }
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val text = it.toString()
                    // Проверяем, есть ли точка в числе
                    if (text.contains(".")) {
                        // Разделяем число на две части: до и после запятой
                        val parts = text.split(".")
                        // Если после запятой больше 2 цифр, обрезаем строку
                        if (parts.size > 1 && parts[1].length > 2) {
                            it.replace(0, it.length, parts[0] + "." + parts[1].substring(0, 2))
                        }
                    }
                }
            }
        })

        binding.SumWithoutNDS.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.SumWithoutNDS.isFocused) {
                    doCalculation()
                }
            }
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val text = it.toString()
                    // Проверяем, есть ли точка в числе
                    if (text.contains(".")) {
                        // Разделяем число на две части: до и после запятой
                        val parts = text.split(".")
                        // Если после запятой больше 2 цифр, обрезаем строку
                        if (parts.size > 1 && parts[1].length > 2) {
                            it.replace(0, it.length, parts[0] + "." + parts[1].substring(0, 2))
                        }
                    }
                }
            }
        })
        binding.SumNDS.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.SumNDS.isFocused) {
                    doCalculation()
                }
            }
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val text = it.toString()
                    // Проверяем, есть ли точка в числе
                    if (text.contains(".")) {
                        // Разделяем число на две части: до и после запятой
                        val parts = text.split(".")
                        // Если после запятой больше 2 цифр, обрезаем строку
                        if (parts.size > 1 && parts[1].length > 2) {
                            it.replace(0, it.length, parts[0] + "." + parts[1].substring(0, 2))
                        }
                    }
                }
            }
        })

        binding.autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() == getString(R.string.snip4)) {
                    val viewd: View = layoutInflater.inflate(R.layout.dialog, null) as LinearLayout
                    val prc = viewd.findViewById<View>(R.id.prc) as EditText
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle( R.string.Customprocent)
                        .setCancelable(false)
                        .setView(viewd)
                        .setPositiveButton(R.string.ProcentOK) { dialog, _ ->
                            if (prc.text.toString().isEmpty()) {
                                choosePercent.setText( getText(R.string.snip1), false)
                                Snackbar.make(findViewById(android.R.id.content),
                                    getString(R.string.SetBasePercent), Snackbar.LENGTH_SHORT).show()

                            } else {
                                choosePercent.setText(prc.text.toString() + "%", false)
                                dialog.dismiss()  // Закрытие диалога
                            }
                        }
                        .create().apply {
                            setOnShowListener {
                                // Устанавливаем фокус на EditText при показе диалога
                                prc.requestFocus()
//                                // Показываем клавиатуру
                                // Используем Handler для отложенного выполнения кода после показа диалога
                                Handler(Looper.getMainLooper()).postDelayed({
                                    // Показываем цифровую клавиатуру
                                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.showSoftInput(prc, InputMethodManager.SHOW_IMPLICIT)
                                }, 200) // Задержка в миллисекундах

                            }
                        }
                        .show()

                    val decimalInputFilter = object : InputFilter {
                        private val beforeDecimal = 2
                        private val afterDecimal = 2
                        override fun filter(
                            source: CharSequence, start: Int, end: Int,
                            dest: Spanned, dstart: Int, dend: Int
                        ): CharSequence? {
                            val etText = prc.text.toString()
                            if (etText.isEmpty()) {
                                return null
                            }
                            var temp = prc.text.toString() + source.toString()
                            if (temp == ".") {
                                return "0."
                            } else if (temp.indexOf(".") == -1) {
                                // Decimal point not placed yet
                                if (temp.length > beforeDecimal) {
                                    return ""
                                }
                            } else {
                                val cursorPosition = prc.selectionStart
                                val dotPosition = if (etText.indexOf(".") == -1) {
                                    temp.indexOf(".")
                                } else {
                                    etText.indexOf(".")
                                }
                                if (cursorPosition <= dotPosition) {
                                    val beforeDot = etText.substring(0, dotPosition)
                                    return if (beforeDot.length < beforeDecimal) {
                                        source
                                    } else {
                                        if (source.toString().equals(".", ignoreCase = true)) {
                                            source
                                        } else {
                                            ""
                                        }
                                    }
                                } else {
                                    temp = temp.substring(temp.indexOf(".") + 1)
                                    if (temp.length > afterDecimal) {
                                        return ""
                                    }
                                }
                            }
                            return null
                        }
                    }
                    // Устанавливаем фильтр в EditText
                    prc.filters = arrayOf(decimalInputFilter)
                } else  {
                    doCalculation()
                }
            }
            override fun afterTextChanged(s: Editable?) {
                sharedPreferences.edit().putString("autoCompleteTextView", s.toString()).apply()
            }
        })

        // Установка слушателя изменения фокуса для EditText sumWithNDS
        sumWithNDS.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                sumWithNDS.hint = "" // Убираем hint при фокусировке
            } else {
                sumWithNDS.hint = getString(R.string.Hint) // Возвращаем hint, когда фокус теряется
            }
        }

        // Установка слушателя изменения фокуса для EditText sumWithoutNDS
        sumWithoutNDS.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                sumWithoutNDS.hint = ""
            } else {
                sumWithoutNDS.hint = getString(R.string.Hint)
            }
        }

        // Установка слушателя изменения фокуса для EditText sumNDS
        sumNDS.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                sumNDS.hint = ""
            } else {
                sumNDS.hint = getString(R.string.Hint)
            }
        }
    }


}



