/**
 * タスク一覧画面(tasks/list.html)の非同期処理。
 *
 * ステータス変更と削除を、画面を再読み込みせずに反映する。
 */

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
 * ステータス変更時の処理を各プルダウンに登録する。
 *
 * 選択が変わったらAPIへ変更を送信し、成功した場合のみ行の表示スタイルを切り替える。
 */
function registerStatusChangeHandler() {
  // イベントが発生した要素をthisで受け取るため、ここはアロー関数にしない
  $('.status-select').on('change', function () {
    const $select = $(this);
    const $row = $select.closest('tr');
    const taskId = $row.data('task-id');
    const newStatus = $select.val();
    // 通信に失敗した場合の復元先として、変更前の値を控える
    const previousStatus = $select.data('previous-status');

    // 応答が返る前に次の変更を受け付けると、後から返った応答で表示が上書きされるため操作を止める
    $select.prop('disabled', true);

    $.ajax({
      url: buildApiUrl(`api/tasks/${taskId}/status`),
      method: 'PATCH',
      contentType: 'application/json',
      dataType: 'json',
      data: JSON.stringify({ status: newStatus }),
    })
      .done((response) => {
        // 見た目の定義はCSSに任せ、JavaScriptはステータスを表すクラスの付け替えだけを行う
        $row.removeClass(`status-${previousStatus}`);
        $row.addClass(`status-${response.status}`);
        // 次回の失敗時に正しい値へ戻せるよう、控えている値を更新する
        $select.data('previous-status', response.status);
      })
      .fail(() => {
        // 画面の表示とデータベースの状態が食い違わないよう、選択を元に戻す
        $select.val(previousStatus);
        window.alert('ステータスの変更に失敗しました。');
      })
      .always(() => {
        // 失敗時も操作不可のまま残さない
        $select.prop('disabled', false);
      });
  });
}

/**
 * 削除ボタン押下時の処理を各ボタンに登録する。
 *
 * 確認ダイアログで同意が得られた場合のみAPIへ削除を送信し、成功した行を画面から取り除く。
 */
function registerDeleteHandler() {
  // イベントが発生した要素をthisで受け取るため、ここはアロー関数にしない
  $('.delete-button').on('click', function () {
    const $button = $(this);
    const $row = $button.closest('tr');
    const taskId = $row.data('task-id');
    // どのタスクを消すのかダイアログで示すため、行のタイトルを取り出す
    const title = $row.find('.task-title').text();

    if (!window.confirm(`${title}\nこのタスクを削除します。よろしいですか？`)) {
      return;
    }

    // 応答を待つ間に再度押されると、削除済みのIDへ二重に要求を送ることになるため操作を止める
    $button.prop('disabled', true);

    $.ajax({
      url: buildApiUrl(`api/tasks/${taskId}`),
      method: 'DELETE',
    })
      .done(() => {
        $row.remove();
        switchToEmptyMessageIfNoTask();
      })
      .fail(() => {
        window.alert('削除に失敗しました。');
        // 削除できていないため、再操作できるようボタンを戻す
        $button.prop('disabled', false);
      });
  });
}

/**
 * 表示できるタスクが1件も残っていない場合に、一覧表を隠して代わりのメッセージを表示する。
 *
 * 一覧を開いたときはサーバー側で切り替えているが、削除で0件になった場合はその処理を通らないため、ここで切り替える。
 */
function switchToEmptyMessageIfNoTask() {
  if ($('#task-table tbody tr').length === 0) {
    $('#task-table').prop('hidden', true);
    $('#empty-message').prop('hidden', false);
  }
}

$(() => {
  registerStatusChangeHandler();
  registerDeleteHandler();
});
