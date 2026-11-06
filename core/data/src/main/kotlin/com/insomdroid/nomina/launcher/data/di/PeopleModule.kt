package com.insomdroid.nomina.data.di

import com.insomdroid.nomina.data.people.AndroidPeoplePermissionChecker
import com.insomdroid.nomina.data.people.ContentResolverPeopleContactDataSource
import com.insomdroid.nomina.data.people.PeopleContactDataSource
import com.insomdroid.nomina.data.people.PeoplePermissionChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PeopleModule {
    @Binds
    abstract fun peopleContactDataSource(impl: ContentResolverPeopleContactDataSource): PeopleContactDataSource

    @Binds
    abstract fun peoplePermissionChecker(impl: AndroidPeoplePermissionChecker): PeoplePermissionChecker
}
