package com.agy.shell;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Root access bridge supporting KernelSU, Magisk, APatch, and standard su binaries.
 * Features asynchronous initial probing, non-blocking status checks, and resilient execution.
 */
public final class RootBridge {

    private static final String TAG = "RootBridge";
    private final Context appContext;
    private final SuLocator locator;
    private final AtomicBoolean rootActive = new AtomicBoolean(false);
    private volatile boolean probed = false;
    private volatile String providerName = "None";

    public RootBridge(Context context) {
        this.appContext = context.getApplicationContext();
        this.locator = new SuLocator(context);
        this.providerName = locator.detectProvider();
        probeAsync();
    }

    public boolean isAvailable() {
        return locator.hasSuBinary();
    }

    public boolean isRootActive() {
        if (!probed) {
            // If probe hasn't finished, do a quick fast-check
            probeSync(1500);
        }
        return rootActive.get();
    }

    public String getRootProvider() {
        return providerName;
    }

    public String getSuPath() {
        return locator.suBinary().getAbsolutePath();
    }

    public void probeAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                probeSync(3000);
            }
        }, "RootProbeThread").start();
    }

    private synchronized void probeSync(long timeoutMs) {
        if (!locator.hasSuBinary()) {
            probed = true;
            rootActive.set(false);
            return;
        }

        File su = locator.suBinary();
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(su.getAbsolutePath(), "-c", "id");
            pb.redirectErrorStream(true);
            pb.directory(appContext.getFilesDir());
            process = pb.start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (finished && process.exitValue() == 0) {
                String result = out.toString().trim();
                if (result.contains("uid=0")) {
                    rootActive.set(true);
                    providerName = locator.detectProvider();
                    Log.i(TAG, "Root detected and active (" + providerName + "): " + result);
                } else {
                    rootActive.set(false);
                    Log.w(TAG, "su returned non-root identity: " + result);
                }
            } else {
                rootActive.set(false);
                Log.w(TAG, "su probe failed or timed out (exit=" + (finished ? process.exitValue() : "timeout") + ")");
            }
        } catch (Throwable t) {
            rootActive.set(false);
            Log.w(TAG, "Root probe exception: " + t.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
            probed = true;
        }
    }

    public String exec(String cmd) {
        return exec(cmd, 30000);
    }

    public String exec(String cmd, long timeoutMs) {
        if (cmd == null || cmd.trim().isEmpty()) {
            return "error: empty command";
        }
        File su = locator.suBinary();
        if (!su.exists()) {
            return "error: su binary not found";
        }

        ProcessBuilder pb = new ProcessBuilder(su.getAbsolutePath(), "-c", cmd);
        pb.redirectErrorStream(true);
        pb.directory(appContext.getFilesDir());

        Process process = null;
        try {
            process = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroy();
                return "error: command timed out after " + timeoutMs + "ms";
            }
            int code = process.exitValue();
            String text = out.toString().trim();
            if (code != 0 && text.isEmpty()) {
                return "error: su command exited with code " + code;
            }
            return text;
        } catch (Exception e) {
            return "error: " + e.getMessage();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    public JSONObject getStatusJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("available", isAvailable());
            obj.put("active", isRootActive());
            obj.put("provider", providerName);
            obj.put("suPath", getSuPath());
        } catch (Exception ignored) {}
        return obj;
    }
}
