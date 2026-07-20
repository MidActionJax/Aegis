package com.example.projectaegis;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projectaegis.auth.PinManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ChangePinActivity extends AppCompatActivity {

    private PinManager pinManager;

    private TextInputLayout currentPinLayout;
    private TextInputLayout newPinLayout;
    private TextInputLayout confirmNewPinLayout;
    private TextInputEditText currentPinInput;
    private TextInputEditText newPinInput;
    private TextInputEditText confirmNewPinInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pin);

        pinManager = new PinManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        currentPinLayout = findViewById(R.id.currentPinLayout);
        newPinLayout = findViewById(R.id.newPinLayout);
        confirmNewPinLayout = findViewById(R.id.confirmNewPinLayout);
        currentPinInput = findViewById(R.id.currentPinInput);
        newPinInput = findViewById(R.id.newPinInput);
        confirmNewPinInput = findViewById(R.id.confirmNewPinInput);

        MaterialButton saveButton = findViewById(R.id.savePinButton);
        saveButton.setOnClickListener(v -> handleSave());
    }

    private void handleSave() {
        String currentPin = textOf(currentPinInput);
        String newPin = textOf(newPinInput);
        String confirmPin = textOf(confirmNewPinInput);

        currentPinLayout.setError(null);
        newPinLayout.setError(null);
        confirmNewPinLayout.setError(null);

        if (!pinManager.checkPin(currentPin)) {
            currentPinLayout.setError(getString(R.string.error_current_pin_incorrect));
            return;
        }
        if (newPin.length() < 4 || newPin.length() > 6) {
            newPinLayout.setError(getString(R.string.error_pin_length));
            return;
        }
        if (!newPin.equals(confirmPin)) {
            confirmNewPinLayout.setError(getString(R.string.error_pin_mismatch));
            return;
        }

        pinManager.setPin(newPin);
        Toast.makeText(this, R.string.toast_pin_changed, Toast.LENGTH_SHORT).show();
        finish();
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
