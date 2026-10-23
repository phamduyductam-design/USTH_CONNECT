package vn.edu.usth.connect.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import vn.edu.usth.connect.*;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.ui.chat.ChatFragment;
import vn.edu.usth.connect.ui.chat.ConversationsFragment;
import vn.edu.usth.connect.ui.common.UiHelpers;
import vn.edu.usth.connect.ui.common.WindowInsetsHelper;
import vn.edu.usth.connect.ui.courses.CoursesFragment;
import vn.edu.usth.connect.ui.courses.ServerChannelsFragment;
import vn.edu.usth.connect.ui.profile.ProfileFragment;

public final class MainActivity extends AppCompatActivity {
  private BottomNavigationView navigation;
  private int tab = R.id.nav_courses;
  private boolean keyboardVisible;

  @Override
  protected void onCreate(Bundle saved) {
    getSupportFragmentManager()
        .setFragmentFactory(new vn.edu.usth.connect.ui.common.FeatureFragmentFactory());
    super.onCreate(saved);
    if (ConnectApplication.from(this).session.config == null) {
      openConnect();
      return;
    }
    setContentView(R.layout.activity_main);
    navigation = findViewById(R.id.navigation);
    WindowInsetsHelper.apply(
        this,
        findViewById(R.id.root),
        visible -> {
          keyboardVisible = visible;
          updateNavigation();
        });
    if (saved != null) {
      tab = saved.getInt("tab", R.id.nav_courses);
      if (navigation.getMenu().findItem(tab) == null) tab = R.id.nav_courses;
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
            : id == R.id.nav_profile ? new ProfileFragment() : new CoursesFragment();
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

  public void showServerChannels() {
    if (!(getSupportFragmentManager().findFragmentById(R.id.container)
        instanceof ServerChannelsFragment)) open(new ServerChannelsFragment());
  }

  public void showProfile() {
    navigation.setSelectedItemId(R.id.nav_profile);
  }

  public void back() {
    getOnBackPressedDispatcher().onBackPressed();
  }

  private void updateNavigation() {
    if (navigation == null) return;
    Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
    if (current instanceof ProfileFragment) tab = R.id.nav_profile;
    else if (current instanceof ConversationsFragment) tab = R.id.nav_messages;
    else if (current instanceof ChatFragment) {
      String target = current.requireArguments().getString("target", "");
      tab = IrcParser.isChannel(target) ? R.id.nav_courses : R.id.nav_messages;
    } else if (current != null) tab = R.id.nav_courses;
    navigation.getMenu().findItem(tab).setChecked(true);
    // Keep the three-tab context while browsing; give typing the full remaining height.
    UiHelpers.visible(navigation, !keyboardVisible);
    UiHelpers.visible(findViewById(R.id.navigation_divider), !keyboardVisible);
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
