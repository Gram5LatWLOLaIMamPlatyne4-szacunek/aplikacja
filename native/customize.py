import glob, os, re, shutil
from PIL import Image

E = os.environ.get
R = "android/app/src/main/res"
M = "android/app/src/main/AndroidManifest.xml"
hexc = lambda c: c if re.fullmatch(r"#[0-9a-fA-F]{6}", c or "") else ""
splash, status, nav, orient = hexc(E("SPLASH")), hexc(E("STATUS")), hexc(E("NAV")), E("ORIENT", "")
icon = f"jobs/{E('JOB')}/icon.png"
has_icon = os.path.exists(icon)
read = lambda p: open(p, encoding="utf-8").read()
def write(p, s): open(p, "w", encoding="utf-8").write(s)
def dark(c):
    r, g, b = (int(c[i:i + 2], 16) for i in (1, 3, 5))
    return 0.299 * r + 0.587 * g + 0.114 * b < 150

# ikona
if has_icon:
    im = Image.open(icon).convert("RGBA")
    for d, s in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        o = im.resize((s, s), Image.LANCZOS)
        for n in ("ic_launcher", "ic_launcher_round", "ic_launcher_foreground"):
            o.save(f"{R}/mipmap-{d}/{n}.png")
    shutil.rmtree(f"{R}/mipmap-anydpi-v26", ignore_errors=True)

# splash
if splash or has_icon:
    for f in glob.glob(f"{R}/drawable*/splash.png"):
        os.remove(f)
    logo = ""
    if has_icon:
        im.resize((512, 512), Image.LANCZOS).save(f"{R}/drawable/splash_logo.png")
        logo = '<item android:width="160dp" android:height="160dp" android:gravity="center" android:drawable="@drawable/splash_logo"/>'
    write(f"{R}/drawable/splash.xml",
          '<?xml version="1.0" encoding="utf-8"?><layer-list xmlns:android="http://schemas.android.com/apk/res/android">'
          f'<item><shape><solid android:color="{splash or "#ffffff"}"/></shape></item>{logo}</layer-list>')

# kolory pasków i motyw splasha
sp = f"{R}/values/styles.xml"
s = read(sp)
add = ""
if status:
    add += f'<item name="android:statusBarColor">{status}</item><item name="android:windowLightStatusBar">{"false" if dark(status) else "true"}</item>'
if nav:
    add += f'<item name="android:navigationBarColor">{nav}</item><item name="android:windowLightNavigationBar">{"false" if dark(nav) else "true"}</item>'
if add:
    s = re.sub(r'(<style name="AppTheme\.NoActionBar"[^>]*>)', lambda m: m.group(1) + add, s, 1)
if splash:
    s = re.sub(r'(<style name="AppTheme\.NoActionBarLaunch"[^>]*Theme\.SplashScreen[^>]*>)',
               lambda m: m.group(1) + f'<item name="windowSplashScreenBackground">{splash}</item>', s, 1)
write(sp, s)

# manifest: uprawnienia, orientacja, widżet
m = read(M)
perms = ["VIBRATE", "POST_NOTIFICATIONS", "CAMERA", "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "RECEIVE_BOOT_COMPLETED"]
m = m.replace("<application", "".join(f'<uses-permission android:name="android.permission.{p}"/>' for p in perms)
              + '<uses-feature android:name="android.hardware.camera" android:required="false"/><application', 1)
if orient in ("portrait", "landscape"):
    m = m.replace("<activity", f'<activity android:screenOrientation="{orient}"', 1)
write(M, m)
