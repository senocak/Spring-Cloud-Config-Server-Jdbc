package com.github.senocak.sccsj.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID
import org.hibernate.envers.AuditTable
import org.hibernate.envers.Audited
import org.hibernate.envers.DefaultRevisionEntity
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionListener

@Entity
@Table(name = "Property")
@Audited
@AuditTable(value = "Auited_Property")
class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null
    @Column lateinit var application: String
    @Column lateinit var profile: String
    @Column lateinit var label: String
    @Column lateinit var pkey: String
    @Column lateinit var pvalue: String
    @Column var createdAt: Date = Date()
    @Column var updatedAt: Date = Date()
}

@Entity
@RevisionEntity(AuditRevisionListener::class)
class AuditRevisionEntity : DefaultRevisionEntity() {
    var updatedAt: LocalDateTime? = null
}

class AuditRevisionListener : RevisionListener {
    override fun newRevision(revisionEntity: Any) {
        val auditRevisionEntity: AuditRevisionEntity = revisionEntity as AuditRevisionEntity
        auditRevisionEntity.updatedAt = LocalDateTime.now()
    }
}