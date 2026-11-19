-- 手動テストの項目4のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('通信失敗確認', 'IN_PROGRESS', 'MEDIUM', 1);
