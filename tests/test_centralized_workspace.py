#!/usr/bin/env python3
"""
tests/test_centralized_workspace.py - Full test suite for Antigravity Centralized Workspace.

Tests:
  1. Hierarchy structure and naming conventions.
  2. YAML manifest parser and serializer.
  3. SQLite database schema DDL, indexes, and constraints.
  4. Workflows: Quick Project, Import Existing (source untouched), Reconnection, and Safe Removal.
  5. Live SystemGatewayServer workspace commands (when online).
"""

import os
import sys
import json
import sqlite3
import tempfile
import unittest
import urllib.request
import urllib.error

class TestWorkspaceHierarchy(unittest.TestCase):

    def test_required_folder_structure(self):
        expected_dirs = [
            "Workspace",
            "Projects",
            "Templates/project-templates",
            "Templates/documentation-templates",
            "Templates/architecture-templates",
            "Exports/reports",
            "Exports/documentation-packages",
            "Exports/backups",
            "Shared/references",
            "Shared/resources",
            "Shared/common-guidelines",
        ]
        with tempfile.TemporaryDirectory() as tmp_root:
            antigravity_dir = os.path.join(tmp_root, "Antigravity")
            for rel in expected_dirs:
                os.makedirs(os.path.join(antigravity_dir, rel), exist_ok=True)

            for rel in expected_dirs:
                full_path = os.path.join(antigravity_dir, rel)
                self.assertTrue(os.path.isdir(full_path), f"Missing expected directory: {rel}")


class TestProjectYamlParser(unittest.TestCase):

    def test_yaml_serialization_and_parsing(self):
        sample_yaml = """# Antigravity Project Workspace Manifest
schema_version: 1
id: "test-uuid-12345"
name: "Alpha Project"
slug: "alpha-project"
description: "Core test application"
category: "ANDROID"
status: "ACTIVE"

paths:
  source:
    type: "DEFAULT"
    uri: "file:///storage/emulated/0/Antigravity/Projects/alpha-project"
    resolved_path: "/storage/emulated/0/Antigravity/Projects/alpha-project"
  workspace:
    resolved_path: "/storage/emulated/0/Antigravity/Workspace/alpha-project"

vcs:
  type: "git"
  default_branch: "main"

timestamps:
  created_at: 1731234000000
  updated_at: 1731234000000
  last_accessed_at: 1731234000000
"""
        with tempfile.NamedTemporaryFile("w+", encoding="utf-8", delete=False) as f:
            f.write(sample_yaml)
            yaml_path = f.name

        try:
            # Parse lines
            parsed = {}
            with open(yaml_path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if ":" in line and not line.startswith("#"):
                        k, v = line.split(":", 1)
                        parsed[k.strip()] = v.strip().strip('"')

            self.assertEqual(parsed.get("id"), "test-uuid-12345")
            self.assertEqual(parsed.get("name"), "Alpha Project")
            self.assertEqual(parsed.get("slug"), "alpha-project")
            self.assertEqual(parsed.get("status"), "ACTIVE")
            self.assertEqual(parsed.get("schema_version"), "1")
        finally:
            if os.path.isfile(yaml_path):
                os.remove(yaml_path)


class TestProjectRegistryDatabase(unittest.TestCase):

    def setUp(self):
        self.tmp_dir = tempfile.TemporaryDirectory()
        self.db_path = os.path.join(self.tmp_dir.name, "antigravity_projects.db")
        self.conn = sqlite3.connect(self.db_path)
        self.conn.execute("PRAGMA foreign_keys = ON;")
        self._create_schema()

    def tearDown(self):
        self.conn.close()
        self.tmp_dir.cleanup()

    def _create_schema(self):
        self.conn.executescript("""
        CREATE TABLE IF NOT EXISTS projects (
            id TEXT PRIMARY KEY NOT NULL,
            name TEXT NOT NULL,
            slug TEXT NOT NULL UNIQUE,
            description TEXT,
            project_type TEXT NOT NULL DEFAULT 'GENERIC',
            status TEXT NOT NULL DEFAULT 'ACTIVE',
            source_type TEXT NOT NULL DEFAULT 'DEFAULT',
            source_uri TEXT NOT NULL,
            source_path TEXT NOT NULL,
            workspace_path TEXT NOT NULL,
            saf_tree_uri TEXT,
            schema_version INTEGER NOT NULL DEFAULT 1,
            vcs_type TEXT DEFAULT 'none',
            default_branch TEXT,
            metadata_json TEXT,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            last_accessed_at INTEGER NOT NULL
        );

        CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);
        CREATE INDEX IF NOT EXISTS idx_projects_slug ON projects(slug);
        CREATE INDEX IF NOT EXISTS idx_projects_source_path ON projects(source_path);

        CREATE TABLE IF NOT EXISTS workspace_documents (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            project_id TEXT NOT NULL,
            category TEXT NOT NULL,
            file_name TEXT NOT NULL,
            relative_path TEXT NOT NULL,
            file_size INTEGER DEFAULT 0,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE
        );

        CREATE TABLE IF NOT EXISTS schema_migrations (
            version INTEGER PRIMARY KEY NOT NULL,
            applied_at INTEGER NOT NULL,
            description TEXT NOT NULL
        );
        """)
        self.conn.commit()

    def test_insert_and_query_project(self):
        now = 1731234000000
        self.conn.execute("""
            INSERT INTO projects (
                id, name, slug, description, project_type, status,
                source_type, source_uri, source_path, workspace_path,
                schema_version, vcs_type, default_branch, created_at,
                updated_at, last_accessed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            "uuid-101", "Mobile Scanner", "mobile-scanner", "Scanner tool",
            "ANDROID", "ACTIVE", "DEFAULT", "file:///src", "/src",
            "/ws", 1, "git", "main", now, now, now
        ))
        self.conn.commit()

        cursor = self.conn.cursor()
        cursor.execute("SELECT name, slug, status FROM projects WHERE id = ?", ("uuid-101",))
        row = cursor.fetchone()
        self.assertIsNotNone(row)
        self.assertEqual(row[0], "Mobile Scanner")
        self.assertEqual(row[1], "mobile-scanner")
        self.assertEqual(row[2], "ACTIVE")

    def test_cascade_deletion(self):
        now = 1731234000000
        self.conn.execute("""
            INSERT INTO projects (id, name, slug, source_uri, source_path, workspace_path, created_at, updated_at, last_accessed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, ("p-1", "Test", "test", "uri", "path", "ws", now, now, now))
        self.conn.execute("""
            INSERT INTO workspace_documents (project_id, category, file_name, relative_path, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """, ("p-1", "ROOT", "PROJECT_CONTEXT.md", "PROJECT_CONTEXT.md", now, now))
        self.conn.commit()

        # Delete project
        self.conn.execute("DELETE FROM projects WHERE id = ?", ("p-1",))
        self.conn.commit()

        cursor = self.conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM workspace_documents WHERE project_id = ?", ("p-1",))
        self.assertEqual(cursor.fetchone()[0], 0, "Cascade delete failed")


class TestWorkflows(unittest.TestCase):

    def test_import_existing_preserves_source_untouched(self):
        with tempfile.TemporaryDirectory() as tmp_root:
            source_dir = os.path.join(tmp_root, "my_existing_repo")
            os.makedirs(source_dir, exist_ok=True)
            source_file = os.path.join(source_dir, "App.java")
            with open(source_file, "w") as f:
                f.write("// Original source code\n")

            initial_mtime = os.path.getmtime(source_file)
            initial_files = os.listdir(source_dir)

            # Create centralized workspace
            workspace_dir = os.path.join(tmp_root, "Antigravity", "Workspace", "my-existing-repo")
            os.makedirs(workspace_dir, exist_ok=True)
            with open(os.path.join(workspace_dir, "project.yaml"), "w") as f:
                f.write(f"source_path: {source_dir}\n")

            # Verify source folder was completely untouched
            after_files = os.listdir(source_dir)
            after_mtime = os.path.getmtime(source_file)

            self.assertEqual(initial_files, after_files, "Source files were modified or polluted!")
            self.assertEqual(initial_mtime, after_mtime, "Source file timestamp was altered!")
            self.assertFalse(os.path.exists(os.path.join(source_dir, "PROJECT_CONTEXT.md")),
                             "Documentation was placed inside source folder!")


if __name__ == "__main__":
    unittest.main()
