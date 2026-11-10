package com.agy.workspace.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Represents a registered project in Antigravity Centralized Workspace.
 */
public final class ProjectRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final String STATUS_DISCONNECTED = "DISCONNECTED";

    public static final String SOURCE_TYPE_DEFAULT = "DEFAULT";
    public static final String SOURCE_TYPE_CUSTOM = "CUSTOM";

    public static final String TYPE_GENERIC = "GENERIC";
    public static final String TYPE_ANDROID = "ANDROID";
    public static final String TYPE_WEB = "WEB";
    public static final String TYPE_BACKEND = "BACKEND";
    public static final String TYPE_FULLSTACK = "FULLSTACK";
    public static final String TYPE_LIBRARY = "LIBRARY";
    public static final String TYPE_RESEARCH = "RESEARCH";

    private String id;
    private String name;
    private String slug;
    private String description;
    private String projectType;
    private String status;
    private String sourceType;
    private String sourceUri;
    private String sourcePath;
    private String workspacePath;
    private String safTreeUri;
    private int schemaVersion;
    private String vcsType;
    private String defaultBranch;
    private String metadataJson;
    private long createdAt;
    private long updatedAt;
    private long lastAccessedAt;

    public ProjectRecord() {
        this.status = STATUS_ACTIVE;
        this.sourceType = SOURCE_TYPE_DEFAULT;
        this.projectType = TYPE_GENERIC;
        this.schemaVersion = 1;
        this.vcsType = "none";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.lastAccessedAt = this.createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceUri() { return sourceUri; }
    public void setSourceUri(String sourceUri) { this.sourceUri = sourceUri; }

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

    public String getWorkspacePath() { return workspacePath; }
    public void setWorkspacePath(String workspacePath) { this.workspacePath = workspacePath; }

    public String getSafTreeUri() { return safTreeUri; }
    public void setSafTreeUri(String safTreeUri) { this.safTreeUri = safTreeUri; }

    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }

    public String getVcsType() { return vcsType; }
    public void setVcsType(String vcsType) { this.vcsType = vcsType; }

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public long getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(long lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id != null ? id : "");
            o.put("name", name != null ? name : "");
            o.put("slug", slug != null ? slug : "");
            o.put("description", description != null ? description : "");
            o.put("projectType", projectType != null ? projectType : TYPE_GENERIC);
            o.put("status", status != null ? status : STATUS_ACTIVE);
            o.put("sourceType", sourceType != null ? sourceType : SOURCE_TYPE_DEFAULT);
            o.put("sourceUri", sourceUri != null ? sourceUri : "");
            o.put("sourcePath", sourcePath != null ? sourcePath : "");
            o.put("workspacePath", workspacePath != null ? workspacePath : "");
            o.put("safTreeUri", safTreeUri != null ? safTreeUri : "");
            o.put("schemaVersion", schemaVersion);
            o.put("vcsType", vcsType != null ? vcsType : "none");
            o.put("defaultBranch", defaultBranch != null ? defaultBranch : "");
            o.put("metadataJson", metadataJson != null ? metadataJson : "");
            o.put("createdAt", createdAt);
            o.put("updatedAt", updatedAt);
            o.put("lastAccessedAt", lastAccessedAt);
        } catch (JSONException ignored) {}
        return o;
    }

    public static ProjectRecord fromJson(JSONObject o) {
        if (o == null) return null;
        ProjectRecord rec = new ProjectRecord();
        rec.setId(o.optString("id", null));
        rec.setName(o.optString("name", ""));
        rec.setSlug(o.optString("slug", ""));
        rec.setDescription(o.optString("description", ""));
        rec.setProjectType(o.optString("projectType", TYPE_GENERIC));
        rec.setStatus(o.optString("status", STATUS_ACTIVE));
        rec.setSourceType(o.optString("sourceType", SOURCE_TYPE_DEFAULT));
        rec.setSourceUri(o.optString("sourceUri", ""));
        rec.setSourcePath(o.optString("sourcePath", ""));
        rec.setWorkspacePath(o.optString("workspacePath", ""));
        rec.setSafTreeUri(o.optString("safTreeUri", null));
        rec.setSchemaVersion(o.optInt("schemaVersion", 1));
        rec.setVcsType(o.optString("vcsType", "none"));
        rec.setDefaultBranch(o.optString("defaultBranch", null));
        rec.setMetadataJson(o.optString("metadataJson", null));
        rec.setCreatedAt(o.optLong("createdAt", System.currentTimeMillis()));
        rec.setUpdatedAt(o.optLong("updatedAt", rec.getCreatedAt()));
        rec.setLastAccessedAt(o.optLong("lastAccessedAt", rec.getCreatedAt()));
        return rec;
    }
}
