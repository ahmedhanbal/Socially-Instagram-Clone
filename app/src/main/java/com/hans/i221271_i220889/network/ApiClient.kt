package com.hans.i221271_i220889.network

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    
    private var retrofit: Retrofit? = null
    
    private fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            
            // Add a network interceptor to log the raw response
            val networkInterceptor = HttpLoggingInterceptor { message ->
                if (message.startsWith('{') || message.startsWith('[')) {
                    // This is a JSON response
                    Log.d("ApiClient", "Response: $message")
                } else {
                    // This might be an error message or HTML
                    Log.e("ApiClient", "Unexpected response: $message")
                }
            }
            networkInterceptor.level = HttpLoggingInterceptor.Level.BODY
            
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addNetworkInterceptor(networkInterceptor)
                .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build()
            
            // Create a lenient Gson instance
            val gson = GsonBuilder()
                .setLenient()
                .create()
                
            retrofit = Retrofit.Builder()
                .baseUrl(ApiConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        return retrofit!!
    }
    
    val apiService: ApiService by lazy {
        getRetrofit().create(ApiService::class.java)
    }
}

