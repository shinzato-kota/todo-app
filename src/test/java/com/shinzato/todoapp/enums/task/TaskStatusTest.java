package com.shinzato.todoapp.enums.task;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TaskStatus}のテスト。
 *
 * <p>
 * 定数名はJSONの値、CSSのクラス名、データベースの保存値として外部と共有しているため、
 * 変わっていないことを確認する。
 */
class TaskStatusTest {

    @Test
    @DisplayName("定数名が、外部と共有している値から変わっていない")
    void values_namesAreUnchanged() {
        assertThat(TaskStatus.values()).extracting(Enum::name)
                .containsExactly("TODO", "IN_PROGRESS", "ON_HOLD", "DONE", "CANCELLED");
    }
}
