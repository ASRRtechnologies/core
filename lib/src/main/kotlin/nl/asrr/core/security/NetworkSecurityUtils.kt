/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.security

import java.net.InetAddress

/**
 * Address checks for outbound requests to user-supplied URLs.
 *
 * There is deliberately no "validate this URL" helper here: resolving a host and then letting the
 * HTTP client resolve it again leaves a DNS-rebinding gap. Callers check every address inside the
 * client's own resolver instead, as Alpha's `PublicUrlFetcher`
 * and `WebhookOutboundAdapter` do (an `InetAddress` check in a custom DNS resolver).
 */
object NetworkSecurityUtils {

    private class Cidr(prefix: String, val bits: Int) {
        val bytes: ByteArray = InetAddress.getByName(prefix).address

        fun contains(address: ByteArray): Boolean {
            if (address.size != bytes.size) return false
            val fullBytes = bits / 8
            if ((0 until fullBytes).any { address[it] != bytes[it] }) return false
            val rest = bits % 8
            if (rest == 0) return true
            val mask = (0xff shl (8 - rest)) and 0xff
            return (address[fullBytes].toInt() and mask) == (bytes[fullBytes].toInt() and mask)
        }
    }

    /** IPv4 ranges that are not globally routable (RFC 6890 and friends). */
    private val blockedIpv4 = listOf(
        Cidr("0.0.0.0", 8), // "this" network
        Cidr("10.0.0.0", 8), // private
        Cidr("100.64.0.0", 10), // carrier-grade NAT
        Cidr("127.0.0.0", 8), // loopback
        Cidr("169.254.0.0", 16), // link-local, cloud metadata
        Cidr("172.16.0.0", 12), // private
        Cidr("192.0.0.0", 24), // IETF protocol assignments
        Cidr("192.0.2.0", 24), // TEST-NET-1
        Cidr("192.168.0.0", 16), // private
        Cidr("198.18.0.0", 15), // benchmarking
        Cidr("198.51.100.0", 24), // TEST-NET-2
        Cidr("203.0.113.0", 24), // TEST-NET-3
        Cidr("224.0.0.0", 3), // multicast and reserved
    )

    /** Only global unicast IPv6 is public. */
    private val globalIpv6 = Cidr("2000::", 3)

    /** Transition and special-purpose ranges inside global unicast. */
    private val blockedIpv6 = listOf(
        Cidr("2001::", 23), // IETF protocol assignments, incl. Teredo
        Cidr("2001:db8::", 32), // documentation
        Cidr("2002::", 16), // 6to4
        Cidr("3fff::", 16), // documentation
    )

    /**
     * Whether [address] is a globally routable public address, rejecting loopback, link-local
     * (cloud metadata), private, carrier-grade NAT, multicast and reserved addresses.
     */
    fun isPublicAddress(address: InetAddress): Boolean {
        if (isLocalScope(address)) return false
        val bytes = address.address
        return when (bytes.size) {
            4 -> blockedIpv4.none { it.contains(bytes) }
            16 -> globalIpv6.contains(bytes) && blockedIpv6.none { it.contains(bytes) }
            else -> false
        }
    }

    private fun isLocalScope(address: InetAddress): Boolean = listOf(
        address.isAnyLocalAddress,
        address.isLoopbackAddress,
        address.isLinkLocalAddress,
        address.isSiteLocalAddress,
        address.isMulticastAddress,
    ).any { it }
}
