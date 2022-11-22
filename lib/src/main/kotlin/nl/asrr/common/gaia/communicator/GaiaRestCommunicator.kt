package nl.asrr.common.gaia.communicator

import nl.asrr.common.gaia.dto.CreateApplication
import nl.asrr.common.gaia.dto.NodeUpdate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

class GaiaRestCommunicator {

    fun registerApplication(creation: CreateApplication) {
        try {
            restTemplate().postForEntity<Any>("https://gaia.kube.asrr.nl/api/v1/application/create", creation)
        } catch (_: Exception) {
            // ignore
        }
    }

    fun updateNode(update: NodeUpdate) = try {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity<NodeUpdate>(update, headers)
        restTemplate().put("https://gaia.kube.asrr.nl/api/v1/application/update-node", entity)
    } catch (_: Exception) {
        // ignore
    }

    private fun restTemplate(): RestTemplate = RestTemplate()
}