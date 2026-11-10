package com.agy.workspace.storage;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Manages the physical folder architecture for Antigravity Centralized Workspace.
 *
 * Base hierarchy:
 * /storage/emulated/0/Antigravity/
 * ├── Workspace/
 * ├── Projects/
 * ├── Templates/
 * │   ├── project-templates/
 * │   ├── documentation-templates/
 * │   └── architecture-templates/
 * ├── Exports/
 * │   ├── reports/
 * │   ├── documentation-packages/
 * │   └── backups/
 * └── Shared/
 *     ├── references/
 *     ├── resources/
 *     └── common-guidelines/
 */
public final class WorkspaceStorageManager {

    private static final String TAG = "WorkspaceStorage";

    public static final String ANTIGRAVITY_ROOT = "Antigravity";
    public static final String DIR_WORKSPACE = "Workspace";
    public static final String DIR_PROJECTS = "Projects";
    public static final String DIR_TEMPLATES = "Templates";
    public static final String DIR_EXPORTS = "Exports";
    public static final String DIR_SHARED = "Shared";

    // Templates subdirectories
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

    public WorkspaceStorageManager(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    /**
     * Resolves the primary root directory for Antigravity workspace files.
     * Defaults to /storage/emulated/0/Antigravity. Falls back to app-specific external
     * storage if the top-level external directory cannot be accessed.
     */
    public File getRootDir() {
        File primary = new File(Environment.getExternalStorageDirectory(), ANTIGRAVITY_ROOT);
        try {
            if (primary.isDirectory() || primary.mkdirs()) {
                return primary;
            }
        } catch (Throwable t) {
            Log.w(TAG, "Cannot create primary root at " + primary.getAbsolutePath() + ": " + t.getMessage());
        }

        // Fallback to app-specific scoped external files directory if primary fails
        if (context != null) {
            File fallbackBase = context.getExternalFilesDir(null);
            if (fallbackBase != null) {
                File fallback = new File(fallbackBase, ANTIGRAVITY_ROOT);
                if (fallback.isDirectory() || fallback.mkdirs()) {
                    Log.i(TAG, "Using scoped storage fallback root: " + fallback.getAbsolutePath());
                    return fallback;
                }
            }
        }

        return primary;
    }

    public File getWorkspaceDir() {
        return new File(getRootDir(), DIR_WORKSPACE);
    }

    public File getProjectsDir() {
        return new File(getRootDir(), DIR_PROJECTS);
    }

    public File getTemplatesDir() {
        return new File(getRootDir(), DIR_TEMPLATES);
    }

    public File getExportsDir() {
        return new File(getRootDir(), DIR_EXPORTS);
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
        return new File(getExportsDir(), SUB_BACKUPS);
    }

    public File getSharedReferencesDir() {
        return new File(getSharedDir(), SUB_REFERENCES);
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
        if (slug == null || slug.trim().isEmpty()) {
            return getProjectsDir();
        }
        return new File(getProjectsDir(), slug.trim());
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
            File root = getRootDir();
            if (!ensureDirectory(root)) {
                Log.w(TAG, "Failed to initialize root directory: " + root.getAbsolutePath());
                return false;
            }

            // Primary directories
            ensureDirectory(getWorkspaceDir());
            ensureDirectory(getProjectsDir());
            ensureDirectory(getTemplatesDir());
            ensureDirectory(getExportsDir());
            ensureDirectory(getSharedDir());

            // Templates subdirectories
            ensureDirectory(getProjectTemplatesDir());
            ensureDirectory(getDocTemplatesDir());
            ensureDirectory(getArchTemplatesDir());

            // Exports subdirectories
            ensureDirectory(getExportsReportsDir());
            ensureDirectory(getExportsDocPackagesDir());
            ensureDirectory(getExportsBackupsDir());

            // Shared subdirectories
            ensureDirectory(getSharedReferencesDir());
            ensureDirectory(getSharedResourcesDir());
            ensureDirectory(getSharedGuidelinesDir());

            Log.i(TAG, "Centralized Antigravity workspace hierarchy initialized at: " + root.getAbsolutePath());
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Error initializing workspace hierarchy", t);
            return false;
        }
    }

    private static boolean ensureDirectory(File dir) {
        if (dir == null) {
            return false;
        }
        if (dir.isDirectory()) {
            return true;
        }
        return dir.mkdirs();
    }
}
