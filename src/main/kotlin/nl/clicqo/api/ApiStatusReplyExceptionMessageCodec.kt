package nl.clicqo.api

import io.netty.util.CharsetUtil
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import nl.clicqo.web.HttpStatus
import java.nio.charset.Charset

class ApiStatusReplyExceptionMessageCodec : MessageCodec<ApiStatusReplyException, ApiStatusReplyException> {
  private companion object {
    val CHARSET: Charset = CharsetUtil.UTF_8
  }

  private fun encodeString(
    buffer: Buffer,
    message: String,
  ) {
    val messageArr = message.toByteArray(CHARSET)
    buffer.appendInt(messageArr.size)
    buffer.appendBytes(messageArr)
  }

  private fun decodeString(
    pos: Array<Int>,
    buffer: Buffer,
  ): String {
    val asMessageLen = buffer.getInt(pos[0])
    pos[0] += 4
    val asMessage = String(buffer.getBytes(pos[0], pos[0] + asMessageLen), CHARSET)
    pos[0] += asMessageLen
    return asMessage
  }

  private fun encodeApiStatus(
    buffer: Buffer,
    apiStatus: ApiStatus,
  ) {
    buffer.appendInt(apiStatus.code)
    encodeString(buffer, apiStatus.message)
    encodeString(buffer, apiStatus.httpStatus.name)
  }

  private fun decodeApiStatus(
    pos: Array<Int>,
    buffer: Buffer,
  ): ApiStatus {
    val apiStatusCode = buffer.getInt(pos[0])
    pos[0] += 4
    val asMessage = decodeString(pos, buffer)
    val asHttpStatus = decodeString(pos, buffer)
    return ApiStatus(apiStatusCode, asMessage, HttpStatus.valueOf(asHttpStatus))
  }

  private fun encodeCause(
    buffer: Buffer,
    exception: Throwable?,
  ) {
    val cause = exception?.cause
    if (cause == null || cause == exception) {
      buffer.appendByte(0)
      return
    }
    when (cause) {
      is ApiStatus -> {
        buffer.appendByte(1)
        // First encode ApiStatus object
        encodeApiStatus(buffer, cause)
        // Encode our cause
        encodeCause(buffer, cause)
        // Encode our stacktrace
        encodeStacktrace(buffer, cause.stackTrace)
      }

      is ApiStatusReplyException -> {
        buffer.appendByte(2)
        encodeToWire(buffer, cause)
      }

      else -> {
        buffer.appendByte(-1)
        val eTypeName = cause.javaClass.simpleName
        encodeString(buffer, "$eTypeName: ${cause.message}")
        // Encode our stacktrace
        encodeStacktrace(buffer, cause.stackTrace)
      }
    }
  }

  private fun decodeCause(
    pos: Array<Int>,
    buffer: Buffer,
  ): Throwable? {
    val exceptionType = buffer.getByte(pos[0])
    pos[0]++
    when (exceptionType) {
      1.toByte() -> {
        val e = decodeApiStatus(pos, buffer)
        val cause = decodeCause(pos, buffer)
        if (cause != null) {
          e.initCause(cause)
        }
        e.stackTrace = decodeStacktrace(pos, buffer)
        return e
      }

      2.toByte() -> {
        return decodeFromWire(pos[0], buffer)
      }

      (-1).toByte() -> {
        val message = decodeString(pos, buffer)
        val cause = Throwable(message)
        cause.stackTrace = decodeStacktrace(pos, buffer)
        return cause
      }

      else -> return null
    }
  }

  private fun encodeStacktrace(
    buffer: Buffer,
    stackTrace: Array<StackTraceElement>,
  ) {
    buffer.appendInt(stackTrace.size)
    for (el in stackTrace) {
      encodeString(buffer, el.className)
      val fileName = el.fileName
      if (fileName != null) {
        buffer.appendByte(1)
        encodeString(buffer, fileName)
      } else {
        buffer.appendByte(0)
      }
      encodeString(buffer, el.methodName)
      buffer.appendInt(el.lineNumber)
    }
  }

  private fun decodeStacktrace(
    pos: Array<Int>,
    buffer: Buffer,
  ): Array<StackTraceElement> {
    val numStackTraceElements = buffer.getInt(pos[0])
    pos[0] += 4
    return Array(numStackTraceElements) {
      val declaringClass = decodeString(pos, buffer)
      val hasFilename = buffer.getByte(pos[0]) == 1.toByte()
      pos[0]++
      val filename = if (hasFilename) decodeString(pos, buffer) else null
      val methodName = decodeString(pos, buffer)
      val lineNumber = buffer.getInt(pos[0])
      pos[0] += 4
      StackTraceElement(
        declaringClass,
        methodName,
        filename,
        lineNumber,
      )
    }
  }

  override fun encodeToWire(
    buffer: Buffer?,
    body: ApiStatusReplyException?,
  ) {
    if (buffer == null || body == null) {
      return
    }

    // First encode ApiStatus object
    encodeApiStatus(buffer, body.apiStatus)
    // Encode our custom message
    encodeString(buffer, body.message)
    // Encode source pointer (if available)
    val sourcePointer = body.sourcePointer
    if (sourcePointer != null) {
      buffer.appendByte(1)
      encodeString(buffer, sourcePointer)
    } else {
      buffer.appendByte(0)
    }
    // Encode the cause of the exception (if available)
    encodeCause(buffer, body)
    // Encode our stacktrace
    encodeStacktrace(buffer, body.stackTrace)
  }

  override fun decodeFromWire(
    pos: Int,
    buffer: Buffer?,
  ): ApiStatusReplyException {
    val currentPos = arrayOf(pos)
    if (buffer == null) {
      throw Exception("Did not receive buffer for decoding our ApiStatusReplyException")
    }

    // Decode ApiStatus object
    val apiStatus = decodeApiStatus(currentPos, buffer)

    // Decode our custom message
    val customMessage = decodeString(currentPos, buffer)

    // Decode source pointer (if available)
    val hasSourcePointer = buffer.getByte(currentPos[0]) == 1.toByte()
    currentPos[0]++
    val sourcePointer = if (hasSourcePointer) decodeString(currentPos, buffer) else null

    val cause = decodeCause(currentPos, buffer)
    val e = ApiStatusReplyException(apiStatus, customMessage, sourcePointer)
    if (cause != null) {
      e.initCause(cause)
    }
    e.stackTrace = decodeStacktrace(currentPos, buffer)
    return e
  }

  override fun transform(exception: ApiStatusReplyException?): ApiStatusReplyException =
    exception ?: throw IllegalArgumentException("Cannot transform null exception")

  override fun name(): String = ApiStatusReplyExceptionMessageCodec::class.java.simpleName

  override fun systemCodecID(): Byte = -1
}
