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
package org.floens.chan.ui.layout;

import static org.floens.chan.Chan.inject;
import static org.floens.chan.ui.theme.ThemeHelper.theme;
import static org.floens.chan.utils.AndroidUtils.dp;
import static org.floens.chan.utils.AndroidUtils.getAttrColor;
import static org.floens.chan.utils.AndroidUtils.getString;
import static org.floens.chan.utils.AndroidUtils.setRoundItemBackground;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.floens.chan.R;
import org.floens.chan.core.model.ChanThread;
import org.floens.chan.core.model.orm.Loadable;
import org.floens.chan.core.presenter.ReplyPresenter;
import org.floens.chan.core.settings.ChanSettings;
import org.floens.chan.core.site.SiteAuthentication;
import org.floens.chan.core.site.http.Reply;
import org.floens.chan.ui.activity.ImagePickDelegate;
import org.floens.chan.ui.activity.StartActivity;
import org.floens.chan.ui.captcha.AuthenticationLayoutCallback;
import org.floens.chan.ui.captcha.AuthenticationLayoutInterface;
import org.floens.chan.ui.captcha.CaptchaLayout;
import org.floens.chan.ui.captcha.GenericWebViewAuthenticationLayout;
import org.floens.chan.ui.captcha.LegacyCaptchaLayout;
import org.floens.chan.ui.captcha.NewCaptchaLayout;
import org.floens.chan.ui.captcha.v1.CaptchaNojsLayoutV1;
import org.floens.chan.ui.captcha.v2.CaptchaNoJsLayoutV2;
import org.floens.chan.ui.drawable.DropdownArrowDrawable;
import org.floens.chan.ui.helper.HintPopup;
import org.floens.chan.ui.view.FloatingMenu;
import org.floens.chan.ui.view.FloatingMenuItem;
import org.floens.chan.ui.view.LoadView;
import org.floens.chan.ui.view.SelectionListeningEditText;
import org.floens.chan.utils.AndroidUtils;
import org.floens.chan.utils.ImageDecoder;
import org.floens.chan.utils.TLSSocketFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ReplyLayout extends LoadView implements
        View.OnClickListener,
        View.OnLongClickListener,
        ReplyPresenter.ReplyPresenterCallback,
        TextWatcher,
        ImageDecoder.ImageDecoderCallback,
        SelectionListeningEditText.SelectionChangedListener {
    @Inject
    ReplyPresenter presenter;

    private ReplyLayoutCallback callback;
    private boolean newCaptcha;

    private AuthenticationLayoutInterface authenticationLayout;
    private boolean openingName;

    private boolean blockSelectionChange = false;

    // Progress view (when sending request to the server)
    private View progressLayout;
    private TextView currentProgress;

    // Reply views:
    private View replyInputLayout;
    private TextView message;
    private EditText name;
    private EditText subject;
    private Button flag;
    private EditText options;
    private EditText fileName;
    private LinearLayout nameOptions;
    private ViewGroup commentButtons;
    private Button commentQuoteButton;
    private Button commentSpoilerButton;
    private SelectionListeningEditText comment;
    private TextView commentCounter;
    private CheckBox spoiler;
    private LinearLayout previewHolder;
    private ImageView preview;
    private TextView previewMessage;
    private ImageView attach;
    private ImageView more;
    private ImageView submit;
    private DropdownArrowDrawable moreDropdown;

    // Captcha views:
    private FrameLayout captchaContainer;
    private ImageView captchaHardReset;

    private Runnable closeMessageRunnable = new Runnable() {
        @Override
        public void run() {
            message.setVisibility(View.GONE);
        }
    };

    public ReplyLayout(Context context) {
        this(context, null);
    }

    public ReplyLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReplyLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        inject(this);

        final LayoutInflater inflater = LayoutInflater.from(getContext());

        // Inflate reply input
        replyInputLayout = inflater.inflate(R.layout.layout_reply_input, this, false);
        message = replyInputLayout.findViewById(R.id.message);
        name = replyInputLayout.findViewById(R.id.name);
        subject = replyInputLayout.findViewById(R.id.subject);
        flag = replyInputLayout.findViewById(R.id.flag_button);
        options = replyInputLayout.findViewById(R.id.options);
        fileName = replyInputLayout.findViewById(R.id.file_name);
        nameOptions = replyInputLayout.findViewById(R.id.name_options);
        commentButtons = replyInputLayout.findViewById(R.id.comment_buttons);
        commentQuoteButton = replyInputLayout.findViewById(R.id.comment_quote);
        commentSpoilerButton = replyInputLayout.findViewById(R.id.comment_spoiler);
        comment = replyInputLayout.findViewById(R.id.comment);
        commentCounter = replyInputLayout.findViewById(R.id.comment_counter);
        spoiler = replyInputLayout.findViewById(R.id.spoiler);
        preview = replyInputLayout.findViewById(R.id.preview);
        previewHolder = replyInputLayout.findViewById(R.id.preview_holder);
        previewMessage = replyInputLayout.findViewById(R.id.preview_message);
        attach = replyInputLayout.findViewById(R.id.attach);
        more = replyInputLayout.findViewById(R.id.more);
        submit = replyInputLayout.findViewById(R.id.submit);

        progressLayout = inflater.inflate(R.layout.layout_reply_progress, this, false);
        currentProgress = progressLayout.findViewById(R.id.current_progress);

        // Setup reply layout views
        flag.setOnClickListener(v -> {
            Map<String, String> boardFlags = presenter.getBoardFlags();
            if (boardFlags.isEmpty()) return;
            List<String> sorted;
            if (boardFlags.containsKey("TWI")) {
                String[] sortedMLP = {"AJ","FL","PI","RD","RAR","TWI",
                        "4CC","AN","ANF","APB","AU","BS","BP","BM","BB","CL","CHE","CB","CO","CG",
                        "DD","DAY","DER","DT","DIS","FAU","FLE","GI","LI","LT","LY","MA","MAU",
                        "MIN","NI","NUR","OCT","PAR","PM","PC","PCE","PLU","QC","RLU","S1L","SCO",
                        "SHI","SIL","SP","SPI","STA","STL","SUN","SWB","TS","TX","VS","ZE",
                        "HT","IZ","PP","SPT","SS","ZS",
                        "TFA","TFO","TFP","TP","TFS","TFT","TFV","ADA","AB","SON","SUS",
                        "EQA","EQF","EQP","EQR","ERA","EQS","EQT","EQI"};
                //if (boardFlags.containsKey("HT")) {
                    boardFlags.put("HT", "G5 Hitch Trailblazer");
                    boardFlags.put("IZ", "G5 Izzy Moonbow");
                    boardFlags.put("PP", "G5 Pipp Petals");
                    boardFlags.put("SPT", "G5 Sprout");
                    boardFlags.put("SS", "G5 Sunny Starscout");
                    boardFlags.put("ZS", "G5 Zipp Storm");
                //}
                sorted = new ArrayList<>(Arrays.asList(sortedMLP));
                sorted.retainAll(boardFlags.keySet());
                if (sorted.size() < boardFlags.keySet().size()) {
                    List<String> temp = new ArrayList<>(boardFlags.keySet());
                    temp.removeAll(sorted);
                    sorted.addAll(temp);
                }
            } else {
                String[] keys = boardFlags.keySet().toArray(new String[0]);
                Arrays.sort(keys);
                sorted = Arrays.asList(keys);
            }

            List<FloatingMenuItem> items = new ArrayList<>(boardFlags.size()+1);
            items.add(new FloatingMenuItem(null, "No flag"));
            FloatingMenuItem selected = null;
            for (String key : sorted) {
                FloatingMenuItem flagItem = new FloatingMenuItem(key, boardFlags.get(key));
                if (key.contentEquals(flag.getText())) {
                    selected = flagItem;
                }
                items.add(flagItem);
            }
            FloatingMenu menu = new FloatingMenu(getContext(), flag, items);
            menu.setAnchor(flag, Gravity.CENTER, 0, 0);
            menu.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return items.size();
                }

                @Override
                public String getItem(int position) {
                    return items.get(position).getText();
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    TextView textView = (TextView) (convertView != null
                            ? convertView
                            : LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.toolbar_menu_item, parent, false));
                    textView.setText(getItem(position));
                    return textView;
                }
            });
            menu.setSelectedItem(selected);
            menu.setCallback(new FloatingMenu.FloatingMenuCallback() {
                @Override
                public void onFloatingMenuItemClicked(FloatingMenu menu, FloatingMenuItem item) {
                    flag.setText((String) item.getId());
                }

                @Override
                public void onFloatingMenuDismissed(FloatingMenu menu) { }
            });
            menu.setPopupHeight(dp(300));
            menu.show();
        });
        commentQuoteButton.setOnClickListener(this);
        commentSpoilerButton.setOnClickListener(this);

        comment.addTextChangedListener(this);
        comment.setSelectionChangedListener(this);

        previewHolder.setOnClickListener(this);

        moreDropdown = new DropdownArrowDrawable(dp(16), dp(16), true,
                getAttrColor(getContext(), R.attr.dropdown_dark_color),
                getAttrColor(getContext(), R.attr.dropdown_dark_pressed_color));
        more.setImageDrawable(moreDropdown);
        setRoundItemBackground(more);
        more.setOnClickListener(this);

        theme().imageDrawable.apply(attach);
        setRoundItemBackground(attach);
        attach.setOnClickListener(this);
        attach.setOnLongClickListener(this);

        theme().sendDrawable.apply(submit);
        setRoundItemBackground(submit);
        submit.setOnClickListener(this);

        // Inflate captcha layout
        captchaContainer = (FrameLayout) inflater.inflate(R.layout.layout_reply_captcha, this, false);
        captchaHardReset = captchaContainer.findViewById(R.id.reset);

        // Setup captcha layout views
        captchaContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        theme().refreshDrawable.apply(captchaHardReset);
        setRoundItemBackground(captchaHardReset);
        captchaHardReset.setOnClickListener(this);

        setView(replyInputLayout);

        // Presenter
        presenter.create(this);
    }

    public void setCallback(ReplyLayoutCallback callback) {
        this.callback = callback;
    }

    public ReplyPresenter getPresenter() {
        return presenter;
    }

    public void onOpen(boolean open) {
        presenter.onOpen(open);
    }

    public void bindLoadable(Loadable loadable) {
        presenter.bindLoadable(loadable);
    }

    public void cleanup() {
        presenter.unbindLoadable();
        removeCallbacks(closeMessageRunnable);
    }

    public boolean onBack() {
        return presenter.onBack();
    }

    private void setWrap(boolean wrap) {
        setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                wrap ? LayoutParams.WRAP_CONTENT : LayoutParams.MATCH_PARENT
        ));
    }

    @Override
    public void onClick(View v) {
        if (v == more) {
            presenter.onMoreClicked();
        } else if (v == attach) {
            presenter.onAttachClicked();
        } else if (v == submit) {
            presenter.onSubmitClicked();
        } else if (v == previewHolder) {
            AndroidUtils.hideKeyboard(this);
            callback.showImageReencodingWindow();
        } else if (v == captchaHardReset) {
            if (authenticationLayout != null) {
                authenticationLayout.hardReset();
            }
        } else if (v == commentQuoteButton) {
            presenter.commentQuoteClicked();
        } else if (v == commentSpoilerButton) {
            presenter.commentSpoilerClicked();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        try {
            Context context = getContext();
            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData primary = clipboardManager.getPrimaryClip();
            String clipboardContent = primary != null ? primary.getItemAt(0).coerceToText(context).toString() : "";
            final URL clipboardURL = new URL(clipboardContent);
            final File cacheFile = new File(context.getCacheDir(), "picked_file_dl");

            Request request = new Request.Builder().url(clipboardURL).build();
            OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
            if (Build.VERSION.SDK_INT < 22) {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.
                        getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init((KeyStore) null);
                okHttpClientBuilder.sslSocketFactory(TLSSocketFactory.getInstance(),
                        (X509TrustManager) trustManagerFactory.getTrustManagers()[0]);
            }

            final Handler handler = new Handler(Looper.getMainLooper());
            okHttpClientBuilder.build().newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    handler.post(() -> Toast.makeText(getContext(), "URL download failed", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    int total = 0;
                    if (response.isSuccessful()) {
                        InputStream is = response.body().byteStream();
                        OutputStream os = new FileOutputStream(cacheFile);
                        int read;
                        byte[] buffer = new byte[8192];
                        while ((read = is.read(buffer)) != -1) {
                            total += read;
                            if (total > 26214400) { // 25 MB
                                total = -1;
                                break;
                            } else {
                                os.write(buffer, 0, read);
                            }
                        }
                        os.close();
                    }
                    response.close();

                    if (total > 0) {
                        handler.post(() -> {
                            String[] filenameParts = clipboardURL.getFile().split("/");
                            String filename = filenameParts[filenameParts.length - 1].trim();
                            if (filename.length() == 0) {
                                filename = "file";
                            }
                            presenter.onFilePicked(filename, cacheFile);
                            //Toast.makeText(getContext(), "URL download succeeded", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        handler.post(() -> Toast.makeText(getContext(), "URL download failed", Toast.LENGTH_SHORT).show());
                    }
                }

            });
            Toast.makeText(getContext(), "Downloading URL...", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public void initializeAuthentication(Loadable loadable,
                                         SiteAuthentication authentication,
                                         AuthenticationLayoutCallback callback,
                                         boolean useV2NoJsCaptcha) {
        if (authenticationLayout == null) {
            switch (authentication.type) {
                case CAPTCHA1: {
                    final LayoutInflater inflater = LayoutInflater.from(getContext());
                    authenticationLayout = (LegacyCaptchaLayout) inflater.inflate(
                            R.layout.layout_captcha_legacy, captchaContainer, false);
                    break;
                }
                case CAPTCHA2: {
                    authenticationLayout = new CaptchaLayout(getContext());
                    break;
                }
                case CAPTCHA2_NOJS:
                    if (useV2NoJsCaptcha) {
                        // new captcha window without webview
                        authenticationLayout = new CaptchaNoJsLayoutV2(getContext());
                    } else {
                        // default webview-based captcha view
                        authenticationLayout = new CaptchaNojsLayoutV1(getContext());
                    }

                    ImageView resetButton = captchaContainer.findViewById(R.id.reset);
                    if (resetButton != null) {
                        if (useV2NoJsCaptcha) {
                            // we don't need the default reset button because we have our own
                            resetButton.setVisibility(View.GONE);
                        } else {
                            // restore the button's visibility when using old v1 captcha view
                            resetButton.setVisibility(View.VISIBLE);
                        }
                    }

                    break;
                case GENERIC_WEBVIEW: {
                    GenericWebViewAuthenticationLayout view = new GenericWebViewAuthenticationLayout(getContext());

                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT
                    );
//                    params.setMargins(dp(8), dp(8), dp(8), dp(200));
                    view.setLayoutParams(params);

                    authenticationLayout = view;
                    break;
                }
                case NEW_CAPTCHA: {
                    authenticationLayout = new NewCaptchaLayout(getContext());
                    break;
                }
                case NONE:
                default: {
                    throw new IllegalArgumentException();
                }
            }

            captchaContainer.addView((View) authenticationLayout, 0);
        }

        if (!(authenticationLayout instanceof LegacyCaptchaLayout)) {
            AndroidUtils.hideKeyboard(this);
        }

        authenticationLayout.initialize(loadable, callback);
        authenticationLayout.reset();
    }

    @Override
    public void setPage(ReplyPresenter.Page page, boolean animate) {
        switch (page) {
            case LOADING:
                setWrap(true);
                View progressBar = setView(progressLayout);
                progressBar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp(100)));

                //reset progress to 0 upon uploading start
                currentProgress.setVisibility(View.INVISIBLE);
                break;
            case INPUT:
                setView(replyInputLayout);
                setWrap(!presenter.isExpanded());
                break;
            case AUTHENTICATION:
                setWrap(false);

                setView(captchaContainer);

                captchaContainer.requestFocus(View.FOCUS_DOWN);

                break;
        }

        if (page != ReplyPresenter.Page.AUTHENTICATION && authenticationLayout != null) {
            AndroidUtils.removeFromParentView((View) authenticationLayout);
            authenticationLayout = null;
        }
    }

    @Override
    public void resetAuthentication() {
        authenticationLayout.reset();
    }

    @Override
    public void destroyCurrentAuthentication() {
        if (authenticationLayout == null) {
            return;
        }

        authenticationLayout.onDestroy();
        captchaContainer.removeView((View) authenticationLayout);
        authenticationLayout = null;
    }

    @Override
    public void loadDraftIntoViews(Reply draft) {
        name.setText(draft.name);
        subject.setText(draft.subject);
        flag.setText(draft.flag);
        options.setText(draft.options);
        blockSelectionChange = true;
        comment.setText(draft.comment);
        comment.setSelection(draft.selectionStart, draft.selectionEnd);
        blockSelectionChange = false;
        fileName.setText(draft.fileName);
        spoiler.setChecked(draft.spoilerImage);
    }

    @Override
    public void loadViewsIntoDraft(Reply draft) {
        draft.name = name.getText().toString();
        draft.subject = subject.getText().toString();
        draft.flag = flag.getText().toString();
        draft.options = options.getText().toString();
        draft.comment = comment.getText().toString();
        draft.selectionStart = comment.getSelectionStart();
        draft.selectionEnd = comment.getSelectionEnd();
        draft.fileName = fileName.getText().toString();
        draft.spoilerImage = spoiler.isChecked();
    }

    @Override
    public void openMessage(boolean open, boolean animate, String text, boolean autoHide) {
        removeCallbacks(closeMessageRunnable);
        message.setText(text);
        message.setVisibility(open ? View.VISIBLE : View.GONE);

        if (autoHide) {
            postDelayed(closeMessageRunnable, 5000);
        }
    }

    @Override
    public void onPosted() {
        Toast.makeText(getContext(), R.string.reply_success, Toast.LENGTH_SHORT).show();
        callback.openReply(false);
        callback.requestNewPostLoad();
    }

    @Override
    public void setCommentHint(String hint) {
        comment.setHint(hint);
    }

    @Override
    public void showCommentCounter(boolean show) {
        commentCounter.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showFlag(boolean show) {
        flag.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setExpanded(boolean expanded) {
        setWrap(!expanded);

        comment.setMaxLines(expanded ? 500 : 6);

        previewHolder.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                expanded ? dp(150) : dp(100)
        ));

        ValueAnimator animator = ValueAnimator.ofFloat(expanded ? 0f : 1f, expanded ? 1f : 0f);
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.setDuration(400);
        animator.addUpdateListener(animation ->
                moreDropdown.setRotation((float) animation.getAnimatedValue()));
        animator.start();
    }

    @Override
    public void openNameOptions(boolean open) {
        openingName = open;
        nameOptions.setVisibility(open ? View.VISIBLE : View.GONE);
    }

    @Override
    public void openSubject(boolean open) {
        subject.setVisibility(open ? View.VISIBLE : View.GONE);
    }

    @Override
    public void openCommentQuoteButton(boolean open) {
        commentQuoteButton.setVisibility(open ? View.VISIBLE : View.GONE);
    }

    @Override
    public void openCommentSpoilerButton(boolean open) {
        commentSpoilerButton.setVisibility(open ? View.VISIBLE : View.GONE);
    }

    @Override
    public void openFileName(boolean open) {
        fileName.setVisibility(open ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setFileName(String name) {
        fileName.setText(name);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void updateCommentCount(int count, int maxCount, boolean over) {
        commentCounter.setText(count + "/" + maxCount);
        //noinspection ResourceAsColor
        commentCounter.setTextColor(over ? 0xffff0000 : getAttrColor(getContext(), R.attr.text_color_secondary));
    }

    public void focusComment() {
        comment.post(() -> AndroidUtils.requestViewAndKeyboardFocus(comment));
    }

    @Override
    public void onFallbackToV1CaptchaView() {
        // fallback to v1 captcha window
        presenter.switchPage(ReplyPresenter.Page.AUTHENTICATION, true, false);
    }

    @Override
    public void openPreview(boolean show, File previewFile) {
        if (show) {
            theme().clearDrawable.apply(attach);
        } else {
            theme().imageDrawable.apply(attach);
        }

        if (show) {
            ImageDecoder.decodeFileOnBackgroundThread(previewFile, dp(400), dp(300), this);
        } else {
            spoiler.setVisibility(View.GONE);
            previewHolder.setVisibility(View.GONE);
            previewMessage.setVisibility(View.GONE);
        }
    }

    @Override
    public void openPreviewMessage(boolean show, String message) {
        previewMessage.setVisibility(show ? VISIBLE : GONE);
        previewMessage.setText(message);
    }

    @Override
    public void openSpoiler(boolean show, boolean checked) {
        spoiler.setVisibility(show ? View.VISIBLE : View.GONE);
        spoiler.setChecked(checked);
    }

    @Override
    public void onImageBitmap(File file, Bitmap bitmap) {
        if (bitmap != null) {
            preview.setImageBitmap(bitmap);
            previewHolder.setVisibility(View.VISIBLE);

            showReencodeImageHint();
        } else {
            openPreviewMessage(true, getString(R.string.reply_no_preview));
        }
    }

    @Override
    public void onFilePickLoading() {
        // TODO
    }

    @Override
    public void onFilePickError() {
        Toast.makeText(getContext(), R.string.reply_file_open_failed, Toast.LENGTH_LONG).show();
    }

    @Override
    public void highlightPostNo(int no) {
        callback.highlightPostNo(no);
    }

    @Override
    public void onSelectionChanged(int selStart, int selEnd) {
        if (!blockSelectionChange) {
            presenter.onSelectionChanged();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        presenter.onCommentTextChanged(comment.getText());
    }

    @Override
    public void showThread(Loadable loadable) {
        callback.showThread(loadable);
    }

    @Override
    public ImagePickDelegate getImagePickDelegate() {
        return ((StartActivity) getContext()).getImagePickDelegate();
    }

    @Override
    public ChanThread getThread() {
        return callback.getThread();
    }

    @Override
    public void onUploadingProgress(int percent) {
        if (currentProgress != null) {
            if (percent <= 0) {
                currentProgress.setVisibility(View.VISIBLE);
            }

            currentProgress.setText(String.format(Locale.getDefault(), "%d", percent));
        }
    }

    public void onImageOptionsApplied(Reply reply) {
        // Update the filename EditText. Otherwise it will change back the image name upon changing
        // the message comment (because of the textwatcher)
        fileName.setText(reply.fileName);

        presenter.onImageOptionsApplied(reply);
    }

    private void showReencodeImageHint() {
        if (!ChanSettings.reencodeHintShown.get()) {
            String message = getContext().getString(R.string.click_image_for_extra_options);
            HintPopup hintPopup = HintPopup.show(getContext(), preview, message, dp(-32), dp(16));
            hintPopup.wiggle();

            ChanSettings.reencodeHintShown.set(true);
        }
    }

    public interface ReplyLayoutCallback {
        void highlightPostNo(int no);

        void openReply(boolean open);

        void showThread(Loadable loadable);

        void requestNewPostLoad();

        ChanThread getThread();

        void showImageReencodingWindow();
    }
}
