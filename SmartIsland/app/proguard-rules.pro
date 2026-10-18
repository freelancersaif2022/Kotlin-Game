# Accessibility/notification services and widget receivers are referenced from the manifest
# and kept by AAPT-generated rules. Nothing extra is needed for Compose or DataStore.
-keep class com.smartisland.app.widget.** extends android.appwidget.AppWidgetProvider
