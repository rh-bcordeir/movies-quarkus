package br.com.redhat;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

import br.com.redhat.dto.MovieCreatedDTO;
import br.com.redhat.dto.MovieDTO;
import br.com.redhat.entity.Movie;
import br.com.redhat.repository.MovieRepository;

@Path("/api/v1/movies")
@Produces(MediaType.APPLICATION_JSON)
public class MovieResource {

    @Inject
    MovieRepository movieRepository;

    @GET
    public List<MovieDTO> listAll() {
        return movieRepository.listAll().stream()
                .map(m -> new MovieDTO(m.name, m.year, m.director, m.genre))
                .toList();
    }

    @GET
    @Path("drama")
    public List<MovieDTO> drama() {
        return movieRepository.findByGenre("drama").stream()
                .map(m -> new MovieDTO(m.name, m.year, m.director, m.genre))
                .toList();
    }

    @GET
    @Path("comedia")
    public List<MovieDTO> comedia() {
        return movieRepository.findByGenre("comedia").stream()
                .map(m -> new MovieDTO(m.name, m.year, m.director, m.genre))
                .toList();
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(MovieDTO dto) {
        Movie movie = new Movie(dto.name(), dto.year(), dto.director(), dto.genre());
        movieRepository.persist(movie);
        return Response.status(Response.Status.CREATED)
                .entity(new MovieCreatedDTO(movie.id, dto))
                .build();
    }
}
