-- 手動テストの項目13のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('削除済みのステータス変更', 'TODO', 'MEDIUM', 1),
  ('残すタスク', 'TODO', 'MEDIUM', 2);
