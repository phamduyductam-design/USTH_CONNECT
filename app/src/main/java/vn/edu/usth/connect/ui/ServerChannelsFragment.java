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

/** This catalog is populated only by the IRC server's LIST replies. */
public final class ServerChannelsFragment extends SessionFragment {
  private CatalogAdapter adapter;

  @Override
  public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup p, @Nullable Bundle b) {
    return i.inflate(R.layout.fragment_catalog, p, false);
  }

  @Override
  public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
    super.onViewCreated(v, b);
    Ui.brand(v.findViewById(R.id.brand));
    Ui.visible(v.findViewById(R.id.back), false);
    ((TextView) v.findViewById(R.id.title)).setText(R.string.server_rooms);
    ((EditText) v.findViewById(R.id.search)).setHint(R.string.search_rooms);
    ((ImageButton) v.findViewById(R.id.action)).setImageResource(R.drawable.ic_refresh);
    v.findViewById(R.id.action).setContentDescription(getString(R.string.refresh));
    v.findViewById(R.id.action).setOnClickListener(w -> refresh());
    ((TextView) v.findViewById(R.id.join)).setText(R.string.create_join_room);
    v.findViewById(R.id.join).setOnClickListener(w -> ChannelDialogs.showJoin(main(), ""));
    Ui.visible(v.findViewById(R.id.catalog_footer), true);
    RecyclerView list = v.findViewById(R.id.list);
    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter = new CatalogAdapter();
    list.setAdapter(adapter);
    Ui.watch(v.findViewById(R.id.search), this::render);
  }

  @Override
  public void onStart() {
    super.onStart();
    if (!session.data.listLoaded
        && !session.data.listing
        && session.data.connection == ConnectionState.CONNECTED) refresh();
  }

  private void refresh() {
    try {
      session.listChannels();
    } catch (IllegalStateException e) {
      Ui.toast(requireContext(), R.string.error_offline);
    }
  }

  @Override
  protected void render() {
    if (root == null || adapter == null) return;
    boolean online = session.data.connection == ConnectionState.CONNECTED;
    root.findViewById(R.id.action).setEnabled(online && !session.data.listing);
    root.findViewById(R.id.join).setEnabled(online);
    String query =
        ((EditText) root.findViewById(R.id.search))
            .getText()
            .toString()
            .trim()
            .toLowerCase(Locale.ROOT);
    List<ServerChannel> rooms = new ArrayList<>(session.data.availableChannels.values());
    rooms.sort(Comparator.comparing(c -> IrcParser.fold(c.name)));
    List<CatalogAdapter.Row> rows = new ArrayList<>();
    for (ServerChannel room : rooms) {
      if (!room.name.toLowerCase(Locale.ROOT).contains(query)
          && !room.topic.toLowerCase(Locale.ROOT).contains(query)) continue;
      CatalogAdapter.Row row = new CatalogAdapter.Row();
      row.name = room.name;
      row.detail = getString(R.string.room_users, room.users);
      row.preview = room.topic.isEmpty() ? getString(R.string.no_topic) : room.topic;
      Channel joined = session.data.channels.get(IrcParser.fold(room.name));
      row.selected = joined != null && joined.joined;
      row.joinVisible = !row.selected;
      row.enabled = online;
      row.joining = joined != null && joined.joining;
      row.join = () -> ChannelDialogs.showJoin(main(), room.name);
      row.click =
          () -> {
            if (row.selected) main().openChat(room.name);
            else if (online) row.join.run();
          };
      rows.add(row);
    }
    if (rows.isEmpty())
      rows.add(
          CatalogAdapter.Row.empty(
              getString(
                  session.data.listing
                      ? R.string.rooms_loading
                      : query.isEmpty() ? R.string.rooms_empty : R.string.no_results),
              getString(R.string.rooms_empty_detail)));
    adapter.submit(rows);
    ((TextView) root.findViewById(R.id.catalog_footer))
        .setText(
            !online
                ? Ui.connection(requireContext(), session)
                : session.data.listing
                    ? getString(R.string.rooms_loading)
                    : getString(R.string.rooms_count, rooms.size()));
  }

  @Override
  public void onDestroyView() {
    adapter = null;
    super.onDestroyView();
  }
}
