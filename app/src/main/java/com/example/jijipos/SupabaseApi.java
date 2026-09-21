package com.example.jijipos;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface SupabaseApi {

    // Cloud upload channel for provisioning store profiles
    @POST("businesses")
    Call<ResponseBody> syncBusinessToCloud(
            @Header("apikey") String apiKey,
            @Header("Authorization") String bearerAuth,
            @Header("Content-Type") String contentType,
            @Header("Prefer") String representation,
            @Body okhttp3.RequestBody jsonPayload
    );

    // Cloud upload channel for user/employee records
    @POST("users")
    Call<ResponseBody> syncUserToCloud(
            @Header("apikey") String apiKey,
            @Header("Authorization") String bearerAuth,
            @Header("Content-Type") String contentType,
            @Header("Prefer") String representation,
            @Body okhttp3.RequestBody jsonPayload
    );

    // Cloud upload channel for real-time sales tickets copy retention
    @POST("transactions")
    Call<ResponseBody> syncTransactionToCloud(
            @Header("apikey") String apiKey,
            @Header("Authorization") String bearerAuth,
            @Header("Content-Type") String contentType,
            @Header("Prefer") String representation,
            @Body okhttp3.RequestBody jsonPayload
    );
}
