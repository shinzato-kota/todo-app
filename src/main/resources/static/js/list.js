/**
 * タスク一覧画面(tasks/list.html)の非同期処理。
 *
 * 絞り込み、並び替え、ステータス変更、削除を、画面を再読み込みせずに反映する。
 */

// ESLint用: common.jsで定義した関数のため、定義されていないと判定しないよう知らせる
/* global buildApiUrl, toClassValue */

/**
 * キーワードを打ち終わってから、絞り込みのリクエストを送るまでの待ち時間(ミリ秒)。
 *
 * @type {number}
 */
const KEYWORD_WAIT_MILLISECONDS = 500;

/**
 * 選んでいる絞り込みの条件。
 *
 * 空文字は「すべて」(条件を付けない)を表す。
 * 「すべて」には、絞り込みの欄のボタンの値(data-value="")と、プルダウンの値(value="")をそのまま入れる。
 * キーはAPIの条件の名前と同じにする。
 *
 * @type {{status: string, due: string, priority: string, keyword: string, category: string}}
 */
const filterConditions = {
  status: '',
  due: '',
  priority: '',
  keyword: '',
  category: '',
};

/**
 * 最後に送った絞り込みのリクエスト。
 *
 * レスポンスが返る前に次のリクエストを送るときに、これを中止する。
 *
 * @type {(jqXHR|null)}
 */
let filterRequest = null;

/**
 * キーワードの待ち時間を数えているタイマーのID。
 *
 * 入力されるたびに、これで前のタイマーを止めて数え直す。
 *
 * @type {(number|null)}
 */
let keywordTimerId = null;

/**
 * 絞り込みのリクエストを送ってから、行を作り直すか失敗するまでの間かどうか。
 *
 * @type {boolean}
 */
let isFilterWaiting = false;

/**
 * 並び替えのレスポンスを待っている間かどうか。
 *
 * @type {boolean}
 */
let isReorderWaiting = false;

/**
 * 並び替えのレスポンスを待つ間に絞り込みの条件が変わり、レスポンスの後に絞り込みのリクエストを送る必要があるかどうか。
 *
 * @type {boolean}
 */
let hasDeferredFilter = false;

/**
 * レスポンスを待っている、行の「削除」とステータス変更のリクエストの数。
 *
 * @type {number}
 */
let rowRequestCount = 0;

$(() => {
  setUpSortable();
  registerFilterButtonHandler();
  registerCategorySelectHandler();
  registerKeywordInputHandler();
  registerStatusChangeHandler();
  registerDeleteHandler();
  showAlertMessage();
});

/**
 * タスクの行を、「≡」をつかんでドラッグして並び替えられるようにする。
 *
 * 行を離したら、並びをAPIへ送る。
 */
function setUpSortable() {
  $('.js-task-table tbody').sortable({
    handle: '.js-sort-handle',
    // つかんだ行に position: absolute が付くと、セルの幅が中身の文字の量で決まるようになり縮むため、付く前に固定する。startでは間に合わない
    helper: (event, $row) => {
      $row.children().each(function () {
        $(this).width($(this).width());
      });
      return $row;
    },
    start: () => {
      // 押しただけで動かしていない間に絞り込みのリクエストを送ると、動かし始めたときにここへ来る
      // この中で直接取り消すとエラーになるため、後で取り消す
      if (isFilterWaiting) {
        setTimeout(cancelDragging);
      }
    },
    update: () => {
      // ドラッグの取り消しでもここへ来るため、絞り込みで止めている間は送らない
      if (!isFilterWaiting) {
        sendReorderRequest();
      }
    },
  });
}

/**
 * ドラッグしている途中なら、ドラッグを取り消して行を元の位置に戻す。
 */
function cancelDragging() {
  // cancelはドラッグの後に呼んでも直前の並び替えを戻すため、ドラッグ中（helperのクラスがある間）だけ呼ぶ
  if ($('.ui-sortable-helper').length > 0) {
    $('.js-task-table tbody').sortable('cancel');
  }
}

/**
 * 今の行の並びを、並び替えのAPIへ送る。
 *
 * 一覧が古くなっていた場合(409)は、知らせを出して画面を再読み込みする。
 * それ以外で失敗した場合は、知らせを出して行を元の位置に戻す。
 */
function sendReorderRequest() {
  const taskIds = $('.js-task-table tbody tr')
    .map((index, row) => $(row).data('task-id'))
    .get();

  isReorderWaiting = true;
  updateSortableState();
  // 待つ間に行が消えると、失敗したときに動かした行を元の位置に戻せないため、「削除」も止める
  disableRowControls();

  $.ajax({
    url: buildApiUrl('api/tasks/reorder'),
    method: 'PUT',
    contentType: 'application/json',
    data: JSON.stringify({ taskIds }),
  })
    .done(finishReorderWaiting)
    .fail((jqXHR) => {
      if (jqXHR.status === 409) {
        // 再読み込みで一覧を取り直すため、止めた操作は戻さず、待っている絞り込みも送らない
        window.alert('並び替えに失敗しました。画面を再読み込みします。');
        window.location.reload();
        return;
      }
      window.alert('並び替えに失敗しました。');
      $('.js-task-table tbody').sortable('cancel');
      finishReorderWaiting();
    });
}

/**
 * 行をつかめるかを、次の4つの状態に合わせて切り替える。
 *
 * 次のどれか1つにでも当たる間は、行をつかめないようにする。どれにも当たらなければ、つかめるようにする。
 * - 「すべて」でない絞り込みの条件を、1つでも選んでいる（isFilterApplied()）
 * - 絞り込みのレスポンスを待っている（isFilterWaiting）
 * - 並び替えのレスポンスを待っている（isReorderWaiting）
 * - 行の「削除」かステータス変更のレスポンスを、1つでも待っている（rowRequestCount）
 */
function updateSortableState() {
  const isStopped =
    isFilterApplied() ||
    isFilterWaiting ||
    isReorderWaiting ||
    rowRequestCount > 0;
  $('.js-task-table tbody').sortable(isStopped ? 'disable' : 'enable');
}

/**
 * 絞り込みをかけているかを返す。
 *
 * @returns {boolean} 「すべて」でない条件が1つでもあれば true
 */
function isFilterApplied() {
  return Object.values(filterConditions).some((value) => value !== '');
}

/**
 * 行の「削除」とステータス変更の操作を止める。
 */
function disableRowControls() {
  $('.js-status-select, .js-delete-button').prop('disabled', true);
}

/**
 * 行の「削除」とステータス変更の操作を、できるようにする。
 */
function enableRowControls() {
  $('.js-status-select, .js-delete-button').prop('disabled', false);
}

/**
 * 並び替えのレスポンスを待つ状態を終え、止めていた操作を戻す。
 *
 * 待つ間に絞り込みの条件が変わっていたら、最後の条件で絞り込みのリクエストを送る。
 */
function finishReorderWaiting() {
  isReorderWaiting = false;
  enableRowControls();
  if (hasDeferredFilter) {
    hasDeferredFilter = false;
    requestFilteredTasks();
  } else {
    updateSortableState();
  }
}

/**
 * 選んでいる条件で絞り込みのAPIを呼ぶ。
 *
 * 並び替えのレスポンスを待っている間は、絞り込みのリクエストを送らず、並び替えのレスポンスが返ってから送る。
 * 絞り込みのリクエストを送ってから、行を作り直すか失敗するまでの間は、行をつかめないようにする。行をドラッグしている途中なら、その行を元の位置に戻す。
 */
function requestFilteredTasks() {
  // 保存が確定する前にタスクを読むと、古い並びで表示されることがあるため、レスポンスを待つ
  if (isReorderWaiting) {
    hasDeferredFilter = true;
    return;
  }

  // 前のリクエストのレスポンスが後から届くと、新しい条件の結果が古い条件の結果で上書きされるため、中止する
  if (filterRequest !== null) {
    filterRequest.abort();
  }

  isFilterWaiting = true;
  updateSortableState();
  cancelDragging();

  filterRequest = $.ajax({
    url: buildApiUrl('api/tasks'),
    method: 'GET',
    dataType: 'json',
    data: buildFilterParams(),
  })
    .done(showFilteredTasks)
    .fail(handleFilterFailure);
}

/**
 * 選んでいる条件のうち、「すべて」でない条件だけを、APIへ送る形にする。
 *
 * @returns {Object} クエリ文字列にする条件
 */
function buildFilterParams() {
  return Object.fromEntries(
    Object.entries(filterConditions).filter(([, value]) => value !== ''),
  );
}

/**
 * 絞り込みのAPIが返したタスクで、表の行を作り直し、行をつかめるかを切り替える。
 *
 * @param {Object[]} tasks 絞り込みのAPIが返したタスクの一覧
 */
function showFilteredTasks(tasks) {
  const $tbody = $('.js-task-table tbody');
  // 「≡」を押したまま行が消えた後に動かすと、jQuery UIの中でエラーになるため、作り直して押している状態を終わらせる
  $tbody.sortable('destroy');
  $tbody.empty().append(tasks.map(createTaskRow));
  setUpSortable();
  switchListDisplay();
  isFilterWaiting = false;
  updateSortableState();
}

/**
 * 絞り込みのAPIが返したタスク1件から、表の行をテンプレートから作る。
 *
 * @param {Object} task 絞り込みのAPIが返したタスク
 * @returns {jQuery} 表の行
 */
function createTaskRow(task) {
  const content = $('.js-task-row-template').prop('content');
  const $row = $(document.importNode(content, true).firstElementChild);

  // 一覧を開いたときにサーバーが作る行と同じ形にするため、値は属性に書く
  $row.attr('data-task-id', task.id);
  $row.attr('data-due-date-status', task.dueDateStatus);
  $row.addClass(`task-table-row--status_${toClassValue(task.status)}`);
  $row.addClass(`task-table-row--due_${toClassValue(task.dueDateStatus)}`);

  if (isFilterApplied()) {
    $row.find('.js-sort-handle').remove();
  }

  fillTitleAndDescription($row, task);
  fillCategoryLabel($row, task.category);
  $row.find('.js-priority-cell').append(createPriorityBadge(task.priority));
  fillDueDate($row, task.dueDate);
  fillStatusSelect($row, task.status);

  const $editLink = $row.find('.js-edit-link');
  $editLink.attr('href', `${$editLink.attr('href')}/${task.id}/edit`);

  return $row;
}

/**
 * タスクの行に、タイトルと詳細を入れる。詳細がないタスクは、詳細の要素を取り除く。
 *
 * @param {jQuery} $row テンプレートから作ったタスクの行
 * @param {Object} task 絞り込みのAPIが返したタスク
 */
function fillTitleAndDescription($row, task) {
  // 利用者が入力した文字列をHTMLとして解釈させないため、文字列は.text()で入れる
  $row.find('.js-task-title').text(task.title);
  if (task.description === null) {
    $row.find('.js-task-description').remove();
  } else {
    $row.find('.js-task-description').text(task.description);
  }
}

/**
 * タスクの行に、カテゴリの色の印と名前を入れる。カテゴリなしのタスクは、カテゴリの要素を取り除く。
 *
 * @param {jQuery} $row テンプレートから作ったタスクの行
 * @param {(Object|null)} category 絞り込みのAPIが返したタスクのカテゴリ。カテゴリなしのタスクは null
 */
function fillCategoryLabel($row, category) {
  if (category === null) {
    $row.find('.js-category-label').remove();
    return;
  }
  // 利用者が入力した文字列をHTMLとして解釈させないため、文字列は.text()で入れる
  $row.find('.js-category-name').text(category.name);
  if (category.color === null) {
    $row.find('.js-category-label').addClass('category-label--no-color');
  } else {
    $row.find('.js-category-color').css('color', category.color);
  }
}

/**
 * 優先度のバッジを、テンプレートを複製して作る。
 *
 * @param {string} priority 優先度の列挙型の値
 * @returns {jQuery} 優先度のバッジ
 */
function createPriorityBadge(priority) {
  const content = $('.js-priority-badge-template').prop('content');
  return $(content)
    .find(`.js-priority-badge[data-priority="${priority}"]`)
    .clone();
}

/**
 * タスクの行に、期限日を yyyy/MM/dd の形で入れる。期限のないタスクは、期限日と「期限切れ」のラベルの要素を取り除く。
 *
 * @param {jQuery} $row テンプレートから作ったタスクの行
 * @param {(string|null)} dueDate 絞り込みのAPIが返した yyyy-MM-dd の期限日。期限のないタスクは null
 */
function fillDueDate($row, dueDate) {
  if (dueDate === null) {
    $row.find('.js-due-date').remove();
    $row.find('.js-overdue-label').remove();
  } else {
    // APIは yyyy-MM-dd で返すため、一覧を開いたときと同じ yyyy/MM/dd にそろえる
    $row.find('.js-due-date').text(dueDate.replaceAll('-', '/'));
  }
}

/**
 * タスクの行のステータスのプルダウンで、タスクのステータスを選び、ステータスのクラスと、失敗したときに戻す値を付ける。
 *
 * @param {jQuery} $row テンプレートから作ったタスクの行
 * @param {string} status ステータスの列挙型の値
 */
function fillStatusSelect($row, status) {
  const $select = $row.find('.js-status-select');
  $select.val(status);
  $select.addClass(`status-select--status_${toClassValue(status)}`);
  $select.attr('data-previous-status', status);
}

/**
 * 表の行の数と、絞り込みの条件を選んでいるかに合わせて、表と0件のメッセージのうち1つだけを表示する。
 *
 * 行があれば表、行がなく条件を選んでいなければ「タスクはありません」、条件を選んでいれば「条件に合うタスクはありません」を出す。
 * 行があれば表の上の文も出し、条件を選んでいなければ並び替えのやり方の文、条件を選んでいれば絞り込み中の文にする。
 */
function switchListDisplay() {
  hideListContents();
  if ($('.js-task-table tbody tr').length > 0) {
    const filterApplied = isFilterApplied();
    $('.js-task-table, .js-sort-notice').prop('hidden', false);
    $('.js-sort-guide').prop('hidden', filterApplied);
    $('.js-filter-notice').prop('hidden', !filterApplied);
    return;
  }
  const messageSelector = isFilterApplied()
    ? '.js-no-match-message'
    : '.js-empty-message';
  $(messageSelector).prop('hidden', false);
}

/**
 * 表と、0件・取得の失敗のメッセージと、表の上の文を、すべて隠す。
 */
function hideListContents() {
  $(
    '.js-task-table, .js-empty-message, .js-no-match-message, .js-load-error-message, .js-sort-notice',
  ).prop('hidden', true);
}

/**
 * 絞り込みのリクエストが失敗したときに、知らせを出し、表の場所にタスクを取得できなかったことを表示する。
 *
 * 新しいリクエストを送るために中止した場合は、何もしない。
 *
 * @param {jqXHR} jqXHR 絞り込みのリクエストのjqXHR
 * @param {string} textStatus 失敗の種類。中止した場合は 'abort'
 */
function handleFilterFailure(jqXHR, textStatus) {
  // 中止したのは新しいリクエストを出すためで、失敗ではないため、何も表示しない
  if (textStatus === 'abort') {
    return;
  }
  // 画面の条件は利用者が最後に操作したとおりに残し、古い結果がその条件の結果に見えないよう表を隠す
  window.alert('絞り込みに失敗しました。');
  showLoadError();
  isFilterWaiting = false;
  updateSortableState();
}

/**
 * 絞り込みに失敗したときに、表と0件のメッセージを隠し、タスクを取得できなかったことを表の場所に表示する。
 */
function showLoadError() {
  hideListContents();
  $('.js-load-error-message').prop('hidden', false);
}

/**
 * 絞り込みのボタンを押したときに、選んだ条件で絞り込みのリクエストを送る関数を設定する。
 *
 * 押したボタンだけを同じ条件の中で選ばれた状態にし、絞り込みのリクエストを送る。
 */
function registerFilterButtonHandler() {
  // イベントが発生した要素をthisで受け取るため、ここはアロー関数にしない
  $('.js-filter-button').on('click', function () {
    const $button = $(this);
    const $group = $button.closest('.js-filter-group');

    // 見た目はクラスで決め、aria-pressedはスクリーンリーダーに選ばれているかを伝えるために付ける
    $group
      .find('.js-filter-button')
      .removeClass('filter-button--selected')
      .attr('aria-pressed', 'false');
    $button.addClass('filter-button--selected').attr('aria-pressed', 'true');

    filterConditions[$group.data('condition')] = $button.data('value');
    requestFilteredTasks();
  });
}

/**
 * カテゴリのプルダウンを変えたときに、選んだカテゴリで絞り込みのリクエストを送る関数を設定する。
 */
function registerCategorySelectHandler() {
  // イベントが発生した要素をthisで受け取るため、ここはアロー関数にしない
  $('.js-category-select').on('change', function () {
    filterConditions.category = $(this).val();
    requestFilteredTasks();
  });
}

/**
 * キーワードの入力欄に入力したときに、待ち時間の後で絞り込みのリクエストを送る関数を設定する。
 */
function registerKeywordInputHandler() {
  const $input = $('.js-keyword-input');
  $input.on('input', (event) => {
    // 日本語の変換中は送らず、確定したとき(compositionend)に送る
    if (event.originalEvent.isComposing) {
      return;
    }
    scheduleKeywordFilter($input.val());
  });
  $input.on('compositionend', () => {
    scheduleKeywordFilter($input.val());
  });
}

/**
 * キーワードの入力が止まってから待ち時間が過ぎたら、そのキーワードで絞り込みのリクエストを送る。
 *
 * 待ち時間の間にまた入力されたら、待ち直す。
 *
 * @param {string} keyword 入力欄のキーワード
 */
function scheduleKeywordFilter(keyword) {
  clearTimeout(keywordTimerId);
  keywordTimerId = setTimeout(() => {
    // 空白だけのキーワードは、条件なしにする
    filterConditions.keyword = keyword.trim();
    requestFilteredTasks();
  }, KEYWORD_WAIT_MILLISECONDS);
}

/**
 * 行のステータスのプルダウンを変えたときに、ステータスの変更のリクエストを送る関数を設定する。
 */
function registerStatusChangeHandler() {
  // 絞り込みのたびに行は作り直す。作り直した行のプルダウンにも効くよう、表を対象にする
  // イベントが発生した要素をthisで受け取るため、ここはアロー関数にしない
  $('.js-task-table').on('change', '.js-status-select', function () {
    sendStatusChangeRequest($(this));
  });
}

/**
 * プルダウンで選んだステータスへの変更のリクエストを送る。
 *
 * レスポンスを待つ間は、そのプルダウンの操作と、行をつかむことを止める。
 *
 * @param {jQuery} $select 選択が変わったステータスのプルダウン
 */
function sendStatusChangeRequest($select) {
  const $row = $select.closest('tr');
  const taskId = $row.data('task-id');
  const newStatus = $select.val();
  // 通信に失敗した場合の復元先として、変更前の値を控える
  const previousStatus = $select.data('previous-status');

  // レスポンスが返る前に次の変更を受け付けると、後から返ったレスポンスで表示が上書きされるため操作を止める
  $select.prop('disabled', true);
  startRowRequest();

  $.ajax({
    url: buildApiUrl(`api/tasks/${taskId}/status`),
    method: 'PATCH',
    contentType: 'application/json',
    dataType: 'json',
    data: JSON.stringify({ status: newStatus }),
  })
    .done((response) => {
      updateStatusDisplay($row, $select, previousStatus, response);
    })
    .fail((jqXHR) => {
      handleStatusChangeFailure(jqXHR, $row, $select, previousStatus);
    })
    .always(() => {
      // 失敗時も操作不可のまま残さない
      $select.prop('disabled', false);
      finishRowRequest();
    });
}

/**
 * 行の「削除」かステータス変更のリクエストを送るときに、待っているリクエストの数を1つ増やし、行をつかめないようにする。
 */
function startRowRequest() {
  rowRequestCount += 1;
  updateSortableState();
}

/**
 * 行の「削除」かステータス変更のレスポンスが返ったときに、待っているリクエストの数を1つ減らし、行をつかめるかを切り替える。
 */
function finishRowRequest() {
  rowRequestCount -= 1;
  updateSortableState();
}

/**
 * ステータスの変更のレスポンスに合わせて、行とプルダウンのステータスのクラスと、行の期限の状況のクラスを付け替える。
 *
 * @param {jQuery} $row ステータスを変えたタスクの行
 * @param {jQuery} $select ステータスを変えたプルダウン
 * @param {string} previousStatus 変更する前のステータスの列挙型の値
 * @param {Object} response ステータスの変更のAPIが返したタスク
 */
function updateStatusDisplay($row, $select, previousStatus, response) {
  $row.removeClass(`task-table-row--status_${toClassValue(previousStatus)}`);
  $row.addClass(`task-table-row--status_${toClassValue(response.status)}`);
  $select.removeClass(`status-select--status_${toClassValue(previousStatus)}`);
  $select.addClass(`status-select--status_${toClassValue(response.status)}`);
  // 完了・中止にすると期限切れでなくなるなど、ステータスで期限の状況が変わるため、サーバーの判定で付け替える
  $row.removeClass(
    `task-table-row--due_${toClassValue($row.data('due-date-status'))}`,
  );
  $row.addClass(`task-table-row--due_${toClassValue(response.dueDateStatus)}`);
  $row.data('due-date-status', response.dueDateStatus);
  // 次回の失敗時に正しい値へ戻せるよう、控えている値を更新する
  $select.data('previous-status', response.status);
}

/**
 * ステータスの変更のリクエストが失敗したときに、知らせを出す。
 *
 * タスクが見つからなかった場合(404)は、行を取り除く。それ以外の場合は、プルダウンの選択を変更する前のステータスに戻す。
 *
 * @param {jqXHR} jqXHR ステータスの変更のリクエストのjqXHR
 * @param {jQuery} $row ステータスを変えようとしたタスクの行
 * @param {jQuery} $select ステータスを変えようとしたプルダウン
 * @param {string} previousStatus 変更する前のステータスの列挙型の値
 */
function handleStatusChangeFailure(jqXHR, $row, $select, previousStatus) {
  if (jqXHR.status === 404) {
    // ほかの画面で削除されていたため、削除に成功したときと同じく行を取り除く
    window.alert('タスクが見つかりません。');
    $row.remove();
    switchListDisplay();
    return;
  }
  // 画面の表示とデータベースの状態が食い違わないよう、選択を元に戻す
  $select.val(previousStatus);
  window.alert('ステータスの変更に失敗しました。');
}

/**
 * 行の「削除」ボタンを押したときに、確認してからタスクの削除のリクエストを送る関数を設定する。
 */
function registerDeleteHandler() {
  // 絞り込みのたびに行は作り直す。作り直した行のボタンにも効くよう、表を対象にする
  // イベントが発生した要素をthisで受け取るため、ここはアロー関数にしない
  $('.js-task-table').on('click', '.js-delete-button', function () {
    sendDeleteRequest($(this));
  });
}

/**
 * 確認ダイアログで同意が得られた場合のみ、押されたボタンの行のタスクの削除のリクエストを送り、成功した行を画面から取り除く。
 *
 * レスポンスを待つ間は、そのボタンの操作と、行をつかむことを止める。
 *
 * @param {jQuery} $button 押された「削除」ボタン
 */
function sendDeleteRequest($button) {
  const $row = $button.closest('tr');
  const taskId = $row.data('task-id');
  // どのタスクを消すのかダイアログで示すため、行のタイトルを取り出す
  const title = $row.find('.js-task-title').text();

  if (!window.confirm(`${title}\nこのタスクを削除します。よろしいですか？`)) {
    return;
  }

  // レスポンスを待つ間に再度押されると、削除済みのIDへ二重にリクエストを送ることになるため操作を止める
  $button.prop('disabled', true);
  startRowRequest();

  $.ajax({
    url: buildApiUrl(`api/tasks/${taskId}`),
    method: 'DELETE',
  })
    .done(() => {
      $row.remove();
      switchListDisplay();
    })
    .fail((jqXHR) => {
      handleDeleteFailure(jqXHR, $row, $button);
    })
    .always(finishRowRequest);
}

/**
 * タスクの削除のリクエストが失敗したときに、知らせを出す。
 *
 * タスクが見つからなかった場合(404)は、行を取り除く。それ以外の場合は、もう一度押せるようボタンを戻す。
 *
 * @param {jqXHR} jqXHR 削除のリクエストのjqXHR
 * @param {jQuery} $row 削除しようとしたタスクの行
 * @param {jQuery} $button 押された「削除」ボタン
 */
function handleDeleteFailure(jqXHR, $row, $button) {
  if (jqXHR.status === 404) {
    // ほかの画面で削除されていたため、削除に成功したときと同じく行を取り除く
    window.alert('タスクが見つかりません。');
    $row.remove();
    switchListDisplay();
    return;
  }
  window.alert('削除に失敗しました。');
  // 削除できていないため、再操作できるようボタンを戻す
  $button.prop('disabled', false);
}

/**
 * サーバーから知らせの文を渡された場合に、ダイアログで表示する。
 *
 * 編集画面を開くときや保存するときにタスクが見つからず、一覧へ戻された場合に渡される。
 */
function showAlertMessage() {
  const message = $('body').data('alert-message');
  if (message) {
    window.alert(message);
  }
}
