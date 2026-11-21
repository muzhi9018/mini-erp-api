-- 商品管理第一版建表脚本，对应 docs/product-management-design.md。
-- 一次性执行：目标 schema 中以下 11 张表必须尚不存在；与 schema.sql 中的同名表二选一执行。
-- 关联使用逻辑 ID，由 Service 在事务中校验存在性、商品归属、引用限制和类目环路。
-- 主键由应用使用 IdWorker / ASSIGN_ID 生成；主档更新时间和版本递增由应用维护。
-- 商品、图片、规格、SKU 及影响展示的共享资料更新时，由应用在同一事务中递增所属商品版本。
-- 快照与规格、图片明细仅新增、查询，禁止通过通用编辑、删除方法修改；不依赖来源记录的存续。
-- 商品与 SKU 图片共用 product_image，按 image_type 和 owner_id 定位归属，快照按 PRODUCT / SKU 来源保存。
-- 主图从各自图片列表派生；数据库限制每组最多一张主图，非空组恰好一张主图由 Service 事务保证。
-- 采购价、售价分别与 CNY / TWD 币种成对填写或成对为空，不设置默认币种或自动换算。
-- 数量精度、规格值规范化及组合摘要由应用校验或生成，不能依赖 numeric(20,6) 的自动舍入。
BEGIN;

CREATE TABLE product_category (
    id           bigint NOT NULL PRIMARY KEY,
    gmt_create   timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean DEFAULT false NOT NULL,
    parent_id    bigint DEFAULT 0 NOT NULL,
    code         varchar(64) NOT NULL,
    name         varchar(128) NOT NULL,
    sort_order   integer DEFAULT 0 NOT NULL,
    remark       varchar(1000)
);

CREATE UNIQUE INDEX uk_product_category_code
    ON product_category (code);

CREATE UNIQUE INDEX uk_product_category_parent_name
    ON product_category (parent_id, name)
    WHERE is_deleted = false;

CREATE INDEX idx_product_category_parent_sort
    ON product_category (parent_id, sort_order, id)
    WHERE is_deleted = false;

ALTER TABLE product_category
    ADD CONSTRAINT ck_product_category_parent CHECK (
        parent_id >= 0 AND parent_id <> id
    );

COMMENT ON TABLE product_category IS '内部商品类目';

COMMENT ON COLUMN product_category.id IS '主键';
COMMENT ON COLUMN product_category.gmt_create IS '创建时间';
COMMENT ON COLUMN product_category.gmt_modified IS '更新时间';
COMMENT ON COLUMN product_category.create_user IS '创建者';
COMMENT ON COLUMN product_category.update_user IS '更新者';
COMMENT ON COLUMN product_category.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN product_category.parent_id IS '父类目 ID，顶级类目为 0（逻辑关联）';
COMMENT ON COLUMN product_category.code IS '类目编码，全表唯一，逻辑删除后不复用';
COMMENT ON COLUMN product_category.name IS '类目名称，未删除记录中同父级唯一';
COMMENT ON COLUMN product_category.sort_order IS '展示顺序，同值时按主键排序';
COMMENT ON COLUMN product_category.remark IS '类目备注';

CREATE TABLE product_unit (
    id             bigint NOT NULL PRIMARY KEY,
    gmt_create     timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified   timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user    bigint,
    update_user    bigint,
    is_deleted     boolean DEFAULT false NOT NULL,
    code           varchar(32) NOT NULL,
    name           varchar(32) NOT NULL,
    sort_order     integer DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX uk_product_unit_code
    ON product_unit (code);

CREATE UNIQUE INDEX uk_product_unit_name
    ON product_unit (name)
    WHERE is_deleted = false;

CREATE INDEX idx_product_unit_sort
    ON product_unit (sort_order, id)
    WHERE is_deleted = false;

COMMENT ON TABLE product_unit IS '商品计量单位，供 SKU 数量和数值规格使用，不提供单位换算';

COMMENT ON COLUMN product_unit.id IS '主键';
COMMENT ON COLUMN product_unit.gmt_create IS '创建时间';
COMMENT ON COLUMN product_unit.gmt_modified IS '更新时间';
COMMENT ON COLUMN product_unit.create_user IS '创建者';
COMMENT ON COLUMN product_unit.update_user IS '更新者';
COMMENT ON COLUMN product_unit.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN product_unit.code IS '单位编码，全表唯一，逻辑删除后不复用';
COMMENT ON COLUMN product_unit.name IS '单位名称，例如件、张、米、mm，未删除记录中唯一';
COMMENT ON COLUMN product_unit.sort_order IS '展示顺序，同值时按主键排序';

CREATE TABLE product (
    id                        bigint NOT NULL PRIMARY KEY,
    gmt_create                timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified              timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user               bigint,
    update_user               bigint,
    is_deleted                boolean DEFAULT false NOT NULL,
    category_id               bigint NOT NULL,
    code                      varchar(64) NOT NULL,
    name                      varchar(256) NOT NULL,
    brand                     varchar(128),
    description               varchar(2048),
    status                    varchar(16) NOT NULL,
    sort_order                integer DEFAULT 0 NOT NULL,
    version                   bigint DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX uk_product_code
    ON product (code);

CREATE INDEX idx_product_category_sort
    ON product (category_id, sort_order, id)
    WHERE is_deleted = false;

CREATE INDEX idx_product_sort
    ON product (sort_order, id)
    WHERE is_deleted = false;

ALTER TABLE product
    ADD CONSTRAINT ck_product_status CHECK (
        status IN ('ENABLED', 'DISABLED')
    );

ALTER TABLE product
    ADD CONSTRAINT ck_product_version CHECK (
        version >= 0
    );

COMMENT ON TABLE product IS '内部商品档案，与官网商品独立维护';

COMMENT ON COLUMN product.id IS '主键';
COMMENT ON COLUMN product.gmt_create IS '创建时间';
COMMENT ON COLUMN product.gmt_modified IS '更新时间';
COMMENT ON COLUMN product.create_user IS '创建者';
COMMENT ON COLUMN product.update_user IS '更新者';
COMMENT ON COLUMN product.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN product.category_id IS '所属商品类目 ID（逻辑关联），允许任意层级的有效类目';
COMMENT ON COLUMN product.code IS '商品编码，全表唯一，逻辑删除后不复用';
COMMENT ON COLUMN product.name IS '商品名称';
COMMENT ON COLUMN product.brand IS '品牌名称';
COMMENT ON COLUMN product.description IS '商品简要说明，纯文本';
COMMENT ON COLUMN product.status IS '商品状态：ENABLED=启用，DISABLED=停用';
COMMENT ON COLUMN product.sort_order IS '展示顺序，同值时按主键排序';
COMMENT ON COLUMN product.version IS '商品统一版本，商品、图片、规格、SKU 及影响展示的共享资料更新时由应用在同一事务中递增，一次操作每款商品只加 1';

CREATE TABLE product_image (
    id            bigint NOT NULL PRIMARY KEY,
    gmt_create    timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified  timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user   bigint,
    update_user   bigint,
    is_deleted    boolean DEFAULT false NOT NULL,
    image_type    varchar(16) NOT NULL,
    owner_id      bigint NOT NULL,
    attachment_id bigint NOT NULL,
    is_cover      boolean DEFAULT false NOT NULL,
    sort_order    integer DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX uk_product_image_owner_attachment
    ON product_image (image_type, owner_id, attachment_id)
    WHERE is_deleted = false;

CREATE UNIQUE INDEX uk_product_image_owner_cover
    ON product_image (image_type, owner_id)
    WHERE is_deleted = false AND is_cover = true;

CREATE INDEX idx_product_image_owner_sort
    ON product_image (image_type, owner_id, sort_order, id)
    WHERE is_deleted = false;

CREATE INDEX idx_product_image_attachment
    ON product_image (attachment_id)
    WHERE is_deleted = false;

ALTER TABLE product_image
    ADD CONSTRAINT ck_product_image_type CHECK (
        image_type IN ('PRODUCT', 'SKU')
    );

COMMENT ON TABLE product_image IS '商品与 SKU 共用的图片关联表，按归属分别维护图片和主图';

COMMENT ON COLUMN product_image.id IS '主键';
COMMENT ON COLUMN product_image.gmt_create IS '创建时间';
COMMENT ON COLUMN product_image.gmt_modified IS '更新时间';
COMMENT ON COLUMN product_image.create_user IS '创建者';
COMMENT ON COLUMN product_image.update_user IS '更新者';
COMMENT ON COLUMN product_image.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN product_image.image_type IS '图片类型：PRODUCT=商品图片，SKU=该 SKU 自身图片';
COMMENT ON COLUMN product_image.owner_id IS '图片归属 ID（逻辑关联），PRODUCT 存商品 ID，SKU 存 SKU ID';
COMMENT ON COLUMN product_image.attachment_id IS '图片附件 ID（逻辑关联），同一归属内未删除记录唯一，移除关联时保留附件对象';
COMMENT ON COLUMN product_image.is_cover IS '是否该商品或 SKU 的主图，每个归属最多一张有效主图，切换时先解除旧主图标记';
COMMENT ON COLUMN product_image.sort_order IS '图片展示顺序，按目标列表顺序保存，同值时按主键排序';

CREATE TABLE product_spec (
    id           bigint NOT NULL PRIMARY KEY,
    gmt_create   timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean DEFAULT false NOT NULL,
    product_id   bigint NOT NULL,
    name         varchar(64) NOT NULL,
    value_type   varchar(16) NOT NULL,
    unit_id      bigint,
    sort_order   integer DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX uk_product_spec_product_name
    ON product_spec (product_id, name)
    WHERE is_deleted = false;

CREATE INDEX idx_product_spec_product_sort
    ON product_spec (product_id, sort_order, id)
    WHERE is_deleted = false;

CREATE INDEX idx_product_spec_unit
    ON product_spec (unit_id)
    WHERE is_deleted = false AND unit_id IS NOT NULL;

ALTER TABLE product_spec
    ADD CONSTRAINT ck_product_spec_value_type CHECK (
        value_type IN ('TEXT', 'NUMBER')
    );

ALTER TABLE product_spec
    ADD CONSTRAINT ck_product_spec_unit CHECK (
        value_type = 'NUMBER' OR unit_id IS NULL
    );

COMMENT ON TABLE product_spec IS '商品规格维度，按商品定义';

COMMENT ON COLUMN product_spec.id IS '主键';
COMMENT ON COLUMN product_spec.gmt_create IS '创建时间';
COMMENT ON COLUMN product_spec.gmt_modified IS '更新时间';
COMMENT ON COLUMN product_spec.create_user IS '创建者';
COMMENT ON COLUMN product_spec.update_user IS '更新者';
COMMENT ON COLUMN product_spec.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN product_spec.product_id IS '所属商品 ID（逻辑关联）';
COMMENT ON COLUMN product_spec.name IS '规格维度名称，未删除记录中同一商品内唯一';
COMMENT ON COLUMN product_spec.value_type IS '规格值类型：TEXT=文本，NUMBER=数值';
COMMENT ON COLUMN product_spec.unit_id IS '数值规格计量单位 ID（逻辑关联），可为空，文本规格必须为空';
COMMENT ON COLUMN product_spec.sort_order IS '规格显示顺序，不参与 SKU 组合标识计算';

CREATE TABLE product_spec_value (
    id               bigint NOT NULL PRIMARY KEY,
    gmt_create       timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified     timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user      bigint,
    update_user      bigint,
    is_deleted       boolean DEFAULT false NOT NULL,
    spec_id          bigint NOT NULL,
    text_value       varchar(128),
    number_value     numeric(20,6),
    normalized_value varchar(128) NOT NULL,
    sort_order       integer DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX uk_product_spec_value_spec_normalized
    ON product_spec_value (spec_id, normalized_value)
    WHERE is_deleted = false;

CREATE INDEX idx_product_spec_value_spec_sort
    ON product_spec_value (spec_id, sort_order, id)
    WHERE is_deleted = false;

CREATE INDEX idx_product_spec_value_spec_number
    ON product_spec_value (spec_id, number_value, id)
    WHERE is_deleted = false AND number_value IS NOT NULL;

ALTER TABLE product_spec_value
    ADD CONSTRAINT ck_product_spec_value_value CHECK (
        (text_value IS NOT NULL AND number_value IS NULL)
        OR (text_value IS NULL AND number_value IS NOT NULL)
    );

COMMENT ON TABLE product_spec_value IS '商品规格候选值，文本或数值与所属维度类型的一致性由应用校验';

COMMENT ON COLUMN product_spec_value.id IS '主键';
COMMENT ON COLUMN product_spec_value.gmt_create IS '创建时间';
COMMENT ON COLUMN product_spec_value.gmt_modified IS '更新时间';
COMMENT ON COLUMN product_spec_value.create_user IS '创建者';
COMMENT ON COLUMN product_spec_value.update_user IS '更新者';
COMMENT ON COLUMN product_spec_value.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN product_spec_value.spec_id IS '所属规格维度 ID（逻辑关联）';
COMMENT ON COLUMN product_spec_value.text_value IS '文本候选值，去除首尾空白，与数值候选值必须且只能填写一个';
COMMENT ON COLUMN product_spec_value.number_value IS '数值候选值，使用所属维度的单位，与文本候选值必须且只能填写一个';
COMMENT ON COLUMN product_spec_value.normalized_value IS '服务端生成的去重值，文本去除首尾空白，数值按等价规则去除无效尾零';
COMMENT ON COLUMN product_spec_value.sort_order IS '候选值展示顺序，同值时按主键排序';

CREATE TABLE product_sku (
    id                       bigint NOT NULL PRIMARY KEY,
    gmt_create               timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified             timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user              bigint,
    update_user              bigint,
    is_deleted               boolean DEFAULT false NOT NULL,
    product_id               bigint NOT NULL,
    code                     varchar(64) NOT NULL,
    model                    varchar(128),
    unit_id                  bigint NOT NULL,
    quantity_scale           integer DEFAULT 0 NOT NULL,
    reference_purchase_price numeric(20,6),
    reference_purchase_currency varchar(3),
    reference_sale_price     numeric(20,6),
    reference_sale_currency   varchar(3),
    stock_quantity           numeric(20,6) DEFAULT 0 NOT NULL,
    specification_key        varchar(64) NOT NULL,
    status                   varchar(16) NOT NULL,
    sort_order               integer DEFAULT 0 NOT NULL,
    version                  bigint DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX uk_product_sku_code
    ON product_sku (code);

CREATE UNIQUE INDEX uk_product_sku_product_specification
    ON product_sku (product_id, specification_key)
    WHERE is_deleted = false;

CREATE INDEX idx_product_sku_product_sort
    ON product_sku (product_id, sort_order, id)
    WHERE is_deleted = false;

CREATE INDEX idx_product_sku_sort
    ON product_sku (sort_order, id)
    WHERE is_deleted = false;

CREATE INDEX idx_product_sku_unit
    ON product_sku (unit_id)
    WHERE is_deleted = false;

ALTER TABLE product_sku
    ADD CONSTRAINT ck_product_sku_quantity_scale CHECK (
        quantity_scale BETWEEN 0 AND 6
    );

ALTER TABLE product_sku
    ADD CONSTRAINT ck_product_sku_reference_purchase_price CHECK (
        reference_purchase_price >= 0
    );

ALTER TABLE product_sku
    ADD CONSTRAINT ck_product_sku_reference_sale_price CHECK (
        reference_sale_price >= 0
    );

ALTER TABLE product_sku
    ADD CONSTRAINT ck_product_sku_reference_purchase_currency CHECK (
        (reference_purchase_price IS NULL AND reference_purchase_currency IS NULL)
        OR (reference_purchase_price IS NOT NULL AND reference_purchase_currency IS NOT NULL
            AND reference_purchase_currency IN ('CNY', 'TWD'))
    );

ALTER TABLE product_sku
    ADD CONSTRAINT ck_product_sku_reference_sale_currency CHECK (
        (reference_sale_price IS NULL AND reference_sale_currency IS NULL)
        OR (reference_sale_price IS NOT NULL AND reference_sale_currency IS NOT NULL
            AND reference_sale_currency IN ('CNY', 'TWD'))
    );

ALTER TABLE product_sku
    ADD CONSTRAINT ck_product_sku_stock_quantity CHECK (
        stock_quantity >= 0
    );

ALTER TABLE product_sku
    ADD CONSTRAINT ck_product_sku_specification_key CHECK (
        specification_key ~ '^[0-9a-f]{64}$'
    );

ALTER TABLE product_sku
    ADD CONSTRAINT ck_product_sku_status CHECK (
        status IN ('ENABLED', 'DISABLED')
    );

ALTER TABLE product_sku
    ADD CONSTRAINT ck_product_sku_version CHECK (
        version >= 0
    );

COMMENT ON TABLE product_sku IS '内部商品 SKU，库存为人工维护的当前数量，不自动增减或预占';

COMMENT ON COLUMN product_sku.id IS '主键';
COMMENT ON COLUMN product_sku.gmt_create IS '创建时间';
COMMENT ON COLUMN product_sku.gmt_modified IS '更新时间';
COMMENT ON COLUMN product_sku.create_user IS '创建者';
COMMENT ON COLUMN product_sku.update_user IS '更新者';
COMMENT ON COLUMN product_sku.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN product_sku.product_id IS '所属商品 ID（逻辑关联），创建后不直接修改';
COMMENT ON COLUMN product_sku.code IS 'SKU 货号，全表唯一，逻辑删除后不复用';
COMMENT ON COLUMN product_sku.model IS '厂家型号等型号信息';
COMMENT ON COLUMN product_sku.unit_id IS '基本计量单位 ID（逻辑关联），库存及后续单据数量使用此单位';
COMMENT ON COLUMN product_sku.quantity_scale IS '该 SKU 允许的数量小数位数，范围 0～6，0 表示只允许整数';
COMMENT ON COLUMN product_sku.reference_purchase_price IS '参考采购价，非负，与采购币种成对填写或成对为空，0 表示零价';
COMMENT ON COLUMN product_sku.reference_purchase_currency IS '参考采购价币种，仅 CNY=人民币、TWD=新台币，无默认值，可与售价币种不同';
COMMENT ON COLUMN product_sku.reference_sale_price IS '参考售价，非负，与售价币种成对填写或成对为空，0 表示零价';
COMMENT ON COLUMN product_sku.reference_sale_currency IS '参考售价币种，仅 CNY=人民币、TWD=新台币，无默认值，可与采购币种不同';
COMMENT ON COLUMN product_sku.stock_quantity IS '当前人工库存数量，非负，由应用按 SKU 的 quantity_scale 校验，通过独立操作设置绝对值';
COMMENT ON COLUMN product_sku.specification_key IS '服务端对按规格 ID 排序的规格 ID 与候选值 ID 列表规范化后计算的 SHA-256 小写十六进制摘要，无规格使用空列表';
COMMENT ON COLUMN product_sku.status IS 'SKU 状态：ENABLED=启用，DISABLED=停用，停用仍占用规格组合';
COMMENT ON COLUMN product_sku.sort_order IS '展示顺序，同值时按主键排序';
COMMENT ON COLUMN product_sku.version IS 'SKU 资料、状态及库存操作共用的行版本，由应用校验并递增，同时递增所属商品统一版本，不作为快照唯一键';

CREATE TABLE product_sku_spec_value (
    id            bigint NOT NULL PRIMARY KEY,
    gmt_create    timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified  timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user   bigint,
    update_user   bigint,
    is_deleted    boolean DEFAULT false NOT NULL,
    sku_id        bigint NOT NULL,
    spec_id       bigint NOT NULL,
    spec_value_id bigint NOT NULL
);

CREATE UNIQUE INDEX uk_product_sku_spec_value_sku_spec
    ON product_sku_spec_value (sku_id, spec_id)
    WHERE is_deleted = false;

CREATE INDEX idx_product_sku_spec_value_value_sku
    ON product_sku_spec_value (spec_value_id, sku_id)
    WHERE is_deleted = false;

CREATE INDEX idx_product_sku_spec_value_spec
    ON product_sku_spec_value (spec_id)
    WHERE is_deleted = false;

COMMENT ON TABLE product_sku_spec_value IS 'SKU 规格选择关系，商品归属和维度选择完整性由应用事务保证';

COMMENT ON COLUMN product_sku_spec_value.id IS '主键';
COMMENT ON COLUMN product_sku_spec_value.gmt_create IS '创建时间';
COMMENT ON COLUMN product_sku_spec_value.gmt_modified IS '更新时间';
COMMENT ON COLUMN product_sku_spec_value.create_user IS '创建者';
COMMENT ON COLUMN product_sku_spec_value.update_user IS '更新者';
COMMENT ON COLUMN product_sku_spec_value.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN product_sku_spec_value.sku_id IS 'SKU ID（逻辑关联）';
COMMENT ON COLUMN product_sku_spec_value.spec_id IS '规格维度 ID（逻辑关联），未删除记录中同一 SKU 的每个维度只选一个值';
COMMENT ON COLUMN product_sku_spec_value.spec_value_id IS '规格候选值 ID（逻辑关联），必须属于所选维度且与 SKU 属于同一商品';

CREATE TABLE product_snapshot (
    id                        bigint NOT NULL PRIMARY KEY,
    gmt_create                timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified              timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user               bigint,
    update_user               bigint,
    is_deleted                boolean DEFAULT false NOT NULL,
    source_product_id         bigint NOT NULL,
    source_sku_id             bigint NOT NULL,
    version                   bigint NOT NULL,
    source_sku_version        bigint NOT NULL,
    product_code              varchar(64) NOT NULL,
    sku_code                  varchar(64) NOT NULL,
    product_name              varchar(256) NOT NULL,
    brand                     varchar(128),
    model                     varchar(128),
    description               varchar(2048),
    category_code             varchar(64) NOT NULL,
    category_name             varchar(128) NOT NULL,
    category_path             text NOT NULL,
    unit_code                 varchar(32) NOT NULL,
    unit_name                 varchar(32) NOT NULL,
    quantity_scale            integer NOT NULL,
    specification_text        text NOT NULL
);

CREATE UNIQUE INDEX uk_product_snapshot_sku_version
    ON product_snapshot (source_sku_id, version);

CREATE INDEX idx_product_snapshot_product_version
    ON product_snapshot (source_product_id, version, id);

ALTER TABLE product_snapshot
    ADD CONSTRAINT ck_product_snapshot_version CHECK (
        version >= 0
    );

ALTER TABLE product_snapshot
    ADD CONSTRAINT ck_product_snapshot_source_sku_version CHECK (
        source_sku_version >= 0
    );

ALTER TABLE product_snapshot
    ADD CONSTRAINT ck_product_snapshot_quantity_scale CHECK (
        quantity_scale BETWEEN 0 AND 6
    );

ALTER TABLE product_snapshot
    ADD CONSTRAINT ck_product_snapshot_not_deleted CHECK (
        is_deleted = false
    );

COMMENT ON TABLE product_snapshot IS 'SKU 商品版本快照，同一 SKU 与商品版本全表唯一，仅新增和查询，不随来源主档删除';

COMMENT ON COLUMN product_snapshot.id IS '主键';
COMMENT ON COLUMN product_snapshot.gmt_create IS '创建时间';
COMMENT ON COLUMN product_snapshot.gmt_modified IS '更新时间';
COMMENT ON COLUMN product_snapshot.create_user IS '创建者';
COMMENT ON COLUMN product_snapshot.update_user IS '更新者';
COMMENT ON COLUMN product_snapshot.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN product_snapshot.source_product_id IS '来源商品 ID，仅用于追溯，不依赖来源记录存在';
COMMENT ON COLUMN product_snapshot.source_sku_id IS '来源 SKU ID，仅用于追溯，与商品版本组成快照唯一键';
COMMENT ON COLUMN product_snapshot.version IS '生成时的商品统一版本，由服务端显式写入，创建后不变，不作为快照行版本递增';
COMMENT ON COLUMN product_snapshot.source_sku_version IS '生成时的来源 SKU 行版本，仅辅助追溯，不参与快照唯一键';
COMMENT ON COLUMN product_snapshot.product_code IS '快照生成时的商品编码';
COMMENT ON COLUMN product_snapshot.sku_code IS '快照生成时的 SKU 编码';
COMMENT ON COLUMN product_snapshot.product_name IS '快照生成时的商品名称';
COMMENT ON COLUMN product_snapshot.brand IS '快照生成时的品牌名称';
COMMENT ON COLUMN product_snapshot.model IS '快照生成时的 SKU 型号';
COMMENT ON COLUMN product_snapshot.description IS '快照生成时的商品简要说明，纯文本';
COMMENT ON COLUMN product_snapshot.category_code IS '快照生成时的类目编码';
COMMENT ON COLUMN product_snapshot.category_name IS '快照生成时的类目名称';
COMMENT ON COLUMN product_snapshot.category_path IS '快照生成时的完整类目名称路径';
COMMENT ON COLUMN product_snapshot.unit_code IS '快照生成时的 SKU 基本计量单位编码';
COMMENT ON COLUMN product_snapshot.unit_name IS '快照生成时的 SKU 基本计量单位名称';
COMMENT ON COLUMN product_snapshot.quantity_scale IS '快照生成时从来源 SKU 复制的数量小数位数，范围 0～6';
COMMENT ON COLUMN product_snapshot.specification_text IS '快照生成时按显示顺序生成的规格摘要，无规格时为空字符串';

CREATE TABLE product_snapshot_spec (
    id                   bigint NOT NULL PRIMARY KEY,
    gmt_create           timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified         timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user          bigint,
    update_user          bigint,
    is_deleted           boolean DEFAULT false NOT NULL,
    snapshot_id          bigint NOT NULL,
    source_spec_id       bigint NOT NULL,
    source_spec_value_id bigint NOT NULL,
    spec_name            varchar(64) NOT NULL,
    value_type           varchar(16) NOT NULL,
    text_value           varchar(128),
    number_value         numeric(20,6),
    unit_code            varchar(32),
    unit_name            varchar(32),
    sort_order           integer NOT NULL
);

CREATE UNIQUE INDEX uk_product_snapshot_spec_snapshot_spec
    ON product_snapshot_spec (snapshot_id, source_spec_id);

CREATE INDEX idx_product_snapshot_spec_snapshot_sort
    ON product_snapshot_spec (snapshot_id, sort_order, id);

ALTER TABLE product_snapshot_spec
    ADD CONSTRAINT ck_product_snapshot_spec_value CHECK (
        (value_type = 'TEXT' AND text_value IS NOT NULL AND number_value IS NULL)
        OR (value_type = 'NUMBER' AND text_value IS NULL AND number_value IS NOT NULL)
    );

ALTER TABLE product_snapshot_spec
    ADD CONSTRAINT ck_product_snapshot_spec_text_unit CHECK (
        value_type = 'NUMBER' OR unit_code IS NULL
    );

ALTER TABLE product_snapshot_spec
    ADD CONSTRAINT ck_product_snapshot_spec_unit_pair CHECK (
        (unit_code IS NULL AND unit_name IS NULL)
        OR (unit_code IS NOT NULL AND unit_name IS NOT NULL)
    );

ALTER TABLE product_snapshot_spec
    ADD CONSTRAINT ck_product_snapshot_spec_not_deleted CHECK (
        is_deleted = false
    );

COMMENT ON TABLE product_snapshot_spec IS '商品快照规格明细，完整复制当时的名称、选中值和单位，仅新增和查询，不动态读取来源主档';

COMMENT ON COLUMN product_snapshot_spec.id IS '主键';
COMMENT ON COLUMN product_snapshot_spec.gmt_create IS '创建时间';
COMMENT ON COLUMN product_snapshot_spec.gmt_modified IS '更新时间';
COMMENT ON COLUMN product_snapshot_spec.create_user IS '创建者';
COMMENT ON COLUMN product_snapshot_spec.update_user IS '更新者';
COMMENT ON COLUMN product_snapshot_spec.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN product_snapshot_spec.snapshot_id IS '所属商品快照 ID（逻辑关联），与快照主记录在同一事务中完整新增';
COMMENT ON COLUMN product_snapshot_spec.source_spec_id IS '来源规格维度 ID，仅用于追溯，同一快照内唯一';
COMMENT ON COLUMN product_snapshot_spec.source_spec_value_id IS '来源规格候选值 ID，仅用于追溯，不依赖来源记录存在';
COMMENT ON COLUMN product_snapshot_spec.spec_name IS '快照生成时的规格维度名称';
COMMENT ON COLUMN product_snapshot_spec.value_type IS '快照规格值类型：TEXT=文本，NUMBER=数值';
COMMENT ON COLUMN product_snapshot_spec.text_value IS '快照文本值，仅文本类型填写，与数值必须且只能填写一个';
COMMENT ON COLUMN product_snapshot_spec.number_value IS '快照数值，仅数值类型填写，使用快照保存的规格单位';
COMMENT ON COLUMN product_snapshot_spec.unit_code IS '快照数值规格的单位编码，文本类型必须为空，与单位名称成对填写';
COMMENT ON COLUMN product_snapshot_spec.unit_name IS '快照数值规格的单位名称，文本类型必须为空，与单位编码成对填写';
COMMENT ON COLUMN product_snapshot_spec.sort_order IS '快照生成时的显示顺序，同值时按主键排序';

CREATE TABLE product_snapshot_image (
    id              bigint NOT NULL PRIMARY KEY,
    gmt_create      timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified    timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user     bigint,
    update_user     bigint,
    is_deleted      boolean DEFAULT false NOT NULL,
    snapshot_id     bigint NOT NULL,
    image_source    varchar(16) NOT NULL,
    source_image_id bigint NOT NULL,
    attachment_id   bigint NOT NULL,
    is_cover        boolean NOT NULL,
    sort_order      integer NOT NULL
);

CREATE UNIQUE INDEX uk_product_snapshot_image_snapshot_attachment
    ON product_snapshot_image (snapshot_id, image_source, attachment_id);

CREATE UNIQUE INDEX uk_product_snapshot_image_snapshot_cover
    ON product_snapshot_image (snapshot_id, image_source)
    WHERE is_cover = true;

CREATE INDEX idx_product_snapshot_image_snapshot_sort
    ON product_snapshot_image (snapshot_id, image_source, sort_order, id);

CREATE INDEX idx_product_snapshot_image_attachment
    ON product_snapshot_image (attachment_id);

ALTER TABLE product_snapshot_image
    ADD CONSTRAINT ck_product_snapshot_image_source CHECK (
        image_source IN ('PRODUCT', 'SKU')
    );

ALTER TABLE product_snapshot_image
    ADD CONSTRAINT ck_product_snapshot_image_not_deleted CHECK (
        is_deleted = false
    );

COMMENT ON TABLE product_snapshot_image IS '商品快照图片明细，按 PRODUCT / SKU 分别保留当时图片、主图和顺序，仅新增和查询，不随来源图片删除';

COMMENT ON COLUMN product_snapshot_image.id IS '主键';
COMMENT ON COLUMN product_snapshot_image.gmt_create IS '创建时间';
COMMENT ON COLUMN product_snapshot_image.gmt_modified IS '更新时间';
COMMENT ON COLUMN product_snapshot_image.create_user IS '创建者';
COMMENT ON COLUMN product_snapshot_image.update_user IS '更新者';
COMMENT ON COLUMN product_snapshot_image.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN product_snapshot_image.snapshot_id IS '所属商品快照 ID（逻辑关联），与快照主表和规格明细在同一事务中完整新增';
COMMENT ON COLUMN product_snapshot_image.image_source IS '图片来源：PRODUCT=商品图片，SKU=该 SKU 自身图片，禁止将商品图回填为 SKU 图';
COMMENT ON COLUMN product_snapshot_image.source_image_id IS '统一图片表 product_image 的关联 ID，仅用于追溯，不依赖来源记录存在';
COMMENT ON COLUMN product_snapshot_image.attachment_id IS '快照生成时的图片附件 ID，同一快照及来源内唯一，附件记录及实际对象必须保留';
COMMENT ON COLUMN product_snapshot_image.is_cover IS '快照生成时是否主图，同一快照每个来源最多一张主图，非空组恰好一张由应用保证';
COMMENT ON COLUMN product_snapshot_image.sort_order IS '快照生成时按每个来源组的图片顺序连续赋值的展示顺序，同值时按主键排序';

COMMIT;
