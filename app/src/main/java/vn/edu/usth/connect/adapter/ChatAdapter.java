package vn.edu.usth.connect.adapter;

import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;
import vn.edu.usth.connect.R;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.model.ChatMessage;
import vn.edu.usth.connect.ui.common.UiHelpers;

public final class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.Holder> {
  public interface Retry {
    void retry(ChatMessage message);
  }

  private static final class Row {
    final ChatMessage message;
    final String separator;
    final boolean unread;
    final boolean grouped;

    Row(ChatMessage message, String separator, boolean unread, boolean grouped) {
      this.message = message;
      this.separator = separator;
      this.unread = unread;
      this.grouped = grouped;
    }
  }

  private final List<Row> rows = new ArrayList<>();
  private final Retry retry;
  private final Context context;
  private final boolean privateChat;

  public ChatAdapter(Context context, Retry retry, boolean privateChat) {
    this.context = context;
    this.retry = retry;
    this.privateChat = privateChat;
    setHasStableIds(true);
  }

  public void submit(List<ChatMessage> messages, long unreadId) {
    rows.clear();
    String previousDate = "";
    ChatMessage previous = null;
    for (ChatMessage message : messages) {
      String date = UiHelpers.date(context, message.timestamp);
      boolean newDay = !date.equals(previousDate);
      if (newDay) {
        rows.add(new Row(null, date, false, false));
        previousDate = date;
      }
      if (message.id == unreadId)
        rows.add(new Row(null, context.getString(R.string.new_messages), true, false));
      boolean grouped =
          !newDay
              && message.id != unreadId
              && previous != null
              && previous.event.isEmpty()
              && message.event.isEmpty()
              && previous.outgoing == message.outgoing
              && IrcParser.fold(previous.sender).equals(IrcParser.fold(message.sender))
              && message.timestamp >= previous.timestamp
              && message.timestamp - previous.timestamp <= 5 * 60 * 1000;
      rows.add(new Row(message, "", false, grouped));
      previous = message;
    }
    notifyDataSetChanged();
  }

  @Override
  public int getItemCount() {
    return rows.size();
  }

  @Override
  public long getItemId(int position) {
    Row row = rows.get(position);
    return row.message != null ? row.message.id : -1L - position;
  }

  @Override
  public int getItemViewType(int position) {
    Row row = rows.get(position);
    return row.message == null || !row.message.event.isEmpty() ? 1 : 0;
  }

  static final class Holder extends RecyclerView.ViewHolder {
    Holder(View v) {
      super(v);
    }
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
    return new Holder(
        LayoutInflater.from(parent.getContext())
            .inflate(
                type == 1 ? R.layout.item_chat_separator : R.layout.item_chat_message,
                parent,
                false));
  }

  @Override
  public void onBindViewHolder(@NonNull Holder h, int position) {
    Row row = rows.get(position);
    View v = h.itemView;
    ChatMessage m = row.message;
    if (getItemViewType(position) == 1) {
      TextView text = v.findViewById(R.id.text);
      String value = row.separator;
      if (m != null) {
        int resource =
            m.event.equals("JOIN")
                ? R.string.event_join
                : m.event.equals("PART")
                    ? R.string.event_part
                    : m.event.equals("KICK")
                        ? R.string.event_kick
                        : m.event.equals("NICK") ? R.string.event_nick : R.string.event_quit;
        value = context.getString(resource, m.sender, m.text);
      }
      text.setText(value);
      text.setTextColor(
          ContextCompat.getColor(context, row.unread ? R.color.error : R.color.text_secondary));
      text.setBackgroundResource(m == null && !row.unread ? R.drawable.bg_chat_date : 0);
      UiHelpers.visible(v.findViewById(R.id.separator_left), row.unread);
      UiHelpers.visible(v.findViewById(R.id.separator_right), row.unread);
      return;
    }
    LinearLayout container = (LinearLayout) v;
    boolean outgoingPrivate = privateChat && m.outgoing;
    container.setGravity(outgoingPrivate ? Gravity.END : Gravity.START);
    container.setPadding(0, dp(row.grouped ? 3 : 12), 0, dp(4));
    TextView avatar = v.findViewById(R.id.avatar);
    UiHelpers.avatar(avatar, m.sender);
    avatar.setVisibility(outgoingPrivate ? View.GONE : row.grouped ? View.INVISIBLE : View.VISIBLE);
    LinearLayout contentContainer = v.findViewById(R.id.message_content);
    LinearLayout.LayoutParams contentParams =
        (LinearLayout.LayoutParams) contentContainer.getLayoutParams();
    contentParams.setMarginStart(outgoingPrivate ? dp(44) : dp(10));
    contentContainer.setLayoutParams(contentParams);
    contentContainer.setGravity(outgoingPrivate ? Gravity.END : Gravity.START);
    UiHelpers.visible(v.findViewById(R.id.message_heading), !privateChat && !row.grouped);
    ((TextView) v.findViewById(R.id.name)).setText(m.sender);
    ((TextView) v.findViewById(R.id.time)).setText(UiHelpers.time(m.timestamp));
    TextView body = v.findViewById(R.id.text);
    int width = context.getResources().getDisplayMetrics().widthPixels;
    body.setMaxWidth(Math.max(dp(120), width - dp(privateChat ? 96 : 88)));
    android.text.SpannableString content = new android.text.SpannableString(m.text);
    for (int[] range : vn.edu.usth.connect.irc.NicknameMentions.ranges(m.text, m.mentionNick)) {
      content.setSpan(
          new android.text.style.BackgroundColorSpan(
              ContextCompat.getColor(context, R.color.selected)),
          range[0],
          range[1],
          android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      content.setSpan(
          new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
          range[0],
          range[1],
          android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      content.setSpan(
          new android.text.style.ForegroundColorSpan(
              ContextCompat.getColor(context, R.color.primary)),
          range[0],
          range[1],
          android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    body.setText(content);
    body.setBackgroundResource(
        outgoingPrivate ? R.drawable.bg_chat_outgoing : R.drawable.bg_chat_incoming);
    TextView messageTime = v.findViewById(R.id.message_time);
    messageTime.setText(UiHelpers.time(m.timestamp));
    UiHelpers.visible(messageTime, privateChat);
    TextView status = v.findViewById(R.id.status);
    UiHelpers.visible(
        status,
        m.outgoing
            && m.status != ChatMessage.Status.WRITTEN
            && m.status != ChatMessage.Status.RECEIVED);
    int resource =
        m.status == ChatMessage.Status.QUEUED
            ? R.string.queued
            : m.status == ChatMessage.Status.FAILED
                ? R.string.send_failed
                : m.status == ChatMessage.Status.UNCERTAIN
                    ? R.string.send_uncertain
                    : R.string.written;
    status.setText(resource);
    status.setTextColor(
        ContextCompat.getColor(
            context,
            m.status == ChatMessage.Status.FAILED || m.status == ChatMessage.Status.UNCERTAIN
                ? R.color.error
                : R.color.text_secondary));
    View button = v.findViewById(R.id.retry);
    UiHelpers.visible(
        button, m.status == ChatMessage.Status.FAILED || m.status == ChatMessage.Status.UNCERTAIN);
    button.setOnClickListener(ignored -> retry.retry(m));
  }

  private int dp(int value) {
    return Math.round(value * context.getResources().getDisplayMetrics().density);
  }
}
