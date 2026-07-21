/*
 * This is the source code of Telegram for Android v. 7.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2020.
 *
 * Modified for use with a custom MyTelegram backend server.
 */

package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.android.billingclient.api.ProductDetails;

import java.util.Objects;

public class BuildVars {

    public static boolean DEBUG_VERSION = BuildConfig.DEBUG_VERSION;
    public static boolean LOGS_ENABLED = BuildConfig.DEBUG_VERSION;
    public static boolean DEBUG_PRIVATE_VERSION = BuildConfig.DEBUG_PRIVATE_VERSION;
    public static boolean USE_CLOUD_STRINGS = true;
    public static boolean CHECK_UPDATES = false; // disabled — we run our own backend
    public static boolean NO_SCOPED_STORAGE = Build.VERSION.SDK_INT <= 29;
    public static String BUILD_VERSION_STRING = BuildConfig.BUILD_VERSION_STRING;

    // ============================================================
    // MyTelegram backend configuration
    // ============================================================
    // Replace these with the api_id / api_hash registered on your backend
    // (the mytelegram-dev default is api_id=4 / api_hash from the test config).
    public static int APP_ID = 4;
    public static String APP_HASH = "014b35b6184100b085b0d0572f9b5103";

    // The IP address of your MyTelegram backend (DcOptions[0].IpAddress
    // in appsettings.json).
    public static String BACKEND_HOST = "5.42.217.167";
    // The main DC port (matches DcOptions[0].Port = 20443 in appsettings.json).
    public static int BACKEND_PORT = 20443;

    // SafetyNet / Play Integrity — set to empty to disable Google attestation.
    // Our backend never returns TL_auth_sentCodeTypeFirebaseSms, so we don't
    // actually need this. Leaving it empty disables the entire Firebase flow.
    public static String SAFETYNET_KEY = "";
    public static String PLAYSTORE_APP_URL = "https://play.google.com/store/apps/details?id=org.telegram.messenger";
    public static String HUAWEI_STORE_URL = "https://appgallery.huawei.com/app/C101184875";
    public static String GOOGLE_AUTH_CLIENT_ID = "760348033671-81kmi3pi84p11ub8hp9a1funsv0rn2p9.apps.googleusercontent.com";

    public static String HUAWEI_APP_ID = "101184875";

    // You can use this flag to disable Google Play Billing (If you're making fork and want it to be in Google Play)
    public static boolean IS_BILLING_UNAVAILABLE = false;

    // works only on official app ids, disable on your forks
    public static boolean SUPPORTS_PASSKEYS = false;

    static {
        if (ApplicationLoader.applicationContext != null) {
            SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("systemConfig", Context.MODE_PRIVATE);
            LOGS_ENABLED = DEBUG_VERSION || sharedPreferences.getBoolean("logsEnabled", DEBUG_VERSION);
            if (LOGS_ENABLED) {
                final Thread.UncaughtExceptionHandler pastHandler = Thread.getDefaultUncaughtExceptionHandler();
                Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
                    FileLog.fatal(exception, false);
                    if (pastHandler != null) {
                        pastHandler.uncaughtException(thread, exception);
                    }
                });
            }
        }
    }

    public static boolean useInvoiceBilling() {
        return BillingController.billingClientEmpty || DEBUG_VERSION && false || ApplicationLoader.isStandaloneBuild() || isBetaApp() && false || isHuaweiStoreApp() || hasDirectCurrency();
    }

    private static boolean hasDirectCurrency() {
        if (!BillingController.getInstance().isReady() || BillingController.PREMIUM_PRODUCT_DETAILS == null) {
            return false;
        }
        for (ProductDetails.SubscriptionOfferDetails offerDetails : BillingController.PREMIUM_PRODUCT_DETAILS.getSubscriptionOfferDetails()) {
            for (ProductDetails.PricingPhase phase : offerDetails.getPricingPhases().getPricingPhaseList()) {
                for (String cur : MessagesController.getInstance(UserConfig.selectedAccount).directPaymentsCurrency) {
                    if (Objects.equals(phase.getPriceCurrencyCode(), cur)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Boolean betaApp;
    public static boolean isBetaApp() {
        if (betaApp == null) {
            betaApp = ApplicationLoader.applicationContext != null && "org.telegram.messenger.beta".equals(ApplicationLoader.applicationContext.getPackageName());
        }
        return betaApp;
    }


    public static boolean isHuaweiStoreApp() {
        return ApplicationLoader.isHuaweiStoreBuild();
    }

    /**
     * SMS hash used by Google Play Services SMS Retriever to auto-fill OTP.
     *
     * The hash is derived from your app's package name + signing certificate.
     * See https://developers.google.com/identity/sms-retriever/get-hash
     *
     * IMPORTANT: even if you set this correctly, the SMS body produced by
     * sms.ir (templateId 986819) does NOT include this hash — so auto-fill
     * won't work out of the box. The user will have to type the OTP manually.
     *
     * To enable auto-fill, either:
     *   (a) Edit MyTelegram.SmsSender/AppCodeEventHandler.cs to append the
     *       11-char app hash to the SMS body, or
     *   (b) Compute the hash for your app and set it below (then update the
     *       backend SMS template to include it).
     *
     * Returning null here disables the auto-fill registration entirely —
     * which is the safest default for a fork without the official hash.
     */
    public static String getSmsHash() {
        // Returning null causes LoginActivity to skip allow_app_hash / allow_firebase
        // in auth.sendCode's TL_codeSettings, which means the backend won't try to
        // ask us for Play Integrity / SafetyNet attestation.
        return null;
    }
}
