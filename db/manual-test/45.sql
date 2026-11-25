-- 手動テストの項目45のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('既存のタスクA', 'TODO', 'MEDIUM', 1),
  ('既存のタスクB', 'TODO', 'MEDIUM', 2);
