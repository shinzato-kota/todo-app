package com.shinzato.todoapp.repository.category;

import com.shinzato.todoapp.entity.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * categoryテーブルへのアクセスを行うリポジトリ。
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 全カテゴリを作成した順で取得する。
     *
     * @return IDの昇順に並んだ全カテゴリ
     */
    List<Category> findAllByOrderByIdAsc();

    /**
     * 指定した名前のカテゴリが存在するかを返す。
     *
     * <p>
     * データベースの照合順序で比べるため、大文字・小文字だけが違う名前も同じ名前として扱う。
     *
     * @param name 調べるカテゴリ名
     * @return 同じ名前のカテゴリが存在すればtrue
     */
    boolean existsByName(String name);
}
