package nl.asrr.common.gaia.communicator

import nl.asrr.common.gaia.dto.CreateApplication
import nl.asrr.common.gaia.dto.NodeUpdate
import org.springframework.web.client.RestTemplate

class GaiaRestCommunicator {

    fun registerApplication(creation: CreateApplication) {
        // TODO: send post call
    }

    fun updateNode(update: NodeUpdate){
        restTemplate().put("https://gaia.kube.asrr.nl/api/v1/application/update-node", update) // TODO: make url an application property
    }

    private fun restTemplate(): RestTemplate = RestTemplate()
}