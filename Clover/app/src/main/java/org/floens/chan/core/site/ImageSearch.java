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
package org.floens.chan.core.site;

import org.floens.chan.BuildConfig;

import java.util.ArrayList;
import java.util.List;

public abstract class ImageSearch {
    public static final List<ImageSearch> engines = new ArrayList<>();

    public abstract int getId();

    public abstract String getName();

    public abstract String getUrl(String imageUrl);

    public boolean showFor(String boardCode) {
        return true;
    }

    static {
        engines.add(new ImageSearch() {
            public int getId() {
                return 0;
            }

            public String getName() {
                return "Google Lens";
            }

            public String getUrl(String imageUrl) {
                return "https://lens.google.com/uploadbyurl?url=" + imageUrl + "&safe=off";
            }
        });

        engines.add(new ImageSearch() {
            public int getId() {
                return 1;
            }

            public String getName() {
                return "Yandex";
            }

            public String getUrl(String imageUrl) {
                return "https://www.yandex.com/images/search?rpt=imageview&url=" + imageUrl;
            }
        });

        engines.add(new ImageSearch() {
            public int getId() {
                return 6;
            }

            public String getName() {
                return "Derpibooru";
            }

            public String getUrl(String imageUrl) {
                return "https://derpibooru.org/search/reverse?url=" + imageUrl;
            }

            @Override
            public boolean showFor(String boardCode) {
                return BuildConfig.FLAVOR.equals("dev") || boardCode.equals("mlp") || boardCode.equals("trash");
            }
        });

        engines.add(new ImageSearch() {
            public int getId() {
                return 7;
            }

            public String getName() {
                return "Furbooru";
            }

            public String getUrl(String imageUrl) {
                return "https://furbooru.org/search/reverse?url=" + imageUrl;
            }

            @Override
            public boolean showFor(String boardCode) {
                return boardCode.equals("trash");
            }
        });

        engines.add(new ImageSearch() {
            public int getId() {
                return 2;
            }

            public String getName() {
                return "iqdb";
            }

            public String getUrl(String imageUrl) {
                return "http://iqdb.org/?url=" + imageUrl;
            }
        });

        engines.add(new ImageSearch() {
            public int getId() {
                return 3;
            }

            public String getName() {
                return "SauceNao";
            }

            public String getUrl(String imageUrl) {
                return "https://saucenao.com/search.php?url=" + imageUrl;
            }
        });

        engines.add(new ImageSearch() {
            public int getId() {
                return 4;
            }

            public String getName() {
                return "TinEye";
            }

            public String getUrl(String imageUrl) {
                return "http://tineye.com/search/?url=" + imageUrl;
            }
        });

        engines.add(new ImageSearch() {
            public int getId() {
                return 5;
            }

            public String getName() {
                return "WAIT";
            }

            public String getUrl(String imageUrl) {
                return "https://trace.moe/?url=" + imageUrl;
            }
        });

    }
}
