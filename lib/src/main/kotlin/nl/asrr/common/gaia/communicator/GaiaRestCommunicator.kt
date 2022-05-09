package nl.asrr.common.gaia.communicator

import nl.asrr.common.gaia.dto.CreateApplication
import nl.asrr.common.gaia.dto.NodeUpdate
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

class GaiaRestCommunicator {

    fun registerApplication(creation: CreateApplication) {
        try {
            restTemplate().postForEntity<Any>("https://gaia.kube.asrr.nl/api/v1/application/create", creation)
        } catch (_: Exception) {
        }
    }

    fun updateNode(update: NodeUpdate) {
        try {
            restTemplate().put("https://gaia.kube.asrr.nl/api/v1/application/update-node", update)
        } catch (_: Exception) {
        }
    }

    private fun restTemplate(): RestTemplate = RestTemplate()
}