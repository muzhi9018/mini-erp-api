# 商品分类接口

基于 `product-management-design.md` 实施，仅维护内部 `product_category`，与官网分类独立。业务资料只有一套；分类校验、错误信息等系统提示按请求 `Accept-Language` 国际化，支持英文、简体中文和繁体中文，资源位于 `i18n/product/messages*.properties`，中文仅作为资源缺失时的兜底。

所有接口要求登录并具备下表权限，返回 `JsonResult<T>`。权限码需在现有菜单、角色授权体系中配置。

| 方法与路径 | 权限码 | 请求字段 | 返回 data |
| --- | --- | --- | --- |
| `GET /product/category/tree` | `product:category:query` | 无 | `List<ProductCategoryVO>` |
| `POST /product/category/create` | `product:category:create` | `code`、`name` 必填；`parentId`、`sortOrder`、`remark` 选填 | 创建后的 `ProductCategory` |
| `POST /product/category/update` | `product:category:update` | `id`、`name` 必填；`sortOrder`、`remark` 选填 | 修改后的 `ProductCategory` |
| `POST /product/category/delete` | `product:category:delete` | `id` 必填 | `true` |

写入请求复用 `ProductCategory` 实体，各接口仅处理表中列出的字段。主键、审计字段和删除标记由服务端维护；类目创建后父级固定，普通编辑仅修改名称、排序和备注。

## 参数和业务规则

- `id` 必须大于 0；`parentId` 必须大于或等于 0，0 表示顶级分类。新增不传 `parentId` 时使用 0。
- 商品分类最多三级，顶级为第 1 级；第 3 级分类下不允许新增子分类。
- `code`、`name` 去首尾空白后分别最多 64、128 个字符且不能为空；`remark` 去首尾空白后最多 1000 个字符，空白保存为 null。
- 新增不传 `sortOrder` 时使用 0。编辑不传 `sortOrder` 时保留原值；`remark` 缺省或 null 表示清空。
- 编码在包含逻辑删除记录的全表中唯一，不可复用。有效类目同父级名称唯一，不同父级可重名，删除后释放同级名称。
- 新增时父级必须存在且未删除；创建后不调整父级。
- 存在未删除子类目或商品时禁止删除，包括停用商品。已逻辑删除的商品不阻止删除。
- 树只返回有效类目，同级按 `sortOrder`、`id` 升序排列，每个节点继承分类全部字段并增加 `children`，叶节点为 `[]`，空树为 `[]`。
- 改名时，本类目和全部后代类目下的有效商品统一版本各加 1，商品审计字段同步更新；已删除或其他类目商品不变。只改排序、备注不递增商品版本。

## 事务

商品分类维护按无并发场景实现，不添加显式表锁、行锁或锁顺序协调。分类改名和关联商品版本递增在同一事务内提交，任一步骤失败整体回滚。

## 前端配合

分类管理页面调用以上接口，并按权限显示操作。新增、编辑按字段白名单提交，父级仅在新增时指定。后端未修改前端文件，也未对业务数据库建表或写入菜单权限数据。

## 验证

`mvn -Dtest=ProductCategoryControllerTest,ProductCategoryServiceImplTest test`：参数边界、方法权限，以及临时 PostgreSQL 中的三级层级上限、树排序、唯一性、逻辑删除、字段白名单、商品版本联动和失败回滚。
