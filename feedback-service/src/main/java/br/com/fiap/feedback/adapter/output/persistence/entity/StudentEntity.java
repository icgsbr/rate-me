package br.com.fiap.feedback.adapter.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** JPA mapping for the {@code student} table. */
@Entity
@Table(name = "student")
@Getter
@Setter
@NoArgsConstructor
public class StudentEntity {

    @Id
    private UUID id;

    private String name;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "auth_id")
    private UUID authId;
}
