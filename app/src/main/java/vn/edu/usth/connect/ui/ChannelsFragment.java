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

public final class ChannelsFragment extends SessionFragment {
  private CatalogAdapter adapter;
  private Course course;

  public static ChannelsFragment create(String id) {
    ChannelsFragment f = new ChannelsFragment();
    Bundle b = new Bundle();
    b.putString("course", id);
    f.setArguments(b);
    return f;
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup p, @Nullable Bundle s) {
    return i.inflate(R.layout.fragment_catalog, p, false);
  }

  @Override
  public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
    super.onViewCreated(v, b);
    course = app.courses.find(requireArguments().getString("course"));
    ((TextView) v.findViewById(R.id.title)).setText(course.name);
    ((TextView) v.findViewById(R.id.description)).setText(course.description);
    Ui.visible(v.findViewById(R.id.description), true);
    Ui.visible(v.findViewById(R.id.action), false);
    Ui.visible(v.findViewById(R.id.brand), false);
    Ui.visible(v.findViewById(R.id.join), false);
    v.findViewById(R.id.back).setOnClickListener(w -> main().back());
    adapter = new CatalogAdapter();
    RecyclerView list = v.findViewById(R.id.list);
    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    list.setAdapter(adapter);
    Ui.watch(v.findViewById(R.id.search), this::render);
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
    List<CatalogAdapter.Row> joined = new ArrayList<>(),
        available = new ArrayList<>(),
        rows = new ArrayList<>();
    for (Course.ChannelInfo info : course.channels) {
      if (!info.name.toLowerCase(Locale.ROOT).contains(query)
          && !info.description.toLowerCase(Locale.ROOT).contains(query)) continue;
      Channel channel = session.data.channels.get(IrcParser.fold(info.name));
      boolean in = channel != null && channel.joined;
      CatalogAdapter.Row row = new CatalogAdapter.Row();
      row.name = info.name.substring(1);
      row.detail = info.description;
      row.badge = channel == null ? 0 : channel.unread;
      row.selected = row.badge > 0;
      row.click = () -> main().openChat(info.name);
      row.joinVisible = !in;
      row.joining = channel != null && channel.joining;
      row.join = () -> Dialogs.join(main(), info.name);
      (in ? joined : available).add(row);
    }
    if (!joined.isEmpty()) {
      rows.add(CatalogAdapter.Row.header(getString(R.string.joined_channels)));
      rows.addAll(joined);
    }
    if (!available.isEmpty()) {
      rows.add(CatalogAdapter.Row.header(getString(R.string.available_channels)));
      rows.addAll(available);
    }
    if (rows.isEmpty()) rows.add(CatalogAdapter.Row.empty(getString(R.string.no_results), ""));
    adapter.submit(rows);
  }

  @Override
  public void onDestroyView() {
    adapter = null;
    course = null;
    super.onDestroyView();
  }
}
