package com.github.senocak.sccsj

import com.github.senocak.sccsj.domain.ExceptionDto
import com.github.senocak.sccsj.domain.OmaErrorMessageType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class RestExceptionHandler {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    @ExceptionHandler(ServerException::class)
    fun handleServerException(ex: ServerException): ResponseEntity<Any> =
        generateResponseEntity(httpStatus = ex.statusCode, omaErrorMessageType = ex.omaErrorMessageType, variables = ex.variables)

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<Any> =
        generateResponseEntity(httpStatus = HttpStatus.INTERNAL_SERVER_ERROR, variables = arrayOf(ex.message),
            omaErrorMessageType = OmaErrorMessageType.GENERIC_SERVICE_ERROR)

    /**
     * @param httpStatus -- returned code
     * @return -- returned body
     */
    private fun generateResponseEntity(httpStatus: HttpStatus, omaErrorMessageType: OmaErrorMessageType,
                                       variables: Array<String?>): ResponseEntity<Any> {
        log.error("Exception is handled. HttpStatus: $httpStatus, OmaErrorMessageType: $omaErrorMessageType, variables: ${variables.toList()}")
        val exceptionDto = ExceptionDto()
            .also {
                it.statusCode = httpStatus.value()
                it.error = ExceptionDto.OmaErrorMessageTypeDto(
                    id = omaErrorMessageType.messageId,
                    text = omaErrorMessageType.text
                )
                it.variables = variables
            }
        return ResponseEntity.status(httpStatus).body(exceptionDto)
    }
}