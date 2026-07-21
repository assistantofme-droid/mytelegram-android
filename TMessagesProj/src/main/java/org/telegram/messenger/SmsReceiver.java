/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 *
 * Modified to support custom OTP messages from sms.ir (Persian template
 * "کد ورود شما: 12345" etc.) in addition to the official Telegram format.
 */
package org.telegram.messenger;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.gms.auth.api.phone.SmsRetriever;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsReceiver extends BroadcastReceiver {

    // Accept the last run of 3-8 digits anywhere in the SMS body. Works for:
    //   - Official Telegram format: "MyTelegram code: 12345"
    //   - sms.ir template 986819: "کد ورود شما: 12345" / "Login code: 12345"
    //   - Plain numeric codes: "12345"
    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{3,8}");

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        try {
            String message = "";
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            String hash = preferences.getString("sms_hash", null);
            if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
                if (!AndroidUtilities.isWaitingForSms()) {
                    return;
                }
                Bundle bundle = intent.getExtras();
                message = (String) bundle.get(SmsRetriever.EXTRA_SMS_MESSAGE);
            }
            if (TextUtils.isEmpty(message)) {
                return;
            }
            Matcher matcher = CODE_PATTERN.matcher(message);
            // Find the LAST digit-run in the message — most OTP templates put
            // the code at the end ("... code: 12345").
            String code = null;
            while (matcher.find()) {
                code = matcher.group();
            }
            if (code == null) {
                return;
            }
            code = code.replace("-", "");
            if (code.length() >= 3 && code.length() <= 8) {
                if (hash != null) {
                    preferences.edit().putString("sms_hash_code", hash + "|" + code).commit();
                }
                final String finalCode = code;
                AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didReceiveSmsCode, finalCode));
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
