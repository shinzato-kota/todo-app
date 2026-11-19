package com.shinzato.todoapp.mapper.task;

import com.shinzato.todoapp.dto.task.TaskEditValues;
import com.shinzato.todoapp.dto.task.TaskInput;
import com.shinzato.todoapp.enums.task.TaskPriority;
import com.shinzato.todoapp.form.task.TaskForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TaskFormMapper}のテスト。
 *
 * <p>
 * データベースを使わないため、Springを起動せずにインスタンスを直接作る。
 */
class TaskFormMapperTest {

    private final TaskFormMapper taskFormMapper = new TaskFormMapper();

    @Nested
    @DisplayName("入力のDTOの生成（toInput）")
    class ToInput {
        @Test
        @DisplayName("フォームから入力のDTOを作ると、タイトル・詳細・期限・優先度・カテゴリIDが移される")
        void toInput_copiesAllItems() {
            // 準備
            String title = "入力したタスク";
            String description = "1行目\n2行目";
            LocalDate dueDate = LocalDate.of(2026, 10, 31);
            Long categoryId = 3L;
            TaskForm form = new TaskForm();
            form.setTitle(title);
            form.setDescription(description);
            form.setDueDate(dueDate);
            form.setPriority(TaskPriority.HIGH);
            form.setCategoryId(categoryId);

            // 実行
            TaskInput input = taskFormMapper.toInput(form);

            // 確認
            assertThat(input).isEqualTo(new TaskInput(title, description, dueDate, TaskPriority.HIGH, categoryId));
        }
    }

    @Nested
    @DisplayName("編集画面表示用のフォームの生成（toForm）")
    class ToForm {
        @Test
        @DisplayName("編集画面表示用のDTOからフォームを作ると、ID・タイトル・詳細・期限・優先度・カテゴリIDが移される")
        void toForm_copiesAllItems() {
            // 準備: toFormがタスクIDとカテゴリIDを入れ違えて移したときにテストが失敗するよう、タスクIDを5、カテゴリIDを3と、違う値にする
            Long id = 5L;
            String title = "編集するタスク";
            String description = "1行目\n2行目";
            LocalDate dueDate = LocalDate.of(2026, 10, 31);
            Long categoryId = 3L;
            TaskEditValues editValues = new TaskEditValues(id, title, description, dueDate, TaskPriority.HIGH,
                    categoryId);

            // 実行
            TaskForm form = taskFormMapper.toForm(editValues);

            // 確認
            assertThat(form.getId()).isEqualTo(id);
            assertThat(form.getTitle()).isEqualTo(title);
            assertThat(form.getDescription()).isEqualTo(description);
            assertThat(form.getDueDate()).isEqualTo(dueDate);
            assertThat(form.getPriority()).isEqualTo(TaskPriority.HIGH);
            assertThat(form.getCategoryId()).isEqualTo(categoryId);
        }
    }
}
