package com.shinzato.todoapp.form.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import com.shinzato.todoapp.enums.task.TaskPriority;
import com.shinzato.todoapp.validation.category.ExistingCategory;

import java.time.LocalDate;

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

    /** 詳細。未設定の場合はnull */
    @Size(max = 2000)
    private String description;

    /** 期限。未設定の場合はnull */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    /** 優先度 */
    @NotNull
    private TaskPriority priority;

    /** カテゴリID。カテゴリなしの場合はnull */
    @ExistingCategory
    private Long categoryId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    /**
     * タイトルを設定する。
     *
     * <p>
     * 空白だけ（全角スペースを含む）の場合はnullにする。
     *
     * @param title タイトル
     */
    public void setTitle(String title) {
        // 全角スペースだけのタイトルは@NotBlankを通ってしまうため、nullにして未入力のエラーにする
        this.title = (title == null || title.isBlank()) ? null : title;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 詳細を設定する。
     *
     * <p>
     * 改行（CR+LF）をLFにそろえる。空白・改行だけ（全角スペースを含む）の場合はnullにする。
     *
     * @param description 詳細
     */
    public void setDescription(String description) {
        // 空白だけの詳細は入力がないのと同じため、空欄と同じく未設定(null)として保存する
        // 画面で上限まで入力できた詳細が、保存するときに文字数の上限を超えたエラーにならないよう、改行をLFにそろえる
        this.description = (description == null || description.isBlank())
                ? null
                : description.replace("\r\n", "\n");
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}
