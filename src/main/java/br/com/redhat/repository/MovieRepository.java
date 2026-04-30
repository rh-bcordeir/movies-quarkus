package br.com.redhat.repository;

import br.com.redhat.entity.Movie;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class MovieRepository implements PanacheRepository<Movie> {

    public List<Movie> findByGenre(String genreKey) {
        String key = genreKey.trim().toLowerCase(Locale.ROOT);
        if ("comedia".equals(key)) {
            return find("lower(genre) in ('comedia', 'comedy')").list();
        }
        return find("lower(genre) = ?1", key).list();
    }
}
