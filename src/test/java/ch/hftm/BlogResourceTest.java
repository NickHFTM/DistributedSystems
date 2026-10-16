package ch.hftm;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
public class BlogResourceTest {

    @Test
    void testCreateBlog() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "title": "REST Test",
                  "content": "Testinhalt"
                }
                """)
        .when()
            .post("/blogs")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("title", equalTo("REST Test"))
            .body("content", equalTo("Testinhalt"));
    }

    @Test
    void testInvalidBlog() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "title": "",
                  "content": ""
                }
                """)
        .when()
            .post("/blogs")
        .then()
            .statusCode(400);
    }
}