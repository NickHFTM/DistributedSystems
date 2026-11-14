package ch.hftm;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class BlogResourceIT {

    @Test
    void publicBlogsEndpointIsReachable() {
        given()
        .when()
            .get("/blogs")
        .then()
            .statusCode(200);
    }

    @Test
    void swaggerUiIsReachable() {
        given()
        .when()
            .get("/q/swagger-ui")
        .then()
            .statusCode(200);
    }

    @Test
    void protectedEndpointRejectsAnonymousUser() {
        given()
            .contentType("application/json")
            .body("""
                {
                  "title": "Integration Test",
                  "content": "Blackbox Test"
                }
                """)
        .when()
            .post("/blogs")
        .then()
            .statusCode(401);
    }
}