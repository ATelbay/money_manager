package com.atelbay.money_manager.presentation.statistics.ui

import com.patrykandpatrick.vico.core.common.data.ExtraStore

// ExtraStore keys for Vico chart metadata
internal val xToLabelMapKey = ExtraStore.Key<Map<Double, String>>()
internal val xToDateStringKey = ExtraStore.Key<Map<Double, String>>()
internal val todayIndexKey = ExtraStore.Key<Int>()
internal val currencySymbolKey = ExtraStore.Key<String>()
internal val currencyPrefixKey = ExtraStore.Key<Boolean>()
