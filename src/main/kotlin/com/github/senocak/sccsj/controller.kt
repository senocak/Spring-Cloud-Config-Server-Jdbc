package com.github.senocak.sccsj

import com.github.senocak.sccsj.domain.ExceptionDto
import com.github.senocak.sccsj.domain.OmaErrorMessageType
import com.github.senocak.sccsj.domain.PageRequestBuilder
import com.github.senocak.sccsj.domain.PaginationCriteria
import com.github.senocak.sccsj.domain.PropertiesCreateDTO
import com.github.senocak.sccsj.domain.PropertiesUpdateDTO
import com.github.senocak.sccsj.domain.Property
import com.github.senocak.sccsj.domain.PropertyDTO
import com.github.senocak.sccsj.domain.PropertyPaginationDTO
import com.github.senocak.sccsj.domain.RevisionDTO
import com.github.senocak.sccsj.domain.RevisionPaginationDTO
import com.github.senocak.sccsj.domain.ServerException
import com.github.senocak.sccsj.domain.convertToDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.validation.constraints.Pattern
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.config.server.environment.EnvironmentRepository
import org.springframework.context.SmartLifecycle
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.history.Revision
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RequestMapping("env")
@RestController
@Tag(name = "index", description = "Index Controller")
class IndexController(
    private val appConfigRepository: AppConfigRepository,
    private val environmentRepository: EnvironmentRepository
): SmartLifecycle{
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping
    @Operation(
        summary = "All Properties",
        tags = ["index"],
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = PropertyPaginationDTO::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = ExceptionDto::class))))
        ]
    )
    fun getAllAppConfigs(
        @Parameter(name = "page", description = "Page number", example = "0") @RequestParam(defaultValue = "0", required = false) page: Int,
        @Parameter(name = "size", description = "Page size", example = "20") @RequestParam(defaultValue = "\${spring.data.web.pageable.default-page-size:10}", required = false) size: Int,
        @Parameter(name = "sortBy", description = "Sort by column", example = "id") @RequestParam(defaultValue = "id", required = false) sortBy: String,
        @Parameter(name = "sort", description = "Sort direction", schema = Schema(type = "string", allowableValues = ["asc", "desc"])) @RequestParam(defaultValue = "asc", required = false) @Pattern(regexp = "asc|desc") sort: String,
        @Parameter(name = "q", description = "Search keyword", example = "lorem") @RequestParam(required = false) q: String?,
        @Parameter(name = "revisions", description = "Boolean revisions flag", example = "true") @RequestParam(value = "revisions", defaultValue = "false") revisions: Boolean,
        @Parameter(name = "revisionsPage", description = "Page number", example = "0") @RequestParam(defaultValue = "0", required = false) revisionsPage: Int,
        @Parameter(name = "revisionsSize", description = "Page size", example = "20") @RequestParam(defaultValue = "\${spring.data.web.pageable.default-page-size:10}", required = false) revisionsSize: Int,
    ): PropertyPaginationDTO =
        arrayListOf("id", "application", "profile", "label", "createdAt")
            .run {
                if (this.none { it == sortBy }) {
                    "Invalid sort column"
                        .also { log.error(it) }
                        .run error@ { throw ServerException(omaErrorMessageType = OmaErrorMessageType.BASIC_INVALID_INPUT,
                            variables = arrayOf(this@error), statusCode = HttpStatus.BAD_REQUEST) }
                }
                PaginationCriteria(page = page, size = size)
                    .also { it: PaginationCriteria ->
                        it.sortBy = sortBy
                        it.sort = sort
                        it.columns = this
                    }
                    .run paginationCriteria@ {
                        appConfigRepository.findAll(
                            createSpecificationForUser(q = q),
                            PageRequestBuilder.build(paginationCriteria = this@paginationCriteria)
                        )
                    }
                    .run messagePage@ {
                        PropertyPaginationDTO(
                            pageModel = this@messagePage,
                            items = this@messagePage.content.map { it: Property -> it.convertToDTO() }.toList(),
                            sortBy = sortBy,
                            sort = sort
                        )
                    }
            }
            .also { it: PropertyPaginationDTO ->
                if (revisions)
                    it.items?.map { pdto: PropertyDTO ->
                        val revisionPage: Page<Revision<Long, Property>> = appConfigRepository.findRevisions(pdto.id, PageRequest.of(revisionsPage, revisionsSize))
                        val revisionPaginationDTO: List<RevisionDTO> = revisionPage.content
                            .map { c: Revision<Long, Property> ->
                                RevisionDTO(
                                    revisionType = c.metadata.revisionType,
                                    revisionInstant = c.metadata.revisionInstant.get().toEpochMilli(),
                                    data = c.entity.convertToDTO()
                                )
                            }
                        pdto.revisions = RevisionPaginationDTO(pageModel = revisionPage, items = revisionPaginationDTO)
                    }
            }

    @GetMapping("/{id}")
    @Operation(
        summary = "Property by id",
        tags = ["index"],
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = PropertyDTO::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = ExceptionDto::class))))
        ]
    )
    fun findById(
        @Parameter(name = "id", description = "Property id", example = "27c343d6-76a7-44b4-b3ca-a40618b3bff4") @PathVariable(value = "id") id: UUID,
        @Parameter(name = "revisions", description = "Boolean revisions flag", example = "false") @RequestParam(value = "revisions", defaultValue = "false") revisions: Boolean,
        @Parameter(name = "page", description = "Page number", example = "0") @RequestParam(defaultValue = "0", required = false) page: Int,
        @Parameter(name = "size", description = "Page size", example = "20") @RequestParam(defaultValue = "\${spring.data.web.pageable.default-page-size:10}", required = false) size: Int,
    ): PropertyDTO =
        findByIdOrThrow(id = id)
            .convertToDTO()
            .also { it: PropertyDTO ->
                if (revisions) {
                    val revisionPage: Page<Revision<Long, Property>> = appConfigRepository.findRevisions(id, PageRequest.of(page, size))
                    val revisionPaginationDTO: List<RevisionDTO> = revisionPage.content
                        .map { c: Revision<Long, Property> ->
                            RevisionDTO(
                                revisionType = c.metadata.revisionType,
                                revisionInstant = c.metadata.revisionInstant.get().toEpochMilli(),
                                data = c.entity.convertToDTO()
                            )
                        }
                    it.revisions = RevisionPaginationDTO(pageModel = revisionPage, items = revisionPaginationDTO)
                }
            }

    @GetMapping("/application/{application}")
    @Operation(
        summary = "Property by application",
        tags = ["index"],
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = List::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = ExceptionDto::class))))
        ]
    )
    fun getAppConfigByApplication(
        @Parameter(name = "application", description = "Application name", example = "lorem") @PathVariable(value = "application") application: String
    ): List<PropertyDTO> =
        appConfigRepository.findByApplication(application = application)
            .map { it.convertToDTO() }

    @GetMapping("/application/{application}/profile/{profileName}")
    @Operation(
        summary = "Property by application and profileName",
        tags = ["index"],
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = List::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = ExceptionDto::class))))
        ]
    )
    fun getAppConfigByApplicationAndModule(
        @Parameter(name = "application", description = "Application name", example = "lorem") @PathVariable(value = "application") application: String,
        @Parameter(name = "profileName", description = "Profile name", example = "prof") @PathVariable(value = "profileName") profile: String
    ): List<PropertyDTO> =
        appConfigRepository.findByApplicationAndProfile(application = application, profile = profile)
            .map { it.convertToDTO() }

    @GetMapping("/application/{application}/profile/{profileName}/label/{label}")
    @Operation(
        summary = "Property by application and profileName and label",
        tags = ["index"],
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = List::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = ExceptionDto::class))))
        ]
    )
    fun getAppConfigByApplicationAndModuleAndConfigType(
        @Parameter(name = "application", description = "Application name", example = "lorem") @PathVariable(value = "application") application: String,
        @Parameter(name = "profileName", description = "Profile name", example = "prof") @PathVariable(value = "profileName") profile: String,
        @Parameter(name = "label", description = "Label name", example = "lab") @PathVariable(value = "label") label: String
    ): List<PropertyDTO> =
        appConfigRepository.findByApplicationAndProfileAndLabel(application = application, profile = profile, label = label)
            .map { it.convertToDTO() }
    //.also { environmentRepository.findOne(application, profile, label) }

    @GetMapping("/application/{application}/profile/{profileName}/label/{label}/key/{key}")
    @Operation(
        summary = "Property by application and profileName and label and key",
        tags = ["index"],
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = List::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = ExceptionDto::class))))
        ]
    )
    fun findByApplicationAndProfileAndLabelAndPkey(
        @Parameter(name = "application", description = "Application name", example = "lorem") @PathVariable(value = "application") application: String,
        @Parameter(name = "profileName", description = "Profile name", example = "prof") @PathVariable(value = "profileName") profile: String,
        @Parameter(name = "label", description = "Label name", example = "lab") @PathVariable(value = "label") label: String,
        @Parameter(name = "key", description = "Key", example = "ipsum") @PathVariable(value = "key") key: String
    ): String =
        (appConfigRepository.findByApplicationAndProfileAndLabelAndPkey(application = application, profile = profile, label = label, key = key)
            ?: throw ServerException(omaErrorMessageType = OmaErrorMessageType.NOT_FOUND, variables = arrayOf(), statusCode = HttpStatus.NOT_FOUND))
            .run { this.pvalue }

    @PostMapping
    @Operation(
        summary = "Create new property",
        tags = ["index"],
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = List::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = ExceptionDto::class))))
        ]
    )
    fun createProperty(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "RequestBody to create", required = true) @RequestBody createDTO: PropertiesCreateDTO
    ): ResponseEntity<PropertyDTO> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            appConfigRepository.save(Property()
                .also { it: Property ->
                    it.application = createDTO.application
                    it.profile = createDTO.profile
                    it.label = createDTO.label
                    it.pkey = createDTO.pkey
                    it.pvalue = createDTO.pvalue
                }).convertToDTO()
        )

    @PatchMapping("/{id}")
    @Operation(
        summary = "Update property by id",
        tags = ["index"],
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = List::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = ExceptionDto::class))))
        ]
    )
    fun updatePropertyById(
        @Parameter(name = "id", description = "Property id", example = "27c343d6-76a7-44b4-b3ca-a40618b3bff4") @PathVariable(value = "id") id: UUID,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "RequestBody to update", required = true) @RequestBody body: PropertiesUpdateDTO
    ): PropertyDTO =
        findByIdOrThrow(id = id)
            .run {
                if (body.application != null) this.application = body.application!!
                if (body.profile != null) this.profile = body.profile!!
                if (body.label != null) this.label = body.label!!
                if (body.pkey != null) this.pkey = body.pkey!!
                if (body.pvalue != null) this.pvalue = body.pvalue!!
                appConfigRepository.save(this)
            }
            .run { this.convertToDTO() }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete property by id",
        tags = ["index"],
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = List::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = ExceptionDto::class))))
        ]
    )
    fun deleteProperty(
        @Parameter(name = "id", description = "Property id", example = "27c343d6-76a7-44b4-b3ca-a40618b3bff4") @PathVariable(value = "id") id: UUID
    ): ResponseEntity<Unit> =
        findByIdOrThrow(id = id)
            .run { appConfigRepository.delete(this) }
            .run { ResponseEntity.noContent().build() }

    private fun createSpecificationForUser(q: String?): Specification<Property> {
        return Specification { root: Root<Property>, query: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder ->
            val predicates: MutableList<Predicate> = ArrayList()
            if (!q.isNullOrEmpty()) {
                val predicateApplication: Predicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("application")), "%${q.lowercase()}%")
                val predicateProfile: Predicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("profile")), "%${q.lowercase()}%")
                val predicateLabel: Predicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("label")), "%${q.lowercase()}%")
                val predicatePKey: Predicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("pkey")), "%${q.lowercase()}%")
                val predicatePValue: Predicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("pvalue")), "%${q.lowercase()}%")
                predicates.add(element = criteriaBuilder.or(predicateApplication, predicateProfile, predicateLabel, predicatePKey, predicatePValue))
            }
            query.where(*predicates.toTypedArray()).distinct(true).restriction
        }
    }

    private fun findByIdOrThrow(id: UUID): Property =
        appConfigRepository.findById(id)
            .orElseThrow {
                ServerException(omaErrorMessageType = OmaErrorMessageType.NOT_FOUND, variables = arrayOf(), statusCode = HttpStatus.NOT_FOUND)
            }

    private val running = AtomicBoolean(false)
    override fun start(): Unit = running.compareAndSet(false, true).run { log.info("start") }
    override fun stop(): Unit = running.compareAndSet(true, false).run { log.info("stop") }
    override fun isRunning(): Boolean = running.get().also { log.info("is runnign") }
}