package br.com.redhat.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "movies")
public class Movie extends PanacheEntity {

    public String name;
    @Column(name = "release_year")
    public String year;
    public String director;
    public String genre;

    public Movie() {}

    public Movie(String name, String year, String director, String genre) {
        this.name = name;
        this.year = year;
        this.director = director;
        this.genre = genre;
    }
}
