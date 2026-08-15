package com.priyanshu.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String location;
    private String email;
    private String githubUrl;
    private String linkedinUrl;
    private String twitterUrl;
    private String avatarUrl;
}
