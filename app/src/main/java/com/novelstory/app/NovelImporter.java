package com.novelstory.app;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class NovelImporter {

    public static final int OK = 0;
    public static final int ERR_READ = 1;
    public static final int ERR_INVALID = 2;

    private static final int MAX_FILES = 500;
    private static final long MAX_BYTES = 30L * 1024 * 1024;

    private NovelImporter() {
    }

    public static int importZip(Context c, Uri uri) {
        File base = new File(c.getFilesDir(), "novels");
        if (!base.exists() && !base.mkdirs()) {
            return ERR_READ;
        }
        String id = "novel_" + System.currentTimeMillis();
        File dir = new File(base, id);
        if (!dir.mkdirs()) {
            return ERR_READ;
        }

        long total = 0;
        int files = 0;
        try (InputStream raw = c.getContentResolver().openInputStream(uri);
             ZipInputStream zin = raw == null ? null : new ZipInputStream(raw)) {
            if (zin == null) {
                deleteRecursive(dir);
                return ERR_READ;
            }
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    zin.closeEntry();
                    continue;
                }
                if (name.contains("..") || name.startsWith("/") || name.contains("\\")) {
                    deleteRecursive(dir);
                    return ERR_INVALID;
                }
                if (!name.matches("^(tl|en)/(chapter\\d{2}\\.md|quiz\\.json)$")) {
                    zin.closeEntry();
                    continue;
                }
                File out = new File(dir, name);
                if (!out.getParentFile().exists() && !out.getParentFile().mkdirs()) {
                    deleteRecursive(dir);
                    return ERR_READ;
                }
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = zin.read(buf)) != -1) {
                        total += r;
                        if (total > MAX_BYTES) {
                            deleteRecursive(dir);
                            return ERR_READ;
                        }
                        fos.write(buf, 0, r);
                    }
                }
                zin.closeEntry();
                files++;
                if (files > MAX_FILES) {
                    deleteRecursive(dir);
                    return ERR_READ;
                }
            }
        } catch (IOException e) {
            deleteRecursive(dir);
            return ERR_READ;
        }

        boolean hasTl = new File(dir, "tl/chapter01.md").isFile();
        boolean hasEn = new File(dir, "en/chapter01.md").isFile();
        if (!hasTl && !hasEn) {
            deleteRecursive(dir);
            return ERR_INVALID;
        }

        String previous = Prefs.getImportedNovel(c);
        Prefs.setImportedNovel(c, id);
        Prefs.setActiveNovel(c, id);
        if (!previous.isEmpty() && !previous.equals(id)) {
            deleteRecursive(Content.importedRoot(c, previous));
        }
        return OK;
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) {
                deleteRecursive(k);
            }
        }
        f.delete();
    }
}
