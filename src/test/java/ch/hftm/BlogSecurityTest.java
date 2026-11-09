package ch.hftm;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
public class BlogSecurityTest {

    @Inject
    BlogRepository blogRepository;

    @Test
    void getBlogsIsPublic() {
        given()
        .when()
            .get("/blogs")
        .then()
            .statusCode(200);
    }

    @Test
    void postWithoutLoginIsRejected() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "title": "Nicht erlaubt",
                  "content": "Kein Login"
                }
                """)
        .when()
            .post("/blogs")
        .then()
            .statusCode(401);
    }

    @Test
    @TestSecurity(user = "reader1", roles = {"reader"})
    void readerCannotCreateBlog() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "title": "Reader Blog",
                  "content": "Reader darf keinen Blog erstellen"
                }
                """)
        .when()
            .post("/blogs")
        .then()
            .statusCode(403);
    }

    @Test
    @TestSecurity(user = "author1", roles = {"author"})
    void authorCanCreateBlog() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "title": "Author Blog",
                  "content": "Von author1 erstellt"
                }
                """)
        .when()
            .post("/blogs")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("title", equalTo("Author Blog"))
            .body("content", equalTo("Von author1 erstellt"));
    }

    @Test
    @TestSecurity(user = "admin1", roles = {"admin"})
    void adminCanCreateBlog() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "title": "Admin Blog",
                  "content": "Von admin1 erstellt"
                }
                """)
        .when()
            .post("/blogs")
        .then()
            .statusCode(201);
    }

    @Test
    @TestSecurity(user = "author2", roles = {"author"})
    void authorCannotUpdateForeignBlog() {

        Long blogId = QuarkusTransaction.requiringNew().call(() -> {
            Blog blog = new Blog(
                    "Blog von author1",
                    "Dieser Blog gehört author1"
            );

            blog.setOwner("author1");

            blogRepository.persist(blog);

            return blog.getId();
        });

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "title": "Manipulierter Blog",
                  "content": "author2 versucht den Blog zu ändern"
                }
                """)
        .when()
            .put("/blogs/" + blogId)
        .then()
            .statusCode(403)
            .body("error", equalTo("Zugriff verweigert"));
    }

    @Test
    @TestSecurity(user = "author1", roles = {"author"})
    void authorCanUpdateOwnBlog() {

        Long blogId = QuarkusTransaction.requiringNew().call(() -> {
            Blog blog = new Blog(
                    "Eigener Blog",
                    "Dieser Blog gehört author1"
            );

            blog.setOwner("author1");

            blogRepository.persist(blog);

            return blog.getId();
        });

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "title": "Eigener Blog geändert",
                  "content": "author1 darf den eigenen Blog ändern"
                }
                """)
        .when()
            .put("/blogs/" + blogId)
        .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin1", roles = {"admin"})
    void adminCanUpdateForeignBlog() {

        Long blogId = QuarkusTransaction.requiringNew().call(() -> {
            Blog blog = new Blog(
                    "Blog von author1",
                    "Dieser Blog gehört author1"
            );

            blog.setOwner("author1");

            blogRepository.persist(blog);

            return blog.getId();
        });

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "title": "Vom Admin geändert",
                  "content": "Admin darf fremde Blogs bearbeiten"
                }
                """)
        .when()
            .put("/blogs/" + blogId)
        .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "author1", roles = {"author"})
    void authorCannotDeleteBlog() {

        Long blogId = QuarkusTransaction.requiringNew().call(() -> {
            Blog blog = new Blog(
                    "Blog zum Löschen",
                    "Author darf ihn nicht löschen"
            );

            blog.setOwner("author1");

            blogRepository.persist(blog);

            return blog.getId();
        });

        given()
        .when()
            .delete("/blogs/" + blogId)
        .then()
            .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin1", roles = {"admin"})
    void adminCanDeleteBlog() {

        Long blogId = QuarkusTransaction.requiringNew().call(() -> {
            Blog blog = new Blog(
                    "Blog zum Löschen",
                    "Admin darf ihn löschen"
            );

            blog.setOwner("author1");

            blogRepository.persist(blog);

            return blog.getId();
        });

        given()
        .when()
            .delete("/blogs/" + blogId)
        .then()
            .statusCode(204);
    }
}