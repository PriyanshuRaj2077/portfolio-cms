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
    private String startDate;
    private String endDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String highlightsJson; // JSON array of string bullets

    private Integer sortOrder;
}
