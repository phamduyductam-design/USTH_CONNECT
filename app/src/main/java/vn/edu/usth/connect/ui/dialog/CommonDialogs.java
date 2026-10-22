package vn.edu.usth.connect.ui.dialog;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.function.Consumer;
import vn.edu.usth.connect.R;
import vn.edu.usth.connect.ui.MainActivity;
import vn.edu.usth.connect.ui.UiHelpers;

/** Shared confirmation, validated text input and IRC command help. */
public final class CommonDialogs {
  private CommonDialogs() {}

  public static void confirm(
      MainActivity activity, int title, int detail, int action, Runnable accepted) {
    View view = activity.getLayoutInflater().inflate(R.layout.dialog_confirm, null);
    ((TextView) view.findViewById(R.id.confirm_title)).setText(title);
    ((TextView) view.findViewById(R.id.confirm_detail)).setText(detail);
    ((TextView) view.findViewById(R.id.confirm_accept)).setText(action);
    AlertDialog dialog = new MaterialAlertDialogBuilder(activity).setView(view).create();
    view.findViewById(R.id.confirm_cancel).setOnClickListener(v -> dialog.dismiss());
    view.findViewById(R.id.confirm_accept)
        .setOnClickListener(
            v -> {
              dialog.dismiss();
              accepted.run();
            });
    dialog.show();
  }

  public static void showCommandHelp(MainActivity activity) {
    new MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.commands_title)
        .setMessage(R.string.commands_help)
        .setPositiveButton(R.string.close, null)
        .show();
  }

  static void input(
      MainActivity activity, int title, int hint, String preset, Consumer<String> accepted) {
    View view = activity.getLayoutInflater().inflate(R.layout.dialog_input, null);
    EditText input = view.findViewById(R.id.input);
    input.setText(preset);
    input.setHint(hint);
    input.setSingleLine(true);
    AlertDialog dialog =
        new MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(R.string.apply, null)
            .setNegativeButton(R.string.cancel, null)
            .create();
    dialog.setOnShowListener(
        w ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      try {
                        accepted.accept(input.getText().toString().trim());
                        dialog.dismiss();
                      } catch (IllegalArgumentException e) {
                        input.setError(activity.getString(R.string.error_message));
                      } catch (IllegalStateException e) {
                        UiHelpers.toast(activity, R.string.error_offline);
                      }
                    }));
    dialog.show();
  }
}
