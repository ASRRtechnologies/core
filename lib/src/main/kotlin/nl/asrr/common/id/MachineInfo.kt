package nl.asrr.common.id

import lombok.extern.log4j.Log4j2
import org.springframework.util.DigestUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.NetworkInterface


object MachineInfo {
    val idAsHex: String
        get() {
            val machineBytes = concatByteArray(systemInfo, macAddresses)
            return DigestUtils.md5DigestAsHex(machineBytes)
        }

    @JvmStatic
    val id: ByteArray
        get() {
            val machineBytes = concatByteArray(systemInfo, macAddresses)
            return DigestUtils.md5Digest(machineBytes)
        }

    private fun concatByteArray(systemInfo: ByteArray, macAddresses: ByteArray): ByteArray {
        val machineBytes = ByteArray(systemInfo.size + macAddresses.size)
        System.arraycopy(systemInfo, 0, machineBytes, 0, systemInfo.size)
        System.arraycopy(macAddresses, 0, machineBytes, systemInfo.size, macAddresses.size)
        return machineBytes
    }

    private val systemInfo: ByteArray
        get() {
            val systemInfo = System.getProperty("os.name") +
                    System.getProperty("os.version") +
                    System.getProperty("os.arch") +
                    Runtime.getRuntime().availableProcessors()
            return systemInfo.toByteArray()
        }

    private val macAddresses: ByteArray
        get() {
            val macAddresses = ByteArrayOutputStream()
            try {
                val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val networkInterface = networkInterfaces.nextElement()
                    val mac = networkInterface.hardwareAddress
                    if (mac != null) {
                        macAddresses.write(mac)
                    }
                }
            } catch (e: IOException) {
//                MachineInfo.log.error(e)
            }
            return macAddresses.toByteArray()
        }
}