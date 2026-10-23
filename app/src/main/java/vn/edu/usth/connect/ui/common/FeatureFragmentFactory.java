package vn.edu.usth.connect.ui.common;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import vn.edu.usth.connect.ui.chat.ChatFragment;
import vn.edu.usth.connect.ui.chat.ConversationsFragment;
import vn.edu.usth.connect.ui.courses.CourseChannelsFragment;
import vn.edu.usth.connect.ui.courses.CoursesFragment;
import vn.edu.usth.connect.ui.courses.ServerChannelsFragment;
import vn.edu.usth.connect.ui.members.MembersBottomSheet;
import vn.edu.usth.connect.ui.profile.ProfileFragment;

/** Restores task/back-stack state saved before UI classes were grouped by feature. */
public final class FeatureFragmentFactory extends FragmentFactory {
  @NonNull
  @Override
  public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
    switch (className) {
      case "vn.edu.usth.connect.ui.ChatFragment":
        className = ChatFragment.class.getName();
        break;
      case "vn.edu.usth.connect.ui.ConversationsFragment":
        className = ConversationsFragment.class.getName();
        break;
      case "vn.edu.usth.connect.ui.CoursesFragment":
        className = CoursesFragment.class.getName();
        break;
      case "vn.edu.usth.connect.ui.CourseChannelsFragment":
        className = CourseChannelsFragment.class.getName();
        break;
      case "vn.edu.usth.connect.ui.ServerChannelsFragment":
        className = ServerChannelsFragment.class.getName();
        break;
      case "vn.edu.usth.connect.ui.MembersBottomSheet":
        className = MembersBottomSheet.class.getName();
        break;
      case "vn.edu.usth.connect.ui.ProfileFragment":
        className = ProfileFragment.class.getName();
        break;
      default:
        break;
    }
    return super.instantiate(classLoader, className);
  }
}
