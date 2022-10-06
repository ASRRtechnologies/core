package nl.asrr.common.id

import nl.asrr.common.id.MachineInfo.id
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * A threadsafe id generator to generate unique ids with.
 */
class IdGenerator {
    /**
     * A random number generator.
     */
    private val random: SecureRandom

    /**
     * The local counter.
     */
    private var counter: AtomicInteger

    /**
     * Contains a cached instance of the machine hash.
     */
    private val cachedMachineHash = AtomicReference<ByteArray>()

    /**
     * Constructs an [IdGenerator] with predictable output.
     *
     * @param seed the seed for the random number generator
     */
    internal constructor(seed: ByteArray?) {
        random = SecureRandom(seed)
        counter = AtomicInteger(random.nextInt())
    }

    /**
     * Constructs an [IdGenerator].
     */
    constructor() {
        random = SecureRandom()
        counter = AtomicInteger(random.nextInt())
    }

    /**
     * Generates an unique id.
     *
     * @return an unique id
     */
    fun generate(): String {
        val randomByteArray = ByteArray(1)
        random.nextBytes(randomByteArray)
        val localTime = System.currentTimeMillis()
        val machineHash = machineHash
        val randomByte = randomByteArray[0]
        val counterValue = counter.getAndIncrement()
        return generate(localTime, machineHash, randomByte, counterValue)
    }

    /**
     * Generates an unique id.
     *
     * @param localTime   the local time of the machine represented in milliseconds
     * @param machineHash a hash that can identify this machine
     * @param randomBits  a random byte that represents random data
     * @param counterBits the id counter on this machine
     * @return an unique id
     */
    fun generate(localTime: Long, machineHash: ByteArray?, randomBits: Byte, counterBits: Int): String {
        // take 63 bits of local time and reserve the last bit
        var timeBits = localTime
        timeBits = timeBits and 0x7FFFFFFFFFFFFFFFL

        // 12 bits for the machine id
        val machineBits = getPartialMachineHash(machineHash)

        // combine the 3 parts into 1 32-bit integer
        var machineCounterRandomInfo = machineBits and 0xFFF
        machineCounterRandomInfo = machineCounterRandomInfo shl 12
        machineCounterRandomInfo = machineCounterRandomInfo or (counterBits and 0xFFF)
        machineCounterRandomInfo = machineCounterRandomInfo shl 8
        machineCounterRandomInfo = machineCounterRandomInfo or (randomBits.toInt() and 0xFF)
        val idSize = 96
        val idBuffer = ByteBuffer.allocate(idSize / java.lang.Byte.SIZE)

        // add the 63 bits of local time and 1 reserved bit
        idBuffer.putLong(timeBits)

        // add the 12 machine bits, 12 counter bits and 8 the random bits
        idBuffer.putInt(machineCounterRandomInfo)
        return Base64.getUrlEncoder().encodeToString(idBuffer.array())
    }

    /**
     * Takes the first 12 bits from the `hash`.
     *
     * @param hash the hash of the machine
     * @return the first 12 bits from the `hash`
     */
    private fun getPartialMachineHash(hash: ByteArray?): Int {
        var machineId = hash!![0].toInt() and 0x0F
        machineId = machineId shl 8
        machineId = machineId or (hash[1].toInt() and 0xFF)
        return machineId
    }

    /**
     * Lazily returns the machine hash.
     *
     * @return the machine hash
     */
    private val machineHash: ByteArray?
        get() {
            var machineHash = cachedMachineHash.get()
            if (machineHash == null) {
                cachedMachineHash.set(id)
                machineHash = cachedMachineHash.get()
            }
            return machineHash
        }
}