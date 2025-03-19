/*
 * Based on
 *   https://raw.githubusercontent.com/andrealaforgia/mjpeg-client/refs/heads/master/src/main/java/com/andrealaforgia/mjpegclient/MjpegRunner.java
 */
package org.ts.pnp_camera_server_client.mjpeg_stream

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.StringWriter
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URI
import java.util.*


class MjpegStream {
	var isOpened = false
		private set

	private var viewer: MjpegViewer? = null
	private var urlStream: InputStream? = null
	private var readTimeoutCounter = 0

	/**
	 * Open HTTP stream that contains the MJPEG stream
	 *
	 * @param viewer Object instance that provides required callbacks
	 * @param url MJPEG stream URL
	 */
	fun openStream(viewer: MjpegViewer, url: String) {
		if (isOpened) {
			closeStream()
		}

		//
		this.viewer = viewer

		// open the stream
		val urlConn = URI(url).toURL().openConnection().apply {
			readTimeout = 2000  // ms
			connect()
		}
		urlStream = urlConn.getInputStream()

		//
		readTimeoutCounter = 0

		// start retrieving images periodically
		isOpened = true
		val threadStatus = Thread(runnerGetNextFrameThread())
		threadStatus.isDaemon = false
		threadStatus.start()
	}

	/**
	 * Close HTTP stream that contains the MJPEG stream
	 */
	fun closeStream() {
		isOpened = false
		try {
			urlStream!!.close()
		} catch (ex: IOException) {
			System.err.println("MjpegStream.closeStream(): Failed to close stream: $ex")
		}
	}

	/**
	 * Runner for fetching/outputting next image frame
	 */
	private fun runnerGetNextFrameThread() = Runnable {
		while (isOpened) {
			try {
				Thread.sleep(1)
			} catch (e: InterruptedException) {
				Thread.interrupted()
				break
			}
			if (isOpened) {
				getAndOutputNextFrame()
			}
		}
	}

	/**
	 * Retrieves a single MJPEG image frame and passes it to the [viewer]
	 */
	private fun getAndOutputNextFrame() {
		var connectionLost = false

		try {
			val imageBytes = retrieveNextImage()
			if (! isOpened) {
				return
			}
			if (imageBytes.size < 100) {
				return
			}

			val bais = ByteArrayInputStream(imageBytes)
			viewer?.mjpegSetRawImageData(bais)
		} catch (ex: SocketTimeoutException) {
			if (isOpened) {
				if (++readTimeoutCounter == 5) {
					viewer?.mjpegLogError("Read from socket timeout: $ex")
					connectionLost = true
					readTimeoutCounter = 0
				}
			}
		} catch (ex: SocketException) {
			if (isOpened) {
				viewer?.mjpegLogError("Lost connection: $ex")
				connectionLost = true
			}
		} catch (ex: IOException) {
			if (isOpened) {
				viewer?.mjpegLogError("Failed to read stream: $ex")
				connectionLost = true
			}
		}

		if (connectionLost) {
			isOpened = false
			viewer?.mjpegLostConnection()
		}
	}

	/**
	 * Using the urlStream get the next JPEG image as a byte[]
	 *
	 * @return byte[] of the JPEG
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun retrieveNextImage(): ByteArray {
		// build headers
		//   (the D-Link DCS-930L camera stops its headers)
		var captureContentLength = false
		val contentLengthStringWriter = StringWriter(128)
		val headerWriter = StringWriter(128)

		var currByte = 0
		var contentLength = 0

		while (isOpened && (urlStream!!.read().also { currByte = it }) > -1) {
			if (captureContentLength) {
				if (currByte == 10 || currByte == 13) {
					contentLength = contentLengthStringWriter.toString().toInt()
					break
				}
				contentLengthStringWriter.write(currByte)
			} else {
				headerWriter.write(currByte)
				val tempString = headerWriter.toString()
				val indexOf = tempString.indexOf(CONTENT_LENGTH)
				if (indexOf > 0) {
					captureContentLength = true
				}
			}
		}
		if (! isOpened) {
			return ByteArray(0)
		}

		// 255 indicates the start of the jpeg image
		var tmpByte: Int
		while (isOpened) {
			// just skip extras
			tmpByte = urlStream!!.read()
			if (tmpByte == 255) {
				break
			}
			if (tmpByte == -1) {
				throw SocketException("Stream has ended")
			}
		}
		if (! isOpened) {
			return ByteArray(0)
		}

		// rest is the buffer
		val imageBytes = ByteArray(contentLength + 1)
		// since we ate the original 255, shove it back in
		imageBytes[0] = 255.toByte()
		var offset = 1
		var numRead = 0
		while (isOpened && offset < imageBytes.size &&
					(urlStream!!.read(imageBytes, offset, imageBytes.size - offset).also { numRead = it }) >= 0
				) {
			offset += numRead
		}
		if (! isOpened) {
			return ByteArray(0)
		}

		return imageBytes
	}

	// dirty but works: content-length parsing
	/*
	private fun contentLength(header: String): Int {
		val indexOfContentLength: Int = header.indexOf(CONTENT_LENGTH)
		val valueStartPos: Int = indexOfContentLength + CONTENT_LENGTH.length
		val indexOfEOL: Int = header.indexOf('\n', indexOfContentLength)

		val lengthValStr: String = header.substring(valueStartPos, indexOfEOL).trim()

		val retValue: Int = Integer.parseInt(lengthValStr)

		return retValue
	}
	*/

	companion object {
		private const val CONTENT_LENGTH = "Content-Length: "
		//private const val CONTENT_TYPE = "Content-type: image/jpeg"
	}
}
