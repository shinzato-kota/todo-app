package com.shinzato.todoapp.mapper.task;

import com.shinzato.todoapp.dto.task.TaskEditValues;
import com.shinzato.todoapp.dto.task.TaskSummary;
import com.shinzato.todoapp.entity.category.Category;
import com.shinzato.todoapp.entity.task.Task;
import com.shinzato.todoapp.enums.task.DueDateStatus;
import com.shinzato.todoapp.enums.task.TaskPriority;
import com.shinzato.todoapp.enums.task.TaskStatus;
import com.shinzato.todoapp.mapper.category.CategoryEntityMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TaskEntityMapper}のテスト。
 *
 * <p>
 * データベースを使わないため、Springを起動せずにインスタンスを直接作る。
 */
class TaskEntityMapperTest {

    private final TaskEntityMapper taskEntityMapper = new TaskEntityMapper(new CategoryEntityMapper());

    @Nested
    @DisplayName("一覧表示用のDTOの生成（toSummary）")
    class ToSummary {
        @Test
        @DisplayName("期限の状況がnullの場合、一覧表示用のDTOを作らずに例外が発生する")
        void toSummary_nullDueDateStatus_throwsNullPointerException() {
            assertThatThrownBy(() -> taskEntityMapper.toSummary(new Task(), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("dueDateStatusはnullであってはいけません");
        }

        @Test
        @DisplayName("一覧表示用のDTOを作ると、詳細・優先度・期限・期限の状況も移される")
        void toSummary_copiesDescriptionPriorityDueDateAndDueDateStatus() {
            // 準備: 優先度はエンティティの初期値(中)と区別できるよう、高にする
            String title = "一覧に出すタスク";
            String description = "1行目\n2行目";
            LocalDate dueDate = LocalDate.of(2026, 10, 31);
            Task task = new Task();
            task.setTitle(title);
            task.setDescription(description);
            task.setPriority(TaskPriority.HIGH);
            task.setDueDate(dueDate);

            // 実行
            TaskSummary summary = taskEntityMapper.toSummary(task, DueDateStatus.OVERDUE);

            // 確認
            assertThat(summary.title()).isEqualTo(title);
            assertThat(summary.description()).isEqualTo(description);
            assertThat(summary.priority()).isEqualTo(TaskPriority.HIGH);
            assertThat(summary.dueDate()).isEqualTo(dueDate);
            assertThat(summary.dueDateStatus()).isEqualTo(DueDateStatus.OVERDUE);
            assertThat(summary.status()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        @DisplayName("カテゴリのあるタスクは、一覧表示用のDTOにカテゴリの名前と色が移される")
        void toSummary_taskWithCategory_copiesCategoryNameAndColor() {
            // 準備: IDはデータベースに保存しないと入らないため、名前と色を確かめる
            String categoryName = "仕事";
            String categoryColor = "#1e88e5";
            Category category = new Category();
            category.setName(categoryName);
            category.setColor(categoryColor);
            Task task = new Task();
            task.setTitle("カテゴリのあるタスク");
            task.setCategory(category);

            // 実行
            TaskSummary summary = taskEntityMapper.toSummary(task, DueDateStatus.NORMAL);

            // 確認
            assertThat(summary.category().name()).isEqualTo(categoryName);
            assertThat(summary.category().color()).isEqualTo(categoryColor);
        }

        @Test
        @DisplayName("カテゴリなしのタスクは、一覧表示用のDTOのカテゴリがnullになる")
        void toSummary_taskWithoutCategory_setsNullCategory() {
            // 準備
            Task task = new Task();
            task.setTitle("カテゴリなしのタスク");

            // 実行
            TaskSummary summary = taskEntityMapper.toSummary(task, DueDateStatus.NORMAL);

            // 確認
            assertThat(summary.category()).isNull();
        }
    }

    @Nested
    @DisplayName("編集画面表示用のDTOの生成（toEditValues）")
    class ToEditValues {
        @Test
        @DisplayName("編集画面表示用のDTOを作ると、詳細・期限・優先度も移される")
        void toEditValues_copiesDescriptionDueDateAndPriority() {
            // 準備: 優先度はエンティティの初期値(中)と区別できるよう、高にする
            String title = "編集するタスク";
            String description = "1行目\n2行目";
            LocalDate dueDate = LocalDate.of(2026, 10, 31);
            Task task = new Task();
            task.setTitle(title);
            task.setDescription(description);
            task.setDueDate(dueDate);
            task.setPriority(TaskPriority.HIGH);

            // 実行
            TaskEditValues editValues = taskEntityMapper.toEditValues(task);

            // 確認
            assertThat(editValues.title()).isEqualTo(title);
            assertThat(editValues.description()).isEqualTo(description);
            assertThat(editValues.dueDate()).isEqualTo(dueDate);
            assertThat(editValues.priority()).isEqualTo(TaskPriority.HIGH);
        }
    }

    @Nested
    @DisplayName("ステータスを変更した結果のDTOの生成（toStatusChangeResult）")
    class ToStatusChangeResult {
        @Test
        @DisplayName("期限の状況がnullの場合、ステータスを変更した結果のDTOを作らずに例外が発生する")
        void toStatusChangeResult_nullDueDateStatus_throwsNullPointerException() {
            assertThatThrownBy(() -> taskEntityMapper.toStatusChangeResult(new Task(), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("dueDateStatusはnullであってはいけません");
        }
    }
}
