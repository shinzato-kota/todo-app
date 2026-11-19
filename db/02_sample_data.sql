-- 動作確認用のサンプルデータ
-- 01_schema.sql の後に実行する。期限は実行した日を基準に決まるため、いつ実行しても期限切れ・今日が期限の表示を確認できる
USE tododb;

INSERT INTO
  category (name, color)
VALUES
  ('仕事', '#1e88e5'),
  ('プライベート', '#43a047'),
  ('勉強', '#8e24aa');

INSERT INTO
  task (
    title,
    description,
    status,
    due_date,
    priority,
    category_id,
    sort_order,
    completed_at,
    created_at,
    updated_at
  )
VALUES
  (
    '要件定義書を確認する',
    NULL,
    'DONE',
    CURRENT_DATE,
    'HIGH',
    1,
    1,
    '2026-09-01 10:30:00',
    '2026-09-01 09:00:00',
    '2026-09-01 10:30:00'
  ),
  (
    'データベースを設計する',
    NULL,
    'DONE',
    DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY),
    'MEDIUM',
    1,
    2,
    '2026-09-02 17:00:00',
    '2026-09-01 09:05:00',
    '2026-09-02 17:00:00'
  ),
  (
    '一覧画面を実装する',
    '期限と優先度の列を追加する\n・期限切れは赤字で表示する\n・優先度はバッジで表示する📝',
    'IN_PROGRESS',
    DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY),
    'HIGH',
    1,
    3,
    NULL,
    '2026-09-03 09:00:00',
    '2026-09-03 09:00:00'
  ),
  (
    '入力チェックを追加する',
    NULL,
    'TODO',
    CURRENT_DATE,
    'MEDIUM',
    3,
    4,
    NULL,
    '2026-09-03 09:10:00',
    '2026-09-03 09:10:00'
  ),
  (
    'デザインの調整を待つ',
    NULL,
    'ON_HOLD',
    DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY),
    'LOW',
    NULL,
    5,
    NULL,
    '2026-09-04 11:00:00',
    '2026-09-04 11:00:00'
  ),
  (
    '旧画面の移行対応',
    NULL,
    'CANCELLED',
    DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY),
    'LOW',
    1,
    6,
    NULL,
    '2026-09-04 11:20:00',
    '2026-09-05 14:00:00'
  ),
  (
    '歯医者の予約をする',
    NULL,
    'TODO',
    NULL,
    'LOW',
    2,
    7,
    NULL,
    '2026-09-05 09:00:00',
    '2026-09-05 09:00:00'
  );
