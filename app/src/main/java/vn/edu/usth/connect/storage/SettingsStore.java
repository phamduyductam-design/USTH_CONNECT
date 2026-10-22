package vn.edu.usth.connect.storage;

import android.content.Context;
import android.content.SharedPreferences;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.model.ConnectionConfig;

/** Non-sensitive preferences only. No server password or channel key is persisted. */
public final class SettingsStore {
  private final SharedPreferences prefs;

  public SettingsStore(Context context) {
    prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
  }

  public boolean darkMode() {
    return prefs.getBoolean("darkMode", false);
  }

  public void darkMode(boolean enabled) {
    prefs.edit().putBoolean("darkMode", enabled).apply();
  }

  public String host() {
    return prefs.getString("host", "");
  }

  public int port() {
    return prefs.getInt("port", 6697);
  }

  public boolean tls() {
    return prefs.getBoolean("tls", true);
  }

  public String nick() {
    return prefs.getString("nick", "minh_usth");
  }

  public String username() {
    return prefs.getString("username", "usth");
  }

  public String realName() {
    return prefs.getString("realName", "USTH Connect");
  }

  public boolean notifications() {
    return prefs.getBoolean("notifications", false);
  }

  public void notifications(boolean enabled) {
    prefs.edit().putBoolean("notifications", enabled).apply();
  }

  public void save(ConnectionConfig c) {
    prefs
        .edit()
        .putString("host", c.host)
        .putInt("port", c.port)
        .putBoolean("tls", c.tls)
        .putString("nick", c.nickname)
        .putString("username", c.username)
        .putString("realName", c.realName)
        .apply();
  }

  public String draft(String server, String target) {
    return prefs.getString(draftKey(server, target), "");
  }

  public void draft(String server, String target, String text) {
    SharedPreferences.Editor edit = prefs.edit();
    if (text.isEmpty()) edit.remove(draftKey(server, target));
    else edit.putString(draftKey(server, target), text);
    edit.apply();
  }

  private String draftKey(String server, String target) {
    return "draft:" + server + "/" + IrcParser.fold(target);
  }
}
