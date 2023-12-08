package com.github.senocak.sccsj

import com.github.senocak.sccsj.domain.Property
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.history.RevisionRepository

interface AppConfigRepository:
    RevisionRepository<Property, UUID, Long>,
    JpaRepository<Property, UUID>,
    CrudRepository<Property, UUID>,
    JpaSpecificationExecutor<Property>
{
    fun findByApplication(application: String): List<Property>
    fun findByApplicationAndProfile(application: String, profile: String): List<Property>
    fun findByApplicationAndProfileAndLabel(application: String, profile: String, label: String): List<Property>
    fun findByApplicationAndProfileAndLabelAndPkey(application: String, profile: String, label: String, key: String): Property?
}