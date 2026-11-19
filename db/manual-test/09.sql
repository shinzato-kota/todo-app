-- 手動テストの項目9のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('残すタスク1', 'TODO', 'MEDIUM', 1),
  ('削除確認', 'TODO', 'MEDIUM', 2),
  ('残すタスク2', 'TODO', 'MEDIUM', 3);
