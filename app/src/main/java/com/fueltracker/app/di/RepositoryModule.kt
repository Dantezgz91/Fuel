package com.fueltracker.app.di

import com.fueltracker.app.data.repository.GasStationRepositoryImpl
import com.fueltracker.app.data.repository.PriceRepositoryImpl
import com.fueltracker.app.domain.repository.GasStationRepository
import com.fueltracker.app.domain.repository.PriceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGasStationRepository(impl: GasStationRepositoryImpl): GasStationRepository

    @Binds
    @Singleton
    abstract fun bindPriceRepository(impl: PriceRepositoryImpl): PriceRepository
}
