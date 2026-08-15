package com.priyanshu.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "experience")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperienceEntity {
    @Id
    private String id;

    private String role;
    private String company;
    private String location;
    private String startDate;
    private String endDate;

    @Column(name = "is_current_role")
    private Boolean currentRole;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String highlightsJson; // JSON array of string bullets

    private Integer sortOrder;
}
