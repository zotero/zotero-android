package org.zotero.android.api

import android.content.Context
import android.net.ConnectivityManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.zotero.android.BuildConfig
import org.zotero.android.api.network.InternetConnectionStatusManager
import org.zotero.android.api.network.internetConnectionStatus
import org.zotero.android.architecture.database.DbWrapper
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@DisableInstallInCheck
object ApiModule {

    @Provides
    @Singleton
    fun provideNoAuthenticationApi(@ForBaseApi retrofit: Retrofit): NoAuthenticationApi {
        return retrofit.create(NoAuthenticationApi::class.java)
    }

    @Provides
    @Singleton
    @ForApiWithAuthentication
    fun provideOkHttpWithAuthentication(
        @ForBaseApi baseClient: OkHttpClient,
        authInterceptor: AuthNetworkInterceptor,
    ): OkHttpClient {
        val clientBuilder = baseClient.newBuilder()
        clientBuilder.addInterceptor(authInterceptor)
        return clientBuilder.build()
    }

    @Provides
    @Singleton
    @ForBaseApi
    fun provideBaseOkHttp(
        clientInfoNetworkInterceptor: ClientInfoNetworkInterceptor,
        configuration: NetworkConfiguration
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .setNetworkTimeout(configuration.networkTimeout)
            .addInterceptor(run {
                val httpLoggingInterceptor = HttpLoggingInterceptor()
                httpLoggingInterceptor.apply {
                    httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.HEADERS
                }
            })
            .addInterceptor(clientInfoNetworkInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @ForWebSocket
    fun provideSocketOkHttpClient(
    ): OkHttpClient {
        val timeout = 15L
        return OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor { message ->
                    Timber.tag("SvcSocket")
                    Timber.d(message)
                }
                    .apply { level = HttpLoggingInterceptor.Level.BODY }
            )
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @ForApiWithAuthentication
    fun provideRetrofitWithAuthenticationBuilder(
        @ForBaseApi baseBuilder: Retrofit.Builder,
        @ForApiWithAuthentication okHttpClient: OkHttpClient
    ): Retrofit.Builder {
        return baseBuilder.client(okHttpClient)
    }

    @Provides
    @ForBaseApi
    fun provideBaseRetrofitBuilder(gson: Gson): Retrofit.Builder {
        val stringConverter = ScalarsConverterFactory.create()
        val gsonConverter = GsonConverterFactory.create(gson)
        return Retrofit.Builder()
            .addConverterFactory(stringConverter)
            .addConverterFactory(gsonConverter)
    }

    @Provides
    @Singleton
    @ForBaseApi
    fun provideRetrofit(
        @ForBaseApi baseBuilder: Retrofit.Builder,
        @ForBaseApi okHttpClient: OkHttpClient
    ): Retrofit {
        return baseBuilder
            .baseUrl(BuildConfig.BASE_API_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    @ForApiWithAuthentication
    fun provideRetrofitWithAuthentication(
        @ForApiWithAuthentication baseBuilder: Retrofit.Builder,
        @ForApiWithAuthentication okHttpClient: OkHttpClient
    ): Retrofit {
        return baseBuilder
            .baseUrl(BuildConfig.BASE_API_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideAccountApi(@ForApiWithAuthentication retrofit: Retrofit) = retrofit.create(AccountApi::class.java)

    @Provides
    @Singleton
    fun provideSyncApi(@ForApiWithAuthentication retrofit: Retrofit) = retrofit.create(SyncApi::class.java)

    private fun OkHttpClient.Builder.setNetworkTimeout(seconds: Long) =
        connectTimeout(seconds, TimeUnit.SECONDS)
            .readTimeout(seconds, TimeUnit.SECONDS)
            .writeTimeout(seconds, TimeUnit.SECONDS)

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideInternetConnectionStatusHolder(
        context: Context
    ): InternetConnectionStatusManager {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val currentStatus = connectivityManager.internetConnectionStatus
        return InternetConnectionStatusManager(currentStatus)
    }

    @Provides
    @Singleton
    fun provideGsonInstance(): Gson {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        gsonBuilder.setLenient()
        return gsonBuilder.create()
    }

    @Provides
    @Singleton
    fun provideDbWrapper(
    ): DbWrapper {
       return DbWrapper()
    }
}
