package net.kdt.pojavlaunch.modloaders.modpacks.api;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.Tools;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import git.artdeell.mojo.R;

/**
 * Shared utility for installing local modpack files from a content URI.
 * Used by both SearchModFragment (in-app file picker) and ImportModpackActivity (external file open).
 */
public class LocalModpackInstaller {

    /**
     * Perform a local modpack install from a content URI.
     * Must be called on a background thread.
     *
     * @param uri            the content URI of the modpack file
     * @param context        application context for resource access
     * @param contentResolver content resolver to open the URI stream
     * @return true if the installation succeeded, false otherwise
     */
    public static boolean installFromUri(Uri uri, Context context, ContentResolver contentResolver) {
        String fileName = Tools.getFileName(context, uri);
        if (fileName == null) return false;
        File outFile = new File(Tools.DIR_CACHE, fileName + ".cf");
        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, R.string.multirt_progress_caching);
        try (InputStream inputStream = contentResolver.openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(outFile)) {
            if (inputStream == null) return false;
            IOUtils.copy(inputStream, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            Tools.showErrorRemote("Error", e);
            return false;
        }
        try {
            ModpackApi modpackApi = new CommonApi(context.getString(R.string.curseforge_api_key));
            modpackApi.installLocalModpack(fileName, outFile, null);
            return true;
        } catch (IOException e) {
            Tools.showErrorRemote("Error", e);
            return false;
        } finally {
            outFile.delete();
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
        }
    }
}
