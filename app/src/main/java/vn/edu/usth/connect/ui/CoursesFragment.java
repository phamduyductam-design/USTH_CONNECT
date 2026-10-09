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

public final class CoursesFragment extends SessionFragment {
  private CatalogAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle saved) {
    return inflater.inflate(R.layout.fragment_catalog, parent, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
    super.onViewCreated(view, saved);
    Ui.visible(view.findViewById(R.id.back), false);
    view.findViewById(R.id.action).setOnClickListener(v -> main().showProfile());
    view.findViewById(R.id.join).setOnClickListener(v -> Dialogs.join(main(), ""));
    adapter = new CatalogAdapter();
    RecyclerView list = view.findViewById(R.id.list);
    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    list.setAdapter(adapter);
    Ui.watch(view.findViewById(R.id.search), this::render);
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
      row.icon = Ui.courseIcon(course.icon);
      row.detail = count > 0 ? getString(R.string.course_count, count, unread) : course.description;
      row.badge = unread;
      row.click = () -> main().open(ChannelsFragment.create(course.id));
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
    if (query.isEmpty() && totalJoined == 0)
      rows.add(
          CatalogAdapter.Row.empty(
              getString(R.string.no_courses), getString(R.string.no_courses_detail)));
    if (!joined.isEmpty()) {
      rows.add(CatalogAdapter.Row.header(getString(R.string.joined)));
      rows.addAll(joined);
    }
    if (!other.isEmpty()) {
      rows.add(CatalogAdapter.Row.header(getString(R.string.other_channels)));
      rows.addAll(other);
    }
    if (!discover.isEmpty()) {
      rows.add(CatalogAdapter.Row.header(getString(R.string.discover)));
      rows.addAll(discover);
    }
    if (rows.isEmpty()) rows.add(CatalogAdapter.Row.empty(getString(R.string.no_results), ""));
    adapter.submit(rows);
  }

  @Override
  public void onDestroyView() {
    adapter = null;
    super.onDestroyView();
  }
}
