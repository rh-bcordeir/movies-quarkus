package br.com.redhat.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "directors")
public class Director extends PanacheEntity {

    public String name;
    public String nationality;
    @Column(name = "birth_year")
    public String birthYear;

    public Director() {}

    public Director(String name, String nationality, String birthYear) {
        this.name = name;
        this.nationality = nationality;
        this.birthYear = birthYear;
    }
}
