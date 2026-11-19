-- 手動テストの項目10のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('先に消すタスク', 'TODO', 'MEDIUM', 1),
  ('最後のタスク', 'TODO', 'MEDIUM', 2);
