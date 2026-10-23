package vn.edu.usth.connect.ui.common;

import android.app.Activity;
import android.view.View;
import androidx.core.view.*;

public final class WindowInsetsHelper {
  public static void apply(Activity activity, View root) {
    apply(activity, root, null);
  }

  public static void apply(
      Activity activity, View root, java.util.function.Consumer<Boolean> keyboard) {
    WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (v, insets) -> {
          androidx.core.graphics.Insets bars =
              insets.getInsets(
                  WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
          v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          if (keyboard != null) keyboard.accept(insets.isVisible(WindowInsetsCompat.Type.ime()));
          return WindowInsetsCompat.CONSUMED;
        });
    ViewCompat.requestApplyInsets(root);
  }
}
