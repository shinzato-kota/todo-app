-- 手動テストの項目49のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, due_date, priority, sort_order)
VALUES
  ('期限切れのタスクA', 'TODO', DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), 'MEDIUM', 1),
  ('期限切れのタスクB', 'TODO', DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY), 'MEDIUM', 2),
  ('期限なしのタスクC', 'TODO', NULL, 'MEDIUM', 3);
