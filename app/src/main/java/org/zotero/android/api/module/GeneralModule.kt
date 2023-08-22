package org.zotero.android.api.module

import android.content.Context
import android.net.ConnectivityManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import kotlinx.serialization.json.Json
import org.zotero.android.api.ForGsonWithRoundedDecimals
import org.zotero.android.api.network.InternetConnectionStatusManager
import org.zotero.android.api.network.internetConnectionStatus
import org.zotero.android.database.DbWrapper
import org.zotero.android.files.FormattedDoubleJsonSerializer
import javax.inject.Singleton

@Module
@DisableInstallInCheck
object GeneralModule {

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
    //Must not be singleton as it's used for the creation of multiple Gson instances with different configs
    fun provideGsonBuilder(): GsonBuilder {
        val gsonBuilder = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .setLenient()
        return gsonBuilder
    }

    @Provides
    @Singleton
    fun provideGson(gsonBuilder: GsonBuilder): Gson {
        return gsonBuilder.create()
    }

    @Provides
    @Singleton
    @ForGsonWithRoundedDecimals
    fun provideGsonWithRoundedDecimals(gsonBuilder: GsonBuilder): Gson {
        val type = object : TypeToken<Double>() {}.type
        gsonBuilder.registerTypeAdapter(type, FormattedDoubleJsonSerializer())
        return gsonBuilder.create()
    }

    @Provides
    @Singleton
    fun provideDbWrapper(
    ): DbWrapper {
        return DbWrapper()
    }
}
