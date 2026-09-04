package ch.hftm;

import java.util.List;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BlogService {

    @Inject
    BlogRepository blogRepository;

    public List<Blog> getBlogs() {
        var blogs = blogRepository.getBlogs();
        Log.info("Returning " + blogs.size() + " blogs");
        return blogs;
    }

    public Blog getBlog(long id) {
        Log.info("Returning blog with id " + id);
        return blogRepository.getBlog(id);
    }

    public void addBlog(Blog blog) {
        Log.info("Adding blog " + blog.getTitle());
        blogRepository.addBlog(blog);
    }

    public void updateBlog(long id, Blog blog) {
        Log.info("Updating blog with id " + id);
        blogRepository.updateBlog(id, blog);
    }

    public void deleteBlog(long id) {
        Log.info("Deleting blog with id " + id);
        blogRepository.deleteBlog(id);
    }
}