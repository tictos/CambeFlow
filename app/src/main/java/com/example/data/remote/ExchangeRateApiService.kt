package com.example.data.remote

import com.example.model.ExchangeRateApiResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface ExchangeRateApiService {
    @GET("v6/latest/{base}")
    suspend fun getLatestRates(
        @Path("base") baseCurrency: String
    ): ExchangeRateApiResponse

    companion object {
        private const val BASE_URL = "https://open.er-api.com/"

        fun create(): ExchangeRateApiService {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(ExchangeRateApiService::class.java)
        }
    }
}
