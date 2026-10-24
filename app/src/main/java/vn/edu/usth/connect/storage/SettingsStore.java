package vn.edu.usth.connect.storage;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.regex.Pattern;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.model.ConnectionConfig;

/** Non-sensitive preferences only. No server password or channel key is persisted. */
public final class SettingsStore {
  private static final Pattern SENSITIVE_COMMAND =
      Pattern.compile("^/\\s*(?:j|jo|joi|join|m|mo|mod|mode)(?:\\s|$)", Pattern.CASE_INSENSITIVE);
  private final SharedPreferences prefs;

  public SettingsStore(Context context) {
    this(context.getSharedPreferences("settings", Context.MODE_PRIVATE));
  }

  public SettingsStore(SharedPreferences preferences) {
    prefs = preferences;
    // Clean old drafts too, even when their conversation is never opened again.
    SharedPreferences.Editor edit = prefs.edit();
    for (java.util.Map.Entry<String, ?> entry : prefs.getAll().entrySet())
      if (entry.getKey().startsWith("draft:")
          && entry.getValue() instanceof String
          && containsSensitiveCommand((String) entry.getValue())) edit.remove(entry.getKey());
    edit.apply();
  }

  private static boolean containsSensitiveCommand(String text) {
    int start = 0;
    while (start < text.length()
        && (Character.isWhitespace(text.charAt(start))
            || Character.isSpaceChar(text.charAt(start)))) start++;
    return SENSITIVE_COMMAND.matcher(text.substring(start)).find();
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
    String key = draftKey(server, target);
    String text = prefs.getString(key, "");
    if (containsSensitiveCommand(text)) {
      prefs.edit().remove(key).apply();
      return "";
    }
    return text;
  }

  public void draft(String server, String target, String text) {
    SharedPreferences.Editor edit = prefs.edit();
    if (text.isEmpty() || containsSensitiveCommand(text)) edit.remove(draftKey(server, target));
    else edit.putString(draftKey(server, target), text);
    edit.apply();
  }

  private String draftKey(String server, String target) {
    return "draft:" + server + "/" + IrcParser.fold(target);
  }
}
