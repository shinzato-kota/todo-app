-- 手動テストの項目15のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  category (name, color)
VALUES
  ('削除済みのカテゴリ', NULL),
  ('残すカテゴリ', NULL);
