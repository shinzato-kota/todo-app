package com.shinzato.todoapp.form.task;

import com.shinzato.todoapp.entity.task.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * タスク追加・編集フォームで扱うフォームオブジェクト。
 *
 * <p>
 * 入力(登録・更新)と表示(編集画面)の両方で使う。
 */
public class TaskForm {

    /** タスクID。新規追加時はnullで、この値の有無でフォームの送信先を切り替える */
    private Long id;

    /** タイトル */
    @NotBlank
    @Size(max = 100)
    private String title;

    /**
     * エンティティから編集画面表示用のフォームを生成する。
     *
     * @param task 変換元のタスク
     * @return 生成したフォーム
     */
    public static TaskForm from(Task task) {
        TaskForm form = new TaskForm();
        form.setId(task.getId());
        form.setTitle(task.getTitle());
        return form;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
