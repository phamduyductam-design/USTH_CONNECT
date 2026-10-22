package vn.edu.usth.connect.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;
import vn.edu.usth.connect.*;
import vn.edu.usth.connect.irc.IrcCommands;
import vn.edu.usth.connect.model.*;
import vn.edu.usth.connect.repository.SessionRepository;

public final class ConnectActivity extends AppCompatActivity implements SessionRepository.Listener {
  private SessionRepository session;
  private EditText nick, host, port, username, realName;
  private SwitchMaterial tls;
  private boolean navigating;

  @Override
  protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    setContentView(R.layout.activity_connect);
    WindowInsetsHelper.apply(this, findViewById(R.id.root));
    UiHelpers.themeToggle(this, findViewById(R.id.root));
    ConnectApplication app = ConnectApplication.from(this);
    session = app.session;
    nick = findViewById(R.id.nickname);
    host = findViewById(R.id.host);
    port = findViewById(R.id.port);
    username = findViewById(R.id.username);
    realName = findViewById(R.id.real_name);
    tls = findViewById(R.id.tls);
    if (saved == null) {
      nick.setText(app.settings.nick());
      host.setText(app.settings.host());
      port.setText(String.valueOf(app.settings.port()));
      tls.setChecked(app.settings.tls());
      username.setText(app.settings.username());
      realName.setText(app.settings.realName());
    }
    if (saved != null && saved.getBoolean("advanced"))
      findViewById(R.id.advanced_panel).setVisibility(View.VISIBLE);
    tls.setOnCheckedChangeListener(
        (button, on) -> {
          if (port.getText().toString().equals(on ? "6667" : "6697"))
            port.setText(on ? "6697" : "6667");
          renderServerSummary();
        });
    UiHelpers.watch(port, this::renderServerSummary);
    renderServerSummary();
    findViewById(R.id.advanced_toggle)
        .setOnClickListener(
            v -> {
              View panel = findViewById(R.id.advanced_panel);
              UiHelpers.visible(panel, panel.getVisibility() != View.VISIBLE);
            });
    findViewById(R.id.connect).setOnClickListener(v -> connect());
    findViewById(R.id.cancel).setOnClickListener(v -> session.disconnect());
  }

  private void renderServerSummary() {
    ((TextView) findViewById(R.id.server_summary))
        .setText(
            getString(
                R.string.cp_server_summary,
                port.getText().toString(),
                getString(tls.isChecked() ? R.string.cp_tls_enabled : R.string.cp_tls_disabled)));
  }

  private void connect() {
    try {
      IrcCommands.nick(nick.getText().toString().trim());
    } catch (IllegalArgumentException e) {
      nick.setError(getString(R.string.error_nick));
      return;
    }
    try {
      ConnectionConfig config =
          new ConnectionConfig(
              host.getText().toString().trim(),
              Integer.parseInt(port.getText().toString().trim()),
              tls.isChecked(),
              nick.getText().toString().trim(),
              username.getText().toString().trim(),
              realName.getText().toString().trim());
      session.connect(config);
    } catch (IllegalArgumentException e) {
      UiHelpers.visible(findViewById(R.id.connect_status_panel), true);
      findViewById(R.id.connect_status_panel).setBackgroundResource(R.drawable.bg_error);
      ((TextView) findViewById(R.id.status)).setText(R.string.error_config);
      ((TextView) findViewById(R.id.status))
          .setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.error));
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    session.addListener(this);
  }

  @Override
  protected void onStop() {
    session.removeListener(this);
    super.onStop();
  }

  @Override
  public void changed() {
    if (session.data.connection == ConnectionState.CONNECTED && !navigating) {
      navigating = true;
      startActivity(new Intent(this, MainActivity.class));
      finish();
      return;
    }
    boolean busy =
        session.data.connection == ConnectionState.CONNECTING
            || session.data.connection == ConnectionState.REGISTERING
            || session.data.connection == ConnectionState.RECONNECTING;
    for (View input :
        new View[] {nick, host, port, username, realName, tls, findViewById(R.id.advanced_toggle)})
      input.setEnabled(!busy);
    UiHelpers.visible(findViewById(R.id.progress), busy);
    UiHelpers.visible(
        findViewById(R.id.connect_status_panel),
        busy || session.data.connection == ConnectionState.FAILED);
    findViewById(R.id.connect_status_panel)
        .setBackgroundResource(
            session.data.connection == ConnectionState.FAILED
                ? R.drawable.bg_error
                : R.drawable.bg_selected);
    UiHelpers.visible(findViewById(R.id.cancel), busy);
    findViewById(R.id.connect).setEnabled(!busy);
    ((TextView) findViewById(R.id.connect))
        .setText(
            session.data.connection == ConnectionState.FAILED ? R.string.retry : R.string.connect);
    String status =
        session.data.connection == ConnectionState.DISCONNECTED
            ? ""
            : getString(busy ? R.string.connect_pending_title : R.string.connect_failed_title)
                + "\n"
                + UiHelpers.connection(this, session);
    ((TextView) findViewById(R.id.status)).setText(status);
    ((TextView) findViewById(R.id.status))
        .setTextColor(
            androidx.core.content.ContextCompat.getColor(
                this,
                session.data.connection == ConnectionState.FAILED
                    ? R.color.error
                    : R.color.primary));
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    out.putBoolean("advanced", findViewById(R.id.advanced_panel).getVisibility() == View.VISIBLE);
    super.onSaveInstanceState(out);
  }
}
