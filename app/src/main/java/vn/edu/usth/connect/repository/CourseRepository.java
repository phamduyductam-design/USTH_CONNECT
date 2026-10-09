package vn.edu.usth.connect.repository;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.model.Course;

public final class CourseRepository {
  public final List<Course> courses = new ArrayList<>();

  public CourseRepository(Context context) {
    try (InputStream input = context.getAssets().open("courses.json")) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[2048];
      int count;
      while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
      JSONArray array =
          new JSONObject(output.toString(StandardCharsets.UTF_8.name())).getJSONArray("courses");
      for (int i = 0; i < array.length(); i++) {
        JSONObject obj = array.getJSONObject(i);
        List<Course.ChannelInfo> channels = new ArrayList<>();
        JSONArray names = obj.getJSONArray("channels");
        for (int j = 0; j < names.length(); j++) {
          JSONObject c = names.getJSONObject(j);
          channels.add(new Course.ChannelInfo(c.getString("name"), c.getString("description")));
        }
        courses.add(
            new Course(
                obj.getString("id"),
                obj.getString("name"),
                obj.getString("description"),
                obj.getString("icon"),
                channels));
      }
    } catch (IOException | JSONException e) {
      throw new IllegalStateException("Invalid bundled course catalog", e);
    }
  }

  public Course find(String id) {
    for (Course c : courses) if (c.id.equals(id)) return c;
    return null;
  }

  public Course forChannel(String name) {
    for (Course c : courses)
      for (Course.ChannelInfo channel : c.channels)
        if (IrcParser.fold(name).equals(IrcParser.fold(channel.name))) return c;
    return null;
  }
}
