-- 手動テストの項目5のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('既存のタスク1', 'TODO', 'MEDIUM', 1),
  ('既存のタスク2', 'TODO', 'MEDIUM', 2);
