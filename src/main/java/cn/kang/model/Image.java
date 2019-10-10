package cn.kang.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Image {
    private Integer id;
    private String name;
    private Integer chapter;
    private String url;

    public Image(String name, Integer chapter, String url) {
        this.name = name;
        this.chapter = chapter;
        this.url = url;
    }
}
