package br.com.redhat;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class StorageResourceTest {

    @Test
    void writeReadListDelete() {
        given().contentType(ContentType.TEXT).body("hello pvc")
                .when().put("/api/v1/storage/files/test.txt")
                .then().statusCode(201);

        given().when().get("/api/v1/storage/files/test.txt")
                .then().statusCode(200).body(equalTo("hello pvc"));

        given().when().get("/api/v1/storage/files")
                .then().statusCode(200).body("$", hasItem("test.txt"));

        given().when().delete("/api/v1/storage/files/test.txt")
                .then().statusCode(204);

        given().when().get("/api/v1/storage/files/test.txt")
                .then().statusCode(404);
    }

    @Test
    void rejectsPathTraversal() {
        given().urlEncodingEnabled(false).contentType(ContentType.TEXT).body("x")
                .when().put("/api/v1/storage/files/..%2Fescape.txt")
                .then().statusCode(400);
    }
}
