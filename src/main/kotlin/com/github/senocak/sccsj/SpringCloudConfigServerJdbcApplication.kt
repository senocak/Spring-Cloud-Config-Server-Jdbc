package com.github.senocak.sccsj

import com.github.senocak.sccsj.domain.Property
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.cloud.config.server.EnableConfigServer
import org.springframework.context.event.EventListener
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableConfigServer
@EnableJpaRepositories(repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean::class)
class SpringCloudConfigServerJdbcApplication(
    private val appConfigRepository: AppConfigRepository,
) {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

   @Value("\${spring.jpa.hibernate.ddl-auto}")
   private lateinit var ddl: String

    @EventListener(value = [ApplicationReadyEvent::class])
    fun init(): Unit =
        log.info("[ApplicationReadyEvent]: app is ready")
            .also { _: Unit ->
                if (ddl == "create" || ddl == "create-drop"){
                    appConfigRepository.saveAll(
                        listOf(
                            Property()
                                .also { it: Property ->
                                    it.application = "app-client"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "spring.h2.console.enabled"
                                    it.pvalue = "true"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "app-client"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "spring.datasource.url"
                                    it.pvalue = "jdbc:postgresql://\${SERVER_IP:localhost}:\${POSTGRESQL_PORT:5432}/\${POSTGRESQL_DB:sccsj}?currentSchema=\${POSTGRESQL_SCHEMA:public}"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "app-client"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "spring.datasource.username"
                                    it.pvalue = "\${POSTGRESQL_USER:postgres}"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "app-client"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "spring.datasource.password"
                                    it.pvalue = "\${POSTGRESQL_PASSWORD:senocak}"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "app-client"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "threadpool.min.size"
                                    it.pvalue = "10"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "app-client"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "threadpool.min.size"
                                    it.pvalue = "10"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "app-client"
                                    it.profile = "dev"
                                    it.label = "test"
                                    it.pkey = "threadpool.min.size"
                                    it.pvalue = "10"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "app-client"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "threadpool.size"
                                    it.pvalue = "50"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "app-client"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "app.scheduler.timeout"
                                    it.pvalue = "9000"
                                }
                        )
                    )
                        .run {
                            appConfigRepository.save(
                                appConfigRepository.findAll().first()
                                    .also { it: Property ->
                                        it.profile = "test"
                                    }
                            )
                        }
                }
            }
}

fun main(args: Array<String>) {
    runApplication<SpringCloudConfigServerJdbcApplication>(*args)
}
