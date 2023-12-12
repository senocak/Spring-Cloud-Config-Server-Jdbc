package com.github.senocak.sccsj.domain

import org.springframework.http.HttpStatus


open class RestException(msg: String, t: Throwable? = null): Exception(msg, t)

class ServerException(var omaErrorMessageType: OmaErrorMessageType, var variables: Array<String?>, var statusCode: HttpStatus = HttpStatus.BAD_REQUEST):
    RestException(msg = "OmaErrorMessageType: $omaErrorMessageType, variables: $variables, statusCode: $statusCode")