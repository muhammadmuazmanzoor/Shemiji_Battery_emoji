package com.shemiji.emogibattery.di

import com.shemiji.emogibattery.BuildConfig
import com.shemiji.emogibattery.data.model.ContentSourceMode
import com.shemiji.emogibattery.data.remote.ContentApiService
import com.shemiji.emogibattery.data.repository.ContentRepository
import com.shemiji.emogibattery.data.repository.ContentRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindContentRepository(
        implementation: ContentRepositoryImpl,
    ): ContentRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentSourceMode(): ContentSourceMode =
        if (BuildConfig.USE_REMOTE_DATA) {
            ContentSourceMode.REMOTE_API
        } else {
            ContentSourceMode.LOCAL_DRAWABLES
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideContentApiService(retrofit: Retrofit): ContentApiService =
        retrofit.create(ContentApiService::class.java)
}
