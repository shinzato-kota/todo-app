package com.shinzato.todoapp.service.category;

import com.shinzato.todoapp.dto.category.CategoryInput;
import com.shinzato.todoapp.dto.category.CategorySummary;
import com.shinzato.todoapp.entity.category.Category;
import com.shinzato.todoapp.exception.category.CategoryNotFoundException;
import com.shinzato.todoapp.exception.category.DuplicateCategoryNameException;
import com.shinzato.todoapp.mapper.category.CategoryEntityMapper;
import com.shinzato.todoapp.repository.category.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * カテゴリに関する業務処理を行うサービス。
 *
 * <p>
 * エンティティはこのクラスの中だけで扱い、呼び出し元へはDTOを返す。
 */
@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryEntityMapper categoryEntityMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryEntityMapper categoryEntityMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryEntityMapper = categoryEntityMapper;
    }

    /**
     * 全カテゴリを作成した順で取得する。
     *
     * @return 作成した順に並んだカテゴリ一覧
     */
    @Transactional(readOnly = true)
    public List<CategorySummary> getCategoryList() {
        return categoryRepository.findAllByOrderByIdAsc().stream()
                .map(categoryEntityMapper::toSummary)
                .toList();
    }

    /**
     * 指定したIDのカテゴリが存在するかどうかを返す。
     *
     * @param id 確かめるカテゴリのID
     * @return 存在すればtrue
     * @throws NullPointerException idがnullの場合
     */
    @Transactional(readOnly = true)
    public boolean existsCategory(Long id) {
        Objects.requireNonNull(id, "idはnullであってはいけません");
        return categoryRepository.existsById(id);
    }

    /**
     * カテゴリを新規登録する。
     *
     * @param input 登録する内容
     * @throws NullPointerException           inputがnullの場合
     * @throws DuplicateCategoryNameException 同じ名前（大文字・小文字の違いは区別しない）のカテゴリがすでに存在する場合
     */
    public void registerCategory(CategoryInput input) {
        // Javaで名前を比べると、大文字・小文字を区別しないデータベースの一意制約と結果が食い違うため、データベースに問い合わせる
        if (categoryRepository.existsByName(input.name())) {
            throw new DuplicateCategoryNameException(input.name());
        }
        Category category = new Category();
        categoryEntityMapper.copyInputToCategory(input, category);
        categoryRepository.save(category);
    }

    /**
     * カテゴリを削除する。このカテゴリに属していたタスクは「カテゴリなし」になる。
     *
     * @param id 削除するカテゴリのID
     * @throws NullPointerException      idがnullの場合
     * @throws CategoryNotFoundException 指定したIDのカテゴリが存在しない場合
     */
    public void removeCategory(Long id) {
        // 存在しないIDの削除を成功として扱わないため、取得できたものだけを削除する
        categoryRepository.delete(getCategoryOrThrow(id));
    }

    /**
     * IDを指定してカテゴリのエンティティを取得し、存在しなければ例外を投げる。
     *
     * @param id 取得するカテゴリのID
     * @return 取得したカテゴリ
     * @throws CategoryNotFoundException 指定したIDのカテゴリが存在しない場合
     */
    private Category getCategoryOrThrow(Long id) {
        Objects.requireNonNull(id, "idはnullであってはいけません");
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }
}
