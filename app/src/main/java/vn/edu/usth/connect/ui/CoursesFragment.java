package vn.edu.usth.connect.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.recyclerview.widget.*;
import java.util.*;
import vn.edu.usth.connect.R;
import vn.edu.usth.connect.adapter.CatalogAdapter;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.model.*;
import vn.edu.usth.connect.ui.dialog.ChannelDialogs;

public final class CoursesFragment extends SessionAwareFragment {
  private CatalogAdapter adapter;
  private boolean discovering;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle saved) {
    return inflater.inflate(R.layout.fragment_catalog, parent, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
    super.onViewCreated(view, saved);
    discovering = saved != null && saved.getBoolean("discovering");
    UiHelpers.brand(view.findViewById(R.id.brand));
    UiHelpers.visible(view.findViewById(R.id.back), false);
    UiHelpers.visible(view.findViewById(R.id.action), false);
    UiHelpers.visible(view.findViewById(R.id.account_avatar), true);
    view.findViewById(R.id.account_avatar).setOnClickListener(v -> main().showProfile());
    view.findViewById(R.id.account_avatar).setContentDescription(getString(R.string.profile));
    view.findViewById(R.id.empty_join)
        .setOnClickListener(
            v -> {
              discovering = true;
              render();
            });
    view.findViewById(R.id.join).setOnClickListener(v -> ChannelDialogs.showJoin(main(), ""));
    UiHelpers.visible(view.findViewById(R.id.browse_server), true);
    view.findViewById(R.id.browse_server).setOnClickListener(v -> main().showServerChannels());
    adapter = new CatalogAdapter();
    RecyclerView list = view.findViewById(R.id.list);
    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    list.setAdapter(adapter);
    UiHelpers.watch(view.findViewById(R.id.search), this::render);
  }

  @Override
  protected void render() {
    if (root == null || adapter == null) return;
    String query =
        ((EditText) root.findViewById(R.id.search))
            .getText()
            .toString()
            .trim()
            .toLowerCase(Locale.ROOT);
    List<CatalogAdapter.Row> joined = new ArrayList<>(), discover = new ArrayList<>();
    UiHelpers.avatar(root.findViewById(R.id.account_avatar), session.data.nickname);
    int totalJoined = 0;
    for (Course course : app.courses.courses) {
      int count = 0, unread = 0;
      boolean match = course.name.toLowerCase(Locale.ROOT).contains(query);
      for (Course.ChannelInfo info : course.channels) {
        Channel channel = session.data.channels.get(IrcParser.fold(info.name));
        if (channel != null && channel.joined) {
          count++;
          unread += channel.unread;
        }
        match |= info.name.toLowerCase(Locale.ROOT).contains(query);
      }
      totalJoined += count;
      if (!match) continue;
      CatalogAdapter.Row row = new CatalogAdapter.Row();
      row.name = course.name;
      row.icon = UiHelpers.courseIcon(course.icon);
      row.iconColor = UiHelpers.courseColor(course.icon);
      row.detail = getString(R.string.catalog_course_joined, course.channels.size(), count);
      row.preview = course.description;
      row.badge = unread;
      row.click = () -> main().open(CourseChannelsFragment.create(course.id));
      (count > 0 ? joined : discover).add(row);
    }
    List<CatalogAdapter.Row> other = new ArrayList<>();
    for (Channel c : session.data.channels.values())
      if (app.courses.forChannel(c.target) == null
          && c.joined
          && c.target.toLowerCase(Locale.ROOT).contains(query)) {
        CatalogAdapter.Row row = new CatalogAdapter.Row();
        row.name = c.target;
        row.detail = getString(R.string.other_channels);
        row.badge = c.unread;
        row.click = () -> main().openChat(c.target);
        other.add(row);
        totalJoined++;
      }
    List<CatalogAdapter.Row> rows = new ArrayList<>();
    boolean empty = query.isEmpty() && totalJoined == 0 && !discovering;
    UiHelpers.visible(root.findViewById(R.id.catalog_empty), empty);
    UiHelpers.visible(root.findViewById(R.id.list), true);
    UiHelpers.visible(root.findViewById(R.id.join), true);
    boolean online = session.data.connection == ConnectionState.CONNECTED;
    root.findViewById(R.id.join).setEnabled(online);
    TextView connection = root.findViewById(R.id.catalog_connection);
    connection.setText(UiHelpers.connection(requireContext(), session));
    connection.setTextColor(
        androidx.core.content.ContextCompat.getColor(
            requireContext(), online ? R.color.success_text : R.color.error));
    UiHelpers.visible(connection, true);
    Channel active = null;
    for (Channel channel : session.data.channels.values()) {
      if (channel.joined && (active == null || lastTime(channel) > lastTime(active)))
        active = channel;
    }
    UiHelpers.visible(root.findViewById(R.id.active_channel), active != null && query.isEmpty());
    if (active != null) {
      Channel selected = active;
      ((TextView) root.findViewById(R.id.active_channel_name)).setText(active.target);
      Course course = app.courses.forChannel(active.target);
      ((TextView) root.findViewById(R.id.active_channel_detail))
          .setText(
              getString(
                  R.string.catalog_active_detail,
                  course == null ? getString(R.string.other_channels) : course.name,
                  active.unread));
      root.findViewById(R.id.active_channel)
          .setOnClickListener(v -> main().openChat(selected.target));
    }
    if (!joined.isEmpty()) {
      CatalogAdapter.Row header =
          CatalogAdapter.Row.header(getString(R.string.catalog_courses_yours));
      header.headerCount = getString(R.string.catalog_courses_count, joined.size());
      rows.add(header);
      rows.addAll(joined);
    }
    if (!other.isEmpty()) {
      rows.add(CatalogAdapter.Row.header(getString(R.string.other_channels)));
      rows.addAll(other);
    }
    if (!discover.isEmpty()) {
      CatalogAdapter.Row header = CatalogAdapter.Row.header(getString(R.string.discover));
      header.headerCount = getString(R.string.catalog_courses_count, discover.size());
      rows.add(header);
      rows.addAll(discover);
    }
    if (rows.isEmpty()) rows.add(CatalogAdapter.Row.empty(getString(R.string.no_results), ""));
    adapter.submit(rows);
  }

  private long lastTime(Channel channel) {
    return channel.messages.isEmpty()
        ? 0
        : channel.messages.get(channel.messages.size() - 1).timestamp;
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle out) {
    super.onSaveInstanceState(out);
    out.putBoolean("discovering", discovering);
  }

  @Override
  public void onDestroyView() {
    adapter = null;
    super.onDestroyView();
  }
}
