/*
 * Based on
 *   https://raw.githubusercontent.com/andrealaforgia/mjpeg-client/refs/heads/master/src/main/java/com/andrealaforgia/mjpegclient/MjpegRunner.java
 * Adapted and slightly improved by TS, Mar 2025
 */
package org.ts.javafx_camera_server_client.mjpeg_stream

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
			urlStream?.close()
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
		var hdValContentLength = 0

		// read headers
		//   (the D-Link DCS-930L camera stops its headers)
		var captureContentLength = false
		val headerValueWriter = StringWriter(128)
		val headerBufferWriter = StringWriter(128)
		var currByte = 0
		var headerByteCount = 0

		while (isOpened && (urlStream!!.read().also { currByte = it }) > -1) {
			if (! captureContentLength) {
				// keep reading the headers until HTTP_HD_CONTENT_LENGTH_STR was found
				headerBufferWriter.write(currByte)
				if (++headerByteCount > HTTP_HD_CONTENT_LENGTH_SLEN + HTTP_HD_CONTENT_TYPE_SLEN + MIME_TYPE_JPEG_SLEN) {
					val tmpBufString = headerBufferWriter.toString()
					val indexOf = tmpBufString.lowercase().indexOf(HTTP_HD_CONTENT_LENGTH_STR_LC)
					if (indexOf >= 0) {
						captureContentLength = true
					}
				}
			} else {
				if (currByte == 10 || currByte == 13) {
					val tmpBufString = headerBufferWriter.toString()
					val indexOf = tmpBufString.lowercase().indexOf(HTTP_HD_CONTENT_TYPE_STR_LC)
					if (indexOf < 0) {
						throw SocketException("Invalid data received from stream")
					}
					var tmpHdValContentType = tmpBufString.substring(indexOf)
					tmpHdValContentType = tmpHdValContentType.substring(0, tmpHdValContentType.indexOf('\n'))
					tmpHdValContentType = tmpHdValContentType.substring(tmpHdValContentType.indexOf(':') + 1)
					tmpHdValContentType = tmpHdValContentType.trim()
					if (tmpHdValContentType != MIME_TYPE_JPEG_STR) {
						throw SocketException("Invalid MIME-Type received from stream")
					}
					//
					hdValContentLength = headerValueWriter.toString().toInt()
					break
				}
				if (currByte != 32) {
					// we write only the bytes for the Content-Length into [headerValueWriter]
					headerValueWriter.write(currByte)
				}
			}
		}
		if (! isOpened || hdValContentLength < 100) {  // 100 bytes is an arbitrary value
			return ByteArray(0)
		}

		// 0xFF indicates the start of the jpeg image
		var tmpByte: Int
		while (isOpened) {
			// just skip extras
			tmpByte = urlStream!!.read()
			if (tmpByte == 0xFF) {
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
		val imageBytes = ByteArray(hdValContentLength + 1)
		// since we ate the original 0xFF, shove it back in
		imageBytes[0] = 0xFF.toByte()
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

	companion object {
		private val HTTP_HD_CONTENT_LENGTH_STR_LC = "Content-Length:".lowercase().trim()
		private val HTTP_HD_CONTENT_LENGTH_SLEN = HTTP_HD_CONTENT_LENGTH_STR_LC.length

		private val HTTP_HD_CONTENT_TYPE_STR_LC = "Content-Type:".lowercase().trim()
		private val HTTP_HD_CONTENT_TYPE_SLEN = HTTP_HD_CONTENT_TYPE_STR_LC.length

		private const val MIME_TYPE_JPEG_STR = "image/jpeg"
		private const val MIME_TYPE_JPEG_SLEN = MIME_TYPE_JPEG_STR.length
	}
}
