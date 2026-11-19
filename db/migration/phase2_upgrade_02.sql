-- フェーズ1のサンプルデータを、フェーズ2の新規の環境(02_sample_data.sql)と同じ内容にそろえる
-- フェーズ1の02_sample_data.sqlを実行した環境で、phase2_upgrade_01.sql の後に1回だけ実行する
USE tododb;

INSERT INTO
    category (name, color)
VALUES
    ('仕事', '#1e88e5'),
    ('プライベート', '#43a047'),
    ('勉強', '#8e24aa');

-- 更新日時が自動で変わらないよう、updated_at は元の値のまま指定する
UPDATE task
SET
    due_date = CURRENT_DATE,
    priority = 'HIGH',
    category_id = (
        SELECT
            id
        FROM
            category
        WHERE
            name = '仕事'
    ),
    updated_at = updated_at
WHERE
    title = '要件定義書を確認する';

UPDATE task
SET
    due_date = DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY),
    priority = 'MEDIUM',
    category_id = (
        SELECT
            id
        FROM
            category
        WHERE
            name = '仕事'
    ),
    updated_at = updated_at
WHERE
    title = 'データベースを設計する';

UPDATE task
SET
    description = '期限と優先度の列を追加する\n・期限切れは赤字で表示する\n・優先度はバッジで表示する📝',
    due_date = DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY),
    priority = 'HIGH',
    category_id = (
        SELECT
            id
        FROM
            category
        WHERE
            name = '仕事'
    ),
    updated_at = updated_at
WHERE
    title = '一覧画面を実装する';

UPDATE task
SET
    due_date = CURRENT_DATE,
    priority = 'MEDIUM',
    category_id = (
        SELECT
            id
        FROM
            category
        WHERE
            name = '勉強'
    ),
    updated_at = updated_at
WHERE
    title = '入力チェックを追加する';

UPDATE task
SET
    due_date = DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY),
    priority = 'LOW',
    updated_at = updated_at
WHERE
    title = 'デザインの調整を待つ';

UPDATE task
SET
    due_date = DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY),
    priority = 'LOW',
    category_id = (
        SELECT
            id
        FROM
            category
        WHERE
            name = '仕事'
    ),
    updated_at = updated_at
WHERE
    title = '旧画面の移行対応';

-- 表示順は、今あるタスクの一番下にする
INSERT INTO
    task (
        title,
        description,
        status,
        due_date,
        priority,
        category_id,
        sort_order,
        completed_at,
        created_at,
        updated_at
    )
SELECT
    '歯医者の予約をする',
    NULL,
    'TODO',
    NULL,
    'LOW',
    (
        SELECT
            id
        FROM
            category
        WHERE
            name = 'プライベート'
    ),
    MAX(sort_order) + 1,
    NULL,
    '2026-09-05 09:00:00',
    '2026-09-05 09:00:00'
FROM
    task;
