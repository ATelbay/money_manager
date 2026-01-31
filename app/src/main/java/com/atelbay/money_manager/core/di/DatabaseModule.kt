package com.atelbay.money_manager.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.atelbay.money_manager.core.database.DefaultCategories
import com.atelbay.money_manager.core.database.MoneyManagerDatabase
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        categoryDaoProvider: Provider<CategoryDao>,
    ): MoneyManagerDatabase {
        return Room.databaseBuilder(
            context,
            MoneyManagerDatabase::class.java,
            "money_manager.db",
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        categoryDaoProvider.get().insertAll(DefaultCategories.all())
                    }
                }
            })
            .build()
    }

    @Provides
    fun provideAccountDao(db: MoneyManagerDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: MoneyManagerDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTransactionDao(db: MoneyManagerDatabase): TransactionDao = db.transactionDao()
}
