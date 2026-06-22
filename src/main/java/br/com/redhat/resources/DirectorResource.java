package br.com.redhat.resources;

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

import br.com.redhat.dto.DirectorCreatedDTO;
import br.com.redhat.dto.DirectorDTO;
import br.com.redhat.entity.Director;
import br.com.redhat.repository.DirectorRepository;

@Path("/api/v1/directors")
@Produces(MediaType.APPLICATION_JSON)
public class DirectorResource {

    @Inject
    DirectorRepository directorRepository;

    @GET
    public List<DirectorDTO> listAll() {
        return directorRepository.listAll().stream()
                .map(d -> new DirectorDTO(d.name, d.nationality, d.birthYear))
                .toList();
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(DirectorDTO dto) {
        Director director = new Director(dto.name(), dto.nationality(), dto.birthYear());
        directorRepository.persist(director);
        return Response.status(Response.Status.CREATED)
                .entity(new DirectorCreatedDTO(director.id, dto))
                .build();
    }
}
