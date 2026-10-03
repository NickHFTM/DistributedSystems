package ch.hftm;

import java.util.List;
import java.util.Map;

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

    @GET
    public List<Blog> getBlogs(@QueryParam("title") String title) {
        return blogService.getBlogs(title);
    }

    @GET
    @Path("/{id}")
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
public Response addBlog(@Valid CreateBlogRequest request) {

    Blog blog = new Blog(request.title, request.content);

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
    public Response updateBlog(
            @PathParam("id") long id,
            @Valid Blog blog) {

        boolean updated = blogService.updateBlog(id, blog);

        if (!updated) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Blog nicht gefunden",
                            "message", "Blog mit ID " + id
                                    + " konnte nicht aktualisiert werden"
                    ))
                    .build();
        }

        return Response.ok(blog).build();
    }

    @DELETE
    @Path("/{id}")
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