package com.github.senocak.sccsj

import com.github.senocak.sccsj.domain.OmaErrorMessageType
import com.github.senocak.sccsj.domain.PageRequestBuilder
import com.github.senocak.sccsj.domain.PaginationCriteria
import com.github.senocak.sccsj.domain.PropertiesCreateDTO
import com.github.senocak.sccsj.domain.PropertiesUpdateDTO
import com.github.senocak.sccsj.domain.Property
import com.github.senocak.sccsj.domain.PropertyDTO
import com.github.senocak.sccsj.domain.PropertyPaginationDTO
import com.github.senocak.sccsj.domain.RevisionDTO
import com.github.senocak.sccsj.domain.convertToDTO
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.validation.constraints.Pattern
import java.sql.Timestamp
import java.time.Instant
import java.util.Optional
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.config.server.environment.EnvironmentRepository
import org.springframework.data.history.Revision
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RequestMapping("env")
@RestController
class IndexController(
    private val appConfigRepository: AppConfigRepository,
    private val environmentRepository: EnvironmentRepository
){
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping
    fun getAllAppConfigs(
        @Parameter(name = "page", description = "Page number", example = "0") @RequestParam(defaultValue = "1", required = false) page: Int,
        @Parameter(name = "size", description = "Page size", example = "20") @RequestParam(defaultValue = "\${spring.data.web.pageable.default-page-size:10}", required = false) size: Int,
        @Parameter(name = "sortBy", description = "Sort by column", example = "id") @RequestParam(defaultValue = "id", required = false) sortBy: String,
        @Parameter(name = "sort", description = "Sort direction", schema = Schema(type = "string", allowableValues = ["asc", "desc"])) @RequestParam(defaultValue = "asc", required = false) @Pattern(regexp = "asc|desc") sort: String,
        @Parameter(name = "q", description = "Search keyword", example = "lorem") @RequestParam(required = false) q: String?
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

    @GetMapping("/{id}")
    fun findById(
        @PathVariable(value = "id") id: UUID,
        @RequestParam(value = "revisions", defaultValue = "true") revisions: Boolean
    ): PropertyDTO =
        findByIdOrThrow(id = id)
            .convertToDTO()
            .also { it: PropertyDTO ->
                if (revisions) {
                    it.revisions = appConfigRepository.findRevisions(id).content
                        .map { c: Revision<Long, Property> ->
                            RevisionDTO(
                                revisionType = c.metadata.revisionType,
                                revisionInstant = c.metadata.revisionInstant.get().toEpochMilli(),
                                data = c.entity.convertToDTO()
                            )
                        }
                }
            }

    @GetMapping("/application/{application}")
    fun getAppConfigByApplication(@PathVariable(value = "application") application: String): List<PropertyDTO> =
        appConfigRepository.findByApplication(application = application)
            .map { it.convertToDTO() }

    @GetMapping("/application/{application}/profile/{profileName}")
    fun getAppConfigByApplicationAndModule(
        @PathVariable(value = "application") application: String,
        @PathVariable(value = "profileName") profile: String
    ): List<PropertyDTO> =
        appConfigRepository.findByApplicationAndProfile(application = application, profile = profile)
            .map { it.convertToDTO() }

    @GetMapping("/application/{application}/profile/{profileName}/label/{label}")
    fun getAppConfigByApplicationAndModuleAndConfigType(
        @PathVariable(value = "application") application: String,
        @PathVariable(value = "profileName") profile: String,
        @PathVariable(value = "label") label: String
    ): List<PropertyDTO> =
        appConfigRepository.findByApplicationAndProfileAndLabel(application = application, profile = profile, label = label)
            .map { it.convertToDTO() }
    //.also { environmentRepository.findOne(application, profile, label) }

    @GetMapping("/application/{application}/profile/{profileName}/label/{label}/key/{key}")
    fun findByApplicationAndProfileAndLabelAndPkey(
        @PathVariable(value = "application") application: String,
        @PathVariable(value = "profileName") profile: String,
        @PathVariable(value = "label") label: String,
        @PathVariable(value = "key") key: String
    ): String =
        (appConfigRepository.findByApplicationAndProfileAndLabelAndPkey(application = application, profile = profile, label = label, key = key)
            ?: throw ServerException(omaErrorMessageType = OmaErrorMessageType.NOT_FOUND, variables = arrayOf(), statusCode = HttpStatus.NOT_FOUND))
            .run { this.pvalue }

    @PostMapping
    fun createProperty(@RequestBody createDTO: PropertiesCreateDTO): ResponseEntity<PropertyDTO> =
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
    fun updatePropertyById(
        @PathVariable(value = "id") id: UUID,
        @RequestBody body: PropertiesUpdateDTO
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
    fun deleteProperty(@PathVariable(value = "id") id: UUID): ResponseEntity<Unit> =
        findByIdOrThrow(id = id)
            .run { appConfigRepository.delete(this) }
            .run { ResponseEntity.noContent().build() }


}