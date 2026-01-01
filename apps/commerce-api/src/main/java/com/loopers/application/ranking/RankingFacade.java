package com.loopers.application.ranking;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.infrastructure.ranking.ProductRankMonthly;
import com.loopers.infrastructure.ranking.ProductRankMonthlyRepository;
import com.loopers.infrastructure.ranking.ProductRankWeekly;
import com.loopers.infrastructure.ranking.ProductRankWeeklyRepository;
import com.loopers.infrastructure.ranking.RankingEntry;
import com.loopers.infrastructure.ranking.RankingRedisService;
import com.loopers.interfaces.api.ranking.RankingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RankingFacade {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RankingRedisService rankingRedisService;
    private final ProductRepository productRepository;
    private final ProductRankWeeklyRepository productRankWeeklyRepository;
    private final ProductRankMonthlyRepository productRankMonthlyRepository;

    public RankingDto.RankingListResponse getRankings(String period, String dateStr, int page, int size) {
        return switch (period.toLowerCase()) {
            case "weekly" -> getWeeklyRankings(dateStr, page, size);
            case "monthly" -> getMonthlyRankings(dateStr, page, size);
            default -> getDailyRankings(dateStr, page, size);
        };
    }

    private RankingDto.RankingListResponse getDailyRankings(String dateStr, int page, int size) {
        LocalDate date = parseDate(dateStr);
        int offset = page * size;

        List<RankingEntry> entries = rankingRedisService.getTopProducts(date, offset, size);

        if (entries.isEmpty()) {
            return new RankingDto.RankingListResponse(List.of(), page, size, 0);
        }

        long totalCount = rankingRedisService.getTotalCount(date);

        List<Long> productIds = entries.stream()
                .map(RankingEntry::productId)
                .toList();

        Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<RankingDto.RankingResponse> rankings = new ArrayList<>();
        int rank = offset + 1;
        for (RankingEntry entry : entries) {
            Product product = productMap.get(entry.productId());
            if (product != null) {
                rankings.add(new RankingDto.RankingResponse(
                        rank++,
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        entry.score()
                ));
            }
        }

        return new RankingDto.RankingListResponse(rankings, page, size, totalCount);
    }

    private RankingDto.RankingListResponse getWeeklyRankings(String dateStr, int page, int size) {
        LocalDate date = parseDate(dateStr);
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);

        List<ProductRankWeekly> weeklyRanks = productRankWeeklyRepository
                .findByPeriodStartOrderByRankingAsc(weekStart);

        if (weeklyRanks.isEmpty()) {
            return new RankingDto.RankingListResponse(List.of(), page, size, 0);
        }

        int offset = page * size;
        int toIndex = Math.min(offset + size, weeklyRanks.size());
        if (offset >= weeklyRanks.size()) {
            return new RankingDto.RankingListResponse(List.of(), page, size, weeklyRanks.size());
        }

        List<ProductRankWeekly> pagedRanks = weeklyRanks.subList(offset, toIndex);

        List<Long> productIds = pagedRanks.stream()
                .map(ProductRankWeekly::getProductId)
                .toList();

        Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<RankingDto.RankingResponse> rankings = new ArrayList<>();
        for (ProductRankWeekly rank : pagedRanks) {
            Product product = productMap.get(rank.getProductId());
            if (product != null) {
                rankings.add(new RankingDto.RankingResponse(
                        rank.getRanking(),
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        rank.getScore().doubleValue()
                ));
            }
        }

        return new RankingDto.RankingListResponse(rankings, page, size, weeklyRanks.size());
    }

    private RankingDto.RankingListResponse getMonthlyRankings(String dateStr, int page, int size) {
        LocalDate date = parseDate(dateStr);
        YearMonth yearMonth = YearMonth.from(date);
        LocalDate monthStart = yearMonth.atDay(1);

        List<ProductRankMonthly> monthlyRanks = productRankMonthlyRepository
                .findByPeriodStartOrderByRankingAsc(monthStart);

        if (monthlyRanks.isEmpty()) {
            return new RankingDto.RankingListResponse(List.of(), page, size, 0);
        }

        int offset = page * size;
        int toIndex = Math.min(offset + size, monthlyRanks.size());
        if (offset >= monthlyRanks.size()) {
            return new RankingDto.RankingListResponse(List.of(), page, size, monthlyRanks.size());
        }

        List<ProductRankMonthly> pagedRanks = monthlyRanks.subList(offset, toIndex);

        List<Long> productIds = pagedRanks.stream()
                .map(ProductRankMonthly::getProductId)
                .toList();

        Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<RankingDto.RankingResponse> rankings = new ArrayList<>();
        for (ProductRankMonthly rank : pagedRanks) {
            Product product = productMap.get(rank.getProductId());
            if (product != null) {
                rankings.add(new RankingDto.RankingResponse(
                        rank.getRanking(),
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        rank.getScore().doubleValue()
                ));
            }
        }

        return new RankingDto.RankingListResponse(rankings, page, size, monthlyRanks.size());
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
}
