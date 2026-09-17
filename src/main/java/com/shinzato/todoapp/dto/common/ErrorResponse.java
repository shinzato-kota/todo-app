package com.shinzato.todoapp.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * APIがエラー時に返すレスポンスのDTO。
 *
 * <p>
 * 入力値エラーとそれ以外を同じ形式で返すため、項目別のエラーは任意項目としている。
 *
 * @param message エラーの内容を表すメッセージ
 * @param errors  項目名とエラーメッセージの対応。入力値エラー以外ではnull
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String message, Map<String, String> errors) {

    /**
     * 項目別のエラーを伴わないエラーレスポンスを生成する。
     *
     * @param message エラーの内容を表すメッセージ
     * @return 生成したDTO
     */
    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, null);
    }

    /**
     * 項目別のエラーを伴うエラーレスポンスを生成する。
     *
     * @param message エラーの内容を表すメッセージ
     * @param errors  項目名とエラーメッセージの対応
     * @return 生成したDTO
     */
    public static ErrorResponse of(String message, Map<String, String> errors) {
        return new ErrorResponse(message, errors);
    }
}
