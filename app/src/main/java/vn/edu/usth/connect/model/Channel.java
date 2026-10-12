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

  public Channel(String name) {
    super(name);
  }
}
