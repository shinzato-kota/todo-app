package com.shinzato.todoapp.mapper.task;

import com.shinzato.todoapp.dto.category.CategorySummary;
import com.shinzato.todoapp.dto.task.TaskEditValues;
import com.shinzato.todoapp.dto.task.TaskInput;
import com.shinzato.todoapp.dto.task.TaskStatusChangeResult;
import com.shinzato.todoapp.dto.task.TaskSummary;
import com.shinzato.todoapp.entity.category.Category;
import com.shinzato.todoapp.entity.task.Task;
import com.shinzato.todoapp.enums.task.DueDateStatus;
import com.shinzato.todoapp.mapper.category.CategoryEntityMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * タスクのエンティティと、サービスが受け取る・返すDTOとの間で詰め替える。
 */
@Component
public class TaskEntityMapper {

    private final CategoryEntityMapper categoryEntityMapper;

    public TaskEntityMapper(CategoryEntityMapper categoryEntityMapper) {
        this.categoryEntityMapper = categoryEntityMapper;
    }

    /**
     * 一覧表示用のDTOを生成する。
     *
     * @param task          変換元のタスク
     * @param dueDateStatus サービスで判定した、期限の状況
     * @return 生成したDTO
     * @throws NullPointerException taskまたはdueDateStatusがnullの場合
     */
    public TaskSummary toSummary(Task task, DueDateStatus dueDateStatus) {
        Objects.requireNonNull(dueDateStatus, "dueDateStatusはnullであってはいけません");
        CategorySummary category = task.getCategory() == null ? null
                : categoryEntityMapper.toSummary(task.getCategory());
        return new TaskSummary(task.getId(), task.getTitle(), task.getDescription(), category,
                task.getPriority(), task.getDueDate(), dueDateStatus, task.getStatus());
    }

    /**
     * 編集画面表示用のDTOを生成する。
     *
     * @param task 変換元のタスク
     * @return 生成したDTO
     * @throws NullPointerException taskがnullの場合
     */
    public TaskEditValues toEditValues(Task task) {
        Long categoryId = task.getCategory() == null ? null : task.getCategory().getId();
        return new TaskEditValues(task.getId(), task.getTitle(), task.getDescription(), task.getDueDate(),
                task.getPriority(), categoryId);
    }

    /**
     * 入力された項目（タイトル・詳細・期限・優先度・カテゴリ）を、タスクのエンティティへ移す。
     *
     * @param input    移す元の入力
     * @param category 移すカテゴリ。カテゴリなしの場合はnull
     * @param task     移す先のタスクのエンティティ
     * @throws NullPointerException inputまたはtaskがnullの場合
     */
    public void copyInputToTask(TaskInput input, Category category, Task task) {
        task.setTitle(input.title());
        task.setDescription(input.description());
        task.setDueDate(input.dueDate());
        task.setPriority(input.priority());
        task.setCategory(category);
    }

    /**
     * ステータスを変更した結果のDTOを生成する。
     *
     * @param task          変換元のタスク
     * @param dueDateStatus サービスで判定した、期限の状況
     * @return 生成したDTO
     * @throws NullPointerException taskまたはdueDateStatusがnullの場合
     */
    public TaskStatusChangeResult toStatusChangeResult(Task task, DueDateStatus dueDateStatus) {
        Objects.requireNonNull(dueDateStatus, "dueDateStatusはnullであってはいけません");
        return new TaskStatusChangeResult(task.getId(), task.getStatus(), dueDateStatus);
    }
}
