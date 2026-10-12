package vn.edu.usth.connect.ui;

import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import vn.edu.usth.connect.R;
import vn.edu.usth.connect.irc.IrcCommands;

final class Dialogs {
  static void confirm(MainActivity activity, int title, int detail, int action, Runnable accepted) {
    View view = activity.getLayoutInflater().inflate(R.layout.dialog_confirm, null);
    ((android.widget.TextView) view.findViewById(R.id.confirm_title)).setText(title);
    ((android.widget.TextView) view.findViewById(R.id.confirm_detail)).setText(detail);
    ((android.widget.TextView) view.findViewById(R.id.confirm_accept)).setText(action);
    AlertDialog dialog = new MaterialAlertDialogBuilder(activity).setView(view).create();
    view.findViewById(R.id.confirm_cancel).setOnClickListener(v -> dialog.dismiss());
    view.findViewById(R.id.confirm_accept).setOnClickListener(v -> { dialog.dismiss(); accepted.run(); });
    dialog.show();
  }

  static void help(MainActivity activity) {
    new MaterialAlertDialogBuilder(activity).setTitle(R.string.commands_title)
        .setMessage(R.string.commands_help).setPositiveButton(R.string.close, null).show();
  }

  private static void input(MainActivity activity, int title, int hint, String preset,
      java.util.function.Consumer<String> accepted) {
    View view = activity.getLayoutInflater().inflate(R.layout.dialog_input, null);
    EditText input = view.findViewById(R.id.input);
    input.setText(preset); input.setHint(hint); input.setSingleLine(true);
    AlertDialog dialog = new MaterialAlertDialogBuilder(activity).setTitle(title).setView(view)
        .setPositiveButton(R.string.apply, null).setNegativeButton(R.string.cancel, null).create();
    dialog.setOnShowListener(w -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
      try { accepted.accept(input.getText().toString().trim()); dialog.dismiss(); }
      catch (IllegalArgumentException e) { input.setError(activity.getString(R.string.error_message)); }
      catch (IllegalStateException e) { Ui.toast(activity, R.string.error_offline); }
    }));
    dialog.show();
  }

  static void away(MainActivity activity) {
    input(activity, R.string.away_set, R.string.away_reason, activity.getString(R.string.away_default),
        text -> vn.edu.usth.connect.ConnectApplication.from(activity).session.setAway(text));
  }

  static void member(MainActivity activity, String target, String nick) {
    vn.edu.usth.connect.repository.SessionRepository session = vn.edu.usth.connect.ConnectApplication.from(activity).session;
    int[] labels = {R.string.give_operator, R.string.remove_operator,
        R.string.give_voice, R.string.remove_voice, R.string.kick_member};
    String[] items = new String[labels.length];
    for (int i = 0; i < labels.length; i++) items[i] = activity.getString(labels[i]);
    new MaterialAlertDialogBuilder(activity).setTitle(nick).setItems(items, (dialog, which) -> {
      if (!session.data.canManage(target)) { Ui.toast(activity, R.string.operator_required); return; }
      if (which == 4) {
        input(activity, R.string.kick_member, R.string.kick_reason, "",
            reason -> session.kick(target, nick, reason));
      } else session.mode(target, new String[] {"+o", "-o", "+v", "-v"}[which], nick);
    }).show();
  }

  private static void roomModes(MainActivity activity, String target) {
    vn.edu.usth.connect.repository.SessionRepository session = vn.edu.usth.connect.ConnectApplication.from(activity).session;
    if (!session.data.canManage(target)) { Ui.toast(activity, R.string.operator_required); return; }
    char[] flags = {'i', 'm', 't'};
    boolean[] before = new boolean[flags.length];
    for (int i = 0; i < flags.length; i++) before[i] = session.data.channel(target).modes.contains(flags[i]);
    boolean[] selected = before.clone();
    String[] labels = {activity.getString(R.string.mode_invite), activity.getString(R.string.mode_moderated), activity.getString(R.string.mode_topic)};
    new MaterialAlertDialogBuilder(activity).setTitle(R.string.room_modes)
        .setMultiChoiceItems(labels, selected, (d, which, checked) -> selected[which] = checked)
        .setPositiveButton(R.string.apply, (d, which) -> {
          if (session.data.connection != vn.edu.usth.connect.model.ConnectionState.CONNECTED) {
            Ui.toast(activity, R.string.error_offline); return;
          }
          for (int i = 0; i < flags.length; i++) if (selected[i] != before[i])
            session.mode(target, (selected[i] ? "+" : "-") + flags[i], "");
        }).setNegativeButton(R.string.cancel, null).show();
  }

  static void chatMenu(MainActivity activity, String target) {
    vn.edu.usth.connect.repository.SessionRepository session = vn.edu.usth.connect.ConnectApplication.from(activity).session;
    if (!vn.edu.usth.connect.irc.IrcParser.isChannel(target)) {
      AlertDialog dialog = new MaterialAlertDialogBuilder(activity).setTitle(target)
          .setPositiveButton(R.string.commands_title, (d, which) -> help(activity))
          .setNegativeButton(R.string.close, null).create();
      vn.edu.usth.connect.repository.SessionRepository.Listener listener = () -> {
        String key = vn.edu.usth.connect.irc.IrcParser.fold(target);
        String status = session.data.awayUsers.containsKey(key)
            ? activity.getString(R.string.away_detail, session.data.awayUsers.get(key))
            : activity.getString(session.data.onlineUsers.contains(key) ? R.string.online_presence : R.string.presence_unknown);
        dialog.setMessage(status);
      };
      androidx.lifecycle.LifecycleEventObserver observer = (owner, event) -> {
        if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) dialog.dismiss();
      };
      dialog.setOnDismissListener(d -> {
        session.removeListener(listener); activity.getLifecycle().removeObserver(observer);
      });
      dialog.show(); activity.getLifecycle().addObserver(observer); session.addListener(listener);
      session.refreshWho(target);
      return;
    }
    int[] labels = {R.string.refresh_members, R.string.edit_topic, R.string.room_modes, R.string.commands_title, R.string.leave};
    String[] items = new String[labels.length];
    for (int i = 0; i < labels.length; i++) items[i] = activity.getString(labels[i]);
    new MaterialAlertDialogBuilder(activity).setTitle(target).setItems(items, (d, which) -> {
      if (which == 3) { help(activity); return; }
      if (!session.canSend(target)) { Ui.toast(activity, R.string.error_offline); return; }
      if (which == 0) session.refreshNames(target);
      else if (which == 1) {
        if (session.data.channel(target).modes.contains('t') && !session.data.canManage(target))
          Ui.toast(activity, R.string.operator_required);
        else input(activity, R.string.edit_topic, R.string.topic_hint, session.data.channel(target).topic,
            text -> session.topic(target, text));
      } else if (which == 2) roomModes(activity, target);
      else new MaterialAlertDialogBuilder(activity).setTitle(activity.getString(R.string.leave_title, target))
          .setMessage(R.string.leave_detail).setPositiveButton(R.string.leave, (dialog, action) -> {
            session.part(target); activity.back();
          }).setNegativeButton(R.string.cancel, null).show();
    }).show();
  }

  static void join(MainActivity activity, String preset) {
    View view = activity.getLayoutInflater().inflate(R.layout.dialog_join, null);
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
                        vn.edu.usth.connect.ConnectApplication.from(activity)
                            .session
                            .join(target, key.getText().toString());
                        dialog.dismiss();
                        activity.openChat(target);
                      } catch (IllegalArgumentException e) {
                        channel.setError(activity.getString(R.string.error_channel));
                      } catch (IllegalStateException e) {
                        Ui.toast(activity, R.string.error_offline);
                      }
                    }));
    dialog.show();
  }

  static void newConversation(MainActivity activity) {
    View view = activity.getLayoutInflater().inflate(R.layout.dialog_input, null);
    EditText input = view.findViewById(R.id.input);
    AlertDialog dialog =
        new MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.new_conversation)
            .setView(view)
            .setPositiveButton(R.string.private_message, null)
            .setNegativeButton(R.string.cancel, null)
            .create();
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      String nick = input.getText().toString().trim();
                      try {
                        IrcCommands.nick(nick);
                        dialog.dismiss();
                        activity.openChat(nick);
                      } catch (IllegalArgumentException e) {
                        input.setError(activity.getString(R.string.error_nick));
                      }
                    }));
    dialog.show();
  }
}
