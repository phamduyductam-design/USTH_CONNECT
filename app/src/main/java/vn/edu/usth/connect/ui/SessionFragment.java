package vn.edu.usth.connect.ui;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import vn.edu.usth.connect.ConnectApplication;
import vn.edu.usth.connect.repository.SessionRepository;

public abstract class SessionFragment extends Fragment implements SessionRepository.Listener {
  protected View root;
  protected ConnectApplication app;
  protected SessionRepository session;
  private String lastError = "";

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    app = ConnectApplication.from(requireContext());
    session = app.session;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
    super.onViewCreated(view, saved);
    root = view;
    Ui.themeToggle(requireActivity(), view);
  }

  @Override
  public void onStart() {
    super.onStart();
    session.addListener(this);
  }

  @Override
  public void onStop() {
    session.removeListener(this);
    super.onStop();
  }

  @Override
  public final void changed() {
    if (root == null) return;
    render();
    String error = session.data.errorCode + session.data.errorTarget;
    if (!session.data.errorCode.isEmpty() && !error.equals(lastError)) {
      android.widget.Toast.makeText(
              requireContext(),
              Ui.error(requireContext(), session.data.errorCode)
                  + (session.data.errorDetail.isEmpty() ? "" : "\n" + session.data.errorDetail),
              android.widget.Toast.LENGTH_LONG)
          .show();
    }
    lastError = error;
  }

  protected abstract void render();

  protected MainActivity main() {
    return (MainActivity) requireActivity();
  }

  @Override
  public void onDestroyView() {
    root = null;
    super.onDestroyView();
  }
}
