package vn.edu.usth.connect.ui;

import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.recyclerview.widget.*;
import vn.edu.usth.connect.R;
import vn.edu.usth.connect.adapter.ChatAdapter;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.irc.SlashCommand;
import vn.edu.usth.connect.model.*;
import vn.edu.usth.connect.ui.dialog.ChannelDialogs;
import vn.edu.usth.connect.ui.dialog.CommonDialogs;
import vn.edu.usth.connect.ui.dialog.UserDialogs;

public final class ChatFragment extends SessionAwareFragment {
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
                ChannelDialogs.showJoin(main(), target);
              else session.reconnect();
            });
    v.findViewById(R.id.more)
        .setOnClickListener(
            w -> {
              if (session.data.connection == ConnectionState.CONNECTED) showChatMenu();
              else UiHelpers.toast(requireContext(), R.string.error_offline);
            });
    EditText composer = v.findViewById(R.id.composer);
    composer.setText(session.draft(target));
    UiHelpers.watch(
        composer,
        () -> {
          composer.setError(null);
          session.saveDraft(target, composer.getText().toString());
          updateSend();
        });
    v.findViewById(R.id.send).setOnClickListener(w -> send());
    v.findViewById(R.id.latest)
        .setOnClickListener(
            w -> {
              pinned = true;
              list.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
              UiHelpers.visible(v.findViewById(R.id.latest), false);
            });
  }

  @Override
  public void onStart() {
    super.onStart();
    session.setActive(target);
    if (!IrcParser.isChannel(target) && session.data.connection == ConnectionState.CONNECTED)
      session.refreshWho(target);
  }

  @Override
  public void onStop() {
    session.clearActive(target);
    super.onStop();
  }

  private void send() {
    EditText composer = root.findViewById(R.id.composer);
    try {
      // A composer line break is text, not the end of an IRC command.
      String text = composer.getText().toString().replaceAll("[\\r\\n]+", " ").trim();
      if (text.startsWith("/") && !text.startsWith("//")) {
        sendSlashCommand(text, composer);
        return;
      }
      session.send(target, text.startsWith("//") ? text.substring(1) : text);
      composer.setText("");
      pinned = true;
      ((RecyclerView) root.findViewById(R.id.list))
          .scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      composer.setError(
          composer.getText().toString().trim().startsWith("/")
              ? message != null
                      && (message.startsWith("Cách dùng") || message.startsWith("Lệnh không"))
                  ? message
                  : getString(R.string.command_invalid)
              : getString(R.string.error_message));
    } catch (IllegalStateException e) {
      UiHelpers.toast(requireContext(), R.string.error_offline);
    }
  }

  private void showChatMenu() {
    if (IrcParser.isChannel(target)) ChannelDialogs.showMenu(main(), target);
    else UserDialogs.showPrivateInfo(main(), target);
  }

  private void sendSlashCommand(String text, EditText composer) {
    SlashCommand command = SlashCommand.parse(text, target);
    if (command.name.equals("help")) CommonDialogs.showCommandHelp(main());
    else session.execute(command);
    composer.setText("");
    if (command.name.equals("join") || command.name.equals("msg")) main().openChat(command.target);
    else if (command.name.equals("list")) main().showServerChannels();
    else if (command.name.equals("part") && command.target.equalsIgnoreCase(target)) main().back();
  }

  private void retry(ChatMessage message) {
    if (!session.canSend(target)) {
      UiHelpers.toast(requireContext(), R.string.error_offline);
      return;
    }
    CommonDialogs.confirm(
        main(),
        R.string.retry_title,
        R.string.retry_detail,
        R.string.retry,
        () -> session.retry(target, message));
  }

  private void updateSend() {
    if (root == null) return;
    View send = root.findViewById(R.id.send);
    String input = ((EditText) root.findViewById(R.id.composer)).getText().toString().trim();
    boolean command = input.startsWith("/") && !input.startsWith("//");
    boolean enabled =
        session.data.connection == ConnectionState.CONNECTED
            && !input.isEmpty()
            && (command || session.canSend(target));
    send.setEnabled(enabled);
    send.setAlpha(1f);
  }

  @Override
  protected void render() {
    if (root == null || adapter == null) return;
    syncTargetAndDraft();
    renderHeader();
    renderConnectionState();
    renderMessages();
    updateSend();
  }

  private void syncTargetAndDraft() {
    // A PM conversation object remains stable when the peer changes nickname.
    EditText composer = root.findViewById(R.id.composer);
    String oldTarget = target;
    target = chat.target;
    if (!oldTarget.equals(target)) {
      String draft = composer.getText().toString();
      session.saveDraft(oldTarget, "");
      session.saveDraft(target, draft);
    }
  }

  private void renderHeader() {
    boolean channel = chat instanceof Channel;
    Channel c = channel ? (Channel) chat : null;
    ((TextView) root.findViewById(R.id.title))
        .setText(channel ? target : UiHelpers.displayName(target));
    UiHelpers.visible(root.findViewById(R.id.chat_avatar), !channel);
    if (!channel) UiHelpers.avatar(root.findViewById(R.id.chat_avatar), target);
    Course course = channel ? app.courses.forChannel(target) : null;
    ((TextView) root.findViewById(R.id.subtitle))
        .setText(
            channel
                ? (course == null ? getString(R.string.other_channels) : course.name)
                : target
                    + " · "
                    + getString(
                        session.data.awayUsers.containsKey(IrcParser.fold(target))
                            ? R.string.away_presence
                            : session.data.onlineUsers.contains(IrcParser.fold(target))
                                ? R.string.online_presence
                                : R.string.presence_unknown));
    UiHelpers.visible(root.findViewById(R.id.members), channel);
    UiHelpers.visible(root.findViewById(R.id.more), true);
    ((TextView) root.findViewById(R.id.members))
        .setText(String.valueOf(channel ? c.members.size() : 0));
    UiHelpers.visible(root.findViewById(R.id.topic), channel);
    ((TextView) root.findViewById(R.id.topic))
        .setText(channel ? (c.topic.isEmpty() ? getString(R.string.no_topic) : c.topic) : "");
  }

  private void renderConnectionState() {
    EditText composer = root.findViewById(R.id.composer);
    boolean channel = chat instanceof Channel;
    Channel c = channel ? (Channel) chat : null;
    boolean online = session.data.connection == ConnectionState.CONNECTED;
    UiHelpers.visible(root.findViewById(R.id.banner), !online || (channel && !c.joined));
    ((TextView) root.findViewById(R.id.connection_status))
        .setText(
            online
                ? getString(R.string.not_joined)
                : getString(R.string.connection_lost_title)
                    + "\n"
                    + getString(R.string.connection_lost_detail));
    ((TextView) root.findViewById(R.id.reconnect))
        .setText(online ? R.string.join : R.string.reconnect);
    root.findViewById(R.id.reconnect).setEnabled(!online || !channel || !c.joining);
    composer.setHint(
        getString(
            channel ? R.string.chat_hint : R.string.pm_hint,
            channel ? target : UiHelpers.displayName(target)));
    ((TextView) root.findViewById(R.id.send_hint))
        .setText(
            !online
                ? getString(R.string.offline_hint)
                : channel && !c.joined ? getString(R.string.not_joined) : "");
    UiHelpers.visible(root.findViewById(R.id.send_hint), !online || (channel && !c.joined));
  }

  private void renderMessages() {
    boolean channel = chat instanceof Channel;
    if (!channel)
      ((TextView) root.findViewById(R.id.empty_detail)).setText(R.string.pm_empty_detail);
    boolean hasChat = false;
    for (ChatMessage m : chat.messages)
      if (m.event.isEmpty()) {
        hasChat = true;
        break;
      }
    UiHelpers.visible(root.findViewById(R.id.empty), !hasChat);
    RecyclerView list = root.findViewById(R.id.list);
    UiHelpers.visible(list, hasChat);
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
      if (currentSize > messageCount) UiHelpers.visible(root.findViewById(R.id.latest), true);
    }
    messageCount = currentSize;
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
