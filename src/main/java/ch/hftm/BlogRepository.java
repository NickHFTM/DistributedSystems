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

    public List<Blog> searchBlogs(String title) {
        return find(
                "lower(title) like ?1",
                "%" + title.toLowerCase() + "%"
        ).list();
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
    public boolean updateBlog(long id, Blog updatedBlog) {

        Blog blog = findById(id);

        if (blog == null) {
            return false;
        }

        blog.setTitle(updatedBlog.getTitle());
        blog.setContent(updatedBlog.getContent());

        return true;
    }

    @Transactional
    public boolean deleteBlog(long id) {
        return deleteById(id);
    }
}