package ch.hftm;

import java.util.List;
import java.util.Map;

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
import ch.hftm.dto.CreateBlogRequest;
import ch.hftm.dto.BlogResponse;

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
        return blogService.getBlogs(title);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getBlog(@PathParam("id") long id) {

        Blog blog = blogService.getBlog(id);

        if (blog == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Blog nicht gefunden",
                            "message", "Es existiert kein Blog mit der ID " + id
                    ))
                    .build();
        }

        return Response.ok(blog).build();
    }

    @POST
    @RolesAllowed({"author", "admin"})
    public Response addBlog(@Valid CreateBlogRequest request) {

        Blog blog = new Blog(request.title, request.content);

        String username = securityIdentity.getPrincipal().getName();
        blog.setOwner(username);

        blogService.addBlog(blog);

        BlogResponse response = new BlogResponse(
                blog.getId(),
                blog.getTitle(),
                blog.getContent()
        );

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

        Blog existingBlog = blogService.getBlog(id);

        if (existingBlog == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Blog nicht gefunden",
                            "message", "Es existiert kein Blog mit der ID " + id
                    ))
                    .build();
        }

        String username = securityIdentity.getPrincipal().getName();

        boolean isAdmin = securityIdentity.hasRole("admin");
        boolean isOwner = username.equals(existingBlog.getOwner());

        if (!isAdmin && !isOwner) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of(
                            "error", "Zugriff verweigert",
                            "message", "Du darfst nur deine eigenen Blogs bearbeiten"
                    ))
                    .build();
        }

        boolean updated = blogService.updateBlog(id, updatedBlog);

        if (!updated) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Blog nicht gefunden",
                            "message", "Blog mit ID " + id
                                    + " konnte nicht aktualisiert werden"
                    ))
                    .build();
        }

        return Response.ok(updatedBlog).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    public Response deleteBlog(@PathParam("id") long id) {

        boolean deleted = blogService.deleteBlog(id);

        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Blog nicht gefunden",
                            "message", "Blog mit ID " + id
                                    + " konnte nicht gelöscht werden"
                    ))
                    .build();
        }

        return Response.noContent().build();
    }
}