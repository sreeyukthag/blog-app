package com.edstem.blogapp.service;

import com.edstem.blogapp.dto.PostDTO;
import com.edstem.blogapp.entity.PostDocument;
import com.edstem.blogapp.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostSearchService {

    private final PostSearchRepository postSearchRepository;

    public List<PostDTO> search(String keyword) {
        List<PostDocument> results = postSearchRepository.findByTitleContainingOrContentContaining(keyword, keyword);
        return results.stream()
                .map(doc -> PostDTO.builder()
                        .id(Long.parseLong(doc.getId()))
                        .title(doc.getTitle())
                        .content(doc.getContent())
                        .author(doc.getAuthor())
                        .build())
                .collect(Collectors.toList());
    }
}
