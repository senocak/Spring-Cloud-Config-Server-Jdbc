package com.github.senocak.sccsj.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.history.Revision
import org.springframework.data.history.RevisionMetadata
import org.springframework.http.HttpStatus

@JsonIgnoreProperties(ignoreUnknown = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
abstract class BaseDto

data class PropertyDTO(
    val id: UUID,
    val application: String,
    val profile: String,
    val label: String,
    val pkey: String,
    val pvalue: String,
    val createdAt: Long,
    val updatedAt: Long
): BaseDto() {
    var revisions: RevisionPaginationDTO? = null
}

data class RevisionDTO(
    val revisionType: RevisionMetadata.RevisionType,
    val revisionInstant: Long,
    val data: PropertyDTO
): BaseDto()

class RevisionPaginationDTO(
    pageModel: Page<Revision<Long, Property>>,
    items: List<RevisionDTO>,
): PaginationResponse<Revision<Long, Property>, RevisionDTO>(page = pageModel, items = items)

class PropertyPaginationDTO(
    pageModel: Page<Property>,
    items: List<PropertyDTO>,
    sortBy: String? = null,
    sort: String? = null
): PaginationResponse<Property, PropertyDTO>(page = pageModel, items = items, sortBy = sortBy, sort = sort)


object PageRequestBuilder {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    fun build(paginationCriteria: PaginationCriteria): PageRequest {
        if (paginationCriteria.page < 0) {
            "Page must be greater than or equal to 0!"
                .also { log.warn(it) }
                .run { throw ServerException(omaErrorMessageType = OmaErrorMessageType.BASIC_INVALID_INPUT,
                    variables = arrayOf(this), statusCode = HttpStatus.BAD_REQUEST) }
        }
        if (paginationCriteria.size < 1) {
            "Size must be greater than 0!"
                .also { log.warn(it) }
                .run { throw ServerException(omaErrorMessageType = OmaErrorMessageType.BASIC_INVALID_INPUT,
                    variables = arrayOf(this), statusCode = HttpStatus.BAD_REQUEST) }
        }
        val pageRequest: PageRequest = PageRequest.of(paginationCriteria.page, paginationCriteria.size)
        if (paginationCriteria.sortBy != null && paginationCriteria.sort != null) {
            val direction: Sort.Direction = when (paginationCriteria.sort) {
                "desc" -> Sort.Direction.DESC
                else -> Sort.Direction.ASC
            }
            if (paginationCriteria.columns.contains(element = paginationCriteria.sortBy))
                return pageRequest.withSort(Sort.by(direction, paginationCriteria.sortBy))
        }
        return pageRequest
    }
}


@JsonPropertyOrder("page", "pages", "total", "sort", "sortBy", "items")
open class PaginationResponse<T, P>(
    page: Page<T>,
    items: List<P>,

    @Schema(example = "id", description = "Sort by", required = true, name = "sortBy", type = "String")
    var sortBy: String? = null,

    @Schema(example = "asc", description = "Sort", required = true, name = "sort", type = "String")
    var sort: String? = null
) : BaseDto() {
    @Schema(example = "0", description = "Current page", required = true, name = "page", type = "String")
    var page: Int = page.number

    @Schema(example = "3", description = "Total pages", required = true, name = "pages", type = "String")
    var pages: Int = page.totalPages

    @Schema(example = "10", description = "Total elements", required = true, name = "total", type = "String")
    var total: Long = page.totalElements

    @ArraySchema(schema = Schema(description = "items", required = true, type = "ListDto"))
    var items: List<P>? = items

    override fun toString(): String = "PaginationResponse(page: $page, pages: $pages, total: $total, items: $items)"
}

data class PaginationCriteria(
    var page: Int,
    var size: Int
): BaseDto() {
    var sortBy: String? = null
    var sort: String? = null
    var columns: ArrayList<String> = arrayListOf()
}

fun Property.convertToDTO(): PropertyDTO =
    PropertyDTO(
        id = this.id!!,
        application = this.application,
        profile = this.profile,
        label = this.label,
        pkey = this.pkey,
        pvalue = this.pvalue,
        createdAt = this.createdAt.time,
        updatedAt = this.updatedAt.time,
    )

data class PropertiesCreateDTO(
    val application: String,
    val profile: String,
    val label: String,
    val pkey: String,
    val pvalue: String,
): BaseDto()

data class PropertiesUpdateDTO(
    var application: String? = null,
    var profile: String? = null,
    var label: String? = null,
    var pkey: String? = null,
    var pvalue: String? = null,
): BaseDto()

@JsonPropertyOrder("statusCode", "error", "variables")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName("exception")
class ExceptionDto : BaseDto() {
    var statusCode = 200
    var error: OmaErrorMessageTypeDto? = null
    var variables: Array<String?> = arrayOf(String())

    @JsonPropertyOrder("id", "text")
    class OmaErrorMessageTypeDto(val id: String? = null, val text: String? = null)
}

enum class OmaErrorMessageType(val messageId: String, val text: String) {
    MANDATORY_INPUT_MISSING("SVC0001", "Mandatory input %1 %2 is missing from request"),
    JSON_SCHEMA_VALIDATOR("SVC0002", "Schema failed."),
    BASIC_INVALID_INPUT("SVC0003", "Invalid input value for message part %1"),
    GENERIC_SERVICE_ERROR("SVC0004", "The following service error occurred: %1. Error code is %2"),
    NOT_FOUND("SVC0005", "Entry is not found")
}