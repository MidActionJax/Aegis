package com.example.projectaegis;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

// SECURE (v2): every screen in the app extends this instead of AppCompatActivity
// directly, so FLAG_SECURE is applied everywhere uniformly. Fixes the v1 flaw where
// revealed passwords were visible in the Recents app-switcher thumbnail and in
// screenshots/screen recordings.
public abstract class SecureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }
}
