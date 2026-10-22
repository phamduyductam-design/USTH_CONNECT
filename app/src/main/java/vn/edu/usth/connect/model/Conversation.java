package vn.edu.usth.connect.model;

import java.util.ArrayList;
import java.util.List;

public class Conversation {
  public static final int MAX_HISTORY_MESSAGES = 500;
  public String target;
  public final List<ChatMessage> messages = new ArrayList<>();
  public int unread;
  public long firstUnreadId;

  public Conversation(String target) {
    this.target = target;
  }

  public void add(ChatMessage message) {
    messages.add(message);
    if (messages.size() > MAX_HISTORY_MESSAGES)
      messages.remove(0); // Bounded process-local history.
  }
}
