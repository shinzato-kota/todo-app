-- 手動テストの項目2のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, description, status, priority, sort_order)
VALUES
  ('ステータス確認', '詳細の文', 'TODO', 'MEDIUM', 1),
  ('比べる行', NULL, 'TODO', 'MEDIUM', 2);
