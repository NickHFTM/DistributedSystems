package ch.hftm.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateBlogRequest {

    @NotBlank(message = "Der Titel darf nicht leer sein")
    public String title;

    @NotBlank(message = "Der Inhalt darf nicht leer sein")
    public String content;
}