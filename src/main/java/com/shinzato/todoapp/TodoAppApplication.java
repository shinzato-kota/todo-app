package com.shinzato.todoapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * アプリケーションの起動クラス。
 */
@SpringBootApplication
public class TodoAppApplication {

    /**
     * アプリケーションを起動する。
     *
     * @param args 起動時の引数
     */
    public static void main(String[] args) {
        SpringApplication.run(TodoAppApplication.class, args);
    }

}
