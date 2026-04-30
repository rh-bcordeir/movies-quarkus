package br.com.redhat;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MovieResourceTest {

    @Test
    @Order(1)
    void listAll_returnsSeededMovies() {
        given()
                .when()
                .get("/api/v1/movies")
                .then()
                .statusCode(200)
                .body("$", hasSize(5));
    }

    @Test
    @Order(2)
    void drama_returnsOnlyDrama() {
        given()
                .when()
                .get("/api/v1/movies/drama")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("[0].genre", equalTo("Drama"))
                .body("[1].genre", equalTo("Drama"));
    }

    @Test
    @Order(3)
    void comedia_returnsComediaAndComedy() {
        given()
                .when()
                .get("/api/v1/movies/comedia")
                .then()
                .statusCode(200)
                .body("$", hasSize(2));
    }

    @Test
    @Order(4)
    void post_createsMovie() {
        given()
                .contentType(ContentType.JSON)
                .body(
                        """
                        {
                          "name": "Test Film",
                          "year": "2024",
                          "director": "Tester",
                          "genre": "Drama"
                        }
                        """)
                .when()
                .post("/api/v1/movies")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("movie.name", equalTo("Test Film"));

        given()
                .when()
                .get("/api/v1/movies")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(6)));
    }
}
