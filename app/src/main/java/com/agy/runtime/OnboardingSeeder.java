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
                    userSettings.put("allowAgentAccessNonWorkspaceFiles", true);
                    userSettings.put("artifactReviewMode", "ARTIFACT_REVIEW_MODE_TURBO");
                    userSettings.put("autoExecutionPolicy", "CASCADE_COMMANDS_AUTO_EXECUTION_EAGER");
                    userSettings.put("browserJsExecutionPolicy", "BROWSER_JS_EXECUTION_POLICY_TURBO");
                    userSettings.put("enableTerminalSandbox", false);
                    userSettings.put("nonWorkspaceFileAccessPolicy", "AGENT_SETTING_POLICY_ALLOW");
                    userSettings.put("permissionPreset", 3);

                    org.json.JSONObject globalGrants = new org.json.JSONObject();
                    org.json.JSONArray allowList = new org.json.JSONArray();
                    allowList.put("command(*)");
                    allowList.put("read_file(*)");
                    allowList.put("write_file(*)");
                    allowList.put("unsandboxed(*)");
                    allowList.put("read_file(/)");
                    allowList.put("write_file(/)");
                    allowList.put("read_file(/storage/emulated/0)");
                    allowList.put("write_file(/storage/emulated/0)");
                    allowList.put("read_file(/storage/emulated/0/**)");
                    allowList.put("write_file(/storage/emulated/0/**)");
                    allowList.put("read_file(/data/user/0/com.agy)");
                    allowList.put("write_file(/data/user/0/com.agy)");
                    allowList.put("read_file(/data/user/0/com.agy/**)");
                    allowList.put("write_file(/data/user/0/com.agy/**)");
                    allowList.put("read_url(*)");
                    allowList.put("execute_url(*)");
                    allowList.put("mcp(*)");
                    globalGrants.put("allow", allowList);
                    userSettings.put("globalPermissionGrants", globalGrants);

                    AtomicFileWriter.writeText(configFile, cfgObj.toString(2) + "\n");
                    Log.i(TAG, "seeded default turbo config.json with unrestricted grants");
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
            if (!outside.isFile() || outside.length() == 0) {
                try {
                    com.agy.workspace.storage.WorkspaceRootManager rootMgr =
                            com.agy.workspace.storage.WorkspaceRootManager.getInstance(appContext);
                    String projectsUri = "file://" + rootMgr.getProjectsDir().getAbsolutePath();
                    String defaultWsUri = "file://" + rootMgr.getDefaultWorkspaceDir().getAbsolutePath();
                    String antigravityUri = "file://" + rootMgr.getRootDir().getAbsolutePath();

                    org.json.JSONObject outsideObj = new org.json.JSONObject();
                    outsideObj.put("id", "outside-of-project");
                    outsideObj.put("name", "General Workspace");

                    org.json.JSONObject resourcesObj = new org.json.JSONObject();
                    org.json.JSONArray resList = new org.json.JSONArray();
                    org.json.JSONObject resProjects = new org.json.JSONObject();
                    resProjects.put("folderUri", projectsUri);
                    resList.put(resProjects);
                    org.json.JSONObject resWs = new org.json.JSONObject();
                    resWs.put("folderUri", defaultWsUri);
                    resList.put(resWs);
                    org.json.JSONObject resAntigravity = new org.json.JSONObject();
                    resAntigravity.put("folderUri", antigravityUri);
                    resList.put(resAntigravity);
                    org.json.JSONObject resInternal = new org.json.JSONObject();
                    resInternal.put("folderUri", "file:///data/user/0/com.agy/files");
                    resList.put(resInternal);
                    resourcesObj.put("resources", resList);
                    outsideObj.put("projectResources", resourcesObj);

                    org.json.JSONObject settingsObj = new org.json.JSONObject();
                    settingsObj.put("autoExecutionPolicy", "CASCADE_COMMANDS_AUTO_EXECUTION_EAGER");
                    settingsObj.put("fileAccessPolicy", "AGENT_SETTING_POLICY_ALLOW");
                    settingsObj.put("sandboxMode", false);
                    settingsObj.put("artifactReviewMode", "ARTIFACT_REVIEW_MODE_TURBO");
                    outsideObj.put("settings", settingsObj);

                    org.json.JSONObject permObj = new org.json.JSONObject();
                    org.json.JSONObject permInner = new org.json.JSONObject();
                    org.json.JSONArray pAllow = new org.json.JSONArray();
                    pAllow.put("command(*)");
                    pAllow.put("read_file(*)");
                    pAllow.put("write_file(*)");
                    pAllow.put("unsandboxed(*)");
                    pAllow.put("read_file(/)");
                    pAllow.put("write_file(/)");
                    pAllow.put("read_file(/storage/emulated/0)");
                    pAllow.put("write_file(/storage/emulated/0)");
                    pAllow.put("read_file(/storage/emulated/0/**)");
                    pAllow.put("write_file(/storage/emulated/0/**)");
                    pAllow.put("read_file(/storage/emulated/0/Antigravity)");
                    pAllow.put("write_file(/storage/emulated/0/Antigravity)");
                    pAllow.put("read_file(/storage/emulated/0/Antigravity/**)");
                    pAllow.put("write_file(/storage/emulated/0/Antigravity/**)");
                    pAllow.put("read_file(" + rootMgr.getRootDir().getAbsolutePath() + "/**)");
                    pAllow.put("write_file(" + rootMgr.getRootDir().getAbsolutePath() + "/**)");
                    permInner.put("allow", pAllow);
                    permObj.put("permissionGrants", permInner);
                    outsideObj.put("permissionGrants", permObj);

                    AtomicFileWriter.writeText(outside, outsideObj.toString(2) + "\n");
                } catch (Throwable t) {
                    Log.w(TAG, "outside-of-project seed failed: " + t.getMessage());
                }
            }

            // Pre-seed default project pointing to centralized Antigravity workspace
            File defaultProject = new File(projects, "default-projects.json");
            if (!defaultProject.isFile() || defaultProject.length() == 0) {
                try {
                    com.agy.workspace.storage.WorkspaceRootManager rootMgr =
                            com.agy.workspace.storage.WorkspaceRootManager.getInstance(appContext);
                    String projectsUri = "file://" + rootMgr.getProjectsDir().getAbsolutePath();
                    String defaultWsUri = "file://" + rootMgr.getDefaultWorkspaceDir().getAbsolutePath();
                    String antigravityUri = "file://" + rootMgr.getRootDir().getAbsolutePath();

                    org.json.JSONObject defProjObj = new org.json.JSONObject();
                    defProjObj.put("id", "default-projects-workshop");
                    defProjObj.put("name", "Projects");

                    org.json.JSONObject resourcesObj = new org.json.JSONObject();
                    org.json.JSONArray resList = new org.json.JSONArray();
                    org.json.JSONObject resProj = new org.json.JSONObject();
                    resProj.put("folderUri", projectsUri);
                    resList.put(resProj);
                    org.json.JSONObject resWs = new org.json.JSONObject();
                    resWs.put("folderUri", defaultWsUri);
                    resList.put(resWs);
                    org.json.JSONObject resAntigravity = new org.json.JSONObject();
                    resAntigravity.put("folderUri", antigravityUri);
                    resList.put(resAntigravity);

                    // Only add legacy folder if it actually exists on the filesystem
                    File legacyProjects = new File(com.agy.workspace.storage.WorkspaceRootManager.LEGACY_EXTERNAL_PROJECTS);
                    if (legacyProjects.isDirectory()) {
                        org.json.JSONObject resLegacy = new org.json.JSONObject();
                        resLegacy.put("folderUri", "file://" + legacyProjects.getAbsolutePath());
                        resList.put(resLegacy);
                    }

                    resourcesObj.put("resources", resList);
                    defProjObj.put("projectResources", resourcesObj);

                    org.json.JSONObject settingsObj = new org.json.JSONObject();
                    settingsObj.put("autoExecutionPolicy", "CASCADE_COMMANDS_AUTO_EXECUTION_EAGER");
                    settingsObj.put("fileAccessPolicy", "AGENT_SETTING_POLICY_ALLOW");
                    settingsObj.put("sandboxMode", false);
                    settingsObj.put("artifactReviewMode", "ARTIFACT_REVIEW_MODE_TURBO");
                    defProjObj.put("settings", settingsObj);

                    org.json.JSONObject permObj = new org.json.JSONObject();
                    org.json.JSONObject permInner = new org.json.JSONObject();
                    org.json.JSONArray pAllow = new org.json.JSONArray();
                    pAllow.put("command(*)");
                    pAllow.put("read_file(*)");
                    pAllow.put("write_file(*)");
                    pAllow.put("unsandboxed(*)");
                    pAllow.put("read_file(/)");
                    pAllow.put("write_file(/)");
                    pAllow.put("read_file(/storage/emulated/0)");
                    pAllow.put("write_file(/storage/emulated/0)");
                    pAllow.put("read_file(/storage/emulated/0/**)");
                    pAllow.put("write_file(/storage/emulated/0/**)");
                    pAllow.put("read_file(/storage/emulated/0/Antigravity/**)");
                    pAllow.put("write_file(/storage/emulated/0/Antigravity/**)");
                    pAllow.put("read_file(" + rootMgr.getRootDir().getAbsolutePath() + "/**)");
                    pAllow.put("write_file(" + rootMgr.getRootDir().getAbsolutePath() + "/**)");
                    permInner.put("allow", pAllow);
                    permObj.put("permissionGrants", permInner);
                    defProjObj.put("permissionGrants", permObj);
                    defProjObj.put("isWorkspaceOnly", false);

                    AtomicFileWriter.writeText(defaultProject, defProjObj.toString(2) + "\n");
                } catch (Throwable t) {
                    Log.w(TAG, "default-projects seed failed: " + t.getMessage());
                }
            }

            try {
                // Initialize Centralized Workspace architecture
                com.agy.workspace.storage.WorkspaceRootManager rootMgr =
                        com.agy.workspace.storage.WorkspaceRootManager.getInstance(appContext);
                rootMgr.initializeHierarchy();

                // Non-destructive migration for existing legacy /storage/emulated/0/Projects (only if it already exists!)
                File sharedProjects = new File(com.agy.workspace.storage.WorkspaceRootManager.LEGACY_EXTERNAL_PROJECTS);
                if (sharedProjects.isDirectory()) {
                    File[] subdirs = sharedProjects.listFiles();
                    if (subdirs != null) {
                        com.agy.workspace.db.ProjectRegistryRepository repo =
                                new com.agy.workspace.db.ProjectRegistryRepository(appContext);
                        com.agy.workspace.generator.WorkspaceDocumentGenerator docGen =
                                new com.agy.workspace.generator.WorkspaceDocumentGenerator(appContext);
                        try {
                            for (File sub : subdirs) {
                                if (sub.isDirectory() && !sub.getName().startsWith(".")) {
                                    String absPath = sub.getAbsolutePath();
                                    if (repo.getProjectBySourcePath(absPath) == null) {
                                        com.agy.workspace.model.ProjectRecord p =
                                                new com.agy.workspace.model.ProjectRecord();
                                        p.setId(java.util.UUID.randomUUID().toString());
                                        p.setName(sub.getName());
                                        String cleanSlug = sub.getName().toLowerCase().replaceAll("[^a-z0-9_\\-]+", "-");
                                        p.setSlug(cleanSlug.isEmpty() ? "project" : cleanSlug);
                                        p.setSourcePath(absPath);
                                        p.setSourceUri("file://" + absPath);
                                        p.setSourceType(com.agy.workspace.model.ProjectRecord.SOURCE_TYPE_DEFAULT);
                                        File gitDir = new File(sub, ".git");
                                        if (gitDir.isDirectory()) p.setVcsType("git");
                                        docGen.initializeWorkspace(p);
                                        repo.insertProject(p);
                                        Log.i(TAG, "Migrated legacy project: " + sub.getName() + " -> centralized workspace");
                                    }
                                }
                            }
                        } finally {
                            repo.close();
                        }
                    }
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
