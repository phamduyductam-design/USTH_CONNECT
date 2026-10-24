package vn.edu.usth.connect.model;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class Channel extends Conversation {
  public boolean joined, wanted, joining, namesInProgress;
  public String topic = "";
  public final Set<Character> modes = new HashSet<>();
  public final Map<String, IrcUser> members = new LinkedHashMap<>();
  public final Map<String, IrcUser> namesBatch = new LinkedHashMap<>();
  public final Set<String> namesRemoved = new HashSet<>();
  private int pendingNamesReplies;
  private boolean mergeNames;

  public Channel(String name) {
    super(name);
  }

  public boolean awaitingNamesReplies() {
    return pendingNamesReplies > 0;
  }

  public void beginNames() {
    // A timed-out reply can arrive during a retry. Its tail is not a full snapshot.
    mergeNames = awaitingNamesReplies();
    if (!mergeNames) namesRemoved.clear();
    pendingNamesReplies++;
    namesInProgress = true;
    namesBatch.clear();
  }

  public void cancelNames(boolean rejected) {
    if (rejected && namesInProgress && awaitingNamesReplies()) pendingNamesReplies--;
    namesInProgress = false;
    namesBatch.clear();
    if (!awaitingNamesReplies()) namesRemoved.clear();
  }

  public void finishNames() {
    if (awaitingNamesReplies()) pendingNamesReplies--;
    if (joined && namesInProgress) {
      if (mergeNames) {
        for (Map.Entry<String, IrcUser> entry : namesBatch.entrySet())
          members.putIfAbsent(entry.getKey(), entry.getValue());
      } else {
        members.clear();
        members.putAll(namesBatch);
      }
      namesBatch.clear();
      if (awaitingNamesReplies()) return;
    }
    cancelNames(false);
  }

  public void resetNames() {
    pendingNamesReplies = 0;
    mergeNames = false;
    cancelNames(false);
  }
}
