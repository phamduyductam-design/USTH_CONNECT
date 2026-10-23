package vn.edu.usth.connect.ui.chat;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.recyclerview.widget.*;
import java.util.*;
import vn.edu.usth.connect.R;
import vn.edu.usth.connect.adapter.CatalogAdapter;
import vn.edu.usth.connect.model.*;
import vn.edu.usth.connect.ui.common.SessionAwareFragment;
import vn.edu.usth.connect.ui.common.UiHelpers;
import vn.edu.usth.connect.ui.dialog.UserDialogs;

public final class ConversationsFragment extends SessionAwareFragment {
  private CatalogAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup p, @Nullable Bundle b) {
    return i.inflate(R.layout.fragment_catalog, p, false);
  }

  @Override
  public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
    super.onViewCreated(v, b);
    UiHelpers.visible(v.findViewById(R.id.back), false);
    UiHelpers.visible(v.findViewById(R.id.join), true);
    ((TextView) v.findViewById(R.id.join)).setText(R.string.catalog_start_conversation);
    v.findViewById(R.id.join).setOnClickListener(w -> UserDialogs.showNewConversation(main()));
    UiHelpers.visible(v.findViewById(R.id.catalog_footer), true);
    ((TextView) v.findViewById(R.id.title)).setText(R.string.messages);
    ((EditText) v.findViewById(R.id.search)).setHint(R.string.search_nick);
    ((ImageButton) v.findViewById(R.id.action)).setImageResource(R.drawable.ic_plus);
    v.findViewById(R.id.action).setContentDescription(getString(R.string.new_conversation));
    v.findViewById(R.id.action).setOnClickListener(w -> UserDialogs.showNewConversation(main()));
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
        ((EditText) root.findViewById(R.id.search)).getText().toString().toLowerCase(Locale.ROOT);
    List<Conversation> chats = new ArrayList<>(session.data.conversations.values());
    chats.sort((a, b) -> Long.compare(lastTime(b), lastTime(a)));
    List<CatalogAdapter.Row> rows = new ArrayList<>();
    for (Conversation c : chats)
      if (c.target.toLowerCase(Locale.ROOT).contains(query)) {
        CatalogAdapter.Row row = new CatalogAdapter.Row();
        row.name = c.target;
        row.avatar = c.target;
        row.detail = "";
        row.badge = c.unread;
        row.preview =
            c.messages.isEmpty()
                ? getString(R.string.conversation_empty)
                : c.messages.get(c.messages.size() - 1).text;
        row.time = c.messages.isEmpty() ? "" : UiHelpers.time(lastTime(c));
        row.click = () -> main().openChat(c.target);
        rows.add(row);
      }
    if (rows.isEmpty())
      rows.add(
          CatalogAdapter.Row.empty(
              getString(query.isEmpty() ? R.string.no_conversations : R.string.no_results),
              query.isEmpty() ? getString(R.string.no_conversations_detail) : ""));
    else {
      CatalogAdapter.Row header = CatalogAdapter.Row.header(getString(R.string.recent));
      header.headerCount = getString(R.string.catalog_recent_count, rows.size());
      rows.add(0, header);
    }
    adapter.submit(rows);
  }

  private long lastTime(Conversation c) {
    return c.messages.isEmpty() ? 0 : c.messages.get(c.messages.size() - 1).timestamp;
  }

  @Override
  public void onDestroyView() {
    adapter = null;
    super.onDestroyView();
  }
}
