package com.cognite.cdfdev

import java.io.File

data class ServiceConfig(
    val name: String,
    val namespace: String,
    val deployment: String
)

data class BuildConfig(
    val type: String = "bazel",
    val target: String,
    val pushTarget: String
)

data class ClusterConfig(
    val context: String = "az-arn-dev-002",
    val registry: String
)

data class PortsConfig(
    val api: Int? = null,
    val metrics: Int? = null
)

data class CdfDevConfig(
    val service: ServiceConfig,
    val build: BuildConfig,
    val cluster: ClusterConfig,
    val ports: PortsConfig? = null,
    val envPresets: Map<String, List<String>>? = null
) {
    companion object {
        fun load(configFile: File = File(".cdf-dev.yaml")): CdfDevConfig? {
            if (!configFile.exists()) return null

            val lines = configFile.readLines()
            val config = mutableMapOf<String, Any>()
            var currentSection: String? = null
            var currentMap = config
            val sectionStack = mutableListOf<MutableMap<String, Any>>()
            val presets = mutableMapOf<String, MutableList<String>>()

            var inEnvPresets = false
            var currentPreset: String? = null

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

                val indent = line.takeWhile { it == ' ' }.length

                when {
                    trimmed == "envPresets:" -> {
                        inEnvPresets = true
                        continue
                    }
                    inEnvPresets && indent == 2 && trimmed.endsWith(":") -> {
                        currentPreset = trimmed.removeSuffix(":")
                        presets[currentPreset!!] = mutableListOf()
                        continue
                    }
                    inEnvPresets && indent == 4 && trimmed.startsWith("- ") -> {
                        currentPreset?.let { presets[it]?.add(trimmed.removePrefix("- ")) }
                        continue
                    }
                    indent == 0 && trimmed.endsWith(":") -> {
                        currentSection = trimmed.removeSuffix(":")
                        val newMap = mutableMapOf<String, Any>()
                        config[currentSection!!] = newMap
                        currentMap = newMap
                        sectionStack.clear()
                        sectionStack.add(newMap)
                        inEnvPresets = false
                    }
                    indent == 2 && trimmed.contains(":") -> {
                        val (key, value) = trimmed.split(":", limit = 2)
                        val trimmedValue = value.trim()
                        if (trimmedValue.isEmpty()) continue
                        sectionStack.last()[key.trim()] = trimmedValue
                    }
                }
            }

            return CdfDevConfig(
                service = ServiceConfig(
                    name = config["service"]?.let { (it as Map<*, *>)["name"] as? String } ?: "",
                    namespace = config["service"]?.let { (it as Map<*, *>)["namespace"] as? String } ?: "",
                    deployment = config["service"]?.let { (it as Map<*, *>)["deployment"] as? String } ?: ""
                ),
                build = BuildConfig(
                    target = config["build"]?.let { (it as Map<*, *>)["target"] as? String } ?: "",
                    pushTarget = config["build"]?.let { (it as Map<*, *>)["pushTarget"] as? String } ?: ""
                ),
                cluster = ClusterConfig(
                    context = config["cluster"]?.let { (it as Map<*, *>)["context"] as? String } ?: "az-arn-dev-002",
                    registry = config["cluster"]?.let { (it as Map<*, *>)["registry"] as? String } ?: ""
                ),
                ports = config["ports"]?.let {
                    val portsMap = it as Map<*, *>
                    PortsConfig(
                        api = (portsMap["api"] as? String)?.toIntOrNull(),
                        metrics = (portsMap["metrics"] as? String)?.toIntOrNull()
                    )
                },
                envPresets = if (presets.isEmpty()) null else presets.toMap()
            )
        }

        fun findConfig(startDir: File = File(".")): CdfDevConfig? {
            var currentDir = startDir.absoluteFile
            while (currentDir != null) {
                val configFile = File(currentDir, ".cdf-dev.yaml")
                if (configFile.exists()) {
                    return load(configFile)
                }
                currentDir = currentDir.parentFile
            }
            return null
        }
    }
}
