package com.jupgi.origami.core.di

import com.jupgi.origami.data.repository.SampleOrigamiRepository
import com.jupgi.origami.domain.repository.OrigamiRepository
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
    abstract fun bindOrigamiRepository(impl: SampleOrigamiRepository): OrigamiRepository
}
