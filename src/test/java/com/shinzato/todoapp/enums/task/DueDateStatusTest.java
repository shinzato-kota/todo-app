package com.shinzato.todoapp.enums.task;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DueDateStatus}のテスト。
 *
 * <p>
 * 定数名はJSONの値とCSSのクラス名として外部と共有しているため、
 * 変わっていないことを確認する。
 */
class DueDateStatusTest {

    @Test
    @DisplayName("定数名が、外部と共有している値から変わっていない")
    void values_namesAreUnchanged() {
        assertThat(DueDateStatus.values()).extracting(Enum::name)
                .containsExactly("OVERDUE", "DUE_TODAY", "NORMAL");
    }
}
