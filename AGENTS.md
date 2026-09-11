# 项目编码约定

适用于整个仓库。基于现有代码整理；对历史上混用的写法，新增或修改的代码按下述规则统一，不顺带重排无关代码。

## 分层与命名

- Java 25、Spring Boot、MyBatis-Plus、Lombok、MapStruct Plus；依赖和版本以 `pom.xml` 为准。
- 基础包为 `com.muzhi.minierp`，业务按 `system`、`website` 等模块放入对应的 `controller`、`service`、`mapper`、`entity`、`vo` 包。
- 命名沿用 `XxxController`、`IXxxService`、`XxxServiceImpl`、`XxxMapper`、`XxxVO`。Controller 接收并校验参数，Service 处理业务与事务，Mapper 访问数据库；接口返回 `JsonResult<T>`。
- 依赖注入使用 `@RequiredArgsConstructor` 配合 `private final` 字段。实体/VO 沿用 Lombok；VO 继承实体时使用 `@EqualsAndHashCode(callSuper = true)`。
- 优先复用已有实体和 VO，不为每个接口、操作随意新增 VO。确需扩展时，VO 尽量继承对应实体，复用原有字段，只声明额外的业务字段（如子节点、关联列表、展示字段），不重复声明或遮蔽实体已有字段；参考 `WebsiteProductVO extends WebsiteProduct`，仅新增 `productI18n`。

## 注释

- 中文注释，类沿用现有 Javadoc 模板及 `@author`、`@since`；字段用多行 Javadoc 简述业务含义，类型/状态字段链接对应枚举。
- 接口方法和复杂方法写清用途，按需补充 `@param`、`@return`；实现方法不重复接口注释。
- 方法内用独占一行的 `// 说明` 解释业务约束或原因，避免逐句解释代码和保留废弃代码。

## 排版

- 使用 4 个空格缩进，不用 Tab；左花括号不另起一行，`else` / `catch` 紧接前一个 `}`，即使只有一条语句也保留花括号。
- 字段之间、方法之间、不同业务步骤之间空一行；注释、注解与所修饰的声明紧邻。不要连续堆叠空行，不留行尾空格。
- 新文件使用 UTF-8、LF，文件末尾保留一个换行；旧文件存在 LF/CRLF 混用，修改时保持该文件的原有行尾，避免整文件产生差异。

## 语句与方法调用

- 优先拆分语句、逐步换行：查询构造、数据库访问、业务计算等调用不要嵌套在其他方法的实参中，先用有业务含义的局部变量接收结果，再传参；不要仅把嵌套调用的参数换行。
- 查询条件先声明 Wrapper，再逐条设置；简单调用可保持单行。长参数列表按参数换行，续行增加两级缩进，关闭的 `);` 与调用起始行对齐；必须保留的长链式调用从 `.` 前换行。

```java
// 反例：参数换行后仍嵌套查询构造与数据库调用。
Assert.isTrue(
        super.baseMapper.exists(Wrappers.<WebsiteProductCategory>lambdaQuery().eq(WebsiteProductCategory::getCode, code)),
        "website.product-category.code-already-exists",
        "商品分类编码已存在"
);

// 正例：构造条件、查询、校验分别执行。
LambdaQueryWrapper<WebsiteProductCategory> codeQuery = Wrappers.lambdaQuery();
codeQuery.eq(WebsiteProductCategory::getCode, code);
boolean codeExisted = super.baseMapper.exists(codeQuery);
Assert.isTrue(codeExisted, "website.product-category.code-already-exists", "商品分类编码已存在");
```

## 本类与父类访问

- 本类实例方法显式写 `this.方法(...)`；参数和局部变量直接使用变量名。
- 本类字段仅在 Java Bean（如 Entity、VO、Model）访问自身数据属性时使用 `this.字段`，例如 setter 中的 `this.username = username`。Service、Controller 等类中注入的 Mapper、Service、工具组件等依赖直接使用字段名调用，如 `userMapper.insert(user)`，不加 `this`；此规则按字段用途区分，不是按字段类型是否为 Spring Bean 区分。
- 调用父类实现写 `super.方法(...)`；访问父类可见的实例字段写 `super.字段`，例如 `ServiceImpl` 提供的 `super.baseMapper`。父类的 `private` 字段通过其可访问的 getter/setter 操作，不能直接访问。
- `super` 会选择父类实现；需要当前类重写逻辑时使用 `this`，不能为了格式而改变动态分派。接口默认方法的正常调用使用 `this`；显式选择直接父接口的默认实现才使用 `接口名.super.方法(...)`。
- 静态工具方法使用 `类名.方法(...)`，不通过 `this` / `super` / 对象调用；本类常量可直接使用常量名，外部常量、枚举保留所属类型。Lombok 的静态日志字段使用 `log.info(...)` 等现有写法。

```java
// SysUserServiceImpl：本类方法、本类依赖、父类字段。
SysUser dbUser = this.findByUsername(user.getUsername());
String encodedPassword = passwordEncoder.encode(password);
super.baseMapper.insert(user);

// UserServiceImpl：本类注入的 Mapper 直接使用字段名。
userMapper.insert(user);

// Java Bean 的 setter：自身数据属性使用 this。
public void setUsername(String username) {
    this.username = username;
}

// LoginUser：父类私有字段通过父类 setter 修改，自身数据属性用 this。
super.setUsername(username);
super.setPassword(null);
this.enabled = enabled;

// 静态方法与枚举。
Long id = IdWorker.getId();
user.setStatus(SysUserStatus.ENABLED.value());
```

## Bean 转换

- 统一使用项目的 `BeanConvertUtils`（MapStruct Plus），不新增 `BeanUtils.copyProperties`、JSON 序列化中转或另一套转换工具。
- 参考 `SysMenuVO`、`WebsiteProductI18nVO`，在源/目标类型上配置对应的 `@AutoMappers` / `@AutoMapper(target = Xxx.class)`，确认所需转换方向能生成映射；不要修改 `target/generated-sources` 中的生成代码。
- 单对象用 `convert`，列表用 `mapList`，其他集合用 `mapCollection`。当前工具对 `null` 单对象返回 `null`，对 `null`/空集合返回空集合；调用前按业务要求处理必填校验。
- 转换后显式处理主键、关联 ID、默认值和不允许客户端修改的字段；更新时只写允许修改的字段，不能把整个请求对象无差别覆盖到数据库实体。

```java
WebsiteProductI18n translation = BeanConvertUtils.convert(query, WebsiteProductI18n.class);
translation.setId(IdWorker.getId());
translation.setProductId(productId);

List<SysMenuVO> menus = BeanConvertUtils.mapList(allMenuList, SysMenuVO.class);
Collection<SysMenuVO> menuViews = BeanConvertUtils.mapCollection(menuCollection, SysMenuVO.class);
```

## 枚举与常量

- 业务类型、状态、分类等有限取值统一用枚举定义和校验，不在业务代码中直接比较 `1`、`2`、`"FEATURE"` 等编码，也不用 `ordinal()` 持久化。
- 同一业务域的枚举集中放在 `enums/XxxEnum.java`，外层为接口，内部按维度定义枚举；以 `WebsiteProductEnum.DetailItemType`、`WebsiteProductEnum.MediaItemType` 为准。订单可采用下列结构（仅为示例，不代表现有订单模型）。

```java
public interface OrderEnum {

    @Getter
    enum Type {

        PURCHASE("PURCHASE", "采购"),
        SALE("SALE", "销售");

        private final String code;

        private final String name;

        Type(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum Status {

        CREATED("CREATED", "已创建"),
        COMPLETED("COMPLETED", "已完成");

        private final String code;

        private final String name;

        Status(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
```

- 枚举值使用大写下划线，字段使用 `private final`、`@Getter` 和显式构造器；编码类型与数据库/API 保持一致，不能为了统一结构修改已有编码。
- 方法参数优先接收具体枚举，如 `this.validateDetailItem(features, WebsiteProductEnum.DetailItemType.FEATURE)`；枚举对象用 `==` / `!=` 比较，字符串编码用 `OrderEnum.Type.PURCHASE.getCode().equals(code)`，不要用 `==` 比较字符串或包装数值。
- 外部编码的合法性检查集中在对应枚举的 `ofCode` / `eq` 等方法中，参考 `SysLocale.ofCode`、`SysUserStatus.eq`；需要时补充方法，再校验未知值，不能静默回退为正常状态。既有独立枚举继续复用，新增同域维度采用接口嵌套结构。
- Controller、Service 中尽量不定义类级常量。普通正则、默认参数及仅当前方法使用的数量/阈值直接在方法内定义，使用有业务含义的局部变量，如 `Pattern slugPattern = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*")`、`int defaultSortOrder = 0`、`int minimumCount = 4`。
- 避免含义不明的魔法数字和业务字符串：类型/状态使用枚举；确需跨方法、跨类共享的固定值优先复用已有常量，按业务归属集中定义，需要动态调整的值使用配置，不为单次使用机械抽取常量。普通循环起点、空数组长度等 `0` 可直接使用，集合容量无依据时使用默认构造器。

## 接口参数校验

- 一般入参校验放在 Controller：不依赖数据库或其他业务数据的必填、空值、字符串格式/长度、数值范围、集合数量、枚举编码合法性及入参字段间的关系校验，在调用 Service 前完成；先判空再访问属性或遍历集合，重复校验可提取为 Controller 私有方法。
- 依赖数据库或其他业务数据的校验放在 Service，如记录是否存在、编码是否重复、关联数据是否有效、当前业务状态是否允许操作；Controller 不为校验直接查询 Mapper。需保证一致性的校验与写入放在同一事务中。
- 使用本项目的 `com.muzhi.minierp.util.Assert`：**条件命中即抛异常**。例如 `Assert.isNull(dbUser, code, message)` 在对象为 `null` 时抛出，`Assert.isTrue(invalid, code, message)` 在条件为 `true` 时抛出；不要套用 Spring Assert 的语义。
- 业务异常使用 `BusinessException`，提供国际化 key 和中文兜底；占位符采用 `{0}`，参数单独传入。新增提示同步维护 `src/main/resources/i18n/{模块}/messages*.properties` 的各语言版本。

## 持久化与验证

- 查询优先使用 `Wrappers.lambdaQuery()` 和实体方法引用。自定义 SQL 放在 `src/main/resources/mapper/{模块}`，与 Mapper 方法及 `@Param` 对应，参数使用 `#{...}`。
- 多表写入在 Service 入口使用 `@Transactional(rollbackFor = Exception.class)`；主键沿用 `IdWorker` / `ASSIGN_ID`，审计字段沿用自动填充，逻辑删除按实体及现有配置处理。
- Java 改动按影响范围运行 `mvn -Dtest=测试类名 test` 或 `mvn test`；涉及 Bean 映射需通过编译验证生成映射。仅文档修改检查内容和差异即可；验证受环境限制时如实说明。
