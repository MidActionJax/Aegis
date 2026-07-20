package com.example.projectaegis;

import android.os.Bundle;
import android.text.TextUtils;

import com.example.projectaegis.data.Credential;
import com.example.projectaegis.data.VaultRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class EntryFormActivity extends SecureActivity {

    private VaultRepository repository;
    private Long editingId;

    private TextInputLayout accountNameLayout;
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText accountNameInput;
    private TextInputEditText urlInput;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_form);

        repository = VaultRepository.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        accountNameLayout = findViewById(R.id.accountNameLayout);
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        accountNameInput = findViewById(R.id.accountNameInput);
        urlInput = findViewById(R.id.urlInput);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);

        long credentialId = getIntent().getLongExtra(VaultActivity.EXTRA_CREDENTIAL_ID, -1);
        if (credentialId != -1) {
            editingId = credentialId;
            toolbar.setTitle(R.string.title_edit_credential);
            prefillFrom(repository.getById(credentialId));
        } else {
            toolbar.setTitle(R.string.title_add_credential);
        }

        MaterialButton saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> handleSave());
    }

    private void prefillFrom(Credential credential) {
        if (credential == null) return;
        accountNameInput.setText(credential.getAccountName());
        urlInput.setText(credential.getUrl());
        usernameInput.setText(credential.getUsername());
        passwordInput.setText(credential.getPassword());
    }

    private void handleSave() {
        String accountName = textOf(accountNameInput);
        String url = textOf(urlInput);
        String username = textOf(usernameInput);
        String password = textOf(passwordInput);

        boolean valid = true;
        accountNameLayout.setError(null);
        usernameLayout.setError(null);
        passwordLayout.setError(null);

        if (TextUtils.isEmpty(accountName)) {
            accountNameLayout.setError(getString(R.string.error_required_field));
            valid = false;
        }
        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError(getString(R.string.error_required_field));
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError(getString(R.string.error_required_field));
            valid = false;
        }
        if (!valid) return;

        if (editingId != null) {
            repository.update(editingId, accountName, url, username, password);
        } else {
            repository.insert(accountName, url, username, password);
        }
        finish();
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
