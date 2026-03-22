package com.atelbay.money_manager.domain.importstatement.usecase

fun interface ImportProgressCollector {
    fun emit(event: ImportStepEvent)
}

object NoOpCollector : ImportProgressCollector {
    override fun emit(event: ImportStepEvent) {}
}
