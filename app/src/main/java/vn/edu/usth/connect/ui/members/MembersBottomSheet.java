package vn.edu.usth.connect.ui.members;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.recyclerview.widget.*;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.*;
import vn.edu.usth.connect.*;
import vn.edu.usth.connect.adapter.CatalogAdapter;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.model.*;
import vn.edu.usth.connect.repository.IrcSessionController;
import vn.edu.usth.connect.ui.MainActivity;
import vn.edu.usth.connect.ui.common.UiHelpers;
import vn.edu.usth.connect.ui.dialog.UserDialogs;

public final class MembersBottomSheet extends BottomSheetDialogFragment
    implements IrcSessionController.Listener {
  private View root;
  private IrcSessionController session;
  private CatalogAdapter adapter;
  private String target;
  private String selectedNick = "";

  public static MembersBottomSheet create(String target) {
    MembersBottomSheet f = new MembersBottomSheet();
    Bundle b = new Bundle();
    b.putString("target", target);
    f.setArguments(b);
    return f;
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup p, @Nullable Bundle b) {
    return i.inflate(R.layout.dialog_members, p, false);
  }

  @Override
  public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
    super.onViewCreated(v, b);
    root = v;
    session = ConnectApplication.from(requireContext()).session;
    target = requireArguments().getString("target");
    if (b != null) selectedNick = b.getString("selected", "");
    v.findViewById(R.id.close).setOnClickListener(w -> dismiss());
    v.findViewById(R.id.refresh_members).setOnClickListener(w -> refresh());
    v.findViewById(R.id.manage_member)
        .setOnClickListener(
            w ->
                UserDialogs.showMemberActions(
                    (MainActivity) requireActivity(), target, selectedNick));
    adapter = new CatalogAdapter();
    RecyclerView list = v.findViewById(R.id.list);
    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    list.setAdapter(adapter);
    UiHelpers.watch(v.findViewById(R.id.search), this::changed);
    v.findViewById(R.id.message_member).setOnClickListener(w -> openSelected());
  }

  @Override
  public void onStart() {
    super.onStart();
    ViewGroup.LayoutParams params = root.getLayoutParams();
    params.height =
        Math.min(
            getResources().getDimensionPixelSize(R.dimen.sheet_height),
            (int) (getResources().getDisplayMetrics().heightPixels * 0.8f));
    root.setLayoutParams(params);
    View sheet = requireDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
    if (sheet != null)
      com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
          .setShouldRemoveExpandedCorners(false);
    session.addListener(this);
    refresh();
  }

  private void refresh() {
    if (session.canSend(target)) session.refreshNames(target);
  }

  @Override
  public void onStop() {
    session.removeListener(this);
    super.onStop();
  }

  @Override
  public void changed() {
    if (root == null) return;
    Channel c = session.data.channel(target);
    if (!selectedNick.isEmpty() && !c.members.containsKey(IrcParser.fold(selectedNick)))
      selectedNick = "";
    ((TextView) root.findViewById(R.id.subtitle))
        .setText(
            getString(
                R.string.error_caption,
                target,
                getString(R.string.member_count, c.members.size())));
    String query =
        ((EditText) root.findViewById(R.id.search)).getText().toString().toLowerCase(Locale.ROOT);
    List<CatalogAdapter.Row> rows = new ArrayList<>();
    List<IrcUser> users = new ArrayList<>(c.members.values());
    users.sort(
        Comparator.comparing(
                (IrcUser u) ->
                    !IrcParser.fold(u.nickname).equals(IrcParser.fold(session.data.nickname)))
            .thenComparing(u -> IrcParser.fold(u.nickname)));
    for (IrcUser user : users)
      if (user.nickname.toLowerCase(Locale.ROOT).contains(query)) {
        CatalogAdapter.Row row = new CatalogAdapter.Row();
        row.name = UiHelpers.displayName(user.nickname);
        row.detail =
            user.nickname
                + " · "
                + UiHelpers.role(requireContext(), session.data.role(target, user.nickname));
        if (session.data.awayUsers.containsKey(IrcParser.fold(user.nickname)))
          row.preview =
              getString(R.string.away_presence)
                  + (session.data.awayUsers.get(IrcParser.fold(user.nickname)).isEmpty()
                      ? ""
                      : ": " + session.data.awayUsers.get(IrcParser.fold(user.nickname)));
        row.avatar = user.nickname;
        row.self = IrcParser.fold(user.nickname).equals(IrcParser.fold(session.data.nickname));
        row.selected = IrcParser.fold(user.nickname).equals(IrcParser.fold(selectedNick));
        if (!row.self) {
          row.click =
              () -> {
                selectedNick = user.nickname;
                changed();
              };
          row.joinVisible = true;
          row.actionText = R.string.private_message;
          row.join =
              () -> {
                selectedNick = user.nickname;
                openSelected();
              };
        }
        rows.add(row);
      }
    if (rows.isEmpty()) rows.add(CatalogAdapter.Row.empty(getString(R.string.empty_members), ""));
    adapter.submit(rows);
    root.findViewById(R.id.refresh_members)
        .setEnabled(session.canSend(target) && !c.namesInProgress);
    UiHelpers.visible(
        root.findViewById(R.id.manage_member),
        !selectedNick.isEmpty() && session.canSend(target) && session.data.canManage(target));
    android.widget.TextView action = root.findViewById(R.id.message_member);
    action.setEnabled(!selectedNick.isEmpty());
    UiHelpers.visible(action, !selectedNick.isEmpty());
    action.setText(
        selectedNick.isEmpty()
            ? getString(R.string.private_message)
            : getString(R.string.message_member, UiHelpers.displayName(selectedNick)));
  }

  private void openSelected() {
    if (selectedNick.isEmpty()) return;
    ((MainActivity) requireActivity()).openChat(selectedNick);
    dismiss();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle out) {
    super.onSaveInstanceState(out);
    out.putString("selected", selectedNick);
  }

  @Override
  public void onDestroyView() {
    root = null;
    adapter = null;
    super.onDestroyView();
  }
}
