package com.atelbay.money_manager.presentation.settings.ui

data class SupportedCurrency(
    val code: String,
    val name: String,
)

object SupportedCurrencies {
    val all: List<SupportedCurrency> = listOf(
        SupportedCurrency(code = "KZT", name = "Тенге Казахстана"),
        SupportedCurrency(code = "AUD", name = "Австралийский доллар"),
        SupportedCurrency(code = "AZN", name = "Азербайджанский манат"),
        SupportedCurrency(code = "AMD", name = "Армянский драм"),
        SupportedCurrency(code = "BYN", name = "Белорусский рубль"),
        SupportedCurrency(code = "BRL", name = "Бразильский реал"),
        SupportedCurrency(code = "HUF", name = "Венгерский форинт"),
        SupportedCurrency(code = "HKD", name = "Гонконгский доллар"),
        SupportedCurrency(code = "GEL", name = "Грузинский лари"),
        SupportedCurrency(code = "DKK", name = "Датская крона"),
        SupportedCurrency(code = "AED", name = "Дирхам ОАЭ"),
        SupportedCurrency(code = "USD", name = "Доллар США"),
        SupportedCurrency(code = "EUR", name = "Евро"),
        SupportedCurrency(code = "INR", name = "Индийская рупия"),
        SupportedCurrency(code = "IRR", name = "Иранский риал"),
        SupportedCurrency(code = "CAD", name = "Канадский доллар"),
        SupportedCurrency(code = "CNY", name = "Китайский юань"),
        SupportedCurrency(code = "KWD", name = "Кувейтский динар"),
        SupportedCurrency(code = "KGS", name = "Киргизский сом"),
        SupportedCurrency(code = "MYR", name = "Малайзийский ринггит"),
        SupportedCurrency(code = "MXN", name = "Мексиканское песо"),
        SupportedCurrency(code = "MDL", name = "Молдавский лей"),
        SupportedCurrency(code = "NOK", name = "Норвежская крона"),
        SupportedCurrency(code = "PLN", name = "Польский злотый"),
        SupportedCurrency(code = "SAR", name = "Саудовский риял"),
        SupportedCurrency(code = "RUB", name = "Российский рубль"),
        SupportedCurrency(code = "XDR", name = "СДР"),
        SupportedCurrency(code = "SGD", name = "Сингапурский доллар"),
        SupportedCurrency(code = "TJS", name = "Таджикский сомони"),
        SupportedCurrency(code = "THB", name = "Тайский бат"),
        SupportedCurrency(code = "TRY", name = "Турецкая лира"),
        SupportedCurrency(code = "UZS", name = "Узбекский сум"),
        SupportedCurrency(code = "UAH", name = "Украинская гривна"),
        SupportedCurrency(code = "GBP", name = "Фунт стерлингов"),
        SupportedCurrency(code = "CZK", name = "Чешская крона"),
        SupportedCurrency(code = "SEK", name = "Шведская крона"),
        SupportedCurrency(code = "CHF", name = "Швейцарский франк"),
        SupportedCurrency(code = "ZAR", name = "Южноафриканский рэнд"),
        SupportedCurrency(code = "KRW", name = "Южнокорейская вона"),
        SupportedCurrency(code = "JPY", name = "Японская иена"),
    )

    private val byCode = all.associateBy { it.code }

    fun fromCode(code: String): SupportedCurrency {
        return byCode[code.uppercase()] ?: defaultBase
    }

    val defaultBase: SupportedCurrency = fromCode("KZT")
    val defaultTarget: SupportedCurrency = fromCode("USD")
}
