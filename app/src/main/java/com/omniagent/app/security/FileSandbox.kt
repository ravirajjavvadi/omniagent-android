package com.omniagent.app.security

import java.io.File

/**
 * File Sandbox Manager.
 * Restricts file operations to the designated /uploads/ directory.
 * Prevents directory traversal and unauthorized file access.
 */
object FileSandbox {

    private const val UPLOADS_DIR = "uploads"
    private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5MB max
    private const val MAX_INPUT_LENGTH = 50_000 // 50K characters max
    
    // File type whitelist validation
    private val ALLOWED_EXTENSIONS = setOf("txt", "json", "log", "csv", "md", "xml", "py", "kt", "js", "html", "css")

    private lateinit var sandboxRoot: File

    fun initialize(filesDir: File) {
        sandboxRoot = File(filesDir, UPLOADS_DIR)
        if (!sandboxRoot.exists()) {
            sandboxRoot.mkdirs()
        }
    }

    /**
     * Get the sandbox root directory.
     */
    fun getSandboxDir(): File = sandboxRoot

    /**
     * Validate that a file path is within the sandbox.
     * Prevents directory traversal attacks.
     */
    fun isPathSafe(path: String): Boolean {
        if (path.contains("..") || path.contains("~")) return false

        val file = File(sandboxRoot, path)
        val resolvedPath = file.canonicalPath
        return resolvedPath.startsWith(sandboxRoot.canonicalPath) && hasValidExtension(path)
    }

    /**
     * Validate against strict whitelist of file types.
     */
    fun hasValidExtension(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in ALLOWED_EXTENSIONS || fileName.indexOf('.') == -1 // allow extensionless like "README"
    }

    /**
     * Validate file size is within limits.
     */
    fun isFileSizeValid(file: File): Boolean {
        return file.length() <= MAX_FILE_SIZE_BYTES
    }

    /**
     * Validate input text length.
     */
    fun isInputValid(input: String): Boolean {
        return input.length <= MAX_INPUT_LENGTH && input.isNotBlank()
    }

    /**
     * Sanitize input — remove potentially dangerous characters.
     */
    fun sanitizeInput(input: String): String {
        // Trim to max length
        val trimmed = if (input.length > MAX_INPUT_LENGTH) {
            input.substring(0, MAX_INPUT_LENGTH)
        } else {
            input
        }
        return trimmed.trim()
    }

    /**
     * Read file from sandbox safely.
     */
    fun readFile(fileName: String): String? {
        if (!isPathSafe(fileName)) return null

        val file = File(sandboxRoot, fileName)
        if (!file.exists() || !isFileSizeValid(file)) return null

        return try {
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Write file to sandbox safely.
     */
    fun writeFile(fileName: String, content: String): Boolean {
        if (!isPathSafe(fileName)) return false
        if (content.length > MAX_FILE_SIZE_BYTES) return false

        return try {
            val file = File(sandboxRoot, fileName)
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * List files in sandbox.
     */
    fun listFiles(): List<String> {
        return sandboxRoot.listFiles()?.map { it.name } ?: emptyList()
    }

    /**
     * Delete file from sandbox.
     */
    fun deleteFile(fileName: String): Boolean {
        if (!isPathSafe(fileName)) return false
        val file = File(sandboxRoot, fileName)
        return file.delete()
    }
}
