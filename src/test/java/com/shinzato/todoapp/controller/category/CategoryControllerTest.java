package com.shinzato.todoapp.controller.category;

import com.shinzato.todoapp.dto.category.CategorySummary;
import com.shinzato.todoapp.entity.category.Category;
import com.shinzato.todoapp.repository.category.CategoryRepository;
import com.shinzato.todoapp.service.category.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CategoryController}のテスト。
 *
 * <p>
 * 実際にリクエストを送って画面のレスポンスを確認する。
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
class CategoryControllerTest {

    /** 色を確かめないテストで送る色。値そのものに意味はない */
    private static final String COLOR_NOT_CHECKED = "#1e88e5";

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    /** 設定値と同じコンテキストパスを使うため、プロパティから読み込む */
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Nested
    @DisplayName("一覧の画面（GET /categories）")
    class ListPage {
        @Test
        @DisplayName("一覧画面には、カテゴリが作成した順に表示される")
        void list_showsCategoriesInCreatedOrder() {
            // 準備: 名前の順で並べた場合と結果が変わるよう、「Zで始まるカテゴリ」「Aで始まるカテゴリ」の順で作成する
            createCategory("Zで始まるカテゴリ");
            createCategory("Aで始まるカテゴリ");

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/categories").contextPath(contextPath))
                    .hasStatusOk()
                    .hasViewName("categories/list")
                    .bodyText()
                    .satisfies(html -> assertThat(html.indexOf("Zで始まるカテゴリ"))
                            .isNotNegative()
                            .isLessThan(html.indexOf("Aで始まるカテゴリ")));
        }

        @Test
        @DisplayName("カテゴリがあるときは「カテゴリはありません」が表示されない")
        void list_withCategories_hidesEmptyMessage() {
            // 準備
            createCategory("一覧に出すカテゴリ");

            // 実行・確認: メッセージは件数に関係なくHTMLに含まれ、hidden属性で表示を切り替えているため、属性を確認する
            assertThat(mockMvc.get().uri(contextPath + "/categories").contextPath(contextPath))
                    .hasStatusOk()
                    .hasViewName("categories/list")
                    .bodyText()
                    .contains("class=\"empty-message category-list__empty-message js-empty-message\" hidden=\"hidden\"")
                    .doesNotContain("class=\"data-table category-list__table js-category-table\" hidden=\"hidden\"");
        }

        @Test
        @DisplayName("カテゴリが0件のときは「カテゴリはありません」が表示される")
        void list_noCategories_showsEmptyMessage() {
            // 準備: このテストのトランザクション内で全件削除する(終了時にロールバックされる)
            categoryRepository.deleteAll();

            // 実行・確認: メッセージは件数に関係なくHTMLに含まれ、hidden属性で表示を切り替えているため、属性を確認する
            assertThat(mockMvc.get().uri(contextPath + "/categories").contextPath(contextPath))
                    .hasStatusOk()
                    .hasViewName("categories/list")
                    .bodyText()
                    .doesNotContain(
                            "class=\"empty-message category-list__empty-message js-empty-message\" hidden=\"hidden\"")
                    .contains("class=\"data-table category-list__table js-category-table\" hidden=\"hidden\"");
        }
    }

    @Nested
    @DisplayName("追加の画面（GET /categories/new）")
    class NewForm {
        @Test
        @DisplayName("カテゴリの追加画面は、URLのクエリパラメーターnameにカテゴリ名を付けて開いても、そのカテゴリ名を入力欄に入れずに表示される")
        void newForm_withQueryParam_showsEmptyForm() {
            // 実行・確認: URLに付けて開いたときを確かめるため、.paramではなく.queryParamで送る
            assertThat(mockMvc.get().uri(contextPath + "/categories/new").contextPath(contextPath)
                    .queryParam("name", "URLで渡したカテゴリ"))
                    .hasStatusOk()
                    .hasViewName("categories/form")
                    .bodyText().doesNotContain("URLで渡したカテゴリ");
        }
    }

    @Nested
    @DisplayName("追加の保存（POST /categories）")
    class Create {
        @Test
        @DisplayName("名前と色を入力して作成すると、一覧画面へ遷移し、色は小文字で登録される")
        void create_validNameAndColor_savesWithLowerCaseColor() {
            // 準備: 開発用のデータが残っていても判定できるよう、作成前の件数を控える
            long before = categoryRepository.count();

            // 実行・確認: リダイレクト先はコンテキストパスを含む形で返る
            assertThat(mockMvc.post().uri(contextPath + "/categories").contextPath(contextPath)
                    .param("name", "テストで作成したカテゴリ")
                    .param("color", "#1E88E5"))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/categories");

            // 確認
            clearPersistenceContext();
            List<CategorySummary> categories = categoryService.getCategoryList();
            assertThat(categories).hasSize((int) before + 1);
            CategorySummary saved = categories.get(categories.size() - 1);
            assertThat(saved.name()).isEqualTo("テストで作成したカテゴリ");
            assertThat(saved.color()).isEqualTo("#1e88e5");
        }

        @Test
        @DisplayName("「色を付けない」を選んで作成すると、色の欄の値は使われず、色なしで登録される")
        void create_noColorChecked_savesColorAsNull() {
            // 準備: ブラウザーは「色を付けない」を選んでも色の欄の値(既定は#000000)を送るため、同じ形で送る
            long before = categoryRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/categories").contextPath(contextPath)
                    .param("name", "色なしのカテゴリ")
                    .param("color", "#000000")
                    .param("noColor", "true"))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/categories");

            // 確認
            clearPersistenceContext();
            List<CategorySummary> categories = categoryService.getCategoryList();
            assertThat(categories).hasSize((int) before + 1);
            assertThat(categories.get(categories.size() - 1).color()).isNull();
        }

        @Test
        @DisplayName("名前の前後の空白(全角スペースを含む)は削って登録される")
        void create_nameWithSurroundingSpaces_savesStrippedName() {
            // 準備: 全角スペースは見た目で半角と区別できないため、文字コードで書く
            String name = "\u3000 前後に空白のあるカテゴリ \u3000";
            long before = categoryRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/categories").contextPath(contextPath)
                    .param("name", name)
                    .param("color", COLOR_NOT_CHECKED))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/categories");

            // 確認
            clearPersistenceContext();
            List<CategorySummary> categories = categoryService.getCategoryList();
            assertThat(categories).hasSize((int) before + 1);
            assertThat(categories.get(categories.size() - 1).name()).isEqualTo("前後に空白のあるカテゴリ");
        }

        @Test
        @DisplayName("名前が1文字なら登録できる")
        void create_name1Char_saves() {
            // 準備
            String name = "あ";
            long before = categoryRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/categories").contextPath(contextPath)
                    .param("name", name)
                    .param("color", COLOR_NOT_CHECKED))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/categories");

            // 確認
            clearPersistenceContext();
            List<CategorySummary> categories = categoryService.getCategoryList();
            assertThat(categories).hasSize((int) before + 1);
            assertThat(categories.get(categories.size() - 1).name()).isEqualTo(name);
        }

        @Test
        @DisplayName("名前が20文字ちょうどなら登録できる")
        void create_name20Chars_saves() {
            // 準備: 全角文字でもバイト数ではなく文字数で数えることを確かめるため、日本語で作る
            String name = "あ".repeat(20);
            long before = categoryRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/categories").contextPath(contextPath)
                    .param("name", name)
                    .param("color", COLOR_NOT_CHECKED))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/categories");

            // 確認
            clearPersistenceContext();
            List<CategorySummary> categories = categoryService.getCategoryList();
            assertThat(categories).hasSize((int) before + 1);
            assertThat(categories.get(categories.size() - 1).name()).isEqualTo(name);
        }

        @Test
        @DisplayName("名前が空だとエラーになり、登録されない")
        void create_emptyName_showsErrorAndDoesNotSave() {
            // 準備
            long before = categoryRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/categories").contextPath(contextPath)
                    .param("name", "")
                    .param("color", COLOR_NOT_CHECKED))
                    .hasStatusOk()
                    .hasViewName("categories/form")
                    .bodyText().contains("必須入力です");

            // 確認
            clearPersistenceContext();
            assertThat(categoryRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("名前が全角スペースだけの場合もエラーになり、登録されない")
        void create_fullWidthSpaceName_showsErrorAndDoesNotSave() {
            // 準備: 全角スペースは見た目で半角と区別できないため、文字コードで書く
            String name = "\u3000\u3000\u3000";
            long before = categoryRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/categories").contextPath(contextPath)
                    .param("name", name)
                    .param("color", COLOR_NOT_CHECKED))
                    .hasStatusOk()
                    .hasViewName("categories/form")
                    .bodyText().contains("必須入力です");

            // 確認
            clearPersistenceContext();
            assertThat(categoryRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("名前が21文字だとエラーになり、登録されない")
        void create_name21Chars_showsErrorAndDoesNotSave() {
            // 準備
            long before = categoryRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/categories").contextPath(contextPath)
                    .param("name", "あ".repeat(21))
                    .param("color", COLOR_NOT_CHECKED))
                    .hasStatusOk()
                    .hasViewName("categories/form")
                    .bodyText().contains("20文字以内で入力してください");

            // 確認
            clearPersistenceContext();
            assertThat(categoryRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("大文字・小文字だけが違う名前のカテゴリがあると、重複のエラーになり、登録されず、入力した名前と色が残る")
        void create_nameDiffersOnlyInCase_showsDuplicateErrorAndDoesNotSave() {
            // 準備: 大文字・小文字を区別しないデータベースの照合順序で比べていることを確かめるため、大文字の名前を登録しておく
            createCategory("Work");
            long before = categoryRepository.count();

            // 実行・確認: 重複は入力チェックとは別の経路でエラーにするため、入力した名前と色がこの経路でも残ることを確かめる
            assertThat(mockMvc.post().uri(contextPath + "/categories").contextPath(contextPath)
                    .param("name", "work")
                    .param("color", "#43a047"))
                    .hasStatusOk()
                    .hasViewName("categories/form")
                    .bodyText()
                    .contains("同じ名前のカテゴリがあります")
                    .contains("value=\"work\"")
                    .contains("value=\"#43a047\"");

            // 確認
            clearPersistenceContext();
            assertThat(categoryRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("色が#rrggbbの形でないとエラーになり、登録されない")
        void create_invalidColor_showsErrorAndDoesNotSave() {
            // 準備
            long before = categoryRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/categories").contextPath(contextPath)
                    .param("name", "色の形が違うカテゴリ")
                    .param("color", "red"))
                    .hasStatusOk()
                    .hasViewName("categories/form")
                    .bodyText().contains("入力値が不正です");

            // 確認
            clearPersistenceContext();
            assertThat(categoryRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("「色を付けない」を選んでも、色が#rrggbbの形でないとエラーになり、登録されない")
        void create_noColorCheckedAndInvalidColor_showsErrorAndDoesNotSave() {
            // 準備
            long before = categoryRepository.count();

            // 実行・確認: 画面からは送れない値のため、色の欄の値を書き換えて送られた場合を試す
            assertThat(mockMvc.post().uri(contextPath + "/categories").contextPath(contextPath)
                    .param("name", "色の形が違う色なしのカテゴリ")
                    .param("color", "red")
                    .param("noColor", "true"))
                    .hasStatusOk()
                    .hasViewName("categories/form")
                    .bodyText().contains("入力値が不正です");

            // 確認
            clearPersistenceContext();
            assertThat(categoryRepository.count()).isEqualTo(before);
        }

        /**
         * 永続性コンテキストの変更をデータベースへ反映し、永続性コンテキストを空にする。
         *
         * <p>
         * 取得時に永続性コンテキストのカテゴリではなくデータベースに保存された値を読むため、確認の前に呼び出す。
         */
        private void clearPersistenceContext() {
            entityManager.flush();
            entityManager.clear();
        }
    }

    /**
     * テストの準備用に、名前だけを指定したカテゴリを登録する。
     *
     * <p>
     * 検証対象の画面を準備に使わないよう、直接保存する。
     *
     * @param name 登録するカテゴリ名
     * @return データベースから読み直したカテゴリ
     */
    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return entityManager.persistFlushFind(category);
    }
}
