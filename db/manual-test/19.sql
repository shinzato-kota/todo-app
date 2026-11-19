-- 手動テストの項目19のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, description, status, priority, sort_order)
VALUES
  ('資料のタイトルA', NULL, 'TODO', 'MEDIUM', 1),
  ('資料のタイトルB', NULL, 'TODO', 'MEDIUM', 2),
  ('詳細に含むA', '資料を添える', 'TODO', 'MEDIUM', 3),
  ('詳細に含むB', '資料を添える', 'TODO', 'MEDIUM', 4),
  ('含まないA', 'メモを添える', 'TODO', 'MEDIUM', 5),
  ('含まないB', 'メモを添える', 'TODO', 'MEDIUM', 6);
