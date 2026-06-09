package com.fueltracker.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE gas_stations ADD COLUMN schedule TEXT")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fuel_tracker.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideGasStationDao(db: AppDatabase): GasStationDao = db.gasStationDao()

    @Provides
    fun providePriceRecordDao(db: AppDatabase): PriceRecordDao = db.priceRecordDao()
}
