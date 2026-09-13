package ch.hftm;

import java.util.List;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BlogService {

    @Inject
    BlogRepository blogRepository;

    public List<Blog> getBlogs(String title) {

        if (title != null && !title.isBlank()) {
            Log.info("Searching blogs with title: " + title);
            return blogRepository.searchBlogs(title);
        }

        var blogs = blogRepository.getBlogs();
        Log.info("Returning " + blogs.size() + " blogs");

        return blogs;
    }

    public Blog getBlog(long id) {
        return blogRepository.getBlog(id);
    }

    public void addBlog(Blog blog) {
        Log.info("Adding blog " + blog.getTitle());
        blogRepository.addBlog(blog);
    }

    public boolean updateBlog(long id, Blog blog) {
        Log.info("Updating blog " + id);
        return blogRepository.updateBlog(id, blog);
    }

    public boolean deleteBlog(long id) {
        Log.info("Deleting blog " + id);
        return blogRepository.deleteBlog(id);
    }
}