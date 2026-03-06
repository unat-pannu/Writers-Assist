package com.writersassist.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Script implements Serializable {
    private String id;
    private String title;
    private StringBuilder content;
    private List<String> scenes;

    public Script(String id, String title) {
        this.id = id;
        this.title = title;
        this.content = new StringBuilder();
        this.scenes = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content.toString();
    }

    public void setContent(String content) {
        this.content = new StringBuilder(content);
    }
}
