-- 手動テストの項目17のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (
    title,
    status,
    due_date,
    priority,
    sort_order,
    completed_at
  )
VALUES
  ('今日の未着手', 'TODO', CURRENT_DATE, 'MEDIUM', 1, NULL),
  ('今日の保留', 'ON_HOLD', CURRENT_DATE, 'MEDIUM', 2, NULL),
  ('今日の完了', 'DONE', CURRENT_DATE, 'MEDIUM', 3, CURRENT_TIMESTAMP),
  ('今日の中止', 'CANCELLED', CURRENT_DATE, 'MEDIUM', 4, NULL),
  ('昨日の未着手', 'TODO', DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 'MEDIUM', 5, NULL),
  ('10日前の進行中', 'IN_PROGRESS', DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), 'MEDIUM', 6, NULL),
  ('昨日の完了', 'DONE', DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 'MEDIUM', 7, CURRENT_TIMESTAMP),
  ('昨日の中止', 'CANCELLED', DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 'MEDIUM', 8, NULL),
  ('明日の未着手', 'TODO', DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), 'MEDIUM', 9, NULL),
  ('期限なし', 'TODO', NULL, 'MEDIUM', 10, NULL);
