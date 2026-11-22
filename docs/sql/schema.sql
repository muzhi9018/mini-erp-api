CREATE TABLE sys_user (
    id                bigint NOT NULL PRIMARY KEY,
    gmt_create        timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified      timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user       bigint,
    update_user       bigint,
    is_deleted        boolean DEFAULT false NOT NULL,
    status            smallint DEFAULT 1 NOT NULL,
    username          varchar(32) NOT NULL,
    password          varchar(256) NOT NULL,
    user_no           varchar(32),
    current_role_code varchar(32),
    real_name         varchar(64),
    mobile            varchar(32),
    remark            varchar(128),
    sort_order        integer DEFAULT 0
);

CREATE UNIQUE INDEX uk_su_username
    ON sys_user (username)
    WHERE is_deleted = false;

ALTER TABLE sys_user
    ADD CONSTRAINT ck_sys_user_status CHECK (
        status IN (0, 1)
    );

COMMENT ON TABLE sys_user IS '系统用户信息';

COMMENT ON COLUMN sys_user.id IS '主键';
COMMENT ON COLUMN sys_user.gmt_create IS '创建时间';
COMMENT ON COLUMN sys_user.gmt_modified IS '更新时间';
COMMENT ON COLUMN sys_user.create_user IS '创建者';
COMMENT ON COLUMN sys_user.update_user IS '更新者';
COMMENT ON COLUMN sys_user.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN sys_user.status IS '状态(1:正常; 0:禁用)';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码';
COMMENT ON COLUMN sys_user.user_no IS '用户编号';
COMMENT ON COLUMN sys_user.current_role_code IS '当前使用角色';
COMMENT ON COLUMN sys_user.real_name IS '用户昵称';
COMMENT ON COLUMN sys_user.mobile IS '手机号';
COMMENT ON COLUMN sys_user.remark IS '备注';
COMMENT ON COLUMN sys_user.sort_order IS '排序';

CREATE TABLE sys_role_permission (
    id              bigint NOT NULL PRIMARY KEY,
    gmt_create      timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    gmt_modified    timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    create_user     bigint,
    update_user     bigint,
    role_id         bigint NOT NULL,
    menu_id         bigint NOT NULL,
    role_code       varchar(32) NOT NULL,
    permission_code varchar(64) NOT NULL
);

CREATE UNIQUE INDEX uk_sys_role_permission_role_menu
    ON sys_role_permission (role_id, menu_id);

CREATE INDEX idx_sys_role_permission_role_code
    ON sys_role_permission (role_code);

CREATE INDEX idx_sys_role_permission_permission_code
    ON sys_role_permission (permission_code);

COMMENT ON TABLE sys_role_permission IS '角色权限关联表';

COMMENT ON COLUMN sys_role_permission.id IS '主键id';
COMMENT ON COLUMN sys_role_permission.gmt_create IS '创建时间';
COMMENT ON COLUMN sys_role_permission.gmt_modified IS '更新时间';
COMMENT ON COLUMN sys_role_permission.create_user IS '创建者';
COMMENT ON COLUMN sys_role_permission.update_user IS '更新者';
COMMENT ON COLUMN sys_role_permission.role_id IS '角色id';
COMMENT ON COLUMN sys_role_permission.menu_id IS '菜单id';
COMMENT ON COLUMN sys_role_permission.role_code IS '角色编码';
COMMENT ON COLUMN sys_role_permission.permission_code IS '权限编码';

CREATE TABLE sys_menu (
    id              bigint NOT NULL PRIMARY KEY,
    parent_id       bigint DEFAULT 0 NOT NULL,
    hierarchy       varchar(1024),
    client_id       varchar(32),
    type            smallint NOT NULL,
    name            varchar(32) NOT NULL,
    i18n_code       varchar(128),
    path            varchar(128),
    component       varchar(128),
    icon            varchar(128),
    permission_code varchar(64),
    sort_order      integer DEFAULT 0 NOT NULL,
    gmt_create      timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    gmt_modified    timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    create_user     bigint,
    update_user     bigint,
    is_deleted      boolean DEFAULT false NOT NULL
);

CREATE INDEX idx_sys_menu_parent_id
    ON sys_menu (parent_id);

ALTER TABLE sys_menu
    ADD CONSTRAINT ck_sys_menu_type CHECK (
        type IN (1, 2, 3, 4)
    );

COMMENT ON TABLE sys_menu IS '系统菜单';

COMMENT ON COLUMN sys_menu.id IS 'id';
COMMENT ON COLUMN sys_menu.parent_id IS '父级id（顶级菜单为 0）';
COMMENT ON COLUMN sys_menu.hierarchy IS '层级（记录级联的id层级）';
COMMENT ON COLUMN sys_menu.client_id IS '所属客户端';
COMMENT ON COLUMN sys_menu.type IS '1->目录; 2->菜单; 3->按钮; 4->接口';
COMMENT ON COLUMN sys_menu.name IS '菜单名称';
COMMENT ON COLUMN sys_menu.i18n_code IS '国际化编码';
COMMENT ON COLUMN sys_menu.path IS '路径';
COMMENT ON COLUMN sys_menu.component IS '组件';
COMMENT ON COLUMN sys_menu.icon IS '图标';
COMMENT ON COLUMN sys_menu.permission_code IS '权限码';
COMMENT ON COLUMN sys_menu.sort_order IS '排序';
COMMENT ON COLUMN sys_menu.gmt_create IS '创建时间';
COMMENT ON COLUMN sys_menu.gmt_modified IS '更新时间';
COMMENT ON COLUMN sys_menu.create_user IS '创建者';
COMMENT ON COLUMN sys_menu.update_user IS '更新者';
COMMENT ON COLUMN sys_menu.is_deleted IS '是否删除';

CREATE TABLE sys_role (
    id           bigint NOT NULL PRIMARY KEY,
    gmt_create   timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    gmt_modified timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean DEFAULT false NOT NULL,
    status       smallint DEFAULT 1 NOT NULL,
    sort_order   integer DEFAULT 0 NOT NULL,
    role_name    varchar(32) NOT NULL,
    role_code    varchar(32) NOT NULL,
    remark       varchar(128)
);

CREATE UNIQUE INDEX uk_sys_role_role_code_active
    ON sys_role (role_code)
    WHERE is_deleted = false;

ALTER TABLE sys_role
    ADD CONSTRAINT ck_sys_role_status CHECK (
        status IN (0, 1)
    );

COMMENT ON TABLE sys_role IS '系统角色信息';

COMMENT ON COLUMN sys_role.id IS 'id';
COMMENT ON COLUMN sys_role.gmt_create IS '创建时间';
COMMENT ON COLUMN sys_role.gmt_modified IS '更新时间';
COMMENT ON COLUMN sys_role.create_user IS '创建者';
COMMENT ON COLUMN sys_role.update_user IS '更新者';
COMMENT ON COLUMN sys_role.is_deleted IS '是否删除';
COMMENT ON COLUMN sys_role.status IS '状态（1:正常; 0:禁用）';
COMMENT ON COLUMN sys_role.sort_order IS '排序';
COMMENT ON COLUMN sys_role.role_name IS '角色名称';
COMMENT ON COLUMN sys_role.role_code IS '角色编码';
COMMENT ON COLUMN sys_role.remark IS '备注';

CREATE TABLE sys_user_role (
    id           bigint NOT NULL PRIMARY KEY,
    gmt_create   timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    gmt_modified timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean DEFAULT false NOT NULL,
    status       smallint DEFAULT 1 NOT NULL,
    sort_order   integer DEFAULT 0 NOT NULL,
    user_id      bigint NOT NULL,
    role_id      bigint NOT NULL
);

CREATE UNIQUE INDEX uk_sys_user_role_active
    ON sys_user_role (user_id, role_id)
    WHERE is_deleted = false;

CREATE INDEX idx_sys_user_role_user_id
    ON sys_user_role (user_id);

CREATE INDEX idx_sys_user_role_role_id
    ON sys_user_role (role_id);

ALTER TABLE sys_user_role
    ADD CONSTRAINT ck_sys_user_role_status CHECK (
        status IN (0, 1)
    );

COMMENT ON TABLE sys_user_role IS '用户角色关联表';

COMMENT ON COLUMN sys_user_role.id IS 'id';
COMMENT ON COLUMN sys_user_role.gmt_create IS '创建时间';
COMMENT ON COLUMN sys_user_role.gmt_modified IS '更新时间';
COMMENT ON COLUMN sys_user_role.create_user IS '创建者';
COMMENT ON COLUMN sys_user_role.update_user IS '更新者';
COMMENT ON COLUMN sys_user_role.is_deleted IS '是否删除(0:否; 1:是)';
COMMENT ON COLUMN sys_user_role.status IS '状态（1:正常; 0:禁用）';
COMMENT ON COLUMN sys_user_role.sort_order IS '排序';
COMMENT ON COLUMN sys_user_role.user_id IS '用户id';
COMMENT ON COLUMN sys_user_role.role_id IS '角色id';

CREATE TABLE locale (
    id          bigint NOT NULL PRIMARY KEY,
    code        varchar(20),
    name        varchar(100) NOT NULL,
    native_name varchar(100) NOT NULL,
    enabled     boolean DEFAULT true NOT NULL,
    is_default  boolean DEFAULT false NOT NULL,
    is_deleted  boolean DEFAULT false NOT NULL,
    sort_order  integer DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX uk_locale_code
    ON locale (code)
    WHERE is_deleted = false;

COMMENT ON TABLE locale IS '国际化语言';

COMMENT ON COLUMN locale.id IS 'id';
COMMENT ON COLUMN locale.code IS '国际化编码';
COMMENT ON COLUMN locale.name IS '语言名称';
COMMENT ON COLUMN locale.native_name IS '本地名称';
COMMENT ON COLUMN locale.enabled IS '是否启用';
COMMENT ON COLUMN locale.is_default IS '是否默认语言';
COMMENT ON COLUMN locale.is_deleted IS '是否已逻辑删除';
COMMENT ON COLUMN locale.sort_order IS '排序';

CREATE TABLE website_product_category (
    id           bigint NOT NULL PRIMARY KEY,
    gmt_create   timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean DEFAULT false NOT NULL,
    code         varchar(128) NOT NULL,
    sort_order   integer DEFAULT 0 NOT NULL,
    is_show      boolean DEFAULT true NOT NULL
);

CREATE UNIQUE INDEX uk_wpc_code
    ON website_product_category (code)
    WHERE is_deleted = false;

COMMENT ON TABLE website_product_category IS '官网商品分类';

COMMENT ON COLUMN website_product_category.id IS '主键';
COMMENT ON COLUMN website_product_category.gmt_create IS '创建时间';
COMMENT ON COLUMN website_product_category.gmt_modified IS '更新时间';
COMMENT ON COLUMN website_product_category.create_user IS '创建用户 id';
COMMENT ON COLUMN website_product_category.update_user IS '更新用户 id';
COMMENT ON COLUMN website_product_category.is_deleted IS '是否已逻辑删除';
COMMENT ON COLUMN website_product_category.code IS '稳定的分类编码';
COMMENT ON COLUMN website_product_category.sort_order IS '官网展示顺序，数值越小越靠前';
COMMENT ON COLUMN website_product_category.is_show IS '是否在官网展示';

CREATE TABLE website_product_category_i18n (
    id                          bigint NOT NULL PRIMARY KEY,
    gmt_create                  timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified                timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user                 bigint,
    update_user                 bigint,
    is_deleted                  boolean DEFAULT false NOT NULL,
    website_product_category_id bigint NOT NULL,
    locale                      varchar(32) NOT NULL,
    name                        varchar(256) NOT NULL,
    description                 text,
    image_attachment_id         bigint
);

CREATE UNIQUE INDEX uk_wpci_category_locale
    ON website_product_category_i18n (website_product_category_id, locale)
    WHERE is_deleted = false;

COMMENT ON TABLE website_product_category_i18n IS '官网商品分类国际化';

COMMENT ON COLUMN website_product_category_i18n.id IS '主键';
COMMENT ON COLUMN website_product_category_i18n.gmt_create IS '创建时间';
COMMENT ON COLUMN website_product_category_i18n.gmt_modified IS '更新时间';
COMMENT ON COLUMN website_product_category_i18n.create_user IS '创建用户 id';
COMMENT ON COLUMN website_product_category_i18n.update_user IS '更新用户 id';
COMMENT ON COLUMN website_product_category_i18n.is_deleted IS '是否已逻辑删除';
COMMENT ON COLUMN website_product_category_i18n.website_product_category_id IS '商品分类 id（逻辑关联）';
COMMENT ON COLUMN website_product_category_i18n.locale IS '语言标识，例如 zh-CN、en-US';
COMMENT ON COLUMN website_product_category_i18n.name IS '分类名称';
COMMENT ON COLUMN website_product_category_i18n.description IS '分类描述';
COMMENT ON COLUMN website_product_category_i18n.image_attachment_id IS '分类图片附件id';

CREATE TABLE website_product (
    id             bigint NOT NULL PRIMARY KEY,
    gmt_create     timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified   timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user    bigint,
    update_user    bigint,
    is_deleted     boolean DEFAULT false NOT NULL,
    category_id    bigint NOT NULL,
    slug           varchar(128) NOT NULL,
    sort_order     integer DEFAULT 0 NOT NULL,
    is_show        boolean DEFAULT true NOT NULL,
    is_recommended boolean DEFAULT false NOT NULL
);

CREATE UNIQUE INDEX uk_wp_slug
    ON website_product (slug)
    WHERE is_deleted = false;

CREATE INDEX idx_wp_category_display
    ON website_product (category_id, sort_order)
    WHERE ((is_deleted = false) AND (is_show = true));

COMMENT ON TABLE website_product IS '官网商品';

COMMENT ON COLUMN website_product.id IS '主键';
COMMENT ON COLUMN website_product.gmt_create IS '创建时间';
COMMENT ON COLUMN website_product.gmt_modified IS '更新时间';
COMMENT ON COLUMN website_product.create_user IS '创建用户 id';
COMMENT ON COLUMN website_product.update_user IS '更新用户 id';
COMMENT ON COLUMN website_product.is_deleted IS '是否已逻辑删除';
COMMENT ON COLUMN website_product.category_id IS '商品分类 id（逻辑关联）';
COMMENT ON COLUMN website_product.slug IS '官网详情页路由标识；创建后不应随意修改';
COMMENT ON COLUMN website_product.sort_order IS '官网展示顺序，数值越小越靠前';
COMMENT ON COLUMN website_product.is_show IS '是否在官网展示';
COMMENT ON COLUMN website_product.is_recommended IS '是否用于全局推荐展示位';

CREATE TABLE website_product_i18n (
    id                          bigint NOT NULL PRIMARY KEY,
    gmt_create                  timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified                timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user                 bigint,
    update_user                 bigint,
    is_deleted                  boolean DEFAULT false NOT NULL,
    product_id                  bigint NOT NULL,
    locale                      varchar(32) NOT NULL,
    name                        varchar(128) NOT NULL,
    subtitle                    varchar(128),
    summary                     text,
    tagline                     text,
    feature_introduction        text,
    specification_introduction  text,
    application_introduction    text,
    case_introduction           text,
    cover_image_attachment_id   bigint,
    feature_image_attachment_id bigint
);

CREATE UNIQUE INDEX uk_wpi_product_locale
    ON website_product_i18n (product_id, locale)
    WHERE is_deleted = false;

COMMENT ON TABLE website_product_i18n IS '官网商品国际化';

COMMENT ON COLUMN website_product_i18n.id IS '主键';
COMMENT ON COLUMN website_product_i18n.gmt_create IS '创建时间';
COMMENT ON COLUMN website_product_i18n.gmt_modified IS '更新时间';
COMMENT ON COLUMN website_product_i18n.create_user IS '创建用户 id';
COMMENT ON COLUMN website_product_i18n.update_user IS '更新用户 id';
COMMENT ON COLUMN website_product_i18n.is_deleted IS '是否已逻辑删除';
COMMENT ON COLUMN website_product_i18n.product_id IS '商品 id（逻辑关联）';
COMMENT ON COLUMN website_product_i18n.locale IS '语言标识，例如 zh-CN、en-US';
COMMENT ON COLUMN website_product_i18n.name IS '商品名称';
COMMENT ON COLUMN website_product_i18n.subtitle IS '商品卡片的次级名称，例如英文名称';
COMMENT ON COLUMN website_product_i18n.summary IS '商品卡片摘要';
COMMENT ON COLUMN website_product_i18n.tagline IS '商品详情页 Hero 文案';
COMMENT ON COLUMN website_product_i18n.feature_introduction IS '商品特点区域介绍文案';
COMMENT ON COLUMN website_product_i18n.specification_introduction IS '技术参数区域介绍文案';
COMMENT ON COLUMN website_product_i18n.application_introduction IS '应用场景区域介绍文案';
COMMENT ON COLUMN website_product_i18n.case_introduction IS '案例展示区域介绍文案';
COMMENT ON COLUMN website_product_i18n.cover_image_attachment_id IS ' 商品卡片封面图片附件 id';
COMMENT ON COLUMN website_product_i18n.feature_image_attachment_id IS '商品特点区域图片附件 id';

CREATE TABLE website_product_detail_item (
    id              bigint NOT NULL PRIMARY KEY,
    gmt_create      timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified    timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user     bigint,
    update_user     bigint,
    is_deleted      boolean DEFAULT false NOT NULL,
    product_i18n_id bigint NOT NULL,
    item_type       varchar(32) NOT NULL,
    title           varchar(256) NOT NULL,
    content         text NOT NULL,
    sort_order      integer DEFAULT 0 NOT NULL,
    is_show         boolean DEFAULT true NOT NULL
);

CREATE UNIQUE INDEX uk_wpdi_i18n_type_sort
    ON website_product_detail_item (product_i18n_id, item_type, sort_order)
    WHERE is_deleted = false;

CREATE INDEX idx_wpdi_display
    ON website_product_detail_item (product_i18n_id, item_type, sort_order)
    WHERE ((is_deleted = false) AND (is_show = true));

ALTER TABLE website_product_detail_item
    ADD CONSTRAINT ck_wpdi_item_type CHECK (
        item_type IN ('FEATURE', 'SPECIFICATION')
    );

COMMENT ON TABLE website_product_detail_item IS '官网商品文本明细（特点、技术参数）';

COMMENT ON COLUMN website_product_detail_item.id IS '主键';
COMMENT ON COLUMN website_product_detail_item.gmt_create IS '创建时间';
COMMENT ON COLUMN website_product_detail_item.gmt_modified IS '更新时间';
COMMENT ON COLUMN website_product_detail_item.create_user IS '创建用户 id';
COMMENT ON COLUMN website_product_detail_item.update_user IS '更新用户 id';
COMMENT ON COLUMN website_product_detail_item.is_deleted IS '是否已逻辑删除';
COMMENT ON COLUMN website_product_detail_item.product_i18n_id IS '商品国际化 id（逻辑关联）';
COMMENT ON COLUMN website_product_detail_item.item_type IS '明细类型：FEATURE=特点，SPECIFICATION=技术参数';
COMMENT ON COLUMN website_product_detail_item.title IS '特点标题或参数名称';
COMMENT ON COLUMN website_product_detail_item.content IS '特点描述或参数值';
COMMENT ON COLUMN website_product_detail_item.sort_order IS '同类型内容展示顺序，数值越小越靠前';
COMMENT ON COLUMN website_product_detail_item.is_show IS '是否在官网展示';

CREATE TABLE website_product_media_item (
    id                  bigint NOT NULL PRIMARY KEY,
    gmt_create          timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified        timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user         bigint,
    update_user         bigint,
    is_deleted          boolean DEFAULT false NOT NULL,
    product_i18n_id     bigint NOT NULL,
    item_type           varchar(32) NOT NULL,
    title               varchar(256) NOT NULL,
    description         text,
    image_attachment_id bigint NOT NULL,
    sort_order          integer DEFAULT 0 NOT NULL,
    is_show             boolean DEFAULT true NOT NULL
);

CREATE UNIQUE INDEX uk_wpmi_i18n_type_sort
    ON website_product_media_item (product_i18n_id, item_type, sort_order)
    WHERE is_deleted = false;

CREATE INDEX idx_wpmi_display
    ON website_product_media_item (product_i18n_id, item_type, sort_order)
    WHERE ((is_deleted = false) AND (is_show = true));

ALTER TABLE website_product_media_item
    ADD CONSTRAINT ck_wpmi_item_type CHECK (
        item_type IN ('APPLICATION', 'CASE', 'CAROUSEL_IMAGE', 'DETAIL_IMAGE')
    );

COMMENT ON TABLE website_product_media_item IS '官网商品媒体明细（应用场景、案例展示）';

COMMENT ON COLUMN website_product_media_item.id IS '主键';
COMMENT ON COLUMN website_product_media_item.gmt_create IS '创建时间';
COMMENT ON COLUMN website_product_media_item.gmt_modified IS '更新时间';
COMMENT ON COLUMN website_product_media_item.create_user IS '创建用户 id';
COMMENT ON COLUMN website_product_media_item.update_user IS '更新用户 id';
COMMENT ON COLUMN website_product_media_item.is_deleted IS '是否已逻辑删除';
COMMENT ON COLUMN website_product_media_item.product_i18n_id IS '商品国际化 id（逻辑关联）';
COMMENT ON COLUMN website_product_media_item.item_type IS '明细类型：APPLICATION=应用场景，CASE=案例展示, CAROUSEL_IMAGE=商品轮播图, DETAIL_IMAGE=商品详情图';
COMMENT ON COLUMN website_product_media_item.title IS '应用场景或案例标题';
COMMENT ON COLUMN website_product_media_item.description IS '应用场景或案例描述';
COMMENT ON COLUMN website_product_media_item.image_attachment_id IS '应用场景或案例图片附件 ID';
COMMENT ON COLUMN website_product_media_item.sort_order IS '同类型内容展示顺序，数值越小越靠前';
COMMENT ON COLUMN website_product_media_item.is_show IS '是否在官网展示';

CREATE TABLE website_product_recommend (
    id                   bigint NOT NULL PRIMARY KEY,
    gmt_create           timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified         timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user          bigint,
    update_user          bigint,
    is_deleted           boolean DEFAULT false NOT NULL,
    product_id           bigint NOT NULL,
    recommend_product_id bigint NOT NULL,
    sort_order           integer DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX uk_wpr_product_recommend
    ON website_product_recommend (product_id, recommend_product_id)
    WHERE is_deleted = false;

CREATE INDEX idx_wpr_display
    ON website_product_recommend (product_id, sort_order)
    WHERE is_deleted = false;

ALTER TABLE website_product_recommend
    ADD CONSTRAINT ck_wpr_not_self_recommend CHECK (
        product_id <> recommend_product_id
    );

COMMENT ON TABLE website_product_recommend IS '官网商品详情页定向推荐关联表';

COMMENT ON COLUMN website_product_recommend.id IS '主键';
COMMENT ON COLUMN website_product_recommend.gmt_create IS '创建时间';
COMMENT ON COLUMN website_product_recommend.gmt_modified IS '更新时间';
COMMENT ON COLUMN website_product_recommend.create_user IS '创建用户 id';
COMMENT ON COLUMN website_product_recommend.update_user IS '更新用户 id';
COMMENT ON COLUMN website_product_recommend.is_deleted IS '是否已逻辑删除';
COMMENT ON COLUMN website_product_recommend.product_id IS '当前详情页商品 id';
COMMENT ON COLUMN website_product_recommend.recommend_product_id IS '被推荐商品 id';
COMMENT ON COLUMN website_product_recommend.sort_order IS '推荐商品展示顺序，数值越小越靠前';

CREATE TABLE attachment (
    id            bigint NOT NULL PRIMARY KEY,
    storage_type  varchar(32) DEFAULT 'rustfs' NOT NULL,
    bucket_name   varchar(128) NOT NULL,
    object_key    varchar(1024) NOT NULL,
    original_name varchar(255) NOT NULL,
    content_type  varchar(128),
    file_size     bigint NOT NULL,
    etag          varchar(128),
    access_type   varchar(16) DEFAULT 'private' NOT NULL,
    gmt_create    timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified  timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_user   bigint,
    create_user   bigint,
    is_deleted    boolean DEFAULT false NOT NULL
);

ALTER TABLE attachment
    ADD CONSTRAINT uk_attachment_object UNIQUE (storage_type, bucket_name, object_key);

CREATE INDEX idx_attachment_bucket_name
    ON attachment (bucket_name);

CREATE INDEX idx_attachment_created_at
    ON attachment (gmt_create desc);

ALTER TABLE attachment
    ADD CONSTRAINT ck_attachment_access_type CHECK (
        access_type IN ('public', 'private')
    );

COMMENT ON INDEX idx_attachment_bucket_name IS '附件存储桶名称索引';
COMMENT ON INDEX idx_attachment_created_at IS '附件创建时间倒序索引';

COMMENT ON TABLE attachment IS '附件信息表，用于记录上传到对象存储中的文件信息';

COMMENT ON COLUMN attachment.id IS '附件id，业务主键';
COMMENT ON COLUMN attachment.storage_type IS '存储类型，例如：rustfs、s3、oss、cos';
COMMENT ON COLUMN attachment.bucket_name IS '对象存储桶名称，例如：mini-erp';
COMMENT ON COLUMN attachment.object_key IS '对象在存储桶中的唯一key，例如：product/2026/09/abc.png';
COMMENT ON COLUMN attachment.original_name IS '用户上传时的原始文件名称，例如：img_6232.png';
COMMENT ON COLUMN attachment.content_type IS '文件mime类型，例如：image/png、image/jpeg、application/pdf';
COMMENT ON COLUMN attachment.file_size IS '文件大小，单位：字节（byte）';
COMMENT ON COLUMN attachment.etag IS '对象存储返回的etag，用于标识对象版本或内容特征，不建议作为严格文件哈希使用';
COMMENT ON COLUMN attachment.access_type IS '访问类型：public-公开访问，private-私有访问；私有附件需要生成预签名url访问';
COMMENT ON COLUMN attachment.gmt_create IS '创建时间';
COMMENT ON COLUMN attachment.gmt_modified IS '最后修改时间';
COMMENT ON COLUMN attachment.update_user IS '最后修改人id';
COMMENT ON COLUMN attachment.create_user IS '创建人id';
COMMENT ON COLUMN attachment.is_deleted IS '逻辑删除标记：false-未删除，true-已删除';

CREATE TABLE vault_account (
    id                       bigint NOT NULL PRIMARY KEY,
    owner_id                 bigint NOT NULL,
    webauthn_user_handle     bytea NOT NULL,
    auth_epoch               bigint DEFAULT 1 NOT NULL,
    credential_revision      bigint DEFAULT 1 NOT NULL,
    recovery_required        boolean DEFAULT true NOT NULL,
    active_recovery_batch_id uuid,
    gmt_create               timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified             timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user              bigint,
    update_user              bigint,
    is_deleted               boolean DEFAULT false NOT NULL,
    delete_user              bigint,
    gmt_deleted              timestamp with time zone
);

ALTER TABLE vault_account
    ADD UNIQUE (owner_id);

ALTER TABLE vault_account
    ADD UNIQUE (webauthn_user_handle);

ALTER TABLE vault_account
    ADD CONSTRAINT vault_account_id_check CHECK (
        id > 0
    );

ALTER TABLE vault_account
    ADD CONSTRAINT vault_account_webauthn_user_handle_check CHECK (
        octet_length(webauthn_user_handle) = 32
    );

ALTER TABLE vault_account
    ADD CONSTRAINT vault_account_auth_epoch_check CHECK (
        auth_epoch BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_account
    ADD CONSTRAINT vault_account_credential_revision_check CHECK (
        credential_revision BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_account
    ADD CONSTRAINT ck_account_recovery_ready CHECK (
        recovery_required OR active_recovery_batch_id IS NOT NULL
    );

ALTER TABLE vault_account
    ADD CONSTRAINT ck_vault_account_delete_audit CHECK (
        (NOT is_deleted AND delete_user IS NULL AND gmt_deleted IS NULL)
        OR (is_deleted AND gmt_deleted IS NOT NULL)
    );

COMMENT ON TABLE vault_account IS '密码库独立认证账户；不保存主密码、账户密钥、恢复密钥或条目明文';

COMMENT ON COLUMN vault_account.id IS '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';
COMMENT ON COLUMN vault_account.owner_id IS '现有不可变系统用户 ID；API ownerSubject 为其规范十进制字符串';
COMMENT ON COLUMN vault_account.webauthn_user_handle IS '随机 WebAuthn 用户句柄，固定 32 字节，不使用用户名或邮箱';
COMMENT ON COLUMN vault_account.auth_epoch IS '认证代次；请求必须与会话代次相等，敏感变更在数据库事务内递增';
COMMENT ON COLUMN vault_account.credential_revision IS '凭据集合版本，用于新增、撤销和恢复操作的并发检查';
COMMENT ON COLUMN vault_account.recovery_required IS '首次绑定或身份恢复后，未确认新一批 MFA 恢复码时禁止数据接口';
COMMENT ON COLUMN vault_account.active_recovery_batch_id IS '当前有效 MFA 恢复码批次；未确认的批次不能用于恢复';
COMMENT ON COLUMN vault_account.gmt_create IS '创建时间';
COMMENT ON COLUMN vault_account.gmt_modified IS '更新时间，由应用更新，DEFAULT 不会自动更新';
COMMENT ON COLUMN vault_account.create_user IS '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';
COMMENT ON COLUMN vault_account.update_user IS '最近更新人系统用户 ID，由服务端填充；系统任务可为空';
COMMENT ON COLUMN vault_account.is_deleted IS '逻辑删除标记，false=有效、true=已删除';
COMMENT ON COLUMN vault_account.delete_user IS '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';
COMMENT ON COLUMN vault_account.gmt_deleted IS '逻辑删除时间，删除时与标记及删除人一起写入';

CREATE TABLE vault_passkey (
    id              bigint NOT NULL PRIMARY KEY,
    account_id      bigint NOT NULL,
    credential_id   bytea NOT NULL,
    public_key_cose bytea NOT NULL,
    signature_count bigint DEFAULT 0 NOT NULL,
    transports      text[] DEFAULT '{}' NOT NULL,
    aaguid          uuid NOT NULL,
    backup_eligible boolean NOT NULL,
    backup_state    boolean NOT NULL,
    label           varchar(128) NOT NULL,
    last_used_at    timestamp with time zone,
    revoked_at      timestamp with time zone,
    gmt_create      timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified    timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user     bigint,
    update_user     bigint,
    is_deleted      boolean DEFAULT false NOT NULL,
    delete_user     bigint,
    gmt_deleted     timestamp with time zone
);

ALTER TABLE vault_passkey
    ADD UNIQUE (credential_id);

CREATE INDEX idx_vault_passkey_account
    ON vault_passkey (account_id);

CREATE INDEX idx_vault_passkey_active
    ON vault_passkey (account_id, id)
    WHERE ((revoked_at IS NULL) AND (is_deleted = false));

ALTER TABLE vault_passkey
    ADD CONSTRAINT vault_passkey_id_check CHECK (
        id > 0
    );

ALTER TABLE vault_passkey
    ADD CONSTRAINT vault_passkey_credential_id_check CHECK (
        octet_length(credential_id) BETWEEN 1 AND 1024
    );

ALTER TABLE vault_passkey
    ADD CONSTRAINT vault_passkey_public_key_cose_check CHECK (
        octet_length(public_key_cose) BETWEEN 1 AND 4096
    );

ALTER TABLE vault_passkey
    ADD CONSTRAINT vault_passkey_signature_count_check CHECK (
        signature_count BETWEEN 0 AND 4294967295
    );

ALTER TABLE vault_passkey
    ADD CONSTRAINT vault_passkey_transports_check CHECK (
        transports <@ ARRAY[
            'usb',
            'nfc',
            'ble',
            'internal',
            'hybrid',
            'smart-card'
        ]
    );

ALTER TABLE vault_passkey
    ADD CONSTRAINT vault_passkey_label_check CHECK (
        char_length(label) BETWEEN 1 AND 128
    );

ALTER TABLE vault_passkey
    ADD CONSTRAINT ck_passkey_backup CHECK (
        NOT backup_state OR backup_eligible
    );

ALTER TABLE vault_passkey
    ADD CONSTRAINT ck_vault_passkey_delete_audit CHECK (
        (NOT is_deleted AND delete_user IS NULL AND gmt_deleted IS NULL)
        OR (is_deleted AND gmt_deleted IS NOT NULL)
    );

ALTER TABLE vault_passkey
    ADD CONSTRAINT ck_vault_passkey_delete_state CHECK (
        NOT is_deleted OR revoked_at IS NOT NULL
    );

COMMENT ON TABLE vault_passkey IS 'WebAuthn 凭据；保存公钥和验证元数据，不保存认证器私钥';

COMMENT ON COLUMN vault_passkey.id IS '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';
COMMENT ON COLUMN vault_passkey.account_id IS '所属密码库认证账户';
COMMENT ON COLUMN vault_passkey.credential_id IS 'WebAuthn 凭据 ID；HTTP 中使用无填充 Base64url';
COMMENT ON COLUMN vault_passkey.public_key_cose IS '验证库输出的 COSE 公钥';
COMMENT ON COLUMN vault_passkey.signature_count IS '签名计数，由 WebAuthn 库按认证器类型执行检查，不自行要求每次必增';
COMMENT ON COLUMN vault_passkey.transports IS '认证器传输提示；不作为认证成功依据';
COMMENT ON COLUMN vault_passkey.aaguid IS '认证器型号标识，由已验证的注册结果取得';
COMMENT ON COLUMN vault_passkey.backup_eligible IS '凭据是否允许同步或备份';
COMMENT ON COLUMN vault_passkey.backup_state IS '凭据是否已备份；不能在不支持备份时为真';
COMMENT ON COLUMN vault_passkey.label IS '用户可见名称，仅作为纯文本展示';
COMMENT ON COLUMN vault_passkey.last_used_at IS '最近一次成功认证时间';
COMMENT ON COLUMN vault_passkey.revoked_at IS '撤销时间；被撤销的凭据不得用于认证';
COMMENT ON COLUMN vault_passkey.gmt_create IS '创建时间';
COMMENT ON COLUMN vault_passkey.gmt_modified IS '更新时间，由应用更新，DEFAULT 不会自动更新';
COMMENT ON COLUMN vault_passkey.create_user IS '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';
COMMENT ON COLUMN vault_passkey.update_user IS '最近更新人系统用户 ID，由服务端填充；系统任务可为空';
COMMENT ON COLUMN vault_passkey.is_deleted IS '逻辑删除标记，false=有效、true=已删除';
COMMENT ON COLUMN vault_passkey.delete_user IS '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';
COMMENT ON COLUMN vault_passkey.gmt_deleted IS '逻辑删除时间，删除时与标记及删除人一起写入';

CREATE TABLE vault_auth_recovery_code (
    id           bigint NOT NULL PRIMARY KEY,
    account_id   bigint NOT NULL,
    batch_id     uuid NOT NULL,
    code_digest  bytea NOT NULL,
    confirmed_at timestamp with time zone,
    consumed_at  timestamp with time zone,
    revoked_at   timestamp with time zone,
    gmt_create   timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean DEFAULT false NOT NULL,
    delete_user  bigint,
    gmt_deleted  timestamp with time zone
);

ALTER TABLE vault_auth_recovery_code
    ADD UNIQUE (code_digest);

CREATE INDEX idx_vault_recovery_account_batch
    ON vault_auth_recovery_code (account_id, batch_id);

ALTER TABLE vault_auth_recovery_code
    ADD CONSTRAINT vault_auth_recovery_code_id_check CHECK (
        id > 0
    );

ALTER TABLE vault_auth_recovery_code
    ADD CONSTRAINT vault_auth_recovery_code_code_digest_check CHECK (
        octet_length(code_digest) = 32
    );

ALTER TABLE vault_auth_recovery_code
    ADD CONSTRAINT ck_recovery_code_consumed CHECK (
        consumed_at IS NULL OR confirmed_at IS NOT NULL
    );

ALTER TABLE vault_auth_recovery_code
    ADD CONSTRAINT ck_vault_auth_recovery_code_delete_audit CHECK (
        (NOT is_deleted AND delete_user IS NULL AND gmt_deleted IS NULL)
        OR (is_deleted AND gmt_deleted IS NOT NULL)
    );

ALTER TABLE vault_auth_recovery_code
    ADD CONSTRAINT ck_vault_auth_recovery_code_delete_state CHECK (
        NOT is_deleted OR consumed_at IS NOT NULL OR revoked_at IS NOT NULL
    );

COMMENT ON TABLE vault_auth_recovery_code IS '仅用于重新绑定 Passkey 的一次性 MFA 恢复码摘要；与客户端解密恢复密钥 RK 完全分离';

COMMENT ON COLUMN vault_auth_recovery_code.id IS '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';
COMMENT ON COLUMN vault_auth_recovery_code.account_id IS '所属认证账户';
COMMENT ON COLUMN vault_auth_recovery_code.batch_id IS '恢复码批次；只有账户 active_recovery_batch_id 指向的批次有效';
COMMENT ON COLUMN vault_auth_recovery_code.code_digest IS '独立随机 32 字节恢复码的 SHA-256 摘要；明文仅生成时返回一次';
COMMENT ON COLUMN vault_auth_recovery_code.confirmed_at IS '该批次经用户重新导入校验后的确认时间';
COMMENT ON COLUMN vault_auth_recovery_code.consumed_at IS '单次原子消费时间';
COMMENT ON COLUMN vault_auth_recovery_code.revoked_at IS '替换批次或安全操作后的撤销时间';
COMMENT ON COLUMN vault_auth_recovery_code.gmt_create IS '创建时间';
COMMENT ON COLUMN vault_auth_recovery_code.gmt_modified IS '更新时间，由应用更新，DEFAULT 不会自动更新';
COMMENT ON COLUMN vault_auth_recovery_code.create_user IS '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';
COMMENT ON COLUMN vault_auth_recovery_code.update_user IS '最近更新人系统用户 ID，由服务端填充；系统任务可为空';
COMMENT ON COLUMN vault_auth_recovery_code.is_deleted IS '逻辑删除标记，false=有效、true=已删除';
COMMENT ON COLUMN vault_auth_recovery_code.delete_user IS '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';
COMMENT ON COLUMN vault_auth_recovery_code.gmt_deleted IS '逻辑删除时间，删除时与标记及删除人一起写入';

CREATE TABLE vault_info (
    id                  bigint NOT NULL PRIMARY KEY,
    vault_id            uuid NOT NULL,
    account_id          bigint NOT NULL,
    status              varchar(16) NOT NULL,
    active_generation   bigint,
    meta_revision       bigint DEFAULT 1 NOT NULL,
    collection_revision bigint DEFAULT 1 NOT NULL,
    gmt_create          timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified        timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user         bigint,
    update_user         bigint,
    is_deleted          boolean DEFAULT false NOT NULL,
    delete_user         bigint,
    gmt_deleted         timestamp with time zone
);

ALTER TABLE vault_info
    ADD UNIQUE (vault_id);

ALTER TABLE vault_info
    ADD UNIQUE (account_id);

ALTER TABLE vault_info
    ADD CONSTRAINT vault_info_id_check CHECK (
        id > 0
    );

ALTER TABLE vault_info
    ADD CONSTRAINT vault_info_status_check CHECK (
        status IN ('EMPTY', 'ACTIVE', 'RESTORING')
    );

ALTER TABLE vault_info
    ADD CONSTRAINT vault_info_active_generation_check CHECK (
        active_generation BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_info
    ADD CONSTRAINT vault_info_meta_revision_check CHECK (
        meta_revision BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_info
    ADD CONSTRAINT vault_info_collection_revision_check CHECK (
        collection_revision BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_info
    ADD CONSTRAINT ck_vault_active_pointer CHECK (
        (status = 'EMPTY' AND active_generation IS NULL)
        OR (status = 'ACTIVE' AND active_generation IS NOT NULL)
        OR status = 'RESTORING'
    );

ALTER TABLE vault_info
    ADD CONSTRAINT ck_vault_info_delete_audit CHECK (
        (NOT is_deleted AND delete_user IS NULL AND gmt_deleted IS NULL)
        OR (is_deleted AND gmt_deleted IS NOT NULL)
    );

COMMENT ON TABLE vault_info IS '个人密码库元数据；一个认证账户仅有一个库';

COMMENT ON COLUMN vault_info.id IS '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';
COMMENT ON COLUMN vault_info.vault_id IS '公开协议库 ID，由客户端生成随机 UUID，以规范小写字符串参与 AAD';
COMMENT ON COLUMN vault_info.account_id IS '所属认证账户，所有归属条件从认证会话推导';
COMMENT ON COLUMN vault_info.status IS 'EMPTY=未初始化占位，ACTIVE=可读取活动代，RESTORING=恢复暂存中';
COMMENT ON COLUMN vault_info.active_generation IS '当前活动代；空库占位为 NULL，逻辑关联 vault_generation，由 Service 事务校验';
COMMENT ON COLUMN vault_info.meta_revision IS '元数据与活动密钥槽版本';
COMMENT ON COLUMN vault_info.collection_revision IS '当前代条目或密钥槽集合版本';
COMMENT ON COLUMN vault_info.gmt_create IS '创建时间';
COMMENT ON COLUMN vault_info.gmt_modified IS '更新时间，由应用更新，DEFAULT 不会自动更新';
COMMENT ON COLUMN vault_info.create_user IS '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';
COMMENT ON COLUMN vault_info.update_user IS '最近更新人系统用户 ID，由服务端填充；系统任务可为空';
COMMENT ON COLUMN vault_info.is_deleted IS '逻辑删除标记，false=有效、true=已删除';
COMMENT ON COLUMN vault_info.delete_user IS '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';
COMMENT ON COLUMN vault_info.gmt_deleted IS '逻辑删除时间，删除时与标记及删除人一起写入';

CREATE TABLE vault_generation (
    id           bigint NOT NULL PRIMARY KEY,
    vault_id     bigint NOT NULL,
    generation   bigint NOT NULL,
    key_epoch    bigint NOT NULL,
    status       varchar(16) NOT NULL,
    gmt_create   timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    retired_at   timestamp with time zone,
    gmt_modified timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean DEFAULT false NOT NULL,
    delete_user  bigint,
    gmt_deleted  timestamp with time zone
);

ALTER TABLE vault_generation
    ADD UNIQUE (vault_id, generation);

CREATE UNIQUE INDEX uk_vault_generation_active
    ON vault_generation (vault_id)
    WHERE status = 'ACTIVE' AND is_deleted = false;

CREATE INDEX idx_vault_generation_cleanup
    ON vault_generation (retired_at, id)
    WHERE status IN ('RETIRED', 'ABORTED');

ALTER TABLE vault_generation
    ADD CONSTRAINT vault_generation_id_check CHECK (
        id > 0
    );

ALTER TABLE vault_generation
    ADD CONSTRAINT vault_generation_generation_check CHECK (
        generation BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_generation
    ADD CONSTRAINT vault_generation_key_epoch_check CHECK (
        key_epoch BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_generation
    ADD CONSTRAINT vault_generation_status_check CHECK (
        status IN ('STAGING', 'ACTIVE', 'RETIRED', 'ABORTED')
    );

ALTER TABLE vault_generation
    ADD CONSTRAINT ck_vault_generation_delete_audit CHECK (
        (NOT is_deleted AND delete_user IS NULL AND gmt_deleted IS NULL)
        OR (is_deleted AND gmt_deleted IS NOT NULL)
    );

ALTER TABLE vault_generation
    ADD CONSTRAINT ck_vault_generation_delete_state CHECK (
        NOT is_deleted OR status IN ('RETIRED', 'ABORTED')
    );

COMMENT ON TABLE vault_generation IS '密码库代次控制记录；清理密文后保留最小记录供轮换回执引用';

COMMENT ON COLUMN vault_generation.id IS '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';
COMMENT ON COLUMN vault_generation.vault_id IS '内部密码库逻辑 ID';
COMMENT ON COLUMN vault_generation.generation IS '库内单调递增代次，取消后不重复使用';
COMMENT ON COLUMN vault_generation.key_epoch IS '客户端密钥代次；普通修改主密码保持不变';
COMMENT ON COLUMN vault_generation.status IS 'STAGING=待提交，ACTIVE=活动，RETIRED=旧代，ABORTED=取消或超时';
COMMENT ON COLUMN vault_generation.gmt_create IS '创建时间';
COMMENT ON COLUMN vault_generation.retired_at IS '退出活动或暂存状态的时间，供密文清理任务使用';
COMMENT ON COLUMN vault_generation.gmt_modified IS '更新时间，由应用更新，DEFAULT 不会自动更新';
COMMENT ON COLUMN vault_generation.create_user IS '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';
COMMENT ON COLUMN vault_generation.update_user IS '最近更新人系统用户 ID，由服务端填充；系统任务可为空';
COMMENT ON COLUMN vault_generation.is_deleted IS '逻辑删除标记，false=有效、true=已删除';
COMMENT ON COLUMN vault_generation.delete_user IS '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';
COMMENT ON COLUMN vault_generation.gmt_deleted IS '逻辑删除时间，删除时与标记及删除人一起写入';

CREATE TABLE vault_key_slot (
    id             bigint NOT NULL PRIMARY KEY,
    vault_id       bigint NOT NULL,
    generation     bigint NOT NULL,
    slot_id        uuid NOT NULL,
    slot_type      varchar(16) NOT NULL,
    key_id         uuid NOT NULL,
    format_version smallint NOT NULL,
    algorithm      varchar(16) NOT NULL,
    iv             bytea NOT NULL,
    ciphertext     bytea NOT NULL,
    kdf_profile    varchar(64),
    kdf_salt       bytea,
    wrap_salt      bytea,
    recovery_salt  bytea,
    gmt_create     timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified   timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user    bigint,
    update_user    bigint,
    is_deleted     boolean DEFAULT false NOT NULL,
    delete_user    bigint,
    gmt_deleted    timestamp with time zone
);

ALTER TABLE vault_key_slot
    ADD UNIQUE (slot_id);

CREATE UNIQUE INDEX uk_vault_key_slot_active
    ON vault_key_slot (vault_id, generation, slot_type)
    WHERE is_deleted = false;

CREATE INDEX idx_vault_key_slot_deleted
    ON vault_key_slot (gmt_deleted, id)
    WHERE (is_deleted = true);

ALTER TABLE vault_key_slot
    ADD CONSTRAINT vault_key_slot_id_check CHECK (
        id > 0
    );

ALTER TABLE vault_key_slot
    ADD CONSTRAINT vault_key_slot_generation_check CHECK (
        generation BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_key_slot
    ADD CONSTRAINT vault_key_slot_slot_type_check CHECK (
        slot_type IN ('NORMAL', 'RECOVERY')
    );

ALTER TABLE vault_key_slot
    ADD CONSTRAINT vault_key_slot_format_version_check CHECK (
        format_version = 1
    );

ALTER TABLE vault_key_slot
    ADD CONSTRAINT vault_key_slot_algorithm_check CHECK (
        algorithm = 'A256GCM'
    );

ALTER TABLE vault_key_slot
    ADD CONSTRAINT vault_key_slot_iv_check CHECK (
        octet_length(iv) = 12
    );

ALTER TABLE vault_key_slot
    ADD CONSTRAINT vault_key_slot_ciphertext_check CHECK (
        octet_length(ciphertext) = 48
    );

ALTER TABLE vault_key_slot
    ADD CONSTRAINT vault_key_slot_kdf_profile_check CHECK (
        kdf_profile = 'ARGON2ID_V19_M65536_T3_P4_L32'
    );

ALTER TABLE vault_key_slot
    ADD CONSTRAINT vault_key_slot_kdf_salt_check CHECK (
        octet_length(kdf_salt) = 16
    );

ALTER TABLE vault_key_slot
    ADD CONSTRAINT vault_key_slot_wrap_salt_check CHECK (
        octet_length(wrap_salt) = 32
    );

ALTER TABLE vault_key_slot
    ADD CONSTRAINT vault_key_slot_recovery_salt_check CHECK (
        octet_length(recovery_salt) = 32
    );

ALTER TABLE vault_key_slot
    ADD CONSTRAINT ck_key_slot_parameters CHECK (
        (slot_type = 'NORMAL' AND kdf_profile IS NOT NULL
            AND kdf_salt IS NOT NULL AND wrap_salt IS NOT NULL AND recovery_salt IS NULL)
        OR (slot_type = 'RECOVERY' AND kdf_profile IS NULL
            AND kdf_salt IS NULL AND wrap_salt IS NULL AND recovery_salt IS NOT NULL)
    );

ALTER TABLE vault_key_slot
    ADD CONSTRAINT ck_vault_key_slot_delete_audit CHECK (
        (NOT is_deleted AND delete_user IS NULL AND gmt_deleted IS NULL)
        OR (is_deleted AND gmt_deleted IS NOT NULL)
    );

COMMENT ON TABLE vault_key_slot IS '每代正常与恢复 VMK 密钥槽；仅保存公开参数和经过认证加密的 VMK';

COMMENT ON COLUMN vault_key_slot.id IS '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';
COMMENT ON COLUMN vault_key_slot.vault_id IS '内部密码库逻辑 ID';
COMMENT ON COLUMN vault_key_slot.generation IS '所属代次';
COMMENT ON COLUMN vault_key_slot.slot_id IS '公开密钥槽 ID；每次重建槽生成新值';
COMMENT ON COLUMN vault_key_slot.slot_type IS 'NORMAL=主密码加账户密钥，RECOVERY=独立恢复密钥';
COMMENT ON COLUMN vault_key_slot.key_id IS 'NORMAL 对应 accountKeyId，RECOVERY 对应 recoveryKeyId；不是秘密本身';
COMMENT ON COLUMN vault_key_slot.format_version IS '密文盒版本，v1 仅支持 1';
COMMENT ON COLUMN vault_key_slot.algorithm IS '认证加密算法，v1 固定 A256GCM';
COMMENT ON COLUMN vault_key_slot.iv IS '每次加密独立生成的 12 字节 IV';
COMMENT ON COLUMN vault_key_slot.ciphertext IS '32 字节 VMK 密文及末尾 16 字节 GCM 标签';
COMMENT ON COLUMN vault_key_slot.kdf_profile IS '正常槽固定 KDF 档案，不接收任意内存或迭代参数';
COMMENT ON COLUMN vault_key_slot.kdf_salt IS '正常槽公开 Argon2 盐';
COMMENT ON COLUMN vault_key_slot.wrap_salt IS '正常槽公开 HKDF 盐';
COMMENT ON COLUMN vault_key_slot.recovery_salt IS '恢复槽公开 HKDF 盐';
COMMENT ON COLUMN vault_key_slot.gmt_create IS '创建时间';
COMMENT ON COLUMN vault_key_slot.gmt_modified IS '更新时间，由应用更新，DEFAULT 不会自动更新';
COMMENT ON COLUMN vault_key_slot.create_user IS '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';
COMMENT ON COLUMN vault_key_slot.update_user IS '最近更新人系统用户 ID，由服务端填充；系统任务可为空';
COMMENT ON COLUMN vault_key_slot.is_deleted IS '逻辑删除标记，false=有效、true=已删除';
COMMENT ON COLUMN vault_key_slot.delete_user IS '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';
COMMENT ON COLUMN vault_key_slot.gmt_deleted IS '逻辑删除时间，删除时与标记及删除人一起写入';

CREATE TABLE vault_item (
    id                            bigint NOT NULL PRIMARY KEY,
    vault_id                      bigint NOT NULL,
    generation                    bigint NOT NULL,
    key_epoch                     bigint NOT NULL,
    item_id                       uuid NOT NULL,
    content_version               bigint NOT NULL,
    row_version                   bigint NOT NULL,
    wrapped_dek_format_version    smallint NOT NULL,
    wrapped_dek_algorithm         varchar(16) NOT NULL,
    wrapped_dek_iv                bytea NOT NULL,
    wrapped_dek_ciphertext        bytea NOT NULL,
    encrypted_item_format_version smallint NOT NULL,
    encrypted_item_algorithm      varchar(16) NOT NULL,
    encrypted_item_iv             bytea NOT NULL,
    encrypted_item_ciphertext     bytea NOT NULL,
    gmt_create                    timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified                  timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user                   bigint,
    update_user                   bigint,
    is_deleted                    boolean DEFAULT false NOT NULL,
    delete_user                   bigint,
    gmt_deleted                   timestamp with time zone
);

ALTER TABLE vault_item
    ADD UNIQUE (vault_id, generation, item_id);

CREATE INDEX idx_vault_item_deleted
    ON vault_item (gmt_deleted, id)
    WHERE (is_deleted = true);

CREATE INDEX vault_item_deleted_page_idx
    ON vault_item (vault_id asc, generation asc, gmt_deleted desc, item_id asc)
    WHERE (is_deleted = true);

CREATE INDEX vault_item_deleted_expiry_idx
    ON vault_item (gmt_deleted, vault_id)
    WHERE (is_deleted = true);

ALTER TABLE vault_item
    ADD CONSTRAINT vault_item_id_check CHECK (
        id > 0
    );

ALTER TABLE vault_item
    ADD CONSTRAINT vault_item_generation_check CHECK (
        generation BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_item
    ADD CONSTRAINT vault_item_key_epoch_check CHECK (
        key_epoch BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_item
    ADD CONSTRAINT vault_item_content_version_check CHECK (
        content_version BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_item
    ADD CONSTRAINT vault_item_row_version_check CHECK (
        row_version BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_item
    ADD CONSTRAINT vault_item_wrapped_dek_format_version_check CHECK (
        wrapped_dek_format_version = 1
    );

ALTER TABLE vault_item
    ADD CONSTRAINT vault_item_wrapped_dek_algorithm_check CHECK (
        wrapped_dek_algorithm = 'A256GCM'
    );

ALTER TABLE vault_item
    ADD CONSTRAINT vault_item_wrapped_dek_iv_check CHECK (
        octet_length(wrapped_dek_iv) = 12
    );

ALTER TABLE vault_item
    ADD CONSTRAINT vault_item_wrapped_dek_ciphertext_check CHECK (
        octet_length(wrapped_dek_ciphertext) = 48
    );

ALTER TABLE vault_item
    ADD CONSTRAINT vault_item_encrypted_item_format_version_check CHECK (
        encrypted_item_format_version = 1
    );

ALTER TABLE vault_item
    ADD CONSTRAINT vault_item_encrypted_item_algorithm_check CHECK (
        encrypted_item_algorithm = 'A256GCM'
    );

ALTER TABLE vault_item
    ADD CONSTRAINT vault_item_encrypted_item_iv_check CHECK (
        octet_length(encrypted_item_iv) = 12
    );

ALTER TABLE vault_item
    ADD CONSTRAINT vault_item_encrypted_item_ciphertext_check CHECK (
        octet_length(encrypted_item_ciphertext) BETWEEN 17 AND 16400
    );

ALTER TABLE vault_item
    ADD CONSTRAINT ck_vault_item_delete_audit CHECK (
        (NOT is_deleted AND delete_user IS NULL AND gmt_deleted IS NULL)
        OR (is_deleted AND gmt_deleted IS NOT NULL)
    );

COMMENT ON TABLE vault_item IS '整条加密的密码条目；暂存和旧代共用本表，不保存可搜索明文或长期历史版本';

COMMENT ON COLUMN vault_item.id IS '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';
COMMENT ON COLUMN vault_item.vault_id IS '内部密码库逻辑 ID';
COMMENT ON COLUMN vault_item.generation IS '所属代次；普通查询只能读取活动代';
COMMENT ON COLUMN vault_item.key_epoch IS '密钥代次，由 Service 在事务内校验与所属代一致';
COMMENT ON COLUMN vault_item.item_id IS '公开条目 ID，库内跨轮换保持稳定';
COMMENT ON COLUMN vault_item.content_version IS '用于 AAD 的内容版本；普通编辑递增，完整轮换保持原值';
COMMENT ON COLUMN vault_item.row_version IS '服务端行版本；普通编辑递增，新代重新从 1 开始';
COMMENT ON COLUMN vault_item.wrapped_dek_format_version IS '条目密钥密文盒版本';
COMMENT ON COLUMN vault_item.wrapped_dek_algorithm IS '条目密钥认证加密算法';
COMMENT ON COLUMN vault_item.wrapped_dek_iv IS '包装 DEK 使用的独立随机 IV';
COMMENT ON COLUMN vault_item.wrapped_dek_ciphertext IS '32 字节 DEK 密文及 16 字节认证标签';
COMMENT ON COLUMN vault_item.encrypted_item_format_version IS '条目内容密文盒版本';
COMMENT ON COLUMN vault_item.encrypted_item_algorithm IS '条目内容认证加密算法';
COMMENT ON COLUMN vault_item.encrypted_item_iv IS '加密条目使用的独立随机 IV';
COMMENT ON COLUMN vault_item.encrypted_item_ciphertext IS '最多 16 KiB 明文对应的密文与 16 字节认证标签';
COMMENT ON COLUMN vault_item.gmt_create IS '创建时间';
COMMENT ON COLUMN vault_item.gmt_modified IS '更新时间，由应用更新，DEFAULT 不会自动更新';
COMMENT ON COLUMN vault_item.create_user IS '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';
COMMENT ON COLUMN vault_item.update_user IS '最近更新人系统用户 ID，由服务端填充；系统任务可为空';
COMMENT ON COLUMN vault_item.is_deleted IS '逻辑删除标记，false=有效、true=已删除';
COMMENT ON COLUMN vault_item.delete_user IS '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';
COMMENT ON COLUMN vault_item.gmt_deleted IS '逻辑删除时间，删除时与标记及删除人一起写入';

CREATE TABLE vault_rotation (
    id                           bigint NOT NULL PRIMARY KEY,
    rotation_id                  uuid NOT NULL,
    vault_id                     bigint NOT NULL,
    mode                         varchar(16) NOT NULL,
    status                       varchar(16) NOT NULL,
    source_generation            bigint,
    target_generation            bigint NOT NULL,
    expected_meta_revision       bigint NOT NULL,
    expected_collection_revision bigint NOT NULL,
    started_auth_epoch           bigint NOT NULL,
    session_binding_id           uuid NOT NULL,
    expected_item_count          integer NOT NULL,
    verified_manifest_digest     bytea,
    commit_receipt               jsonb,
    expires_at                   timestamp with time zone NOT NULL,
    finished_at                  timestamp with time zone,
    gmt_create                   timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified                 timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user                  bigint,
    update_user                  bigint,
    is_deleted                   boolean DEFAULT false NOT NULL,
    delete_user                  bigint,
    gmt_deleted                  timestamp with time zone
);

ALTER TABLE vault_rotation
    ADD UNIQUE (rotation_id);

ALTER TABLE vault_rotation
    ADD UNIQUE (vault_id, target_generation);

CREATE UNIQUE INDEX uk_vault_rotation_open
    ON vault_rotation (vault_id)
    WHERE status = 'UPLOADING' AND is_deleted = false;

CREATE INDEX idx_vault_rotation_source
    ON vault_rotation (vault_id, source_generation);

CREATE INDEX idx_vault_rotation_expiry
    ON vault_rotation (expires_at, id)
    WHERE status = 'UPLOADING';

ALTER TABLE vault_rotation
    ADD CONSTRAINT vault_rotation_id_check CHECK (
        id > 0
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT vault_rotation_mode_check CHECK (
        mode IN ('ROTATION', 'RESTORE')
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT vault_rotation_status_check CHECK (
        status IN ('UPLOADING', 'COMMITTED', 'CANCELLED', 'EXPIRED')
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT vault_rotation_source_generation_check CHECK (
        source_generation BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT vault_rotation_target_generation_check CHECK (
        target_generation BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT vault_rotation_expected_meta_revision_check CHECK (
        expected_meta_revision BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT vault_rotation_expected_collection_revision_check CHECK (
        expected_collection_revision BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT vault_rotation_started_auth_epoch_check CHECK (
        started_auth_epoch BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT vault_rotation_expected_item_count_check CHECK (
        expected_item_count >= 0
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT vault_rotation_verified_manifest_digest_check CHECK (
        octet_length(verified_manifest_digest) = 32
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT vault_rotation_commit_receipt_check CHECK (
        jsonb_typeof(commit_receipt) = 'object' AND octet_length(commit_receipt::text) <= 4096
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT ck_rotation_source CHECK (
        (mode = 'ROTATION' AND source_generation IS NOT NULL)
        OR mode = 'RESTORE'
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT ck_rotation_generation CHECK (
        source_generation IS NULL OR target_generation > source_generation
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT ck_rotation_deadline CHECK (
        expires_at > gmt_create
        AND expires_at <= gmt_create + INTERVAL '30 minutes'
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT ck_rotation_terminal CHECK (
        (status = 'UPLOADING' AND finished_at IS NULL)
        OR (status <> 'UPLOADING' AND finished_at IS NOT NULL)
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT ck_rotation_receipt CHECK (
        (status = 'COMMITTED' AND commit_receipt IS NOT NULL AND verified_manifest_digest IS NOT NULL)
        OR (status <> 'COMMITTED' AND commit_receipt IS NULL AND verified_manifest_digest IS NULL)
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT ck_vault_rotation_delete_audit CHECK (
        (NOT is_deleted AND delete_user IS NULL AND gmt_deleted IS NULL)
        OR (is_deleted AND gmt_deleted IS NOT NULL)
    );

ALTER TABLE vault_rotation
    ADD CONSTRAINT ck_vault_rotation_delete_state CHECK (
        NOT is_deleted OR status IN ('COMMITTED', 'CANCELLED', 'EXPIRED')
    );

COMMENT ON TABLE vault_rotation IS '完整轮换与备份恢复任务；终态回执持久化，用于提交响应丢失后的结果确认';

COMMENT ON COLUMN vault_rotation.id IS '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';
COMMENT ON COLUMN vault_rotation.rotation_id IS '公开任务 ID，不代替账户和会话授权';
COMMENT ON COLUMN vault_rotation.vault_id IS '目标密码库';
COMMENT ON COLUMN vault_rotation.mode IS 'ROTATION=已有库换密钥，RESTORE=向未初始化或空库恢复备份';
COMMENT ON COLUMN vault_rotation.status IS 'UPLOADING=可上传验证，其他状态为不可逆终态';
COMMENT ON COLUMN vault_rotation.source_generation IS '开始时的活动代；目标未初始化时为 NULL';
COMMENT ON COLUMN vault_rotation.target_generation IS '全新暂存代，取消后不复用';
COMMENT ON COLUMN vault_rotation.expected_meta_revision IS '开始时锁定的元数据版本';
COMMENT ON COLUMN vault_rotation.expected_collection_revision IS '开始时锁定的集合版本';
COMMENT ON COLUMN vault_rotation.started_auth_epoch IS '开始时的认证代次；外部敏感变更后禁止旧任务继续提交';
COMMENT ON COLUMN vault_rotation.session_binding_id IS '服务端生成的逻辑会话标识；会话 ID 更新时保持，不是 Cookie 或 Session ID';
COMMENT ON COLUMN vault_rotation.expected_item_count IS '登记的预期条目数量；提交还必须检查逐项集合';
COMMENT ON COLUMN vault_rotation.verified_manifest_digest IS '客户端完整下载校验后提交的规范暂存密文清单摘要，不代表服务器验证了明文';
COMMENT ON COLUMN vault_rotation.commit_receipt IS '仅含提交结果、代次和版本的公开回执，不能含秘密或会话令牌';
COMMENT ON COLUMN vault_rotation.expires_at IS '最长 30 分钟绝对截止时间；二次认证不会延长';
COMMENT ON COLUMN vault_rotation.finished_at IS '提交、取消或超时的终态时间';
COMMENT ON COLUMN vault_rotation.gmt_create IS '创建时间';
COMMENT ON COLUMN vault_rotation.gmt_modified IS '更新时间，由应用更新，DEFAULT 不会自动更新';
COMMENT ON COLUMN vault_rotation.create_user IS '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';
COMMENT ON COLUMN vault_rotation.update_user IS '最近更新人系统用户 ID，由服务端填充；系统任务可为空';
COMMENT ON COLUMN vault_rotation.is_deleted IS '逻辑删除标记，false=有效、true=已删除';
COMMENT ON COLUMN vault_rotation.delete_user IS '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';
COMMENT ON COLUMN vault_rotation.gmt_deleted IS '逻辑删除时间，删除时与标记及删除人一起写入';

CREATE TABLE vault_rotation_expected_item (
    id                     bigint NOT NULL PRIMARY KEY,
    rotation_id            bigint NOT NULL,
    item_id                uuid NOT NULL,
    source_content_version bigint,
    source_row_version     bigint,
    target_content_version bigint NOT NULL,
    gmt_create             timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified           timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user            bigint,
    update_user            bigint,
    is_deleted             boolean DEFAULT false NOT NULL,
    delete_user            bigint,
    gmt_deleted            timestamp with time zone
);

CREATE UNIQUE INDEX uk_vault_rotation_expected_item_rotation_item
    ON vault_rotation_expected_item (rotation_id, item_id);

ALTER TABLE vault_rotation_expected_item
    ADD CONSTRAINT vault_rotation_expected_item_id_check CHECK (
        id > 0
    );

ALTER TABLE vault_rotation_expected_item
    ADD CONSTRAINT vault_rotation_expected_item_source_content_version_check CHECK (
        source_content_version BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_rotation_expected_item
    ADD CONSTRAINT vault_rotation_expected_item_source_row_version_check CHECK (
        source_row_version BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_rotation_expected_item
    ADD CONSTRAINT vault_rotation_expected_item_target_content_version_check CHECK (
        target_content_version BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_rotation_expected_item
    ADD CONSTRAINT ck_rotation_expected_source CHECK (
        (source_content_version IS NULL) = (source_row_version IS NULL)
    );

ALTER TABLE vault_rotation_expected_item
    ADD CONSTRAINT ck_vault_rotation_expected_item_delete_audit CHECK (
        (NOT is_deleted AND delete_user IS NULL AND gmt_deleted IS NULL)
        OR (is_deleted AND gmt_deleted IS NOT NULL)
    );

COMMENT ON TABLE vault_rotation_expected_item IS '轮换开始时冻结的逐项集合，防止只按条目数量提交造成缺项或调包';

COMMENT ON COLUMN vault_rotation_expected_item.id IS '内部记录 ID，由应用使用 IdWorker / ASSIGN_ID 生成';
COMMENT ON COLUMN vault_rotation_expected_item.rotation_id IS '内部轮换任务逻辑 ID';
COMMENT ON COLUMN vault_rotation_expected_item.item_id IS '预期目标条目 ID';
COMMENT ON COLUMN vault_rotation_expected_item.source_content_version IS '轮换源内容版本；备份恢复模式为 NULL';
COMMENT ON COLUMN vault_rotation_expected_item.source_row_version IS '轮换源行版本；备份恢复模式为 NULL';
COMMENT ON COLUMN vault_rotation_expected_item.target_content_version IS '完整轮换保持源版本；备份恢复在新上下文中固定从 1 开始';
COMMENT ON COLUMN vault_rotation_expected_item.gmt_create IS '创建时间';
COMMENT ON COLUMN vault_rotation_expected_item.gmt_modified IS '更新时间，由应用更新，DEFAULT 不会自动更新';
COMMENT ON COLUMN vault_rotation_expected_item.create_user IS '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';
COMMENT ON COLUMN vault_rotation_expected_item.update_user IS '最近更新人系统用户 ID，由服务端填充；系统任务可为空';
COMMENT ON COLUMN vault_rotation_expected_item.is_deleted IS '逻辑删除标记，false=有效、true=已删除';
COMMENT ON COLUMN vault_rotation_expected_item.delete_user IS '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';
COMMENT ON COLUMN vault_rotation_expected_item.gmt_deleted IS '逻辑删除时间，删除时与标记及删除人一起写入';

CREATE TABLE vault_rotation_batch (
    id             bigint NOT NULL PRIMARY KEY,
    rotation_id    bigint NOT NULL,
    batch_id       uuid NOT NULL,
    request_digest bytea NOT NULL,
    item_count     integer NOT NULL,
    gmt_create     timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified   timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user    bigint,
    update_user    bigint,
    is_deleted     boolean DEFAULT false NOT NULL,
    delete_user    bigint,
    gmt_deleted    timestamp with time zone
);

ALTER TABLE vault_rotation_batch
    ADD UNIQUE (rotation_id, batch_id);

ALTER TABLE vault_rotation_batch
    ADD CONSTRAINT vault_rotation_batch_id_check CHECK (
        id > 0
    );

ALTER TABLE vault_rotation_batch
    ADD CONSTRAINT vault_rotation_batch_request_digest_check CHECK (
        octet_length(request_digest) = 32
    );

ALTER TABLE vault_rotation_batch
    ADD CONSTRAINT vault_rotation_batch_item_count_check CHECK (
        item_count BETWEEN 1 AND 100
    );

ALTER TABLE vault_rotation_batch
    ADD CONSTRAINT ck_vault_rotation_batch_delete_audit CHECK (
        (NOT is_deleted AND delete_user IS NULL AND gmt_deleted IS NULL)
        OR (is_deleted AND gmt_deleted IS NOT NULL)
    );

COMMENT ON TABLE vault_rotation_batch IS '暂存批次持久化回执；批次登记与条目写入在同一数据库事务内完成';

COMMENT ON COLUMN vault_rotation_batch.id IS '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';
COMMENT ON COLUMN vault_rotation_batch.rotation_id IS '内部轮换任务逻辑 ID';
COMMENT ON COLUMN vault_rotation_batch.batch_id IS '客户端批次 ID，同任务内唯一';
COMMENT ON COLUMN vault_rotation_batch.request_digest IS '规范请求 SHA-256 摘要；同批次不同内容必须拒绝';
COMMENT ON COLUMN vault_rotation_batch.item_count IS '本批次条目数量；请求体还需限制为 1 MiB';
COMMENT ON COLUMN vault_rotation_batch.gmt_create IS '创建时间';
COMMENT ON COLUMN vault_rotation_batch.gmt_modified IS '更新时间，由应用更新，DEFAULT 不会自动更新';
COMMENT ON COLUMN vault_rotation_batch.create_user IS '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';
COMMENT ON COLUMN vault_rotation_batch.update_user IS '最近更新人系统用户 ID，由服务端填充；系统任务可为空';
COMMENT ON COLUMN vault_rotation_batch.is_deleted IS '逻辑删除标记，false=有效、true=已删除';
COMMENT ON COLUMN vault_rotation_batch.delete_user IS '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';
COMMENT ON COLUMN vault_rotation_batch.gmt_deleted IS '逻辑删除时间，删除时与标记及删除人一起写入';

CREATE TABLE vault_idempotency_record (
    id              bigint NOT NULL PRIMARY KEY,
    account_id      bigint NOT NULL,
    operation       varchar(32) NOT NULL,
    idempotency_key uuid NOT NULL,
    request_digest  bytea NOT NULL,
    http_status     smallint NOT NULL,
    response_body   jsonb NOT NULL,
    gmt_create      timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified    timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user     bigint,
    update_user     bigint,
    is_deleted      boolean DEFAULT false NOT NULL,
    delete_user     bigint,
    gmt_deleted     timestamp with time zone
);

ALTER TABLE vault_idempotency_record
    ADD CONSTRAINT vault_idempotency_record_account_id_operation_idempotency_k_key UNIQUE (account_id, operation, idempotency_key);

ALTER TABLE vault_idempotency_record
    ADD CONSTRAINT vault_idempotency_record_id_check CHECK (
        id > 0
    );

ALTER TABLE vault_idempotency_record
    ADD CONSTRAINT vault_idempotency_record_operation_check CHECK (
        operation IN (
            'INITIALIZE', 'CREATE_ITEM', 'UPDATE_ITEM', 'DELETE_ITEM', 'DELETE_GROUP',
            'BATCH_ITEMS', 'RESTORE_ITEM', 'PURGE_ITEM', 'CHANGE_MASTER', 'ROTATION_START',
            'ROTATION_COMMIT', 'ROTATION_CANCEL', 'ROTATION_RESUME', 'RESTORE_START'
        )
    );

ALTER TABLE vault_idempotency_record
    ADD CONSTRAINT vault_idempotency_record_request_digest_check CHECK (
        octet_length(request_digest) = 32
    );

ALTER TABLE vault_idempotency_record
    ADD CONSTRAINT vault_idempotency_record_http_status_check CHECK (
        http_status BETWEEN 200 AND 299
    );

ALTER TABLE vault_idempotency_record
    ADD CONSTRAINT vault_idempotency_record_response_body_check CHECK (
        jsonb_typeof(response_body) = 'object' AND octet_length(response_body::text) <= 65536
    );

ALTER TABLE vault_idempotency_record
    ADD CONSTRAINT ck_vault_idempotency_record_delete_audit CHECK (
        (NOT is_deleted AND delete_user IS NULL AND gmt_deleted IS NULL)
        OR (is_deleted AND gmt_deleted IS NOT NULL)
    );

COMMENT ON TABLE vault_idempotency_record IS '业务写请求的持久化幂等回执；与对应业务变更同事务提交，不依赖 Redis';

COMMENT ON COLUMN vault_idempotency_record.id IS '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';
COMMENT ON COLUMN vault_idempotency_record.account_id IS '所属认证账户';
COMMENT ON COLUMN vault_idempotency_record.operation IS '业务操作类型；批次幂等使用 rotation_batch';
COMMENT ON COLUMN vault_idempotency_record.idempotency_key IS '客户端随机幂等键';
COMMENT ON COLUMN vault_idempotency_record.request_digest IS '绑定账户、操作及规范业务参数的 SHA-256 摘要';
COMMENT ON COLUMN vault_idempotency_record.http_status IS '已成功提交结果的真实 HTTP 状态';
COMMENT ON COLUMN vault_idempotency_record.response_body IS '仅保存最小公开 JSON 回执；禁止保存任何解密秘密、MFA 恢复码或会话令牌';
COMMENT ON COLUMN vault_idempotency_record.gmt_create IS '创建时间';
COMMENT ON COLUMN vault_idempotency_record.gmt_modified IS '更新时间，由应用更新，DEFAULT 不会自动更新';
COMMENT ON COLUMN vault_idempotency_record.create_user IS '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';
COMMENT ON COLUMN vault_idempotency_record.update_user IS '最近更新人系统用户 ID，由服务端填充；系统任务可为空';
COMMENT ON COLUMN vault_idempotency_record.is_deleted IS '逻辑删除标记，false=有效、true=已删除';
COMMENT ON COLUMN vault_idempotency_record.delete_user IS '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';
COMMENT ON COLUMN vault_idempotency_record.gmt_deleted IS '逻辑删除时间，删除时与标记及删除人一起写入';

CREATE TABLE vault_session_revocation_outbox (
    id              bigint NOT NULL PRIMARY KEY,
    account_id      bigint NOT NULL,
    auth_epoch      bigint NOT NULL,
    status          varchar(16) DEFAULT 'PENDING' NOT NULL,
    attempts        integer DEFAULT 0 NOT NULL,
    available_at    timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    lease_until     timestamp with time zone,
    last_error_code varchar(64),
    completed_at    timestamp with time zone,
    gmt_create      timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified    timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user     bigint,
    update_user     bigint,
    is_deleted      boolean DEFAULT false NOT NULL,
    delete_user     bigint,
    gmt_deleted     timestamp with time zone
);

ALTER TABLE vault_session_revocation_outbox
    ADD UNIQUE (account_id, auth_epoch);

CREATE INDEX idx_vault_revocation_ready
    ON vault_session_revocation_outbox (available_at, id)
    WHERE status = 'PENDING';

CREATE INDEX idx_vault_revocation_lease
    ON vault_session_revocation_outbox (lease_until, id)
    WHERE status = 'PROCESSING';

ALTER TABLE vault_session_revocation_outbox
    ADD CONSTRAINT vault_session_revocation_outbox_id_check CHECK (
        id > 0
    );

ALTER TABLE vault_session_revocation_outbox
    ADD CONSTRAINT vault_session_revocation_outbox_auth_epoch_check CHECK (
        auth_epoch BETWEEN 1 AND 9007199254740991
    );

ALTER TABLE vault_session_revocation_outbox
    ADD CONSTRAINT vault_session_revocation_outbox_status_check CHECK (
        status IN ('PENDING', 'PROCESSING', 'DONE')
    );

ALTER TABLE vault_session_revocation_outbox
    ADD CONSTRAINT vault_session_revocation_outbox_attempts_check CHECK (
        attempts >= 0
    );

ALTER TABLE vault_session_revocation_outbox
    ADD CONSTRAINT ck_revocation_lease CHECK (
        (status = 'PROCESSING' AND lease_until IS NOT NULL)
        OR (status <> 'PROCESSING' AND lease_until IS NULL)
    );

ALTER TABLE vault_session_revocation_outbox
    ADD CONSTRAINT ck_revocation_completed CHECK (
        (status = 'DONE' AND completed_at IS NOT NULL)
        OR (status <> 'DONE' AND completed_at IS NULL)
    );

ALTER TABLE vault_session_revocation_outbox
    ADD CONSTRAINT ck_vault_session_revocation_outbox_delete_audit CHECK (
        (NOT is_deleted AND delete_user IS NULL AND gmt_deleted IS NULL)
        OR (is_deleted AND gmt_deleted IS NOT NULL)
    );

ALTER TABLE vault_session_revocation_outbox
    ADD CONSTRAINT ck_vault_session_revocation_outbox_delete_state CHECK (
        NOT is_deleted OR status = 'DONE'
    );

COMMENT ON TABLE vault_session_revocation_outbox IS '数据库事务内产生的 Redis 旧会话撤销任务；认证代次是立即生效的授权依据';

COMMENT ON COLUMN vault_session_revocation_outbox.id IS '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';
COMMENT ON COLUMN vault_session_revocation_outbox.account_id IS '待清理会话的认证账户';
COMMENT ON COLUMN vault_session_revocation_outbox.auth_epoch IS '清理会话代次小于该值的会话，不能删除之后新建的会话';
COMMENT ON COLUMN vault_session_revocation_outbox.status IS 'PENDING=待重试，PROCESSING=持有租约，DONE=已完成';
COMMENT ON COLUMN vault_session_revocation_outbox.attempts IS '处理次数';
COMMENT ON COLUMN vault_session_revocation_outbox.available_at IS '下一次可领取时间，失败按退避策略重试';
COMMENT ON COLUMN vault_session_revocation_outbox.lease_until IS '工作线程租约截止，崩溃后可重新领取';
COMMENT ON COLUMN vault_session_revocation_outbox.last_error_code IS '服务端固定错误码，不记录异常正文或 Session ID';
COMMENT ON COLUMN vault_session_revocation_outbox.completed_at IS '完成清理时间';
COMMENT ON COLUMN vault_session_revocation_outbox.gmt_create IS '创建时间';
COMMENT ON COLUMN vault_session_revocation_outbox.gmt_modified IS '更新时间，由应用更新，DEFAULT 不会自动更新';
COMMENT ON COLUMN vault_session_revocation_outbox.create_user IS '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';
COMMENT ON COLUMN vault_session_revocation_outbox.update_user IS '最近更新人系统用户 ID，由服务端填充；系统任务可为空';
COMMENT ON COLUMN vault_session_revocation_outbox.is_deleted IS '逻辑删除标记，false=有效、true=已删除';
COMMENT ON COLUMN vault_session_revocation_outbox.delete_user IS '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';
COMMENT ON COLUMN vault_session_revocation_outbox.gmt_deleted IS '逻辑删除时间，删除时与标记及删除人一起写入';

CREATE TABLE vault_audit_event (
    id           bigint NOT NULL PRIMARY KEY,
    account_id   bigint,
    vault_id     bigint,
    action       varchar(32) NOT NULL,
    result       varchar(16) NOT NULL,
    error_code   varchar(64),
    request_id   uuid NOT NULL,
    gmt_create   timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean DEFAULT false NOT NULL,
    delete_user  bigint,
    gmt_deleted  timestamp with time zone
);

CREATE INDEX idx_vault_audit_account_time
    ON vault_audit_event (account_id asc, gmt_create desc, id asc);

CREATE INDEX idx_vault_audit_vault_time
    ON vault_audit_event (vault_id asc, gmt_create desc, id asc);

ALTER TABLE vault_audit_event
    ADD CONSTRAINT vault_audit_event_id_check CHECK (
        id > 0
    );

ALTER TABLE vault_audit_event
    ADD CONSTRAINT vault_audit_event_action_check CHECK (
        action IN (
            'LOGIN', 'LOGOUT', 'PASSKEY_REGISTER', 'PASSKEY_AUTHENTICATE', 'PASSKEY_REVOKE',
            'MFA_RECOVERY', 'RECOVERY_CODES_ISSUE', 'RECOVERY_CODES_CONFIRM', 'INITIALIZE',
            'ITEM_CREATE', 'ITEM_UPDATE', 'ITEM_DELETE', 'ITEM_RESTORE', 'ITEM_PURGE',
            'CHANGE_MASTER', 'ROTATION_START', 'ROTATION_COMMIT', 'ROTATION_CANCEL',
            'ROTATION_RESUME', 'ROTATION_EXPIRE', 'RESTORE_START', 'BACKUP_DOWNLOAD', 'SESSION_REJECT'
        )
    );

ALTER TABLE vault_audit_event
    ADD CONSTRAINT vault_audit_event_result_check CHECK (
        result IN ('SUCCESS', 'DENIED', 'FAILURE')
    );

ALTER TABLE vault_audit_event
    ADD CONSTRAINT ck_vault_audit_event_delete_audit CHECK (
        (NOT is_deleted AND delete_user IS NULL AND gmt_deleted IS NULL)
        OR (is_deleted AND gmt_deleted IS NOT NULL)
    );

COMMENT ON TABLE vault_audit_event IS '认证、密文下载和写入审计；不记录请求正文、条目内容、恢复码或密钥文件';

COMMENT ON COLUMN vault_audit_event.id IS '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';
COMMENT ON COLUMN vault_audit_event.account_id IS '可识别时的认证账户；未知账户登录失败可以为空';
COMMENT ON COLUMN vault_audit_event.vault_id IS '所属密码库；初始化前的认证事件可以为空';
COMMENT ON COLUMN vault_audit_event.action IS '固定审计动作；不允许客户端指定任意动作字符串';
COMMENT ON COLUMN vault_audit_event.result IS '审计结果';
COMMENT ON COLUMN vault_audit_event.error_code IS '服务端固定机器错误码；不拼接用户输入';
COMMENT ON COLUMN vault_audit_event.request_id IS '服务端生成的请求追踪 ID';
COMMENT ON COLUMN vault_audit_event.gmt_create IS '创建时间';
COMMENT ON COLUMN vault_audit_event.gmt_modified IS '更新时间，由应用更新，DEFAULT 不会自动更新';
COMMENT ON COLUMN vault_audit_event.create_user IS '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';
COMMENT ON COLUMN vault_audit_event.update_user IS '最近更新人系统用户 ID，由服务端填充；系统任务可为空';
COMMENT ON COLUMN vault_audit_event.is_deleted IS '逻辑删除标记，false=有效、true=已删除';
COMMENT ON COLUMN vault_audit_event.delete_user IS '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';
COMMENT ON COLUMN vault_audit_event.gmt_deleted IS '逻辑删除时间，删除时与标记及删除人一起写入';

CREATE TABLE vault_site (
    id           bigint NOT NULL PRIMARY KEY,
    gmt_create   timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean DEFAULT false NOT NULL,
    status       smallint DEFAULT 1 NOT NULL,
    site_name    varchar(128) NOT NULL,
    site_domain  varchar(256) NOT NULL,
    sort_order   integer DEFAULT 0
);

CREATE UNIQUE INDEX uk_vs_sd
    ON vault_site (site_domain)
    WHERE is_deleted = false;

ALTER TABLE vault_site
    ADD CONSTRAINT ck_vault_site_status CHECK (
        status IN (0, 1)
    );

COMMENT ON TABLE vault_site IS '密码库站点';

COMMENT ON COLUMN vault_site.id IS '主键';
COMMENT ON COLUMN vault_site.gmt_create IS '创建时间';
COMMENT ON COLUMN vault_site.gmt_modified IS '更新时间';
COMMENT ON COLUMN vault_site.create_user IS '创建者';
COMMENT ON COLUMN vault_site.update_user IS '更新者';
COMMENT ON COLUMN vault_site.is_deleted IS '是否删除(false:否; true:是)';
COMMENT ON COLUMN vault_site.status IS '状态(1:正常; 0:禁用)';
COMMENT ON COLUMN vault_site.site_name IS '站点名称';
COMMENT ON COLUMN vault_site.site_domain IS '站点域名';
COMMENT ON COLUMN vault_site.sort_order IS '排序';

-- 商品管理（对应 20261119_product_management.sql）
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

-- 报价单（对应 20261122_quotation.sql）
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
COMMENT ON COLUMN quotation.customer_name IS '本单客户名称';
COMMENT ON COLUMN quotation.contact_name IS '本单客户联系人';
COMMENT ON COLUMN quotation.contact_phone IS '本单客户联系电话';
COMMENT ON COLUMN quotation.contact_email IS '本单客户联络电子邮件';
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
