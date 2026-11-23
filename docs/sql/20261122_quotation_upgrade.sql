-- 仅用于已执行旧版报价建表定义的库。新版 schema.sql 或 20261122_quotation.sql 已包含这些字段。
BEGIN;

ALTER TABLE quotation
    ADD COLUMN order_generated boolean DEFAULT false NOT NULL,
    ADD COLUMN version bigint DEFAULT 0 NOT NULL,
    ADD COLUMN IF NOT EXISTS contact_email varchar(256);

ALTER TABLE quotation
    ADD CONSTRAINT ck_quotation_version CHECK (
        version >= 0
    );

COMMENT ON COLUMN quotation.order_generated IS '是否已生成订单';
COMMENT ON COLUMN quotation.version IS '报价版本，由应用在修改、删除及转单时递增';
COMMENT ON COLUMN quotation.contact_email IS '本单联系人电子邮件，预留字段';

ALTER TABLE quotation_template
    ADD COLUMN title varchar(256) DEFAULT '报价单' NOT NULL;

ALTER TABLE quotation_template
    ALTER COLUMN title DROP DEFAULT;

COMMENT ON COLUMN quotation_template.title IS '套用模板时填入报价表单的标题';

COMMIT;
