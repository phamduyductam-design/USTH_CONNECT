package vn.edu.usth.connect.ui.common;

import android.app.UiModeManager;
import android.content.Context;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;

/** Apply the saved preference before activities inflate their night-qualified resources. */
public final class ThemeMode {
  private ThemeMode() {}

  public static void apply(Context context, boolean dark) {
    if (Build.VERSION.SDK_INT >= 31) {
      context
          .getSystemService(UiModeManager.class)
          .setApplicationNightMode(
              dark ? UiModeManager.MODE_NIGHT_YES : UiModeManager.MODE_NIGHT_NO);
    }
    AppCompatDelegate.setDefaultNightMode(
        dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
  }
}
