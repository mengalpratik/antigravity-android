package com.agy.shell;

import android.content.Context;
import java.io.File;

/**
 * Locates the system `su` binary and detects the active root manager (KernelSU, Magisk, APatch, etc.).
 */
public final class SuLocator {

    private final Context appContext;

    private static final String[] KNOWN_SU_PATHS = new String[]{
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/adb/ksu/bin/su",
            "/data/adb/ap/bin/su",
            "/data/adb/magisk/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
    };

    public SuLocator(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public File suBinary() {
        for (String path : KNOWN_SU_PATHS) {
            File f = new File(path);
            if (f.exists() && f.canExecute()) {
                return f;
            }
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String[] dirs = pathEnv.split(":");
            for (String dir : dirs) {
                File f = new File(dir, "su");
                if (f.exists() && f.canExecute()) {
                    return f;
                }
            }
        }
        for (String path : KNOWN_SU_PATHS) {
            File f = new File(path);
            if (f.exists()) {
                return f;
            }
        }
        return new File("/system/bin/su");
    }

    public boolean hasSuBinary() {
        File su = suBinary();
        return su != null && su.exists();
    }

    public String detectProvider() {
        File su = suBinary();
        if (su != null && su.exists()) {
            try {
                Process p = new ProcessBuilder(su.getAbsolutePath(), "-v").start();
                java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
                String out = r.readLine();
                p.waitFor();
                if (out != null) {
                    String lower = out.toLowerCase();
                    if (lower.contains("kernelsu") || lower.contains("ksu")) return "KernelSU";
                    if (lower.contains("magisk")) return "Magisk";
                    if (lower.contains("apatch")) return "APatch";
                    if (lower.contains("supersu")) return "SuperSU";
                }
            } catch (Throwable ignored) {}
        }
        if (new File("/data/adb/ksu").isDirectory() || new File("/data/adb/ksu/bin/su").exists()) {
            return "KernelSU";
        }
        if (new File("/data/adb/ap").isDirectory() || new File("/data/adb/ap/bin/su").exists()) {
            return "APatch";
        }
        if (new File("/data/adb/magisk").isDirectory() || new File("/data/adb/magisk/su").exists()) {
            return "Magisk";
        }
        if (new File("/system/xbin/daemonsu").exists() || new File("/system/bin/daemonsu").exists()) {
            return "SuperSU";
        }
        if (hasSuBinary()) {
            return "Standard SU";
        }
        return "None";
    }
}
