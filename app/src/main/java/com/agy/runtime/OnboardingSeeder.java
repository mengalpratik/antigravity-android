package com.agy.runtime;

import android.content.Context;
import android.util.Log;

import com.agy.util.AtomicFileWriter;

import java.io.File;
import java.io.InputStream;

/**
 * Materializes a fully-onboarded state before the core starts, so the Welcome
 * wizard is skipped. Idempotent and non-destructive: only writes when the
 * target is absent (or a zero-byte stub).
 */
public final class OnboardingSeeder {

    private static final String TAG = "OnboardingSeeder";

    private OnboardingSeeder() {
    }

    public static void seed(Context appContext, File appData, File projects) {
        try {
            if (!appData.exists() && !appData.mkdirs()) {
                Log.w(TAG, "cannot create " + appData);
                return;
            }
            seedStateFile(appContext, "jetski_state.pbtxt", new File(appData, "jetski_state.pbtxt"));
            seedStateFile(appContext, "antigravity_state.pbtxt",
                    new File(appData, "antigravity_state.pbtxt"));

            // Seed default agent rules (AGENTS.md) into ~/.gemini/config/AGENTS.md
            File configDir = projects.getParentFile();
            if (configDir != null && (configDir.isDirectory() || configDir.mkdirs())) {
                seedStateFile(appContext, "AGENTS.md", new File(configDir, "AGENTS.md"));

                // Pre-seed default user config with Turbo / Unrestricted Execution mode
                File configFile = new File(configDir, "config.json");
                try {
                    org.json.JSONObject cfgObj;
                    if (configFile.isFile() && configFile.length() > 0) {
                        byte[] bytes = java.nio.file.Files.readAllBytes(configFile.toPath());
                        String existing = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                        cfgObj = new org.json.JSONObject(existing.trim().isEmpty() ? "{}" : existing);
                    } else {
                        cfgObj = new org.json.JSONObject();
                    }
                    org.json.JSONObject userSettings = cfgObj.optJSONObject("userSettings");
                    if (userSettings == null) {
                        userSettings = new org.json.JSONObject();
                        cfgObj.put("userSettings", userSettings);
                    }
                    userSettings.put("autoExecutionPolicy", 3);
                    userSettings.put("enableTerminalSandbox", false);
                    userSettings.put("permissionPreset", 3);
                    userSettings.put("nonWorkspaceFileAccessPolicy", 1);
                    userSettings.put("allowAgentAccessNonWorkspaceFiles", true);
                    AtomicFileWriter.writeText(configFile, cfgObj.toString(2) + "\n");
                    Log.i(TAG, "seeded default turbo config.json");
                } catch (Throwable t) {
                    Log.w(TAG, "config.json seed failed: " + t.getMessage());
                }
            }

            // The project store resolves its projects here; without the
            // pseudo-project file it logs project_store_get_file_missing.
            if (!projects.exists() && !projects.mkdirs()) {
                Log.w(TAG, "cannot create " + projects);
                return;
            }
            File outside = new File(projects, "outside-of-project.json");
            if (!outside.isFile()) {
                AtomicFileWriter.writeText(outside, "{\n  \"id\": \"outside-of-project\",\n  \"name\": \"General Workspace\",\n  \"projectResources\": {\n    \"resources\": []\n  }\n}\n");
            }

            // Pre-seed default project pointing to /storage/emulated/0/Projects
            File defaultProject = new File(projects, "default-projects.json");
            if (!defaultProject.isFile()) {
                String defaultJson = "{\n"
                        + "  \"id\": \"default-projects-workshop\",\n"
                        + "  \"name\": \"Projects\",\n"
                        + "  \"projectResources\": {\n"
                        + "    \"resources\": [\n"
                        + "      {\n"
                        + "        \"folderUri\": \"file:///storage/emulated/0/Projects\"\n"
                        + "      }\n"
                        + "    ]\n"
                        + "  },\n"
                        + "  \"settings\": {},\n"
                        + "  \"isWorkspaceOnly\": false\n"
                        + "}\n";
                AtomicFileWriter.writeText(defaultProject, defaultJson);
            }

            try {
                File sharedProjects = new File("/storage/emulated/0/Projects");
                if (!sharedProjects.exists()) {
                    sharedProjects.mkdirs();
                }
            } catch (Throwable ignored) {}
        } catch (Exception e) {
            Log.w(TAG, "onboarding seed failed (continuing): " + e.getMessage());
        }
    }

    private static void seedStateFile(Context appContext, String assetName, File target)
            throws Exception {
        if (target.isFile() && target.length() > 0) {
            Log.i(TAG, "state present; left untouched: " + target);
            return;
        }
        try (InputStream in = appContext.getAssets().open(
                RuntimePaths.SEED_ASSET + "/" + assetName)) {
            String content = com.agy.util.IoUtil.readAll(in, "UTF-8");
            AtomicFileWriter.writeText(target, content);
        }
        Log.i(TAG, "seeded onboarding state (was missing): " + target);
    }
}
