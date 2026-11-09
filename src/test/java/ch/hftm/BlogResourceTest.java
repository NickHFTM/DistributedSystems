package ch.hftm;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
public class BlogResourceTest {

    @Test
    @TestSecurity(user = "author1", roles = {"author"})
    void testCreateBlog() {

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "title": "REST Assured Test",
                  "content": "Dieser Blog wurde im Test erstellt."
                }
                """)
        .when()
            .post("/blogs")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("title", equalTo("REST Assured Test"))
            .body(
                "content",
                equalTo("Dieser Blog wurde im Test erstellt.")
            );
    }

    @Test
    @TestSecurity(user = "author1", roles = {"author"})
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