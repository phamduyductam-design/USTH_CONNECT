package vn.edu.usth.connect.model;

import java.util.concurrent.atomic.AtomicLong;

public final class ChatMessage {
  private static final AtomicLong IDS = new AtomicLong();

  public enum Status {
    RECEIVED,
    QUEUED,
    WRITTEN,
    FAILED,
    UNCERTAIN
  }

  public final long id = IDS.incrementAndGet(), timestamp;
  public final String sender, text;
  public final boolean outgoing;
  // System rows store an event code, translated with Android string resources in the adapter.
  public final String event;
  public Status status;

  public ChatMessage(String sender, String text, boolean outgoing, Status status) {
    this(sender, text, outgoing, status, "", System.currentTimeMillis());
  }

  public ChatMessage(
      String sender, String text, boolean outgoing, Status status, String event, long timestamp) {
    this.sender = sender;
    this.text = text;
    this.outgoing = outgoing;
    this.status = status;
    this.event = event;
    this.timestamp = timestamp;
  }
}
