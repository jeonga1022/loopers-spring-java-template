package com.loopers.infrastructure.ranking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RankingRedisService {

    private static final String KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisTemplate<String, String> redisTemplate;

    private static final double VIEW_SCORE = 0.1;
    private static final double LIKE_SCORE = 0.2;
    private static final double ORDER_SCORE = 0.6;

    public List<RankingEntry> getTopProducts(LocalDate date, int offset, int limit) {
        String key = generateKey(date);

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, offset, offset + limit - 1);

        List<RankingEntry> entries = new ArrayList<>();
        if (tuples != null) {
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                Long productId = Long.parseLong(tuple.getValue());
                Double score = tuple.getScore();
                entries.add(new RankingEntry(productId, score));
            }
        }
        return entries;
    }

    public void incrementScoreForView(LocalDate date, Long productId) {
        String key = generateKey(date);
        redisTemplate.opsForZSet().incrementScore(key, String.valueOf(productId), VIEW_SCORE);
    }

    public void incrementScoreForLike(LocalDate date, Long productId) {
        String key = generateKey(date);
        redisTemplate.opsForZSet().incrementScore(key, String.valueOf(productId), LIKE_SCORE);
    }

    public void incrementScoreForOrder(LocalDate date, Long productId, int quantity) {
        String key = generateKey(date);
        redisTemplate.opsForZSet().incrementScore(key, String.valueOf(productId), ORDER_SCORE * quantity);
    }

    private String generateKey(LocalDate date) {
        return KEY_PREFIX + date.format(DATE_FORMATTER);
    }
}
