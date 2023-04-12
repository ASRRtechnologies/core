package nl.asrr.core.id

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.*

class IdGeneratorTest {

    @Test
    fun `generate should be predictable`() {
        val localTime: Long = 0xBEEF9
        val machineHash = byteArrayOf(0x22.toByte(), 0x33.toByte())
        val counterBits = 0x6688
        val randomBits: Byte = 0x11
        val idBuffer: ByteBuffer = ByteBuffer.allocate(96 / java.lang.Byte.SIZE)
        idBuffer.putLong(0x00000000000BEEF9L)
        idBuffer.putInt(0x23368811)
        val calculatedId: String = Base64.getUrlEncoder().encodeToString(idBuffer.array())

        val idGenerator = createGenerator()
        val id: String = idGenerator.generate(localTime, machineHash, randomBits, counterBits)

        assertEquals(calculatedId, id)
    }

    private fun createGenerator(): IdGenerator {
        return IdGenerator("1".toByteArray())
    }
}