package com.github.senocak.sccsj

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringCloudConfigServerJdbcApplication

fun main(args: Array<String>) {
    runApplication<SpringCloudConfigServerJdbcApplication>(*args)
}
