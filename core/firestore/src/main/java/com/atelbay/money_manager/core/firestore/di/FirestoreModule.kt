package com.atelbay.money_manager.core.firestore.di

import com.atelbay.money_manager.core.firestore.datasource.FirestoreDataSource
import com.atelbay.money_manager.core.firestore.datasource.FirestoreDataSourceImpl
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FirestoreModule {

    @Binds
    @Singleton
    abstract fun bindFirestoreDataSource(impl: FirestoreDataSourceImpl): FirestoreDataSource

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore
    }
}
