package com.shinzato.todoapp.mapper.task;

import com.shinzato.todoapp.dto.task.CategoryCondition;
import com.shinzato.todoapp.dto.task.TaskSearchCondition;
import com.shinzato.todoapp.dto.task.TaskSearchRequest;
import com.shinzato.todoapp.enums.task.DueDateCondition;
import com.shinzato.todoapp.enums.task.TaskPriority;
import com.shinzato.todoapp.enums.task.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TaskRequestMapper}のテスト。
 *
 * <p>
 * データベースを使わないため、Springを起動せずにインスタンスを直接作る。
 */
class TaskRequestMapperTest {

    private final TaskRequestMapper taskRequestMapper = new TaskRequestMapper();

    @Test
    @DisplayName("ステータス・期限・優先度は、そのまま移される")
    void toSearchCondition_copiesStatusDueAndPriority() {
        // 準備
        TaskSearchRequest request = new TaskSearchRequest(TaskStatus.IN_PROGRESS, DueDateCondition.OVERDUE,
                TaskPriority.HIGH, null, null);

        // 実行
        TaskSearchCondition condition = taskRequestMapper.toSearchCondition(request);

        // 確認
        assertThat(condition).isEqualTo(new TaskSearchCondition(TaskStatus.IN_PROGRESS, DueDateCondition.OVERDUE,
                TaskPriority.HIGH, null, null));
    }

    @Test
    @DisplayName("条件が1つも送られなかった場合は、すべての項目がnullになる")
    void toSearchCondition_noConditions_returnsAllNull() {
        // 準備
        TaskSearchRequest request = new TaskSearchRequest(null, null, null, null, null);

        // 実行
        TaskSearchCondition condition = taskRequestMapper.toSearchCondition(request);

        // 確認
        assertThat(condition).isEqualTo(new TaskSearchCondition(null, null, null, null, null));
    }

    @Test
    @DisplayName("カテゴリに数字を送ると、そのIDのカテゴリの条件になる")
    void toSearchCondition_numericCategory_returnsById() {
        // 準備
        TaskSearchRequest request = new TaskSearchRequest(null, null, null, null, "5");

        // 実行
        TaskSearchCondition condition = taskRequestMapper.toSearchCondition(request);

        // 確認
        assertThat(condition.category()).isEqualTo(new CategoryCondition.ById(5));
    }

    @Test
    @DisplayName("カテゴリにNONEを送ると、カテゴリなしの条件になる")
    void toSearchCondition_noneCategory_returnsNone() {
        // 準備
        TaskSearchRequest request = new TaskSearchRequest(null, null, null, null, TaskSearchRequest.NO_CATEGORY);

        // 実行
        TaskSearchCondition condition = taskRequestMapper.toSearchCondition(request);

        // 確認
        assertThat(condition.category()).isEqualTo(new CategoryCondition.None());
    }

    @Test
    @DisplayName("キーワードの前後の空白（半角・全角）は除かれ、間の空白は残る")
    void toSearchCondition_keywordWithSpaces_stripsBothEnds() {
        // 準備: 前は半角、後ろは全角の空白にする。間には半角の空白を置く
        TaskSearchRequest request = new TaskSearchRequest(null, null, null, " 牛乳 を買う\u3000", null);

        // 実行
        TaskSearchCondition condition = taskRequestMapper.toSearchCondition(request);

        // 確認
        assertThat(condition.keyword()).isEqualTo("牛乳 を買う");
    }

    @Test
    @DisplayName("キーワードが空白（半角・全角）だけの場合は、キーワードで絞り込まない")
    void toSearchCondition_blankKeyword_returnsNullKeyword() {
        // 準備
        TaskSearchRequest request = new TaskSearchRequest(null, null, null, " \u3000 ", null);

        // 実行
        TaskSearchCondition condition = taskRequestMapper.toSearchCondition(request);

        // 確認
        assertThat(condition.keyword()).isNull();
    }
}
