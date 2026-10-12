package vn.edu.usth.connect.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import vn.edu.usth.connect.*;

public final class MainActivity extends AppCompatActivity {
  private BottomNavigationView navigation;
  private int tab = R.id.nav_courses;

  @Override
  protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    if (ConnectApplication.from(this).session.config == null) {
      openConnect();
      return;
    }
    setContentView(R.layout.activity_main);
    Insets.apply(this, findViewById(R.id.root));
    navigation = findViewById(R.id.navigation);
    if (saved != null) {
      tab = saved.getInt("tab", R.id.nav_courses);
      navigation.setSelectedItemId(tab);
    }
    navigation.setOnItemSelectedListener(
        item -> {
          tab = item.getItemId();
          select(tab);
          return true;
        });
    getSupportFragmentManager().addOnBackStackChangedListener(this::updateNavigation);
    getSupportFragmentManager()
        .registerFragmentLifecycleCallbacks(
            new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
              @Override
              public void onFragmentResumed(androidx.fragment.app.FragmentManager fm, Fragment f) {
                updateNavigation();
              }
            },
            false);
    if (saved == null) {
      select(tab);
      openTarget(getIntent());
    }
  }

  private void select(int id) {
    getSupportFragmentManager()
        .popBackStackImmediate(
            null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
    Fragment fragment =
        id == R.id.nav_messages
            ? new ConversationsFragment()
            : id == R.id.nav_profile ? new ProfileFragment()
                : id == R.id.nav_server ? new ServerChannelsFragment() : new CoursesFragment();
    getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  public void open(Fragment fragment) {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.container, fragment)
        .addToBackStack(null)
        .commit();
  }

  public void openChat(String target) {
    ConnectApplication.from(this).session.data.conversation(target);
    open(ChatFragment.create(target));
  }

  public void showServerChannels() { navigation.setSelectedItemId(R.id.nav_server); }

  public void showProfile() {
    navigation.setSelectedItemId(R.id.nav_profile);
  }

  public void back() {
    getOnBackPressedDispatcher().onBackPressed();
  }

  private void updateNavigation() {
    if (navigation != null)
      Ui.visible(
          navigation,
          !(getSupportFragmentManager().findFragmentById(R.id.container) instanceof ChatFragment));
  }

  public void openConnect() {
    startActivity(new Intent(this, ConnectActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    finish();
  }

  private void openTarget(Intent intent) {
    String target = intent.getStringExtra("target");
    if (target != null && !target.isEmpty()) openChat(target);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    openTarget(intent);
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    out.putInt("tab", tab);
    super.onSaveInstanceState(out);
  }
}
