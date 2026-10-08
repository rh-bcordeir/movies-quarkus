package br.com.redhat.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Test endpoint that reads/writes plain-text files in the directory where the PVC is mounted.
 */
@Path("/api/v1/storage/files")
public class StorageResource {

    @ConfigProperty(name = "storage.path")
    String storagePath;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> list() throws IOException {
        java.nio.file.Path dir = baseDir();
        try (Stream<java.nio.file.Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String read(@PathParam("name") String name) throws IOException {
        java.nio.file.Path file = resolve(name);
        if (!Files.isRegularFile(file)) {
            throw new NotFoundException();
        }
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    @PUT
    @Path("/{name}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response write(@PathParam("name") String name, String content) throws IOException {
        java.nio.file.Path file = resolve(name);
        boolean existed = Files.exists(file);
        Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8);
        return Response.status(existed ? Response.Status.NO_CONTENT : Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/{name}")
    public Response delete(@PathParam("name") String name) throws IOException {
        if (!Files.deleteIfExists(resolve(name))) {
            throw new NotFoundException();
        }
        return Response.noContent().build();
    }

    private java.nio.file.Path baseDir() throws IOException {
        return Files.createDirectories(Paths.get(storagePath).toAbsolutePath().normalize());
    }

    // Only plain file names are accepted, so callers cannot escape the storage directory.
    private java.nio.file.Path resolve(String name) throws IOException {
        java.nio.file.Path dir = baseDir();
        java.nio.file.Path file = dir.resolve(name).normalize();
        if (!file.getParent().equals(dir)) {
            throw new BadRequestException("Invalid file name");
        }
        return file;
    }
}
