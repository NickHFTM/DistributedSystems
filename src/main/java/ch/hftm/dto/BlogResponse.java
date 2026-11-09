package ch.hftm.dto;

public class BlogResponse {

    public Long id;
    public String title;
    public String content;

    public BlogResponse(Long id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }
}