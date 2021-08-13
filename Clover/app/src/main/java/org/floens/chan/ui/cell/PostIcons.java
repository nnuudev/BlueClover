package org.floens.chan.ui.cell;

import static org.floens.chan.Chan.injector;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.floens.chan.R;
import org.floens.chan.core.model.PostHttpIcon;
import org.floens.chan.ui.theme.Theme;
import org.floens.chan.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;

public class PostIcons extends View {
    private static final Bitmap stickyIcon;
    private static final Bitmap closedIcon;
    private static final Bitmap trashIcon;
    private static final Bitmap archivedIcon;

    static {
        Resources res = AndroidUtils.getRes();
        stickyIcon = BitmapFactory.decodeResource(res, R.drawable.sticky_icon);
        closedIcon = BitmapFactory.decodeResource(res, R.drawable.closed_icon);
        trashIcon = BitmapFactory.decodeResource(res, R.drawable.trash_icon);
        archivedIcon = BitmapFactory.decodeResource(res, R.drawable.archived_icon);
    }

    static final int STICKY = 0x1;
    static final int CLOSED = 0x2;
    static final int DELETED = 0x4;
    static final int ARCHIVED = 0x8;
    static final int HTTP_ICONS = 0x10;
    static final int HTTP_ICONS_COMPACT = 0x20;

    private int height;
    private int spacing;
    private int icons;
    private int previousIcons;
    private RectF drawRect = new RectF();

    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Rect textRect = new Rect();

    private int httpIconTextColor;
    private int httpIconTextSize;

    private List<PostIcons.PostIconsHttpIcon> httpIcons;

    public PostIcons(Context context) {
        this(context, null);
    }

    public PostIcons(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PostIcons(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        textPaint.setTypeface(Typeface.create((String) null, Typeface.ITALIC));
        setVisibility(View.GONE);
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }

    public void edit() {
        previousIcons = icons;
        httpIcons = null;
    }

    public void apply() {
        if (previousIcons != icons) {
            // Require a layout only if the height changed
            if (previousIcons == 0 || icons == 0) {
                setVisibility(icons == 0 ? View.GONE : View.VISIBLE);
                requestLayout();
            }

            invalidate();
        }
    }

    public void setHttpIcons(List<PostHttpIcon> icons, Theme theme, int size) {
        httpIconTextColor = theme.detailsColor;
        httpIconTextSize = size;
        httpIcons = new ArrayList<>(icons.size());
        for (int i = 0; i < icons.size(); i++) {
            PostHttpIcon icon = icons.get(i);
            PostIcons.PostIconsHttpIcon j = new PostIcons.PostIconsHttpIcon(this, icon.name, icon.url);
            httpIcons.add(j);
            j.request();
        }
    }

    public void cancelRequests() {
        if (httpIcons != null) {
            for (int i = 0; i < httpIcons.size(); i++) {
                PostIcons.PostIconsHttpIcon httpIcon = httpIcons.get(i);
                httpIcon.cancel();
            }
        }
    }

    public void set(int icon, boolean enable) {
        if (enable) {
            icons |= icon;
        } else {
            icons &= ~icon;
        }
    }

    public boolean get(int icon) {
        return (icons & icon) == icon;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measureHeight = icons == 0 ? 0 : (height + getPaddingTop() + getPaddingBottom());

        setMeasuredDimension(widthMeasureSpec, MeasureSpec.makeMeasureSpec(measureHeight, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (icons != 0) {
            canvas.save();
            canvas.translate(getPaddingLeft(), getPaddingTop());

            int offset = 0;

            if (get(STICKY)) {
                offset += drawBitmap(canvas, stickyIcon, offset);
            }

            if (get(CLOSED)) {
                offset += drawBitmap(canvas, closedIcon, offset);
            }

            if (get(DELETED)) {
                offset += drawBitmap(canvas, trashIcon, offset);
            }

            if (get(ARCHIVED)) {
                offset += drawBitmap(canvas, archivedIcon, offset);
            }

            if (get(HTTP_ICONS) || get(HTTP_ICONS_COMPACT)) {
                for (int i = 0; i < httpIcons.size(); i++) {
                    PostIcons.PostIconsHttpIcon httpIcon = httpIcons.get(i);
                    if (httpIcon.bitmap != null) {
                        offset += drawBitmap(canvas, httpIcon.bitmap, offset);

                        if (get(HTTP_ICONS)) {
                            textPaint.setColor(httpIconTextColor);
                            textPaint.setTextSize(httpIconTextSize);
                            textPaint.getTextBounds(httpIcon.name, 0, httpIcon.name.length(), textRect);
                            float y = height / 2f - textRect.exactCenterY();
                            canvas.drawText(httpIcon.name, offset, y, textPaint);
                            offset += textRect.width() + spacing;
                        }
                    }
                }
            }

            canvas.restore();
        }
    }

    private int drawBitmap(Canvas canvas, Bitmap bitmap, int offset) {
        int width = (int) (((float) height / bitmap.getHeight()) * bitmap.getWidth());
        drawRect.set(offset, 0f, offset + width, height);
        canvas.drawBitmap(bitmap, null, drawRect, null);
        return width + spacing;
    }

    private static class PostIconsHttpIcon implements ImageLoader.ImageListener {
        private final PostIcons postIcons;
        private final String name;
        private final HttpUrl url;
        private ImageLoader.ImageContainer request;
        private Bitmap bitmap;

        private PostIconsHttpIcon(PostIcons postIcons, String name, HttpUrl url) {
            this.postIcons = postIcons;
            this.name = name;
            this.url = url;
        }

        private void request() {
            request = injector().instance(ImageLoader.class).get(url.toString(), this);
        }

        private void cancel() {
            if (request != null) {
                request.cancelRequest();
                request = null;
            }
        }

        @Override
        public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
            if (response.getBitmap() != null) {
                bitmap = response.getBitmap();
                postIcons.invalidate();
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {
        }
    }
}
