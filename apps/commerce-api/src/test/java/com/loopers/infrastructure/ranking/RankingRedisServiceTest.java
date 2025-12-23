package com.loopers.infrastructure.ranking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RankingRedisServiceTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RankingRedisService rankingRedisService;

    private static final LocalDate TODAY = LocalDate.now();
    private static final String KEY = "ranking:all:" + TODAY.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    @BeforeEach
    void setUp() {
        redisTemplate.delete(KEY);
    }

    @Test
    @DisplayName("상위 N개 상품을 점수 높은 순으로 조회한다")
    void getTopProducts() {
        // arrange
        redisTemplate.opsForZSet().add(KEY, "1", 3.0);  // 상품1: 3점
        redisTemplate.opsForZSet().add(KEY, "2", 5.0);  // 상품2: 5점
        redisTemplate.opsForZSet().add(KEY, "3", 1.0);  // 상품3: 1점

        // act
        List<RankingEntry> result = rankingRedisService.getTopProducts(TODAY, 0, 3);

        // assert
        // 예상 순서: 상품2(5점) > 상품1(3점) > 상품3(1점)
        assertThat(result).hasSize(3);
        assertThat(result.get(0).productId()).isEqualTo(2L);
        assertThat(result.get(0).score()).isEqualTo(5.0);
        assertThat(result.get(1).productId()).isEqualTo(1L);
        assertThat(result.get(2).productId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("상품 조회 시 0.1점을 증가시킨다")
    void incrementScoreForView() {
        // arrange - 아무 데이터 없는 상태

        // act
        rankingRedisService.incrementScoreForView(TODAY, 1L);

        // assert
        Double score = redisTemplate.opsForZSet().score(KEY, "1");
        assertThat(score).isEqualTo(0.1);
    }

    @Test
    @DisplayName("상품 조회 시 기존 점수에 0.1점을 누적한다")
    void incrementScoreForView_accumulates() {
        // arrange - 이미 3.0점 있는 상태
        redisTemplate.opsForZSet().add(KEY, "1", 3.0);

        // act
        rankingRedisService.incrementScoreForView(TODAY, 1L);

        // assert
        Double score = redisTemplate.opsForZSet().score(KEY, "1");
        assertThat(score).isEqualTo(3.1);
    }

    @Test
    @DisplayName("좋아요 시 0.2점을 증가시킨다")
    void incrementScoreForLike() {
        // act
        rankingRedisService.incrementScoreForLike(TODAY, 1L);

        // assert
        Double score = redisTemplate.opsForZSet().score(KEY, "1");
        assertThat(score).isEqualTo(0.2);
    }
}
