package com.agy.workspace.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized Workspace Root Manager for Antigravity Android.
 *
 * Replaces the legacy default project directory (/storage/emulated/0/Projects/)
 * with a dedicated, IDE-grade Antigravity application workspace root:
 *
 * /storage/emulated/0/Antigravity/
 * ├── Projects/
 * ├── Workspaces/
 * │   └── Default/
 * ├── System/
 * │   ├── config/
 * │   ├── profiles/
 * │   └── models/
 * ├── Agents/
 * │   ├── prompts/
 * │   ├── instructions/
 * │   └── tools/
 * ├── Knowledge/
 * │   ├── memory/
 * │   ├── references/
 * │   └── documentation/
 * ├── Logs/
 * ├── Cache/
 * ├── Backups/
 * └── Exports/
 *
 * Features:
 * - Dynamic path resolution with Scoped Storage fallback.
 * - Custom workspace root selection via SAF with persistent preferences.
 * - Idempotent, non-destructive directory hierarchy initialization.
 * - Legacy directory detection (/storage/emulated/0/Projects/).
 * - Safe migration engine with integrity verification and rollback support.
 * - Strict isolation of Logs and Cache from user project Git repositories.
 * - Never places app-private runtime binaries, tokens, or credentials in external storage.
 */
public final class WorkspaceRootManager {

    private static final String TAG = "WorkspaceRootManager";

    // Default top-level root name
    public static final String DEFAULT_ROOT_DIR_NAME = "Antigravity";
    public static final String DEFAULT_EXTERNAL_ROOT = "/storage/emulated/0/Antigravity";
    public static final String LEGACY_EXTERNAL_PROJECTS = "/storage/emulated/0/Projects";

    // Primary child directories
    public static final String DIR_PROJECTS = "Projects";
    public static final String DIR_WORKSPACES = "Workspaces";
    public static final String DIR_SYSTEM = "System";
    public static final String DIR_AGENTS = "Agents";
    public static final String DIR_KNOWLEDGE = "Knowledge";
    public static final String DIR_LOGS = "Logs";
    public static final String DIR_CACHE = "Cache";
    public static final String DIR_BACKUPS = "Backups";
    public static final String DIR_EXPORTS = "Exports";

    // Workspaces subdirectories
    public static final String SUB_DEFAULT_WORKSPACE = "Default";

    // System subdirectories
    public static final String SUB_SYSTEM_CONFIG = "config";
    public static final String SUB_SYSTEM_PROFILES = "profiles";
    public static final String SUB_SYSTEM_MODELS = "models";

    // Agents subdirectories
    public static final String SUB_AGENTS_PROMPTS = "prompts";
    public static final String SUB_AGENTS_INSTRUCTIONS = "instructions";
    public static final String SUB_AGENTS_TOOLS = "tools";

    // Knowledge subdirectories
    public static final String SUB_KNOWLEDGE_MEMORY = "memory";
    public static final String SUB_KNOWLEDGE_REFERENCES = "references";
    public static final String SUB_KNOWLEDGE_DOCUMENTATION = "documentation";

    // SharedPreferences persistence constants
    public static final String PREFS_NAME = "antigravity_workspace_prefs";
    public static final String KEY_CUSTOM_ROOT_PATH = "custom_root_path";
    public static final String KEY_CUSTOM_ROOT_SAF_URI = "custom_root_saf_uri";
    public static final String KEY_MIGRATION_COMPLETED = "legacy_migration_completed";

    private static volatile WorkspaceRootManager instance;

    private final Context context;
    private final SharedPreferences prefs;

    public static WorkspaceRootManager getInstance(Context context) {
        if (instance == null) {
            synchronized (WorkspaceRootManager.class) {
                if (instance == null) {
                    Context appCtx = context != null ? context.getApplicationContext() : null;
                    instance = new WorkspaceRootManager(appCtx);
                }
            }
        }
        return instance;
    }

    public WorkspaceRootManager(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        if (this.context != null) {
            this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        } else {
            this.prefs = null;
        }
    }

    // =========================================================================
    // Root Resolution & Custom Selection
    // =========================================================================

    /**
     * Resolves the active Antigravity workspace root.
     * Order of precedence:
     * 1. User-configured custom root (persisted via SAF or settings).
     * 2. Primary external storage (/storage/emulated/0/Antigravity).
     * 3. App-specific external storage fallback (context.getExternalFilesDir(null)/Antigravity).
     */
    public File getRootDir() {
        // 1. Check custom configured root
        String customPath = getCustomRootPath();
        if (customPath != null && !customPath.trim().isEmpty()) {
            File customDir = new File(customPath.trim());
            try {
                if (customDir.isDirectory() || customDir.mkdirs()) {
                    return customDir;
                }
                Log.w(TAG, "Custom root at " + customPath + " is inaccessible; falling back to default.");
            } catch (Throwable t) {
                Log.w(TAG, "Error accessing custom root " + customPath + ": " + t.getMessage());
            }
        }

        // 2. Default primary external storage root
        File primary = new File(Environment.getExternalStorageDirectory(), DEFAULT_ROOT_DIR_NAME);
        try {
            if (primary.isDirectory() || primary.mkdirs()) {
                return primary;
            }
        } catch (Throwable t) {
            Log.w(TAG, "Cannot create primary root at " + primary.getAbsolutePath() + ": " + t.getMessage());
        }

        // 3. App-specific external files fallback
        if (context != null) {
            try {
                File fallbackBase = context.getExternalFilesDir(null);
                if (fallbackBase != null) {
                    File fallback = new File(fallbackBase, DEFAULT_ROOT_DIR_NAME);
                    if (fallback.isDirectory() || fallback.mkdirs()) {
                        Log.i(TAG, "Using scoped storage fallback root: " + fallback.getAbsolutePath());
                        return fallback;
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "Error accessing scoped fallback root: " + t.getMessage());
            }
        }

        return primary;
    }

    /** Returns the custom root path if configured and persisted, or null. */
    public String getCustomRootPath() {
        if (prefs == null) return null;
        return prefs.getString(KEY_CUSTOM_ROOT_PATH, null);
    }

    /** Returns the custom root SAF tree URI string if configured, or null. */
    public String getCustomRootSafUri() {
        if (prefs == null) return null;
        return prefs.getString(KEY_CUSTOM_ROOT_SAF_URI, null);
    }

    /** Whether the user is currently operating with a custom workspace root. */
    public boolean isCustomRoot() {
        String custom = getCustomRootPath();
        return custom != null && !custom.trim().isEmpty();
    }

    /**
     * Sets and persists a custom root path and optional SAF tree URI.
     * Automatically initializes the hierarchy in the new location.
     */
    public synchronized boolean setCustomRoot(String path, String safTreeUri) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        File dir = new File(path.trim());
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Cannot create custom root directory: " + path);
            return false;
        }

        if (prefs != null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_CUSTOM_ROOT_PATH, dir.getAbsolutePath());
            if (safTreeUri != null) {
                editor.putString(KEY_CUSTOM_ROOT_SAF_URI, safTreeUri);
            }
            editor.apply();
        }

        Log.i(TAG, "Custom workspace root set and persisted: " + dir.getAbsolutePath());
        return initializeHierarchy();
    }

    /** Resets the workspace root back to the default (/storage/emulated/0/Antigravity). */
    public synchronized boolean resetToDefaultRoot() {
        if (prefs != null) {
            prefs.edit()
                    .remove(KEY_CUSTOM_ROOT_PATH)
                    .remove(KEY_CUSTOM_ROOT_SAF_URI)
                    .apply();
        }
        Log.i(TAG, "Workspace root reset to default.");
        return initializeHierarchy();
    }

    // =========================================================================
    // Centralized Path Resolution (All Hierarchy Directories)
    // =========================================================================

    /** Default location for newly created projects and Git repositories. */
    public File getProjectsDir() {
        return new File(getRootDir(), DIR_PROJECTS);
    }

    /** Root directory for workspace intelligence, docs, and manifests. */
    public File getWorkspacesDir() {
        return new File(getRootDir(), DIR_WORKSPACES);
    }

    /** Initial default workspace location. */
    public File getDefaultWorkspaceDir() {
        return new File(getWorkspacesDir(), SUB_DEFAULT_WORKSPACE);
    }

    /** System directory: config, profiles, and models. */
    public File getSystemDir() {
        return new File(getRootDir(), DIR_SYSTEM);
    }

    public File getSystemConfigDir() {
        return new File(getSystemDir(), SUB_SYSTEM_CONFIG);
    }

    public File getSystemProfilesDir() {
        return new File(getSystemDir(), SUB_SYSTEM_PROFILES);
    }

    public File getSystemModelsDir() {
        return new File(getSystemDir(), SUB_SYSTEM_MODELS);
    }

    /** Agents directory: prompts, instructions, and tools. */
    public File getAgentsDir() {
        return new File(getRootDir(), DIR_AGENTS);
    }

    public File getAgentPromptsDir() {
        return new File(getAgentsDir(), SUB_AGENTS_PROMPTS);
    }

    public File getAgentInstructionsDir() {
        return new File(getAgentsDir(), SUB_AGENTS_INSTRUCTIONS);
    }

    public File getAgentToolsDir() {
        return new File(getAgentsDir(), SUB_AGENTS_TOOLS);
    }

    /** Knowledge directory: memory, references, and documentation. */
    public File getKnowledgeDir() {
        return new File(getRootDir(), DIR_KNOWLEDGE);
    }

    public File getKnowledgeMemoryDir() {
        return new File(getKnowledgeDir(), SUB_KNOWLEDGE_MEMORY);
    }

    public File getKnowledgeReferencesDir() {
        return new File(getKnowledgeDir(), SUB_KNOWLEDGE_REFERENCES);
    }

    public File getKnowledgeDocumentationDir() {
        return new File(getKnowledgeDir(), SUB_KNOWLEDGE_DOCUMENTATION);
    }

    /** Centralized runtime log directory (strictly decoupled from project repositories). */
    public File getLogsDir() {
        return new File(getRootDir(), DIR_LOGS);
    }

    /** Centralized cache directory (strictly decoupled from project repositories). */
    public File getCacheDir() {
        return new File(getRootDir(), DIR_CACHE);
    }

    /** Snapshots and backups directory. */
    public File getBackupsDir() {
        return new File(getRootDir(), DIR_BACKUPS);
    }

    /** User exports destination. */
    public File getExportsDir() {
        return new File(getRootDir(), DIR_EXPORTS);
    }

    // =========================================================================
    // Child Directory & Project Resolution Helpers
    // =========================================================================

    /** Resolves a specific project source directory under Projects/. */
    public File resolveProjectDir(String projectSlug) {
        if (projectSlug == null || projectSlug.trim().isEmpty()) {
            return getProjectsDir();
        }
        return new File(getProjectsDir(), sanitizeName(projectSlug));
    }

    /** Resolves a specific workspace documentation directory under Workspaces/. */
    public File resolveWorkspaceDir(String workspaceSlug) {
        if (workspaceSlug == null || workspaceSlug.trim().isEmpty()) {
            return getDefaultWorkspaceDir();
        }
        return new File(getWorkspacesDir(), sanitizeName(workspaceSlug));
    }

    /** Resolves a relative path safely within the workspace root, preventing traversal. */
    public File resolveRelativePath(String relPath) {
        if (relPath == null || relPath.trim().isEmpty()) {
            return getRootDir();
        }
        String clean = relPath.replace('\\', '/').trim();
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        // Disallow path traversal
        if (clean.contains("..")) {
            throw new IllegalArgumentException("Path traversal not allowed: " + relPath);
        }
        return new File(getRootDir(), clean);
    }

    private static String sanitizeName(String name) {
        return name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    // =========================================================================
    // Hierarchy Initialization
    // =========================================================================

    /**
     * Initializes all missing directories for the complete IDE-grade hierarchy.
     * Idempotent and non-destructive: never overwrites existing user files.
     */
    public synchronized boolean initializeHierarchy() {
        try {
            File root = getRootDir();
            if (!ensureDir(root)) {
                Log.w(TAG, "Failed to create root directory at: " + root.getAbsolutePath());
                return false;
            }

            // 1. Projects/
            ensureDir(getProjectsDir());

            // 2. Workspaces/ and Workspaces/Default/
            ensureDir(getWorkspacesDir());
            ensureDir(getDefaultWorkspaceDir());

            // 3. System/ (config/, profiles/, models/)
            ensureDir(getSystemDir());
            ensureDir(getSystemConfigDir());
            ensureDir(getSystemProfilesDir());
            ensureDir(getSystemModelsDir());

            // 4. Agents/ (prompts/, instructions/, tools/)
            ensureDir(getAgentsDir());
            ensureDir(getAgentPromptsDir());
            ensureDir(getAgentInstructionsDir());
            ensureDir(getAgentToolsDir());

            // 5. Knowledge/ (memory/, references/, documentation/)
            ensureDir(getKnowledgeDir());
            ensureDir(getKnowledgeMemoryDir());
            ensureDir(getKnowledgeReferencesDir());
            ensureDir(getKnowledgeDocumentationDir());

            // 6. Logs/ (with .nomedia to protect media indexer)
            File logsDir = getLogsDir();
            if (ensureDir(logsDir)) {
                createNoMedia(logsDir);
            }

            // 7. Cache/ (with .nomedia to protect media indexer)
            File cacheDir = getCacheDir();
            if (ensureDir(cacheDir)) {
                createNoMedia(cacheDir);
            }

            // 8. Backups/
            ensureDir(getBackupsDir());

            // 9. Exports/
            ensureDir(getExportsDir());

            Log.i(TAG, "Centralized Antigravity workspace root initialized at: " + root.getAbsolutePath());
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Error initializing workspace hierarchy", t);
            return false;
        }
    }

    private static boolean ensureDir(File dir) {
        if (dir == null) return false;
        if (dir.isDirectory()) return true;
        return dir.mkdirs();
    }

    private static void createNoMedia(File dir) {
        try {
            File noMedia = new File(dir, ".nomedia");
            if (!noMedia.exists()) {
                noMedia.createNewFile();
            }
        } catch (Throwable ignored) {}
    }

    // =========================================================================
    // Legacy Directory Detection & Safe Migration Engine
    // =========================================================================

    /**
     * Detects if the legacy directory (/storage/emulated/0/Projects) exists
     * and contains user projects.
     */
    public boolean hasLegacyProjects() {
        File legacyDir = new File(LEGACY_EXTERNAL_PROJECTS);
        if (!legacyDir.isDirectory()) return false;
        File[] files = legacyDir.listFiles();
        if (files == null || files.length == 0) return false;
        for (File f : files) {
            if (f.isDirectory() && !f.getName().startsWith(".")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lists existing projects found in the legacy directory.
     * Returns a list of maps containing project metadata (name, path, hasGit, fileCount, sizeBytes).
     */
    public List<Map<String, Object>> detectLegacyProjects() {
        List<Map<String, Object>> result = new ArrayList<>();
        File legacyDir = new File(LEGACY_EXTERNAL_PROJECTS);
        if (!legacyDir.isDirectory()) return result;

        File[] files = legacyDir.listFiles();
        if (files == null) return result;

        for (File f : files) {
            if (f.isDirectory() && !f.getName().startsWith(".")) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("name", f.getName());
                meta.put("path", f.getAbsolutePath());
                File gitDir = new File(f, ".git");
                meta.put("isGitRepo", gitDir.isDirectory());
                meta.put("lastModified", f.lastModified());
                meta.put("fileCount", countFiles(f));
                result.add(meta);
            }
        }
        return result;
    }

    /**
     * Safe Migration Operation Result.
     */
    public static final class MigrationResult {
        public final boolean success;
        public final String projectName;
        public final String sourcePath;
        public final String targetPath;
        public final int filesCopied;
        public final String error;
        public final boolean rolledBack;

        public MigrationResult(boolean success, String projectName, String sourcePath,
                               String targetPath, int filesCopied, String error, boolean rolledBack) {
            this.success = success;
            this.projectName = projectName;
            this.sourcePath = sourcePath;
            this.targetPath = targetPath;
            this.filesCopied = filesCopied;
            this.error = error;
            this.rolledBack = rolledBack;
        }
    }

    /**
     * Safely migrates a legacy project from /storage/emulated/0/Projects/<name>
     * to the new centralized Projects/ directory.
     *
     * Invariants:
     * - Never deletes or overwrites existing user data.
     * - Preserves Git metadata (.git), hidden files, and timestamps.
     * - Verifies file integrity before marking successful.
     * - Performs clean rollback if any step fails.
     *
     * @param projectName Name of the project folder in legacy directory.
     * @param preserveLegacy If true, keeps original project untouched in legacy folder.
     */
    public synchronized MigrationResult migrateLegacyProject(String projectName, boolean preserveLegacy) {
        if (projectName == null || projectName.trim().isEmpty()) {
            return new MigrationResult(false, projectName, null, null, 0, "Invalid project name", false);
        }

        File legacySource = new File(LEGACY_EXTERNAL_PROJECTS, projectName.trim());
        if (!legacySource.isDirectory() || !legacySource.canRead()) {
            return new MigrationResult(false, projectName, legacySource.getAbsolutePath(), null, 0,
                    "Legacy project source not accessible or not a directory", false);
        }

        File targetProjectsDir = getProjectsDir();
        if (!targetProjectsDir.isDirectory() && !targetProjectsDir.mkdirs()) {
            return new MigrationResult(false, projectName, legacySource.getAbsolutePath(),
                    targetProjectsDir.getAbsolutePath(), 0, "Cannot create target Projects directory", false);
        }

        File targetProject = new File(targetProjectsDir, projectName.trim());
        if (targetProject.exists()) {
            return new MigrationResult(false, projectName, legacySource.getAbsolutePath(),
                    targetProject.getAbsolutePath(), 0, "Target project directory already exists; refusing to overwrite", false);
        }

        int sourceCount = countFiles(legacySource);
        Log.i(TAG, "Starting safe migration of " + projectName + " (" + sourceCount + " files)...");

        boolean copySuccess = false;
        try {
            copyDirectoryRecursive(legacySource, targetProject);
            copySuccess = true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed during copy of legacy project " + projectName + ": " + t.getMessage(), t);
        }

        if (!copySuccess) {
            // Rollback target directory
            deleteRecursive(targetProject);
            return new MigrationResult(false, projectName, legacySource.getAbsolutePath(),
                    targetProject.getAbsolutePath(), 0, "Copy failed during execution", true);
        }

        // Integrity Verification
        int targetCount = countFiles(targetProject);
        boolean gitVerified = true;
        File sourceGit = new File(legacySource, ".git");
        if (sourceGit.isDirectory()) {
            File targetGit = new File(targetProject, ".git");
            gitVerified = targetGit.isDirectory() && targetGit.list() != null && targetGit.list().length > 0;
        }

        if (sourceCount != targetCount || !gitVerified) {
            Log.e(TAG, "Integrity verification failed for " + projectName + ": source=" + sourceCount
                    + " files, target=" + targetCount + " files, gitVerified=" + gitVerified + ". Rolling back.");
            deleteRecursive(targetProject);
            return new MigrationResult(false, projectName, legacySource.getAbsolutePath(),
                    targetProject.getAbsolutePath(), 0, "Integrity verification failed (file count mismatch)", true);
        }

        Log.i(TAG, "Successfully migrated " + projectName + " (" + targetCount + " files verified).");

        // If not preserving legacy, rename legacy to .migrated backup rather than deleting
        if (!preserveLegacy) {
            try {
                File backupDir = new File(getBackupsDir(), "legacy_migration_" + System.currentTimeMillis());
                backupDir.mkdirs();
                File movedBackup = new File(backupDir, projectName);
                legacySource.renameTo(movedBackup);
            } catch (Throwable t) {
                Log.w(TAG, "Could not archive legacy source: " + t.getMessage());
            }
        }

        return new MigrationResult(true, projectName, legacySource.getAbsolutePath(),
                targetProject.getAbsolutePath(), targetCount, null, false);
    }

    /**
     * Recursively copies a directory tree, preserving hidden files, .git, and timestamps.
     */
    public static void copyDirectoryRecursive(File source, File destination) throws IOException {
        if (!source.exists()) {
            throw new IOException("Source does not exist: " + source);
        }
        if (source.isDirectory()) {
            if (!destination.exists() && !destination.mkdirs()) {
                throw new IOException("Cannot create destination directory: " + destination);
            }
            String[] children = source.list();
            if (children != null) {
                for (String child : children) {
                    copyDirectoryRecursive(new File(source, child), new File(destination, child));
                }
            }
            destination.setLastModified(source.lastModified());
        } else {
            File parent = destination.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Cannot create destination parent directory: " + parent);
            }
            copyFile(source, destination);
            destination.setLastModified(source.lastModified());
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buf = new byte[65536];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
        } finally {
            if (in != null) try { in.close(); } catch (Throwable ignored) {}
            if (out != null) try { out.close(); } catch (Throwable ignored) {}
        }
    }

    /** Recursively counts all files within a directory (including hidden files and .git). */
    public static int countFiles(File dir) {
        if (dir == null || !dir.exists()) return 0;
        if (!dir.isDirectory()) return 1;
        int count = 0;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                count += countFiles(child);
            }
        }
        return count;
    }

    /** Recursively deletes a directory tree (used for rollback). */
    public static boolean deleteRecursive(File fileOrDir) {
        if (fileOrDir == null || !fileOrDir.exists()) return true;
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDir.delete();
    }

    // =========================================================================
    // Information Map for Gateway & Diagnostic APIs
    // =========================================================================

    /**
     * Builds a comprehensive map of all resolved paths and configuration state.
     */
    public Map<String, Object> getWorkspaceInfoMap() {
        Map<String, Object> map = new HashMap<>();
        File root = getRootDir();
        map.put("rootDir", root.getAbsolutePath());
        map.put("defaultRoot", DEFAULT_EXTERNAL_ROOT);
        map.put("isCustomRoot", isCustomRoot());
        map.put("customRootPath", getCustomRootPath());
        map.put("customRootSafUri", getCustomRootSafUri());

        Map<String, String> paths = new HashMap<>();
        paths.put("projects", getProjectsDir().getAbsolutePath());
        paths.put("workspaces", getWorkspacesDir().getAbsolutePath());
        paths.put("defaultWorkspace", getDefaultWorkspaceDir().getAbsolutePath());
        paths.put("system", getSystemDir().getAbsolutePath());
        paths.put("systemConfig", getSystemConfigDir().getAbsolutePath());
        paths.put("systemProfiles", getSystemProfilesDir().getAbsolutePath());
        paths.put("systemModels", getSystemModelsDir().getAbsolutePath());
        paths.put("agents", getAgentsDir().getAbsolutePath());
        paths.put("agentsPrompts", getAgentPromptsDir().getAbsolutePath());
        paths.put("agentsInstructions", getAgentInstructionsDir().getAbsolutePath());
        paths.put("agentsTools", getAgentToolsDir().getAbsolutePath());
        paths.put("knowledge", getKnowledgeDir().getAbsolutePath());
        paths.put("knowledgeMemory", getKnowledgeMemoryDir().getAbsolutePath());
        paths.put("knowledgeReferences", getKnowledgeReferencesDir().getAbsolutePath());
        paths.put("knowledgeDocumentation", getKnowledgeDocumentationDir().getAbsolutePath());
        paths.put("logs", getLogsDir().getAbsolutePath());
        paths.put("cache", getCacheDir().getAbsolutePath());
        paths.put("backups", getBackupsDir().getAbsolutePath());
        paths.put("exports", getExportsDir().getAbsolutePath());
        map.put("directories", paths);

        map.put("legacyDetected", hasLegacyProjects());
        map.put("legacyProjectsPath", LEGACY_EXTERNAL_PROJECTS);
        return map;
    }
}
