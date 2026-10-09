package vn.edu.usth.connect.adapter;

import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;
import vn.edu.usth.connect.R;
import vn.edu.usth.connect.model.ChatMessage;
import vn.edu.usth.connect.ui.Ui;

public final class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.Holder> {
  public interface Retry {
    void retry(ChatMessage message);
  }

  private static final class Row {
    final ChatMessage message;
    final String separator;
    final boolean unread;

    Row(ChatMessage message, String separator, boolean unread) {
      this.message = message;
      this.separator = separator;
      this.unread = unread;
    }
  }

  private final List<Row> rows = new ArrayList<>();
  private final Retry retry;
  private final Context context;

  public ChatAdapter(Context context, Retry retry) {
    this.context = context;
    this.retry = retry;
    setHasStableIds(true);
  }

  public void submit(List<ChatMessage> messages, long unreadId) {
    rows.clear();
    String previousDate = "";
    for (ChatMessage message : messages) {
      String date = Ui.date(context, message.timestamp);
      if (!date.equals(previousDate)) {
        rows.add(new Row(null, date, false));
        previousDate = date;
      }
      if (message.id == unreadId)
        rows.add(new Row(null, context.getString(R.string.new_messages), true));
      rows.add(new Row(message, "", false));
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
            .inflate(type == 1 ? R.layout.item_separator : R.layout.item_message, parent, false));
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
          ContextCompat.getColor(context, row.unread ? R.color.badge : R.color.text_secondary));
      return;
    }
    Ui.avatar(v.findViewById(R.id.avatar), m.sender);
    ((TextView) v.findViewById(R.id.name)).setText(m.sender);
    ((TextView) v.findViewById(R.id.time)).setText(Ui.time(m.timestamp));
    ((TextView) v.findViewById(R.id.text)).setText(m.text);
    TextView status = v.findViewById(R.id.status);
    Ui.visible(status, m.outgoing);
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
    Ui.visible(
        button, m.status == ChatMessage.Status.FAILED || m.status == ChatMessage.Status.UNCERTAIN);
    button.setOnClickListener(ignored -> retry.retry(m));
  }
}
