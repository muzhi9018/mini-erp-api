package com.muzhi.minierp.service.sales.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.muzhi.minierp.config.FastJsonWebMvcConfiguration;
import com.muzhi.minierp.config.MybatisPlusConfig;
import com.muzhi.minierp.config.QuotationIssuerProperties;
import com.muzhi.minierp.controller.sales.QuotationController;
import com.muzhi.minierp.controller.sales.QuotationTemplateController;
import com.muzhi.minierp.enums.CurrencyEnum;
import com.muzhi.minierp.enums.QuotationEnum.Scope;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.exception.GlobalExceptionHandler;
import com.muzhi.minierp.i18n.I18nHelper;
import com.muzhi.minierp.model.LoginUser;
import com.muzhi.minierp.security.vault.VaultErrorWriter;
import com.muzhi.minierp.service.product.IProductSkuService;
import com.muzhi.minierp.service.product.impl.ProductImageServiceImpl;
import com.muzhi.minierp.service.product.impl.ProductSkuServiceImpl;
import com.muzhi.minierp.service.product.impl.ProductSnapshotServiceImpl;
import com.muzhi.minierp.service.product.impl.ProductUnitServiceImpl;
import com.muzhi.minierp.service.sales.IQuotationService;
import com.muzhi.minierp.service.sales.IQuotationTemplateService;
import com.muzhi.minierp.service.system.IAttachmentService;
import com.muzhi.minierp.vo.product.ProductQuotationQueryVO;
import com.muzhi.minierp.vo.sales.*;
import io.github.linpeilie.mapstruct.MapstructAutoConfiguration;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 使用临时 PostgreSQL 验证报价事务、模板及 HTTP 协议。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuotationIntegrationTest {

    private static final long SKU_A = 2193456789012345678L;

    private static final long SKU_B = 2193456789012345679L;

    private static final long SKU_TWD = 2193456789012345680L;

    private static final long SKU_UNPRICED = 2193456789012345681L;

    private static final String QUOTE_QUERY = "sales-manager:quotation:query";

    private static final String QUOTE_CREATE = "sales-manager:quotation:create";

    private static final String QUOTE_UPDATE = "sales-manager:quotation:update";

    private static final String PERSONAL_QUERY = "sales-manager:quotation-template:personal:query";

    private static final String PUBLIC_QUERY = "sales-manager:quotation-template:public:query";

    private static final String PERSONAL_DELETE = "sales-manager:quotation-template:personal:delete";

    private static final String PUBLIC_DELETE = "sales-manager:quotation-template:public:delete";

    private static final OffsetDateTime QUOTED_AT = OffsetDateTime.parse("2026-11-20T14:30:00+08:00");

    private EmbeddedPostgres postgres;

    private AnnotationConfigWebApplicationContext context;

    private JdbcTemplate jdbc;

    private IQuotationService quotes;

    private IQuotationTemplateService templates;

    private MockMvc mvc;

    @BeforeAll
    void startDatabaseAndApplication() throws Exception {
        assumeFalse("root".equals(System.getProperty("user.name")), "PostgreSQL initdb requires a non-root user");
        postgres = EmbeddedPostgres.builder().start();
        DataSource dataSource = postgres.getPostgresDatabase();
        ResourceDatabasePopulator schema = new ResourceDatabasePopulator(new FileSystemResource("docs/sql/schema.sql"));
        schema.execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);

        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.addBeanFactoryPostProcessor(factory -> factory.registerSingleton("dataSource", dataSource));
        context.register(TestConfiguration.class);
        context.refresh();
        quotes = context.getBean(IQuotationService.class);
        templates = context.getBean(IQuotationTemplateService.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterAll
    void stopDatabaseAndApplication() throws IOException {
        if (context != null) {
            context.close();
        }
        if (postgres != null) {
            postgres.close();
        }
    }

    @BeforeEach
    void seedProductsAndLogin() {
        jdbc.execute("""
                TRUNCATE quotation, quotation_item, quotation_template, quotation_template_item,
                    product, product_sku, product_unit, product_category, product_snapshot,
                    product_snapshot_spec, product_snapshot_image;
                INSERT INTO product_category (id, code, name) VALUES (1, 'DISPLAY', '陈列设备');
                INSERT INTO product_unit (id, code, name) VALUES (1, 'PC', '件');
                INSERT INTO product (id, category_id, code, name, status) VALUES
                    (11, 1, 'A', '展厅储物设备', 'ENABLED'),
                    (12, 1, 'B', '陈列储物配件', 'ENABLED'),
                    (13, 1, 'C', '展櫃與導軌', 'ENABLED'),
                    (14, 1, 'D', '未定价', 'ENABLED');
                """);
        this.sku(SKU_A, 11L, "A-1", 0, "129.345", "CNY");
        this.sku(SKU_B, 12L, "B-1", 2, "80.00", "CNY");
        this.sku(SKU_TWD, 13L, "C-1", 0, "0", "TWD");
        this.sku(SKU_UNPRICED, 14L, "D-1", 0, null, null);
        this.signIn(1L, QUOTE_QUERY);
    }

    @AfterEach
    void clearLogin() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void previewsThenPersistsHistoricalPricingAndCopy() throws Exception {
        QuotationFormVO source = this.sampleForm();
        QuotationDocumentVO preview = quotes.preview(source);
        assertNull(preview.quoteNumber());
        assertEquals("316.18", preview.totals().totalAmount());
        assertEquals("232.82", preview.items().getFirst().lineAmount());
        assertEquals("16.64", preview.totals().orderDiscountAmount());
        assertEquals(0, this.count("quotation"));
        assertEquals(0, this.count("product_snapshot"));

        QuotationSummaryVO saved = quotes.create(source);
        QuotationDetailVO detail = quotes.detail(saved.getId());
        assertEquals(source.quotedAt(), detail.form().quotedAt());
        assertEquals(0, detail.version());
        assertFalse(detail.orderGenerated());
        assertEquals(saved.getQuoteNumber(), detail.document().quoteNumber());
        assertEquals("演示客户", detail.form().customer().getCustomerName());
        assertEquals(2, this.count("product_snapshot"));
    }

    @Test
    void keepsHistoricalProductAndValidatesAccessVersionAndOrderLock() {
        QuotationSummaryVO saved = quotes.create(this.sampleForm());
        QuotationDetailVO current = quotes.detail(saved.getId());
        jdbc.update("UPDATE quotation SET contact_email = ? WHERE id = ?", "demo@example.test", saved.getId());
        jdbc.update("UPDATE product_sku SET reference_sale_price = 150, version = 1 WHERE id = ?", SKU_A);
        jdbc.update("UPDATE product SET version = 1 WHERE id = 11");
        assertEquals("quotation.sku-version-conflict",
                assertThrows(BusinessException.class, () -> quotes.create(this.sampleForm())).getI18nCode());
        jdbc.update("UPDATE product_sku SET is_deleted = true WHERE id = ?", SKU_A);
        jdbc.update("UPDATE product_unit SET name = '箱' WHERE id = 1");

        QuotationDocumentVO history = quotes.detail(saved.getId()).document();
        assertEquals("件", history.items().getFirst().unitName());
        assertEquals("129.345", history.items().getFirst().unitPrice());
        assertEquals("316.18", history.totals().totalAmount());

        QuotationFormVO edit = current.form();
        QuotationFormVO edited = new QuotationFormVO(edit.reference(), "更新的标题", edit.quotedAt(), null, null, null,
                List.of(new QuotationFormVO.Item(edit.items().getFirst().sourceItemId(), null, null, null,
                        new BigDecimal("3.0"), null, null)));
        QuotationDetailVO updated = quotes.update(edited);
        assertEquals("388.04", updated.document().totals().totalAmount());
        assertEquals(saved.getQuoteNumber(), updated.document().quoteNumber());
        assertEquals(1, updated.version());
        assertNull(updated.form().customer().getCustomerName());
        assertEquals("demo@example.test", jdbc.queryForObject("SELECT contact_email FROM quotation WHERE id = ?", String.class, saved.getId()));
        assertEquals(edit.items().getFirst().sourceItemId(), updated.form().items().getFirst().sourceItemId());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM quotation_item WHERE is_deleted = true", Integer.class));

        assertEquals("quotation.version-conflict", assertThrows(BusinessException.class, () -> quotes.update(edited)).getI18nCode());
        jdbc.update("UPDATE quotation SET order_generated = true, version = 2 WHERE id = ?", saved.getId());
        assertEquals("quotation.order-generated",
                assertThrows(BusinessException.class, () -> quotes.previewUpdate(updated.form())).getI18nCode());
        assertEquals("quotation.order-generated",
                assertThrows(BusinessException.class, () -> quotes.delete(new QuotationVersionedId(saved.getId(), 2L))).getI18nCode());

        QuotationDetailVO copied = quotes.copy(new QuotationCopyRequest(saved.getId(), true));
        assertNull(copied.document().quoteNumber());
        assertEquals("更新的标题", copied.form().title());
        assertEquals(1, this.count("quotation"));
        this.signIn(1L, QUOTE_CREATE);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> quotes.create(copied.form()));
        this.signIn(1L, QUOTE_QUERY, QUOTE_CREATE);
        QuotationDocumentVO copyPreview = quotes.preview(copied.form());
        assertEquals("388.04", copyPreview.totals().totalAmount());
        QuotationSummaryVO copySaved = quotes.create(copied.form());
        assertNotEquals(saved.getQuoteNumber(), copySaved.getQuoteNumber());
        assertFalse(quotes.detail(copySaved.getId()).orderGenerated());
        assertEquals(2, this.count("product_snapshot"));

        QuotationQueryVO query = new QuotationQueryVO();
        query.setQuotedFrom(QUOTED_AT.toLocalDate());
        query.setQuotedTo(QUOTED_AT.toLocalDate());
        query.setOrderGenerated(true);
        assertEquals(List.of(saved.getId()), quotes.list(query, 1, 15).getRecords().stream().map(QuotationSummaryVO::getId).toList());
        query.setQuotedFrom(QUOTED_AT.toLocalDate().minusDays(1));
        query.setQuotedTo(QUOTED_AT.toLocalDate().minusDays(1));
        assertEquals(0, quotes.list(query, 1, 15).getTotal());
        this.signIn(2L, QUOTE_QUERY);
        assertEquals("quotation.not-found", assertThrows(BusinessException.class, () -> quotes.detail(saved.getId())).getI18nCode());
        assertEquals("quotation.not-found", assertThrows(BusinessException.class, () -> quotes.copy(
                new QuotationCopyRequest(saved.getId(), false))).getI18nCode());
        assertEquals("quotation.not-found", assertThrows(BusinessException.class, () -> quotes.delete(
                new QuotationVersionedId(saved.getId(), 2L))).getI18nCode());
        assertTrue(quotes.list(new QuotationQueryVO(), 1, 15).getRecords().isEmpty());
    }

    @Test
    void filtersAvailableSkuOptionsAndPreservesLegacyOptions() {
        IProductSkuService skuService = context.getBean(IProductSkuService.class);
        ProductQuotationQueryVO query = new ProductQuotationQueryVO();
        assertEquals(3, skuService.quotationOptions(query, 1, 15).getTotal());
        query.setReferenceSaleCurrency("TWD");
        assertEquals(SKU_TWD, skuService.quotationOptions(query, 1, 15).getRecords().getFirst().getSkuId());
        query.setSkuId(SKU_A);
        assertEquals(0, skuService.quotationOptions(query, 1, 15).getTotal());
        assertEquals(4, skuService.options(query, 1, 15).getTotal());
        assertEquals("0.00", quotes.preview(this.form(this.line(SKU_TWD, "2.00", null, null))).totals().totalAmount());
        assertEquals("quotation.quantity-scale", assertThrows(BusinessException.class, () -> quotes.preview(
                this.form(this.line(SKU_B, "1.251", null, null)))).getI18nCode());
        assertEquals("quotation.currency-conflict", assertThrows(BusinessException.class, () -> quotes.preview(
                this.form(this.line(SKU_A, "1", null, null), this.line(SKU_TWD, "1", null, null)))).getI18nCode());
    }

    @Test
    void handlesConcurrentFirstNumberRollbackAndDailyLimit() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Taipei"));
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Future<QuotationSummaryVO> cny = executor.submit(() -> {
                this.signIn(1L, QUOTE_QUERY);
                start.await();
                return quotes.create(this.form(this.line(SKU_A, "1", null, null)));
            });
            Future<QuotationSummaryVO> twd = executor.submit(() -> {
                this.signIn(2L, QUOTE_QUERY);
                start.await();
                return quotes.create(this.form(this.line(SKU_TWD, "1", null, null)));
            });
            start.countDown();
            String cnyNumber = cny.get(15, TimeUnit.SECONDS).getQuoteNumber();
            String twdNumber = twd.get(15, TimeUnit.SECONDS).getQuoteNumber();
            assertNotEquals(cnyNumber.substring(cnyNumber.length() - 3), twdNumber.substring(twdNumber.length() - 3));
            assertEquals(List.of(1, 2), jdbc.queryForList("SELECT daily_sequence FROM quotation ORDER BY daily_sequence", Integer.class));
        }

        jdbc.execute("ALTER TABLE quotation_item ADD CONSTRAINT test_abort_save CHECK (remark IS DISTINCT FROM 'rollback')");
        try {
            QuotationFormVO abort = this.form(this.line(SKU_B, "2", null, "rollback"));
            assertThrows(DataIntegrityViolationException.class, () -> quotes.create(abort));
            assertEquals(2, this.count("quotation"));
            assertEquals(2, this.count("product_snapshot"));
        } finally {
            jdbc.execute("ALTER TABLE quotation_item DROP CONSTRAINT test_abort_save");
        }

        QuotationSummaryVO third = quotes.create(this.form(this.line(SKU_B, "1", null, null)));
        assertEquals(QuotationServiceImpl.formatQuoteNumber(today, (short) 3, CurrencyEnum.CNY), third.getQuoteNumber());
        quotes.delete(new QuotationVersionedId(third.getId(), 0L));
        QuotationSummaryVO fourth = quotes.create(this.form(this.line(SKU_B, "1", null, null)));
        assertTrue(fourth.getQuoteNumber().endsWith("-004"));
        jdbc.update("UPDATE quotation SET daily_sequence = 998 WHERE id = ?", fourth.getId());
        assertTrue(quotes.create(this.form(this.line(SKU_B, "1", null, null))).getQuoteNumber().endsWith("-999"));
        assertEquals("quotation.number-exhausted",
                assertThrows(BusinessException.class, () -> quotes.create(this.form(this.line(SKU_B, "1", null, null)))).getI18nCode());
        assertEquals("QT-1151231-001", QuotationServiceImpl.formatQuoteNumber(LocalDate.of(2026, 12, 31), (short) 1, CurrencyEnum.TWD));
        assertEquals("QT-20270101-001", QuotationServiceImpl.formatQuoteNumber(LocalDate.of(2027, 1, 1), (short) 1, CurrencyEnum.CNY));
    }

    @Test
    void retriesWholeTransactionAfterPostgresSerializationFailure() {
        jdbc.execute("""
                CREATE SEQUENCE quotation_retry_test_seq;
                CREATE FUNCTION quotation_retry_test() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF nextval('quotation_retry_test_seq') = 1 THEN
                        RAISE EXCEPTION 'retry this write' USING ERRCODE = '40001';
                    END IF;
                    RETURN NEW;
                END
                $$;
                CREATE TRIGGER quotation_retry_test
                    BEFORE INSERT ON quotation_item FOR EACH ROW EXECUTE FUNCTION quotation_retry_test();
                """);
        try {
            QuotationSummaryVO saved = quotes.create(this.form(this.line(SKU_A, "1", null, null)));
            assertTrue(saved.getQuoteNumber().endsWith("-001"));
            assertEquals(1, this.count("quotation"));
            assertEquals(1, this.count("quotation_item"));
            assertEquals(1, this.count("product_snapshot"));
            assertEquals(2L, jdbc.queryForObject("SELECT last_value FROM quotation_retry_test_seq", Long.class));
        } finally {
            jdbc.execute("""
                    DROP TRIGGER quotation_retry_test ON quotation_item;
                    DROP FUNCTION quotation_retry_test();
                    DROP SEQUENCE quotation_retry_test_seq;
                    """);
        }
    }

    @Test
    void verifiesSqlInstallUpgradePermissionsAndConstraints() {
        jdbc.execute("CREATE DATABASE quotation_incremental");
        DataSource incremental = postgres.getDatabase("postgres", "quotation_incremental");
        this.executeSql(incremental, "20261122_quotation.sql");
        JdbcTemplate newTables = new JdbcTemplate(incremental);
        newTables.update("""
                INSERT INTO quotation (id, quote_number, number_date, daily_sequence, owner_user_id, currency,
                    issuer_name, original_amount, subtotal_amount, total_amount)
                VALUES (1, 'QT-20261122-001', '2026-11-22', 1, 10, 'CNY', '演示开单方', 0, 0, 0)
                """);
        assertEquals(0L, newTables.queryForObject("SELECT version FROM quotation WHERE id = 1", Long.class));
        assertNull(newTables.queryForObject("SELECT contact_email FROM quotation WHERE id = 1", String.class));
        assertEquals(false, newTables.queryForObject("SELECT order_generated FROM quotation WHERE id = 1", Boolean.class));
        assertThrows(DataIntegrityViolationException.class, () -> newTables.update("UPDATE quotation SET version = -1 WHERE id = 1"));
        assertThrows(DataIntegrityViolationException.class, () -> newTables.update("UPDATE quotation SET daily_sequence = 1000 WHERE id = 1"));
        assertThrows(DataIntegrityViolationException.class, () -> newTables.update("""
                INSERT INTO quotation (id, quote_number, number_date, daily_sequence, owner_user_id, currency,
                    issuer_name, original_amount, subtotal_amount, total_amount)
                VALUES (2, 'QT-1151122-001', '2026-11-22', 1, 10, 'TWD', '演示开单方', 0, 0, 0)
                """));

        jdbc.execute("CREATE DATABASE quotation_legacy");
        DataSource legacy = postgres.getDatabase("postgres", "quotation_legacy");
        JdbcTemplate oldTables = new JdbcTemplate(legacy);
        oldTables.execute("""
                CREATE TABLE quotation (id bigint PRIMARY KEY, contact_email varchar(256));
                CREATE TABLE quotation_template (id bigint PRIMARY KEY, name varchar(128) NOT NULL);
                INSERT INTO quotation (id, contact_email) VALUES (1, 'demo@example.test');
                INSERT INTO quotation_template (id, name) VALUES (1, '存量模板');
                """);
        this.executeSql(legacy, "20261122_quotation_upgrade.sql");
        assertEquals("报价单", oldTables.queryForObject("SELECT title FROM quotation_template WHERE id = 1", String.class));
        assertEquals(false, oldTables.queryForObject("SELECT order_generated FROM quotation WHERE id = 1", Boolean.class));
        assertEquals(0L, oldTables.queryForObject("SELECT version FROM quotation WHERE id = 1", Long.class));
        assertEquals("demo@example.test", oldTables.queryForObject("SELECT contact_email FROM quotation WHERE id = 1", String.class));

        jdbc.execute("""
                INSERT INTO sys_menu (id, hierarchy, client_id, type, name)
                VALUES (2088186609251737602, '2088186609251737602', 'demo-erp', 1, '演示 ERP');
                INSERT INTO sys_role (id, role_name, role_code) VALUES (1, '演示管理员', 'SUPER_ADMIN');
                """);
        DataSource fresh = postgres.getPostgresDatabase();
        this.executeSql(fresh, "20261122_quotation_permissions.sql");
        this.executeSql(fresh, "20261122_quotation_permissions.sql");
        assertEquals(11, jdbc.queryForObject("SELECT count(*) FROM sys_menu WHERE permission_code LIKE 'quotation-manager%'", Integer.class));
        assertEquals(11, jdbc.queryForObject("SELECT count(*) FROM sys_role_permission WHERE role_code = 'SUPER_ADMIN'", Integer.class));
    }

    @Test
    void waitsForOrderWriteBeforeDeletingQuotation() throws Exception {
        QuotationSummaryVO saved = quotes.create(this.form(this.line(SKU_A, "1", null, null)));
        TransactionTemplate transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<BusinessException> deletion = transaction.execute(status -> {
                jdbc.update("UPDATE quotation SET order_generated = true, version = 1 WHERE id = ?", saved.getId());
                CountDownLatch attempting = new CountDownLatch(1);
                Future<BusinessException> future = executor.submit(() -> {
                    this.signIn(1L, QUOTE_QUERY);
                    attempting.countDown();
                    return assertThrows(BusinessException.class, () -> quotes.delete(new QuotationVersionedId(saved.getId(), 0L)));
                });
                assertTrue(assertDoesNotThrow(() -> attempting.await(5, TimeUnit.SECONDS)));
                assertThrows(TimeoutException.class, () -> future.get(150, TimeUnit.MILLISECONDS));
                return future;
            });
            assertEquals("quotation.order-generated", deletion.get(5, TimeUnit.SECONDS).getI18nCode());
        }
    }

    @Test
    void appliesDuplicateTemplateLinesAndReportsCurrentInvalidSkuPositions() throws Exception {
        QuotationTemplateVO form = this.template(null, null, "常用组合", "门店陈列",
                new BigDecimal("95.00"), "整单备注", List.of(
                this.templateItem(SKU_A, new BigDecimal("90"), "第一行"),
                this.templateItem(SKU_A, new BigDecimal("80"), null)
        ));
        QuotationTemplateVO saved = templates.create(Scope.PERSONAL, form);
        QuotationTemplateVO mixed = this.template(null, null, "币种变化", "公开报价",
                null, null, List.of(this.templateItem(SKU_A, null, null),
                this.templateItem(SKU_B, null, null)));
        QuotationTemplateVO publicTemplate = templates.create(Scope.PUBLIC, mixed);
        jdbc.update("UPDATE product_sku SET reference_sale_currency = 'TWD' WHERE id = ?", SKU_B);
        QuotationTemplateVO split = templates.detail(Scope.PUBLIC, publicTemplate.getId());
        assertEquals(List.of("CNY", "TWD"), split.getItems().stream().map(QuotationTemplateVO.Item::getReferenceSaleCurrency).toList());
        assertEquals(2, split.getIssueCount());
        assertEquals(List.of("CURRENCY_CONFLICT"), split.getItems().getFirst().getIssues());
        MvcResult response = mvc.perform(get("/quotation/template/public/detail")
                .with(authentication(this.user(2L))).param("id", publicTemplate.getId().toString())).andReturn();
        JSONObject firstItem = this.json(response).getJSONObject("data").getJSONArray("items").getJSONObject(0);
        assertEquals("CURRENCY_CONFLICT", firstItem.getJSONArray("issues").get(0));

        this.signIn(1L, QUOTE_QUERY);
        jdbc.update("UPDATE product_sku SET reference_sale_currency = 'CNY' WHERE id = ?", SKU_B);
        assertTrue(templates.detail(Scope.PUBLIC, publicTemplate.getId()).getUsable());
        assertEquals(0, this.count("quotation"));
        assertEquals(0, this.count("product_snapshot"));
        jdbc.update("UPDATE product_sku SET reference_sale_price = 120, version = 1 WHERE id = ?", SKU_A);
        jdbc.update("UPDATE product SET version = 1 WHERE id = 11");
        QuotationTemplateVO applied = templates.apply(Scope.PERSONAL, saved.getId());
        assertEquals("门店陈列", applied.getTitle());
        assertEquals("整单备注", applied.getRemark());
        assertEquals("第一行", applied.getItems().getFirst().getRemark());
        assertEquals("80", applied.getItems().getLast().getDiscountRate().toPlainString());
        List<QuotationFormVO.Item> lines = applied.getItems().stream().map(item -> new QuotationFormVO.Item(null,
                item.getProduct().getSkuId(), item.getProduct().getProductVersion(), item.getProduct().getSkuVersion(),
                new BigDecimal(item.getQuantity()), new QuotationFormVO.Discount(item.getDiscountRate(), null),
                item.getRemark())).toList();
        QuotationFormVO quoteForm = new QuotationFormVO(null, applied.getTitle(), QUOTED_AT, null, applied.getRemark(),
                new QuotationFormVO.Discount(applied.getDiscountRate(), null), lines);
        assertEquals("193.80", quotes.preview(quoteForm).totals().totalAmount());

        jdbc.update("UPDATE product_sku SET is_deleted = true WHERE id = ?", SKU_A);
        QuotationTemplateVO broken = templates.detail(Scope.PERSONAL, saved.getId());
        assertEquals(2, broken.getIssueCount());
        assertEquals(List.of("SKU_MISSING"), broken.getItems().getFirst().getIssues());
        assertEquals(SKU_A, broken.getItems().getFirst().getSkuId());
        assertEquals(1, broken.getItems().getLast().getSortOrder());
        assertFalse(templates.list(Scope.PERSONAL, null, 1, 15).getRecords().getFirst().getUsable());
        assertThrows(BusinessException.class, () -> templates.apply(Scope.PERSONAL, saved.getId()));

        QuotationTemplateVO repaired = this.template(saved.getId(), 0L, "常用组合二", "修复后的报价",
                null, null, List.of(this.templateItem(SKU_B, null, null)));
        QuotationTemplateVO updated = templates.update(Scope.PERSONAL, repaired);
        assertTrue(updated.getUsable());
        assertNull(updated.getDiscountRate());
        assertNull(updated.getRemark());
        assertEquals(1, updated.getVersion());
        assertThrows(BusinessException.class, () -> templates.update(Scope.PERSONAL, repaired));
        assertThrows(BusinessException.class, () -> templates.detail(Scope.PUBLIC, saved.getId()));
        this.signIn(2L, PERSONAL_QUERY);
        assertThrows(BusinessException.class, () -> templates.detail(Scope.PERSONAL, saved.getId()));
    }

    @Test
    void httpContractsEnforcePermissionsAndReturnExactEditorData() throws Exception {
        MvcResult denied = mvc.perform(post("/quotation/preview")
                        .with(authentication(this.user(1L, QUOTE_QUERY)))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(this.sampleForm())))
                .andReturn();
        assertEquals(403, this.json(denied).getIntValue("code"));

        Authentication editor = this.user(1L, QUOTE_CREATE, QUOTE_QUERY, QUOTE_UPDATE);
        String payload = JSON.toJSONString(this.sampleForm());
        MvcResult preview = mvc.perform(post("/quotation/preview").with(authentication(editor))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(header().string("Cache-Control", containsString("no-store"))).andReturn();
        JSONObject previewData = this.json(preview).getJSONObject("data");
        assertEquals("316.18", previewData.getJSONObject("totals").get("totalAmount"));
        assertEquals("95.00", previewData.getJSONObject("discount").get("discountRate"));
        assertFalse(previewData.getJSONArray("items").getJSONObject(0).containsKey("skuId"));

        MvcResult created = mvc.perform(post("/quotation/create").with(authentication(editor))
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andReturn();
        JSONObject reference = this.json(created).getJSONObject("data");
        assertEquals(Set.of("id", "quoteNumber"), reference.keySet());
        assertInstanceOf(String.class, reference.get("id"));
        Long quotationId = reference.getLong("id");
        jdbc.update("UPDATE quotation SET contact_email = ? WHERE id = ?", "demo@example.test", quotationId);
        MvcResult detail = mvc.perform(get("/quotation/detail").with(authentication(editor)).param("id", reference.getString("id")))
                .andExpect(header().string("Cache-Control", containsString("no-store"))).andReturn();
        JSONObject data = this.json(detail).getJSONObject("data");
        assertFalse(data.getJSONObject("form").getJSONObject("customer").containsKey("contactEmail"));
        assertFalse(data.getJSONObject("document").getJSONObject("customer").containsKey("contactEmail"));
        assertEquals(reference.getString("id"), data.getJSONObject("form").getJSONObject("reference").getString("id"));
        assertEquals(Long.toString(SKU_A), data.getJSONArray("skuIds").get(0));
        assertEquals("2", data.getJSONObject("form").getJSONArray("items").getJSONObject(0).get("quantity"));

        MvcResult copy = mvc.perform(post("/quotation/copy").with(authentication(editor)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"" + reference.getString("id") + "\",\"clearCustomer\":false}"))
                .andExpect(header().string("Cache-Control", containsString("no-store"))).andReturn();
        JSONObject copied = this.json(copy).getJSONObject("data");
        assertEquals(Set.of("form", "document", "skuIds"), copied.keySet());
        assertEquals("演示客户", copied.getJSONObject("form").getJSONObject("customer").getString("customerName"));
        assertFalse(copied.getJSONObject("form").getJSONObject("customer").containsKey("contactEmail"));

        mvc.perform(get("/quotation/template/public/list").with(anonymous())).andExpect(status().is4xxClientError());
        MvcResult publicList = mvc.perform(get("/quotation/template/public/list").with(authentication(this.user(2L, PUBLIC_QUERY)))).andReturn();
        assertTrue(this.json(publicList).getBooleanValue("success"));

        jdbc.update("UPDATE product SET version = 1 WHERE id = 11");
        MvcResult stale = mvc.perform(post("/quotation/preview").with(authentication(editor))
                .locale(Locale.SIMPLIFIED_CHINESE).contentType(MediaType.APPLICATION_JSON).content(payload)).andReturn();
        JSONObject error = this.json(stale);
        assertEquals(BusinessException.class, stale.getResolvedException().getClass());
        assertFalse(error.getBooleanValue("success"));
        assertEquals(500, error.getIntValue("code"));
        assertEquals("第 1 行的商品资料已变更，请刷新该行", error.getString("message"));
        assertNull(error.get("data"));

        MvcResult refreshed = mvc.perform(get("/quotation/sku/options").with(authentication(editor))
                .param("skuId", Long.toString(SKU_A))).andReturn();
        JSONObject currentSku = this.json(refreshed).getJSONObject("data").getJSONArray("records").getJSONObject(0);
        assertEquals(Long.toString(SKU_A), currentSku.get("skuId"));
        assertEquals(1, currentSku.getLongValue("productVersion"));

        jdbc.update("UPDATE product_sku SET status = 'DISABLED' WHERE id = ?", SKU_A);
        MvcResult unavailable = mvc.perform(post("/quotation/preview").with(authentication(editor))
                .locale(Locale.SIMPLIFIED_CHINESE).contentType(MediaType.APPLICATION_JSON).content(payload)).andReturn();
        JSONObject unavailableError = this.json(unavailable);
        assertEquals(BusinessException.class, unavailable.getResolvedException().getClass());
        assertFalse(unavailableError.getBooleanValue("success"));
        assertEquals("第 1 行的商品或 SKU 不可用，请刷新或替换", unavailableError.getString("message"));
        assertNull(unavailableError.get("data"));
    }

    @Test
    void defaultSecurityRequiresLoginForPublicTemplatesAndCreateAuthorityForApply() throws Exception {
        List<QuotationTemplateVO.Item> items = List.of(this.templateItem(SKU_A, null, null));
        QuotationTemplateVO form = this.template(null, null, "公共模板", "展厅报价", null, null, items);
        QuotationTemplateVO template = templates.create(Scope.PUBLIC, form);
        QuotationTemplateVO personalTemplate = templates.create(Scope.PERSONAL, form);
        String id = template.getId().toString();
        String apply = "{\"id\":\"" + id + "\"}";

        mvc.perform(get("/quotation/template/public/list").with(anonymous()))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/quotation/template/public/detail").with(anonymous()).param("id", id))
                .andExpect(status().is4xxClientError());
        mvc.perform(post("/quotation/template/public/apply").with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON).content(apply))
                .andExpect(status().is4xxClientError());

        Authentication reader = this.user(2L);
        MvcResult listDenied = mvc.perform(get("/quotation/template/public/list").with(authentication(reader))).andReturn();
        assertEquals(403, this.json(listDenied).getIntValue("code"));
        Authentication publicReader = this.user(2L, PUBLIC_QUERY);
        MvcResult listed = mvc.perform(get("/quotation/template/public/list").with(authentication(publicReader))).andReturn();
        assertTrue(this.json(listed).getBooleanValue("success"));
        MvcResult detail = mvc.perform(get("/quotation/template/public/detail").with(authentication(reader)).param("id", id))
                .andReturn();
        assertTrue(this.json(detail).getBooleanValue("success"));
        MvcResult denied = mvc.perform(post("/quotation/template/public/apply").with(authentication(reader))
                .contentType(MediaType.APPLICATION_JSON).content(apply)).andReturn();
        assertEquals(403, this.json(denied).getIntValue("code"));

        Authentication creator = this.user(2L, QUOTE_CREATE);
        MvcResult applied = mvc.perform(post("/quotation/template/public/apply").with(authentication(creator))
                .contentType(MediaType.APPLICATION_JSON).content(apply)).andReturn();
        assertTrue(this.json(applied).getBooleanValue("success"));

        String personalApply = "{\"id\":\"" + personalTemplate.getId() + "\"}";
        Authentication personalReader = this.user(1L, PERSONAL_QUERY);
        MvcResult personalDenied = mvc.perform(post("/quotation/template/personal/apply").with(authentication(personalReader))
                .contentType(MediaType.APPLICATION_JSON).content(personalApply)).andReturn();
        assertEquals(403, this.json(personalDenied).getIntValue("code"));

        Authentication personalCreator = this.user(1L, PERSONAL_QUERY, QUOTE_CREATE);
        MvcResult personalApplied = mvc.perform(post("/quotation/template/personal/apply").with(authentication(personalCreator))
                .contentType(MediaType.APPLICATION_JSON).content(personalApply)).andReturn();
        assertTrue(this.json(personalApplied).getBooleanValue("success"));
    }

    @Test
    void httpAcceptsDiscountBoundariesAndChecksTemplateAuthorities() throws Exception {
        String publicMaintain = "sales-manager:quotation-template:public:create";
        String publicUpdate = "sales-manager:quotation-template:public:update";
        String remark = "范".repeat(1024);
        String payload = """
                {"name":"示例模板","title":"演示报价","discountRate":"95.55","remark":"%s",
                 "scope":"PERSONAL","version":99,"ownerUserId":"2","items":[
                    {"skuId":"%s","discountRate":"0.00","remark":"%s"},
                    {"skuId":"%s","discountRate":"100.00"}
                ]}
                """.formatted(remark, SKU_A, remark, SKU_A);
        Authentication maintainer = this.user(1L, publicMaintain);
        MvcResult created = mvc.perform(post("/quotation/template/public/create").with(authentication(maintainer))
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andReturn();
        JSONObject template = this.json(created).getJSONObject("data");
        assertEquals("PUBLIC", template.getString("scope"));
        assertEquals(0L, template.getLongValue("version"));
        assertFalse(template.containsKey("ownerUserId"));
        assertEquals("95.55", template.get("discountRate"));
        assertEquals(remark, template.getString("remark"));
        assertEquals(remark, template.getJSONArray("items").getJSONObject(0).getString("remark"));
        assertEquals("0", template.getJSONArray("items").getJSONObject(0).get("discountRate"));
        assertEquals("100", template.getJSONArray("items").getJSONObject(1).get("discountRate"));

        for (String invalid : List.of(payload.replace("95.55", "95.555"), payload.replace("95.55", "100.01"),
                payload.replace(remark, remark + "长"))) {
            MvcResult rejected = mvc.perform(post("/quotation/template/public/create").with(authentication(maintainer))
                    .contentType(MediaType.APPLICATION_JSON).content(invalid)).andReturn();
            assertFalse(this.json(rejected).getBooleanValue("success"));
        }
        assertEquals(1, this.count("quotation_template"));
        String id = template.getString("id");
        String apply = "{\"id\":\"" + id + "\"}";
        MvcResult denied = mvc.perform(post("/quotation/template/public/apply").with(authentication(maintainer))
                .contentType(MediaType.APPLICATION_JSON).content(apply)).andReturn();
        assertEquals(403, this.json(denied).getIntValue("code"));
        MvcResult applied = mvc.perform(post("/quotation/template/public/apply").with(authentication(this.user(2L, QUOTE_CREATE)))
                .contentType(MediaType.APPLICATION_JSON).content(apply)).andReturn();
        JSONObject application = this.json(applied).getJSONObject("data");
        assertEquals(Set.of("title", "discountRate", "remark", "items"), application.keySet());
        assertEquals("演示报价", application.getString("title"));
        JSONObject appliedItem = application.getJSONArray("items").getJSONObject(0);
        assertEquals(Set.of("product", "quantity", "discountRate", "remark"), appliedItem.keySet());
        assertEquals("1", appliedItem.get("quantity"));

        MvcResult listed = mvc.perform(get("/quotation/template/public/list").with(authentication(this.user(2L, PUBLIC_QUERY)))).andReturn();
        JSONObject summary = this.json(listed).getJSONObject("data").getJSONArray("records").getJSONObject(0);
        assertEquals(Set.of("id", "name", "title", "scope", "version", "ownedByCurrentUser",
                "gmtModified", "usable", "issueCount"), summary.keySet());
        assertEquals(Boolean.FALSE, summary.getBoolean("ownedByCurrentUser"));

        String update = """
                {"id":"%s","expectedVersion":0,"name":"改名","title":"新标题","items":[{"skuId":"%s"}]}
                """.formatted(id, SKU_B);
        Authentication otherMaintainer = this.user(2L, publicUpdate);
        MvcResult updated = mvc.perform(post("/quotation/template/public/update").with(authentication(otherMaintainer))
                .contentType(MediaType.APPLICATION_JSON).content(update)).andReturn();
        assertEquals("新标题", this.json(updated).getJSONObject("data").getString("title"));
        assertEquals(0, this.count("quotation"));
    }

    @Test
    void onlyCreatorsWithScopePermissionCanDeleteQuotationTemplates() throws Exception {
        List<QuotationTemplateVO.Item> items = List.of(this.templateItem(SKU_A, null, null));
        QuotationTemplateVO form = this.template(null, null, "删除测试模板", "展示报价", null, null, items);
        QuotationTemplateVO personal = templates.create(Scope.PERSONAL, form);
        QuotationTemplateVO shared = templates.create(Scope.PUBLIC, form);
        Authentication ownerWithoutDelete = this.user(1L, PERSONAL_QUERY, PUBLIC_QUERY);
        Authentication owner = this.user(1L, PERSONAL_QUERY, PUBLIC_QUERY, PERSONAL_DELETE, PUBLIC_DELETE, QUOTE_CREATE);
        Authentication other = this.user(2L, PERSONAL_DELETE, PUBLIC_DELETE);

        for (Scope scope : Scope.values()) {
            boolean isPersonal = scope == Scope.PERSONAL;
            String prefix = isPersonal ? "/quotation/template/personal" : "/quotation/template/public";
            String otherPrefix = isPersonal ? "/quotation/template/public" : "/quotation/template/personal";
            QuotationTemplateVO template = isPersonal ? personal : shared;
            Long id = template.getId();
            String request = "{\"id\":\"%s\",\"expectedVersion\":0}".formatted(id);

            MvcResult detail = mvc.perform(get(prefix + "/detail").with(authentication(owner))
                    .param("id", id.toString())).andReturn();
            assertEquals(Boolean.TRUE, this.json(detail).getJSONObject("data").getBoolean("ownedByCurrentUser"));

            MvcResult noPermission = mvc.perform(post(prefix + "/delete").with(authentication(ownerWithoutDelete))
                    .contentType(MediaType.APPLICATION_JSON).content(request)).andReturn();
            assertEquals(403, this.json(noPermission).getIntValue("code"));

            MvcResult notOwner = mvc.perform(post(prefix + "/delete").with(authentication(other))
                    .contentType(MediaType.APPLICATION_JSON).content(request)).andReturn();
            BusinessException ownerError = assertInstanceOf(BusinessException.class, notOwner.getResolvedException());
            assertEquals("quotation.template.not-found", ownerError.getI18nCode());

            MvcResult wrongScope = mvc.perform(post(otherPrefix + "/delete").with(authentication(owner))
                    .contentType(MediaType.APPLICATION_JSON).content(request)).andReturn();
            BusinessException scopeError = assertInstanceOf(BusinessException.class, wrongScope.getResolvedException());
            assertEquals("quotation.template.not-found", scopeError.getI18nCode());

            String staleRequest = "{\"id\":\"%s\",\"expectedVersion\":1}".formatted(id);
            MvcResult staleVersion = mvc.perform(post(prefix + "/delete").with(authentication(owner))
                    .contentType(MediaType.APPLICATION_JSON).content(staleRequest)).andReturn();
            BusinessException versionError = assertInstanceOf(BusinessException.class, staleVersion.getResolvedException());
            assertEquals("quotation.template.version-conflict", versionError.getI18nCode());

            MvcResult removed = mvc.perform(post(prefix + "/delete").with(authentication(owner))
                    .contentType(MediaType.APPLICATION_JSON).content(request)).andReturn();
            assertTrue(this.json(removed).getBooleanValue("success"));
            assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT is_deleted FROM quotation_template WHERE id = ?", Boolean.class, id));
            assertEquals(1L, jdbc.queryForObject("SELECT version FROM quotation_template WHERE id = ?", Long.class, id));
            assertEquals(0, jdbc.queryForObject("""
                    SELECT count(*) FROM quotation_template_item WHERE template_id = ? AND is_deleted = false
                    """, Integer.class, id));

            MvcResult listed = mvc.perform(get(prefix + "/list").with(authentication(owner))).andReturn();
            assertTrue(this.json(listed).getJSONObject("data").getJSONArray("records").isEmpty());
            MvcResult deletedDetail = mvc.perform(get(prefix + "/detail").with(authentication(owner))
                    .param("id", id.toString())).andReturn();
            BusinessException detailError = assertInstanceOf(BusinessException.class, deletedDetail.getResolvedException());
            assertEquals("quotation.template.not-found", detailError.getI18nCode());

            String apply = "{\"id\":\"%s\"}".formatted(id);
            MvcResult applied = mvc.perform(post(prefix + "/apply").with(authentication(owner))
                    .contentType(MediaType.APPLICATION_JSON).content(apply)).andReturn();
            BusinessException applyError = assertInstanceOf(BusinessException.class, applied.getResolvedException());
            assertEquals("quotation.template.not-found", applyError.getI18nCode());

            MvcResult repeated = mvc.perform(post(prefix + "/delete").with(authentication(owner))
                    .contentType(MediaType.APPLICATION_JSON).content(request)).andReturn();
            BusinessException repeatError = assertInstanceOf(BusinessException.class, repeated.getResolvedException());
            assertEquals("quotation.template.not-found", repeatError.getI18nCode());
        }
    }

    private QuotationTemplateVO template(
            Long id, Long expectedVersion, String name, String title, BigDecimal rate, String remark,
            List<QuotationTemplateVO.Item> items
    ) {
        QuotationTemplateVO form = new QuotationTemplateVO();
        form.setId(id);
        form.setExpectedVersion(expectedVersion);
        form.setName(name);
        form.setTitle(title);
        form.setDiscountRate(rate);
        form.setRemark(remark);
        form.setItems(items);
        return form;
    }

    private QuotationTemplateVO.Item templateItem(Long skuId, BigDecimal rate, String remark) {
        QuotationTemplateVO.Item item = new QuotationTemplateVO.Item();
        item.setSkuId(skuId);
        item.setDiscountRate(rate);
        item.setRemark(remark);
        return item;
    }

    private void executeSql(DataSource dataSource, String name) {
        FileSystemResource script = new FileSystemResource("docs/sql/" + name);
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(script);
        populator.execute(dataSource);
    }

    private JSONObject json(MvcResult response) throws Exception {
        return JSON.parseObject(response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void sku(long id, long product, String code, int scale, String price, String currency) {
        BigDecimal amount = price == null ? null : new BigDecimal(price);
        jdbc.update("""
                INSERT INTO product_sku (id, product_id, code, unit_id, quantity_scale,
                    reference_sale_price, reference_sale_currency, specification_key, status)
                VALUES (?, ?, ?, 1, ?, ?, ?, repeat('0', 64), 'ENABLED')
                """, id, product, code, scale, amount, currency);
    }

    private QuotationFormVO sampleForm() {
        QuotationCustomerVO customer = new QuotationCustomerVO();
        customer.setCustomerName("演示客户");
        customer.setContactPhone("0900-000-000");
        customer.setShippingAddress("演示地址 1 号");
        QuotationFormVO.Discount order = new QuotationFormVO.Discount(new BigDecimal("95.00"), null);
        QuotationFormVO.Discount lineRate = new QuotationFormVO.Discount(new BigDecimal("90"), null);
        QuotationFormVO.Discount lineFixed = new QuotationFormVO.Discount(null, new BigDecimal("100"));
        return new QuotationFormVO(null, "展厅储物设备报价", QUOTED_AT, customer, "交期另行确认", order,
                List.of(this.line(SKU_A, "2.00", lineRate, "贴标签"), this.line(SKU_B, "1.500", lineFixed, null)));
    }

    private QuotationFormVO form(QuotationFormVO.Item... items) {
        return new QuotationFormVO(null, null, QUOTED_AT, null, null, null, List.of(items));
    }

    private QuotationFormVO.Item line(long sku, String quantity, QuotationFormVO.Discount discount, String remark) {
        return new QuotationFormVO.Item(null, sku, 0L, 0L, new BigDecimal(quantity), discount, remark);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private void signIn(long userId, String... permissions) {
        Authentication authentication = this.user(userId, permissions);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Authentication user(long id, String... permissions) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList();
        LoginUser user = new LoginUser("tester", "", true, authorities);
        user.setId(id);
        return UsernamePasswordAuthenticationToken.authenticated(user, null, authorities);
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan({"com.muzhi.minierp.vo.product", "com.muzhi.minierp.entity.product", "com.muzhi.minierp.entity.sales"})
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    @EnableTransactionManagement(proxyTargetClass = true)
    @Import({
            MybatisPlusConfig.class, MapstructAutoConfiguration.class, FastJsonWebMvcConfiguration.class,
            QuotationIssuerProperties.class, QuotationController.class, QuotationTemplateController.class,
            QuotationServiceImpl.class, QuotationTemplateServiceImpl.class, QuotationItemServiceImpl.class,
            ProductSkuServiceImpl.class, ProductSnapshotServiceImpl.class, ProductImageServiceImpl.class,
            ProductUnitServiceImpl.class, I18nHelper.class, GlobalExceptionHandler.class, VaultErrorWriter.class
    })
    static class TestConfiguration {

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource, MybatisPlusConfig config) throws Exception {
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPlugins(config.mybatisPlusInterceptor());
            com.baomidou.mybatisplus.core.config.GlobalConfig global = new com.baomidou.mybatisplus.core.config.GlobalConfig();
            global.setMetaObjectHandler(config.metaObjectHandler());
            com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig db = new com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig();
            db.setLogicDeleteValue("true");
            db.setLogicNotDeleteValue("false");
            global.setDbConfig(db);
            factory.setGlobalConfig(global);
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:mapper/**/*.xml");
            factory.setMapperLocations(resources);
            return factory.getObject();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        IAttachmentService attachmentService() {
            IAttachmentService service = mock(IAttachmentService.class);
            when(service.getAuthorizedUrls(anyList())).thenReturn(Map.of());
            return service;
        }

        @Bean
        ResourceBundleMessageSource messageSource() {
            ResourceBundleMessageSource source = new ResourceBundleMessageSource();
            source.setBasename("i18n/quotation/messages");
            source.setDefaultEncoding("UTF-8");
            return source;
        }

        @Bean
        SecurityFilterChain security(HttpSecurity http) {
            http.csrf(AbstractHttpConfigurer::disable);
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }
}
