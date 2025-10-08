package com.cognite.cdfdev

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class DeployCommand : CliktCommand(name = "deploy") {
    private val skipBuild by option("--skip-build", help = "Skip building the image").flag()
    private val wait by option("--wait", help = "Wait for deployment to complete").flag(default = true)

    override fun run() {
        val config = CdfDevConfig.findConfig()
            ?: throw IllegalStateException("No .cdf-dev.yaml found in current or parent directories")

        echo("Deploying ${config.service.name} to ${config.cluster.context}")

        // Step 1: Build image
        if (!skipBuild) {
            echo("\n Building image...")
            val buildResult = CommandExecutor.execute(
                listOf("bazelisk", "build", config.build.target)
            )
            if (!buildResult.success) {
                throw RuntimeException("Build failed with exit code ${buildResult.exitCode}")
            }
        }

        // Step 2: Push image
        echo("\n Pushing image...")
        val pushResult = CommandExecutor.execute(
            listOf("bazelisk", "run", config.build.pushTarget)
        )
        if (!pushResult.success) {
            throw RuntimeException("Push failed with exit code ${pushResult.exitCode}")
        }

        // Step 3: Extract image digest from output
        val imageDigest = pushResult.stdout.lines()
            .firstOrNull { it.contains("@sha256:") }
            ?.trim()
            ?: throw RuntimeException("Could not find image digest in push output")

        echo("\n Image digest: $imageDigest")

        // Step 4: Update deployment
        echo("\n Updating deployment...")
        val updateResult = CommandExecutor.execute(
            listOf(
                "kubectl", "set", "image",
                "deployment/${config.service.deployment}",
                "${config.service.deployment}=$imageDigest",
                "-n", config.service.namespace,
                "--context", config.cluster.context
            )
        )
        if (!updateResult.success) {
            throw RuntimeException("Deployment update failed with exit code ${updateResult.exitCode}")
        }

        // Step 5: Wait for rollout
        if (wait) {
            echo("\n Waiting for rollout...")
            val rolloutResult = CommandExecutor.execute(
                listOf(
                    "kubectl", "rollout", "status",
                    "deployment/${config.service.deployment}",
                    "-n", config.service.namespace,
                    "--context", config.cluster.context,
                    "--timeout=3m"
                )
            )
            if (!rolloutResult.success) {
                throw RuntimeException("Rollout failed with exit code ${rolloutResult.exitCode}")
            }
        }

        echo("\n Deployment complete!")
    }
}
