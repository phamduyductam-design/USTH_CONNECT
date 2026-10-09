package vn.edu.usth.connect.ui;

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
import vn.edu.usth.connect.repository.SessionRepository;

public final class MembersBottomSheet extends BottomSheetDialogFragment
    implements SessionRepository.Listener {
  private View root;
  private SessionRepository session;
  private CatalogAdapter adapter;
  private String target;

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
    return i.inflate(R.layout.sheet_members, p, false);
  }

  @Override
  public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
    super.onViewCreated(v, b);
    root = v;
    session = ConnectApplication.from(requireContext()).session;
    target = requireArguments().getString("target");
    v.findViewById(R.id.close).setOnClickListener(w -> dismiss());
    adapter = new CatalogAdapter();
    RecyclerView list = v.findViewById(R.id.list);
    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    list.setAdapter(adapter);
    Ui.watch(v.findViewById(R.id.search), this::changed);
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
    session.addListener(this);
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
    users.sort(Comparator.comparing(u -> IrcParser.fold(u.nickname)));
    for (IrcUser user : users)
      if (user.nickname.toLowerCase(Locale.ROOT).contains(query)) {
        CatalogAdapter.Row row = new CatalogAdapter.Row();
        row.name = user.nickname;
        row.detail = user.prefixes;
        row.avatar = user.nickname;
        row.self = IrcParser.fold(user.nickname).equals(IrcParser.fold(session.data.nickname));
        if (!row.self)
          row.click =
              () -> {
                ((MainActivity) requireActivity()).openChat(user.nickname);
                dismiss();
              };
        rows.add(row);
      }
    if (rows.isEmpty()) rows.add(CatalogAdapter.Row.empty(getString(R.string.empty_members), ""));
    adapter.submit(rows);
  }

  @Override
  public void onDestroyView() {
    root = null;
    adapter = null;
    super.onDestroyView();
  }
}
