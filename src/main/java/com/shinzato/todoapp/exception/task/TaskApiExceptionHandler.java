package com.shinzato.todoapp.exception.task;

import com.shinzato.todoapp.controller.task.TaskApiController;
import com.shinzato.todoapp.dto.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API用の例外ハンドラー。
 *
 * <p>
 * エラーの内容にかかわらず同じ形式のJSONを返す。
 *
 * <p>
 * 画面用のコントローラーはリダイレクトのレスポンスを返すため、対象を{@link TaskApiController}に限定している。
 */
@RestControllerAdvice(assignableTypes = TaskApiController.class)
public class TaskApiExceptionHandler {

    /**
     * 対象のタスクが存在しない場合を処理する。
     *
     * @return 404と、固定のメッセージ
     */
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("タスクが見つかりません"));
    }

    /**
     * 並び替えで送られたIDの並びが、今あるタスクのIDをすべて1回ずつ含む形でない場合を処理する。
     *
     * @return 409と、固定のメッセージ
     */
    @ExceptionHandler(TaskOrderConflictException.class)
    public ResponseEntity<ErrorResponse> handleTaskOrderConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("送られたタスクIDが、今あるタスクのIDと一致しません"));
    }

    /**
     * リクエストボディとクエリ文字列の入力値エラーを処理する。
     *
     * <p>
     * クエリ文字列の値を型に変換できなかった項目は、固定のメッセージにする。
     *
     * @param ex 発生した例外
     * @return 400と、項目ごとのエラーメッセージ
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // 型に変換できなかったときのメッセージには内部のクラス名が含まれるため、そのままは返さない
            String message = fieldError.isBindingFailure() ? null : fieldError.getDefaultMessage();
            // メッセージが設定されていない場合でも項目名だけが残らないようにする
            errors.put(fieldError.getField(), message != null ? message : "入力値が不正です");
        }
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("入力値が不正です", errors));
    }

    /**
     * リクエストを解釈できない場合を処理する。
     *
     * <p>
     * ボディが不正なJSONの場合、ボディの形式（Content-Type）がJSONでない場合、
     * ボディで存在しないステータス値を指定された場合、URLのIDが数値でない場合が該当する。
     *
     * @return 400と、固定のメッセージ
     */
    @ExceptionHandler({ HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class,
            MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ErrorResponse> handleMalformedRequest() {
        // 例外メッセージには内部のクラス名や引数の型が含まれるため、そのままは返さない
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("リクエストの形式が正しくありません"));
    }
}
