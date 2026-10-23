package vn.edu.usth.connect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.fragment.app.Fragment;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import vn.edu.usth.connect.ui.chat.ChatFragment;
import vn.edu.usth.connect.ui.common.FeatureFragmentFactory;

@RunWith(AndroidJUnit4.class)
public class FragmentCompatibilityTest {
  @Test
  public void restoresFragmentClassNamesSavedByPreviousAppVersion() {
    FeatureFragmentFactory factory = new FeatureFragmentFactory();
    ClassLoader loader = getClass().getClassLoader();
    String[][] names = {
      {"CoursesFragment", "courses"},
      {"CourseChannelsFragment", "courses"},
      {"ServerChannelsFragment", "courses"},
      {"ChatFragment", "chat"},
      {"ConversationsFragment", "chat"},
      {"MembersBottomSheet", "members"},
      {"ProfileFragment", "profile"}
    };
    for (String[] name : names) {
      Fragment restored = factory.instantiate(loader, "vn.edu.usth.connect.ui." + name[0]);
      assertEquals(
          "vn.edu.usth.connect.ui." + name[1] + "." + name[0], restored.getClass().getName());
    }
    assertTrue(factory.instantiate(loader, ChatFragment.class.getName()) instanceof ChatFragment);
    assertEquals(Fragment.class, factory.instantiate(loader, Fragment.class.getName()).getClass());
  }
}
