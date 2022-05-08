package nl.asrr.common.gaia.service

import nl.asrr.common.gaia.communicator.GaiaRestCommunicator
import nl.asrr.common.gaia.dto.CreateApplication
import nl.asrr.common.gaia.dto.NodeUpdate
import org.springframework.scheduling.annotation.Scheduled

class GaiaNodeService(private val creation: CreateApplication) {

    private val communicator = GaiaRestCommunicator()

    init {
        communicator.registerApplication(creation)
    }

    @Scheduled(fixedDelay = 10000)
    fun getNodeData(): NodeUpdate {

        // TODO: replace strings with actual values
        return NodeUpdate(
            creation.company,
            creation.project,
            creation.name,
            "id",
            "machineName",
            "user",
            "os"
        )
    }

}