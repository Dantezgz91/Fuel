package com.fueltracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fueltracker.app.data.local.dao.GasStationDao
import com.fueltracker.app.data.local.dao.PriceRecordDao
import com.fueltracker.app.data.local.entity.GasStationEntity
import com.fueltracker.app.data.local.entity.PriceRecordEntity

@Database(
    entities = [GasStationEntity::class, PriceRecordEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gasStationDao(): GasStationDao
    abstract fun priceRecordDao(): PriceRecordDao
}
