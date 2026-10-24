package vn.edu.usth.connect.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.*;
import android.view.View;
import android.widget.*;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.*;
import vn.edu.usth.connect.R;
import vn.edu.usth.connect.model.*;
import vn.edu.usth.connect.repository.IrcSessionController;

public final class UiHelpers {
  private UiHelpers() {}

  public static void themeToggle(android.app.Activity activity, View root) {
    ImageButton button = root.findViewById(R.id.theme_toggle);
    if (button == null) return;
    boolean dark =
        (activity.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
            == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    button.setImageResource(dark ? R.drawable.ic_sun : R.drawable.ic_moon);
    button.setContentDescription(
        activity.getString(dark ? R.string.switch_light_mode : R.string.switch_dark_mode));
    button.setOnClickListener(
        v -> {
          vn.edu.usth.connect.ConnectApplication.from(activity).settings.darkMode(!dark);
          AppTheme.apply(activity, !dark);
        });
  }

  public static String displayName(String nickname) {
    String name = nickname.split("_", 2)[0];
    return name.isEmpty()
        ? nickname
        : name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
  }

  public static int courseColor(String icon) {
    if (icon.equals("brain")) return R.color.purple;
    if (icon.equals("phone")) return R.color.filled_red;
    if (icon.equals("book")) return R.color.teal;
    return R.color.filled_primary;
  }

  public static String role(Context context, String role) {
    int id =
        role.equals("q")
            ? R.string.owner_role
            : role.equals("a")
                ? R.string.admin_role
                : role.equals("o")
                    ? R.string.operator_role
                    : role.equals("h")
                        ? R.string.halfop_role
                        : role.equals("v") ? R.string.voice_role : R.string.member_role;
    return context.getString(id);
  }

  public static void watch(EditText edit, Runnable action) {
    edit.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            action.run();
          }

          public void afterTextChanged(Editable e) {}
        });
  }

  public static void avatar(TextView view, String nickname) {
    String initials = nickname.isEmpty() ? "?" : nickname.substring(0, 1).toUpperCase(Locale.ROOT);
    view.setText(initials);
    int[] colors = {
      R.color.avatar_blue, R.color.avatar_purple, R.color.avatar_coral, R.color.avatar_teal
    };
    String lower = nickname.toLowerCase(Locale.ROOT);
    int index = Math.floorMod(lower.hashCode(), colors.length);
    view.setTextColor(ContextCompat.getColor(view.getContext(), R.color.primary));
    view.setBackgroundTintList(
        ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), colors[index])));
  }

  public static String time(long timestamp) {
    return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
  }

  public static String date(Context context, long timestamp) {
    Calendar a = Calendar.getInstance(), b = Calendar.getInstance();
    b.setTimeInMillis(timestamp);
    if (sameDay(a, b)) return context.getString(R.string.today);
    a.add(Calendar.DAY_OF_YEAR, -1);
    if (sameDay(a, b)) return context.getString(R.string.yesterday);
    return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(timestamp));
  }

  private static boolean sameDay(Calendar a, Calendar b) {
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
        && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
  }

  public static String error(Context c, String code) {
    int resource;
    switch (code) {
      case "433":
        resource = R.string.error_433;
        break;
      case "432":
        resource = R.string.error_432;
        break;
      case "401":
        resource = R.string.error_401;
        break;
      case "404":
        resource = R.string.error_404;
        break;
      case "403":
        resource = R.string.error_403;
        break;
      case "471":
        resource = R.string.error_471;
        break;
      case "473":
        resource = R.string.error_473;
        break;
      case "474":
        resource = R.string.error_474;
        break;
      case "475":
        resource = R.string.error_475;
        break;
      case "442":
        resource = R.string.error_442;
        break;
      case "482":
        resource = R.string.error_482;
        break;
      case "464":
        resource = R.string.error_464;
        break;
      case "465":
        resource = R.string.error_465;
        break;
      case "tls":
        resource = R.string.error_tls;
        break;
      case "timeout":
        resource = R.string.error_timeout;
        break;
      case "join_timeout":
        resource = R.string.error_join_timeout;
        break;
      case "server":
        resource = R.string.error_server;
        break;
      case "network":
        resource = R.string.error_network;
        break;
      case "":
        return "";
      default:
        return c.getString(R.string.error_generic, code);
    }
    return c.getString(resource);
  }

  public static String connection(Context context, IrcSessionController session) {
    switch (session.data.connection) {
      case CONNECTING:
        return context.getString(R.string.connecting);
      case REGISTERING:
        return context.getString(R.string.registering);
      case CONNECTED:
        return context.getString(R.string.connected);
      case RECONNECTING:
        return session.reason.startsWith("retry:")
            ? context.getString(R.string.network_retry, session.reason.substring(6))
            : context.getString(R.string.reconnecting);
      case FAILED:
        return error(context, session.reason);
      default:
        return context.getString(R.string.disconnected);
    }
  }

  public static int courseIcon(String icon) {
    switch (icon) {
      case "code":
        return R.drawable.ic_code;
      case "brain":
        return R.drawable.ic_brain;
      case "phone":
        return R.drawable.ic_phone;
      default:
        return R.drawable.ic_book;
    }
  }

  public static void visible(View view, boolean visible) {
    view.setVisibility(visible ? View.VISIBLE : View.GONE);
  }

  public static void toast(Context context, int text) {
    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
  }
}
