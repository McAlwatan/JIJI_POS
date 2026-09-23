package com.example.jijipos;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

public class SupabaseClient {

    private static final String BASE_URL = "https://vgrdweyrdakbcancumka.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZncmR3ZXlyZGFrYmNhbmN1bWthIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk5ODU0NTUsImV4cCI6MjEwNTU2MTQ1NX0.4sISJ2bEEai4tfk2izHaHJMPQydBaQ0BF35oOsZoJU4";

    private static SupabaseClient instance;
    private final SupabaseApi supabaseApi;

    private SupabaseClient() {
        // Intercept network streams to print diagnostic transaction logs to Logcat
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        supabaseApi = retrofit.create(SupabaseApi.class);
    }

    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }

    public interface CloudSyncCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    /**
     * Pushes a local data record array object directly up to your remote PostgREST cloud endpoint.
     * @param targetTable "businesses", "users", or "transactions"
     * @param jsonStringPayload Raw structured database object serialized as JSON
     */
    public void pushRecordToCloud(String targetTable, String jsonStringPayload, CloudSyncCallback callback) {
        RequestBody body = RequestBody.create(jsonStringPayload, MediaType.parse("application/json"));
        String authorizationHeader = "Bearer " + API_KEY;
        String preferenceHeader = "return=minimal";

        Call<ResponseBody> call;
        if (targetTable.equalsIgnoreCase("businesses")) {
            call = supabaseApi.syncBusinessToCloud(API_KEY, authorizationHeader, "application/json", preferenceHeader, body);
        } else if (targetTable.equalsIgnoreCase("users")) {
            call = supabaseApi.syncUserToCloud(API_KEY, authorizationHeader, "application/json", preferenceHeader, body);
        } else {
            call = supabaseApi.syncTransactionToCloud(API_KEY, authorizationHeader, "application/json", preferenceHeader, body);
        }

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onFailure("Server rejected request code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    /**
     * Synchronously pulls the raw JSON array for a single user profile matched
     * by phone number. MUST be called from a background thread (uses
     * {@link Call#execute()}). Returns the response body string, or null when
     * the server answered unsuccessfully.
     */
    public String fetchUserJsonByPhone(String phone) throws IOException {
        Call<ResponseBody> call = supabaseApi.fetchUsersByPhone(
                API_KEY, "Bearer " + API_KEY, "eq." + phone, 1);
        Response<ResponseBody> response = call.execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body().string();
        }
        return null;
    }

    /**
     * Synchronously pulls the raw JSON array for a single MANAGER profile matched
     * by phone number, used to validate a cashier invitation code when the manager
     * registered on a different device. MUST be called from a background thread.
     */
    public String fetchManagerJsonByPhone(String phone) throws IOException {
        Call<ResponseBody> call = supabaseApi.fetchUsersByPhoneAndRole(
                API_KEY, "Bearer " + API_KEY, "eq." + phone, "eq.MANAGER", 1);
        Response<ResponseBody> response = call.execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body().string();
        }
        return null;
    }

    /**
     * Synchronously pulls the raw JSON array for a single business by id.
     * MUST be called from a background thread.
     */
    public String fetchBusinessJsonById(long businessId) throws IOException {
        Call<ResponseBody> call = supabaseApi.fetchBusinessById(
                API_KEY, "Bearer " + API_KEY, "eq." + businessId, 1);
        Response<ResponseBody> response = call.execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body().string();
        }
        return null;
    }
}
