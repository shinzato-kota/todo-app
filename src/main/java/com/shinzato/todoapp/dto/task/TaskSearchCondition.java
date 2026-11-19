package com.shinzato.todoapp.dto.task;

import com.shinzato.todoapp.enums.task.DueDateCondition;
import com.shinzato.todoapp.enums.task.TaskPriority;
import com.shinzato.todoapp.enums.task.TaskStatus;

/**
 * タスク一覧の絞り込みで、サービスが受け取る条件のDTO。
 *
 * <p>
 * nullの項目は、その条件では絞り込まない。
 *
 * @param status   ステータス。絞り込まない場合はnull
 * @param due      期限の条件。絞り込まない場合はnull
 * @param priority 優先度。絞り込まない場合はnull
 * @param keyword  タイトルか詳細に含まれる文字列。絞り込まない場合はnull
 * @param category カテゴリの条件。絞り込まない場合はnull
 */
public record TaskSearchCondition(TaskStatus status, DueDateCondition due, TaskPriority priority, String keyword,
        CategoryCondition category) {
}
