-- 手動テストの項目21のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('1件目のタスク', 'TODO', 'MEDIUM', 1),
  ('2件目のタスク', 'TODO', 'MEDIUM', 2);
