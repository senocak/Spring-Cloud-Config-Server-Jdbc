package org.github.senocak.sccsjc

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.cloud.context.environment.EnvironmentChangeEvent
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@RestController
//@RefreshScope
class Client1Application(
    private val appConfig: AppConfig
){
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    @Value("\${spring.h2.console.enabled}")
    private var h2Enabled: Boolean? = null

    @GetMapping("/")
    fun getH2Enabled(): Boolean? = h2Enabled.also { println("appConfig: $appConfig") }

    @EventListener(value = [ApplicationReadyEvent::class])
    fun applicationReadyEvent(): Unit =
        log.info("[ApplicationReadyEvent]: Is h2 enable: $h2Enabled")

    @EventListener(value = [EnvironmentChangeEvent::class])
    fun environmentChangeEvent(event: EnvironmentChangeEvent): Unit =
        log.info("[EnvironmentChangeEvent: $event]: Is h2 enable: $h2Enabled")

    @EventListener(value = [RefreshScopeRefreshedEvent::class])
    fun refreshScopeRefreshedEvent(event: RefreshScopeRefreshedEvent): Unit =
        log.info("[RefreshScopeRefreshedEvent: $event]: Is h2 enable: $h2Enabled")

}

fun main(args: Array<String>) {
    runApplication<Client1Application>(*args)
}

@Component
@RefreshScope
@ConfigurationProperties(prefix = "spring.h2.console")
class AppConfig {
    var enabled: String? = null

    override fun toString(): String = "AppConfig(enabled=$enabled)"
}