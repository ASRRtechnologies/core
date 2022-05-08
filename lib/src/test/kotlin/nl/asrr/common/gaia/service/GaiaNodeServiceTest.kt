package nl.asrr.common.gaia.service

import nl.asrr.common.gaia.dto.CreateApplication
import org.junit.Ignore
import org.junit.jupiter.api.Test

class GaiaNodeServiceTest {

    @Ignore
    @Test
    fun `test machine info`() {
        val service = GaiaNodeService(CreateApplication("A", "B", "C", "acc"))

        val nodeData = service.getNodeData()

        nodeData.id
    }
}