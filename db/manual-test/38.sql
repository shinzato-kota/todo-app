-- 手動テストの項目38のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, due_date, priority, sort_order)
VALUES
  ('昨日の未着手', 'TODO', DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), 'MEDIUM', 1),
  ('10日前の進行中', 'IN_PROGRESS', DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), 'MEDIUM', 2),
  ('今日の未着手', 'TODO', CURRENT_DATE, 'MEDIUM', 3),
  ('今日の保留', 'ON_HOLD', CURRENT_DATE, 'MEDIUM', 4),
  ('明日の未着手', 'TODO', DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), 'MEDIUM', 5),
  ('期限なし', 'TODO', NULL, 'MEDIUM', 6);
