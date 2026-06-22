package br.com.redhat.repository;

import br.com.redhat.entity.Director;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DirectorRepository implements PanacheRepository<Director> {
}
