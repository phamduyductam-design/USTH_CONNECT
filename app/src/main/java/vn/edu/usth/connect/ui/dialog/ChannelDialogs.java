package vn.edu.usth.connect.ui.dialog;

import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import vn.edu.usth.connect.ConnectApplication;
import vn.edu.usth.connect.R;
import vn.edu.usth.connect.model.ConnectionState;
import vn.edu.usth.connect.repository.SessionRepository;
import vn.edu.usth.connect.ui.MainActivity;
import vn.edu.usth.connect.ui.UiHelpers;

/** Join, topic, room modes and channel menu. The server remains authoritative. */
public final class ChannelDialogs {
  private ChannelDialogs() {}

  // Keep the menu order in showMenu aligned with these named actions.
  private static final int ACTION_REFRESH_MEMBERS = 0;
  private static final int ACTION_EDIT_TOPIC = 1;
  private static final int ACTION_ROOM_MODES = 2;
  private static final int ACTION_COMMANDS = 3;

  public static void showJoin(MainActivity activity, String preset) {
    View view = activity.getLayoutInflater().inflate(R.layout.dialog_join_channel, null);
    EditText channel = view.findViewById(R.id.channel), key = view.findViewById(R.id.key);
    channel.setText(preset);
    AlertDialog dialog =
        new MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.join_channel)
            .setView(view)
            .setPositiveButton(R.string.join, null)
            .setNegativeButton(R.string.cancel, null)
            .create();
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      String target = channel.getText().toString().trim();
                      try {
                        ConnectApplication.from(activity)
                            .session
                            .join(target, key.getText().toString());
                        dialog.dismiss();
                        activity.openChat(target);
                      } catch (IllegalArgumentException e) {
                        channel.setError(activity.getString(R.string.error_channel));
                      } catch (IllegalStateException e) {
                        UiHelpers.toast(activity, R.string.error_offline);
                      }
                    }));
    dialog.show();
  }

  public static void showMenu(MainActivity activity, String target) {
    SessionRepository session = ConnectApplication.from(activity).session;
    int[] labels = {
      R.string.refresh_members,
      R.string.edit_topic,
      R.string.room_modes,
      R.string.commands_title,
      R.string.leave
    };
    String[] items = new String[labels.length];
    for (int i = 0; i < labels.length; i++) items[i] = activity.getString(labels[i]);
    new MaterialAlertDialogBuilder(activity)
        .setTitle(target)
        .setItems(
            items,
            (d, which) -> {
              if (which == ACTION_COMMANDS) {
                CommonDialogs.showCommandHelp(activity);
                return;
              }
              if (!session.canSend(target)) {
                UiHelpers.toast(activity, R.string.error_offline);
                return;
              }
              if (which == ACTION_REFRESH_MEMBERS) session.refreshNames(target);
              else if (which == ACTION_EDIT_TOPIC) {
                if (session.data.channel(target).modes.contains('t')
                    && !session.data.canManage(target))
                  UiHelpers.toast(activity, R.string.operator_required);
                else
                  CommonDialogs.input(
                      activity,
                      R.string.edit_topic,
                      R.string.topic_hint,
                      session.data.channel(target).topic,
                      text -> session.topic(target, text));
              } else if (which == ACTION_ROOM_MODES) showRoomModes(activity, target);
              else
                new MaterialAlertDialogBuilder(activity)
                    .setTitle(activity.getString(R.string.leave_title, target))
                    .setMessage(R.string.leave_detail)
                    .setPositiveButton(
                        R.string.leave,
                        (dialog, action) -> {
                          session.part(target);
                          activity.back();
                        })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            })
        .show();
  }

  private static void showRoomModes(MainActivity activity, String target) {
    SessionRepository session = ConnectApplication.from(activity).session;
    if (!session.data.canManage(target)) {
      UiHelpers.toast(activity, R.string.operator_required);
      return;
    }
    char[] flags = {'i', 'm', 't'};
    boolean[] before = new boolean[flags.length];
    for (int i = 0; i < flags.length; i++)
      before[i] = session.data.channel(target).modes.contains(flags[i]);
    boolean[] selected = before.clone();
    String[] labels = {
      activity.getString(R.string.mode_invite),
      activity.getString(R.string.mode_moderated),
      activity.getString(R.string.mode_topic)
    };
    new MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.room_modes)
        .setMultiChoiceItems(labels, selected, (d, which, checked) -> selected[which] = checked)
        .setPositiveButton(
            R.string.apply,
            (d, which) -> {
              if (session.data.connection != ConnectionState.CONNECTED) {
                UiHelpers.toast(activity, R.string.error_offline);
                return;
              }
              for (int i = 0; i < flags.length; i++)
                if (selected[i] != before[i])
                  session.mode(target, (selected[i] ? "+" : "-") + flags[i], "");
            })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }
}
