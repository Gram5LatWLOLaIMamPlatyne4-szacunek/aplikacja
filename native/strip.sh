# Awaryjnie: usuwa natywne dodatki (widżet, sprawdzanie w tle), zostawia samą aplikację
P=android/app/src/main/java/$(echo "$APPID" | tr . /)
rm -f "$P"/BontoWidgetPlugin.java "$P"/BontoWidgetProvider.java "$P"/EdupageParse.java "$P"/EdupageWatcher.java "$P"/EdupageJobService.java
cp "$P/MainActivity.java.orig" "$P/MainActivity.java"
cp android/app/src/main/AndroidManifest.xml.orig android/app/src/main/AndroidManifest.xml
