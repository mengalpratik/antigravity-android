package com.agy.workspace.storage;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Storage manager for Antigravity Workspace.
 * Delegates to the centralized {@link WorkspaceRootManager} for all core paths,
 * hierarchy initialization, and safe migrations, while preserving full backwards
 * compatibility with existing project-management APIs.
 */
public final class WorkspaceStorageManager {

    private static final String TAG = "WorkspaceStorage";

    public static final String ANTIGRAVITY_ROOT = WorkspaceRootManager.DEFAULT_ROOT_DIR_NAME;
    public static final String DIR_WORKSPACE = "Workspaces";
    public static final String DIR_PROJECTS = WorkspaceRootManager.DIR_PROJECTS;
    public static final String DIR_TEMPLATES = "Templates";
    public static final String DIR_EXPORTS = WorkspaceRootManager.DIR_EXPORTS;
    public static final String DIR_SHARED = "Shared";

    // Legacy/Templates subdirectories
    public static final String SUB_PROJECT_TEMPLATES = "project-templates";
    public static final String SUB_DOC_TEMPLATES = "documentation-templates";
    public static final String SUB_ARCH_TEMPLATES = "architecture-templates";

    // Exports subdirectories
    public static final String SUB_REPORTS = "reports";
    public static final String SUB_DOC_PACKAGES = "documentation-packages";
    public static final String SUB_BACKUPS = "backups";

    // Shared subdirectories
    public static final String SUB_REFERENCES = "references";
    public static final String SUB_RESOURCES = "resources";
    public static final String SUB_GUIDELINES = "common-guidelines";

    private final Context context;
    private final WorkspaceRootManager rootManager;

    public WorkspaceStorageManager(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.rootManager = WorkspaceRootManager.getInstance(this.context);
    }

    public WorkspaceRootManager getRootManager() {
        return rootManager;
    }

    /** Resolves the active root directory for Antigravity workspace files. */
    public File getRootDir() {
        return rootManager.getRootDir();
    }

    /** Returns the Workspaces directory for documentation & intelligence. */
    public File getWorkspaceDir() {
        return rootManager.getWorkspacesDir();
    }

    public File getWorkspacesDir() {
        return rootManager.getWorkspacesDir();
    }

    public File getDefaultWorkspaceDir() {
        return rootManager.getDefaultWorkspaceDir();
    }

    /** Returns the default source code projects directory. */
    public File getProjectsDir() {
        return rootManager.getProjectsDir();
    }

    public File getTemplatesDir() {
        return new File(getRootDir(), DIR_TEMPLATES);
    }

    public File getExportsDir() {
        return rootManager.getExportsDir();
    }

    public File getSharedDir() {
        return new File(getRootDir(), DIR_SHARED);
    }

    public File getProjectTemplatesDir() {
        return new File(getTemplatesDir(), SUB_PROJECT_TEMPLATES);
    }

    public File getDocTemplatesDir() {
        return new File(getTemplatesDir(), SUB_DOC_TEMPLATES);
    }

    public File getArchTemplatesDir() {
        return new File(getTemplatesDir(), SUB_ARCH_TEMPLATES);
    }

    public File getExportsReportsDir() {
        return new File(getExportsDir(), SUB_REPORTS);
    }

    public File getExportsDocPackagesDir() {
        return new File(getExportsDir(), SUB_DOC_PACKAGES);
    }

    public File getExportsBackupsDir() {
        return rootManager.getBackupsDir();
    }

    public File getSharedReferencesDir() {
        return rootManager.getKnowledgeReferencesDir();
    }

    public File getSharedResourcesDir() {
        return new File(getSharedDir(), SUB_RESOURCES);
    }

    public File getSharedGuidelinesDir() {
        return new File(getSharedDir(), SUB_GUIDELINES);
    }

    /** Returns the centralized workspace documentation folder for a specific project slug. */
    public File getProjectWorkspaceDir(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            return getWorkspaceDir();
        }
        return new File(getWorkspaceDir(), slug.trim());
    }

    /** Returns the default source code folder for a specific project slug. */
    public File getProjectSourceDir(String slug) {
        return rootManager.resolveProjectDir(slug);
    }

    /** Returns the path to project.yaml within the centralized workspace directory. */
    public File getProjectYaml(String slug) {
        return new File(getProjectWorkspaceDir(slug), "project.yaml");
    }

    /**
     * Initializes the full folder architecture idempotently.
     * Safe to invoke multiple times during application startup.
     */
    public boolean initializeHierarchy() {
        try {
            boolean ok = rootManager.initializeHierarchy();

            // Optional templates/shared helpers
            ensureDirectory(getTemplatesDir());
            ensureDirectory(getProjectTemplatesDir());
            ensureDirectory(getDocTemplatesDir());
            ensureDirectory(getArchTemplatesDir());
            ensureDirectory(getSharedDir());
            ensureDirectory(getSharedResourcesDir());
            ensureDirectory(getSharedGuidelinesDir());

            return ok;
        } catch (Throwable t) {
            Log.e(TAG, "Error initializing workspace hierarchy", t);
            return false;
        }
    }

    private static boolean ensureDirectory(File dir) {
        if (dir == null) return false;
        if (dir.isDirectory()) return true;
        return dir.mkdirs();
    }
}
