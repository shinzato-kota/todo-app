package com.shinzato.todoapp.mapper.category;

import com.shinzato.todoapp.dto.category.CategoryInput;
import com.shinzato.todoapp.form.category.CategoryForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CategoryFormMapper}のテスト。
 *
 * <p>
 * データベースを使わないため、Springを起動せずにインスタンスを直接作る。
 */
class CategoryFormMapperTest {

    private final CategoryFormMapper categoryFormMapper = new CategoryFormMapper();

    @Test
    @DisplayName("「色を付けない」を選ばないと、入力のDTOに名前と、小文字にした色が移される")
    void toInput_noColorUnchecked_copiesNameAndColor() {
        // 準備
        String name = "仕事";
        CategoryForm form = new CategoryForm();
        form.setName(name);
        form.setColor("#1E88E5");

        // 実行
        CategoryInput input = categoryFormMapper.toInput(form);

        // 確認
        assertThat(input).isEqualTo(new CategoryInput(name, "#1e88e5"));
    }

    @Test
    @DisplayName("「色を付けない」を選ぶと、入力のDTOの色がnullになる")
    void toInput_noColorChecked_setsColorToNull() {
        // 準備: ブラウザーは「色を付けない」を選んでも色の欄の値を送るため、同じ形で色も入れる
        String name = "その他";
        CategoryForm form = new CategoryForm();
        form.setName(name);
        form.setColor("#000000");
        form.setNoColor(true);

        // 実行
        CategoryInput input = categoryFormMapper.toInput(form);

        // 確認
        assertThat(input).isEqualTo(new CategoryInput(name, null));
    }
}
