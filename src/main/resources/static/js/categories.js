/**
 * カテゴリ一覧画面(categories/list.html)の非同期処理。
 *
 * 削除を、画面を再読み込みせずに反映する。
 */

// ESLint用: common.jsで定義した関数のため、
// 定義されていないと判定しないよう知らせる
/* global buildApiUrl */

$(() => {
  registerDeleteHandler();
});

/**
 * 削除ボタンを押したときに、
 * 確認してからカテゴリの削除のリクエストを送る関数を設定する。
 */
function registerDeleteHandler() {
  // イベントが発生した要素をthisで受け取るため、ここはアロー関数にしない
  $('.js-delete-button').on('click', function () {
    sendDeleteRequest($(this));
  });
}

/**
 * 確認ダイアログで同意が得られた場合のみ、
 * 押されたボタンの行のカテゴリの削除のリクエストを送り、
 * 成功した行を画面から取り除く。
 *
 * カテゴリが見つからなかった場合も、知らせを出して行を取り除く。
 *
 * @param {jQuery} $button 押された削除ボタン
 */
function sendDeleteRequest($button) {
  const $row = $button.closest('tr');
  const categoryId = $row.data('category-id');
  // どのカテゴリを消すのかダイアログで示すため、行のカテゴリ名を取り出す
  const name = $row.find('.js-category-name').text();

  if (
    !window.confirm(
      `${name}\nこのカテゴリを削除します。このカテゴリのタスクは「カテゴリなし」になります。よろしいですか？`,
    )
  ) {
    return;
  }

  // レスポンスを待つ間に再度押されると、
  // 削除済みのIDへ二重にリクエストを送ることになるため操作を止める
  $button.prop('disabled', true);

  $.ajax({
    url: buildApiUrl(`api/categories/${categoryId}`),
    method: 'DELETE',
  })
    .done(() => {
      $row.remove();
      switchToEmptyMessageIfNoCategory();
    })
    .fail((jqXHR) => {
      handleDeleteFailure(jqXHR, $row, $button);
    });
}

/**
 * 表示できるカテゴリが1件も残っていない場合に、
 * 一覧表を隠して代わりのメッセージを表示する。
 *
 * 画面を開いたときはサーバー側で切り替えているが、
 * 削除で0件になった場合はその処理を通らないため、ここで切り替える。
 */
function switchToEmptyMessageIfNoCategory() {
  if ($('.js-category-table tbody tr').length === 0) {
    $('.js-category-table').prop('hidden', true);
    $('.js-empty-message').prop('hidden', false);
  }
}

/**
 * カテゴリの削除のリクエストが失敗したときに、知らせを出す。
 *
 * カテゴリが見つからなかった場合(404)は、行を取り除く。それ以外の場合は、
 * もう一度押せるようボタンを戻す。
 *
 * @param {jqXHR} jqXHR 削除のリクエストのjqXHR
 * @param {jQuery} $row 削除しようとしたカテゴリの行
 * @param {jQuery} $button 押された削除ボタン
 */
function handleDeleteFailure(jqXHR, $row, $button) {
  if (jqXHR.status === 404) {
    // ほかの画面で削除されていたため、削除に成功したときと同じく行を取り除く
    window.alert('カテゴリが見つかりません。');
    $row.remove();
    switchToEmptyMessageIfNoCategory();
    return;
  }
  window.alert('削除に失敗しました。');
  // 削除できていないため、再操作できるようボタンを戻す
  $button.prop('disabled', false);
}
