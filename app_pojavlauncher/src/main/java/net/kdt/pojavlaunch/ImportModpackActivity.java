package net.kdt.pojavlaunch;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.modloaders.modpacks.api.LocalModpackInstaller;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import git.artdeell.mojo.R;

/**
 * activity that handles opening .mrpack files from file manager as such.
 * Shows a confirmation dialog, then installs the modpack as a new instance.
 */
public class ImportModpackActivity extends BaseActivity {

    private Uri mUriData;

    @Override
    protected boolean shouldIgnoreNotch() {
        return false;
    }

    @Override
    public boolean setFullscreen() {
        return false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Tools.checkStorageRoot(this)) {
            startActivity(new Intent(this, MissingStorageActivity.class));
            finish();
            return;
        }
        LauncherPreferences.loadPreferences(getApplicationContext());
        Tools.initStorageConstants(getApplicationContext());

        mUriData = getUriData();
        if (mUriData == null) {
            Toast.makeText(this, R.string.import_modpack_no_file, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_import_modpack);

        String fileName = Tools.getFileName(this, mUriData);
        if (fileName == null) fileName = "unknown";

        TextView fileNameView = findViewById(R.id.import_modpack_file_name);
        fileNameView.setText(getString(R.string.import_modpack_confirm, fileName));

        findViewById(R.id.import_modpack_install_button).setOnClickListener(v -> startInstall());
        findViewById(R.id.import_modpack_cancel_button).setOnClickListener(v -> {
            finishAndRemoveTask();
        });
    }

    private void startInstall() {
        // Disable buttons to prevent double-tap
        findViewById(R.id.import_modpack_install_button).setEnabled(false);
        findViewById(R.id.import_modpack_cancel_button).setEnabled(false);

        final Uri uri = mUriData;
        final ContentResolver contentResolver = getContentResolver();
        final android.content.Context appContext = getApplicationContext();

        PojavApplication.sExecutorService.execute(() -> {
            boolean success = LocalModpackInstaller.installFromUri(uri, appContext, contentResolver);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(appContext, R.string.import_modpack_success, Toast.LENGTH_SHORT).show();
                    // Navigate to the launcher so the user sees their new instance
                    Intent launcherIntent = new Intent(appContext, LauncherActivity.class);
                    launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(launcherIntent);
                } else {
                    Toast.makeText(appContext, R.string.import_modpack_failed, Toast.LENGTH_SHORT).show();
                }
                finishAndRemoveTask();
            });
        });
    }


    private Uri getUriData() {
        Intent intent = getIntent();
        if (intent == null) return null;

        // VIEW action: data is in getData()
        Uri data = intent.getData();
        if (data != null) return data;

        // SEND action: data may be in extras
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            return intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }

        // Try clipData as fallback
        try {
            return intent.getClipData().getItemAt(0).getUri();
        } catch (Exception ignored) {}

        return null;
    }
}
