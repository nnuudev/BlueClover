package org.floens.chan.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.webkit.WebView;

import org.floens.chan.core.settings.ChanSettings;

import java.util.Locale;

public class LocaleUtils {
    public static void overrideLocaleToEnglishIfNeeded(Context context) {
        if (ChanSettings.forceEnglishLocale.get()) {
            new WebView(context).destroy();
            setLocaleToEnglish(context);
        }
    }

    public static Context getEnglishContextIfNeeded(Context context) {
        // ChanSettings isn't available yet
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean("preference_force_english_locale", false)) {
            setLocaleToEnglish(context);
            return context.createConfigurationContext(context.getResources().getConfiguration());
        } else {
            return context;
        }
    }

    private static void setLocaleToEnglish(Context context) {
        Locale.setDefault(Locale.ENGLISH);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();

        configuration.setLocale(Locale.ENGLISH);
        configuration.setLayoutDirection(Locale.ENGLISH);

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }
}
