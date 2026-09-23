package com.example.jijipos;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Single source of truth for the signed-in user session.
 * Every screen that launches DashboardActivity (login, registration,
 * PIN setup, PIN lock) writes/reads the identity through this helper so
 * the role, user id, phone and business id can never get lost in transit
 * and the dashboard can never silently fall back to the customer UI.
 */
public class SessionManager {

    private static final String PREFS_NAME = "JIJI_POS_SECURITY_PREFS";

    public static final String KEY_PIN = "SECURITY_PIN_KEY";
    private static final String KEY_USER_ID = "SAVED_USER_ID";
    private static final String KEY_USER_NAME = "SAVED_USER_NAME";
    private static final String KEY_USER_PHONE = "SAVED_USER_PHONE";
    private static final String KEY_USER_ROLE = "SAVED_USER_ROLE";
    private static final String KEY_BUSINESS_ID = "SAVED_BUSINESS_ID";

    private SessionManager() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void saveSession(Context context, long userId, String name, String phone, String role, long businessId) {
        prefs(context).edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_PHONE, phone)
                .putString(KEY_USER_ROLE, (role != null) ? role.trim().toUpperCase() : "CUSTOMER")
                .putLong(KEY_BUSINESS_ID, businessId)
                .apply();
    }

    public static long getUserId(Context context) {
        return prefs(context).getLong(KEY_USER_ID, 0L);
    }

    public static String getUserName(Context context) {
        return prefs(context).getString(KEY_USER_NAME, null);
    }

    public static String getUserPhone(Context context) {
        return prefs(context).getString(KEY_USER_PHONE, null);
    }

    public static String getUserRole(Context context) {
        return prefs(context).getString(KEY_USER_ROLE, null);
    }

    public static long getBusinessId(Context context) {
        return prefs(context).getLong(KEY_BUSINESS_ID, 0L);
    }

    public static boolean hasPin(Context context) {
        return prefs(context).contains(KEY_PIN);
    }

    /** Stamps the full session onto an intent headed for DashboardActivity. */
    public static Intent attachSession(Context context, Intent intent) {
        intent.putExtra("USER_ID", getUserId(context));
        intent.putExtra("USER_NAME", getUserName(context));
        intent.putExtra("USER_PHONE", getUserPhone(context));
        intent.putExtra("USER_ROLE", getUserRole(context));
        intent.putExtra("BUSINESS_ID", getBusinessId(context));
        return intent;
    }
}
