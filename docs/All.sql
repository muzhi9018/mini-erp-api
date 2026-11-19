create table sys_user
(
    id                bigint                                             not null
        primary key,
    gmt_create        timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified      timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user       bigint,
    update_user       bigint,
    is_deleted        boolean                  default false             not null,
    status            smallint                 default 1                 not null
        constraint ck_sys_user_status
            check (status = ANY (ARRAY [0, 1])),
    username          varchar(32)                                        not null,
    password          varchar(256)                                       not null,
    user_no           varchar(32),
    current_role_code varchar(32),
    real_name         varchar(64),
    mobile            varchar(32),
    remark            varchar(128),
    sort              integer                  default 0
);

comment on table sys_user is '系统用户信息';

comment on column sys_user.id is '主键';

comment on column sys_user.gmt_create is '创建时间';

comment on column sys_user.gmt_modified is '更新时间';

comment on column sys_user.create_user is '创建者';

comment on column sys_user.update_user is '更新者';

comment on column sys_user.is_deleted is '是否删除(false:否; true:是)';

comment on column sys_user.status is '状态(1:正常; 0:禁用)';

comment on column sys_user.username is '用户名';

comment on column sys_user.password is '密码';

comment on column sys_user.user_no is '用户编号';

comment on column sys_user.current_role_code is '当前使用角色';

comment on column sys_user.real_name is '用户昵称';

comment on column sys_user.mobile is '手机号';

comment on column sys_user.remark is '备注';

comment on column sys_user.sort is '排序';


create unique index uk_su_username
    on sys_user (username)
    where (is_deleted = false);

create table sys_role_permission
(
    id              bigint      not null
        primary key,
    gmt_create      timestamp with time zone default CURRENT_TIMESTAMP,
    gmt_modified    timestamp with time zone default CURRENT_TIMESTAMP,
    create_user     bigint,
    update_user     bigint,
    role_id         bigint      not null,
    menu_id         bigint      not null,
    role_code       varchar(32) not null,
    permission_code varchar(64) not null
);

comment on table sys_role_permission is '角色权限关联表';

comment on column sys_role_permission.id is '主键id';

comment on column sys_role_permission.gmt_create is '创建时间';

comment on column sys_role_permission.gmt_modified is '更新时间';

comment on column sys_role_permission.create_user is '创建者';

comment on column sys_role_permission.update_user is '更新者';

comment on column sys_role_permission.role_id is '角色id';

comment on column sys_role_permission.menu_id is '菜单id';

comment on column sys_role_permission.role_code is '角色编码';

comment on column sys_role_permission.permission_code is '权限编码';


create unique index uk_sys_role_permission_role_menu
    on sys_role_permission (role_id, menu_id);

create index idx_sys_role_permission_role_code
    on sys_role_permission (role_code);

create index idx_sys_role_permission_permission_code
    on sys_role_permission (permission_code);

create table sys_menu
(
    id              bigint                                 not null
        primary key,
    parent_id       bigint                   default 0     not null,
    hierarchy       varchar(1024),
    client_id       varchar(32),
    type            smallint                               not null
        constraint ck_sys_menu_type
            check (type = ANY (ARRAY [1, 2, 3, 4])),
    name            varchar(32)                            not null,
    i18n_code       varchar(128),
    path            varchar(128),
    component       varchar(128),
    icon            varchar(128),
    permission_code varchar(64),
    sort            integer                  default 0     not null,
    gmt_create      timestamp with time zone default CURRENT_TIMESTAMP,
    gmt_modified    timestamp with time zone default CURRENT_TIMESTAMP,
    create_user     bigint,
    update_user     bigint,
    is_deleted      boolean                  default false not null
);

comment on table sys_menu is '系统菜单';

comment on column sys_menu.id is 'id';

comment on column sys_menu.parent_id is '父级id（顶级菜单为 0）';

comment on column sys_menu.hierarchy is '层级（记录级联的id层级）';

comment on column sys_menu.client_id is '所属客户端';

comment on column sys_menu.type is '1->目录; 2->菜单; 3->按钮; 4->接口';

comment on column sys_menu.name is '菜单名称';

comment on column sys_menu.i18n_code is '国际化编码';

comment on column sys_menu.path is '路径';

comment on column sys_menu.component is '组件';

comment on column sys_menu.icon is '图标';

comment on column sys_menu.permission_code is '权限码';

comment on column sys_menu.sort is '排序';

comment on column sys_menu.gmt_create is '创建时间';

comment on column sys_menu.gmt_modified is '更新时间';

comment on column sys_menu.create_user is '创建者';

comment on column sys_menu.update_user is '更新者';

comment on column sys_menu.is_deleted is '是否删除';


create index idx_sys_menu_parent_id
    on sys_menu (parent_id);

create table sys_role
(
    id           bigint                                 not null
        primary key,
    gmt_create   timestamp with time zone default CURRENT_TIMESTAMP,
    gmt_modified timestamp with time zone default CURRENT_TIMESTAMP,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean                  default false not null,
    status       smallint                 default 1     not null
        constraint ck_sys_role_status
            check (status = ANY (ARRAY [0, 1])),
    sort         integer                  default 0     not null,
    role_name    varchar(32)                            not null,
    role_code    varchar(32)                            not null,
    remark       varchar(128)
);

comment on table sys_role is '系统角色信息';

comment on column sys_role.id is 'id';

comment on column sys_role.gmt_create is '创建时间';

comment on column sys_role.gmt_modified is '更新时间';

comment on column sys_role.create_user is '创建者';

comment on column sys_role.update_user is '更新者';

comment on column sys_role.is_deleted is '是否删除';

comment on column sys_role.status is '状态（1:正常; 0:禁用）';

comment on column sys_role.sort is '排序';

comment on column sys_role.role_name is '角色名称';

comment on column sys_role.role_code is '角色编码';

comment on column sys_role.remark is '备注';


create unique index uk_sys_role_role_code_active
    on sys_role (role_code)
    where (is_deleted = false);

create table sys_user_role
(
    id           bigint                                 not null
        primary key,
    gmt_create   timestamp with time zone default CURRENT_TIMESTAMP,
    gmt_modified timestamp with time zone default CURRENT_TIMESTAMP,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean                  default false not null,
    status       smallint                 default 1     not null
        constraint ck_sys_user_role_status
            check (status = ANY (ARRAY [0, 1])),
    sort         integer                  default 0     not null,
    user_id      bigint                                 not null,
    role_id      bigint                                 not null
);

comment on table sys_user_role is '用户角色关联表';

comment on column sys_user_role.id is 'id';

comment on column sys_user_role.gmt_create is '创建时间';

comment on column sys_user_role.gmt_modified is '更新时间';

comment on column sys_user_role.create_user is '创建者';

comment on column sys_user_role.update_user is '更新者';

comment on column sys_user_role.is_deleted is '是否删除(0:否; 1:是)';

comment on column sys_user_role.status is '状态（1:正常; 0:禁用）';

comment on column sys_user_role.sort is '排序';

comment on column sys_user_role.user_id is '用户id';

comment on column sys_user_role.role_id is '角色id';


create unique index uk_sys_user_role_active
    on sys_user_role (user_id, role_id)
    where (is_deleted = false);

create index idx_sys_user_role_user_id
    on sys_user_role (user_id);

create index idx_sys_user_role_role_id
    on sys_user_role (role_id);

create table locale
(
    id          bigint                not null
        primary key,
    code        varchar(20),
    name        varchar(100)          not null,
    native_name varchar(100)          not null,
    enabled     boolean default true  not null,
    is_default  boolean default false not null,
    is_deleted  boolean default false not null,
    sort_order  integer default 0     not null
);

comment on table locale is '国际化语言';

comment on column locale.id is 'id';

comment on column locale.code is '国际化编码';

comment on column locale.native_name is '本地名称';

comment on column locale.is_default is '是否默认语言';

comment on column locale.sort_order is '排序';


create unique index uk_locale_code
    on locale (code)
    where (is_deleted = false);

create table website_product_category
(
    id           bigint                                             not null
        primary key,
    gmt_create   timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean                  default false             not null,
    code         varchar(128)                                       not null,
    sort_order   integer                  default 0                 not null,
    is_show      boolean                  default true              not null
);

comment on table website_product_category is '官网商品分类';

comment on column website_product_category.id is '主键';

comment on column website_product_category.gmt_create is '创建时间';

comment on column website_product_category.gmt_modified is '更新时间';

comment on column website_product_category.create_user is '创建用户 id';

comment on column website_product_category.update_user is '更新用户 id';

comment on column website_product_category.is_deleted is '是否已逻辑删除';

comment on column website_product_category.code is '稳定的分类编码';

comment on column website_product_category.sort_order is '官网展示顺序，数值越小越靠前';

comment on column website_product_category.is_show is '是否在官网展示';


create unique index uk_wpc_code
    on website_product_category (code)
    where (is_deleted = false);

create table website_product_category_i18n
(
    id                          bigint                                             not null
        primary key,
    gmt_create                  timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified                timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user                 bigint,
    update_user                 bigint,
    is_deleted                  boolean                  default false             not null,
    website_product_category_id bigint                                             not null,
    locale                      varchar(32)                                        not null,
    name                        varchar(256)                                       not null,
    description                 text,
    image_attachment_id         bigint
);

comment on table website_product_category_i18n is '官网商品分类国际化';

comment on column website_product_category_i18n.id is '主键';

comment on column website_product_category_i18n.gmt_create is '创建时间';

comment on column website_product_category_i18n.gmt_modified is '更新时间';

comment on column website_product_category_i18n.create_user is '创建用户 id';

comment on column website_product_category_i18n.update_user is '更新用户 id';

comment on column website_product_category_i18n.is_deleted is '是否已逻辑删除';

comment on column website_product_category_i18n.website_product_category_id is '商品分类 id（逻辑关联）';

comment on column website_product_category_i18n.locale is '语言标识，例如 zh-CN、en-US';

comment on column website_product_category_i18n.name is '分类名称';

comment on column website_product_category_i18n.description is '分类描述';

comment on column website_product_category_i18n.image_attachment_id is '分类图片附件id';


create unique index uk_wpci_category_locale
    on website_product_category_i18n (website_product_category_id, locale)
    where (is_deleted = false);

create table website_product
(
    id             bigint                                             not null
        primary key,
    gmt_create     timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified   timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user    bigint,
    update_user    bigint,
    is_deleted     boolean                  default false             not null,
    category_id    bigint                                             not null,
    slug           varchar(128)                                       not null,
    sort_order     integer                  default 0                 not null,
    is_show        boolean                  default true              not null,
    is_recommended boolean                  default false             not null
);

comment on table website_product is '官网商品';

comment on column website_product.id is '主键';

comment on column website_product.gmt_create is '创建时间';

comment on column website_product.gmt_modified is '更新时间';

comment on column website_product.create_user is '创建用户 id';

comment on column website_product.update_user is '更新用户 id';

comment on column website_product.is_deleted is '是否已逻辑删除';

comment on column website_product.category_id is '商品分类 id（逻辑关联）';

comment on column website_product.slug is '官网详情页路由标识；创建后不应随意修改';

comment on column website_product.sort_order is '官网展示顺序，数值越小越靠前';

comment on column website_product.is_show is '是否在官网展示';

comment on column website_product.is_recommended is '是否用于全局推荐展示位';


create unique index uk_wp_slug
    on website_product (slug)
    where (is_deleted = false);

create index idx_wp_category_display
    on website_product (category_id, sort_order)
    where ((is_deleted = false) AND (is_show = true));

create table website_product_i18n
(
    id                          bigint                                             not null
        primary key,
    gmt_create                  timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified                timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user                 bigint,
    update_user                 bigint,
    is_deleted                  boolean                  default false             not null,
    product_id                  bigint                                             not null,
    locale                      varchar(32)                                        not null,
    name                        varchar(128)                                       not null,
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

comment on table website_product_i18n is '官网商品国际化';

comment on column website_product_i18n.id is '主键';

comment on column website_product_i18n.gmt_create is '创建时间';

comment on column website_product_i18n.gmt_modified is '更新时间';

comment on column website_product_i18n.create_user is '创建用户 id';

comment on column website_product_i18n.update_user is '更新用户 id';

comment on column website_product_i18n.is_deleted is '是否已逻辑删除';

comment on column website_product_i18n.product_id is '商品 id（逻辑关联）';

comment on column website_product_i18n.locale is '语言标识，例如 zh-CN、en-US';

comment on column website_product_i18n.name is '商品名称';

comment on column website_product_i18n.subtitle is '商品卡片的次级名称，例如英文名称';

comment on column website_product_i18n.summary is '商品卡片摘要';

comment on column website_product_i18n.tagline is '商品详情页 Hero 文案';

comment on column website_product_i18n.feature_introduction is '商品特点区域介绍文案';

comment on column website_product_i18n.specification_introduction is '技术参数区域介绍文案';

comment on column website_product_i18n.application_introduction is '应用场景区域介绍文案';

comment on column website_product_i18n.case_introduction is '案例展示区域介绍文案';

comment on column website_product_i18n.cover_image_attachment_id is ' 商品卡片封面图片附件 id';

comment on column website_product_i18n.feature_image_attachment_id is '商品特点区域图片附件 id';


create unique index uk_wpi_product_locale
    on website_product_i18n (product_id, locale)
    where (is_deleted = false);

create table website_product_detail_item
(
    id              bigint                                             not null
        primary key,
    gmt_create      timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified    timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user     bigint,
    update_user     bigint,
    is_deleted      boolean                  default false             not null,
    product_i18n_id bigint                                             not null,
    item_type       varchar(32)                                        not null
        constraint ck_wpdi_item_type
            check ((item_type)::text = ANY
        (ARRAY [('FEATURE'::character varying)::text, ('SPECIFICATION'::character varying)::text])),
    title           varchar(256)                                       not null,
    content         text                                               not null,
    sort_order      integer                  default 0                 not null,
    is_show         boolean                  default true              not null
);

comment on table website_product_detail_item is '官网商品文本明细（特点、技术参数）';

comment on column website_product_detail_item.id is '主键';

comment on column website_product_detail_item.gmt_create is '创建时间';

comment on column website_product_detail_item.gmt_modified is '更新时间';

comment on column website_product_detail_item.create_user is '创建用户 id';

comment on column website_product_detail_item.update_user is '更新用户 id';

comment on column website_product_detail_item.is_deleted is '是否已逻辑删除';

comment on column website_product_detail_item.product_i18n_id is '商品国际化 id（逻辑关联）';

comment on column website_product_detail_item.item_type is '明细类型：FEATURE=特点，SPECIFICATION=技术参数';

comment on column website_product_detail_item.title is '特点标题或参数名称';

comment on column website_product_detail_item.content is '特点描述或参数值';

comment on column website_product_detail_item.sort_order is '同类型内容展示顺序，数值越小越靠前';

comment on column website_product_detail_item.is_show is '是否在官网展示';


create unique index uk_wpdi_i18n_type_sort
    on website_product_detail_item (product_i18n_id, item_type, sort_order)
    where (is_deleted = false);

create index idx_wpdi_display
    on website_product_detail_item (product_i18n_id, item_type, sort_order)
    where ((is_deleted = false) AND (is_show = true));

create table website_product_media_item
(
    id                  bigint                                             not null
        primary key,
    gmt_create          timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified        timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user         bigint,
    update_user         bigint,
    is_deleted          boolean                  default false             not null,
    product_i18n_id     bigint                                             not null,
    item_type           varchar(32)                                        not null
        constraint ck_wpmi_item_type
            check ((item_type)::text = ANY
        (ARRAY [('APPLICATION'::character varying)::text, ('CASE'::character varying)::text, ('CAROUSEL_IMAGE'::character varying)::text, ('DETAIL_IMAGE'::character varying)::text])),
    title               varchar(256)                                       not null,
    description         text,
    image_attachment_id bigint                                             not null,
    sort_order          integer                  default 0                 not null,
    is_show             boolean                  default true              not null
);

comment on table website_product_media_item is '官网商品媒体明细（应用场景、案例展示）';

comment on column website_product_media_item.id is '主键';

comment on column website_product_media_item.gmt_create is '创建时间';

comment on column website_product_media_item.gmt_modified is '更新时间';

comment on column website_product_media_item.create_user is '创建用户 id';

comment on column website_product_media_item.update_user is '更新用户 id';

comment on column website_product_media_item.is_deleted is '是否已逻辑删除';

comment on column website_product_media_item.product_i18n_id is '商品国际化 id（逻辑关联）';

comment on column website_product_media_item.item_type is '明细类型：APPLICATION=应用场景，CASE=案例展示, CAROUSEL_IMAGE=商品轮播图, DETAIL_IMAGE=商品详情图';

comment on column website_product_media_item.title is '应用场景或案例标题';

comment on column website_product_media_item.description is '应用场景或案例描述';

comment on column website_product_media_item.image_attachment_id is '应用场景或案例图片附件 ID';

comment on column website_product_media_item.sort_order is '同类型内容展示顺序，数值越小越靠前';

comment on column website_product_media_item.is_show is '是否在官网展示';


create unique index uk_wpmi_i18n_type_sort
    on website_product_media_item (product_i18n_id, item_type, sort_order)
    where (is_deleted = false);

create index idx_wpmi_display
    on website_product_media_item (product_i18n_id, item_type, sort_order)
    where ((is_deleted = false) AND (is_show = true));

create table website_product_recommend
(
    id                   bigint                                             not null
        primary key,
    gmt_create           timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified         timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user          bigint,
    update_user          bigint,
    is_deleted           boolean                  default false             not null,
    product_id           bigint                                             not null,
    recommend_product_id bigint                                             not null,
    sort_order           integer                  default 0                 not null,
    constraint ck_wpr_not_self_recommend
        check (product_id <> recommend_product_id)
);

comment on table website_product_recommend is '官网商品详情页定向推荐关联表';

comment on column website_product_recommend.id is '主键';

comment on column website_product_recommend.gmt_create is '创建时间';

comment on column website_product_recommend.gmt_modified is '更新时间';

comment on column website_product_recommend.create_user is '创建用户 id';

comment on column website_product_recommend.update_user is '更新用户 id';

comment on column website_product_recommend.is_deleted is '是否已逻辑删除';

comment on column website_product_recommend.product_id is '当前详情页商品 id';

comment on column website_product_recommend.recommend_product_id is '被推荐商品 id';

comment on column website_product_recommend.sort_order is '推荐商品展示顺序，数值越小越靠前';


create unique index uk_wpr_product_recommend
    on website_product_recommend (product_id, recommend_product_id)
    where (is_deleted = false);

create index idx_wpr_display
    on website_product_recommend (product_id, sort_order)
    where (is_deleted = false);

create table attachment
(
    id            bigint                                                        not null
        primary key,
    storage_type  varchar(32)              default 'rustfs'::character varying  not null,
    bucket_name   varchar(128)                                                  not null,
    object_key    varchar(1024)                                                 not null,
    original_name varchar(255)                                                  not null,
    content_type  varchar(128),
    file_size     bigint                                                        not null,
    etag          varchar(128),
    access_type   varchar(16)              default 'private'::character varying not null
        constraint ck_attachment_access_type
            check ((access_type)::text = ANY
                   (ARRAY [('public'::character varying)::text, ('private'::character varying)::text])),
    gmt_create    timestamp with time zone default CURRENT_TIMESTAMP            not null,
    gmt_modified  timestamp with time zone default CURRENT_TIMESTAMP            not null,
    update_user   bigint,
    create_user   bigint,
    is_deleted    boolean                  default false                        not null,
    constraint uk_attachment_object
        unique (storage_type, bucket_name, object_key)
);

comment on table attachment is '附件信息表，用于记录上传到对象存储中的文件信息';

comment on column attachment.id is '附件id，业务主键';

comment on column attachment.storage_type is '存储类型，例如：rustfs、s3、oss、cos';

comment on column attachment.bucket_name is '对象存储桶名称，例如：mini-erp';

comment on column attachment.object_key is '对象在存储桶中的唯一key，例如：product/2026/09/abc.png';

comment on column attachment.original_name is '用户上传时的原始文件名称，例如：img_6232.png';

comment on column attachment.content_type is '文件mime类型，例如：image/png、image/jpeg、application/pdf';

comment on column attachment.file_size is '文件大小，单位：字节（byte）';

comment on column attachment.etag is '对象存储返回的etag，用于标识对象版本或内容特征，不建议作为严格文件哈希使用';

comment on column attachment.access_type is '访问类型：public-公开访问，private-私有访问；私有附件需要生成预签名url访问';

comment on column attachment.gmt_create is '创建时间';

comment on column attachment.gmt_modified is '最后修改时间';

comment on column attachment.update_user is '最后修改人id';

comment on column attachment.create_user is '创建人id';

comment on column attachment.is_deleted is '逻辑删除标记：false-未删除，true-已删除';


create index idx_attachment_bucket_name
    on attachment (bucket_name);

comment on index idx_attachment_bucket_name is '附件存储桶名称索引';

create index idx_attachment_created_at
    on attachment (gmt_create desc);

comment on index idx_attachment_created_at is '附件创建时间倒序索引';

create table vault_account
(
    id                       bigint                                             not null
        unique
        constraint vault_account_id_check
            check (id > 0),
    owner_id                 bigint                                             not null
        unique,
    webauthn_user_handle     bytea                                              not null
        unique
        constraint vault_account_webauthn_user_handle_check
            check (octet_length(webauthn_user_handle) = 32),
    auth_epoch               bigint                   default 1                 not null
        constraint vault_account_auth_epoch_check
            check ((auth_epoch >= 1) AND (auth_epoch <= '9007199254740991'::bigint)),
    credential_revision      bigint                   default 1                 not null
        constraint vault_account_credential_revision_check
            check ((credential_revision >= 1) AND (credential_revision <= '9007199254740991'::bigint)),
    recovery_required        boolean                  default true              not null,
    active_recovery_batch_id uuid,
    gmt_create               timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified             timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user              bigint,
    update_user              bigint,
    is_deleted               boolean                  default false             not null,
    delete_user              bigint,
    gmt_deleted              timestamp with time zone,
    constraint ck_account_recovery_ready
        check (recovery_required OR (active_recovery_batch_id IS NOT NULL)),
    constraint ck_vault_account_delete_audit
        check (((NOT is_deleted) AND (delete_user IS NULL) AND (gmt_deleted IS NULL)) OR
               (is_deleted AND (gmt_deleted IS NOT NULL)))
);

comment on table vault_account is '密码库独立认证账户；不保存主密码、账户密钥、恢复密钥或条目明文';

comment on column vault_account.id is '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';

comment on column vault_account.owner_id is '现有不可变系统用户 ID；API ownerSubject 为其规范十进制字符串';

comment on column vault_account.webauthn_user_handle is '随机 WebAuthn 用户句柄，固定 32 字节，不使用用户名或邮箱';

comment on column vault_account.auth_epoch is '认证代次；请求必须与会话代次相等，敏感变更在数据库事务内递增';

comment on column vault_account.credential_revision is '凭据集合版本，用于新增、撤销和恢复操作的并发检查';

comment on column vault_account.recovery_required is '首次绑定或身份恢复后，未确认新一批 MFA 恢复码时禁止数据接口';

comment on column vault_account.active_recovery_batch_id is '当前有效 MFA 恢复码批次；未确认的批次不能用于恢复';

comment on column vault_account.gmt_create is '创建时间';

comment on column vault_account.gmt_modified is '更新时间，由应用更新，DEFAULT 不会自动更新';

comment on column vault_account.create_user is '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';

comment on column vault_account.update_user is '最近更新人系统用户 ID，由服务端填充；系统任务可为空';

comment on column vault_account.is_deleted is '逻辑删除标记，false=有效、true=已删除';

comment on column vault_account.delete_user is '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';

comment on column vault_account.gmt_deleted is '逻辑删除时间，删除时与标记及删除人一起写入';


create table vault_passkey
(
    id              bigint                                             not null
        unique
        constraint vault_passkey_id_check
            check (id > 0),
    account_id      bigint                                             not null,
    credential_id   bytea                                              not null
        unique
        constraint vault_passkey_credential_id_check
            check ((octet_length(credential_id) >= 1) AND (octet_length(credential_id) <= 1024)),
    public_key_cose bytea                                              not null
        constraint vault_passkey_public_key_cose_check
            check ((octet_length(public_key_cose) >= 1) AND (octet_length(public_key_cose) <= 4096)),
    signature_count bigint                   default 0                 not null
        constraint vault_passkey_signature_count_check
            check ((signature_count >= 0) AND (signature_count <= '4294967295'::bigint)),
    transports      text[]                   default '{}'::text[]      not null
        constraint vault_passkey_transports_check
            check (transports <@
                   ARRAY ['usb'::text, 'nfc'::text, 'ble'::text, 'internal'::text, 'hybrid'::text, 'smart-card'::text]),
    aaguid          uuid                                               not null,
    backup_eligible boolean                                            not null,
    backup_state    boolean                                            not null,
    label           varchar(128)                                       not null
        constraint vault_passkey_label_check
            check ((char_length((label)::text) >= 1) AND (char_length((label)::text) <= 128)),
    last_used_at    timestamp with time zone,
    revoked_at      timestamp with time zone,
    gmt_create      timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified    timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user     bigint,
    update_user     bigint,
    is_deleted      boolean                  default false             not null,
    delete_user     bigint,
    gmt_deleted     timestamp with time zone,
    constraint ck_passkey_backup
        check ((NOT backup_state) OR backup_eligible),
    constraint ck_vault_passkey_delete_audit
        check (((NOT is_deleted) AND (delete_user IS NULL) AND (gmt_deleted IS NULL)) OR
               (is_deleted AND (gmt_deleted IS NOT NULL))),
    constraint ck_vault_passkey_delete_state
        check ((NOT is_deleted) OR (revoked_at IS NOT NULL))
);

comment on table vault_passkey is 'WebAuthn 凭据；保存公钥和验证元数据，不保存认证器私钥';

comment on column vault_passkey.id is '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';

comment on column vault_passkey.account_id is '所属密码库认证账户';

comment on column vault_passkey.credential_id is 'WebAuthn 凭据 ID；HTTP 中使用无填充 Base64url';

comment on column vault_passkey.public_key_cose is '验证库输出的 COSE 公钥';

comment on column vault_passkey.signature_count is '签名计数，由 WebAuthn 库按认证器类型执行检查，不自行要求每次必增';

comment on column vault_passkey.transports is '认证器传输提示；不作为认证成功依据';

comment on column vault_passkey.aaguid is '认证器型号标识，由已验证的注册结果取得';

comment on column vault_passkey.backup_eligible is '凭据是否允许同步或备份';

comment on column vault_passkey.backup_state is '凭据是否已备份；不能在不支持备份时为真';

comment on column vault_passkey.label is '用户可见名称，仅作为纯文本展示';

comment on column vault_passkey.last_used_at is '最近一次成功认证时间';

comment on column vault_passkey.revoked_at is '撤销时间；被撤销的凭据不得用于认证';

comment on column vault_passkey.gmt_create is '创建时间';

comment on column vault_passkey.gmt_modified is '更新时间，由应用更新，DEFAULT 不会自动更新';

comment on column vault_passkey.create_user is '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';

comment on column vault_passkey.update_user is '最近更新人系统用户 ID，由服务端填充；系统任务可为空';

comment on column vault_passkey.is_deleted is '逻辑删除标记，false=有效、true=已删除';

comment on column vault_passkey.delete_user is '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';

comment on column vault_passkey.gmt_deleted is '逻辑删除时间，删除时与标记及删除人一起写入';


create index idx_vault_passkey_account
    on vault_passkey (account_id);

create index idx_vault_passkey_active
    on vault_passkey (account_id, id)
    where ((revoked_at IS NULL) AND (is_deleted = false));

create table vault_auth_recovery_code
(
    id           bigint                                             not null
        unique
        constraint vault_auth_recovery_code_id_check
            check (id > 0),
    account_id   bigint                                             not null,
    batch_id     uuid                                               not null,
    code_digest  bytea                                              not null
        unique
        constraint vault_auth_recovery_code_code_digest_check
            check (octet_length(code_digest) = 32),
    confirmed_at timestamp with time zone,
    consumed_at  timestamp with time zone,
    revoked_at   timestamp with time zone,
    gmt_create   timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean                  default false             not null,
    delete_user  bigint,
    gmt_deleted  timestamp with time zone,
    constraint ck_recovery_code_consumed
        check ((consumed_at IS NULL) OR (confirmed_at IS NOT NULL)),
    constraint ck_vault_auth_recovery_code_delete_audit
        check (((NOT is_deleted) AND (delete_user IS NULL) AND (gmt_deleted IS NULL)) OR
               (is_deleted AND (gmt_deleted IS NOT NULL))),
    constraint ck_vault_auth_recovery_code_delete_state
        check ((NOT is_deleted) OR (consumed_at IS NOT NULL) OR (revoked_at IS NOT NULL))
);

comment on table vault_auth_recovery_code is '仅用于重新绑定 Passkey 的一次性 MFA 恢复码摘要；与客户端解密恢复密钥 RK 完全分离';

comment on column vault_auth_recovery_code.id is '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';

comment on column vault_auth_recovery_code.account_id is '所属认证账户';

comment on column vault_auth_recovery_code.batch_id is '恢复码批次；只有账户 active_recovery_batch_id 指向的批次有效';

comment on column vault_auth_recovery_code.code_digest is '独立随机 32 字节恢复码的 SHA-256 摘要；明文仅生成时返回一次';

comment on column vault_auth_recovery_code.confirmed_at is '该批次经用户重新导入校验后的确认时间';

comment on column vault_auth_recovery_code.consumed_at is '单次原子消费时间';

comment on column vault_auth_recovery_code.revoked_at is '替换批次或安全操作后的撤销时间';

comment on column vault_auth_recovery_code.gmt_create is '创建时间';

comment on column vault_auth_recovery_code.gmt_modified is '更新时间，由应用更新，DEFAULT 不会自动更新';

comment on column vault_auth_recovery_code.create_user is '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';

comment on column vault_auth_recovery_code.update_user is '最近更新人系统用户 ID，由服务端填充；系统任务可为空';

comment on column vault_auth_recovery_code.is_deleted is '逻辑删除标记，false=有效、true=已删除';

comment on column vault_auth_recovery_code.delete_user is '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';

comment on column vault_auth_recovery_code.gmt_deleted is '逻辑删除时间，删除时与标记及删除人一起写入';


create index idx_vault_recovery_account_batch
    on vault_auth_recovery_code (account_id, batch_id);

create table vault_info
(
    id                  bigint                                             not null
        unique
        constraint vault_info_id_check
            check (id > 0),
    vault_id            uuid                                               not null
        unique,
    account_id          bigint                                             not null
        unique,
    status              varchar(16)                                        not null
        constraint vault_info_status_check
            check ((status)::text = ANY
        ((ARRAY ['EMPTY'::character varying, 'ACTIVE'::character varying, 'RESTORING'::character varying])::text[])),
    active_generation   bigint
        constraint vault_info_active_generation_check
            check ((active_generation >= 1) AND (active_generation <= '9007199254740991'::bigint)),
    meta_revision       bigint                   default 1                 not null
        constraint vault_info_meta_revision_check
            check ((meta_revision >= 1) AND (meta_revision <= '9007199254740991'::bigint)),
    collection_revision bigint                   default 1                 not null
        constraint vault_info_collection_revision_check
            check ((collection_revision >= 1) AND (collection_revision <= '9007199254740991'::bigint)),
    gmt_create          timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified        timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user         bigint,
    update_user         bigint,
    is_deleted          boolean                  default false             not null,
    delete_user         bigint,
    gmt_deleted         timestamp with time zone,
    constraint ck_vault_active_pointer
        check ((((status)::text = 'EMPTY'::text) AND (active_generation IS NULL)) OR
               (((status)::text = 'ACTIVE'::text) AND (active_generation IS NOT NULL)) OR
               ((status)::text = 'RESTORING'::text)),
    constraint ck_vault_info_delete_audit
        check (((NOT is_deleted) AND (delete_user IS NULL) AND (gmt_deleted IS NULL)) OR
               (is_deleted AND (gmt_deleted IS NOT NULL)))
);

comment on table vault_info is '个人密码库元数据；一个认证账户仅有一个库';

comment on column vault_info.id is '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';

comment on column vault_info.vault_id is '公开协议库 ID，由客户端生成随机 UUID，以规范小写字符串参与 AAD';

comment on column vault_info.account_id is '所属认证账户，所有归属条件从认证会话推导';

comment on column vault_info.status is 'EMPTY=未初始化占位，ACTIVE=可读取活动代，RESTORING=恢复暂存中';

comment on column vault_info.active_generation is '当前活动代；空库占位为 NULL，逻辑关联 vault_generation，由 Service 事务校验';

comment on column vault_info.meta_revision is '元数据与活动密钥槽版本';

comment on column vault_info.collection_revision is '当前代条目或密钥槽集合版本';

comment on column vault_info.gmt_create is '创建时间';

comment on column vault_info.gmt_modified is '更新时间，由应用更新，DEFAULT 不会自动更新';

comment on column vault_info.create_user is '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';

comment on column vault_info.update_user is '最近更新人系统用户 ID，由服务端填充；系统任务可为空';

comment on column vault_info.is_deleted is '逻辑删除标记，false=有效、true=已删除';

comment on column vault_info.delete_user is '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';

comment on column vault_info.gmt_deleted is '逻辑删除时间，删除时与标记及删除人一起写入';


create table vault_generation
(
    id           bigint                                             not null
        unique
        constraint vault_generation_id_check
            check (id > 0),
    vault_id     bigint                                             not null,
    generation   bigint                                             not null
        constraint vault_generation_generation_check
            check ((generation >= 1) AND (generation <= '9007199254740991'::bigint)),
    key_epoch    bigint                                             not null
        constraint vault_generation_key_epoch_check
            check ((key_epoch >= 1) AND (key_epoch <= '9007199254740991'::bigint)),
    status       varchar(16)                                        not null
        constraint vault_generation_status_check
            check ((status)::text = ANY
        ((ARRAY ['STAGING'::character varying, 'ACTIVE'::character varying, 'RETIRED'::character varying, 'ABORTED'::character varying])::text[])),
    gmt_create   timestamp with time zone default CURRENT_TIMESTAMP not null,
    retired_at   timestamp with time zone,
    gmt_modified timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean                  default false             not null,
    delete_user  bigint,
    gmt_deleted  timestamp with time zone,
    unique (vault_id, generation),
    constraint ck_vault_generation_delete_audit
        check (((NOT is_deleted) AND (delete_user IS NULL) AND (gmt_deleted IS NULL)) OR
               (is_deleted AND (gmt_deleted IS NOT NULL))),
    constraint ck_vault_generation_delete_state
        check ((NOT is_deleted) OR
               ((status)::text = ANY ((ARRAY ['RETIRED'::character varying, 'ABORTED'::character varying])::text[])))
);

comment on table vault_generation is '密码库代次控制记录；清理密文后保留最小记录供轮换回执引用';

comment on column vault_generation.id is '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';

comment on column vault_generation.vault_id is '内部密码库逻辑 ID';

comment on column vault_generation.generation is '库内单调递增代次，取消后不重复使用';

comment on column vault_generation.key_epoch is '客户端密钥代次；普通修改主密码保持不变';

comment on column vault_generation.status is 'STAGING=待提交，ACTIVE=活动，RETIRED=旧代，ABORTED=取消或超时';

comment on column vault_generation.gmt_create is '创建时间';

comment on column vault_generation.retired_at is '退出活动或暂存状态的时间，供密文清理任务使用';

comment on column vault_generation.gmt_modified is '更新时间，由应用更新，DEFAULT 不会自动更新';

comment on column vault_generation.create_user is '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';

comment on column vault_generation.update_user is '最近更新人系统用户 ID，由服务端填充；系统任务可为空';

comment on column vault_generation.is_deleted is '逻辑删除标记，false=有效、true=已删除';

comment on column vault_generation.delete_user is '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';

comment on column vault_generation.gmt_deleted is '逻辑删除时间，删除时与标记及删除人一起写入';


create unique index uk_vault_generation_active
    on vault_generation (vault_id)
    where ((status)::text = 'ACTIVE'::text);

create index idx_vault_generation_cleanup
    on vault_generation (retired_at, id)
    where ((status)::text = ANY ((ARRAY ['RETIRED'::character varying, 'ABORTED'::character varying])::text[]));

create table vault_key_slot
(
    id             bigint                                             not null
        unique
        constraint vault_key_slot_id_check
            check (id > 0),
    vault_id       bigint                                             not null,
    generation     bigint                                             not null
        constraint vault_key_slot_generation_check
            check ((generation >= 1) AND (generation <= '9007199254740991'::bigint)),
    slot_id        uuid                                               not null
        unique,
    slot_type      varchar(16)                                        not null
        constraint vault_key_slot_slot_type_check
            check ((slot_type)::text = ANY
        ((ARRAY ['NORMAL'::character varying, 'RECOVERY'::character varying])::text[])),
    key_id         uuid                                               not null,
    format_version smallint                                           not null
        constraint vault_key_slot_format_version_check
            check (format_version = 1),
    algorithm      varchar(16)                                        not null
        constraint vault_key_slot_algorithm_check
            check ((algorithm)::text = 'A256GCM'::text),
    iv             bytea                                              not null
        constraint vault_key_slot_iv_check
            check (octet_length(iv) = 12),
    ciphertext     bytea                                              not null
        constraint vault_key_slot_ciphertext_check
            check (octet_length(ciphertext) = 48),
    kdf_profile    varchar(64)
        constraint vault_key_slot_kdf_profile_check
            check ((kdf_profile)::text = 'ARGON2ID_V19_M65536_T3_P4_L32'::text),
    kdf_salt       bytea
        constraint vault_key_slot_kdf_salt_check
            check (octet_length(kdf_salt) = 16),
    wrap_salt      bytea
        constraint vault_key_slot_wrap_salt_check
            check (octet_length(wrap_salt) = 32),
    recovery_salt  bytea
        constraint vault_key_slot_recovery_salt_check
            check (octet_length(recovery_salt) = 32),
    gmt_create     timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified   timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user    bigint,
    update_user    bigint,
    is_deleted     boolean                  default false             not null,
    delete_user    bigint,
    gmt_deleted    timestamp with time zone,
    constraint ck_key_slot_parameters
        check ((((slot_type)::text = 'NORMAL'::text) AND (kdf_profile IS NOT NULL) AND (kdf_salt IS NOT NULL) AND
                (wrap_salt IS NOT NULL) AND (recovery_salt IS NULL)) OR
               (((slot_type)::text = 'RECOVERY'::text) AND (kdf_profile IS NULL) AND (kdf_salt IS NULL) AND
                (wrap_salt IS NULL) AND (recovery_salt IS NOT NULL))),
    constraint ck_vault_key_slot_delete_audit
        check (((NOT is_deleted) AND (delete_user IS NULL) AND (gmt_deleted IS NULL)) OR
               (is_deleted AND (gmt_deleted IS NOT NULL)))
);

comment on table vault_key_slot is '每代正常与恢复 VMK 密钥槽；仅保存公开参数和经过认证加密的 VMK';

comment on column vault_key_slot.id is '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';

comment on column vault_key_slot.vault_id is '内部密码库逻辑 ID';

comment on column vault_key_slot.generation is '所属代次';

comment on column vault_key_slot.slot_id is '公开密钥槽 ID；每次重建槽生成新值';

comment on column vault_key_slot.slot_type is 'NORMAL=主密码加账户密钥，RECOVERY=独立恢复密钥';

comment on column vault_key_slot.key_id is 'NORMAL 对应 accountKeyId，RECOVERY 对应 recoveryKeyId；不是秘密本身';

comment on column vault_key_slot.format_version is '密文盒版本，v1 仅支持 1';

comment on column vault_key_slot.algorithm is '认证加密算法，v1 固定 A256GCM';

comment on column vault_key_slot.iv is '每次加密独立生成的 12 字节 IV';

comment on column vault_key_slot.ciphertext is '32 字节 VMK 密文及末尾 16 字节 GCM 标签';

comment on column vault_key_slot.kdf_profile is '正常槽固定 KDF 档案，不接收任意内存或迭代参数';

comment on column vault_key_slot.kdf_salt is '正常槽公开 Argon2 盐';

comment on column vault_key_slot.wrap_salt is '正常槽公开 HKDF 盐';

comment on column vault_key_slot.recovery_salt is '恢复槽公开 HKDF 盐';

comment on column vault_key_slot.gmt_create is '创建时间';

comment on column vault_key_slot.gmt_modified is '更新时间，由应用更新，DEFAULT 不会自动更新';

comment on column vault_key_slot.create_user is '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';

comment on column vault_key_slot.update_user is '最近更新人系统用户 ID，由服务端填充；系统任务可为空';

comment on column vault_key_slot.is_deleted is '逻辑删除标记，false=有效、true=已删除';

comment on column vault_key_slot.delete_user is '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';

comment on column vault_key_slot.gmt_deleted is '逻辑删除时间，删除时与标记及删除人一起写入';


create unique index uk_vault_key_slot_active on vault_key_slot (vault_id, generation, slot_type) where (is_deleted = false);

create index idx_vault_key_slot_deleted
    on vault_key_slot (gmt_deleted, id)
    where (is_deleted = true);

create table vault_item
(
    id                            bigint                                             not null
        unique
        constraint vault_item_id_check
            check (id > 0),
    vault_id                      bigint                                             not null,
    generation                    bigint                                             not null
        constraint vault_item_generation_check
            check ((generation >= 1) AND (generation <= '9007199254740991'::bigint)),
    key_epoch                     bigint                                             not null
        constraint vault_item_key_epoch_check
            check ((key_epoch >= 1) AND (key_epoch <= '9007199254740991'::bigint)),
    item_id                       uuid                                               not null,
    content_version               bigint                                             not null
        constraint vault_item_content_version_check
            check ((content_version >= 1) AND (content_version <= '9007199254740991'::bigint)),
    row_version                   bigint                                             not null
        constraint vault_item_row_version_check
            check ((row_version >= 1) AND (row_version <= '9007199254740991'::bigint)),
    wrapped_dek_format_version    smallint                                           not null
        constraint vault_item_wrapped_dek_format_version_check
            check (wrapped_dek_format_version = 1),
    wrapped_dek_algorithm         varchar(16)                                        not null
        constraint vault_item_wrapped_dek_algorithm_check
            check ((wrapped_dek_algorithm)::text = 'A256GCM'::text),
    wrapped_dek_iv                bytea                                              not null
        constraint vault_item_wrapped_dek_iv_check
            check (octet_length(wrapped_dek_iv) = 12),
    wrapped_dek_ciphertext        bytea                                              not null
        constraint vault_item_wrapped_dek_ciphertext_check
            check (octet_length(wrapped_dek_ciphertext) = 48),
    encrypted_item_format_version smallint                                           not null
        constraint vault_item_encrypted_item_format_version_check
            check (encrypted_item_format_version = 1),
    encrypted_item_algorithm      varchar(16)                                        not null
        constraint vault_item_encrypted_item_algorithm_check
            check ((encrypted_item_algorithm)::text = 'A256GCM'::text),
    encrypted_item_iv             bytea                                              not null
        constraint vault_item_encrypted_item_iv_check
            check (octet_length(encrypted_item_iv) = 12),
    encrypted_item_ciphertext     bytea                                              not null
        constraint vault_item_encrypted_item_ciphertext_check
            check ((octet_length(encrypted_item_ciphertext) >= 17) AND
                   (octet_length(encrypted_item_ciphertext) <= 16400)),
    gmt_create                    timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified                  timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user                   bigint,
    update_user                   bigint,
    is_deleted                    boolean                  default false             not null,
    delete_user                   bigint,
    gmt_deleted                   timestamp with time zone,
    unique (vault_id, generation, item_id),
    constraint ck_vault_item_delete_audit
        check (((NOT is_deleted) AND (delete_user IS NULL) AND (gmt_deleted IS NULL)) OR
               (is_deleted AND (gmt_deleted IS NOT NULL)))
);

comment on table vault_item is '整条加密的密码条目；暂存和旧代共用本表，不保存可搜索明文或长期历史版本';

comment on column vault_item.id is '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';

comment on column vault_item.vault_id is '内部密码库逻辑 ID';

comment on column vault_item.generation is '所属代次；普通查询只能读取活动代';

comment on column vault_item.key_epoch is '密钥代次，由 Service 在事务内校验与所属代一致';

comment on column vault_item.item_id is '公开条目 ID，库内跨轮换保持稳定';

comment on column vault_item.content_version is '用于 AAD 的内容版本；普通编辑递增，完整轮换保持原值';

comment on column vault_item.row_version is '服务端行版本；普通编辑递增，新代重新从 1 开始';

comment on column vault_item.wrapped_dek_format_version is '条目密钥密文盒版本';

comment on column vault_item.wrapped_dek_algorithm is '条目密钥认证加密算法';

comment on column vault_item.wrapped_dek_iv is '包装 DEK 使用的独立随机 IV';

comment on column vault_item.wrapped_dek_ciphertext is '32 字节 DEK 密文及 16 字节认证标签';

comment on column vault_item.encrypted_item_format_version is '条目内容密文盒版本';

comment on column vault_item.encrypted_item_algorithm is '条目内容认证加密算法';

comment on column vault_item.encrypted_item_iv is '加密条目使用的独立随机 IV';

comment on column vault_item.encrypted_item_ciphertext is '最多 16 KiB 明文对应的密文与 16 字节认证标签';

comment on column vault_item.gmt_create is '创建时间';

comment on column vault_item.gmt_modified is '更新时间，由应用更新，DEFAULT 不会自动更新';

comment on column vault_item.create_user is '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';

comment on column vault_item.update_user is '最近更新人系统用户 ID，由服务端填充；系统任务可为空';

comment on column vault_item.is_deleted is '逻辑删除标记，false=有效、true=已删除';

comment on column vault_item.delete_user is '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';

comment on column vault_item.gmt_deleted is '逻辑删除时间，删除时与标记及删除人一起写入';


create index idx_vault_item_deleted
    on vault_item (gmt_deleted, id)
    where (is_deleted = true);

create index vault_item_deleted_page_idx
    on vault_item (vault_id asc, generation asc, gmt_deleted desc, item_id asc)
    where (is_deleted = true);

create index vault_item_deleted_expiry_idx
    on vault_item (gmt_deleted, vault_id)
    where (is_deleted = true);

create table vault_rotation
(
    id                           bigint                                             not null
        unique
        constraint vault_rotation_id_check
            check (id > 0),
    rotation_id                  uuid                                               not null
        unique,
    vault_id                     bigint                                             not null,
    mode                         varchar(16)                                        not null
        constraint vault_rotation_mode_check
            check ((mode)::text = ANY ((ARRAY ['ROTATION'::character varying, 'RESTORE'::character varying])::text[])),
    status                       varchar(16)                                        not null
        constraint vault_rotation_status_check
            check ((status)::text = ANY
                   ((ARRAY ['UPLOADING'::character varying, 'COMMITTED'::character varying, 'CANCELLED'::character varying, 'EXPIRED'::character varying])::text[])),
    source_generation            bigint
        constraint vault_rotation_source_generation_check
            check ((source_generation >= 1) AND (source_generation <= '9007199254740991'::bigint)),
    target_generation            bigint                                             not null
        constraint vault_rotation_target_generation_check
            check ((target_generation >= 1) AND (target_generation <= '9007199254740991'::bigint)),
    expected_meta_revision       bigint                                             not null
        constraint vault_rotation_expected_meta_revision_check
            check ((expected_meta_revision >= 1) AND (expected_meta_revision <= '9007199254740991'::bigint)),
    expected_collection_revision bigint                                             not null
        constraint vault_rotation_expected_collection_revision_check
            check ((expected_collection_revision >= 1) AND
                   (expected_collection_revision <= '9007199254740991'::bigint)),
    started_auth_epoch           bigint                                             not null
        constraint vault_rotation_started_auth_epoch_check
            check ((started_auth_epoch >= 1) AND (started_auth_epoch <= '9007199254740991'::bigint)),
    session_binding_id           uuid                                               not null,
    expected_item_count          integer                                            not null
        constraint vault_rotation_expected_item_count_check
            check (expected_item_count >= 0),
    verified_manifest_digest     bytea
        constraint vault_rotation_verified_manifest_digest_check
            check (octet_length(verified_manifest_digest) = 32),
    commit_receipt               jsonb
        constraint vault_rotation_commit_receipt_check
            check ((jsonb_typeof(commit_receipt) = 'object'::text) AND (octet_length((commit_receipt)::text) <= 4096)),
    expires_at                   timestamp with time zone                           not null,
    finished_at                  timestamp with time zone,
    gmt_create                   timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified                 timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user                  bigint,
    update_user                  bigint,
    is_deleted                   boolean                  default false             not null,
    delete_user                  bigint,
    gmt_deleted                  timestamp with time zone,
    unique (vault_id, target_generation),
    constraint ck_rotation_source
        check ((((mode)::text = 'ROTATION'::text) AND (source_generation IS NOT NULL)) OR
               ((mode)::text = 'RESTORE'::text)),
    constraint ck_rotation_generation
        check ((source_generation IS NULL) OR (target_generation > source_generation)),
    constraint ck_rotation_deadline
        check ((expires_at > gmt_create) AND (expires_at <= (gmt_create + '00:30:00'::interval))),
    constraint ck_rotation_terminal
        check ((((status)::text = 'UPLOADING'::text) AND (finished_at IS NULL)) OR
               (((status)::text <> 'UPLOADING'::text) AND (finished_at IS NOT NULL))),
    constraint ck_rotation_receipt
        check ((((status)::text = 'COMMITTED'::text) AND (commit_receipt IS NOT NULL) AND
                (verified_manifest_digest IS NOT NULL)) OR
               (((status)::text <> 'COMMITTED'::text) AND (commit_receipt IS NULL) AND
                (verified_manifest_digest IS NULL))),
    constraint ck_vault_rotation_delete_audit
        check (((NOT is_deleted) AND (delete_user IS NULL) AND (gmt_deleted IS NULL)) OR
               (is_deleted AND (gmt_deleted IS NOT NULL))),
    constraint ck_vault_rotation_delete_state
        check ((NOT is_deleted) OR ((status)::text = ANY
                                    ((ARRAY ['COMMITTED'::character varying, 'CANCELLED'::character varying, 'EXPIRED'::character varying])::text[])))
);

comment on table vault_rotation is '完整轮换与备份恢复任务；终态回执持久化，用于提交响应丢失后的结果确认';

comment on column vault_rotation.id is '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';

comment on column vault_rotation.rotation_id is '公开任务 ID，不代替账户和会话授权';

comment on column vault_rotation.vault_id is '目标密码库';

comment on column vault_rotation.mode is 'ROTATION=已有库换密钥，RESTORE=向未初始化或空库恢复备份';

comment on column vault_rotation.status is 'UPLOADING=可上传验证，其他状态为不可逆终态';

comment on column vault_rotation.source_generation is '开始时的活动代；目标未初始化时为 NULL';

comment on column vault_rotation.target_generation is '全新暂存代，取消后不复用';

comment on column vault_rotation.expected_meta_revision is '开始时锁定的元数据版本';

comment on column vault_rotation.expected_collection_revision is '开始时锁定的集合版本';

comment on column vault_rotation.started_auth_epoch is '开始时的认证代次；外部敏感变更后禁止旧任务继续提交';

comment on column vault_rotation.session_binding_id is '服务端生成的逻辑会话标识；会话 ID 更新时保持，不是 Cookie 或 Session ID';

comment on column vault_rotation.expected_item_count is '登记的预期条目数量；提交还必须检查逐项集合';

comment on column vault_rotation.verified_manifest_digest is '客户端完整下载校验后提交的规范暂存密文清单摘要，不代表服务器验证了明文';

comment on column vault_rotation.commit_receipt is '仅含提交结果、代次和版本的公开回执，不能含秘密或会话令牌';

comment on column vault_rotation.expires_at is '最长 30 分钟绝对截止时间；二次认证不会延长';

comment on column vault_rotation.finished_at is '提交、取消或超时的终态时间';

comment on column vault_rotation.gmt_create is '创建时间';

comment on column vault_rotation.gmt_modified is '更新时间，由应用更新，DEFAULT 不会自动更新';

comment on column vault_rotation.create_user is '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';

comment on column vault_rotation.update_user is '最近更新人系统用户 ID，由服务端填充；系统任务可为空';

comment on column vault_rotation.is_deleted is '逻辑删除标记，false=有效、true=已删除';

comment on column vault_rotation.delete_user is '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';

comment on column vault_rotation.gmt_deleted is '逻辑删除时间，删除时与标记及删除人一起写入';


create index idx_vault_rotation_source
    on vault_rotation (vault_id, source_generation);

create unique index uk_vault_rotation_open
    on vault_rotation (vault_id)
    where ((status)::text = 'UPLOADING'::text);

create index idx_vault_rotation_expiry
    on vault_rotation (expires_at, id)
    where ((status)::text = 'UPLOADING'::text);

create table vault_rotation_expected_item
(
    rotation_id            bigint                                             not null,
    item_id                uuid                                               not null,
    source_content_version bigint
        constraint vault_rotation_expected_item_source_content_version_check
            check ((source_content_version >= 1) AND (source_content_version <= '9007199254740991'::bigint)),
    source_row_version     bigint
        constraint vault_rotation_expected_item_source_row_version_check
            check ((source_row_version >= 1) AND (source_row_version <= '9007199254740991'::bigint)),
    target_content_version bigint                                             not null
        constraint vault_rotation_expected_item_target_content_version_check
            check ((target_content_version >= 1) AND (target_content_version <= '9007199254740991'::bigint)),
    gmt_create             timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified           timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user            bigint,
    update_user            bigint,
    is_deleted             boolean                  default false             not null,
    delete_user            bigint,
    gmt_deleted            timestamp with time zone,
    unique (rotation_id, item_id),
    constraint ck_rotation_expected_source
        check ((source_content_version IS NULL) = (source_row_version IS NULL)),
    constraint ck_vault_rotation_expected_item_delete_audit
        check (((NOT is_deleted) AND (delete_user IS NULL) AND (gmt_deleted IS NULL)) OR
               (is_deleted AND (gmt_deleted IS NOT NULL)))
);

comment on table vault_rotation_expected_item is '轮换开始时冻结的逐项集合，防止只按条目数量提交造成缺项或调包';

comment on column vault_rotation_expected_item.rotation_id is '内部轮换任务逻辑 ID';

comment on column vault_rotation_expected_item.item_id is '预期目标条目 ID';

comment on column vault_rotation_expected_item.source_content_version is '轮换源内容版本；备份恢复模式为 NULL';

comment on column vault_rotation_expected_item.source_row_version is '轮换源行版本；备份恢复模式为 NULL';

comment on column vault_rotation_expected_item.target_content_version is '完整轮换保持源版本；备份恢复在新上下文中固定从 1 开始';

comment on column vault_rotation_expected_item.gmt_create is '创建时间';

comment on column vault_rotation_expected_item.gmt_modified is '更新时间，由应用更新，DEFAULT 不会自动更新';

comment on column vault_rotation_expected_item.create_user is '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';

comment on column vault_rotation_expected_item.update_user is '最近更新人系统用户 ID，由服务端填充；系统任务可为空';

comment on column vault_rotation_expected_item.is_deleted is '逻辑删除标记，false=有效、true=已删除';

comment on column vault_rotation_expected_item.delete_user is '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';

comment on column vault_rotation_expected_item.gmt_deleted is '逻辑删除时间，删除时与标记及删除人一起写入';


create table vault_rotation_batch
(
    id             bigint                                             not null
        unique
        constraint vault_rotation_batch_id_check
            check (id > 0),
    rotation_id    bigint                                             not null,
    batch_id       uuid                                               not null,
    request_digest bytea                                              not null
        constraint vault_rotation_batch_request_digest_check
            check (octet_length(request_digest) = 32),
    item_count     integer                                            not null
        constraint vault_rotation_batch_item_count_check
            check ((item_count >= 1) AND (item_count <= 100)),
    gmt_create     timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified   timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user    bigint,
    update_user    bigint,
    is_deleted     boolean                  default false             not null,
    delete_user    bigint,
    gmt_deleted    timestamp with time zone,
    unique (rotation_id, batch_id),
    constraint ck_vault_rotation_batch_delete_audit
        check (((NOT is_deleted) AND (delete_user IS NULL) AND (gmt_deleted IS NULL)) OR
               (is_deleted AND (gmt_deleted IS NOT NULL)))
);

comment on table vault_rotation_batch is '暂存批次持久化回执；批次登记与条目写入在同一数据库事务内完成';

comment on column vault_rotation_batch.id is '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';

comment on column vault_rotation_batch.rotation_id is '内部轮换任务逻辑 ID';

comment on column vault_rotation_batch.batch_id is '客户端批次 ID，同任务内唯一';

comment on column vault_rotation_batch.request_digest is '规范请求 SHA-256 摘要；同批次不同内容必须拒绝';

comment on column vault_rotation_batch.item_count is '本批次条目数量；请求体还需限制为 1 MiB';

comment on column vault_rotation_batch.gmt_create is '创建时间';

comment on column vault_rotation_batch.gmt_modified is '更新时间，由应用更新，DEFAULT 不会自动更新';

comment on column vault_rotation_batch.create_user is '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';

comment on column vault_rotation_batch.update_user is '最近更新人系统用户 ID，由服务端填充；系统任务可为空';

comment on column vault_rotation_batch.is_deleted is '逻辑删除标记，false=有效、true=已删除';

comment on column vault_rotation_batch.delete_user is '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';

comment on column vault_rotation_batch.gmt_deleted is '逻辑删除时间，删除时与标记及删除人一起写入';


create table vault_idempotency_record
(
    id              bigint                                             not null
        unique
        constraint vault_idempotency_record_id_check
            check (id > 0),
    account_id      bigint                                             not null,
    operation       varchar(32)                                        not null
        constraint vault_idempotency_record_operation_check
            check ((operation)::text = ANY
        ((ARRAY ['INITIALIZE'::character varying, 'CREATE_ITEM'::character varying, 'UPDATE_ITEM'::character varying, 'DELETE_ITEM'::character varying, 'DELETE_GROUP'::character varying, 'BATCH_ITEMS'::character varying, 'RESTORE_ITEM'::character varying, 'PURGE_ITEM'::character varying, 'CHANGE_MASTER'::character varying, 'ROTATION_START'::character varying, 'ROTATION_COMMIT'::character varying, 'ROTATION_CANCEL'::character varying, 'ROTATION_RESUME'::character varying, 'RESTORE_START'::character varying])::text[])),
    idempotency_key uuid                                               not null,
    request_digest  bytea                                              not null
        constraint vault_idempotency_record_request_digest_check
            check (octet_length(request_digest) = 32),
    http_status     smallint                                           not null
        constraint vault_idempotency_record_http_status_check
            check ((http_status >= 200) AND (http_status <= 299)),
    response_body   jsonb                                              not null
        constraint vault_idempotency_record_response_body_check
            check ((jsonb_typeof(response_body) = 'object'::text) AND (octet_length((response_body)::text) <= 65536)),
    gmt_create      timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified    timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user     bigint,
    update_user     bigint,
    is_deleted      boolean                  default false             not null,
    delete_user     bigint,
    gmt_deleted     timestamp with time zone,
    constraint vault_idempotency_record_account_id_operation_idempotency_k_key
        unique (account_id, operation, idempotency_key),
    constraint ck_vault_idempotency_record_delete_audit
        check (((NOT is_deleted) AND (delete_user IS NULL) AND (gmt_deleted IS NULL)) OR
               (is_deleted AND (gmt_deleted IS NOT NULL)))
);

comment on table vault_idempotency_record is '业务写请求的持久化幂等回执；与对应业务变更同事务提交，不依赖 Redis';

comment on column vault_idempotency_record.id is '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';

comment on column vault_idempotency_record.account_id is '所属认证账户';

comment on column vault_idempotency_record.operation is '业务操作类型；批次幂等使用 rotation_batch';

comment on column vault_idempotency_record.idempotency_key is '客户端随机幂等键';

comment on column vault_idempotency_record.request_digest is '绑定账户、操作及规范业务参数的 SHA-256 摘要';

comment on column vault_idempotency_record.http_status is '已成功提交结果的真实 HTTP 状态';

comment on column vault_idempotency_record.response_body is '仅保存最小公开 JSON 回执；禁止保存任何解密秘密、MFA 恢复码或会话令牌';

comment on column vault_idempotency_record.gmt_create is '创建时间';

comment on column vault_idempotency_record.gmt_modified is '更新时间，由应用更新，DEFAULT 不会自动更新';

comment on column vault_idempotency_record.create_user is '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';

comment on column vault_idempotency_record.update_user is '最近更新人系统用户 ID，由服务端填充；系统任务可为空';

comment on column vault_idempotency_record.is_deleted is '逻辑删除标记，false=有效、true=已删除';

comment on column vault_idempotency_record.delete_user is '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';

comment on column vault_idempotency_record.gmt_deleted is '逻辑删除时间，删除时与标记及删除人一起写入';


create table vault_session_revocation_outbox
(
    id              bigint                                                        not null
        unique
        constraint vault_session_revocation_outbox_id_check
            check (id > 0),
    account_id      bigint                                                        not null,
    auth_epoch      bigint                                                        not null
        constraint vault_session_revocation_outbox_auth_epoch_check
            check ((auth_epoch >= 1) AND (auth_epoch <= '9007199254740991'::bigint)),
    status          varchar(16)              default 'PENDING'::character varying not null
        constraint vault_session_revocation_outbox_status_check
            check ((status)::text = ANY
                   ((ARRAY ['PENDING'::character varying, 'PROCESSING'::character varying, 'DONE'::character varying])::text[])),
    attempts        integer                  default 0                            not null
        constraint vault_session_revocation_outbox_attempts_check
            check (attempts >= 0),
    available_at    timestamp with time zone default CURRENT_TIMESTAMP            not null,
    lease_until     timestamp with time zone,
    last_error_code varchar(64),
    completed_at    timestamp with time zone,
    gmt_create      timestamp with time zone default CURRENT_TIMESTAMP            not null,
    gmt_modified    timestamp with time zone default CURRENT_TIMESTAMP            not null,
    create_user     bigint,
    update_user     bigint,
    is_deleted      boolean                  default false                        not null,
    delete_user     bigint,
    gmt_deleted     timestamp with time zone,
    unique (account_id, auth_epoch),
    constraint ck_revocation_lease
        check ((((status)::text = 'PROCESSING'::text) AND (lease_until IS NOT NULL)) OR
               (((status)::text <> 'PROCESSING'::text) AND (lease_until IS NULL))),
    constraint ck_revocation_completed
        check ((((status)::text = 'DONE'::text) AND (completed_at IS NOT NULL)) OR
               (((status)::text <> 'DONE'::text) AND (completed_at IS NULL))),
    constraint ck_vault_session_revocation_outbox_delete_audit
        check (((NOT is_deleted) AND (delete_user IS NULL) AND (gmt_deleted IS NULL)) OR
               (is_deleted AND (gmt_deleted IS NOT NULL))),
    constraint ck_vault_session_revocation_outbox_delete_state
        check ((NOT is_deleted) OR ((status)::text = 'DONE'::text))
);

comment on table vault_session_revocation_outbox is '数据库事务内产生的 Redis 旧会话撤销任务；认证代次是立即生效的授权依据';

comment on column vault_session_revocation_outbox.id is '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';

comment on column vault_session_revocation_outbox.account_id is '待清理会话的认证账户';

comment on column vault_session_revocation_outbox.auth_epoch is '清理会话代次小于该值的会话，不能删除之后新建的会话';

comment on column vault_session_revocation_outbox.status is 'PENDING=待重试，PROCESSING=持有租约，DONE=已完成';

comment on column vault_session_revocation_outbox.attempts is '处理次数';

comment on column vault_session_revocation_outbox.available_at is '下一次可领取时间，失败按退避策略重试';

comment on column vault_session_revocation_outbox.lease_until is '工作线程租约截止，崩溃后可重新领取';

comment on column vault_session_revocation_outbox.last_error_code is '服务端固定错误码，不记录异常正文或 Session ID';

comment on column vault_session_revocation_outbox.completed_at is '完成清理时间';

comment on column vault_session_revocation_outbox.gmt_create is '创建时间';

comment on column vault_session_revocation_outbox.gmt_modified is '更新时间，由应用更新，DEFAULT 不会自动更新';

comment on column vault_session_revocation_outbox.create_user is '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';

comment on column vault_session_revocation_outbox.update_user is '最近更新人系统用户 ID，由服务端填充；系统任务可为空';

comment on column vault_session_revocation_outbox.is_deleted is '逻辑删除标记，false=有效、true=已删除';

comment on column vault_session_revocation_outbox.delete_user is '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';

comment on column vault_session_revocation_outbox.gmt_deleted is '逻辑删除时间，删除时与标记及删除人一起写入';


create index idx_vault_revocation_ready on vault_session_revocation_outbox (available_at, id) where ((status)::text = 'PENDING'::text);

create index idx_vault_revocation_lease on vault_session_revocation_outbox (lease_until, id) where ((status)::text = 'PROCESSING'::text);

create table vault_audit_event
(
    id           bigint                                             not null
        unique
        constraint vault_audit_event_id_check
            check (id > 0),
    account_id   bigint,
    vault_id     bigint,
    action       varchar(32)                                        not null
        constraint vault_audit_event_action_check
            check ((action)::text = ANY
        ((ARRAY ['LOGIN'::character varying, 'LOGOUT'::character varying, 'PASSKEY_REGISTER'::character varying, 'PASSKEY_AUTHENTICATE'::character varying, 'PASSKEY_REVOKE'::character varying, 'MFA_RECOVERY'::character varying, 'RECOVERY_CODES_ISSUE'::character varying, 'RECOVERY_CODES_CONFIRM'::character varying, 'INITIALIZE'::character varying, 'ITEM_CREATE'::character varying, 'ITEM_UPDATE'::character varying, 'ITEM_DELETE'::character varying, 'ITEM_RESTORE'::character varying, 'ITEM_PURGE'::character varying, 'CHANGE_MASTER'::character varying, 'ROTATION_START'::character varying, 'ROTATION_COMMIT'::character varying, 'ROTATION_CANCEL'::character varying, 'ROTATION_RESUME'::character varying, 'ROTATION_EXPIRE'::character varying, 'RESTORE_START'::character varying, 'BACKUP_DOWNLOAD'::character varying, 'SESSION_REJECT'::character varying])::text[])),
    result       varchar(16)                                        not null
        constraint vault_audit_event_result_check
            check ((result)::text = ANY
                   ((ARRAY ['SUCCESS'::character varying, 'DENIED'::character varying, 'FAILURE'::character varying])::text[])),
    error_code   varchar(64),
    request_id   uuid                                               not null,
    gmt_create   timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean                  default false             not null,
    delete_user  bigint,
    gmt_deleted  timestamp with time zone,
    constraint ck_vault_audit_event_delete_audit
        check (((NOT is_deleted) AND (delete_user IS NULL) AND (gmt_deleted IS NULL)) OR
               (is_deleted AND (gmt_deleted IS NOT NULL)))
);

comment on table vault_audit_event is '认证、密文下载和写入审计；不记录请求正文、条目内容、恢复码或密钥文件';

comment on column vault_audit_event.id is '内部逻辑 ID，由应用使用 IdWorker / ASSIGN_ID 生成';

comment on column vault_audit_event.account_id is '可识别时的认证账户；未知账户登录失败可以为空';

comment on column vault_audit_event.vault_id is '所属密码库；初始化前的认证事件可以为空';

comment on column vault_audit_event.action is '固定审计动作；不允许客户端指定任意动作字符串';

comment on column vault_audit_event.result is '审计结果';

comment on column vault_audit_event.error_code is '服务端固定机器错误码；不拼接用户输入';

comment on column vault_audit_event.request_id is '服务端生成的请求追踪 ID';

comment on column vault_audit_event.gmt_create is '创建时间';

comment on column vault_audit_event.gmt_modified is '更新时间，由应用更新，DEFAULT 不会自动更新';

comment on column vault_audit_event.create_user is '创建人系统用户 ID，由服务端填充；系统任务或未知身份事件可为空';

comment on column vault_audit_event.update_user is '最近更新人系统用户 ID，由服务端填充；系统任务可为空';

comment on column vault_audit_event.is_deleted is '逻辑删除标记，false=有效、true=已删除';

comment on column vault_audit_event.delete_user is '执行逻辑删除的系统用户 ID；系统清理任务可为空，不使用伪造的用户 ID';

comment on column vault_audit_event.gmt_deleted is '逻辑删除时间，删除时与标记及删除人一起写入';


create index idx_vault_audit_account_time on vault_audit_event (account_id asc, gmt_create desc, id asc);

create index idx_vault_audit_vault_time on vault_audit_event (vault_id asc, gmt_create desc, id asc);

create table vault_site
(
    id           bigint                                             not null
        primary key,
    gmt_create   timestamp with time zone default CURRENT_TIMESTAMP not null,
    gmt_modified timestamp with time zone default CURRENT_TIMESTAMP not null,
    create_user  bigint,
    update_user  bigint,
    is_deleted   boolean                  default false             not null,
    status       smallint                 default 1                 not null
        constraint ck_vault_site_status
            check (status = ANY (ARRAY [0, 1])),
    site_name    varchar(128)                                       not null,
    site_domain  varchar(256)                                       not null,
    sort         integer                  default 0
);

comment on table vault_site is '密码库站点';

comment on column vault_site.id is '主键';

comment on column vault_site.gmt_create is '创建时间';

comment on column vault_site.gmt_modified is '更新时间';

comment on column vault_site.create_user is '创建者';

comment on column vault_site.update_user is '更新者';

comment on column vault_site.is_deleted is '是否删除(false:否; true:是)';

comment on column vault_site.status is '状态(1:正常; 0:禁用)';

comment on column vault_site.site_name is '站点名称';

comment on column vault_site.site_domain is '站点域名';

comment on column vault_site.sort is '排序';


create unique index uk_vs_sd on vault_site (site_domain) where (is_deleted = false);

