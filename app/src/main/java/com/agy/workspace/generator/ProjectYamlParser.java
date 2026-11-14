package com.agy.workspace.generator;

import com.agy.workspace.model.ProjectRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Lightweight, zero-dependency parser and serializer for workspace project.yaml.
 */
public final class ProjectYamlParser {

    private ProjectYamlParser() {}

    /** Serializes a ProjectRecord to standardized, human-readable YAML. */
    public static String toYaml(ProjectRecord p) {
        if (p == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("# Antigravity Project Workspace Manifest\n");
        sb.append("schema_version: ").append(p.getSchemaVersion()).append("\n");
        sb.append("id: \"").append(escape(p.getId())).append("\"\n");
        sb.append("name: \"").append(escape(p.getName())).append("\"\n");
        sb.append("slug: \"").append(escape(p.getSlug())).append("\"\n");
        if (p.getDescription() != null && !p.getDescription().isEmpty()) {
            sb.append("description: \"").append(escape(p.getDescription())).append("\"\n");
        }
        sb.append("category: \"").append(escape(p.getProjectType())).append("\"\n");
        sb.append("status: \"").append(escape(p.getStatus())).append("\"\n\n");

        sb.append("paths:\n");
        sb.append("  source:\n");
        sb.append("    type: \"").append(escape(p.getSourceType())).append("\"\n");
        sb.append("    uri: \"").append(escape(p.getSourceUri())).append("\"\n");
        sb.append("    resolved_path: \"").append(escape(p.getSourcePath())).append("\"\n");
        sb.append("  workspace:\n");
        sb.append("    resolved_path: \"").append(escape(p.getWorkspacePath())).append("\"\n\n");

        sb.append("vcs:\n");
        sb.append("  type: \"").append(escape(p.getVcsType())).append("\"\n");
        if (p.getDefaultBranch() != null && !p.getDefaultBranch().isEmpty()) {
            sb.append("  default_branch: \"").append(escape(p.getDefaultBranch())).append("\"\n");
        }
        sb.append("\n");

        sb.append("timestamps:\n");
        sb.append("  created_at: ").append(p.getCreatedAt()).append("\n");
        sb.append("  updated_at: ").append(p.getUpdatedAt()).append("\n");
        sb.append("  last_accessed_at: ").append(p.getLastAccessedAt()).append("\n");

        return sb.toString();
    }

    /** Simple line-based reader for extracting key fields from project.yaml. */
    public static ProjectRecord fromYamlFile(File yamlFile) {
        if (yamlFile == null || !yamlFile.isFile()) return null;
        ProjectRecord rec = new ProjectRecord();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(yamlFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;
                if (line.startsWith("schema_version:")) {
                    rec.setSchemaVersion(parseInt(extractValue(line), 1));
                } else if (line.startsWith("id:")) {
                    rec.setId(extractValue(line));
                } else if (line.startsWith("name:")) {
                    rec.setName(extractValue(line));
                } else if (line.startsWith("slug:")) {
                    rec.setSlug(extractValue(line));
                } else if (line.startsWith("description:")) {
                    rec.setDescription(extractValue(line));
                } else if (line.startsWith("category:")) {
                    rec.setProjectType(extractValue(line));
                } else if (line.startsWith("status:")) {
                    rec.setStatus(extractValue(line));
                } else if (line.startsWith("resolved_path:")) {
                    if (rec.getSourcePath() == null || rec.getSourcePath().isEmpty()) {
                        rec.setSourcePath(extractValue(line));
                    } else if (rec.getWorkspacePath() == null || rec.getWorkspacePath().isEmpty()) {
                        rec.setWorkspacePath(extractValue(line));
                    }
                } else if (line.startsWith("uri:")) {
                    rec.setSourceUri(extractValue(line));
                } else if (line.startsWith("type:") && rec.getSourceType() == null) {
                    rec.setSourceType(extractValue(line));
                }
            }
        } catch (Throwable t) {
            return null;
        }
        return rec;
    }

    private static String extractValue(String line) {
        int idx = line.indexOf(':');
        if (idx == -1) return "";
        String val = line.substring(idx + 1).trim();
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
            val = val.substring(1, val.length() - 1);
        }
        return val.replace("\\\"", "\"");
    }

    private static int parseInt(String val, int def) {
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return def;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
