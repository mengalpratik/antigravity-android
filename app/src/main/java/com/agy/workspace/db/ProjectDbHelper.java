package com.agy.workspace.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * SQLite helper for the Antigravity Centralized Workspace project registry.
 */
public final class ProjectDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "ProjectDbHelper";
    public static final String DATABASE_NAME = "antigravity_projects.db";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_PROJECTS = "projects";
    public static final String TABLE_DOCUMENTS = "workspace_documents";
    public static final String TABLE_MIGRATIONS = "schema_migrations";

    public ProjectDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PROJECTS + " ("
                    + "id TEXT PRIMARY KEY NOT NULL, "
                    + "name TEXT NOT NULL, "
                    + "slug TEXT NOT NULL UNIQUE, "
                    + "description TEXT, "
                    + "project_type TEXT NOT NULL DEFAULT 'GENERIC', "
                    + "status TEXT NOT NULL DEFAULT 'ACTIVE', "
                    + "source_type TEXT NOT NULL DEFAULT 'DEFAULT', "
                    + "source_uri TEXT NOT NULL, "
                    + "source_path TEXT NOT NULL, "
                    + "workspace_path TEXT NOT NULL, "
                    + "saf_tree_uri TEXT, "
                    + "schema_version INTEGER NOT NULL DEFAULT 1, "
                    + "vcs_type TEXT DEFAULT 'none', "
                    + "default_branch TEXT, "
                    + "metadata_json TEXT, "
                    + "created_at INTEGER NOT NULL, "
                    + "updated_at INTEGER NOT NULL, "
                    + "last_accessed_at INTEGER NOT NULL"
                    + ");");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_projects_status ON " + TABLE_PROJECTS + "(status);");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_projects_slug ON " + TABLE_PROJECTS + "(slug);");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_projects_source_path ON " + TABLE_PROJECTS + "(source_path);");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DOCUMENTS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "project_id TEXT NOT NULL, "
                    + "category TEXT NOT NULL, "
                    + "file_name TEXT NOT NULL, "
                    + "relative_path TEXT NOT NULL, "
                    + "file_size INTEGER DEFAULT 0, "
                    + "created_at INTEGER NOT NULL, "
                    + "updated_at INTEGER NOT NULL, "
                    + "FOREIGN KEY(project_id) REFERENCES " + TABLE_PROJECTS + "(id) ON DELETE CASCADE"
                    + ");");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_docs_project_id ON " + TABLE_DOCUMENTS + "(project_id);");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MIGRATIONS + " ("
                    + "version INTEGER PRIMARY KEY NOT NULL, "
                    + "applied_at INTEGER NOT NULL, "
                    + "description TEXT NOT NULL"
                    + ");");

            db.execSQL("INSERT OR IGNORE INTO " + TABLE_MIGRATIONS + " (version, applied_at, description) "
                    + "VALUES (1, " + System.currentTimeMillis() + ", 'Initial centralized workspace schema');");

            Log.i(TAG, "Initialized antigravity_projects.db schema version 1");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to create project database schema", t);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
    }
}
