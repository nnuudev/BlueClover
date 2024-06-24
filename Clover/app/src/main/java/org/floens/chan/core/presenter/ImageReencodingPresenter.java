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
package org.floens.chan.core.presenter;

import static org.floens.chan.Chan.inject;
import static org.floens.chan.utils.AndroidUtils.dp;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import org.floens.chan.core.manager.ReplyManager;
import org.floens.chan.core.model.orm.Loadable;
import org.floens.chan.core.site.http.Reply;
import org.floens.chan.utils.BackgroundUtils;
import org.floens.chan.utils.BitmapUtils;
import org.floens.chan.utils.ImageDecoder;
import org.floens.chan.utils.Logger;

import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class ImageReencodingPresenter {
    private final static String TAG = "ImageReencodingPresenter";

    @Inject
    ReplyManager replyManager;

    private static final int DECODED_IMAGE_WIDTH = 340;
    private static final int DECODED_IMAGE_HEGIHT = 180;

    private Executor executor = Executors.newSingleThreadExecutor();
    private ImageReencodingPresenterCallback callback;
    private Loadable loadable;
    private ImageOptions imageOptions;
    private BackgroundUtils.Cancelable cancelable;

    public ImageReencodingPresenter(ImageReencodingPresenterCallback callback, Loadable loadable) {
        inject(this);

        this.loadable = loadable;
        this.callback = callback;
        this.imageOptions = new ImageOptions();
    }

    public void onDestroy() {
        synchronized (this) {
            if (cancelable != null) {
                cancelable.cancel();
                cancelable = null;
            }
        }
    }

    public void loadImagePreview() {
        Reply reply = replyManager.getReply(loadable);

        ImageDecoder.decodeFileOnBackgroundThread(
                reply.file,
                dp(DECODED_IMAGE_WIDTH),
                dp(DECODED_IMAGE_HEGIHT),
                (file, bitmap) -> {
                    if (bitmap == null) {
                        callback.showCouldNotDecodeBitmapError();
                        return;
                    }

                    callback.showImagePreview(bitmap);
                });
    }

    @Nullable
    public Bitmap.CompressFormat getImageFormat() {
        try {
            Reply reply = replyManager.getReply(loadable);
            return BitmapUtils.getImageFormat(reply.file);
        } catch (IOException e) {
            Logger.e(TAG, "Error while trying to get image format", e);
            return null;
        }
    }

    public void setReencode(@Nullable Reencode reencode) {
        if (reencode != null) {
            imageOptions.setReencode(reencode);
        } else {
            imageOptions.setReencode(null);
        }
    }

    public void fixExif(boolean isChecked) {
        imageOptions.setFixExif(isChecked);
    }

    public void removeMetadata(boolean isChecked) {
        imageOptions.setRemoveMetadata(isChecked);
    }

    public void removeFilename(boolean isChecked) {
        imageOptions.setRemoveFilename(isChecked);
    }

    public void changeImageChecksum(boolean isChecked) {
        imageOptions.setChangeImageChecksum(isChecked);
    }

    public void applyImageOptions() {
        Reply reply;

        synchronized (this) {
            if (cancelable != null) {
                return;
            }

            reply = replyManager.getReply(loadable);
        }

        Logger.d(TAG, "imageOptions: [" + imageOptions.toString() + "]");

        //all options are default - do nothing
        if (!imageOptions.getRemoveFilename()
                && !imageOptions.getFixExif()
                && !imageOptions.getRemoveMetadata()
                && !imageOptions.getChangeImageChecksum()
                && imageOptions.getReencode() == null) {
            callback.onImageOptionsApplied(reply);
            return;
        }

        //only the "remove filename" option is selected
        if (imageOptions.getRemoveFilename()
                && !imageOptions.getFixExif()
                && !imageOptions.getRemoveMetadata()
                && !imageOptions.getChangeImageChecksum()
                && imageOptions.getReencode() == null) {
            reply.fileName = getNewImageName();
            callback.onImageOptionsApplied(reply);
            return;
        }

        //one of the options that affects the image is selected (reencode/remove metadata/change checksum)
        BackgroundUtils.Cancelable localCancelable = BackgroundUtils.runWithExecutor(executor, () -> {
            try {
                callback.disableOrEnableButtons(false);

                reply.file = BitmapUtils.reencodeBitmapFile(
                        reply.file,
                        imageOptions.getFixExif(),
                        imageOptions.getRemoveMetadata(),
                        imageOptions.getChangeImageChecksum(),
                        imageOptions.getReencode()
                );

                if (imageOptions.getRemoveFilename()) {
                    reply.fileName = getNewImageName();
                } else if (imageOptions.getReencode() != null && imageOptions.getReencode().getReencodeType() != ReencodeType.AS_IS) {
                    int lastDotInFilename = reply.fileName.lastIndexOf(".");
                    if (lastDotInFilename != -1)
                        reply.fileName = reply.fileName.substring(0, lastDotInFilename);
                    reply.fileName += "." + imageOptions.getReencode().getReencodeType().name().substring(3).toLowerCase(Locale.ENGLISH);
                }
            } catch (Throwable error) {
                Logger.e(TAG, "Error while trying to re-encode bitmap file", error);
                callback.disableOrEnableButtons(true);
                callback.showFailedToReencodeImage(error);
                cancelable = null;
                return;
            } finally {
                callback.disableOrEnableButtons(true);
            }

            callback.onImageOptionsApplied(reply);

            synchronized (this) {
                cancelable = null;
            }
        });

        synchronized (this) {
            cancelable = localCancelable;
        }
    }

    private String getNewImageName() {
        Random random = new Random();
        long now = System.currentTimeMillis();
        long offset = random.nextInt(31536000) * 1000L; // go back up to one year
        long newTime = (now - offset) * 1000L - random.nextInt(1000);
        return String.valueOf(newTime);
    }

    public static class ImageOptions {
        private boolean fixExif;
        private boolean removeMetadata;
        private boolean removeFilename;
        private boolean changeImageChecksum;

        @Nullable
        private Reencode reencode;

        public ImageOptions() {
            this.fixExif = false;
            this.removeMetadata = false;
            this.removeFilename = false;
            this.changeImageChecksum = false;
            this.reencode = null;
        }

        public boolean getFixExif() {
            return fixExif;
        }

        public void setFixExif(boolean fixExif) {
            this.fixExif = fixExif;
        }

        public boolean getRemoveMetadata() {
            return removeMetadata;
        }

        public void setRemoveMetadata(boolean removeMetadata) {
            this.removeMetadata = removeMetadata;
        }

        public boolean getRemoveFilename() {
            return removeFilename;
        }

        public void setRemoveFilename(boolean removeFilename) {
            this.removeFilename = removeFilename;
        }

        @Nullable
        public Reencode getReencode() {
            return reencode;
        }

        public void setReencode(@Nullable Reencode reencode) {
            this.reencode = reencode;
        }

        public boolean getChangeImageChecksum() {
            return changeImageChecksum;
        }

        public void setChangeImageChecksum(boolean changeImageChecksum) {
            this.changeImageChecksum = changeImageChecksum;
        }

        @Override
        public String toString() {
            String reencodeStr = reencode != null ? reencode.toString() : "null";

            return "fixExif = " + fixExif + ", removeMetadata = " + removeMetadata + ", removeFilename = " + removeFilename +
                    ", changeImageChecksum = " + changeImageChecksum + ", " + reencodeStr;
        }
    }

    public static class Reencode {
        private ReencodeType reencodeType;
        private int reencodeQuality;
        private int reduce;

        public Reencode(ReencodeType reencodeType, int reencodeQuality, int reduce) {
            this.reencodeType = reencodeType;
            this.reencodeQuality = reencodeQuality;
            this.reduce = reduce;
        }

        public ReencodeType getReencodeType() {
            return reencodeType;
        }

        public void setReencodeType(ReencodeType reencodeType) {
            this.reencodeType = reencodeType;
        }

        public int getReencodeQuality() {
            return reencodeQuality;
        }

        public void setReencodeQuality(int reencodeQuality) {
            this.reencodeQuality = reencodeQuality;
        }

        public int getReduce() {
            return reduce;
        }

        public void setReduce(int reduce) {
            this.reduce = reduce;
        }

        public boolean isDefault() {
            return reencodeType == ReencodeType.AS_IS && reencodeQuality == 100 && reduce == 1;
        }

        @Override
        public String toString() {
            return "reencodeType = " + reencodeType + ", reencodeQuality = " + reencodeQuality +
                    ", reduce = " + reduce;
        }
    }

    public enum ReencodeType {
        AS_IS,
        AS_JPEG,
        AS_PNG;

        public static ReencodeType fromInt(int value) {
            if (value == AS_IS.ordinal()) {
                return AS_IS;
            } else if (value == AS_PNG.ordinal()) {
                return AS_PNG;
            } else if (value == AS_JPEG.ordinal()) {
                return AS_JPEG;
            }

            throw new RuntimeException("Cannot get ReencodeType from int value: " + value);
        }
    }

    public interface ImageReencodingPresenterCallback {
        void showCouldNotDecodeBitmapError();

        void showImagePreview(Bitmap bitmap);

        void disableOrEnableButtons(boolean enabled);

        void onImageOptionsApplied(Reply reply);

        void showFailedToReencodeImage(Throwable error);
    }
}
