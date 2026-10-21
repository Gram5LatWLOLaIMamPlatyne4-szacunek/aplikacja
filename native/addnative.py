M = "android/app/src/main/AndroidManifest.xml"
m = open(M, encoding="utf-8").read()
add = ('<receiver android:name=".BontoWidgetProvider" android:exported="true"><intent-filter>'
       '<action android:name="android.appwidget.action.APPWIDGET_UPDATE"/></intent-filter>'
       '<meta-data android:name="android.appwidget.provider" android:resource="@xml/bonto_widget_info"/></receiver>'
       '<service android:name=".EdupageJobService" android:permission="android.permission.BIND_JOB_SERVICE" android:exported="false"/>')
open(M, "w", encoding="utf-8").write(m.replace("</application>", add + "</application>", 1))
