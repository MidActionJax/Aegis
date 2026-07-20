package com.example.projectaegis;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.projectaegis.data.Credential;
import com.example.projectaegis.data.VaultRepository;
import com.google.android.material.appbar.MaterialToolbar;

public class CredentialDetailActivity extends AppCompatActivity {

    private static final String TAG = "CredentialDetail";

    private VaultRepository repository;
    private Credential credential;
    private boolean passwordRevealed = false;

    private TextView detailPassword;
    private ImageButton detailToggleVisibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credential_detail);

        repository = VaultRepository.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_credential_detail);
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        detailPassword = findViewById(R.id.detailPassword);
        detailToggleVisibility = findViewById(R.id.detailToggleVisibility);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCredential();
    }

    private void loadCredential() {
        long id = getIntent().getLongExtra(VaultActivity.EXTRA_CREDENTIAL_ID, -1);
        credential = repository.getById(id);
        if (credential == null) {
            finish();
            return;
        }

        TextView detailAccountName = findViewById(R.id.detailAccountName);
        TextView detailUrl = findViewById(R.id.detailUrl);
        TextView detailUsername = findViewById(R.id.detailUsername);
        ImageButton detailCopyButton = findViewById(R.id.detailCopyButton);

        detailAccountName.setText(credential.getAccountName());
        detailUrl.setText(credential.getUrl());
        detailUsername.setText(credential.getUsername());
        renderPassword();

        detailToggleVisibility.setOnClickListener(v -> {
            passwordRevealed = !passwordRevealed;
            renderPassword();
        });
        detailCopyButton.setOnClickListener(v -> copyPassword());
    }

    private void renderPassword() {
        String password = credential.getPassword();
        detailPassword.setText(passwordRevealed ? password : maskOf(password));
        detailToggleVisibility.setImageResource(
                passwordRevealed ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
    }

    private String maskOf(String password) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Math.max(password.length(), 8); i++) {
            builder.append('•');
        }
        return builder.toString();
    }

    private void copyPassword() {
        // INSECURE (v1, planted flaw): same unbounded clipboard copy + sensitive
        // logging as the vault dashboard. Fix in v2 alongside VaultActivity.onCopyPassword.
        Log.d(TAG, "Copied password for " + credential.getAccountName() + ": " + credential.getPassword());

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("password", credential.getPassword());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show();
    }

    private boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit) {
            Intent intent = new Intent(this, EntryFormActivity.class);
            intent.putExtra(VaultActivity.EXTRA_CREDENTIAL_ID, credential.getId());
            startActivity(intent);
            return true;
        } else if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return false;
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    repository.delete(credential.getId());
                    finish();
                })
                .show();
    }
}
