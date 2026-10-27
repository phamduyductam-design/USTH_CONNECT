package vn.edu.usth.connect.ui.chat;

import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.*;
import androidx.recyclerview.widget.*;
import vn.edu.usth.connect.R;
import vn.edu.usth.connect.adapter.ChatAdapter;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.irc.SlashCommand;
import vn.edu.usth.connect.model.*;
import vn.edu.usth.connect.ui.common.SessionAwareFragment;
import vn.edu.usth.connect.ui.common.UiHelpers;
import vn.edu.usth.connect.ui.dialog.ChannelDialogs;
import vn.edu.usth.connect.ui.dialog.CommonDialogs;
import vn.edu.usth.connect.ui.dialog.UserDialogs;
import vn.edu.usth.connect.ui.members.MembersBottomSheet;

public final class ChatFragment extends SessionAwareFragment {
  private String target;
  private Conversation chat;
  private ChatAdapter adapter;
  private Parcelable scrollState;
  private long unreadBoundary;
  private boolean firstRender = true, pinned = true;
  private long lastMessageId;
  private boolean newMessagesPending;
  private boolean draftOwner;
  private String observedDraft = "";
  private boolean searchOpen, normalPinned;
  private String searchQuery = "";
  private Parcelable normalScrollState;
  private OnBackPressedCallback searchBack;

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
      searchOpen = saved.getBoolean("search_open");
      searchQuery = saved.getString("search_query", "");
      normalScrollState = saved.getParcelable("search_normal_scroll");
      normalPinned = saved.getBoolean("search_normal_pinned", true);
      lastMessageId = saved.getLong("last_message_id");
      newMessagesPending = saved.getBoolean("new_messages_pending");
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
    adapter = new ChatAdapter(requireContext(), this::retry, !IrcParser.isChannel(target));
    list.setAdapter(adapter);
    list.addOnScrollListener(
        new RecyclerView.OnScrollListener() {
          @Override
          public void onScrolled(@NonNull RecyclerView recycler, int dx, int dy) {
            if (dy != 0 && !searchOpen) {
              pinned = !recycler.canScrollVertically(1);
              if (pinned) {
                newMessagesPending = false;
                UiHelpers.visible(v.findViewById(R.id.latest), false);
              }
            }
          }
        });
    v.findViewById(R.id.back).setOnClickListener(w -> main().back());
    v.findViewById(R.id.chat_commands)
        .setOnClickListener(w -> CommonDialogs.showCommandHelp(main()));
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
    v.findViewById(R.id.more).setOnClickListener(w -> showChatMenu());
    EditText search = v.findViewById(R.id.chat_search_query);
    search.setText(searchQuery);
    UiHelpers.visible(v.findViewById(R.id.chat_search_panel), searchOpen);
    UiHelpers.watch(
        search,
        () -> {
          String query = search.getText().toString();
          if (!searchOpen || query.equals(searchQuery)) return;
          searchQuery = query;
          pinned = false;
          renderMessages();
          list.scrollToPosition(0);
        });
    v.findViewById(R.id.chat_search_close).setOnClickListener(w -> closeSearch());
    searchBack =
        new OnBackPressedCallback(searchOpen) {
          @Override
          public void handleOnBackPressed() {
            closeSearch();
          }
        };
    requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), searchBack);
    EditText composer = v.findViewById(R.id.composer);
    observedDraft = session.draft(target);
    composer.setText(observedDraft);
    UiHelpers.watch(
        composer,
        () -> {
          composer.setError(null);
          if (draftOwner) {
            session.saveDraft(target, composer.getText().toString());
            observedDraft = session.draft(target);
          }
          updateSend();
        });
    v.findViewById(R.id.send).setOnClickListener(w -> send());
    v.findViewById(R.id.latest)
        .setOnClickListener(
            w -> {
              pinned = true;
              newMessagesPending = false;
              list.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
              UiHelpers.visible(v.findViewById(R.id.latest), false);
            });
  }

  @Override
  public void onStart() {
    super.onStart();
    if (!IrcParser.isChannel(target) && session.data.connection == ConnectionState.CONNECTED)
      session.refreshWho(target);
  }

  @Override
  public void onStop() {
    updateChatVisibility(false);
    super.onStop();
  }

  @Override
  public void onResume() {
    super.onResume();
    updateChatVisibility(requireActivity().hasWindowFocus());
  }

  @Override
  public void onPause() {
    updateChatVisibility(false);
    super.onPause();
  }

  /** Window focus also excludes sheets and dialogs that leave the Fragment resumed. */
  public void updateChatVisibility(boolean focused) {
    boolean visible = focused && isResumed() && isVisible() && root != null;
    if (!visible) {
      if (draftOwner && root != null) {
        syncTargetAndDraft();
        session.saveDraft(
            target, ((EditText) root.findViewById(R.id.composer)).getText().toString());
        observedDraft = session.draft(target);
      }
      draftOwner = false;
      session.clearActive(target);
      return;
    }
    if (!draftOwner) {
      syncTargetAndDraft();
      unreadBoundary = chat.firstUnreadId;
      EditText composer = root.findViewById(R.id.composer);
      String draft = session.draft(target);
      // A dialog should not discard an in-memory JOIN/MODE that is deliberately not persisted.
      if (!observedDraft.equals(draft)) composer.setText(draft);
      observedDraft = draft;
      draftOwner = true;
    }
    session.setActive(target);
  }

  private void send() {
    if (searchOpen) closeSearch();
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
    if (IrcParser.isChannel(target)) ChannelDialogs.showMenu(main(), target, this::showSearch);
    else UserDialogs.showPrivateInfo(main(), target, this::showSearch);
  }

  private void showSearch() {
    if (root == null) return;
    if (!searchOpen) {
      normalScrollState =
          ((RecyclerView) root.findViewById(R.id.list)).getLayoutManager().onSaveInstanceState();
      normalPinned = pinned;
    }
    searchOpen = true;
    pinned = false;
    searchBack.setEnabled(true);
    UiHelpers.visible(root.findViewById(R.id.chat_search_panel), true);
    UiHelpers.visible(root.findViewById(R.id.latest), false);
    root.findViewById(R.id.chat_search_query).requestFocus();
    renderMessages();
  }

  /** Closing search keeps this chat visible and retains ownership of its draft. */
  public boolean closeSearchIfOpen() {
    if (!searchOpen || root == null) return false;
    closeSearch();
    return true;
  }

  private void closeSearch() {
    if (root == null) return;
    searchOpen = false;
    searchQuery = "";
    searchBack.setEnabled(false);
    ((EditText) root.findViewById(R.id.chat_search_query)).setText("");
    UiHelpers.visible(root.findViewById(R.id.chat_search_panel), false);
    android.view.inputmethod.InputMethodManager keyboard =
        requireContext().getSystemService(android.view.inputmethod.InputMethodManager.class);
    if (keyboard != null) keyboard.hideSoftInputFromWindow(root.getWindowToken(), 0);
    pinned = normalPinned;
    scrollState = normalPinned ? null : normalScrollState;
    firstRender = true;
    renderMessages();
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
    send.setAlpha(enabled ? 1f : 0.4f);
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
    // The destination of a renamed PM can be merged into a different conversation object.
    EditText composer = root.findViewById(R.id.composer);
    String oldTarget = target;
    target = chat.target;
    Conversation current = session.data.conversation(target);
    if (current != chat) chat = current;
    target = chat.target;
    if (!oldTarget.equals(target) && draftOwner) {
      String draft = composer.getText().toString();
      session.saveDraft(oldTarget, "");
      session.saveDraft(target, draft);
      observedDraft = session.draft(target);
    }
    if (draftOwner) {
      session.data.activeTarget = target;
      session.data.markRead(target);
    }
  }

  private void renderHeader() {
    boolean channel = chat instanceof Channel;
    Channel c = channel ? (Channel) chat : null;
    ((TextView) root.findViewById(R.id.title))
        .setText(channel ? target : UiHelpers.displayName(target));
    TextView avatar = root.findViewById(R.id.chat_avatar);
    if (channel) {
      avatar.setText("#");
      avatar.setBackgroundTintList(
          android.content.res.ColorStateList.valueOf(
              androidx.core.content.ContextCompat.getColor(requireContext(), R.color.selected)));
      avatar.setTextColor(
          androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
    } else UiHelpers.avatar(avatar, target);
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
    String query = searchOpen ? searchQuery.trim() : "";
    java.util.List<ChatMessage> messages = MessageSearch.filter(chat.messages, query);
    ((TextView) root.findViewById(R.id.chat_search_count))
        .setText(
            query.isEmpty()
                ? getString(R.string.search_messages_scope)
                : getResources()
                    .getQuantityString(
                        R.plurals.search_messages_count, messages.size(), messages.size()));
    ((TextView) root.findViewById(R.id.empty_title))
        .setText(query.isEmpty() ? R.string.chat_empty : R.string.search_messages_none);
    ((TextView) root.findViewById(R.id.empty_detail))
        .setText(
            !query.isEmpty()
                ? R.string.search_messages_none_detail
                : channel ? R.string.chat_empty_detail : R.string.pm_empty_detail);
    boolean hasChat = false;
    for (ChatMessage m : messages)
      if (m.event.isEmpty()) {
        hasChat = true;
        break;
      }
    UiHelpers.visible(root.findViewById(R.id.empty), !hasChat);
    RecyclerView list = root.findViewById(R.id.list);
    UiHelpers.visible(list, hasChat);
    Parcelable anchor = list.getLayoutManager().onSaveInstanceState();
    long currentLastMessageId =
        chat.messages.isEmpty() ? 0 : chat.messages.get(chat.messages.size() - 1).id;
    // IDs still advance when the bounded history is full, unlike its row count.
    if (currentLastMessageId > lastMessageId && !(searchOpen ? normalPinned : pinned))
      newMessagesPending = true;
    adapter.submit(messages, unreadBoundary, query);
    if (firstRender) {
      if (scrollState != null) list.getLayoutManager().onRestoreInstanceState(scrollState);
      else list.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
      firstRender = false;
    } else if (pinned) list.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
    else {
      list.getLayoutManager().onRestoreInstanceState(anchor);
    }
    if (!searchOpen && pinned) newMessagesPending = false;
    UiHelpers.visible(root.findViewById(R.id.latest), !searchOpen && newMessagesPending);
    lastMessageId = currentLastMessageId;
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle out) {
    super.onSaveInstanceState(out);
    out.putString("target", target);
    out.putLong("unread", unreadBoundary);
    out.putBoolean("pinned", pinned);
    out.putBoolean("search_open", searchOpen);
    out.putString("search_query", searchQuery);
    out.putParcelable("search_normal_scroll", normalScrollState);
    out.putBoolean("search_normal_pinned", normalPinned);
    out.putLong("last_message_id", lastMessageId);
    out.putBoolean("new_messages_pending", newMessagesPending);
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
      // onPause saved the owner draft; a covered/stale view must not overwrite a newer one.
    }
    adapter = null;
    super.onDestroyView();
  }
}
