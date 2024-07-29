package org.zotero.android.api.module

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import kotlinx.serialization.json.Json
import org.zotero.android.api.annotations.ForGsonWithRoundedDecimals
import org.zotero.android.api.network.InternetConnectionStatusManager
import org.zotero.android.api.network.internetConnectionStatus
import org.zotero.android.architecture.serialization.SealedClassTypeAdapter
import org.zotero.android.architecture.serialization.UriTypeAdapter
import org.zotero.android.files.FormattedDoubleJsonSerializer
import javax.inject.Singleton
import kotlin.jvm.internal.Reflection

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
            .registerTypeAdapterFactory(
                object : TypeAdapterFactory {
                    override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
                        val kclass = Reflection.getOrCreateKotlinClass(type.rawType)
                        return if (kclass.sealedSubclasses.any()) {
                            SealedClassTypeAdapter<T>(kclass, gson)
                        } else
                            gson.getDelegateAdapter(this, type)
                    }
                })
            .registerTypeAdapter(Uri::class.java, UriTypeAdapter)
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
}
