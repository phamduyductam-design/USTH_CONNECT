package vn.edu.usth.connect;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import vn.edu.usth.connect.repository.*;
import vn.edu.usth.connect.storage.SettingsStore;
import vn.edu.usth.connect.ui.MainActivity;

public final class ConnectApplication extends Application {
  public SettingsStore settings;
  public CourseRepository courses;
  public SessionRepository session;
  private int started;

  public static ConnectApplication from(Context context) {
    return (ConnectApplication) context.getApplicationContext();
  }

  @Override
  public void onCreate() {
    super.onCreate();
    settings = new SettingsStore(this);
    vn.edu.usth.connect.ui.ThemeMode.apply(this, settings.darkMode());
    courses = new CourseRepository(this);
    session = new SessionRepository(settings);
    NotificationManager manager = getSystemService(NotificationManager.class);
    if (Build.VERSION.SDK_INT >= 26) {
      manager.createNotificationChannel(
          new NotificationChannel(
              "messages",
              getString(R.string.notifications),
              NotificationManager.IMPORTANCE_DEFAULT));
      manager.createNotificationChannel(
          new NotificationChannel(
              "mentions",
              getString(R.string.mentions_channel),
              NotificationManager.IMPORTANCE_HIGH));
    }
    registerActivityLifecycleCallbacks(
        new ActivityLifecycleCallbacks() {
          public void onActivityStarted(Activity a) {
            started++;
          }

          public void onActivityStopped(Activity a) {
            started--;
          }

          public void onActivityCreated(Activity a, Bundle b) {}

          public void onActivityResumed(Activity a) {}

          public void onActivityPaused(Activity a) {}

          public void onActivitySaveInstanceState(Activity a, Bundle b) {}

          public void onActivityDestroyed(Activity a) {}
        });
    session.incomingListener =
        (target, message) -> {
          if (started > 0 || !settings.notifications()) return;
          if (Build.VERSION.SDK_INT >= 33
              && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                  != PackageManager.PERMISSION_GRANTED) return;
          NotificationManagerCompat notifications = NotificationManagerCompat.from(this);
          if (!notifications.areNotificationsEnabled()) return;
          Intent intent =
              new Intent(this, MainActivity.class)
                  .putExtra("target", target)
                  .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
          PendingIntent pending =
              PendingIntent.getActivity(
                  this,
                  target.hashCode(),
                  intent,
                  PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
          try {
            notifications.notify(
                target.hashCode(),
                new NotificationCompat.Builder(
                        this, message.mentionNick.isEmpty() ? "messages" : "mentions")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(
                        getString(
                            message.mentionNick.isEmpty()
                                ? R.string.notification_title
                                : R.string.mention_title,
                            message.sender,
                            target))
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setPriority(
                        message.mentionNick.isEmpty()
                            ? NotificationCompat.PRIORITY_DEFAULT
                            : NotificationCompat.PRIORITY_HIGH)
                    .setContentText(message.text)
                    .setContentIntent(pending)
                    .setAutoCancel(true)
                    .build());
          } catch (SecurityException ignored) {
            /* Permission may have been revoked between the check and notify. */
          }
        };
  }
}
