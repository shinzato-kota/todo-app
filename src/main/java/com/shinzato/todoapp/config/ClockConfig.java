package com.shinzato.todoapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 現在日時の取得に使う{@link Clock}を登録する設定。
 */
@Configuration
public class ClockConfig {

    /**
     * 日本時間の現在日時を返すClockを返す。
     *
     * <p>
     * サーバーのタイムゾーン設定によって「今日」が変わらないよう、タイムゾーンを明示する。
     *
     * @return 日本時間のClock
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Tokyo"));
    }
}
