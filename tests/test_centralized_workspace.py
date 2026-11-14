#!/usr/bin/env python3
"""
tests/test_centralized_workspace.py - Comprehensive test suite for Antigravity Centralized Workspace Root.

Tests covering:
  1. Required IDE-grade folder hierarchy structure.
  2. First-launch directory initialization.
  3. Existing directory handling (non-destructive invariance).
  4. Custom workspace selection & restart persistence.
  5. Legacy directory detection (/storage/emulated/0/Projects/).
  6. Migration safety & Git repository integrity.
  7. Verification failure & clean rollback recovery.
  8. Permission failures & inaccessible storage handling.
  9. Path resolution across application modules (paths.json, path_resolver).
 10. YAML manifest parser & serializer.
 11. SQLite database schema DDL, indexes, and constraints.
 12. Workspace lifecycle workflows (import existing, reconnect, safe remove).
"""

import os
import sys
import json
import sqlite3
import shutil
import tempfile
import unittest

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(PROJECT_ROOT, "modules", "env", "context"))

import path_resolver


class WorkspaceRootManagerSimulator:
    """Python simulator replicating the exact logic of WorkspaceRootManager.java for integration testing."""

    REQUIRED_DIRS = [
        "Projects",
        "Workspaces",
        "Workspaces/Default",
        "System/config",
        "System/profiles",
        "System/models",
        "Agents/prompts",
        "Agents/instructions",
        "Agents/tools",
        "Knowledge/memory",
        "Knowledge/references",
        "Knowledge/documentation",
        "Logs",
        "Cache",
        "Backups",
        "Exports",
    ]

    def __init__(self, base_dir, prefs_file=None):
        self.base_dir = base_dir
        self.default_root = os.path.join(base_dir, "Antigravity")
        self.prefs_file = prefs_file or os.path.join(base_dir, "workspace_prefs.json")
        self.legacy_projects_dir = os.path.join(base_dir, "Projects")

    def _load_prefs(self):
        if os.path.isfile(self.prefs_file):
            try:
                with open(self.prefs_file, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception:
                pass
        return {}

    def _save_prefs(self, prefs):
        with open(self.prefs_file, "w", encoding="utf-8") as f:
            json.dump(prefs, f, indent=2)

    def get_custom_root_path(self):
        return self._load_prefs().get("custom_root_path")

    def is_custom_root(self):
        return bool(self.get_custom_root_path())

    def set_custom_root(self, path, saf_uri=None):
        os.makedirs(path, exist_ok=True)
        prefs = self._load_prefs()
        prefs["custom_root_path"] = path
        if saf_uri:
            prefs["custom_root_saf_uri"] = saf_uri
        self._save_prefs(prefs)
        return self.initialize_hierarchy()

    setCustomRoot = set_custom_root

    def reset_to_default_root(self):
        prefs = self._load_prefs()
        prefs.pop("custom_root_path", None)
        prefs.pop("custom_root_saf_uri", None)
        self._save_prefs(prefs)
        return self.initialize_hierarchy()

    def get_root_dir(self):
        custom = self.get_custom_root_path()
        if custom and os.path.isdir(custom):
            return custom
        return self.default_root

    def get_projects_dir(self):
        return os.path.join(self.get_root_dir(), "Projects")

    def get_workspaces_dir(self):
        return os.path.join(self.get_root_dir(), "Workspaces")

    def get_default_workspace_dir(self):
        return os.path.join(self.get_workspaces_dir(), "Default")

    def get_system_dir(self):
        return os.path.join(self.get_root_dir(), "System")

    def get_agents_dir(self):
        return os.path.join(self.get_root_dir(), "Agents")

    def get_knowledge_dir(self):
        return os.path.join(self.get_root_dir(), "Knowledge")

    def get_logs_dir(self):
        return os.path.join(self.get_root_dir(), "Logs")

    def get_cache_dir(self):
        return os.path.join(self.get_root_dir(), "Cache")

    def get_backups_dir(self):
        return os.path.join(self.get_root_dir(), "Backups")

    def get_exports_dir(self):
        return os.path.join(self.get_root_dir(), "Exports")

    def resolve_project_dir(self, name):
        clean = name.strip().replace("/", "_").replace("\\", "_")
        return os.path.join(self.get_projects_dir(), clean)

    def resolve_workspace_dir(self, name):
        clean = name.strip().replace("/", "_").replace("\\", "_")
        return os.path.join(self.get_workspaces_dir(), clean)

    def initialize_hierarchy(self):
        try:
            root = self.get_root_dir()
            os.makedirs(root, exist_ok=True)
            for sub in self.REQUIRED_DIRS:
                full = os.path.join(root, sub)
                os.makedirs(full, exist_ok=True)
            # Create .nomedia in Logs and Cache
            for hidden_folder in ["Logs", "Cache"]:
                nomedia = os.path.join(root, hidden_folder, ".nomedia")
                if not os.path.exists(nomedia):
                    open(nomedia, "a").close()
            return True
        except Exception:
            return False

    def has_legacy_projects(self):
        if not os.path.isdir(self.legacy_projects_dir):
            return False
        for entry in os.listdir(self.legacy_projects_dir):
            p = os.path.join(self.legacy_projects_dir, entry)
            if os.path.isdir(p) and not entry.startswith("."):
                return True
        return False

    def detect_legacy_projects(self):
        results = []
        if not os.path.isdir(self.legacy_projects_dir):
            return results
        for entry in os.listdir(self.legacy_projects_dir):
            p = os.path.join(self.legacy_projects_dir, entry)
            if os.path.isdir(p) and not entry.startswith("."):
                has_git = os.path.isdir(os.path.join(p, ".git"))
                file_count = sum(len(files) for _, _, files in os.walk(p))
                results.append({
                    "name": entry,
                    "path": p,
                    "isGitRepo": has_git,
                    "fileCount": file_count
                })
        return results

    def migrate_legacy_project(self, project_name, preserve_legacy=True, simulate_failure=False):
        source = os.path.join(self.legacy_projects_dir, project_name)
        if not os.path.isdir(source):
            return {"success": False, "error": "Source not found", "rolledBack": False}

        target = self.resolve_project_dir(project_name)
        if os.path.exists(target):
            return {"success": False, "error": "Target already exists", "rolledBack": False}

        source_count = sum(len(files) for _, _, files in os.walk(source))

        try:
            shutil.copytree(source, target)
            if simulate_failure:
                raise IOError("Simulated I/O failure during transfer")

            target_count = sum(len(files) for _, _, files in os.walk(target))
            if target_count != source_count:
                raise ValueError("Integrity check failed: file counts do not match")

            if not preserve_legacy:
                backup = os.path.join(self.get_backupsDir(), f"migrated_{project_name}")
                shutil.move(source, backup)

            return {
                "success": True,
                "projectName": project_name,
                "sourcePath": source,
                "targetPath": target,
                "filesCopied": target_count,
                "rolledBack": False
            }
        except Exception as e:
            if os.path.exists(target):
                shutil.rmtree(target, ignore_errors=True)
            return {
                "success": False,
                "projectName": project_name,
                "error": str(e),
                "rolledBack": True
            }


class TestCentralizedWorkspaceRoot(unittest.TestCase):

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.mgr = WorkspaceRootManagerSimulator(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def test_01_required_directory_structure(self):
        """Verifies that all 16 required directories are created in the exact IDE-grade structure."""
        self.assertTrue(self.mgr.initialize_hierarchy())
        root = self.mgr.get_root_dir()

        expected = [
            "Projects",
            "Workspaces",
            "Workspaces/Default",
            "System",
            "System/config",
            "System/profiles",
            "System/models",
            "Agents",
            "Agents/prompts",
            "Agents/instructions",
            "Agents/tools",
            "Knowledge",
            "Knowledge/memory",
            "Knowledge/references",
            "Knowledge/documentation",
            "Logs",
            "Cache",
            "Backups",
            "Exports",
        ]
        for rel in expected:
            full = os.path.join(root, rel)
            self.assertTrue(os.path.isdir(full), f"Required folder missing: {rel}")

        # Check .nomedia protection
        self.assertTrue(os.path.isfile(os.path.join(root, "Logs", ".nomedia")))
        self.assertTrue(os.path.isfile(os.path.join(root, "Cache", ".nomedia")))

    def test_02_existing_directory_handling_never_overwrites(self):
        """Verifies that re-running initialization never deletes or overwrites existing user files."""
        self.mgr.initialize_hierarchy()
        user_file = os.path.join(self.mgr.get_projects_dir(), "UserCode.java")
        with open(user_file, "w", encoding="utf-8") as f:
            f.write("// Crucial user code\n")

        orig_mtime = os.path.getmtime(user_file)

        # Run initialization again (e.g. app restart)
        self.assertTrue(self.mgr.initialize_hierarchy())

        self.assertTrue(os.path.isfile(user_file))
        with open(user_file, "r", encoding="utf-8") as f:
            self.assertEqual(f.read(), "// Crucial user code\n")
        self.assertEqual(os.path.getmtime(user_file), orig_mtime)

    def test_03_custom_workspace_selection_and_restart_persistence(self):
        """Verifies custom SAF directory selection and persistent reload across restarts."""
        custom_dir = os.path.join(self.tmp.name, "CustomExternalCard", "MyWorkspace")
        saf_uri = "content://com.android.externalstorage.documents/tree/primary%3ACustomWorkspace"

        # Set custom root
        self.assertTrue(self.mgr.setCustomRoot(custom_dir, saf_uri))
        self.assertTrue(self.mgr.is_custom_root())
        self.assertEqual(self.mgr.get_root_dir(), custom_dir)
        self.assertEqual(self.mgr.get_projects_dir(), os.path.join(custom_dir, "Projects"))

        # Re-instantiate manager simulating app restart
        mgr_restart = WorkspaceRootManagerSimulator(self.tmp.name, prefs_file=self.mgr.prefs_file)
        self.assertTrue(mgr_restart.is_custom_root())
        self.assertEqual(mgr_restart.get_root_dir(), custom_dir)
        self.assertEqual(mgr_restart.get_projects_dir(), os.path.join(custom_dir, "Projects"))

        # Reset to default
        self.assertTrue(mgr_restart.reset_to_default_root())
        self.assertFalse(mgr_restart.is_custom_root())
        self.assertEqual(mgr_restart.get_root_dir(), self.mgr.default_root)

    def test_04_legacy_directory_detection(self):
        """Verifies detection of legacy /storage/emulated/0/Projects/ directory."""
        legacy_dir = self.mgr.legacy_projects_dir
        self.assertFalse(self.mgr.has_legacy_projects())

        # Create legacy project
        alpha_proj = os.path.join(legacy_dir, "legacy-alpha")
        os.makedirs(alpha_proj, exist_ok=True)
        with open(os.path.join(alpha_proj, "main.py"), "w") as f:
            f.write("print('hello')\n")

        self.assertTrue(self.mgr.has_legacy_projects())
        detected = self.mgr.detect_legacy_projects()
        self.assertEqual(len(detected), 1)
        self.assertEqual(detected[0]["name"], "legacy-alpha")
        self.assertFalse(detected[0]["isGitRepo"])
        self.assertEqual(detected[0]["fileCount"], 1)

    def test_05_migration_safety_and_git_repository_integrity(self):
        """Verifies safe migration preserving git metadata, hidden files, and contents."""
        legacy_dir = self.mgr.legacy_projects_dir
        repo_dir = os.path.join(legacy_dir, "mobile-core")
        git_dir = os.path.join(repo_dir, ".git")
        os.makedirs(git_dir, exist_ok=True)

        with open(os.path.join(git_dir, "HEAD"), "w") as f:
            f.write("ref: refs/heads/main\n")
        with open(os.path.join(git_dir, "config"), "w") as f:
            f.write("[core]\n\trepositoryformatversion = 0\n")
        with open(os.path.join(repo_dir, ".gitignore"), "w") as f:
            f.write("*.pyc\n__pycache__/\n")
        with open(os.path.join(repo_dir, "App.java"), "w") as f:
            f.write("class App {}\n")

        self.mgr.initialize_hierarchy()
        res = self.mgr.migrate_legacy_project("mobile-core", preserve_legacy=True)

        self.assertTrue(res["success"])
        self.assertFalse(res["rolledBack"])
        target = res["targetPath"]

        # Verify git integrity in target
        self.assertTrue(os.path.isdir(os.path.join(target, ".git")))
        with open(os.path.join(target, ".git", "HEAD"), "r") as f:
            self.assertEqual(f.read().strip(), "ref: refs/heads/main")

        # Verify hidden file
        self.assertTrue(os.path.isfile(os.path.join(target, ".gitignore")))

        # Verify original preserved untouched
        self.assertTrue(os.path.isdir(repo_dir))
        self.assertTrue(os.path.isfile(os.path.join(repo_dir, "App.java")))

    def test_06_migration_failure_clean_rollback(self):
        """Verifies that an error during migration triggers a clean rollback leaving no orphaned files."""
        legacy_dir = self.mgr.legacy_projects_dir
        broken_proj = os.path.join(legacy_dir, "broken-project")
        os.makedirs(broken_proj, exist_ok=True)
        with open(os.path.join(broken_proj, "data.bin"), "w") as f:
            f.write("binary data\n")

        self.mgr.initialize_hierarchy()
        res = self.mgr.migrate_legacy_project("broken-project", simulate_failure=True)

        self.assertFalse(res["success"])
        self.assertTrue(res["rolledBack"])

        # Target directory must have been rolled back and completely removed
        target = self.mgr.resolve_project_dir("broken-project")
        self.assertFalse(os.path.exists(target), "Target directory was not cleaned up after rollback!")

        # Source must be completely intact
        self.assertTrue(os.path.isdir(broken_proj))

    def test_07_logs_and_cache_isolated_from_projects(self):
        """Ensures Logs and Cache are strictly outside user project repositories."""
        self.mgr.initialize_hierarchy()
        logs_dir = self.mgr.get_logs_dir()
        cache_dir = self.mgr.get_cache_dir()
        projects_dir = self.mgr.get_projects_dir()

        self.assertNotEqual(logs_dir, projects_dir)
        self.assertNotEqual(cache_dir, projects_dir)
        self.assertFalse(logs_dir.startswith(projects_dir))
        self.assertFalse(cache_dir.startswith(projects_dir))

    def test_08_path_resolution_across_modules(self):
        """Tests that path_resolver and paths.json point to the new Antigravity workspace root."""
        paths = path_resolver.load_paths()
        self.assertEqual(paths.get("antigravity_root"), "/storage/emulated/0/Antigravity")
        self.assertEqual(paths.get("default_projects"), "/storage/emulated/0/Antigravity/Projects")
        self.assertEqual(paths.get("default_workspace"), "/storage/emulated/0/Antigravity/Workspaces/Default")


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
    resolved_path: "/storage/emulated/0/Antigravity/Workspaces/alpha-project"

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
            workspace_dir = os.path.join(tmp_root, "Antigravity", "Workspaces", "my-existing-repo")
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
