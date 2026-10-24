package vn.edu.usth.connect.ui.dialog;

import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import vn.edu.usth.connect.ConnectApplication;
import vn.edu.usth.connect.R;
import vn.edu.usth.connect.irc.IrcCommands;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.repository.IrcSessionController;
import vn.edu.usth.connect.ui.MainActivity;
import vn.edu.usth.connect.ui.common.UiHelpers;

/** Private conversations, member privileges and away presence. */
public final class UserDialogs {
  private UserDialogs() {}

  private static final int ACTION_KICK = 4;

  public static void showNewConversation(MainActivity activity) {
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

  public static void showMemberActions(MainActivity activity, String target, String nick) {
    IrcSessionController session = ConnectApplication.from(activity).session;
    int[] labels = {
      R.string.give_operator,
      R.string.remove_operator,
      R.string.give_voice,
      R.string.remove_voice,
      R.string.kick_member
    };
    String[] items = new String[labels.length];
    for (int i = 0; i < labels.length; i++) items[i] = activity.getString(labels[i]);
    new MaterialAlertDialogBuilder(activity)
        .setTitle(nick)
        .setItems(
            items,
            (dialog, which) -> {
              if (!session.data.canManage(target)) {
                UiHelpers.toast(activity, R.string.operator_required);
                return;
              }
              if (which == ACTION_KICK) {
                CommonDialogs.input(
                    activity,
                    R.string.kick_member,
                    R.string.kick_reason,
                    "",
                    reason -> session.kick(target, nick, reason));
              } else session.mode(target, new String[] {"+o", "-o", "+v", "-v"}[which], nick);
            })
        .show();
  }

  public static void showAway(MainActivity activity) {
    CommonDialogs.input(
        activity,
        R.string.away_set,
        R.string.away_reason,
        activity.getString(R.string.away_default),
        text -> ConnectApplication.from(activity).session.setAway(text));
  }

  public static void showPrivateInfo(MainActivity activity, String target) {
    IrcSessionController session = ConnectApplication.from(activity).session;
    AlertDialog dialog =
        new MaterialAlertDialogBuilder(activity)
            .setTitle(target)
            .setPositiveButton(
                R.string.commands_title, (d, which) -> CommonDialogs.showCommandHelp(activity))
            .setNegativeButton(R.string.close, null)
            .create();
    IrcSessionController.Listener listener =
        () -> {
          String key = IrcParser.fold(target);
          String status =
              session.data.awayUsers.containsKey(key)
                  ? activity.getString(R.string.away_detail, session.data.awayUsers.get(key))
                  : activity.getString(
                      session.data.onlineUsers.contains(key)
                          ? R.string.online_presence
                          : R.string.presence_unknown);
          dialog.setMessage(status);
        };
    LifecycleEventObserver observer =
        (owner, event) -> {
          if (event == Lifecycle.Event.ON_DESTROY) dialog.dismiss();
        };
    dialog.setOnDismissListener(
        d -> {
          session.removeListener(listener);
          activity.getLifecycle().removeObserver(observer);
        });
    dialog.show();
    activity.getLifecycle().addObserver(observer);
    session.addListener(listener);
    session.refreshWho(target);
  }
}
