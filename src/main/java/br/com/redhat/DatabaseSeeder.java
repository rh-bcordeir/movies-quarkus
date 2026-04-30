package br.com.redhat;


import br.com.redhat.entity.Movie;
import br.com.redhat.repository.MovieRepository;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DatabaseSeeder {

    @Inject
    MovieRepository repo;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (repo.count() > 0) return;
        repo.persist(new Movie("The Shawshank Redemption", "1994", "Frank Darabont", "Drama"));
        repo.persist(new Movie("Parasite", "2019", "Bong Joon-ho", "Drama"));
        repo.persist(new Movie("O Auto da Compadecida", "2000", "Guel Arraes", "Comedia"));
        repo.persist(new Movie("Superbad", "2007", "Greg Mottola", "Comedy"));
        repo.persist(new Movie("Mad Max: Fury Road", "2015", "George Miller", "Action"));
    }
}
