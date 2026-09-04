package ch.hftm;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/blogs")
@ApplicationScoped
public class BlogResource {

    @Inject
    BlogService blogService;

    @GET
    public List<Blog> getBlogs() {
        return blogService.getBlogs();
    }

    @GET
    @Path("/{id}")
    public Blog getBlog(@PathParam("id") long id) {
        return blogService.getBlog(id);
    }

    @POST
    public void addBlog(Blog blog) {
        blogService.addBlog(blog);
    }

    @PUT
    @Path("/{id}")
    public void updateBlog(@PathParam("id") long id, Blog blog) {
        blogService.updateBlog(id, blog);
    }

    @DELETE
    @Path("/{id}")
    public void deleteBlog(@PathParam("id") long id) {
        blogService.deleteBlog(id);
    }
}