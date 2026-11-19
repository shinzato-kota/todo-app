package com.shinzato.todoapp.mapper.task;

import com.shinzato.todoapp.dto.task.TaskEditValues;
import com.shinzato.todoapp.dto.task.TaskInput;
import com.shinzato.todoapp.form.task.TaskForm;
import org.springframework.stereotype.Component;

/**
 * タスクの追加・編集フォームと、サービスが受け取る・返すDTOとの間で詰め替える。
 */
@Component
public class TaskFormMapper {

    /**
     * 画面で入力した項目から、サービスに渡す入力のDTOを生成する。
     *
     * @param form 変換元のフォーム
     * @return 生成したDTO
     * @throws NullPointerException formがnullの場合
     */
    public TaskInput toInput(TaskForm form) {
        return new TaskInput(form.getTitle(), form.getDescription(), form.getDueDate(), form.getPriority(),
                form.getCategoryId());
    }

    /**
     * 編集画面表示用のフォームを生成する。
     *
     * @param task 変換元の、編集画面表示用のDTO
     * @return 生成したフォーム
     * @throws NullPointerException taskがnullの場合
     */
    public TaskForm toForm(TaskEditValues task) {
        TaskForm form = new TaskForm();
        form.setId(task.id());
        form.setTitle(task.title());
        form.setDescription(task.description());
        form.setDueDate(task.dueDate());
        form.setPriority(task.priority());
        form.setCategoryId(task.categoryId());
        return form;
    }
}
