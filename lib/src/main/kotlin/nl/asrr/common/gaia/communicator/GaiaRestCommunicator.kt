package nl.asrr.common.gaia.communicator

import nl.asrr.common.gaia.dto.ApplicationResponse
import nl.asrr.common.gaia.dto.CreateApplication
import nl.asrr.common.gaia.dto.NodeUpdate
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import java.util.concurrent.TimeUnit

class GaiaRestCommunicator {

    fun registerApplication(creation: CreateApplication) {
        // TODO: send post call
    }

    fun updateNode(update: NodeUpdate){
        // TODO: send post call using
        var postForEntity = restTemplate().postForEntity<Any>("https://gaia.kube.asrr.nl/api/v1/application/update-node", update, ApplicationResponse::class.java);

    }

    fun restTemplate(): RestTemplate = RestTemplate()
}