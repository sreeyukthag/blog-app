package com.edstem.blogapp.dto;

import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDTO implements Serializable {
    private Long id;
    private String title;
    private String content;
    private String author;
}
