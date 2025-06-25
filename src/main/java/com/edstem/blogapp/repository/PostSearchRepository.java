package com.edstem.blogapp.repository;

import com.edstem.blogapp.entity.PostDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, String> {
    List<PostDocument> findByTitleContainingOrContentContaining(String title, String content);
}
