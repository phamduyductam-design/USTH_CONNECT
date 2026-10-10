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
