package com.fueltracker.app.di

import android.content.Context
import androidx.room.Room
import com.fueltracker.app.data.local.AppDatabase
import com.fueltracker.app.data.local.dao.GasStationDao
import com.fueltracker.app.data.local.dao.PriceRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fuel_tracker.db"
        ).build()
    }

    @Provides
    fun provideGasStationDao(db: AppDatabase): GasStationDao = db.gasStationDao()

    @Provides
    fun providePriceRecordDao(db: AppDatabase): PriceRecordDao = db.priceRecordDao()
}
