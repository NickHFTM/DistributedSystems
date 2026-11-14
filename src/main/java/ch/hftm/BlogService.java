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
            Log.debugf("Blogs werden nach Titel '%s' gesucht", title);

            List<Blog> blogs = blogRepository.searchBlogs(title);

            Log.debugf(
                    "%d Blogs für Suchbegriff '%s' gefunden",
                    blogs.size(),
                    title);

            return blogs;
        }

        List<Blog> blogs = blogRepository.getBlogs();

        Log.debugf("%d Blogs insgesamt geladen", blogs.size());

        return blogs;
    }

    public Blog getBlog(long id) {
        Log.debugf("Blog mit ID %d wird geladen", id);

        return blogRepository.getBlog(id);
    }

    public void addBlog(Blog blog) {
        Log.infof(
                "Blog '%s' für Owner '%s' wird persistiert",
                blog.getTitle(),
                blog.getOwner());

        blogRepository.addBlog(blog);
    }

    public boolean updateBlog(long id, Blog blog) {
        Log.infof("Blog mit ID %d wird aktualisiert", id);

        return blogRepository.updateBlog(id, blog);
    }

    public boolean deleteBlog(long id) {
        Log.infof("Blog mit ID %d wird gelöscht", id);

        return blogRepository.deleteBlog(id);
    }
}