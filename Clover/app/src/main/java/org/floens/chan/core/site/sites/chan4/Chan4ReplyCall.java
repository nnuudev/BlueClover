/*
 * Clover - 4chan browser https://github.com/Floens/Clover/
 * Copyright (C) 2014  Floens
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
package org.floens.chan.core.site.sites.chan4;

import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import androidx.annotation.Nullable;

import org.floens.chan.Chan;
import org.floens.chan.core.site.Site;
import org.floens.chan.core.site.common.CommonReplyHttpCall;
import org.floens.chan.core.site.http.ProgressRequestBody;
import org.floens.chan.core.site.http.Reply;

import java.util.Locale;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Chan4ReplyCall extends CommonReplyHttpCall {
    public Chan4ReplyCall(Site site, Reply reply) {
        super(site, reply);
    }

    @Override
    public void addParameters(
            MultipartBody.Builder formBuilder,
            @Nullable ProgressRequestBody.ProgressRequestListener progressListener
    ) {
        formBuilder.addFormDataPart("mode", "regist");
        formBuilder.addFormDataPart("pwd", replyResponse.password);

        if (reply.loadable.isThreadMode()) {
            formBuilder.addFormDataPart("resto", String.valueOf(reply.loadable.no));
        }

        formBuilder.addFormDataPart("name", reply.name);
        formBuilder.addFormDataPart("email", reply.options);

        if (!reply.loadable.isThreadMode() && !TextUtils.isEmpty(reply.subject)) {
            formBuilder.addFormDataPart("sub", reply.subject);
        }

        formBuilder.addFormDataPart("com", reply.comment);

        if (reply.captchaResponse != null) {
            if (reply.captchaChallenge != null) {
                //formBuilder.addFormDataPart("recaptcha_challenge_field", reply.captchaChallenge);
                //formBuilder.addFormDataPart("recaptcha_response_field", reply.captchaResponse);
                formBuilder.addFormDataPart("t-challenge", reply.captchaChallenge);
                formBuilder.addFormDataPart("t-response", reply.captchaResponse);
            } else {
                formBuilder.addFormDataPart("g-recaptcha-response", reply.captchaResponse);
            }
        }

        if (reply.file != null) {
            attachFile(formBuilder, progressListener);
        }

        if (reply.spoilerImage) {
            formBuilder.addFormDataPart("spoiler", "on");
        }

        if (!reply.flag.isEmpty()) {
            formBuilder.addFormDataPart("flag", reply.flag);
        }
    }

    private void attachFile(
            MultipartBody.Builder formBuilder,
            @Nullable ProgressRequestBody.ProgressRequestListener progressListener
    ) {
        RequestBody requestBody;

        if (progressListener == null) {
            requestBody = RequestBody.create(
                    MediaType.parse("application/octet-stream"), reply.file
            );
        } else {
            requestBody = new ProgressRequestBody(RequestBody.create(
                    MediaType.parse("application/octet-stream"), reply.file
            ), progressListener);

        }

        formBuilder.addFormDataPart(
                "upfile",
                reply.fileName,
                requestBody);
    }

    @Override
    public void onResponse(Call call, Response response) {
        super.onResponse(call, response);
        // jesus christ how horrifying
        CookieManager cookieManager = CookieManager.getInstance();
        Headers headers = response.headers();
        for (int i = 0; i < headers.size(); i++) {
            if (headers.name(i).toLowerCase(Locale.ENGLISH).equals("set-cookie")) {
                String val = headers.value(i).split(";")[0];
                cookieManager.setCookie("https://sys.4chan.org", val);
            }
        }
        CookieSyncManager.createInstance(Chan.getInstance());
        CookieSyncManager.getInstance().sync();
    }
}
