package ch.hftm;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BlogRepository implements PanacheRepository<Blog> {

    public List<Blog> getBlogs() {
        return listAll();
    }

    public Blog getBlog(long id) {
        return findById(id);
    }

    @Transactional
    public void addBlog(Blog blog) {
        Log.info("Adding blog " + blog.getTitle());
        persist(blog);
    }

    @Transactional
    public void updateBlog(long id, Blog updatedBlog) {
        Blog blog = findById(id);

        if (blog != null) {
            blog.setTitle(updatedBlog.getTitle());
            blog.setContent(updatedBlog.getContent());
        }
    }

    @Transactional
    public void deleteBlog(long id) {
        deleteById(id);
    }
}
