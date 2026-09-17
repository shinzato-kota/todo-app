package com.shinzato.todoapp.dto.task;

import com.shinzato.todoapp.entity.task.Task;
import com.shinzato.todoapp.enums.task.TaskStatus;

/**
 * タスク一覧の1件分の表示項目を持つDTO。
 *
 * @param id     タスクID
 * @param title  タイトル
 * @param status ステータス
 */
public record TaskSummary(Long id, String title, TaskStatus status) {

    /**
     * エンティティから一覧表示用のDTOを生成する。
     *
     * @param task 変換元のタスク
     * @return 生成したDTO
     */
    public static TaskSummary from(Task task) {
        return new TaskSummary(task.getId(), task.getTitle(), task.getStatus());
    }
}
