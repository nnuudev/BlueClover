/*
 * BlueClover - 4chan browser https://github.com/nnuudev/BlueClover
 * Copyright (C) 2021 nnuudev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.floens.chan.ui.captcha;

import static org.floens.chan.utils.AndroidUtils.getAttrColor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import org.floens.chan.BuildConfig;
import org.floens.chan.R;
import org.floens.chan.core.model.orm.Loadable;
import org.floens.chan.core.site.SiteAuthentication;
import org.floens.chan.utils.AndroidUtils;
import org.floens.chan.utils.IOUtils;
import org.floens.chan.utils.Logger;

public class NewCaptchaLayout extends WebView implements AuthenticationLayoutInterface {
    private static final String TAG = "NewShitCaptchaLayout";

    private AuthenticationLayoutCallback callback;
    private String baseUrl;
    private String board;
    private int thread_id;

    public NewCaptchaLayout(Context context) {
        super(context);
    }

    public NewCaptchaLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NewCaptchaLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    public void initialize(Loadable loadable, AuthenticationLayoutCallback callback) {
        this.callback = callback;

        SiteAuthentication authentication = loadable.site.actions().postAuthenticate();
        loadable.site.requestModifier().modifyWebView(this);

        this.baseUrl = authentication.baseUrl;
        this.board = loadable.boardCode;
        this.thread_id = loadable.no;

        //requestDisallowInterceptTouchEvent(true);
        //AndroidUtils.hideKeyboard(this);

        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUserAgentString(BuildConfig.USER_AGENT);

        setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(@NonNull ConsoleMessage consoleMessage) {
                Logger.i(TAG, consoleMessage.lineNumber() + ":" + consoleMessage.message()
                        + " " + consoleMessage.sourceId());
                return true;
            }
        });
        setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (Uri.parse(url).getHost().equals(Uri.parse(NewCaptchaLayout.this.baseUrl).getHost())) {
                    return false;
                } else {
                    AndroidUtils.openLink(url);
                    return true;
                }
            }
        });
        setBackgroundColor(getAttrColor(getContext(), R.attr.backcolor));

        addJavascriptInterface(new CaptchaInterface(this), "CaptchaCallback");
    }

    @Override
    public void reset() {
        hardReset();
    }

    @Override
    public void hardReset() {
        String html = IOUtils.assetAsString(getContext(), "captcha/new_captcha.html");
        html = html.replace("__board__", board).replace("__thread_id__", String.valueOf(thread_id));

        loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
    }

    @Override
    public boolean requireResetAfterComplete() {
        return false;
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new BaseInputConnection(this, false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // captcha is 300px x 150px, and I scale it to the 75% of the screen
            // therefore the height is the 37.5% of the screen
            float realHeight = getMeasuredWidth() * 0.375f;
            // don't let the user swipe the screen if he's touching the part
            // of the screen where the captcha actually is
            if (event.getY() < realHeight) {
                // there be dragons
                ViewParent disallowHere = this;
                while (disallowHere != null) {
                    disallowHere.requestDisallowInterceptTouchEvent(true);
                    disallowHere = disallowHere.getParent();
                }
            }
        }
        return super.onTouchEvent(event);
    }

    private void onCaptchaLoaded() {
        AndroidUtils.requestViewAndKeyboardFocus(this);
    }

    private void onCaptchaEntered(String challenge, String response) {
        //if (TextUtils.isEmpty(response) && !challenge.equals("noop")) {
        //    reset();
        //} else {
            callback.onAuthenticationComplete(this, challenge, response);
        //}
    }

    public static class CaptchaInterface {
        private final NewCaptchaLayout layout;

        public CaptchaInterface(NewCaptchaLayout layout) {
            this.layout = layout;
        }

        @JavascriptInterface
        public void onCaptchaLoaded() {
            AndroidUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    layout.onCaptchaLoaded();
                }
            });
        }

        @JavascriptInterface
        public void onCaptchaEntered(final String challenge, final String response) {
            AndroidUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    layout.onCaptchaEntered(challenge, response);
                }
            });
        }
    }
}
