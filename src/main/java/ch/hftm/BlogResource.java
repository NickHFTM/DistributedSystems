package ch.hftm;

import java.util.List;
import java.util.Map;

import ch.hftm.dto.BlogResponse;
import ch.hftm.dto.CreateBlogRequest;
import io.quarkus.logging.Log;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/blogs")
@ApplicationScoped
public class BlogResource {

    @Inject
    BlogService blogService;

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @PermitAll
    public List<Blog> getBlogs(@QueryParam("title") String title) {
        Log.debugf("GET /blogs aufgerufen, title=%s", title);

        List<Blog> blogs = blogService.getBlogs(title);

        Log.infof("GET /blogs erfolgreich, %d Blogs gefunden", blogs.size());

        return blogs;
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getBlog(@PathParam("id") long id) {
        Log.debugf("GET /blogs/%d aufgerufen", id);

        Blog blog = blogService.getBlog(id);

        if (blog == null) {
            Log.warnf("Blog mit ID %d wurde nicht gefunden", id);

            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Blog nicht gefunden",
                            "message", "Es existiert kein Blog mit der ID " + id))
                    .build();
        }

        Log.infof("Blog mit ID %d erfolgreich geladen", id);

        return Response.ok(blog).build();
    }

    @POST
    @RolesAllowed({"author", "admin"})
    public Response addBlog(@Valid CreateBlogRequest request) {
        String username = securityIdentity.getPrincipal().getName();

        Log.infof(
                "Benutzer '%s' erstellt einen neuen Blog mit Titel '%s'",
                username,
                request.title);

        Blog blog = new Blog(request.title, request.content);
        blog.setOwner(username);

        blogService.addBlog(blog);

        Log.infof(
                "Blog mit ID %d wurde von Benutzer '%s' erfolgreich erstellt",
                blog.getId(),
                username);

        BlogResponse response = new BlogResponse(
                blog.getId(),
                blog.getTitle(),
                blog.getContent());

        return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"author", "admin"})
    public Response updateBlog(
            @PathParam("id") long id,
            @Valid Blog updatedBlog) {

        String username = securityIdentity.getPrincipal().getName();

        Log.infof(
                "Benutzer '%s' versucht Blog %d zu aktualisieren",
                username,
                id);

        Blog existingBlog = blogService.getBlog(id);

        if (existingBlog == null) {
            Log.warnf(
                    "Benutzer '%s' wollte nicht existierenden Blog %d aktualisieren",
                    username,
                    id);

            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Blog nicht gefunden",
                            "message", "Es existiert kein Blog mit der ID " + id))
                    .build();
        }

        boolean isAdmin = securityIdentity.hasRole("admin");
        boolean isOwner = username.equals(existingBlog.getOwner());

        if (!isAdmin && !isOwner) {
            Log.warnf(
                    "Zugriff verweigert: Benutzer '%s' wollte fremden Blog %d von '%s' bearbeiten",
                    username,
                    id,
                    existingBlog.getOwner());

            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of(
                            "error", "Zugriff verweigert",
                            "message", "Du darfst nur deine eigenen Blogs bearbeiten"))
                    .build();
        }

        boolean updated = blogService.updateBlog(id, updatedBlog);

        if (!updated) {
            Log.warnf(
                    "Blog %d konnte von Benutzer '%s' nicht aktualisiert werden",
                    id,
                    username);

            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Blog nicht gefunden",
                            "message", "Blog mit ID " + id + " konnte nicht aktualisiert werden"))
                    .build();
        }

        Log.infof(
                "Blog %d wurde von Benutzer '%s' erfolgreich aktualisiert",
                id,
                username);

        return Response.ok(updatedBlog).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    public Response deleteBlog(@PathParam("id") long id) {
        String username = securityIdentity.getPrincipal().getName();

        Log.infof(
                "Admin '%s' versucht Blog %d zu löschen",
                username,
                id);

        boolean deleted = blogService.deleteBlog(id);

        if (!deleted) {
            Log.warnf(
                    "Admin '%s' wollte nicht existierenden Blog %d löschen",
                    username,
                    id);

            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Blog nicht gefunden",
                            "message", "Blog mit ID " + id + " konnte nicht gelöscht werden"))
                    .build();
        }

        Log.infof(
                "Blog %d wurde von Admin '%s' erfolgreich gelöscht",
                id,
                username);

        return Response.noContent().build();
    }
}