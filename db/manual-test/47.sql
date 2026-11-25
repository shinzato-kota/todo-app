-- 手動テストの項目47のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  category (name, color)
VALUES
  ('仕事', '#1e88e5'),
  ('勉強', '#8e24aa');
