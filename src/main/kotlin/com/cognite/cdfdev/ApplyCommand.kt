package com.cognite.cdfdev

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File

class ApplyCommand : CliktCommand(name = "apply", help = "Build and apply manifest changes to the cluster") {
    private val dryRun by option("--dry-run", help = "Show what would be applied without actually applying").flag()

    override fun run() {
        val config = CdfDevConfig.findConfig()
            ?: throw IllegalStateException("No .cdf-dev.yaml found in current or parent directories")

        echo("Applying manifest for ${config.service.name} to ${config.cluster.context}")

        // Find the infrastructure repo root by looking for the service directory
        val serviceDir = findServiceDirectory()
            ?: throw IllegalStateException("Could not find infrastructure service directory. Are you in the right location?")

        val infraRoot = findInfrastructureRoot(serviceDir)
            ?: throw IllegalStateException("Could not find infrastructure repository root (no WORKSPACE or BUILD.bazel found)")

        echo("\n Infrastructure root: ${infraRoot.absolutePath}")

        // Step 1: Build manifests using bazel
        echo("\n Building manifests...")
        val manifestTarget = ":manifests"
        val buildResult = CommandExecutor.execute(
            command = listOf("bazelisk", "build", manifestTarget),
            workingDir = serviceDir
        )
        if (!buildResult.success) {
            throw RuntimeException("Manifest build failed with exit code ${buildResult.exitCode}\n${buildResult.stderr}")
        }

        // Step 2: Find the rendered manifest YAML for the target cluster
        val manifestDir = File(infraRoot, "bazel-bin/${serviceDir.relativeTo(infraRoot)}/.baker/manifests/${config.service.name}/default")
        val manifestFile = File(manifestDir, "${config.cluster.context}.yaml")

        if (!manifestFile.exists()) {
            throw IllegalStateException("Manifest file not found: ${manifestFile.absolutePath}")
        }

        echo("\n Manifest file: ${manifestFile.absolutePath}")

        // Step 3: Apply the manifest
        if (dryRun) {
            echo("\n Dry run - would apply manifest with:")
            echo("   kubectl apply -f ${manifestFile.absolutePath} --context ${config.cluster.context}")

            echo("\n Manifest preview:")
            val previewResult = CommandExecutor.execute(
                command = listOf("head", "-n", "50", manifestFile.absolutePath)
            )
            echo(previewResult.stdout)
        } else {
            echo("\n Applying manifest...")
            val applyResult = CommandExecutor.execute(
                command = listOf(
                    "kubectl", "apply",
                    "-f", manifestFile.absolutePath,
                    "--context", config.cluster.context
                )
            )
            if (!applyResult.success) {
                throw RuntimeException("kubectl apply failed with exit code ${applyResult.exitCode}\n${applyResult.stderr}")
            }

            echo(applyResult.stdout)
            echo("\n Manifest applied successfully!")
        }
    }

    private fun findServiceDirectory(): File? {
        var currentDir = File(".").absoluteFile
        while (currentDir != null) {
            val configFile = File(currentDir, ".cdf-dev.yaml")
            if (configFile.exists()) {
                return currentDir
            }
            currentDir = currentDir.parentFile
        }
        return null
    }

    private fun findInfrastructureRoot(startDir: File): File? {
        var currentDir = startDir
        while (currentDir != null) {
            // Look for Bazel workspace markers
            if (File(currentDir, "WORKSPACE").exists() ||
                File(currentDir, "WORKSPACE.bazel").exists() ||
                File(currentDir, "MODULE.bazel").exists() ||
                (File(currentDir, "BUILD.bazel").exists() && File(currentDir, "services").exists())) {
                return currentDir
            }
            currentDir = currentDir.parentFile
        }
        return null
    }
}
