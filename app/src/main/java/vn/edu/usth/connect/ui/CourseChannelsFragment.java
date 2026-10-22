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

public final class CourseChannelsFragment extends SessionAwareFragment {
  private CatalogAdapter adapter;
  private Course course;

  public static CourseChannelsFragment create(String id) {
    CourseChannelsFragment f = new CourseChannelsFragment();
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
    ((TextView) v.findViewById(R.id.toolbar_title)).setText(course.name);
    UiHelpers.visible(v.findViewById(R.id.toolbar_title), true);
    UiHelpers.visible(v.findViewById(R.id.title), false);
    UiHelpers.visible(v.findViewById(R.id.brand_container), false);
    UiHelpers.visible(v.findViewById(R.id.course_summary), true);
    ((TextView) v.findViewById(R.id.course_name)).setText(course.name);
    ImageView icon = v.findViewById(R.id.course_icon);
    icon.setImageResource(UiHelpers.courseIcon(course.icon));
    icon.setImageTintList(
        android.content.res.ColorStateList.valueOf(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.on_primary)));
    icon.setBackgroundTintList(
        android.content.res.ColorStateList.valueOf(
            androidx.core.content.ContextCompat.getColor(
                requireContext(), UiHelpers.courseColor(course.icon))));
    ((TextView) v.findViewById(R.id.description)).setText(course.description);
    UiHelpers.visible(v.findViewById(R.id.description), true);
    UiHelpers.visible(v.findViewById(R.id.search), false);
    ((ImageButton) v.findViewById(R.id.action)).setImageResource(R.drawable.ic_info);
    v.findViewById(R.id.action).setContentDescription(getString(R.string.course_info));
    v.findViewById(R.id.action)
        .setOnClickListener(
            w ->
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(course.name)
                    .setMessage(course.description)
                    .setPositiveButton(R.string.close, null)
                    .show());
    UiHelpers.visible(v.findViewById(R.id.brand), false);
    UiHelpers.visible(v.findViewById(R.id.join), false);
    v.findViewById(R.id.back).setOnClickListener(w -> main().back());
    adapter = new CatalogAdapter();
    RecyclerView list = v.findViewById(R.id.list);
    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    list.setAdapter(adapter);
    UiHelpers.watch(v.findViewById(R.id.search), this::render);
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
      row.join = () -> ChannelDialogs.showJoin(main(), info.name);
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
