package vn.edu.usth.connect.ui;

import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.recyclerview.widget.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import vn.edu.usth.connect.R;
import vn.edu.usth.connect.adapter.ChatAdapter;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.model.*;

public final class ChatFragment extends SessionFragment {
  private String target;
  private Conversation chat;
  private ChatAdapter adapter;
  private Parcelable scrollState;
  private long unreadBoundary;
  private boolean firstRender = true, pinned = true;
  private int messageCount;

  public static ChatFragment create(String target) {
    ChatFragment f = new ChatFragment();
    Bundle b = new Bundle();
    b.putString("target", target);
    f.setArguments(b);
    return f;
  }

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    target =
        saved != null
            ? saved.getString("target", requireArguments().getString("target"))
            : requireArguments().getString("target");
    chat = session.data.conversation(target);
    unreadBoundary = saved != null ? saved.getLong("unread") : chat.firstUnreadId;
    if (saved != null) {
      scrollState = saved.getParcelable("scroll");
      pinned = saved.getBoolean("pinned", true);
    }
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup p, @Nullable Bundle b) {
    return i.inflate(R.layout.fragment_chat, p, false);
  }

  @Override
  public void onViewCreated(@NonNull View v, @Nullable Bundle saved) {
    super.onViewCreated(v, saved);
    firstRender = true;
    RecyclerView list = v.findViewById(R.id.list);
    LinearLayoutManager manager = new LinearLayoutManager(requireContext());
    list.setLayoutManager(manager);
    adapter = new ChatAdapter(requireContext(), this::retry);
    list.setAdapter(adapter);
    list.addOnScrollListener(
        new RecyclerView.OnScrollListener() {
          @Override
          public void onScrolled(@NonNull RecyclerView recycler, int dx, int dy) {
            if (dy != 0) pinned = !recycler.canScrollVertically(1);
          }
        });
    v.findViewById(R.id.back).setOnClickListener(w -> main().back());
    v.findViewById(R.id.members)
        .setOnClickListener(
            w -> MembersBottomSheet.create(target).show(getParentFragmentManager(), "members"));
    v.findViewById(R.id.reconnect)
        .setOnClickListener(
            w -> {
              if (session.data.connection == ConnectionState.CONNECTED)
                Dialogs.join(main(), target);
              else session.reconnect();
            });
    v.findViewById(R.id.more)
        .setOnClickListener(
            w -> {
              if (IrcParser.isChannel(target))
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.leave_title, target))
                    .setMessage(R.string.leave_detail)
                    .setPositiveButton(
                        R.string.leave,
                        (d, which) -> {
                          session.part(target);
                          main().back();
                        })
                    .setNegativeButton(R.string.cancel, null)
                      .show();
              else
                new MaterialAlertDialogBuilder(requireContext()).setTitle(Ui.displayName(target))
                    .setMessage(target).setPositiveButton(R.string.close, null).show();
            });
    EditText composer = v.findViewById(R.id.composer);
    composer.setText(session.draft(target));
    Ui.watch(
        composer,
        () -> {
          session.saveDraft(target, composer.getText().toString());
          updateSend();
        });
    v.findViewById(R.id.send).setOnClickListener(w -> send());
    v.findViewById(R.id.latest)
        .setOnClickListener(
            w -> {
              pinned = true;
              list.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
              Ui.visible(v.findViewById(R.id.latest), false);
            });
  }

  @Override
  public void onStart() {
    super.onStart();
    session.setActive(target);
  }

  @Override
  public void onStop() {
    session.clearActive(target);
    super.onStop();
  }

  private void send() {
    EditText composer = root.findViewById(R.id.composer);
    try {
      session.send(target, composer.getText().toString());
      composer.setText("");
      pinned = true;
      ((RecyclerView) root.findViewById(R.id.list))
          .scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
    } catch (IllegalArgumentException e) {
      composer.setError(getString(R.string.error_message));
    } catch (IllegalStateException e) {
      Ui.toast(requireContext(), R.string.error_offline);
    }
  }

  private void retry(ChatMessage message) {
    if (!session.canSend(target)) {
      Ui.toast(requireContext(), R.string.error_offline);
      return;
    }
    Dialogs.confirm(main(), R.string.retry_title, R.string.retry_detail, R.string.retry,
        () -> session.retry(target, message));
  }

  private void updateSend() {
    if (root == null) return;
    View send = root.findViewById(R.id.send);
    boolean enabled =
        session.canSend(target)
            && !((EditText) root.findViewById(R.id.composer)).getText().toString().trim().isEmpty();
    send.setEnabled(enabled);
    send.setAlpha(1f);
  }

  @Override
  protected void render() {
    if (root == null || adapter == null) return;
    // A PM conversation object remains stable when the peer changes nickname.
    EditText composer = root.findViewById(R.id.composer);
    String oldTarget = target;
    target = chat.target;
    if (!oldTarget.equals(target)) {
      String draft = composer.getText().toString();
      session.saveDraft(oldTarget, "");
      session.saveDraft(target, draft);
    }
    boolean channel = chat instanceof Channel;
    Channel c = channel ? (Channel) chat : null;
    ((TextView) root.findViewById(R.id.title)).setText(channel ? target : Ui.displayName(target));
    Ui.visible(root.findViewById(R.id.chat_avatar), !channel);
    if (!channel) Ui.avatar(root.findViewById(R.id.chat_avatar), target);
    Course course = channel ? app.courses.forChannel(target) : null;
    ((TextView) root.findViewById(R.id.subtitle))
        .setText(
            channel
                ? (course == null ? getString(R.string.other_channels) : course.name)
                : target);
    Ui.visible(root.findViewById(R.id.members), channel);
    Ui.visible(root.findViewById(R.id.more), true);
    ((TextView) root.findViewById(R.id.members))
        .setText(String.valueOf(channel ? c.members.size() : 0));
    Ui.visible(root.findViewById(R.id.topic), channel);
    ((TextView) root.findViewById(R.id.topic))
        .setText(channel ? (c.topic.isEmpty() ? getString(R.string.no_topic) : c.topic) : "");
    boolean online = session.data.connection == ConnectionState.CONNECTED;
    Ui.visible(root.findViewById(R.id.banner), !online || (channel && !c.joined));
    ((TextView) root.findViewById(R.id.connection_status))
        .setText(
            online ? getString(R.string.not_joined)
                : getString(R.string.connection_lost_title) + "\n" + getString(R.string.connection_lost_detail));
    ((TextView) root.findViewById(R.id.reconnect))
        .setText(online ? R.string.join : R.string.reconnect);
    root.findViewById(R.id.reconnect).setEnabled(!online || !channel || !c.joining);
    composer.setHint(getString(channel ? R.string.chat_hint : R.string.pm_hint,
        channel ? target : Ui.displayName(target)));
    ((TextView) root.findViewById(R.id.send_hint))
        .setText(
            !online
                ? getString(R.string.offline_hint)
                : channel && !c.joined ? getString(R.string.not_joined) : "");
    Ui.visible(root.findViewById(R.id.send_hint), !online || (channel && !c.joined));
    if (!channel)
      ((TextView) root.findViewById(R.id.empty_detail)).setText(R.string.pm_empty_detail);
    boolean hasChat = false;
    for (ChatMessage m : chat.messages)
      if (m.event.isEmpty()) {
        hasChat = true;
        break;
      }
    Ui.visible(root.findViewById(R.id.empty), !hasChat);
    RecyclerView list = root.findViewById(R.id.list);
    Ui.visible(list, hasChat);
    Parcelable anchor = list.getLayoutManager().onSaveInstanceState();
    int currentSize = chat.messages.size();
    adapter.submit(chat.messages, unreadBoundary);
    if (firstRender) {
      if (scrollState != null) list.getLayoutManager().onRestoreInstanceState(scrollState);
      else list.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
      firstRender = false;
    } else if (pinned) list.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
    else {
      list.getLayoutManager().onRestoreInstanceState(anchor);
      if (currentSize > messageCount) Ui.visible(root.findViewById(R.id.latest), true);
    }
    messageCount = currentSize;
    updateSend();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle out) {
    super.onSaveInstanceState(out);
    out.putString("target", target);
    out.putLong("unread", unreadBoundary);
    out.putBoolean("pinned", pinned);
    Parcelable state =
        root == null
            ? scrollState
            : ((RecyclerView) root.findViewById(R.id.list))
                .getLayoutManager()
                .onSaveInstanceState();
    out.putParcelable("scroll", state);
  }

  @Override
  public void onDestroyView() {
    if (root != null) {
      scrollState =
          ((RecyclerView) root.findViewById(R.id.list)).getLayoutManager().onSaveInstanceState();
      session.saveDraft(target, ((EditText) root.findViewById(R.id.composer)).getText().toString());
    }
    adapter = null;
    super.onDestroyView();
  }
}
