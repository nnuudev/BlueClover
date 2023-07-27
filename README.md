# Blue Clover - imageboard browser for Android

---

## DEPRECATION NOTICE

On August 25th 2022, 4chan changed the captcha *again*, breaking most third-party apps *again*. At the time of writing this notice, the only app that seems to work correctly is [KurobaEx](https://github.com/K1rakishou/Kuroba-Experimental), which also includes an automatic captcha solver, therefore it's highly recommended to just switch to that app.

A ~~final~~\* update has been released as a last-resort attempt to make Blue Clover usable, but its use is completely discouraged. Some key points about this update:

- The Cloudflare cookie is stored only *temporarily*. When you close the app, the cookie is discarded, and the app will try to solve the Cloudflare challenge again the next time you request a captcha. Keep in mind that Cloudflare also forces a new check every time your phone gets a new IP anyway.
- The challenge is solved using the phone's native WebView. It usually takes 5-20 seconds, but some old versions of Android seem to have problems with it and it might take longer than one minute or even completely refuse to solve the challenge at all. You can try updating Android System WebView, but it's not guaranteed to work.
- **This version will not work with Android 4.4.** Since the app was specifically created to maintain compatibility with KitKat (see below), this change completely defeated the original purpose of the app and therefore this repository will not be updated again. *For real life.*

\***(2023-07-27):** Recent changes to Cloudflare made it impossible to get a captcha with the so-called *final* version mentioned above. For the sake of fighting the establishment, a new option to set a custom User-Agent was added under Settings > Behavior. The default settings *might* work on some devices, but it's not guaranteed that this app can actually post anymore.

***See ya!***

![](docs/2389135.gif)

---

### Download APK: [ [latest release](https://github.com/nnuudev/BlueClover/releases/latest) | [all releases](https://github.com/nnuudev/BlueClover/releases) ]

Blue Clover is a fast Android app for browsing 4chan on Android. It adds inline replying, thread watching, notifications, themes, filters and a whole lot more. Blue Clover is licensed under the GPL and will always be free.

The app is based on [Clover-dev 3.0.2 0e32fb7](https://github.com/chandevel/Clover/commit/0e32fb74d5ea4fbfe3248e559e64037bdf9acf17) and some of its more relevant [changes](https://raw.githubusercontent.com/nnuudev/BlueClover/dev/CHANGES.txt) are:

- *New captcha support!*
- Page counter on thread view
- Board flags support
- Quick external image attaching
- Image renaming and reencoding
- Immersive mode for image gallery
- External archive support [partial]
- Alternate layout mode [experimental]

Some parts of the code were backported from [Kuroba](https://github.com/Adamantcheese/Kuroba) or merged from old [pull requests](https://github.com/chandevel/Clover/pulls?q=is%3Apr), check the [commit log](https://github.com/nnuudev/BlueClover/commits/dev) for proper attribution when applicable.

> *The goal of Blue Clover is to add some extra features to Clover while maintaining compatibility with Android 4.4. It is being distributed with the hope that it will be useful, but it is not meant as a replacement for any existing app. If you are interested in a more professional imageboard browser, please check [KurobaEx](https://github.com/K1rakishou/Kuroba-Experimental), or just [contribute to the original Clover](https://github.com/chandevel/Clover).*


## License
Blue Clover is [GPLv3](https://github.com/nnuudev/BlueClover/blob/dev/COPYING.txt), [licenses of the used libraries](https://github.com/nnuudev/BlueClover/blob/dev/Clover/app/src/main/assets/html/licenses.html).