package com.agy.workspace.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.agy.workspace.model.ProjectRecord;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe repository for querying and mutating the project registry.
 */
public final class ProjectRegistryRepository implements Closeable {

    private static final String TAG = "ProjectRegistryRepo";

    private final ProjectDbHelper dbHelper;

    public ProjectRegistryRepository(Context context) {
        this.dbHelper = new ProjectDbHelper(context);
    }

    public synchronized List<ProjectRecord> listProjects(boolean includeArchived) {
        List<ProjectRecord> list = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = dbHelper.getReadableDatabase();
            String selection = includeArchived ? null : "status != ?";
            String[] args = includeArchived ? null : new String[]{ProjectRecord.STATUS_ARCHIVED};
            c = db.query(ProjectDbHelper.TABLE_PROJECTS, null, selection, args, null, null, "last_accessed_at DESC");
            if (c != null && c.moveToFirst()) {
                do {
                    list.add(cursorToRecord(c));
                } while (c.moveToNext());
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error listing projects", t);
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    public synchronized ProjectRecord getProjectById(String id) {
        if (id == null) return null;
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = dbHelper.getReadableDatabase();
            c = db.query(ProjectDbHelper.TABLE_PROJECTS, null, "id = ?", new String[]{id}, null, null, null);
            if (c != null && c.moveToFirst()) {
                return cursorToRecord(c);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error getting project by id: " + id, t);
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public synchronized ProjectRecord getProjectBySlug(String slug) {
        if (slug == null) return null;
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = dbHelper.getReadableDatabase();
            c = db.query(ProjectDbHelper.TABLE_PROJECTS, null, "slug = ?", new String[]{slug}, null, null, null);
            if (c != null && c.moveToFirst()) {
                return cursorToRecord(c);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error getting project by slug: " + slug, t);
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public synchronized ProjectRecord getProjectBySourcePath(String sourcePath) {
        if (sourcePath == null) return null;
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = dbHelper.getReadableDatabase();
            c = db.query(ProjectDbHelper.TABLE_PROJECTS, null, "source_path = ?", new String[]{sourcePath}, null, null, null);
            if (c != null && c.moveToFirst()) {
                return cursorToRecord(c);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error getting project by source_path: " + sourcePath, t);
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public synchronized boolean insertProject(ProjectRecord rec) {
        if (rec == null || rec.getId() == null) return false;
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues cv = recordToValues(rec);
            long rowId = db.insertWithOnConflict(ProjectDbHelper.TABLE_PROJECTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            return rowId != -1;
        } catch (Throwable t) {
            Log.e(TAG, "Error inserting project: " + rec.getId(), t);
            return false;
        }
    }

    public synchronized boolean updateProject(ProjectRecord rec) {
        if (rec == null || rec.getId() == null) return false;
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            rec.setUpdatedAt(System.currentTimeMillis());
            ContentValues cv = recordToValues(rec);
            int rows = db.update(ProjectDbHelper.TABLE_PROJECTS, cv, "id = ?", new String[]{rec.getId()});
            return rows > 0;
        } catch (Throwable t) {
            Log.e(TAG, "Error updating project: " + rec.getId(), t);
            return false;
        }
    }

    public synchronized boolean updateProjectStatus(String id, String newStatus) {
        if (id == null || newStatus == null) return false;
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("status", newStatus);
            cv.put("updated_at", System.currentTimeMillis());
            int rows = db.update(ProjectDbHelper.TABLE_PROJECTS, cv, "id = ?", new String[]{id});
            return rows > 0;
        } catch (Throwable t) {
            Log.e(TAG, "Error updating project status for " + id, t);
            return false;
        }
    }

    public synchronized boolean updateProjectSource(String id, String newSourcePath, String newSourceUri, String newSafTreeUri) {
        if (id == null || newSourcePath == null) return false;
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("source_path", newSourcePath);
            if (newSourceUri != null) cv.put("source_uri", newSourceUri);
            if (newSafTreeUri != null) cv.put("saf_tree_uri", newSafTreeUri);
            cv.put("status", ProjectRecord.STATUS_ACTIVE);
            cv.put("updated_at", System.currentTimeMillis());
            int rows = db.update(ProjectDbHelper.TABLE_PROJECTS, cv, "id = ?", new String[]{id});
            return rows > 0;
        } catch (Throwable t) {
            Log.e(TAG, "Error updating project source for " + id, t);
            return false;
        }
    }

    public synchronized boolean touchLastAccessed(String id) {
        if (id == null) return false;
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("last_accessed_at", System.currentTimeMillis());
            int rows = db.update(ProjectDbHelper.TABLE_PROJECTS, cv, "id = ?", new String[]{id});
            return rows > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    public synchronized boolean deleteProject(String id) {
        if (id == null) return false;
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            int rows = db.delete(ProjectDbHelper.TABLE_PROJECTS, "id = ?", new String[]{id});
            return rows > 0;
        } catch (Throwable t) {
            Log.e(TAG, "Error deleting project: " + id, t);
            return false;
        }
    }

    @Override
    public synchronized void close() {
        try {
            dbHelper.close();
        } catch (Throwable ignored) {}
    }

    private static ProjectRecord cursorToRecord(Cursor c) {
        ProjectRecord rec = new ProjectRecord();
        rec.setId(c.getString(c.getColumnIndexOrThrow("id")));
        rec.setName(c.getString(c.getColumnIndexOrThrow("name")));
        rec.setSlug(c.getString(c.getColumnIndexOrThrow("slug")));
        rec.setDescription(c.getString(c.getColumnIndexOrThrow("description")));
        rec.setProjectType(c.getString(c.getColumnIndexOrThrow("project_type")));
        rec.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
        rec.setSourceType(c.getString(c.getColumnIndexOrThrow("source_type")));
        rec.setSourceUri(c.getString(c.getColumnIndexOrThrow("source_uri")));
        rec.setSourcePath(c.getString(c.getColumnIndexOrThrow("source_path")));
        rec.setWorkspacePath(c.getString(c.getColumnIndexOrThrow("workspace_path")));
        rec.setSafTreeUri(c.getString(c.getColumnIndexOrThrow("saf_tree_uri")));
        rec.setSchemaVersion(c.getInt(c.getColumnIndexOrThrow("schema_version")));
        rec.setVcsType(c.getString(c.getColumnIndexOrThrow("vcs_type")));
        rec.setDefaultBranch(c.getString(c.getColumnIndexOrThrow("default_branch")));
        rec.setMetadataJson(c.getString(c.getColumnIndexOrThrow("metadata_json")));
        rec.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        rec.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow("updated_at")));
        rec.setLastAccessedAt(c.getLong(c.getColumnIndexOrThrow("last_accessed_at")));
        return rec;
    }

    private static ContentValues recordToValues(ProjectRecord rec) {
        ContentValues cv = new ContentValues();
        cv.put("id", rec.getId());
        cv.put("name", rec.getName() != null ? rec.getName() : "");
        cv.put("slug", rec.getSlug() != null ? rec.getSlug() : "");
        cv.put("description", rec.getDescription() != null ? rec.getDescription() : "");
        cv.put("project_type", rec.getProjectType() != null ? rec.getProjectType() : ProjectRecord.TYPE_GENERIC);
        cv.put("status", rec.getStatus() != null ? rec.getStatus() : ProjectRecord.STATUS_ACTIVE);
        cv.put("source_type", rec.getSourceType() != null ? rec.getSourceType() : ProjectRecord.SOURCE_TYPE_DEFAULT);
        cv.put("source_uri", rec.getSourceUri() != null ? rec.getSourceUri() : "");
        cv.put("source_path", rec.getSourcePath() != null ? rec.getSourcePath() : "");
        cv.put("workspace_path", rec.getWorkspacePath() != null ? rec.getWorkspacePath() : "");
        cv.put("saf_tree_uri", rec.getSafTreeUri());
        cv.put("schema_version", rec.getSchemaVersion());
        cv.put("vcs_type", rec.getVcsType() != null ? rec.getVcsType() : "none");
        cv.put("default_branch", rec.getDefaultBranch());
        cv.put("metadata_json", rec.getMetadataJson());
        cv.put("created_at", rec.getCreatedAt());
        cv.put("updated_at", rec.getUpdatedAt());
        cv.put("last_accessed_at", rec.getLastAccessedAt());
        return cv;
    }
}
