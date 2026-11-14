package com.agy.bridge;

import android.content.Context;

import com.agy.runtime.RuntimeConfig;
import com.agy.runtime.RuntimeManager;
import com.agy.shell.RootBridge;
import com.agy.shell.ShizukuBridge;
import com.agy.workspace.storage.WorkspaceRootManager;

import org.json.JSONObject;

/** Serializes the app config object injected as window.__APP_CONFIG__. */
public final class EngineConfigJson {

    private final Context appContext;
    private final RuntimeManager runtime;
    private final EngineBridge engine;
    private final ShizukuBridge shizuku;
    private final ThemeBridge theme;
    private final RootBridge root;

    public EngineConfigJson(Context appContext, RuntimeManager runtime, EngineBridge engine,
                            ShizukuBridge shizuku, ThemeBridge theme) {
        this(appContext, runtime, engine, shizuku, theme, new RootBridge(appContext));
    }

    public EngineConfigJson(Context appContext, RuntimeManager runtime, EngineBridge engine,
                            ShizukuBridge shizuku, ThemeBridge theme, RootBridge root) {
        this.appContext = appContext;
        this.runtime = runtime;
        this.engine = engine;
        this.shizuku = shizuku;
        this.theme = theme;
        this.root = root != null ? root : new RootBridge(appContext);
    }

    public String get() {
        try {
            JSONObject o = new JSONObject();
            o.put("productName", "antigravity-app");
            o.put("csrfToken", engine.getCsrfToken());
            o.put("appVersion", RuntimeConfig.APP_VERSION_NAME);
            o.put("appVersionCode", RuntimeConfig.APP_VERSION_CODE);
            o.put("coreVersion", RuntimeConfig.CORE_VERSION);
            o.put("releaseChannel", RuntimeConfig.DEBUG ? "dev" : "stable");
            o.put("devMode", RuntimeConfig.DEBUG);
            o.put("enginePort", runtime.getHttpsPort());
            o.put("engineBaseUrl", engine.getBaseUrl());
            o.put("filesDir", appContext.getFilesDir().getAbsolutePath());
            o.put("runtimeDir", runtime.getRuntimeDir().getAbsolutePath());
            o.put("home", appContext.getFilesDir().getAbsolutePath());
            o.put("tmpDir", appContext.getCacheDir().getAbsolutePath());
            o.put("appDataDir", runtime.getAppDataDir().getAbsolutePath());

            // Centralized Workspace Root Manager
            WorkspaceRootManager rootMgr = WorkspaceRootManager.getInstance(appContext);
            o.put("workspaceRoot", rootMgr.getRootDir().getAbsolutePath() + "/");
            o.put("antigravityRoot", rootMgr.getRootDir().getAbsolutePath());
            o.put("defaultProjectsDir", rootMgr.getProjectsDir().getAbsolutePath());
            o.put("centralizedWorkspaceDir", rootMgr.getWorkspacesDir().getAbsolutePath());
            o.put("workspacesDir", rootMgr.getWorkspacesDir().getAbsolutePath());
            o.put("defaultWorkspaceDir", rootMgr.getDefaultWorkspaceDir().getAbsolutePath());
            o.put("systemDir", rootMgr.getSystemDir().getAbsolutePath());
            o.put("agentsDir", rootMgr.getAgentsDir().getAbsolutePath());
            o.put("knowledgeDir", rootMgr.getKnowledgeDir().getAbsolutePath());
            o.put("logsDir", rootMgr.getLogsDir().getAbsolutePath());
            o.put("cacheDir", rootMgr.getCacheDir().getAbsolutePath());
            o.put("backupsDir", rootMgr.getBackupsDir().getAbsolutePath());
            o.put("exportsDir", rootMgr.getExportsDir().getAbsolutePath());
            o.put("isCustomWorkspaceRoot", rootMgr.isCustomRoot());

            o.put("shizukuAvailable", shizuku.isAvailable());
            o.put("rootAvailable", root.isAvailable());
            o.put("rootActive", root.isRootActive());
            o.put("rootProvider", root.getRootProvider());
            o.put("theme", theme.getTheme());
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}
