package com.shinzato.todoapp.controller.category;

import com.shinzato.todoapp.entity.category.Category;
import com.shinzato.todoapp.entity.task.Task;
import com.shinzato.todoapp.repository.category.CategoryRepository;
import com.shinzato.todoapp.repository.task.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CategoryApiController}のテスト。
 *
 * <p>
 * jQueryから呼び出すAPIと同じ形式でリクエストを送り、レスポンスを確認する。
 *
 * <p>
 * URLは実際のアクセスと同じ形にするため、コンテキストパスを含めて指定する。
 *
 * <p>
 * 開発用のデータベースに接続して確認するため、テストでの変更が残らないよう@Transactionalを付けている。
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestEntityManager
@Transactional
class CategoryApiControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    /** 設定値と同じコンテキストパスを使うため、プロパティから読み込む */
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Test
    @DisplayName("削除するとデータベースから取り除かれ、204が返る")
    void delete_returnsNoContent() {
        // 準備: 削除するカテゴリと、消えないことを確かめるカテゴリを用意する
        long before = categoryRepository.count();
        Category deletedCategory = createCategory("APIで削除するカテゴリ");
        Category remainingCategory = createCategory("削除の対象外のカテゴリ");

        // 実行・確認
        assertThat(mockMvc.delete().uri(contextPath + "/api/categories/" + deletedCategory.getId())
                .contextPath(contextPath))
                .hasStatus(HttpStatus.NO_CONTENT);

        // 確認: 指定したカテゴリだけが消え、件数は1件だけ減る
        clearPersistenceContext();
        assertThat(categoryRepository.findById(deletedCategory.getId())).isEmpty();
        assertThat(categoryRepository.findById(remainingCategory.getId())).isPresent();
        assertThat(categoryRepository.count()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("削除したカテゴリのタスクは、削除されずに「カテゴリなし」になる")
    void delete_categoryWithTask_setsTaskCategoryToNull() {
        // 準備
        Category category = createCategory("タスクを持つカテゴリ");
        Task task = createTask("カテゴリを削除されるタスク", category);
        // 永続性コンテキストのタスクが削除後のカテゴリを参照したまま残ると、確認の前の反映で例外になるため、ここで永続性コンテキストを空にする
        clearPersistenceContext();

        // 実行・確認
        assertThat(mockMvc.delete().uri(contextPath + "/api/categories/" + category.getId())
                .contextPath(contextPath))
                .hasStatus(HttpStatus.NO_CONTENT);

        // 確認
        clearPersistenceContext();
        assertThat(taskRepository.findById(task.getId()))
                .hasValueSatisfying(saved -> assertThat(saved.getCategory()).isNull());
    }

    @Test
    @DisplayName("存在しないカテゴリを削除しようとすると、404とIDを含まないメッセージが返る")
    void delete_notFound_returnsNotFound() {
        assertThat(mockMvc.delete().uri(contextPath + "/api/categories/-1")
                .contextPath(contextPath))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.message").isEqualTo("カテゴリが見つかりません");
    }

    @Test
    @DisplayName("URLのIDが数値でない場合は、400と内部情報を含まないエラーが返る")
    void delete_nonNumericId_returnsBadRequest() {
        assertThat(mockMvc.delete().uri(contextPath + "/api/categories/abc")
                .contextPath(contextPath))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .isStrictlyEqualTo("{\"message\":\"リクエストの形式が正しくありません\"}");
    }

    /**
     * テストの準備用に、名前だけを指定したカテゴリを登録する。
     *
     * <p>
     * 検証対象のAPIを準備に使わないよう、直接保存する。
     *
     * @param name 登録するカテゴリ名
     * @return データベースから読み直したカテゴリ
     */
    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return entityManager.persistFlushFind(category);
    }

    /**
     * テストの準備用に、タイトルとカテゴリを指定したタスクを登録する。
     *
     * <p>
     * 表示順は、並び順を確かめないため、すべて同じ1で登録し、一覧の並びをIDの順（登録した順）にする。
     *
     * @param title    登録するタイトル
     * @param category タスクのカテゴリ
     * @return データベースから読み直したタスク
     */
    private Task createTask(String title, Category category) {
        Task task = new Task();
        task.setTitle(title);
        task.setCategory(category);
        task.setSortOrder(1);
        return entityManager.persistFlushFind(task);
    }

    /**
     * 永続性コンテキストの変更をデータベースへ反映し、永続性コンテキストを空にする。
     *
     * <p>
     * 取得時に永続性コンテキストのエンティティではなくデータベースに保存された値を読むため、確認の前に呼び出す。
     */
    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
