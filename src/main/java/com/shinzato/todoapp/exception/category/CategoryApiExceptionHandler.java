package com.shinzato.todoapp.exception.category;

import com.shinzato.todoapp.controller.category.CategoryApiController;
import com.shinzato.todoapp.dto.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * カテゴリのAPI用の例外ハンドラー。
 *
 * <p>
 * エラーの内容にかかわらず同じ形式のJSONを返す。
 *
 * <p>
 * 画面用のコントローラーはHTMLのレスポンスを返すため、対象を{@link CategoryApiController}に限定している。
 */
@RestControllerAdvice(assignableTypes = CategoryApiController.class)
public class CategoryApiExceptionHandler {

    /**
     * 対象のカテゴリが存在しない場合を処理する。
     *
     * @return 404と、固定のメッセージ
     */
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("カテゴリが見つかりません"));
    }

    /**
     * URLのIDが数値でない場合を処理する。
     *
     * @return 400と、固定のメッセージ
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest() {
        // 例外メッセージには内部のクラス名や引数の型が含まれるため、そのままは返さない
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("リクエストの形式が正しくありません"));
    }
}
