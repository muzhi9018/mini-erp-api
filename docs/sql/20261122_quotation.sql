-- 报价单第一版建表脚本，对应 docs/quotation-implementation-plan.md。
-- 未建报价表的已有库在商品表建好后执行。新库由 schema.sql 建表，已建旧表的库执行报价升级脚本。
-- 初始化系统菜单及角色后执行 20261122_quotation_permissions.sql 配置报价权限。
-- 报价主表保留全部已发单号。取号在应用事务内按编号日期加锁，再查询当天的最大流水，包含逻辑删除记录。
-- 关联沿用项目的逻辑 ID。商品快照、数量精度、全单币种和金额计算由 Service 在保存事务中校验。
BEGIN;

CREATE TABLE quotation (
    id                        bigint NOT NULL PRIMARY KEY,
    gmt_create                timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified              timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user               bigint,
    update_user               bigint,
    is_deleted                boolean DEFAULT false NOT NULL,
    quote_number              varchar(16) NOT NULL,
    number_date               date NOT NULL,
    daily_sequence            smallint NOT NULL,
    owner_user_id             bigint NOT NULL,
    title                     varchar(256) DEFAULT '报价单' NOT NULL,
    quoted_at                 timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    currency                  varchar(4) NOT NULL,
    order_generated           boolean DEFAULT false NOT NULL,
    version                   bigint DEFAULT 0 NOT NULL,
    customer_name             varchar(256),
    contact_name              varchar(64),
    contact_phone             varchar(32),
    contact_email             varchar(256),
    shipping_address          varchar(512),
    remark                    varchar(1024),
    issuer_name               varchar(256) NOT NULL,
    issuer_contact            varchar(512),
    original_amount           numeric(20,6) NOT NULL,
    subtotal_amount           numeric(20,6) NOT NULL,
    discount_rate             numeric(5,2),
    discounted_amount         numeric(20,6),
    total_amount              numeric(20,6) NOT NULL
);

CREATE UNIQUE INDEX uk_quotation_quote_number
    ON quotation (quote_number);

CREATE UNIQUE INDEX uk_quotation_number_date_sequence
    ON quotation (number_date, daily_sequence);

CREATE INDEX idx_quotation_owner_quoted_at
    ON quotation (owner_user_id, quoted_at DESC, id DESC)
    WHERE is_deleted = false;

ALTER TABLE quotation
    ADD CONSTRAINT ck_quotation_daily_sequence CHECK (
        daily_sequence BETWEEN 1 AND 999
    );

ALTER TABLE quotation
    ADD CONSTRAINT ck_quotation_currency CHECK (
        currency IN ('CNY', 'TWD')
    );

ALTER TABLE quotation
    ADD CONSTRAINT ck_quotation_version CHECK (
        version >= 0
    );

ALTER TABLE quotation
    ADD CONSTRAINT ck_quotation_amounts CHECK (
        original_amount >= 0 AND original_amount <> 'NaN'
        AND subtotal_amount BETWEEN 0 AND original_amount
        AND total_amount BETWEEN 0 AND subtotal_amount
    );

ALTER TABLE quotation
    ADD CONSTRAINT ck_quotation_discount CHECK (
        (discount_rate IS NULL OR discounted_amount IS NULL)
        AND (discount_rate IS NULL OR discount_rate BETWEEN 0 AND 100)
        AND (discounted_amount IS NULL OR discounted_amount BETWEEN 0 AND subtotal_amount)
    );

COMMENT ON TABLE quotation IS '正式报价单，保存客户、开单方及计价结果作为历史记录';

COMMENT ON COLUMN quotation.id IS '主键';
COMMENT ON COLUMN quotation.gmt_create IS '创建时间';
COMMENT ON COLUMN quotation.gmt_modified IS '更新时间';
COMMENT ON COLUMN quotation.create_user IS '创建者';
COMMENT ON COLUMN quotation.update_user IS '更新者';
COMMENT ON COLUMN quotation.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN quotation.quote_number IS '正式报价单号，全表唯一，逻辑删除后不复用';
COMMENT ON COLUMN quotation.number_date IS '正式保存时按台北时区取得的编号日期';
COMMENT ON COLUMN quotation.daily_sequence IS '同一编号日的流水号，两种币种共用 1～999';
COMMENT ON COLUMN quotation.owner_user_id IS '报价所属系统用户 ID，由登录身份写入';
COMMENT ON COLUMN quotation.title IS '报价标题，默认报价单';
COMMENT ON COLUMN quotation.quoted_at IS '报价业务时间，可由用户指定';
COMMENT ON COLUMN quotation.currency IS '全单售价币种：CNY=人民币，TWD=新台币';
COMMENT ON COLUMN quotation.order_generated IS '是否已生成订单';
COMMENT ON COLUMN quotation.version IS '报价版本，由应用在修改、删除及转单时递增';
COMMENT ON COLUMN quotation.customer_name IS '本单客户名称';
COMMENT ON COLUMN quotation.contact_name IS '本单客户联系人';
COMMENT ON COLUMN quotation.contact_phone IS '本单客户联系电话';
COMMENT ON COLUMN quotation.contact_email IS '本单联系人电子邮件，预留字段';
COMMENT ON COLUMN quotation.shipping_address IS '本单收货地址';
COMMENT ON COLUMN quotation.remark IS '客户版可见的整单备注';
COMMENT ON COLUMN quotation.issuer_name IS '正式保存时的开单方展示名称';
COMMENT ON COLUMN quotation.issuer_contact IS '正式保存时的开单方展示联络资料';
COMMENT ON COLUMN quotation.original_amount IS '各行原价合计';
COMMENT ON COLUMN quotation.subtotal_amount IS '各行成交额合计，即整单优惠前金额';
COMMENT ON COLUMN quotation.discount_rate IS '整单应付比例，90 表示 9 折，与整单优惠后金额互斥';
COMMENT ON COLUMN quotation.discounted_amount IS '手填的整单优惠后金额，与整单折扣互斥';
COMMENT ON COLUMN quotation.total_amount IS '应用整单优惠后的最终报价金额';

CREATE TABLE quotation_item (
    id                bigint NOT NULL PRIMARY KEY,
    gmt_create        timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified      timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user       bigint,
    update_user       bigint,
    is_deleted        boolean DEFAULT false NOT NULL,
    quotation_id      bigint NOT NULL,
    snapshot_id       bigint NOT NULL,
    currency          varchar(4) NOT NULL,
    quantity          numeric(20,6) NOT NULL,
    unit_price        numeric(20,6) NOT NULL,
    original_amount   numeric(20,6) NOT NULL,
    discount_rate     numeric(5,2),
    discounted_amount numeric(20,6),
    line_amount       numeric(20,6) NOT NULL,
    remark            varchar(1024),
    sort_order        integer NOT NULL
);

CREATE INDEX idx_quotation_item_quotation_sort
    ON quotation_item (quotation_id, sort_order, id)
    WHERE is_deleted = false;

CREATE INDEX idx_quotation_item_snapshot
    ON quotation_item (snapshot_id);

ALTER TABLE quotation_item
    ADD CONSTRAINT ck_quotation_item_currency CHECK (
        currency IN ('CNY', 'TWD')
    );

ALTER TABLE quotation_item
    ADD CONSTRAINT ck_quotation_item_quantity CHECK (
        quantity > 0 AND quantity <> 'NaN'
    );

ALTER TABLE quotation_item
    ADD CONSTRAINT ck_quotation_item_unit_price CHECK (
        unit_price >= 0 AND unit_price <> 'NaN'
    );

ALTER TABLE quotation_item
    ADD CONSTRAINT ck_quotation_item_amounts CHECK (
        original_amount >= 0 AND original_amount <> 'NaN'
        AND line_amount BETWEEN 0 AND original_amount
    );

ALTER TABLE quotation_item
    ADD CONSTRAINT ck_quotation_item_discount CHECK (
        (discount_rate IS NULL OR discounted_amount IS NULL)
        AND (discount_rate IS NULL OR discount_rate BETWEEN 0 AND 100)
        AND (discounted_amount IS NULL OR discounted_amount BETWEEN 0 AND original_amount)
    );

ALTER TABLE quotation_item
    ADD CONSTRAINT ck_quotation_item_sort_order CHECK (
        sort_order >= 0
    );

COMMENT ON TABLE quotation_item IS '正式报价商品明细，使用商品快照和保存时的成交数据';

COMMENT ON COLUMN quotation_item.id IS '主键';
COMMENT ON COLUMN quotation_item.gmt_create IS '创建时间';
COMMENT ON COLUMN quotation_item.gmt_modified IS '更新时间';
COMMENT ON COLUMN quotation_item.create_user IS '创建者';
COMMENT ON COLUMN quotation_item.update_user IS '更新者';
COMMENT ON COLUMN quotation_item.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN quotation_item.quotation_id IS '所属报价单 ID（逻辑关联）';
COMMENT ON COLUMN quotation_item.snapshot_id IS '商品快照 ID（逻辑关联），来源 SKU 通过快照追溯';
COMMENT ON COLUMN quotation_item.currency IS '保存时的 SKU 售价币种，须与报价主表一致';
COMMENT ON COLUMN quotation_item.quantity IS '报价数量，大于 0，由应用按当时 SKU 的数量精度校验';
COMMENT ON COLUMN quotation_item.unit_price IS '正式保存时的 SKU 参考售价';
COMMENT ON COLUMN quotation_item.original_amount IS '单价乘以数量后四舍五入至两位小数的行原价';
COMMENT ON COLUMN quotation_item.discount_rate IS '行应付比例，90 表示 9 折，与行优惠后金额互斥';
COMMENT ON COLUMN quotation_item.discounted_amount IS '手填的整行优惠后金额，与行折扣互斥';
COMMENT ON COLUMN quotation_item.line_amount IS '应用行优惠后的整行成交额';
COMMENT ON COLUMN quotation_item.remark IS '客户版可见的商品行备注';
COMMENT ON COLUMN quotation_item.sort_order IS '商品行展示顺序，同值时按主键排序';

CREATE TABLE quotation_template (
    id            bigint NOT NULL PRIMARY KEY,
    gmt_create    timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified  timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user   bigint,
    update_user   bigint,
    is_deleted    boolean DEFAULT false NOT NULL,
    name          varchar(128) NOT NULL,
    title         varchar(256) NOT NULL,
    scope         varchar(8) NOT NULL,
    owner_user_id bigint NOT NULL,
    version       bigint DEFAULT 0 NOT NULL,
    discount_rate numeric(5,2),
    remark        varchar(1024)
);

CREATE INDEX idx_quotation_template_scope_modified
    ON quotation_template (scope, gmt_modified DESC, id DESC)
    WHERE is_deleted = false;

CREATE INDEX idx_quotation_template_scope_owner_modified
    ON quotation_template (scope, owner_user_id, gmt_modified DESC, id DESC)
    WHERE is_deleted = false;

ALTER TABLE quotation_template
    ADD CONSTRAINT ck_quotation_template_scope CHECK (
        scope IN ('PERSONAL', 'PUBLIC')
    );

ALTER TABLE quotation_template
    ADD CONSTRAINT ck_quotation_template_version CHECK (
        version >= 0
    );

ALTER TABLE quotation_template
    ADD CONSTRAINT ck_quotation_template_discount_rate CHECK (
        discount_rate BETWEEN 0 AND 100
    );

COMMENT ON TABLE quotation_template IS '个人或公共报价模板，保存 SKU 组合及整单折扣';

COMMENT ON COLUMN quotation_template.id IS '主键';
COMMENT ON COLUMN quotation_template.gmt_create IS '创建时间';
COMMENT ON COLUMN quotation_template.gmt_modified IS '更新时间';
COMMENT ON COLUMN quotation_template.create_user IS '创建者';
COMMENT ON COLUMN quotation_template.update_user IS '更新者';
COMMENT ON COLUMN quotation_template.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN quotation_template.name IS '模板名称';
COMMENT ON COLUMN quotation_template.title IS '套用模板时填入报价表单的标题';
COMMENT ON COLUMN quotation_template.scope IS '模板范围：PERSONAL=个人，PUBLIC=公共';
COMMENT ON COLUMN quotation_template.owner_user_id IS '个人模板所有者或公共模板创建者的系统用户 ID';
COMMENT ON COLUMN quotation_template.version IS '模板版本，由应用在修改主表及明细时校验并递增';
COMMENT ON COLUMN quotation_template.discount_rate IS '可选的整单应付比例，90 表示 9 折';
COMMENT ON COLUMN quotation_template.remark IS '套用模板时填入报价表单的整单备注';

CREATE TABLE quotation_template_item (
    id            bigint NOT NULL PRIMARY KEY,
    gmt_create    timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified  timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user   bigint,
    update_user   bigint,
    is_deleted    boolean DEFAULT false NOT NULL,
    template_id   bigint NOT NULL,
    sku_id        bigint NOT NULL,
    sort_order    integer NOT NULL,
    discount_rate numeric(5,2),
    remark        varchar(1024)
);

CREATE UNIQUE INDEX uk_quotation_template_item_template_sort
    ON quotation_template_item (template_id, sort_order)
    WHERE is_deleted = false;

ALTER TABLE quotation_template_item
    ADD CONSTRAINT ck_quotation_template_item_sort_order CHECK (
        sort_order >= 0
    );

ALTER TABLE quotation_template_item
    ADD CONSTRAINT ck_quotation_template_item_discount_rate CHECK (
        discount_rate BETWEEN 0 AND 100
    );

COMMENT ON TABLE quotation_template_item IS '报价模板商品明细，同一 SKU 可在多个位置使用不同折扣';

COMMENT ON COLUMN quotation_template_item.id IS '主键';
COMMENT ON COLUMN quotation_template_item.gmt_create IS '创建时间';
COMMENT ON COLUMN quotation_template_item.gmt_modified IS '更新时间';
COMMENT ON COLUMN quotation_template_item.create_user IS '创建者';
COMMENT ON COLUMN quotation_template_item.update_user IS '更新者';
COMMENT ON COLUMN quotation_template_item.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN quotation_template_item.template_id IS '所属模板 ID（逻辑关联）';
COMMENT ON COLUMN quotation_template_item.sku_id IS '当前商品 SKU ID（逻辑关联），来源删除后仍保留用于定位和修复';
COMMENT ON COLUMN quotation_template_item.sort_order IS '商品行展示顺序，同一模板的有效明细位置唯一';
COMMENT ON COLUMN quotation_template_item.discount_rate IS '可选的行应付比例，90 表示 9 折';
COMMENT ON COLUMN quotation_template_item.remark IS '套用模板时填入对应商品行的备注';

COMMIT;
