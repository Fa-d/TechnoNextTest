package dev.sadakat.technonexttest.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.sadakat.technonexttest.data.respository.AuthRepositoryImpl
import dev.sadakat.technonexttest.data.respository.PostsRepositoryImpl
import dev.sadakat.technonexttest.domain.repository.AuthRepository
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPostsRepository(
        postsRepositoryImpl: PostsRepositoryImpl
    ): PostsRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}