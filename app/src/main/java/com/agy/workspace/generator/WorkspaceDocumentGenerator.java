package com.agy.workspace.generator;

import android.content.Context;
import android.util.Log;

import com.agy.util.AtomicFileWriter;
import com.agy.workspace.model.ProjectRecord;
import com.agy.workspace.storage.WorkspaceStorageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Generates and initializes documentation and configuration files for a centralized workspace.
 */
public final class WorkspaceDocumentGenerator {

    private static final String TAG = "WorkspaceDocGen";

    private final Context context;
    private final WorkspaceStorageManager storageManager;

    public WorkspaceDocumentGenerator(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.storageManager = new WorkspaceStorageManager(context);
    }

    /**
     * Creates the complete centralized workspace folder hierarchy and initial documents.
     */
    public boolean initializeWorkspace(ProjectRecord p) {
        if (p == null || p.getSlug() == null || p.getSlug().isEmpty()) {
            return false;
        }

        File wsDir = storageManager.getProjectWorkspaceDir(p.getSlug());
        if (!wsDir.isDirectory() && !wsDir.mkdirs()) {
            Log.w(TAG, "Cannot create workspace directory: " + wsDir.getAbsolutePath());
            return false;
        }

        p.setWorkspacePath(wsDir.getAbsolutePath());

        // Create standard categories
        ensureDir(new File(wsDir, "planning"));
        ensureDir(new File(wsDir, "architecture"));
        ensureDir(new File(wsDir, "research"));
        ensureDir(new File(wsDir, "reports"));
        ensureDir(new File(wsDir, "collaboration"));
        ensureDir(new File(wsDir, "sessions"));

        // 1. project.yaml
        File yamlFile = new File(wsDir, "project.yaml");
        try {
            AtomicFileWriter.writeText(yamlFile, ProjectYamlParser.toYaml(p));
        } catch (Throwable t) {
            Log.w(TAG, "Failed writing project.yaml", t);
        }

        // 2. PROJECT_CONTEXT.md
        File ctxFile = new File(wsDir, "PROJECT_CONTEXT.md");
        if (!ctxFile.exists()) {
            try {
                AtomicFileWriter.writeText(ctxFile, generateProjectContext(p));
            } catch (Throwable t) {
                Log.w(TAG, "Failed writing PROJECT_CONTEXT.md", t);
            }
        }

        // 3. TASKS.md
        File tasksFile = new File(wsDir, "TASKS.md");
        if (!tasksFile.exists()) {
            try {
                AtomicFileWriter.writeText(tasksFile, generateTasks(p));
            } catch (Throwable t) {
                Log.w(TAG, "Failed writing TASKS.md", t);
            }
        }

        // 4. Synchronize Language Server JSON store in ~/.gemini/config/projects/<UUID>.json
        syncLanguageServerProjectStore(p);

        Log.i(TAG, "Initialized workspace for " + p.getName() + " at " + wsDir.getAbsolutePath());
        return true;
    }

    /**
     * Synchronizes the project definition with the Go language server core
     * at $HOME/.gemini/config/projects/<id>.json.
     */
    public boolean syncLanguageServerProjectStore(ProjectRecord p) {
        if (p == null || p.getId() == null || context == null) return false;
        try {
            File filesDir = context.getFilesDir();
            File geminiConfigProjects = new File(filesDir, ".gemini/config/projects");
            if (!geminiConfigProjects.isDirectory() && !geminiConfigProjects.mkdirs()) {
                return false;
            }

            File projectJson = new File(geminiConfigProjects, p.getId() + ".json");
            JSONObject root = new JSONObject();
            root.put("id", p.getId());
            root.put("name", p.getName() != null ? p.getName() : p.getSlug());

            JSONObject resObj = new JSONObject();
            JSONArray resArray = new JSONArray();

            File sourceDir = new File(p.getSourcePath());
            File gitDir = new File(sourceDir, ".git");
            boolean isGit = gitDir.isDirectory();

            JSONObject item = new JSONObject();
            String uri = "file://" + p.getSourcePath();

            if (isGit) {
                JSONObject gitFolder = new JSONObject();
                gitFolder.put("folderUri", uri);
                gitFolder.put("defaultBranch", p.getDefaultBranch() != null ? p.getDefaultBranch() : "main");
                item.put("gitFolder", gitFolder);
            } else {
                item.put("folderUri", uri);
            }
            resArray.put(item);
            resObj.put("resources", resArray);
            root.put("projectResources", resObj);

            root.put("settings", new JSONObject());
            root.put("isWorkspaceOnly", false);
            if (ProjectRecord.STATUS_ARCHIVED.equals(p.getStatus())) {
                root.put("archived", true);
            }

            AtomicFileWriter.writeText(projectJson, root.toString(2) + "\n");
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to sync language server project JSON for " + p.getId(), t);
            return false;
        }
    }

    /** Removes the project's JSON definition from the language server store. */
    public boolean removeLanguageServerProjectStore(String id) {
        if (id == null || context == null) return false;
        try {
            File filesDir = context.getFilesDir();
            File projectJson = new File(filesDir, ".gemini/config/projects/" + id + ".json");
            if (projectJson.isFile()) {
                return projectJson.delete();
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void ensureDir(File dir) {
        if (dir != null && !dir.isDirectory()) {
            dir.mkdirs();
        }
    }

    private static String generateProjectContext(ProjectRecord p) {
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append("# Project Context: ").append(p.getName()).append("\n\n");
        sb.append("> **Centralized Development Documentation & Knowledge Base**  \n");
        sb.append("> *Project ID:* `").append(p.getId()).append("`  \n");
        sb.append("> *Created:* ").append(dateStr).append("  \n");
        sb.append("> *Source Type:* `").append(p.getSourceType()).append("`  \n");
        sb.append("> *Source Path:* `").append(p.getSourcePath()).append("`  \n\n");

        sb.append("---\n\n");
        sb.append("## 1. Vision & Purpose\n\n");
        sb.append(p.getDescription() != null && !p.getDescription().isEmpty()
                ? p.getDescription() : "Centralized project intelligence and planning document.").append("\n\n");

        sb.append("## 2. Architecture & Tech Stack\n\n");
        sb.append("- **Category:** `").append(p.getProjectType()).append("`\n");
        sb.append("- **VCS:** `").append(p.getVcsType()).append("`\n");
        sb.append("- **Source Repository:** `").append(p.getSourcePath()).append("`\n");
        sb.append("- **Workspace Root:** `").append(p.getWorkspacePath()).append("`\n\n");

        sb.append("## 3. Directory Navigation\n\n");
        sb.append("- `planning/`: Sprint roadmaps, implementation specifications, milestones.\n");
        sb.append("- `architecture/`: High-level designs, database schemas, API contracts.\n");
        sb.append("- `research/`: Technology evaluations, spikes, benchmarks.\n");
        sb.append("- `reports/`: Audit logs, test results, performance profiling.\n");
        sb.append("- `collaboration/`: Agent guidelines, protocol handoffs, multi-agent notes.\n");
        sb.append("- `sessions/`: Chronological development session summaries.\n");

        return sb.toString();
    }

    private static String generateTasks(ProjectRecord p) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Tasks & Backlog: ").append(p.getName()).append("\n\n");
        sb.append("## Active Sprint / Next Up\n\n");
        sb.append("- [ ] Review `PROJECT_CONTEXT.md` and define project requirements\n");
        sb.append("- [ ] Verify build environment and run initial project tests\n");
        sb.append("- [ ] Establish architecture baseline in `architecture/`\n\n");

        sb.append("## Backlog\n\n");
        sb.append("- [ ] Feature implementation planning\n");
        sb.append("- [ ] Integration and test coverage\n\n");

        sb.append("## Completed\n\n");
        sb.append("- [x] Centralized Workspace initialized on Antigravity Android\n");

        return sb.toString();
    }
}
