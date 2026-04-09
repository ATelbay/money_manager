package com.atelbay.money_manager.presentation.settings.ui

data class SupportedCurrency(
    val code: String,
    val name: String,
    val nameEn: String = name,
    val nameKk: String = name,
)

object SupportedCurrencies {
    val all: List<SupportedCurrency> = listOf(
        SupportedCurrency(code = "KZT", name = "Тенге Казахстана", nameEn = "Kazakhstani Tenge", nameKk = "Қазақстан теңгесі"),
        SupportedCurrency(code = "AUD", name = "Австралийский доллар", nameEn = "Australian Dollar", nameKk = "Австралия доллары"),
        SupportedCurrency(code = "AZN", name = "Азербайджанский манат", nameEn = "Azerbaijani Manat", nameKk = "Әзірбайжан манаты"),
        SupportedCurrency(code = "AMD", name = "Армянский драм", nameEn = "Armenian Dram", nameKk = "Армения драмы"),
        SupportedCurrency(code = "BYN", name = "Белорусский рубль", nameEn = "Belarusian Ruble", nameKk = "Беларусь рублі"),
        SupportedCurrency(code = "BRL", name = "Бразильский реал", nameEn = "Brazilian Real", nameKk = "Бразилия реалы"),
        SupportedCurrency(code = "HUF", name = "Венгерский форинт", nameEn = "Hungarian Forint", nameKk = "Венгрия форинті"),
        SupportedCurrency(code = "HKD", name = "Гонконгский доллар", nameEn = "Hong Kong Dollar", nameKk = "Гонконг доллары"),
        SupportedCurrency(code = "GEL", name = "Грузинский лари", nameEn = "Georgian Lari", nameKk = "Грузия ларисі"),
        SupportedCurrency(code = "DKK", name = "Датская крона", nameEn = "Danish Krone", nameKk = "Дания кронасы"),
        SupportedCurrency(code = "AED", name = "Дирхам ОАЭ", nameEn = "UAE Dirham", nameKk = "БАӘ дирхамы"),
        SupportedCurrency(code = "USD", name = "Доллар США", nameEn = "US Dollar", nameKk = "АҚШ доллары"),
        SupportedCurrency(code = "EUR", name = "Евро", nameEn = "Euro", nameKk = "Еуро"),
        SupportedCurrency(code = "INR", name = "Индийская рупия", nameEn = "Indian Rupee", nameKk = "Үндістан рупиясы"),
        SupportedCurrency(code = "IRR", name = "Иранский риал", nameEn = "Iranian Rial", nameKk = "Иран риалы"),
        SupportedCurrency(code = "CAD", name = "Канадский доллар", nameEn = "Canadian Dollar", nameKk = "Канада доллары"),
        SupportedCurrency(code = "CNY", name = "Китайский юань", nameEn = "Chinese Yuan", nameKk = "Қытай юані"),
        SupportedCurrency(code = "KWD", name = "Кувейтский динар", nameEn = "Kuwaiti Dinar", nameKk = "Кувейт динары"),
        SupportedCurrency(code = "KGS", name = "Киргизский сом", nameEn = "Kyrgyzstani Som", nameKk = "Қырғызстан сомы"),
        SupportedCurrency(code = "MYR", name = "Малайзийский ринггит", nameEn = "Malaysian Ringgit", nameKk = "Малайзия ринггиті"),
        SupportedCurrency(code = "MXN", name = "Мексиканское песо", nameEn = "Mexican Peso", nameKk = "Мексика песосы"),
        SupportedCurrency(code = "MDL", name = "Молдавский лей", nameEn = "Moldovan Leu", nameKk = "Молдова леуі"),
        SupportedCurrency(code = "NOK", name = "Норвежская крона", nameEn = "Norwegian Krone", nameKk = "Норвегия кронасы"),
        SupportedCurrency(code = "PLN", name = "Польский злотый", nameEn = "Polish Zloty", nameKk = "Польша злотыйі"),
        SupportedCurrency(code = "SAR", name = "Саудовский риял", nameEn = "Saudi Riyal", nameKk = "Сауд Арабиясы риялы"),
        SupportedCurrency(code = "RUB", name = "Российский рубль", nameEn = "Russian Ruble", nameKk = "Ресей рублі"),
        SupportedCurrency(code = "XDR", name = "СДР", nameEn = "Special Drawing Rights", nameKk = "Арнайы қарыз алу құқықтары"),
        SupportedCurrency(code = "SGD", name = "Сингапурский доллар", nameEn = "Singapore Dollar", nameKk = "Сингапур доллары"),
        SupportedCurrency(code = "TJS", name = "Таджикский сомони", nameEn = "Tajikistani Somoni", nameKk = "Тәжікстан сомонийі"),
        SupportedCurrency(code = "THB", name = "Тайский бат", nameEn = "Thai Baht", nameKk = "Тайланд баты"),
        SupportedCurrency(code = "TRY", name = "Турецкая лира", nameEn = "Turkish Lira", nameKk = "Түркия лирасы"),
        SupportedCurrency(code = "UZS", name = "Узбекский сум", nameEn = "Uzbekistani Som", nameKk = "Өзбекстан сомы"),
        SupportedCurrency(code = "UAH", name = "Украинская гривна", nameEn = "Ukrainian Hryvnia", nameKk = "Украина гривнасы"),
        SupportedCurrency(code = "GBP", name = "Фунт стерлингов", nameEn = "British Pound Sterling", nameKk = "Британия фунт стерлингі"),
        SupportedCurrency(code = "CZK", name = "Чешская крона", nameEn = "Czech Koruna", nameKk = "Чехия кронасы"),
        SupportedCurrency(code = "SEK", name = "Шведская крона", nameEn = "Swedish Krona", nameKk = "Швеция кронасы"),
        SupportedCurrency(code = "CHF", name = "Швейцарский франк", nameEn = "Swiss Franc", nameKk = "Швейцария франкі"),
        SupportedCurrency(code = "ZAR", name = "Южноафриканский рэнд", nameEn = "South African Rand", nameKk = "Оңтүстік Африка рэнді"),
        SupportedCurrency(code = "KRW", name = "Южнокорейская вона", nameEn = "South Korean Won", nameKk = "Оңтүстік Корея воны"),
        SupportedCurrency(code = "JPY", name = "Японская иена", nameEn = "Japanese Yen", nameKk = "Жапония иенасы"),
    )

    private val byCode = all.associateBy { it.code }
    val defaultBase: SupportedCurrency = requireCurrency("KZT")
    val defaultTarget: SupportedCurrency = requireCurrency("USD")

    fun fromCode(
        code: String,
        fallback: SupportedCurrency = defaultBase,
    ): SupportedCurrency {
        return byCode[code.uppercase()] ?: fallback
    }

    fun isSupported(code: String): Boolean = byCode.containsKey(code.uppercase())

    private fun requireCurrency(code: String): SupportedCurrency {
        return checkNotNull(byCode[code]) {
            "Missing required supported currency: $code"
        }
    }
}
