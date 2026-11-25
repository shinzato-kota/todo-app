package com.shinzato.todoapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * エンティティの作成日時と更新日時を、{@link Clock}の現在日時で入れる監査の設定。
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
public class JpaAuditingConfig {

    /**
     * 作成日時と更新日時に入れる日時として、{@link Clock}の現在日時を返す{@link DateTimeProvider}を返す。
     *
     * <p>
     * 作成日時・更新日時と、完了日時・「今日」の判定のタイムゾーンをそろえるため、同じ{@link Clock}を使う。
     *
     * @param clock 現在日時の取得に使うClock
     * @return Clockの現在日時を返すDateTimeProvider
     */
    @Bean
    public DateTimeProvider dateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }
}
