-- フェーズ1の環境をフェーズ2へ上げる変更(カテゴリ・期限・優先度・詳細・並び順)
-- フェーズ1の01_schema.sqlで作ったtododbに、1回だけ実行する。新しく環境を作る場合は実行しない(01_schema.sqlがフェーズ2の形になっている)
USE tododb;

CREATE TABLE category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'カテゴリID',
  name VARCHAR(20) NOT NULL COMMENT 'カテゴリ名',
  color VARCHAR(7) NULL COMMENT 'ラベル色',
  CONSTRAINT uk_category_name UNIQUE (name)
) COMMENT = 'カテゴリ管理テーブル';

ALTER TABLE task
ADD COLUMN description TEXT NULL COMMENT '詳細' AFTER title,
ADD COLUMN due_date DATE NULL COMMENT '期限' AFTER status,
ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '優先度' AFTER due_date,
ADD COLUMN category_id BIGINT NULL COMMENT 'カテゴリID' AFTER priority,
-- 既存のタスクに値を入れるための仮の既定値。表示順を振り終えたら、このファイルの最後で外す
ADD COLUMN sort_order INT NOT NULL DEFAULT -1 COMMENT '表示順' AFTER category_id,
ADD CONSTRAINT fk_task_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE SET NULL;

-- 既存のタスクは、フェーズ1の一覧と同じ並び(登録日時の昇順、同じならIDの昇順)で1から振る
-- 表示順を振っただけで更新日時が変わらないよう、updated_at は元の値のまま指定する
UPDATE task t
SET
  t.sort_order = (
    SELECT
      n.rn
    FROM
      (
        SELECT
          id,
          ROW_NUMBER() OVER (
            ORDER BY
              created_at,
              id
          ) AS rn
        FROM
          task
      ) n
    WHERE
      n.id = t.id
  ),
  t.updated_at = t.updated_at;

-- 値を振り終えたら既定値を外し、sort_orderを指定しない登録を誤りとして止める
ALTER TABLE task
ALTER COLUMN sort_order
DROP DEFAULT;
