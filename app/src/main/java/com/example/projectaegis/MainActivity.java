package com.example.projectaegis;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.projectaegis.auth.PinManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private PinManager pinManager;

    private LinearLayout setupGroup;
    private TextInputEditText setupPinInput;
    private TextInputEditText setupPinConfirmInput;
    private TextView setupError;
    private Button setupButton;

    private LinearLayout unlockGroup;
    private TextInputEditText unlockPinInput;
    private TextView unlockError;
    private Button unlockButton;
    private Button biometricButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main);
        int edgePadding = (int) (32 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(edgePadding, systemBars.top + edgePadding, edgePadding, systemBars.bottom + edgePadding);
            return insets;
        });

        pinManager = new PinManager(this);

        setupGroup = findViewById(R.id.setupGroup);
        setupPinInput = findViewById(R.id.setupPinInput);
        setupPinConfirmInput = findViewById(R.id.setupPinConfirmInput);
        setupError = findViewById(R.id.setupError);
        setupButton = findViewById(R.id.setupButton);

        unlockGroup = findViewById(R.id.unlockGroup);
        unlockPinInput = findViewById(R.id.unlockPinInput);
        unlockError = findViewById(R.id.unlockError);
        unlockButton = findViewById(R.id.unlockButton);
        biometricButton = findViewById(R.id.biometricButton);

        setupButton.setOnClickListener(v -> handleSetupPin());
        unlockButton.setOnClickListener(v -> handleUnlock());

        setUpBiometricButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean pinSet = pinManager.isPinSet();
        setupGroup.setVisibility(pinSet ? View.GONE : View.VISIBLE);
        unlockGroup.setVisibility(pinSet ? View.VISIBLE : View.GONE);
    }

    private void handleSetupPin() {
        String pin = textOf(setupPinInput);
        String confirm = textOf(setupPinConfirmInput);

        if (pin.length() < 4 || pin.length() > 6) {
            showError(setupError, getString(R.string.error_pin_length));
            return;
        }
        if (!pin.equals(confirm)) {
            showError(setupError, getString(R.string.error_pin_mismatch));
            return;
        }

        setupError.setVisibility(View.GONE);
        pinManager.setPin(pin);
        goToVault();
    }

    private void handleUnlock() {
        String pin = textOf(unlockPinInput);
        // INSECURE (v1, planted flaw): logs the raw PIN attempt to Logcat.
        // Fix in v2: remove this log line entirely.
        Log.d(TAG, "Attempting vault unlock with PIN: " + pin);

        if (pinManager.checkPin(pin)) {
            unlockError.setVisibility(View.GONE);
            goToVault();
        } else {
            showError(unlockError, getString(R.string.error_pin_incorrect));
        }
    }

    private void setUpBiometricButton() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            biometricButton.setVisibility(View.GONE);
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        goToVault();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(getString(R.string.action_cancel))
                .build();

        biometricButton.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));
    }

    private void goToVault() {
        startActivity(new Intent(this, VaultActivity.class));
        finish();
    }

    private void showError(TextView errorView, String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
