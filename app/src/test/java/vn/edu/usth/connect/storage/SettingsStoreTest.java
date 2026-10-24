package vn.edu.usth.connect.storage;

import static org.junit.Assert.*;

import org.junit.Test;

public class SettingsStoreTest {
  @Test
  public void neverPersistsSensitiveOrIncompleteCommands() {
    MemoryPreferences memory = new MemoryPreferences();
    SettingsStore store = new SettingsStore(memory.prefs);
    for (String command :
        new String[] {
          "/join #c secret",
          " /MODE #c +k secret",
          "/j secret",
          "/jo secret",
          "/joi secret",
          "/m secret",
          "/mo secret",
          "/mod secret",
          "/ join #c secret"
        }) {
      store.draft("server", "#c", "older");
      store.draft("server", "#c", command);
      assertEquals("", store.draft("server", "#c"));
      assertFalse(memory.values.containsKey("draft:server/#c"));
    }
  }

  @Test
  public void cleansLegacyDraftsAndFiltersAnyLaterLegacyRead() {
    MemoryPreferences memory = new MemoryPreferences();
    memory.values.put("draft:server/#c", "/join #c secret");
    memory.values.put("draft:other/#c", "ordinary draft");
    SettingsStore store = new SettingsStore(memory.prefs);
    assertFalse(memory.values.containsKey("draft:server/#c"));
    assertEquals("ordinary draft", store.draft("other", "#c"));
    memory.values.put("draft:server/#c", "/mode #c +k secret");
    assertEquals("", store.draft("server", "#c"));
    assertFalse(memory.values.containsKey("draft:server/#c"));
  }

  @Test
  public void keepsTextAndEscapedCommandsIsolatedByServerAndRfc1459Target() {
    SettingsStore store = new SettingsStore(new MemoryPreferences().prefs);
    store.draft("one", "Nick[", "//join #c ordinary text");
    store.draft("two", "nick{", "Tiếng Việt");
    assertEquals("//join #c ordinary text", store.draft("one", "NICK{"));
    assertEquals("Tiếng Việt", store.draft("two", "nick["));
    assertEquals("", store.draft("one", "other"));
  }
}
