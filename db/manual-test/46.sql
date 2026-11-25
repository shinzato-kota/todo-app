-- 手動テストの項目46のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('既存のタスク', 'TODO', 'MEDIUM', 1);
