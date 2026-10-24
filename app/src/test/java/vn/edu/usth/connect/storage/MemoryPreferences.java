package vn.edu.usth.connect.storage;

import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** In-memory Android preference boundary for JVM tests without an emulator. */
public final class MemoryPreferences {
  public final Map<String, Object> values = new HashMap<>();
  public final SharedPreferences prefs;

  public MemoryPreferences() {
    SharedPreferences.Editor editor =
        (SharedPreferences.Editor)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {SharedPreferences.Editor.class},
                (proxy, method, args) -> {
                  String name = method.getName();
                  if (name.startsWith("put")) values.put((String) args[0], args[1]);
                  else if (name.equals("remove")) values.remove(args[0]);
                  else if (name.equals("clear")) values.clear();
                  else if (name.equals("apply")) return null;
                  else if (name.equals("commit")) return true;
                  return proxy;
                });
    prefs =
        (SharedPreferences)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {SharedPreferences.class},
                (proxy, method, args) -> {
                  switch (method.getName()) {
                    case "edit":
                      return editor;
                    case "getAll":
                      return new HashMap<>(values);
                    case "contains":
                      return values.containsKey(args[0]);
                    default:
                      if (method.getName().startsWith("get"))
                        return values.getOrDefault(args[0], args[1]);
                      return null;
                  }
                });
  }
}
