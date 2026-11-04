# Keep the app's model classes (used by JSON backup/restore via reflection-free mappers).
-keep class com.piyush.island.model.** { *; }

# Play Billing / App Update
-keep class com.android.billingclient.** { *; }
-keep class com.google.android.play.core.** { *; }

# Accessibility service and notification listener are referenced from the manifest.
-keep class com.piyush.island.overlay.IslandAccessibilityService { *; }
-keep class com.piyush.island.content.IslandNotificationListener { *; }
-keep class com.piyush.island.service.IslandTileService { *; }
