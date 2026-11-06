package com.insomdroid.nomina.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import com.insomdroid.nomina.data.search.AppSearchProvider
import com.insomdroid.nomina.data.search.CalculatorSearchProvider
import com.insomdroid.nomina.data.search.SearchProvider
import com.insomdroid.nomina.data.search.SettingsSearchProvider

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchModule {
    @Binds @IntoSet abstract fun appProvider(p: AppSearchProvider): SearchProvider
    @Binds @IntoSet abstract fun settingsProvider(p: SettingsSearchProvider): SearchProvider
    @Binds @IntoSet abstract fun calcProvider(p: CalculatorSearchProvider): SearchProvider

}
