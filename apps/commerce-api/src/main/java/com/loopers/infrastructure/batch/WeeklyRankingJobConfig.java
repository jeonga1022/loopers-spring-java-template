package com.loopers.infrastructure.batch;

import com.loopers.infrastructure.ranking.ProductRankWeekly;
import com.loopers.infrastructure.ranking.ProductRankWeeklyRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class WeeklyRankingJobConfig {

    private static final int CHUNK_SIZE = 10;
    private static final int TOP_LIMIT = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final ProductRankWeeklyRepository productRankWeeklyRepository;

    public WeeklyRankingJobConfig(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  DataSource dataSource,
                                  ProductRankWeeklyRepository productRankWeeklyRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.dataSource = dataSource;
        this.productRankWeeklyRepository = productRankWeeklyRepository;
    }

    @Bean
    public Job weeklyRankingJob() {
        return new JobBuilder("weeklyRankingJob", jobRepository)
                .start(weeklyRankingStep())
                .build();
    }

    @Bean
    public Step weeklyRankingStep() {
        return new StepBuilder("weeklyRankingStep", jobRepository)
                .<ProductMetricsAggregation, ProductRankWeekly>chunk(CHUNK_SIZE, transactionManager)
                .reader(weeklyMetricsReader(null))
                .processor(weeklyRankingProcessor(null))
                .writer(weeklyRankingWriter(null))
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<ProductMetricsAggregation> weeklyMetricsReader(
            @Value("#{jobParameters['targetDate']}") String targetDateStr) {

        LocalDate targetDate = LocalDate.parse(targetDateStr, DATE_FORMATTER);
        LocalDate weekStart = targetDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("startDate", weekStart);
        parameterValues.put("endDate", weekEnd);

        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("score", Order.DESCENDING);

        RowMapper<ProductMetricsAggregation> rowMapper = (rs, rowNum) -> new ProductMetricsAggregation(
                rs.getLong("product_id"),
                rs.getLong("viewCount"),
                rs.getLong("likeCount"),
                rs.getLong("orderCount"),
                rs.getLong("totalQuantity"),
                rs.getLong("score")
        );

        return new JdbcPagingItemReaderBuilder<ProductMetricsAggregation>()
                .name("weeklyMetricsReader")
                .dataSource(dataSource)
                .selectClause("SELECT product_id, " +
                        "SUM(view_count) as viewCount, " +
                        "SUM(like_count) as likeCount, " +
                        "SUM(order_count) as orderCount, " +
                        "SUM(total_quantity) as totalQuantity, " +
                        "(SUM(view_count) + SUM(like_count) * 2 + SUM(order_count) * 6) as score")
                .fromClause("FROM product_metrics")
                .whereClause("WHERE date BETWEEN :startDate AND :endDate")
                .groupClause("GROUP BY product_id")
                .sortKeys(sortKeys)
                .parameterValues(parameterValues)
                .rowMapper(rowMapper)
                .pageSize(CHUNK_SIZE)
                .maxItemCount(TOP_LIMIT)
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<ProductMetricsAggregation, ProductRankWeekly> weeklyRankingProcessor(
            @Value("#{jobParameters['targetDate']}") String targetDateStr) {

        LocalDate targetDate = LocalDate.parse(targetDateStr, DATE_FORMATTER);
        LocalDate weekStart = targetDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        AtomicInteger rankCounter = new AtomicInteger(0);

        return aggregation -> ProductRankWeekly.create(
                aggregation.getProductId(),
                weekStart,
                weekEnd,
                rankCounter.incrementAndGet(),
                aggregation.getScore(),
                aggregation.getViewCount(),
                aggregation.getLikeCount(),
                aggregation.getOrderCount(),
                aggregation.getTotalQuantity()
        );
    }

    @Bean
    @StepScope
    public ItemWriter<ProductRankWeekly> weeklyRankingWriter(
            @Value("#{jobParameters['targetDate']}") String targetDateStr) {

        LocalDate targetDate = LocalDate.parse(targetDateStr, DATE_FORMATTER);
        LocalDate weekStart = targetDate.with(DayOfWeek.MONDAY);

        return items -> {
            if (!items.isEmpty()) {
                productRankWeeklyRepository.deleteByPeriodStart(weekStart);
                productRankWeeklyRepository.saveAll(items);
            }
        };
    }
}
