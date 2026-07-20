package com.example.projectaegis;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectaegis.data.Credential;
import com.example.projectaegis.data.VaultRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

// NOTE (v1, planted flaw): this Activity never sets FLAG_SECURE, so revealed
// passwords are visible in the Recents app-switcher thumbnail and in screenshots.
// Fix in v2: getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, ...) in onCreate.
public class VaultActivity extends AppCompatActivity implements CredentialAdapter.Listener {

    private static final String TAG = "VaultActivity";

    public static final String EXTRA_CREDENTIAL_ID = "credential_id";

    private VaultRepository repository;
    private CredentialAdapter adapter;
    private LinearLayout emptyState;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        repository = VaultRepository.getInstance(this);

        toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_vault);
        toolbar.setOnMenuItemClickListener(this::onToolbarMenuItemClick);

        RecyclerView recyclerView = findViewById(R.id.credentialList);
        adapter = new CredentialAdapter(this);
        recyclerView.setAdapter(adapter);

        emptyState = findViewById(R.id.emptyState);

        ExtendedFloatingActionButton addFab = findViewById(R.id.addFab);
        addFab.setOnClickListener(v -> startActivity(new Intent(this, EntryFormActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        List<Credential> credentials = repository.getAll();
        adapter.submitList(credentials);
        emptyState.setVisibility(credentials.isEmpty() ? View.VISIBLE : View.GONE);
        toolbar.setSubtitle(credentials.isEmpty() ? null :
                getResources().getQuantityString(R.plurals.vault_subtitle_count, credentials.size(), credentials.size()));
    }

    private boolean onToolbarMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_lock) {
            finish();
            return true;
        } else if (id == R.id.action_change_pin) {
            startActivity(new Intent(this, ChangePinActivity.class));
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(Credential credential) {
        Intent intent = new Intent(this, CredentialDetailActivity.class);
        intent.putExtra(EXTRA_CREDENTIAL_ID, credential.getId());
        startActivity(intent);
    }

    @Override
    public void onCopyPassword(Credential credential) {
        // INSECURE (v1, planted flaw): copies the password with no expiry/auto-clear,
        // and logs the plaintext value. Fix in v2: schedule a 30s clipboard wipe and
        // drop the log line.
        Log.d(TAG, "Copied password for " + credential.getAccountName() + ": " + credential.getPassword());

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("password", credential.getPassword());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show();
    }
}
