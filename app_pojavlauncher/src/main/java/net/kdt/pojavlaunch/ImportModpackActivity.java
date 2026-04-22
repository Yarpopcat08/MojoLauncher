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
 * Activity that handles opening .mrpack files from an external file manager.
 * Shows a confirmation dialog, then starts the install in the background
 * and immediately navigates to the launcher.
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

        // Validate that this is actually a .mrpack file
        String fileName = Tools.getFileName(this, mUriData);
        if (fileName == null || !fileName.toLowerCase().endsWith(".mrpack")) {
            Toast.makeText(this, R.string.import_modpack_unsupported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_import_modpack);

        TextView fileNameView = findViewById(R.id.import_modpack_file_name);
        fileNameView.setText(getString(R.string.import_modpack_confirm, fileName));

        findViewById(R.id.import_modpack_install_button).setOnClickListener(v -> startInstall());
        findViewById(R.id.import_modpack_cancel_button).setOnClickListener(v -> finishAndRemoveTask());
    }

    private void startInstall() {
        final Uri uri = mUriData;
        final ContentResolver contentResolver = getContentResolver();
        final Context appContext = getApplicationContext();

        // Start the install in the background
        PojavApplication.sExecutorService.execute(() ->
            LocalModpackInstaller.installFromUri(uri, appContext, contentResolver)
        );

        // Immediately navigate to the launcher so the user can see progress there
        Toast.makeText(this, R.string.import_modpack_installing, Toast.LENGTH_SHORT).show();
        Intent launcherIntent = new Intent(this, LauncherActivity.class);
        launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(launcherIntent);
        finishAndRemoveTask();
    }

    /**
     * Extract the URI from the incoming intent (supports VIEW action).
     */
    private Uri getUriData() {
        Intent intent = getIntent();
        if (intent == null) return null;

        // VIEW action: data is in getData()
        Uri data = intent.getData();
        if (data != null) return data;

        // Try clipData as fallback
        try {
            return intent.getClipData().getItemAt(0).getUri();
        } catch (Exception ignored) {}

        return null;
    }
}
