package com.shinzato.todoapp;

import com.shinzato.todoapp.service.task.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * エラーページ（templates/error.html）のテスト。
 *
 * <p>
 * エラーのときにエラーページへ回す処理はサーバーが受け持つため、本物のサーバーを起動し、HTTPで画面を開いて確かめる。
 *
 * <p>
 * サーバーとテストが別の処理の流れで動き、@Transactionalでデータベースの変更を取り消せないため、
 * このクラスではデータベースに書き込む操作をしない。
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ErrorPageTest {

    private static final String ERROR_PAGE_HEADING = "エラーが発生しました";

    @Autowired
    private TestRestTemplate restTemplate;

    /** 想定外の例外を起こすため、タスク一覧の取得で例外を投げる偽物に差し替える */
    @MockitoBean
    private TaskService taskService;

    @LocalServerPort
    private int port;

    /** 設定値と同じコンテキストパスを使うため、プロパティから読み込む */
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Test
    @DisplayName("存在しないURLを開くと、404でエラーページが出て、エラーの番号は出ず、画面専用のCSSは読み込まない")
    void open_noSuchPage_showsErrorPageWith404() {
        // 実行
        ResponseEntity<String> response = openAsBrowser("/no-such-page");

        // 確認
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains(ERROR_PAGE_HEADING).doesNotContain("404").doesNotContain("/css/null");
    }

    @Test
    @DisplayName("IDが数値でない編集のURLを開くと、400でエラーページが出て、エラーの番号は出ない")
    void open_nonNumericTaskId_showsErrorPageWith400() {
        // 実行
        ResponseEntity<String> response = openAsBrowser("/tasks/abc/edit");

        // 確認
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains(ERROR_PAGE_HEADING).doesNotContain("400");
    }

    @Test
    @DisplayName("タスク一覧の取得で想定外の例外が起きると、500でエラーページが出て、エラーの番号は出ない")
    void open_unexpectedException_showsErrorPageWith500() {
        // 準備
        when(taskService.getTaskList()).thenThrow(new RuntimeException("テストで起こす想定外の例外"));

        // 実行
        ResponseEntity<String> response = openAsBrowser("/tasks");

        // 確認
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains(ERROR_PAGE_HEADING).doesNotContain("500");
    }

    @Test
    @DisplayName("APIのURLでエラーが起きると、エラーページではなくJSONが返る")
    void open_apiWithUnsupportedMethod_returnsJson() {
        // 準備: ステータス変更のAPIはPATCHだけを受けるため、GETで開いて405を起こす
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        // 実行
        ResponseEntity<String> response = restTemplate.exchange(urlOf("/api/tasks/1/status"), HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        // 確認
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getContentType()).isNotNull()
                .satisfies(contentType -> assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_JSON))
                        .isTrue());
        assertThat(response.getBody()).doesNotContain(ERROR_PAGE_HEADING);
        assertThat(response.getBody()).doesNotContain("\"path\"");
    }

    /**
     * ブラウザと同じくHTMLを求めるリクエストで、画面をGETで開く。
     *
     * @param path コンテキストパスより後ろのパス（例: "/tasks"）
     * @return レスポンス
     */
    private ResponseEntity<String> openAsBrowser(String path) {
        // HTMLを求めないと、エラーのときにエラーページではなくJSONが返るため、ブラウザと同じくHTMLを求める
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE);
        return restTemplate.exchange(urlOf(path), HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    /**
     * 起動したサーバーの、コンテキストパスを含むURLを作る。
     *
     * @param path コンテキストパスより後ろのパス（例: "/tasks"）
     * @return 「http://localhost:ポート番号/todo-app/tasks」の形のURL
     */
    private String urlOf(String path) {
        // コンテキストパスの付け忘れや二重付けで404になり、存在しないURLのテストが誤って通るのを防ぐため、URLを省かずに作る
        return "http://localhost:" + port + contextPath + path;
    }

}
