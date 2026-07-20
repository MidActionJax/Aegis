package com.example.projectaegis;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.example.projectaegis.data.Credential;
import com.example.projectaegis.data.VaultRepository;
import com.example.projectaegis.util.ClipboardUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

public class VaultActivity extends SecureActivity implements CredentialAdapter.Listener {

    public static final String EXTRA_CREDENTIAL_ID = "credential_id";

    private final Handler clipboardHandler = new Handler(Looper.getMainLooper());

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clipboardHandler.removeCallbacksAndMessages(null);
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
        ClipboardUtil.copyWithAutoWipe(this, clipboardHandler, "password", credential.getPassword());
        Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show();
    }
}
