/**
 * 入力画面(tasks/form.html、categories/form.html)の処理。
 *
 * 「保存」の連打で、同じ内容が2回送られないようにする。
 */

$(() => {
  registerSubmitHandler();
  registerPageShowHandler();
});

/**
 * フォームを送信したときに、「保存」ボタンを押せなくする関数を設定する。
 */
function registerSubmitHandler() {
  // 入力欄の必須の点検で送信が止まったときに、ボタンを押せなくしないよう、
  // clickでなくsubmitのときに押せなくする
  $('.js-form').on('submit', () => {
    $('.js-save-button').prop('disabled', true);
  });
}

/**
 * bfcacheから入力画面に戻ったときに、「保存」ボタンを押せるように戻す関数を
 * 設定する。
 */
function registerPageShowHandler() {
  $(window).on('pageshow', (event) => {
    // bfcacheは送信した後のページを、ボタンを押せないまま戻すため
    if (event.originalEvent.persisted) {
      $('.js-save-button').prop('disabled', false);
    }
  });
}
