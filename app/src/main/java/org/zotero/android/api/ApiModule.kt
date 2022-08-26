package org.zotero.android.api

import android.content.Context
import android.net.ConnectivityManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.zotero.android.BuildConfig
import org.zotero.android.api.network.InternetConnectionStatusManager
import org.zotero.android.api.network.internetConnectionStatus
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@DisableInstallInCheck
object ApiModule {

    @Provides
    @Singleton
    fun provideRetrofit(gson: Gson): Retrofit {
        val stringConverter = ScalarsConverterFactory.create()
        val gsonConverter = GsonConverterFactory.create(gson)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(run {
                val httpLoggingInterceptor = HttpLoggingInterceptor()
                httpLoggingInterceptor.apply {
                    httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                }
            })
            .build()
        return Retrofit.Builder()
            .addConverterFactory(stringConverter)
            .addConverterFactory(gsonConverter)
            .client(okHttpClient)
            .baseUrl(BuildConfig.BASE_API_URL)
            .build()
    }

    @Provides
    @Singleton
    fun provideAccountApi(retrofit: Retrofit): AccountApi {
        return retrofit.create(AccountApi::class.java)
    }

    @Provides
    @Singleton
    @Named("apollo")
    fun provideApolloOkHttpClient(
//        authInterceptor: AuthNetworkInterceptor,
        clientInfoNetworkInterceptor: ClientInfoNetworkInterceptor,
        context: Context,
        configuration: ApiConfiguration
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .setNetworkTimeout(configuration.networkTimeout)
            .addInterceptor(httpLoggingInterceptor())
//            .addInterceptor(authInterceptor)
            .addInterceptor(clientInfoNetworkInterceptor)
            .build()
    }

    private fun httpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Timber.tag("Apollo")
            Timber.d(message)
        }
            .apply { level = HttpLoggingInterceptor.Level.BODY }
    }

    private fun OkHttpClient.Builder.setNetworkTimeout(seconds: Long) =
        connectTimeout(seconds, TimeUnit.SECONDS)
            .readTimeout(seconds, TimeUnit.SECONDS)
            .writeTimeout(seconds, TimeUnit.SECONDS)

    @Provides
    @Named("DefaultCallFactory")
    fun provideCallFactory(
        @Named("apollo") okHttpClient: Lazy<OkHttpClient>,
    ): Call.Factory = Call.Factory { request -> okHttpClient.get().newCall(request) }

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
}
