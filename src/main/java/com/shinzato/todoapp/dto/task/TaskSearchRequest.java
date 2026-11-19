package com.shinzato.todoapp.dto.task;

import com.shinzato.todoapp.enums.task.DueDateCondition;
import com.shinzato.todoapp.enums.task.TaskPriority;
import com.shinzato.todoapp.enums.task.TaskStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 絞り込みAPIが、クエリ文字列で受け取る条件のDTO。
 *
 * @param status   ステータス
 * @param due      期限の条件
 * @param priority 優先度
 * @param keyword  キーワード
 * @param category カテゴリID、またはカテゴリなしを表す{@value #NO_CATEGORY}
 */
public record TaskSearchRequest(TaskStatus status, DueDateCondition due, TaskPriority priority,
        @Size(max = 100) String keyword,
        // Longに変えられない19桁以上の数を、変える前に400で止めるため、18桁までにする
        @Pattern(regexp = "\\d{1,18}|" + TaskSearchRequest.NO_CATEGORY) String category) {

    /** カテゴリの条件で、カテゴリなしを表す値 */
    public static final String NO_CATEGORY = "NONE";
}
