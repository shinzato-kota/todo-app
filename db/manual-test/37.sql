-- 手動テストの項目37のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('タスクA', 'TODO', 'MEDIUM', 1);
