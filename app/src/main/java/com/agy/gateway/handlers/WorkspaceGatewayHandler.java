package com.agy.gateway.handlers;

import android.content.Context;
import android.util.Log;

import com.agy.workspace.db.ProjectRegistryRepository;
import com.agy.workspace.generator.WorkspaceDocumentGenerator;
import com.agy.workspace.model.ProjectRecord;
import com.agy.workspace.storage.WorkspaceStorageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Handles workspace and project lifecycle requests received by SystemGatewayServer.
 */
public final class WorkspaceGatewayHandler {

    private static final String TAG = "WorkspaceGateway";

    private final Context context;
    private final WorkspaceStorageManager storageManager;
    private final ProjectRegistryRepository repository;
    private final WorkspaceDocumentGenerator documentGenerator;

    public WorkspaceGatewayHandler(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.storageManager = new WorkspaceStorageManager(context);
        this.repository = new ProjectRegistryRepository(context);
        this.documentGenerator = new WorkspaceDocumentGenerator(context);
    }

    public JSONObject handle(String command, JSONObject payload) {
        if (payload == null) {
            payload = new JSONObject();
        }

        switch (command) {
            case "workspace_list":
                return handleList(payload);
            case "workspace_quick_create":
                return handleQuickCreate(payload);
            case "workspace_import":
                return handleImport(payload);
            case "workspace_reconnect":
                return handleReconnect(payload);
            case "workspace_archive":
                return handleArchive(payload);
            case "workspace_restore":
                return handleRestore(payload);
            case "workspace_remove":
                return handleRemove(payload);
            case "workspace_verify":
                return handleVerify();
            default:
                JSONObject err = new JSONObject();
                try {
                    err.put("ok", false);
                    err.put("error", "unknown workspace command: " + command);
                } catch (Exception ignored) {}
                return err;
        }
    }

    private JSONObject handleList(JSONObject payload) {
        JSONObject resp = new JSONObject();
        try {
            boolean includeArchived = payload.optBoolean("includeArchived", true);
            List<ProjectRecord> list = repository.listProjects(includeArchived);

            JSONArray arr = new JSONArray();
            for (ProjectRecord p : list) {
                // Check live accessibility of source folder
                File src = new File(p.getSourcePath());
                boolean accessible = src.isDirectory() && src.canRead();
                JSONObject obj = p.toJson();
                obj.put("sourceAccessible", accessible);
                arr.put(obj);
            }

            resp.put("ok", true);
            resp.put("projects", arr);

            JSONObject paths = new JSONObject();
            paths.put("root", storageManager.getRootDir().getAbsolutePath());
            paths.put("workspace", storageManager.getWorkspaceDir().getAbsolutePath());
            paths.put("projects", storageManager.getProjectsDir().getAbsolutePath());
            paths.put("templates", storageManager.getTemplatesDir().getAbsolutePath());
            paths.put("exports", storageManager.getExportsDir().getAbsolutePath());
            paths.put("shared", storageManager.getSharedDir().getAbsolutePath());
            resp.put("storagePaths", paths);

        } catch (Throwable t) {
            try {
                resp.put("ok", false);
                resp.put("error", t.getMessage());
            } catch (Exception ignored) {}
        }
        return resp;
    }

    private JSONObject handleQuickCreate(JSONObject payload) {
        JSONObject resp = new JSONObject();
        try {
            String name = payload.optString("name", "").trim();
            if (name.isEmpty()) {
                resp.put("ok", false);
                resp.put("error", "Project name is required");
                return resp;
            }

            String slug = generateSlug(name);
            ProjectRecord existing = repository.getProjectBySlug(slug);
            if (existing != null) {
                slug = slug + "-" + (System.currentTimeMillis() % 1000);
            }

            storageManager.initializeHierarchy();

            File sourceDir = storageManager.getProjectSourceDir(slug);
            if (!sourceDir.isDirectory() && !sourceDir.mkdirs()) {
                resp.put("ok", false);
                resp.put("error", "Could not create source directory: " + sourceDir.getAbsolutePath());
                return resp;
            }

            File wsDir = storageManager.getProjectWorkspaceDir(slug);
            if (!wsDir.isDirectory() && !wsDir.mkdirs()) {
                resp.put("ok", false);
                resp.put("error", "Could not create workspace directory: " + wsDir.getAbsolutePath());
                return resp;
            }

            ProjectRecord p = new ProjectRecord();
            p.setId(UUID.randomUUID().toString());
            p.setName(name);
            p.setSlug(slug);
            p.setDescription(payload.optString("description", ""));
            p.setProjectType(payload.optString("projectType", ProjectRecord.TYPE_GENERIC));
            p.setStatus(ProjectRecord.STATUS_ACTIVE);
            p.setSourceType(ProjectRecord.SOURCE_TYPE_DEFAULT);
            p.setSourcePath(sourceDir.getAbsolutePath());
            p.setSourceUri("file://" + sourceDir.getAbsolutePath());
            p.setWorkspacePath(wsDir.getAbsolutePath());

            // Initialize workspace docs and synchronize with language server
            documentGenerator.initializeWorkspace(p);
            repository.insertProject(p);

            resp.put("ok", true);
            resp.put("project", p.toJson());

        } catch (Throwable t) {
            try {
                resp.put("ok", false);
                resp.put("error", t.getMessage());
            } catch (Exception ignored) {}
        }
        return resp;
    }

    private JSONObject handleImport(JSONObject payload) {
        JSONObject resp = new JSONObject();
        try {
            String sourcePath = payload.optString("sourcePath", "").trim();
            if (sourcePath.isEmpty()) {
                resp.put("ok", false);
                resp.put("error", "sourcePath is required");
                return resp;
            }

            File sourceDir = new File(sourcePath);
            if (!sourceDir.isDirectory()) {
                resp.put("ok", false);
                resp.put("error", "Source path does not exist or is not a directory: " + sourcePath);
                return resp;
            }

            // Existing check
            ProjectRecord existing = repository.getProjectBySourcePath(sourceDir.getAbsolutePath());
            if (existing != null) {
                resp.put("ok", true);
                resp.put("alreadyRegistered", true);
                resp.put("project", existing.toJson());
                return resp;
            }

            String name = payload.optString("name", "").trim();
            if (name.isEmpty()) {
                name = sourceDir.getName();
            }

            String slug = generateSlug(name);
            ProjectRecord existingSlug = repository.getProjectBySlug(slug);
            if (existingSlug != null) {
                slug = slug + "-" + (System.currentTimeMillis() % 1000);
            }

            storageManager.initializeHierarchy();

            File wsDir = storageManager.getProjectWorkspaceDir(slug);
            if (!wsDir.isDirectory() && !wsDir.mkdirs()) {
                resp.put("ok", false);
                resp.put("error", "Could not create workspace directory: " + wsDir.getAbsolutePath());
                return resp;
            }

            ProjectRecord p = new ProjectRecord();
            p.setId(UUID.randomUUID().toString());
            p.setName(name);
            p.setSlug(slug);
            p.setDescription(payload.optString("description", "Imported existing project"));
            p.setProjectType(payload.optString("projectType", ProjectRecord.TYPE_GENERIC));
            p.setStatus(ProjectRecord.STATUS_ACTIVE);
            p.setSourceType(ProjectRecord.SOURCE_TYPE_CUSTOM);
            p.setSourcePath(sourceDir.getAbsolutePath());
            p.setSourceUri("file://" + sourceDir.getAbsolutePath());
            p.setSafTreeUri(payload.optString("safTreeUri", null));
            p.setWorkspacePath(wsDir.getAbsolutePath());

            // Detect Git
            File gitDir = new File(sourceDir, ".git");
            if (gitDir.isDirectory()) {
                p.setVcsType("git");
            }

            // Initialize workspace docs and synchronize with language server
            // NOTE: Source repository is untouched!
            documentGenerator.initializeWorkspace(p);
            repository.insertProject(p);

            resp.put("ok", true);
            resp.put("project", p.toJson());

        } catch (Throwable t) {
            try {
                resp.put("ok", false);
                resp.put("error", t.getMessage());
            } catch (Exception ignored) {}
        }
        return resp;
    }

    private JSONObject handleReconnect(JSONObject payload) {
        JSONObject resp = new JSONObject();
        try {
            String id = payload.optString("id", "").trim();
            String newSourcePath = payload.optString("newSourcePath", "").trim();
            String newSafTreeUri = payload.optString("newSafTreeUri", null);

            if (id.isEmpty() || newSourcePath.isEmpty()) {
                resp.put("ok", false);
                resp.put("error", "Both id and newSourcePath are required");
                return resp;
            }

            File newSourceDir = new File(newSourcePath);
            if (!newSourceDir.isDirectory()) {
                resp.put("ok", false);
                resp.put("error", "New source path is not an accessible directory: " + newSourcePath);
                return resp;
            }

            ProjectRecord p = repository.getProjectById(id);
            if (p == null) {
                resp.put("ok", false);
                resp.put("error", "Project not found: " + id);
                return resp;
            }

            p.setSourcePath(newSourceDir.getAbsolutePath());
            p.setSourceUri("file://" + newSourceDir.getAbsolutePath());
            if (newSafTreeUri != null) {
                p.setSafTreeUri(newSafTreeUri);
            }
            p.setStatus(ProjectRecord.STATUS_ACTIVE);

            repository.updateProjectSource(id, p.getSourcePath(), p.getSourceUri(), p.getSafTreeUri());
            documentGenerator.initializeWorkspace(p);

            resp.put("ok", true);
            resp.put("project", p.toJson());

        } catch (Throwable t) {
            try {
                resp.put("ok", false);
                resp.put("error", t.getMessage());
            } catch (Exception ignored) {}
        }
        return resp;
    }

    private JSONObject handleArchive(JSONObject payload) {
        JSONObject resp = new JSONObject();
        try {
            String id = payload.optString("id", "").trim();
            ProjectRecord p = repository.getProjectById(id);
            if (p == null) {
                resp.put("ok", false);
                resp.put("error", "Project not found: " + id);
                return resp;
            }

            repository.updateProjectStatus(id, ProjectRecord.STATUS_ARCHIVED);
            p.setStatus(ProjectRecord.STATUS_ARCHIVED);
            documentGenerator.syncLanguageServerProjectStore(p);

            resp.put("ok", true);
            resp.put("project", p.toJson());
        } catch (Throwable t) {
            try {
                resp.put("ok", false);
                resp.put("error", t.getMessage());
            } catch (Exception ignored) {}
        }
        return resp;
    }

    private JSONObject handleRestore(JSONObject payload) {
        JSONObject resp = new JSONObject();
        try {
            String id = payload.optString("id", "").trim();
            ProjectRecord p = repository.getProjectById(id);
            if (p == null) {
                resp.put("ok", false);
                resp.put("error", "Project not found: " + id);
                return resp;
            }

            repository.updateProjectStatus(id, ProjectRecord.STATUS_ACTIVE);
            p.setStatus(ProjectRecord.STATUS_ACTIVE);
            documentGenerator.syncLanguageServerProjectStore(p);

            resp.put("ok", true);
            resp.put("project", p.toJson());
        } catch (Throwable t) {
            try {
                resp.put("ok", false);
                resp.put("error", t.getMessage());
            } catch (Exception ignored) {}
        }
        return resp;
    }

    private JSONObject handleRemove(JSONObject payload) {
        JSONObject resp = new JSONObject();
        try {
            String id = payload.optString("id", "").trim();
            boolean keepWorkspace = payload.optBoolean("keepWorkspace", true);

            ProjectRecord p = repository.getProjectById(id);
            if (p == null) {
                resp.put("ok", false);
                resp.put("error", "Project not found: " + id);
                return resp;
            }

            // 1. Remove from database
            repository.deleteProject(id);

            // 2. Remove language server JSON file
            documentGenerator.removeLanguageServerProjectStore(id);

            // 3. Clean up workspace docs only if requested
            if (!keepWorkspace && p.getWorkspacePath() != null) {
                File ws = new File(p.getWorkspacePath());
                if (ws.isDirectory()) {
                    deleteRecursive(ws);
                }
            }

            // NOTE: Source repository is STRICTLY UNTOUCHED!
            resp.put("ok", true);
            resp.put("removedId", id);
            resp.put("sourceCodeUntouched", p.getSourcePath());

        } catch (Throwable t) {
            try {
                resp.put("ok", false);
                resp.put("error", t.getMessage());
            } catch (Exception ignored) {}
        }
        return resp;
    }

    private JSONObject handleVerify() {
        JSONObject resp = new JSONObject();
        try {
            List<ProjectRecord> list = repository.listProjects(true);
            int activeCount = 0;
            int disconnectedCount = 0;

            for (ProjectRecord p : list) {
                File src = new File(p.getSourcePath());
                boolean accessible = src.isDirectory() && src.canRead();
                if (!accessible && ProjectRecord.STATUS_ACTIVE.equals(p.getStatus())) {
                    repository.updateProjectStatus(p.getId(), ProjectRecord.STATUS_DISCONNECTED);
                    disconnectedCount++;
                } else if (accessible && ProjectRecord.STATUS_DISCONNECTED.equals(p.getStatus())) {
                    repository.updateProjectStatus(p.getId(), ProjectRecord.STATUS_ACTIVE);
                    activeCount++;
                } else if (accessible) {
                    activeCount++;
                }
            }

            resp.put("ok", true);
            resp.put("totalChecked", list.size());
            resp.put("activeCount", activeCount);
            resp.put("disconnectedCount", disconnectedCount);

        } catch (Throwable t) {
            try {
                resp.put("ok", false);
                resp.put("error", t.getMessage());
            } catch (Exception ignored) {}
        }
        return resp;
    }

    private static String generateSlug(String name) {
        if (name == null) return "project";
        String slug = name.trim().toLowerCase().replaceAll("[^a-z0-9_\\-]+", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "project" : slug;
    }

    private static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}
