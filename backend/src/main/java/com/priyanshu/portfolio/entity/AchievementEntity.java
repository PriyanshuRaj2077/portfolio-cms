package com.priyanshu.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "achievements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementEntity {
    @Id
    private String id;

    private String title;
    private String metric;

    @Column(columnDefinition = "TEXT")
    private String descText;

    private String issuer;
    private String date;
    private String link;

    private Integer sortOrder;
}
