package com.github.senocak.sccsj

import com.github.senocak.sccsj.domain.Property
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.cloud.config.server.EnableConfigServer
import org.springframework.context.annotation.Bean
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

    @Bean
    fun customOpenAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Spring Cloud Config Server Rest Api - Kotlin")
                    .version("1.0.0")
                    .description("Jdbc based config server")
                    .termsOfService("https://github.com/senocak")
                    .license(License().name("Apache 2.0").url("https://springdoc.org"))
            )

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
                                    it.application = "client1"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "spring.h2.console.enabled"
                                    it.pvalue = "true"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "client1"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "spring.datasource.url"
                                    it.pvalue = "jdbc:postgresql://\${SERVER_IP:localhost}:\${POSTGRESQL_PORT:5432}/\${POSTGRESQL_DB:sccsj}?currentSchema=\${POSTGRESQL_SCHEMA:public}"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "client1"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "spring.datasource.username"
                                    it.pvalue = "\${POSTGRESQL_USER:postgres}"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "client1"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "spring.datasource.password"
                                    it.pvalue = "{cipher}AgByZjTnFIZf4PNPozMOVdNmf7Zc70Vx2oFhVJgS0ON9BW89FXyWP/1wmRcMer5J3821f5ah2GF0v7kx721MEtbUn28qOR37YIUleO4oye+RVQjDMCyeQtOZPFpvSWbKgAuYAHMmIWgGPrRhXAPRyFXTYJASR3Ruu6AO2bLZsYSexjphoASpPLyKMaKWGTefqP1RjABWShHsbcwelLsgwWe1UEJsOwDnRZAro+zOnnlcl/ORdIDhOvmxekXi5wyVZY+g/D7rnAr+Lz0yNLBbAnl3XDy8hIfuCaJci2okwHaWLoKBP9GDjckkznVGIyoveoVkQ0OefXJ9oYf11j/XPpwLP79pJbOvpWOEyrq28nKDz/oqHtYqXiZQBmHi84tVfhCk9rGvdivLhjRD+Jlmtx2+ELvxWDevH88OAkI5UVs7kd8rCgq2aQYHWDNU7VpPS2uQtOyqjB3DF7qNvWMIZ5+6u808Ia9oYUYbeAXcF4SnTlK5mkkBFYSWtJLG5joPccSMQfgqIyHtBAdVu5CFPSdfcYLzxK4ABAlBOWFlAdUMMV1Kkw3MHh8V3b5Rn+ClpD8ebCe8eH1LbNcF5RpwJQQ0+BqajEw6uGx6WN5tVFHFozu9HwBKSfkE07dcQK1b7A9hukIyDbCsvPQHSp1pw/L9C0RLvktAFxDaxqzdXRM6qQmnVABHZUFiMxpBTe/AcePEvp2j1zn9+60ipzgvExY7"
                                    //it.pvalue = "senocak"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "client2"
                                    it.profile = "test"
                                    it.label = "master"
                                    it.pkey = "threadpool.min.size"
                                    it.pvalue = "10"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "client2"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "threadpool.min.size"
                                    it.pvalue = "10"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "client2"
                                    it.profile = "dev"
                                    it.label = "test"
                                    it.pkey = "threadpool.min.size"
                                    it.pvalue = "10"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "client2"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "threadpool.size"
                                    it.pvalue = "50"
                                },
                            Property()
                                .also { it: Property ->
                                    it.application = "client2"
                                    it.profile = "dev"
                                    it.label = "master"
                                    it.pkey = "app.scheduler.timeout"
                                    it.pvalue = "9000"
                                }
                        )
                    )
                }
            }
}

fun main(args: Array<String>) {
    runApplication<SpringCloudConfigServerJdbcApplication>(*args)
}
