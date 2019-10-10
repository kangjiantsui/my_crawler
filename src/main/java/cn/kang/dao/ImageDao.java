package cn.kang.dao;

import cn.kang.model.Image;

import java.util.List;

public interface ImageDao {
    List<Image> findAll();

    void addImage(Image image);

    void addImageList(List<Image> images);
}
