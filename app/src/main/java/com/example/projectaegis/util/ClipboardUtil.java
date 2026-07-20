package com.example.projectaegis.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;

// SECURE (v2): copied passwords are wiped from the clipboard after CLEAR_DELAY_MS,
// fixing the v1 flaw where a copied password sat in the clipboard indefinitely and
// could be scraped by any other app. Only clears the clipboard if it still holds the
// exact value we copied, so we don't clobber something else the user copied meanwhile.
public final class ClipboardUtil {

    public static final long CLEAR_DELAY_MS = 30_000;

    private ClipboardUtil() {
    }

    public static void copyWithAutoWipe(Context context, Handler handler, String label, String value) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value));

        handler.postDelayed(() -> {
            ClipData current = clipboard.getPrimaryClip();
            if (current != null && current.getItemCount() > 0
                    && value.contentEquals(current.getItemAt(0).getText())) {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""));
            }
        }, CLEAR_DELAY_MS);
    }
}
