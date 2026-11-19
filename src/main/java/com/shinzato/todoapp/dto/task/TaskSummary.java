package com.shinzato.todoapp.dto.task;

import com.shinzato.todoapp.dto.category.CategorySummary;
import com.shinzato.todoapp.enums.task.DueDateStatus;
import com.shinzato.todoapp.enums.task.TaskPriority;
import com.shinzato.todoapp.enums.task.TaskStatus;

import java.time.LocalDate;

/**
 * タスク一覧の1件分の表示項目を持つDTO。
 *
 * @param id            タスクID
 * @param title         タイトル
 * @param description   詳細。未設定の場合はnull
 * @param category      カテゴリ。カテゴリなしの場合はnull
 * @param priority      優先度
 * @param dueDate       期限。未設定の場合はnull
 * @param dueDateStatus 期限の状況
 * @param status        ステータス
 */
public record TaskSummary(Long id, String title, String description, CategorySummary category,
        TaskPriority priority, LocalDate dueDate, DueDateStatus dueDateStatus, TaskStatus status) {
}
