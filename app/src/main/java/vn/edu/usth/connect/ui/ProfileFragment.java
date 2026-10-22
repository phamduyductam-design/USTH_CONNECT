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
import vn.edu.usth.connect.ui.dialog.CommonDialogs;
import vn.edu.usth.connect.ui.dialog.UserDialogs;

public final class ProfileFragment extends SessionAwareFragment {
  private boolean binding;
  private String renderedNick = "";
  private final ActivityResultLauncher<String> permission =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          granted -> {
            app.settings.notifications(granted);
            if (!granted) UiHelpers.toast(requireContext(), R.string.permission_denied);
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
    if (b != null)
      UiHelpers.visible(
          v.findViewById(R.id.profile_nickname_panel), b.getBoolean("nickname_expanded"));
    v.findViewById(R.id.profile_nickname_toggle)
        .setOnClickListener(
            w -> {
              View panel = v.findViewById(R.id.profile_nickname_panel);
              UiHelpers.visible(panel, panel.getVisibility() != View.VISIBLE);
            });
    v.findViewById(R.id.profile_theme_settings)
        .setOnClickListener(
            w -> {
              boolean dark = !app.settings.darkMode();
              app.settings.darkMode(dark);
              ThemeMode.apply(requireContext(), dark);
            });
    v.findViewById(R.id.change)
        .setOnClickListener(
            w -> {
              try {
                session.changeNick(
                    ((EditText) v.findViewById(R.id.nickname)).getText().toString().trim());
                UiHelpers.toast(requireContext(), R.string.nickname_pending);
              } catch (IllegalArgumentException e) {
                ((EditText) v.findViewById(R.id.nickname)).setError(getString(R.string.error_nick));
              } catch (IllegalStateException e) {
                UiHelpers.toast(requireContext(), R.string.error_offline);
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
            if (on && !enabled) UiHelpers.toast(requireContext(), R.string.permission_denied);
            render();
          }
        });
    v.findViewById(R.id.away_toggle)
        .setOnClickListener(
            w -> {
              if (session.data.away) session.setAway("");
              else UserDialogs.showAway(main());
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
                CommonDialogs.confirm(
                    main(),
                    R.string.disconnect_title,
                    R.string.disconnect_detail,
                    R.string.disconnect,
                    () -> {
                      session.disconnect();
                      main().openConnect();
                    }));
  }

  @Override
  protected void render() {
    if (root == null) return;
    UiHelpers.avatar(root.findViewById(R.id.avatar), session.data.nickname);
    ((TextView) root.findViewById(R.id.name)).setText(UiHelpers.displayName(session.data.nickname));
    ((TextView) root.findViewById(R.id.profile_nick))
        .setText(getString(R.string.cp_profile_nickname, session.data.nickname));
    ((TextView) root.findViewById(R.id.profile_nickname_value)).setText(session.data.nickname);
    ((TextView) root.findViewById(R.id.profile_theme_value))
        .setText(app.settings.darkMode() ? R.string.cp_theme_dark : R.string.cp_theme_light);
    if (!renderedNick.equals(session.data.nickname)) {
      ((EditText) root.findViewById(R.id.nickname)).setText(session.data.nickname);
      renderedNick = session.data.nickname;
    }
    ((TextView) root.findViewById(R.id.server))
        .setText(getString(R.string.cp_server_address, session.config.host, session.config.port));
    int joined = 0;
    for (Channel channel : session.data.channels.values()) if (channel.joined) joined++;
    ((TextView) root.findViewById(R.id.server_details))
        .setText(
            getString(
                R.string.cp_server_details,
                joined,
                getString(
                    session.config.tls ? R.string.cp_tls_enabled : R.string.cp_tls_disabled)));
    TextView status = root.findViewById(R.id.status);
    boolean online = session.data.connection == ConnectionState.CONNECTED;
    UiHelpers.visible(root.findViewById(R.id.profile_presence_dot), online);
    String connection = UiHelpers.connection(requireContext(), session);
    status.setText(online ? getString(R.string.connected_indicator, connection) : connection);
    status.setTextColor(
        androidx.core.content.ContextCompat.getColor(
            requireContext(), online ? R.color.success_text : R.color.error));
    ((TextView) root.findViewById(R.id.error))
        .setText(UiHelpers.error(requireContext(), session.data.errorCode));
    UiHelpers.visible(root.findViewById(R.id.error), !session.data.errorCode.isEmpty());
    UiHelpers.visible(
        root.findViewById(R.id.reconnect), session.data.connection != ConnectionState.CONNECTED);
    root.findViewById(R.id.away_toggle).setEnabled(online);
    ((TextView) root.findViewById(R.id.away_toggle))
        .setText(session.data.away ? R.string.away_clear : R.string.away_set);
    ((TextView) root.findViewById(R.id.away_status))
        .setText(
            !online
                ? connection
                : session.data.away
                    ? getString(R.string.away_detail, session.data.awayMessage)
                    : getString(R.string.online_presence));
    binding = true;
    ((SwitchMaterial) root.findViewById(R.id.notifications))
        .setChecked(
            app.settings.notifications()
                && NotificationManagerCompat.from(requireContext()).areNotificationsEnabled());
    binding = false;
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle out) {
    super.onSaveInstanceState(out);
    out.putBoolean(
        "nickname_expanded",
        root != null
            && root.findViewById(R.id.profile_nickname_panel).getVisibility() == View.VISIBLE);
  }

  @Override
  public void onDestroyView() {
    renderedNick = "";
    super.onDestroyView();
  }
}
