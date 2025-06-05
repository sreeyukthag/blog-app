package com.edstem.blogapp.service;

import com.edstem.blogapp.dto.PostDTO;
import com.edstem.blogapp.entity.Post;
//import com.edstem.blogapp.event.PostCreatedEvent;
import com.edstem.blogapp.kafka.PostKafkaProducer;
import com.edstem.blogapp.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PostService {


    private final RedisTemplate<String, Object> redisTemplate;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher publisher;
    private final PostKafkaProducer kafkaProducer;

    public PostDTO createPost(PostDTO postDTO) {
        Post post = Post.builder()
                .title(postDTO.getTitle())
                .content(postDTO.getContent())
                .author(postDTO.getAuthor())
                .build();

        Post saved = postRepository.save(post);

        redisTemplate.delete("post:all");

//        publisher.publishEvent(new PostCreatedEvent(this, saved.getTitle()));

        kafkaProducer.sendPostCreatedMessage("New Post Created: " + saved.getTitle());

        return toDTO(saved);
    }

    @Cacheable(value = "posts", key = "#id")
    public PostDTO getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
        return toDTO(post);
    }

    public List<PostDTO> getAllPosts() {
        String key = "post:all";
        List<Post> posts = (List<Post>) redisTemplate.opsForValue().get(key);

        if (posts != null) {
            System.out.println("Fetched from Redis");
            return posts.stream().map(this::toDTO).collect(Collectors.toList());
        }

        posts = postRepository.findAll();
        redisTemplate.opsForValue().set(key, posts, Duration.ofMinutes(10));
        return posts.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @CacheEvict(value = "posts", key = "#id")
    public void deletePost(Long id) {
        if (!postRepository.existsById(id)) {
            throw new RuntimeException("Cannot delete. Post not found with id: " + id);
        }

        postRepository.deleteById(id);

        redisTemplate.delete("post:all");
    }

    private PostDTO toDTO(Post post) {
        return PostDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .author(post.getAuthor())
                .build();
    }
}
