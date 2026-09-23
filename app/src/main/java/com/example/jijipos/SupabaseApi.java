package com.example.jijipos;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

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

    // Cloud read channel: pull a user profile by phone for cross-device login
    @GET("users")
    Call<ResponseBody> fetchUsersByPhone(
            @Header("apikey") String apiKey,
            @Header("Authorization") String bearerAuth,
            @Query("phone_number") String phoneFilter,
            @Query("limit") int limit
    );

    // Cloud read channel: pull a manager profile by phone to validate a cashier invite code
    @GET("users")
    Call<ResponseBody> fetchUsersByPhoneAndRole(
            @Header("apikey") String apiKey,
            @Header("Authorization") String bearerAuth,
            @Query("phone_number") String phoneFilter,
            @Query("role") String roleFilter,
            @Query("limit") int limit
    );

    // Cloud read channel: pull a business profile so a manager's linkage survives a device switch
    @GET("businesses")
    Call<ResponseBody> fetchBusinessById(
            @Header("apikey") String apiKey,
            @Header("Authorization") String bearerAuth,
            @Query("id") String idFilter,
            @Query("limit") int limit
    );
}
