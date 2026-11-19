package com.shinzato.todoapp.mapper.category;

import com.shinzato.todoapp.dto.category.CategoryInput;
import com.shinzato.todoapp.form.category.CategoryForm;
import org.springframework.stereotype.Component;

/**
 * カテゴリの追加フォームと、サービスが受け取るDTOとの間で詰め替える。
 */
@Component
public class CategoryFormMapper {

    /**
     * 画面で入力した項目から、サービスに渡す入力のDTOを生成する。
     *
     * <p>
     * 「色を付けない」を選んだ場合は、ラベル色をnullにする。
     *
     * @param form 変換元のフォーム
     * @return 生成したDTO
     * @throws NullPointerException formがnullの場合
     */
    public CategoryInput toInput(CategoryForm form) {
        return new CategoryInput(form.getName(), form.isNoColor() ? null : form.getColor());
    }
}
