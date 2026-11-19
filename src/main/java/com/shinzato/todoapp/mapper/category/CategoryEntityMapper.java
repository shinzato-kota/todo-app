package com.shinzato.todoapp.mapper.category;

import com.shinzato.todoapp.dto.category.CategoryInput;
import com.shinzato.todoapp.dto.category.CategorySummary;
import com.shinzato.todoapp.entity.category.Category;
import org.springframework.stereotype.Component;

/**
 * カテゴリのエンティティと、サービスが受け取る・返すDTOとの間で詰め替える。
 */
@Component
public class CategoryEntityMapper {

    /**
     * 一覧表示用のDTOを生成する。
     *
     * @param category 変換元のカテゴリ
     * @return 生成したDTO
     * @throws NullPointerException categoryがnullの場合
     */
    public CategorySummary toSummary(Category category) {
        return new CategorySummary(category.getId(), category.getName(), category.getColor());
    }

    /**
     * 入力された項目（名前・ラベル色）を、カテゴリのエンティティへ移す。
     *
     * @param input    移す元の入力
     * @param category 移す先のカテゴリのエンティティ
     * @throws NullPointerException inputまたはcategoryがnullの場合
     */
    public void copyInputToCategory(CategoryInput input, Category category) {
        category.setName(input.name());
        category.setColor(input.color());
    }
}
