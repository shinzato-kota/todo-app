-- 手動テストの項目22のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('APIの設計', 'IN_PROGRESS', 'MEDIUM', 1),
  ('APIの実装', 'ON_HOLD', 'MEDIUM', 2),
  ('APIのテスト', 'ON_HOLD', 'MEDIUM', 3),
  ('画面の設計', 'ON_HOLD', 'MEDIUM', 4),
  ('画面の実装', 'IN_PROGRESS', 'MEDIUM', 5);
