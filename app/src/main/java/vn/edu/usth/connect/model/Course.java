package vn.edu.usth.connect.model;

import java.util.List;

public final class Course {
  public final String id, name, description, icon;
  public final List<ChannelInfo> channels;

  public Course(
      String id, String name, String description, String icon, List<ChannelInfo> channels) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.icon = icon;
    this.channels = channels;
  }

  public static final class ChannelInfo {
    public final String name, description;

    public ChannelInfo(String name, String description) {
      this.name = name;
      this.description = description;
    }
  }
}
