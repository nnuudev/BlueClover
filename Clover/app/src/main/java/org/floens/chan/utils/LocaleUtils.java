package org.floens.chan.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.webkit.WebView;

import org.floens.chan.core.settings.ChanSettings;

import java.util.Locale;

public class LocaleUtils {
    public static void overrideLocaleToEnglishIfNeeded(Context context) {
        if (ChanSettings.forceEnglishLocale.get()) {
            setLocaleToEnglish(context);
        }
    }

    private static void setLocaleToEnglish(Context context) {
        // Android is so retarded holy shit
        // see https://stackoverflow.com/questions/40398528
        new WebView(context).destroy();

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();

        configuration.setLocale(Locale.ENGLISH);

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }
}
