package vn.edu.usth.connect.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import vn.edu.usth.connect.R;
import vn.edu.usth.connect.model.*;

public final class ProfileFragment extends SessionFragment {
  private boolean binding;
  private String renderedNick = "";
  private final ActivityResultLauncher<String> permission =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          granted -> {
            app.settings.notifications(granted);
            if (!granted) Ui.toast(requireContext(), R.string.permission_denied);
            render();
          });

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup p, @Nullable Bundle b) {
    return i.inflate(R.layout.fragment_profile, p, false);
  }

  @Override
  public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
    super.onViewCreated(v, b);
    v.findViewById(R.id.change)
        .setOnClickListener(
            w -> {
              try {
                session.changeNick(
                    ((EditText) v.findViewById(R.id.nickname)).getText().toString().trim());
                Ui.toast(requireContext(), R.string.nickname_pending);
              } catch (IllegalArgumentException e) {
                ((EditText) v.findViewById(R.id.nickname)).setError(getString(R.string.error_nick));
              } catch (IllegalStateException e) {
                Ui.toast(requireContext(), R.string.error_offline);
              }
            });
    SwitchMaterial toggle = v.findViewById(R.id.notifications);
    toggle.setOnCheckedChangeListener(
        (button, on) -> {
          if (binding) return;
          if (on
              && Build.VERSION.SDK_INT >= 33
              && ContextCompat.checkSelfPermission(
                      requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                  != PackageManager.PERMISSION_GRANTED)
            permission.launch(Manifest.permission.POST_NOTIFICATIONS);
          else {
            boolean enabled =
                on && NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
            app.settings.notifications(enabled);
            if (on && !enabled) Ui.toast(requireContext(), R.string.permission_denied);
            render();
          }
        });
    v.findViewById(R.id.reconnect).setOnClickListener(w -> session.reconnect());
    v.findViewById(R.id.connection_options)
        .setOnClickListener(
            w -> {
              ConnectionConfig c = session.config;
              new MaterialAlertDialogBuilder(requireContext())
                  .setTitle(R.string.connection_options)
                  .setMessage(
                      getString(
                          R.string.settings_detail,
                          c.host,
                          c.port,
                          getString(c.tls ? R.string.yes : R.string.no),
                          c.username,
                          c.realName))
                  .setPositiveButton(R.string.close, null)
                  .show();
            });
    v.findViewById(R.id.disconnect)
        .setOnClickListener(
            w ->
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.disconnect_title)
                    .setMessage(R.string.disconnect_detail)
                    .setPositiveButton(
                        R.string.disconnect,
                        (d, which) -> {
                          session.disconnect();
                          main().openConnect();
                        })
                    .setNegativeButton(R.string.cancel, null)
                    .show());
  }

  @Override
  protected void render() {
    if (root == null) return;
    Ui.avatar(root.findViewById(R.id.avatar), session.data.nickname);
    ((TextView) root.findViewById(R.id.name)).setText(session.data.nickname);
    if (!renderedNick.equals(session.data.nickname)) {
      ((EditText) root.findViewById(R.id.nickname)).setText(session.data.nickname);
      renderedNick = session.data.nickname;
    }
    ((TextView) root.findViewById(R.id.server)).setText(session.config.host);
    ((TextView) root.findViewById(R.id.status)).setText(Ui.connection(requireContext(), session));
    ((TextView) root.findViewById(R.id.error))
        .setText(Ui.error(requireContext(), session.data.errorCode));
    Ui.visible(
        root.findViewById(R.id.reconnect), session.data.connection != ConnectionState.CONNECTED);
    binding = true;
    ((SwitchMaterial) root.findViewById(R.id.notifications))
        .setChecked(
            app.settings.notifications()
                && NotificationManagerCompat.from(requireContext()).areNotificationsEnabled());
    binding = false;
  }

  @Override
  public void onDestroyView() {
    renderedNick = "";
    super.onDestroyView();
  }
}
