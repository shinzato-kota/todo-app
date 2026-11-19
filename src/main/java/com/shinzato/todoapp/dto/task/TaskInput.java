package com.shinzato.todoapp.dto.task;

import com.shinzato.todoapp.enums.task.TaskPriority;

import java.time.LocalDate;

/**
 * タスクの登録・更新で、サービスが受け取る入力のDTO。
 *
 * @param title       タイトル
 * @param description 詳細。未設定の場合はnull
 * @param dueDate     期限。未設定の場合はnull
 * @param priority    優先度
 * @param categoryId  カテゴリID。カテゴリなしの場合はnull
 */
public record TaskInput(String title, String description, LocalDate dueDate, TaskPriority priority,
        Long categoryId) {
}
