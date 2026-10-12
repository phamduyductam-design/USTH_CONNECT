package vn.edu.usth.connect.adapter;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.*;
import vn.edu.usth.connect.R;
import vn.edu.usth.connect.ui.Ui;

/** Shared visual rows for course/channel/PM/member lists. The fragments provide behavior. */
public final class CatalogAdapter extends RecyclerView.Adapter<CatalogAdapter.Holder> {
  public static final class Row {
    public int type, icon = R.drawable.ic_hash, badge, iconColor, actionText;
    public String name = "", detail = "", preview = "", time = "", avatar = "";
    public boolean selected, joinVisible, joining, self;
    public boolean enabled = true;
    public Runnable click, join;

    public static Row header(String title) {
      Row r = new Row();
      r.type = 1;
      r.name = title;
      return r;
    }

    public static Row empty(String title, String detail) {
      Row r = new Row();
      r.type = 2;
      r.name = title;
      r.detail = detail;
      return r;
    }
  }

  private final List<Row> rows = new ArrayList<>();

  public void submit(List<Row> items) {
    rows.clear();
    rows.addAll(items);
    notifyDataSetChanged();
  }

  @Override
  public int getItemViewType(int position) {
    return rows.get(position).type;
  }

  @Override
  public int getItemCount() {
    return rows.size();
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
                type == 1
                    ? R.layout.item_header
                    : type == 2 ? R.layout.item_empty : R.layout.item_catalog,
                parent,
                false));
  }

  @Override
  public void onBindViewHolder(@NonNull Holder h, int position) {
    Row r = rows.get(position);
    View v = h.itemView;
    if (r.type == 1) {
      ((TextView) v.findViewById(R.id.text)).setText(r.name);
      return;
    }
    if (r.type == 2) {
      ((TextView) v.findViewById(R.id.empty_title)).setText(r.name);
      ((TextView) v.findViewById(R.id.empty_detail)).setText(r.detail);
      return;
    }
    ((TextView) v.findViewById(R.id.name)).setText(r.name);
    ((TextView) v.findViewById(R.id.detail)).setText(r.detail);
    Ui.visible(v.findViewById(R.id.detail), !r.detail.isEmpty());
    Ui.visible(v.findViewById(R.id.self_badge), r.self);
    TextView preview = v.findViewById(R.id.preview);
    preview.setText(r.preview);
    Ui.visible(preview, !r.preview.isEmpty());
    TextView time = v.findViewById(R.id.time);
    time.setText(r.time);
    Ui.visible(time, !r.time.isEmpty());
    TextView badge = v.findViewById(R.id.badge);
    badge.setText(String.valueOf(r.badge));
    Ui.visible(badge, r.badge > 0);
    TextView avatar = v.findViewById(R.id.avatar);
    Ui.visible(avatar, !r.avatar.isEmpty());
    ImageView icon = v.findViewById(R.id.icon);
    Ui.visible(icon, r.avatar.isEmpty());
    icon.setImageResource(r.icon);
    icon.setBackgroundResource(r.iconColor == 0 ? 0 : R.drawable.bg_primary);
    icon.setBackgroundTintList(r.iconColor == 0 ? null : android.content.res.ColorStateList.valueOf(
        androidx.core.content.ContextCompat.getColor(v.getContext(), r.iconColor)));
    icon.setImageTintList(android.content.res.ColorStateList.valueOf(
        androidx.core.content.ContextCompat.getColor(v.getContext(),
            r.iconColor == 0 ? R.color.text_secondary : R.color.on_primary)));
    android.view.ViewGroup.LayoutParams iconParams = v.findViewById(R.id.icon_container).getLayoutParams();
    iconParams.width = (int) (v.getResources().getDisplayMetrics().density *
        (r.avatar.isEmpty() && r.iconColor == 0 ? 28 : 40));
    v.findViewById(R.id.icon_container).setLayoutParams(iconParams);
    if (!r.avatar.isEmpty()) Ui.avatar(avatar, r.avatar);
    Ui.visible(v.findViewById(R.id.chevron), r.click != null && !r.joinVisible && !r.self);
    MaterialButton join = v.findViewById(R.id.join);
    Ui.visible(join, r.joinVisible);
    join.setText(r.joining ? R.string.joining : r.actionText == 0 ? R.string.join : r.actionText);
    join.setEnabled(r.enabled && !r.joining);
    join.setOnClickListener(
        ignored -> {
          if (r.join != null) r.join.run();
        });
    v.setBackgroundResource(r.selected ? R.drawable.bg_selected : R.drawable.bg_row);
    v.setOnClickListener(
        ignored -> {
          if (r.click != null) r.click.run();
        });
    v.setClickable(r.click != null);
    v.setFocusable(r.click != null);
  }
}
