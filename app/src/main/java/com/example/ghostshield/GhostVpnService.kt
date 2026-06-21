package com.example.ghostshield

import android.net.VpnService
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

class GhostVpnService : VpnService() {

    private val TAG = "UltimateGhost"
    private var isRunning = true

    private val BLOCKED_IPS = setOf(
        "101.32.143.171", "119.28.183.144", "150.109.28.183",
        "129.226.2.37", "203.205.137.232", "49.51.129.54",
        "150.109.29.150", "101.32.143.142", "119.28.145.130",
        "150.109.22.214", "129.226.3.232", "129.226.1.157"
    )

    private val BLOCKED_DOMAINS = setOf(
        "anticheatexpert.com", "anticheat.me", "igamecj.com",
        "log.pubgmobile.com", "report.pubgmobile.com", "tencent.com",
        "qq.com", "bugly.qq.com", "helpshift.com", "pubgmobile.com"
    )

    private fun poissonDelay(meanMs: Double = 50.0): Long {
        val lambda = 1.0 / meanMs
        val p = Random.nextDouble(0.0, 1.0)
        var x = 0
        var cumul = 0.0
        while (true) {
            cumul += Math.exp(-lambda) * Math.pow(lambda, x.toDouble()) / factorial(x)
            if (cumul >= p) break
            x++
        }
        return x.toLong()
    }

    private fun factorial(n: Int): Double {
        var result = 1.0
        for (i in 2..n) result *= i
        return result
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val builder = Builder()
        builder.addAddress("10.0.0.2", 32)
        builder.addRoute("0.0.0.0", 0)
        builder.setSession("Ultimate Ghost Shield")
        val interfaceFile = builder.establish() ?: return START_STICKY

        val input = FileInputStream(interfaceFile.fileDescriptor)
        val output = FileOutputStream(interfaceFile.fileDescriptor)

        Thread {
            val buffer = ByteArray(4096)
            while (isRunning) {
                val length = input.read(buffer)
                if (length > 0) {
                    val packetData = buffer.copyOfRange(0, length)
                    val packetBuffer = ByteBuffer.wrap(packetData).order(ByteOrder.BIG_ENDIAN)

                    val versionAndHeaderLen = packetBuffer.get(0).toInt()
                    val headerLen = (versionAndHeaderLen and 0x0F) * 4
                    val protocol = packetBuffer.get(9).toInt()

                    val destIpBytes = ByteArray(4)
                    packetBuffer.position(16)
                    packetBuffer.get(destIpBytes)
                    val destIp = InetSocketAddress(ByteBuffer.wrap(destIpBytes).int, 0).address.hostAddress

                    if (BLOCKED_IPS.contains(destIp)) {
                        val naturalDelay = poissonDelay(300.0)
                        Thread.sleep(naturalDelay)
                        Log.d(TAG, "[🔥] إسقاط IP ($destIp) بعد تأخير $naturalDelay مللي")
                        continue
                    }

                    if (protocol == 17) {
                        val dstPort = packetBuffer.getShort(headerLen + 2).toInt() and 0xFFFF
                        if (dstPort == 53) {
                            val dnsPayload = packetData.drop(headerLen + 8)
                            val domainName = extractDomainFromDnsRequest(dnsPayload)
                            if (domainName != null && BLOCKED_DOMAINS.any { domainName.contains(it) }) {
                                val fakeResponse = buildFakeDnsResponse(dnsPayload, domainName)
                                if (fakeResponse != null) {
                                    Thread.sleep(poissonDelay(20.0))
                                    output.write(fakeResponse)
                                    output.flush()
                                    Log.d(TAG, "[👻] تضليل DNS: $domainName")
                                    continue
                                }
                            }
                        }
                        if (dstPort == 853 || dstPort == 443) {
                            Thread.sleep(poissonDelay(100.0))
                            Log.d(TAG, "[🔒] منع DoH/DoT على $dstPort")
                            continue
                        }
                    }

                    if (Random.nextInt(1, 100) <= 15) {
                        val humanJitter = poissonDelay(30.0)
                        Thread.sleep(humanJitter)
                    }

                    output.write(packetData)
                    output.flush()
                }
            }
        }.start()

        return START_STICKY
    }

    private fun extractDomainFromDnsRequest(payload: ByteArray): String? {
        try {
            var pos = 12
            val domainBuilder = StringBuilder()
            while (true) {
                val len = payload[pos].toInt() and 0xFF
                if (len == 0) break
                pos++
                for (i in 0 until len) {
                    domainBuilder.append(payload[pos + i].toChar())
                }
                domainBuilder.append('.')
                pos += len
            }
            return domainBuilder.toString().dropLast(1)
        } catch (e: Exception) { return null }
    }

    private fun buildFakeDnsResponse(originalRequest: ByteArray, domain: String): ByteArray? {
        try {
            val response = ByteArray(1024)
            var pos = 0
            response[pos++] = originalRequest[0]
            response[pos++] = originalRequest[1]
            response[pos++] = 0x81.toByte()
            response[pos++] = 0x80.toByte()
            response[pos++] = 0x00; response[pos++] = 0x01
            response[pos++] = 0x00; response[pos++] = 0x01
            val questionPart = originalRequest.sliceArray(12 until (12 + originalRequest.size - 12))
            System.arraycopy(questionPart, 0, response, pos, questionPart.size)
            pos += questionPart.size
            response[pos++] = 0xC0.toByte(); response[pos++] = 0x0C.toByte()
            response[pos++] = 0x00; response[pos++] = 0x01
            response[pos++] = 0x00; response[pos++] = 0x01
            response[pos++] = 0x00; response[pos++] = 0x00
            response[pos++] = 0x00; response[pos++] = 0x3C
            response[pos++] = 0x00; response[pos++] = 0x04
            response[pos++] = 0x7F.toByte(); response[pos++] = 0x00.toByte()
            response[pos++] = 0x00.toByte(); response[pos++] = 0x01.toByte()
            return response.copyOf(pos)
        } catch (e: Exception) { return null }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }
}
