package com.LTD.ltdWorksAPI.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Entity(name = "book")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "book", schema = "writings")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    @ElementCollection
    private List<String> author;
    private String year;
    private String description;
    private Integer pages;
    private String cover;
    @ElementCollection
    private List<String> genre;
    @ElementCollection
    private List<String> tags;
    private Integer prequelId;
    private Integer sequelId;
    @ElementCollection
    private List<Integer> chapterId;
    private String series;
}
