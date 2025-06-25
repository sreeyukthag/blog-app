package com.edstem.blogapp.controller;

import com.edstem.blogapp.dto.PostDTO;
import com.edstem.blogapp.service.PostSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class PostSearchController {

    private final PostSearchService postSearchService;

    @GetMapping
    public List<PostDTO> searchPosts(@RequestParam("q") String keyword) {
        return postSearchService.search(keyword);
    }
}
