/**
 * 複数の画面で共通して使う処理。
 */

// ESLint用: ほかのファイル(list.js、categories.js)で使う関数のため、使われていないと判定しないよう知らせる
/* exported buildApiUrl, toClassValue */

/**
 * APIのURLを組み立てる。
 *
 * コンテキストパスは設定で変わるため、JavaScript側には持たずHTMLから受け取る。
 *
 * @param {string} path コンテキストパスより後ろのパス(先頭の/は不要)
 * @returns {string} 呼び出しに使うURL
 */
function buildApiUrl(path) {
  const contextPath = $('body').data('context-path');
  return `${contextPath}${path}`;
}

/**
 * 列挙型の値を、CSSのクラス名に入れる値に変える(例: ON_HOLD は on-hold)。
 *
 * 一覧を開いたときのクラス名はThymeleafの式で同じ変え方をしているため、変え方を変えるときは両方を変える。
 *
 * @param {string} enumValue 列挙型の値
 * @returns {string} クラス名に入れる値
 */
function toClassValue(enumValue) {
  return enumValue.toLowerCase().replaceAll('_', '-');
}
