package com.shinzato.todoapp.form.task;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TaskForm}のテスト。
 *
 * <p>
 * データベースを使わないため、Springを起動せずにインスタンスを直接作る。
 */
class TaskFormTest {

    private final TaskForm taskForm = new TaskForm();

    @Test
    @DisplayName("詳細の改行がCR+LFの場合、LFにそろえる")
    void setDescription_crlf_convertsToLf() {
        // 実行
        taskForm.setDescription("1行目\r\n2行目\r\n3行目");

        // 確認
        assertThat(taskForm.getDescription()).isEqualTo("1行目\n2行目\n3行目");
    }

    @Test
    @DisplayName("詳細が空白と改行だけの場合、nullにする")
    void setDescription_blank_setsNull() {
        // 実行: 全角スペースは見た目で半角と区別できないため、文字コードで書く
        taskForm.setDescription(" \u3000\r\n");

        // 確認
        assertThat(taskForm.getDescription()).isNull();
    }

    @Test
    @DisplayName("詳細の前後の空白は削らずにそのまま残す")
    void setDescription_leadingAndTrailingSpaces_keepsAsIs() {
        // 実行: 行頭の字下げを残すことを確かめるため、前後に空白を入れる
        taskForm.setDescription("  字下げした行  ");

        // 確認
        assertThat(taskForm.getDescription()).isEqualTo("  字下げした行  ");
    }
}
