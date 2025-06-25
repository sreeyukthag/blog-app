package com.edstem.blogapp.service;

import com.edstem.blogapp.dto.PostDTO;
import com.edstem.blogapp.entity.Post;
import com.edstem.blogapp.kafka.PostKafkaProducer;
import com.edstem.blogapp.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher publisher;
    private final PostKafkaProducer kafkaProducer;

    public PostDTO createPost(PostDTO postDTO) {
        try {
            Post post = Post.builder()
                    .title(postDTO.getTitle())
                    .content(postDTO.getContent())
                    .author(postDTO.getAuthor())
                    .build();

            Post saved = postRepository.save(post);

            // Clear cached list
            redisTemplate.delete("post:all");

            // ✅ Publish PostDTO as Kafka message
            kafkaProducer.sendPostCreatedMessage(toDTO(saved));

            return toDTO(saved);
        } catch (DataAccessException e) {
            throw new RuntimeException("Error saving post to database: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred while creating post: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "posts", key = "#id")
    public PostDTO getPostById(Long id) {
        return postRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
    }

    public List<PostDTO> getAllPosts() {
        String key = "post:all";
        List<PostDTO> result;

        try {
            Object cached = redisTemplate.opsForValue().get(key);

            if (cached instanceof List<?>) {
                List<?> cachedList = (List<?>) cached;
                if (!cachedList.isEmpty() && cachedList.get(0) instanceof Post) {
                    System.out.println("Fetched from Redis cache");
                    return cachedList.stream().map(p -> toDTO((Post) p)).collect(Collectors.toList());
                }
            }

            List<Post> posts = postRepository.findAll();
            redisTemplate.opsForValue().set(key, posts, Duration.ofSeconds(10));
            result = posts.stream().map(this::toDTO).collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Redis fetch error, falling back to DB: " + e.getMessage());
            List<Post> posts = postRepository.findAll();
            result = posts.stream().map(this::toDTO).collect(Collectors.toList());
        }

        return result;
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
