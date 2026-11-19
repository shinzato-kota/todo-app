package com.shinzato.todoapp.dto.task;

import com.shinzato.todoapp.enums.task.TaskPriority;

import java.time.LocalDate;

/**
 * 編集画面に表示するタスク1件分の、編集できる項目を持つDTO。
 *
 * @param id          タスクID
 * @param title       タイトル
 * @param description 詳細。未設定の場合はnull
 * @param dueDate     期限。未設定の場合はnull
 * @param priority    優先度
 * @param categoryId  カテゴリID。カテゴリなしの場合はnull
 */
public record TaskEditValues(Long id, String title, String description, LocalDate dueDate, TaskPriority priority,
        Long categoryId) {
}
