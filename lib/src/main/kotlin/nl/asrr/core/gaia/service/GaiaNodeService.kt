package nl.asrr.core.gaia.service

import mu.KotlinLogging
import nl.asrr.core.gaia.communicator.GaiaRestCommunicator
import nl.asrr.core.gaia.dto.CreateApplication
import nl.asrr.core.gaia.dto.NodeUpdate
import oshi.SystemInfo
import oshi.hardware.CentralProcessor
import oshi.hardware.ComputerSystem
import oshi.hardware.HardwareAbstractionLayer

class GaiaNodeService(private val creation: CreateApplication) {
    private val log = KotlinLogging.logger {}
    private val communicator = GaiaRestCommunicator()
    private val systemInfo = SystemInfo()

    init {
        log.info { "Registering GAIA Application: ${creation.company}|${creation.project}|${creation.name}" }
        communicator.registerApplication(creation)
    }

    fun updateNode() {
        communicator.updateNode(getNodeData())
    }

    fun getNodeData(): NodeUpdate {
        var totalRam: Long = 0
        var available: Long = 0
        try {
            totalRam = systemInfo.hardware.memory.total
            available = systemInfo.hardware.memory.available
        } catch (e: Exception) {
            // Don't spam the logs, this breaks on M1 / arm chips
        }

        return NodeUpdate(
            creation.company,
            creation.project,
            creation.name,
            generateId(),
            systemInfo.operatingSystem.networkParams.hostName,
            System.getProperty("user.name"),
            "${systemInfo.operatingSystem.family} ${systemInfo.operatingSystem.versionInfo.version}",
            totalRam - available,
            totalRam,
            creation.profile
        )
    }

    private fun generateId(): String {
        val hardwareAbstractionLayer: HardwareAbstractionLayer = systemInfo.hardware
        val centralProcessor: CentralProcessor = hardwareAbstractionLayer.processor
        val computerSystem: ComputerSystem = hardwareAbstractionLayer.computerSystem
        val vendor = systemInfo.operatingSystem?.manufacturer ?: ""
        val processorSerialNumber: String = computerSystem.serialNumber
        val processors: Int = centralProcessor.logicalProcessorCount
        return "$vendor-$processorSerialNumber-$processors"
    }

}