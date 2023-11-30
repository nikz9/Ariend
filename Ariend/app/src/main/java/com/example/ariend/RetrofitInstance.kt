package com.example.ariend

import com.google.gson.GsonBuilder
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST


data class ResLogin(val res: List<String>)
data class ReqLogin(
    val username: String,
    val password: String,
)

data class ResRegister(val res: String)

data class ResReset(val res: String)
data class ReqReset(
    val username: String,
)

data class ResMessage(val res: String)
data class ReqMessage(
    val username: String,
    val message: String,
)

interface ApiService {
    @POST("login")
    suspend fun login(@Body data: ReqLogin): Response<ResLogin>

    @POST("register")
    suspend fun register(@Body data: ReqLogin): Response<ResRegister>

    @POST("reset")
    suspend fun reset(@Body data: ReqReset): Response<ResReset>

    @POST("message")
    suspend fun message(@Body data: ReqMessage): Response<ResMessage>
}

object RetrofitInstance {
    private const val BASE_URL = "http://192.168.188.54:5000/"

    private var gson = GsonBuilder()
        .setLenient()
        .create()

    val apiService: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        retrofit.create(ApiService::class.java)
    }
}