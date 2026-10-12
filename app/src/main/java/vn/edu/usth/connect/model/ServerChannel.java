package vn.edu.usth.connect.model;

public final class ServerChannel {
  public final String name, topic;
  public final int users;
  public ServerChannel(String name, int users, String topic) {
    this.name = name;
    this.users = users;
    this.topic = topic;
  }
}
