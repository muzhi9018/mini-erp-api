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
import com.muzhi.minierp.enums.ProductEnum;
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
import com.muzhi.minierp.vo.product.ProductOptionSpecVO;
import com.muzhi.minierp.vo.product.ProductQuotationQueryVO;
import com.muzhi.minierp.vo.product.ProductSnapshotImageVO;
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
import java.util.HashMap;
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
        IAttachmentService attachments = context.getBean(IAttachmentService.class);
        when(attachments.getAuthorizedUrls(anyList())).thenReturn(Map.of());
        jdbc.execute("""
                TRUNCATE quotation, quotation_item, quotation_template, quotation_template_item,
                    product, product_sku, product_unit, product_category, product_image, product_snapshot,
                    product_snapshot_spec, product_snapshot_image, product_spec, product_spec_value, product_sku_spec_value;
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
        QuotationVO source = this.sampleForm();
        QuotationVO preview = quotes.preview(source);
        assertNull(preview.getQuoteNumber());
        assertEquals("316.18", preview.getTotalAmount().toPlainString());
        assertEquals("232.82", preview.getItems().getFirst().getLineAmount().toPlainString());
        assertEquals("332.82", preview.getSubtotalAmount().toPlainString());
        assertEquals(0, this.count("quotation"));
        assertEquals(0, this.count("product_snapshot"));

        QuotationVO saved = quotes.create(preview);
        QuotationVO detail = quotes.detail(saved.getId());
        assertEquals(source.getQuotedAt(), detail.getQuotedAt());
        assertEquals(0, detail.getVersion());
        assertFalse(detail.getOrderGenerated());
        assertEquals(saved.getQuoteNumber(), detail.getQuoteNumber());
        assertEquals("演示客户", detail.getCustomerName());
        assertEquals(2, this.count("product_snapshot"));

        QuotationVO copied = quotes.copy(this.copyRequest(saved.getId(), false));
        QuotationVO copyPreview = quotes.preview(copied);
        assertEquals(copied.getSourceId(), copyPreview.getSourceId());
        assertEquals(copied.getSourceVersion(), copyPreview.getSourceVersion());
        assertEquals(copied.getItems().getFirst().getId(), copyPreview.getItems().getFirst().getId());
        QuotationVO copiedSaved = quotes.create(copyPreview);
        assertNotEquals(saved.getId(), copiedSaved.getId());
        assertNotEquals(saved.getItems().getFirst().getId(), copiedSaved.getItems().getFirst().getId());
        assertEquals(2, this.count("quotation"));
    }

    @Test
    void copiesTheSameHistoricalItemIntoIndependentPricedRows() {
        QuotationItemVO sourceItem = this.line(SKU_A, "1", null, null);
        QuotationVO sourceForm = this.form(sourceItem);
        QuotationVO source = quotes.create(sourceForm);
        QuotationVO copied = quotes.copy(this.copyRequest(source.getId(), false));
        QuotationItemVO first = copied.getItems().getFirst();
        QuotationItemVO second = new QuotationItemVO();
        second.setId(first.getId());
        second.setQuantity(new BigDecimal("2"));
        second.setDiscountRate(new BigDecimal("50"));
        second.setUnitPrice(BigDecimal.ONE);
        copied.setItems(List.of(first, second));

        QuotationVO preview = quotes.preview(copied);
        QuotationItemVO previewFirst = preview.getItems().getFirst();
        QuotationItemVO previewSecond = preview.getItems().getLast();
        assertNotSame(previewFirst, previewSecond);
        assertEquals(BigDecimal.ONE, previewFirst.getQuantity());
        assertEquals(new BigDecimal("2"), previewSecond.getQuantity());
        assertNull(previewFirst.getDiscountRate());
        assertEquals(new BigDecimal("50"), previewSecond.getDiscountRate());
        assertEquals("129.345", previewSecond.getUnitPrice().toPlainString());
        assertEquals("258.70", preview.getTotalAmount().toPlainString());

        QuotationVO saved = quotes.create(preview);
        assertNotEquals(saved.getItems().getFirst().getId(), saved.getItems().getLast().getId());
        assertNotEquals(first.getId(), saved.getItems().getFirst().getId());
        assertEquals(BigDecimal.ONE, saved.getItems().getFirst().getQuantity());
        assertEquals(new BigDecimal("2"), saved.getItems().getLast().getQuantity());
        assertEquals("258.70", saved.getTotalAmount().toPlainString());
        QuotationVO original = quotes.detail(source.getId());
        assertEquals(BigDecimal.ONE, original.getItems().getFirst().getQuantity());
        assertEquals(3, this.count("quotation_item"));
    }

    @Test
    void returnsSnapshotImagesWithFreshUrlsForEachQuotationItem() throws Exception {
        String imageFixtures = """
                INSERT INTO product_image (id, image_type, owner_id, attachment_id, is_cover, sort_order) VALUES
                    (101, 'SKU', %d, 1001, false, 1),
                    (102, 'PRODUCT', 11, 1002, true, 0),
                    (103, 'SKU', %d, 1003, true, 0);
                """.formatted(SKU_A, SKU_A);
        jdbc.execute(imageFixtures);
        IAttachmentService attachments = context.getBean(IAttachmentService.class);
        when(attachments.getAuthorizedUrls(anyList())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            Map<Long, String> urls = new HashMap<>();
            for (Long id : ids) {
                urls.put(id, "https://images.example.test/" + id);
            }
            return urls;
        });

        QuotationVO source = this.sampleForm();
        QuotationVO preview = quotes.preview(source);
        List<ProductSnapshotImageVO> previewImages = preview.getItems().getFirst().getImages();
        assertEquals(List.of(1003L, 1001L), previewImages.stream().map(ProductSnapshotImageVO::getAttachmentId).toList());
        assertEquals(ProductEnum.ImageType.SKU.getCode(), previewImages.getFirst().getImageSource());
        assertEquals("https://images.example.test/1003", previewImages.getFirst().getUrl());
        assertTrue(preview.getItems().get(1).getImages().isEmpty());
        assertEquals("https://images.example.test/1003", preview.getItems().getFirst().getProductImageUrl());
        assertNull(preview.getItems().get(1).getProductImageUrl());

        QuotationVO saved = quotes.create(source);
        jdbc.execute("UPDATE product_image SET is_deleted = true");
        jdbc.update("UPDATE product_sku SET is_deleted = true WHERE id = ?", SKU_A);
        QuotationVO detail = quotes.detail(saved.getId());
        List<ProductSnapshotImageVO> images = detail.getItems().getFirst().getImages();
        assertEquals(List.of(1002L, 1003L, 1001L), images.stream().map(ProductSnapshotImageVO::getAttachmentId).toList());
        assertEquals(ProductEnum.ImageType.PRODUCT.getCode(), images.getFirst().getImageSource());
        assertEquals(ProductEnum.ImageType.SKU.getCode(), images.get(1).getImageSource());
        assertEquals(List.of(true, true, false), images.stream().map(ProductSnapshotImageVO::getIsCover).toList());
        assertEquals(List.of(0, 0, 1), images.stream().map(ProductSnapshotImageVO::getSortOrder).toList());
        assertTrue(detail.getItems().get(1).getImages().isEmpty());
        assertEquals("https://images.example.test/1003", detail.getItems().getFirst().getProductImageUrl());
        assertNull(detail.getItems().get(1).getProductImageUrl());

        when(attachments.getAuthorizedUrls(anyList())).thenReturn(Map.of(1002L, "https://images.example.test/refreshed"));
        MvcResult response = mvc.perform(get("/quotation/detail")
                .with(authentication(this.user(1L, QUOTE_QUERY)))
                .param("id", saved.getId().toString())).andReturn();
        JSONObject document = this.json(response).getJSONObject("data");
        JSONObject image = document.getJSONArray("items").getJSONObject(0).getJSONArray("images").getJSONObject(0);
        assertEquals(1002L, image.getLongValue("attachmentId"));
        assertEquals("https://images.example.test/refreshed", image.getString("url"));
        assertEquals(Set.of("id", "snapshotId", "sourceImageId", "attachmentId", "imageSource", "isCover", "sortOrder", "url",
                "gmtCreate", "gmtModified", "createUser", "updateUser", "isDeleted"), image.keySet());
        assertEquals(0, document.getJSONArray("items").getJSONObject(1).getJSONArray("images").size());
        assertNull(document.getJSONArray("items").getJSONObject(0).getString("productImageUrl"));

        this.signIn(1L, QUOTE_QUERY);
        QuotationVO copied = quotes.copy(this.copyRequest(saved.getId(), false));
        assertEquals(images.size(), copied.getItems().getFirst().getImages().size());
        assertNull(copied.getItems().getFirst().getImages().get(1).getUrl());
        QuotationVO updatePreview = quotes.previewUpdate(detail);
        assertEquals(images.size(), updatePreview.getItems().getFirst().getImages().size());
        QuotationVO updated = quotes.update(detail);
        assertEquals(images.size(), updated.getItems().getFirst().getImages().size());
    }

    @Test
    void keepsCoverImageSnapshotWhenCurrentSkuCoverChanges() throws Exception {
        String imageFixtures = """
                INSERT INTO product_image (id, image_type, owner_id, attachment_id, is_cover, sort_order) VALUES
                    (101, 'SKU', %d, 1001, false, 0),
                    (102, 'PRODUCT', 11, 1002, true, 0),
                    (103, 'SKU', %d, 1003, true, 1);
                """.formatted(SKU_A, SKU_A);
        jdbc.execute(imageFixtures);
        IAttachmentService attachments = context.getBean(IAttachmentService.class);
        when(attachments.getAuthorizedUrls(anyList())).thenReturn(Map.of(
                1001L, "https://images.example.test/extra",
                1002L, "https://images.example.test/product",
                1003L, "https://images.example.test/original",
                1004L, "https://images.example.test/replacement"
        ));
        QuotationVO source = this.sampleForm();
        QuotationVO preview = quotes.preview(source);
        assertEquals("https://images.example.test/original", preview.getItems().getFirst().getProductImageUrl());
        QuotationVO saved = quotes.create(source);
        assertEquals("https://images.example.test/original", saved.getItems().getFirst().getProductImageUrl());

        jdbc.execute("UPDATE product_image SET is_deleted = true WHERE image_type = 'SKU'");
        jdbc.update("INSERT INTO product_image (id, image_type, owner_id, attachment_id, is_cover, sort_order) VALUES (104, 'SKU', ?, 1004, true, 0)", SKU_A);
        jdbc.update("UPDATE product SET version = 1 WHERE id = 11");
        jdbc.update("UPDATE product_sku SET version = 1 WHERE id = ?", SKU_A);
        QuotationItemVO currentItem = source.getItems().getFirst();
        currentItem.setProductVersion(1L);
        currentItem.setSkuVersion(1L);
        QuotationVO freshPreview = quotes.preview(source);
        assertEquals("https://images.example.test/replacement", freshPreview.getItems().getFirst().getProductImageUrl());
        QuotationVO history = quotes.detail(saved.getId());
        assertEquals("https://images.example.test/original", history.getItems().getFirst().getProductImageUrl());
        QuotationVO updatePreview = quotes.previewUpdate(history);
        assertEquals("https://images.example.test/original", updatePreview.getItems().getFirst().getProductImageUrl());
        QuotationVO updated = quotes.update(history);
        assertEquals("https://images.example.test/original", updated.getItems().getFirst().getProductImageUrl());

        QuotationVO copy = quotes.copy(this.copyRequest(saved.getId(), false));
        copy.setItems(List.of(copy.getItems().getFirst(), currentItem));
        QuotationVO copyPreview = quotes.preview(copy);
        assertEquals(List.of("https://images.example.test/original", "https://images.example.test/replacement"),
                copyPreview.getItems().stream().map(QuotationItemVO::getProductImageUrl).toList());
        QuotationVO copied = quotes.create(copy);
        assertEquals(List.of("https://images.example.test/original", "https://images.example.test/replacement"),
                copied.getItems().stream().map(QuotationItemVO::getProductImageUrl).toList());

        MvcResult response = mvc.perform(get("/quotation/detail")
                .with(authentication(this.user(1L, QUOTE_QUERY)))
                .param("id", saved.getId().toString())).andReturn();
        JSONObject document = this.json(response).getJSONObject("data");
        assertEquals("https://images.example.test/original",
                document.getJSONArray("items").getJSONObject(0).getString("productImageUrl"));

        this.signIn(1L, QUOTE_QUERY);
        jdbc.update("DELETE FROM product_snapshot_image WHERE image_source = 'SKU' AND snapshot_id = ?",
                history.getItems().getFirst().getSnapshotId());
        QuotationVO productOnly = quotes.detail(saved.getId());
        assertEquals("https://images.example.test/product", productOnly.getItems().getFirst().getProductImageUrl());
    }

    @Test
    void previewsProductCoverForSkusWithoutTheirOwnImagesAndKeepsItInHistory() {
        jdbc.execute("INSERT INTO product_image (id, image_type, owner_id, attachment_id, is_cover, sort_order) VALUES (101, 'PRODUCT', 11, 1001, true, 0)");
        IAttachmentService attachments = context.getBean(IAttachmentService.class);
        when(attachments.getAuthorizedUrls(anyList())).thenReturn(Map.of(
                1001L, "https://images.example.test/product-original",
                1002L, "https://images.example.test/product-replacement"
        ));
        QuotationVO source = this.sampleForm();
        QuotationVO preview = quotes.preview(source);
        assertEquals("https://images.example.test/product-original", preview.getItems().getFirst().getProductImageUrl());
        ProductSnapshotImageVO previewImage = preview.getItems().getFirst().getImages().getFirst();
        assertEquals(ProductEnum.ImageType.PRODUCT.getCode(), previewImage.getImageSource());
        assertEquals(101L, previewImage.getSourceImageId());
        assertNull(preview.getItems().get(1).getProductImageUrl());
        QuotationVO saved = quotes.create(source);
        assertEquals("https://images.example.test/product-original", saved.getItems().getFirst().getProductImageUrl());

        jdbc.execute("UPDATE product_image SET is_deleted = true WHERE id = 101");
        jdbc.execute("INSERT INTO product_image (id, image_type, owner_id, attachment_id, is_cover, sort_order) VALUES (102, 'PRODUCT', 11, 1002, true, 0)");
        jdbc.execute("UPDATE product SET version = 1 WHERE id = 11");
        source.getItems().getFirst().setProductVersion(1L);
        assertEquals("https://images.example.test/product-replacement", quotes.preview(source).getItems().getFirst().getProductImageUrl());
        QuotationVO history = quotes.detail(saved.getId());
        assertEquals("https://images.example.test/product-original", history.getItems().getFirst().getProductImageUrl());
        assertEquals("https://images.example.test/product-original", quotes.previewUpdate(history).getItems().getFirst().getProductImageUrl());
        QuotationVO copied = quotes.copy(this.copyRequest(saved.getId(), false));
        assertEquals("https://images.example.test/product-original", copied.getItems().getFirst().getProductImageUrl());
        assertEquals("https://images.example.test/product-original", quotes.preview(copied).getItems().getFirst().getProductImageUrl());
    }

    @Test
    void returnsOrderedCurrentAndFrozenSpecsForPreviewHistoryAndReusedLines() throws Exception {
        jdbc.execute("""
                INSERT INTO product_unit (id, code, name) VALUES (2, 'MM', 'mm');
                INSERT INTO product_spec (id, product_id, name, value_type, unit_id, sort_order) VALUES
                    (103, 11, '门数', 'TEXT', NULL, 2),
                    (102, 11, '宽度', 'NUMBER', 2, 1),
                    (101, 11, '颜色', 'TEXT', NULL, 0);
                INSERT INTO product_spec_value (id, spec_id, text_value, number_value, normalized_value) VALUES
                    (201, 101, '石墨灰', NULL, '石墨灰'),
                    (202, 102, NULL, 900, '900'),
                    (203, 103, '2 门', NULL, '2 门');
                """);
        String selections = """
                INSERT INTO product_sku_spec_value (id, sku_id, spec_id, spec_value_id) VALUES
                    (301, %d, 101, 201), (302, %d, 102, 202), (303, %d, 103, 203);
                """.formatted(SKU_A, SKU_A, SKU_A);
        jdbc.execute(selections);
        jdbc.update("UPDATE product_sku SET model = 'LM-02' WHERE id = ?", SKU_A);
        QuotationVO source = this.sampleForm();
        QuotationVO preview = quotes.preview(source);
        assertNotNull(preview.getItems().getFirst().getSpecs());
        assertEquals(List.of("颜色", "宽度", "门数"), preview.getItems().getFirst().getSpecs().stream()
                .map(ProductOptionSpecVO::getSpecName).toList());
        String frozen = JSON.toJSONString(preview.getItems().getFirst().getSpecs());
        assertTrue(preview.getItems().get(1).getSpecs().isEmpty());
        QuotationVO saved = quotes.create(source);
        assertEquals(frozen, JSON.toJSONString(saved.getItems().getFirst().getSpecs()));
        QuotationVO history = quotes.detail(saved.getId());
        assertEquals(frozen, JSON.toJSONString(history.getItems().getFirst().getSpecs()));

        jdbc.execute("UPDATE product_spec SET name = '当前颜色' WHERE id = 101");
        jdbc.execute("UPDATE product_spec SET sort_order = 2 - sort_order");
        jdbc.execute("UPDATE product_spec_value SET text_value = '暖白', normalized_value = '暖白' WHERE id = 201");
        jdbc.execute("UPDATE product_spec_value SET number_value = 1000, normalized_value = '1000' WHERE id = 202");
        jdbc.execute("UPDATE product_unit SET name = '毫米' WHERE id = 2");
        jdbc.execute("UPDATE product SET version = 1 WHERE id = 11");
        jdbc.update("UPDATE product_sku SET version = 1, model = 'LM-03' WHERE id = ?", SKU_A);
        source.getItems().getFirst().setProductVersion(1L);
        source.getItems().getFirst().setSkuVersion(1L);
        QuotationVO current = quotes.preview(source);
        String currentSpecs = JSON.toJSONString(current.getItems().getFirst().getSpecs());
        assertNotEquals(frozen, currentSpecs);
        assertEquals("门数", current.getItems().getFirst().getSpecs().getFirst().getSpecName());
        assertEquals("毫米", current.getItems().getFirst().getSpecs().get(1).getUnitName());
        assertEquals(frozen, JSON.toJSONString(quotes.detail(saved.getId()).getItems().getFirst().getSpecs()));
        assertEquals(frozen, JSON.toJSONString(quotes.previewUpdate(history).getItems().getFirst().getSpecs()));
        QuotationVO updated = quotes.update(history);
        assertEquals(frozen, JSON.toJSONString(updated.getItems().getFirst().getSpecs()));
        assertEquals("LM-02", updated.getItems().getFirst().getModel());

        QuotationVO copy = quotes.copy(this.copyRequest(saved.getId(), false));
        copy.setItems(List.of(copy.getItems().getFirst(), source.getItems().getFirst()));
        QuotationVO copyPreview = quotes.preview(copy);
        assertEquals(frozen, JSON.toJSONString(copyPreview.getItems().getFirst().getSpecs()));
        assertEquals(currentSpecs, JSON.toJSONString(copyPreview.getItems().get(1).getSpecs()));
        QuotationVO copied = quotes.create(copy);
        assertEquals(frozen, JSON.toJSONString(copied.getItems().getFirst().getSpecs()));
        assertEquals(currentSpecs, JSON.toJSONString(copied.getItems().get(1).getSpecs()));

        MvcResult response = mvc.perform(get("/quotation/detail")
                .with(authentication(this.user(1L, QUOTE_QUERY)))
                .param("id", saved.getId().toString())).andReturn();
        JSONObject document = this.json(response).getJSONObject("data");
        JSONObject spec = document.getJSONArray("items").getJSONObject(0).getJSONArray("specs").getJSONObject(0);
        assertEquals("颜色", spec.getString("specName"));
        assertEquals("石墨灰", spec.getString("textValue"));
        assertEquals(101L, spec.getLongValue("specId"));
        assertEquals(201L, spec.getLongValue("specValueId"));
        assertFalse(spec.containsKey("snapshotId"));
        assertFalse(spec.containsKey("gmtCreate"));
    }

    @Test
    void keepsHistoricalProductAndValidatesAccessVersionAndOrderLock() {
        QuotationVO saved = quotes.create(this.sampleForm());
        QuotationVO current = quotes.detail(saved.getId());
        jdbc.update("UPDATE quotation SET contact_email = ? WHERE id = ?", "demo@example.test", saved.getId());
        jdbc.update("UPDATE product_sku SET reference_sale_price = 150, version = 1 WHERE id = ?", SKU_A);
        jdbc.update("UPDATE product SET version = 1 WHERE id = 11");
        assertEquals("quotation.sku-version-conflict",
                assertThrows(BusinessException.class, () -> quotes.create(this.sampleForm())).getI18nCode());
        jdbc.update("UPDATE product_sku SET is_deleted = true WHERE id = ?", SKU_A);
        jdbc.update("UPDATE product_unit SET name = '箱' WHERE id = 1");

        QuotationVO history = quotes.detail(saved.getId());
        assertEquals("件", history.getItems().getFirst().getUnitName());
        assertEquals("129.345", history.getItems().getFirst().getUnitPrice().toPlainString());
        assertEquals("316.18", history.getTotalAmount().toPlainString());

        QuotationVO edit = current;
        QuotationItemVO retained = new QuotationItemVO();
        retained.setId(edit.getItems().getFirst().getId());
        retained.setQuantity(new BigDecimal("3.0"));
        QuotationVO edited = this.form(retained);
        edited.setId(edit.getId());
        edited.setVersion(edit.getVersion());
        edited.setTitle("更新的标题");
        QuotationVO updated = quotes.update(edited);
        assertEquals("388.04", updated.getTotalAmount().toPlainString());
        assertEquals(saved.getQuoteNumber(), updated.getQuoteNumber());
        assertEquals(1, updated.getVersion());
        assertNull(updated.getCustomerName());
        assertEquals("demo@example.test", jdbc.queryForObject("SELECT contact_email FROM quotation WHERE id = ?", String.class, saved.getId()));
        assertEquals(edit.getItems().getFirst().getId(), updated.getItems().getFirst().getId());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM quotation_item WHERE is_deleted = true", Integer.class));

        assertEquals("quotation.version-conflict", assertThrows(BusinessException.class, () -> quotes.update(edited)).getI18nCode());
        jdbc.update("UPDATE quotation SET order_generated = true, version = 2 WHERE id = ?", saved.getId());
        assertEquals("quotation.order-generated",
                assertThrows(BusinessException.class, () -> quotes.previewUpdate(updated)).getI18nCode());
        assertEquals("quotation.order-generated",
                assertThrows(BusinessException.class, () -> quotes.delete(this.versioned(saved.getId(), 2L))).getI18nCode());

        QuotationVO copied = quotes.copy(this.copyRequest(saved.getId(), true));
        assertNull(copied.getQuoteNumber());
        assertEquals("更新的标题", copied.getTitle());
        assertEquals(1, this.count("quotation"));
        this.signIn(1L, QUOTE_CREATE);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> quotes.create(copied));
        this.signIn(1L, QUOTE_QUERY, QUOTE_CREATE);
        QuotationVO copyPreview = quotes.preview(copied);
        assertEquals("388.04", copyPreview.getTotalAmount().toPlainString());
        QuotationVO copySaved = quotes.create(copied);
        assertNotEquals(saved.getQuoteNumber(), copySaved.getQuoteNumber());
        assertFalse(quotes.detail(copySaved.getId()).getOrderGenerated());
        assertEquals(2, this.count("product_snapshot"));

        QuotationVO query = new QuotationVO();
        query.setQuotedFrom(QUOTED_AT.toLocalDate());
        query.setQuotedTo(QUOTED_AT.toLocalDate());
        query.setOrderGenerated(true);
        assertEquals(List.of(saved.getId()), quotes.list(query, 1, 15).getRecords().stream().map(QuotationVO::getId).toList());
        query.setQuotedFrom(QUOTED_AT.toLocalDate().minusDays(1));
        query.setQuotedTo(QUOTED_AT.toLocalDate().minusDays(1));
        assertEquals(0, quotes.list(query, 1, 15).getTotal());
        this.signIn(2L, QUOTE_QUERY);
        assertEquals("quotation.not-found", assertThrows(BusinessException.class, () -> quotes.detail(saved.getId())).getI18nCode());
        assertEquals("quotation.not-found", assertThrows(BusinessException.class, () -> quotes.copy(
                this.copyRequest(saved.getId(), false))).getI18nCode());
        assertEquals("quotation.not-found", assertThrows(BusinessException.class, () -> quotes.delete(
                this.versioned(saved.getId(), 2L))).getI18nCode());
        assertTrue(quotes.list(new QuotationVO(), 1, 15).getRecords().isEmpty());
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
        assertEquals("0.00", quotes.preview(this.form(this.line(SKU_TWD, "2.00", null, null))).getTotalAmount().toPlainString());
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
            Future<QuotationVO> cny = executor.submit(() -> {
                this.signIn(1L, QUOTE_QUERY);
                start.await();
                return quotes.create(this.form(this.line(SKU_A, "1", null, null)));
            });
            Future<QuotationVO> twd = executor.submit(() -> {
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
            QuotationVO abort = this.form(this.line(SKU_B, "2", null, "rollback"));
            assertThrows(DataIntegrityViolationException.class, () -> quotes.create(abort));
            assertEquals(2, this.count("quotation"));
            assertEquals(2, this.count("product_snapshot"));
        } finally {
            jdbc.execute("ALTER TABLE quotation_item DROP CONSTRAINT test_abort_save");
        }

        QuotationVO third = quotes.create(this.form(this.line(SKU_B, "1", null, null)));
        assertEquals(QuotationServiceImpl.formatQuoteNumber(today, (short) 3, CurrencyEnum.CNY), third.getQuoteNumber());
        quotes.delete(this.versioned(third.getId(), 0L));
        QuotationVO fourth = quotes.create(this.form(this.line(SKU_B, "1", null, null)));
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
            QuotationVO saved = quotes.create(this.form(this.line(SKU_A, "1", null, null)));
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
        QuotationVO saved = quotes.create(this.form(this.line(SKU_A, "1", null, null)));
        TransactionTemplate transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<BusinessException> deletion = transaction.execute(status -> {
                jdbc.update("UPDATE quotation SET order_generated = true, version = 1 WHERE id = ?", saved.getId());
                CountDownLatch attempting = new CountDownLatch(1);
                Future<BusinessException> future = executor.submit(() -> {
                    this.signIn(1L, QUOTE_QUERY);
                    attempting.countDown();
                    return assertThrows(BusinessException.class, () -> quotes.delete(this.versioned(saved.getId(), 0L)));
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
        assertEquals(List.of("CNY", "TWD"), split.getItems().stream().map(QuotationTemplateItemVO::getReferenceSaleCurrency).toList());
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
        QuotationVO applied = templates.apply(Scope.PERSONAL, saved.getId());
        assertEquals("门店陈列", applied.getTitle());
        assertEquals("整单备注", applied.getRemark());
        assertEquals("第一行", applied.getItems().getFirst().getRemark());
        assertEquals("80", applied.getItems().getLast().getDiscountRate().toPlainString());
        applied.setQuotedAt(QUOTED_AT);
        assertEquals("193.80", quotes.preview(applied).getTotalAmount().toPlainString());

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
        this.assertJsonNumber(previewData, "totalAmount", "316.18");
        this.assertJsonNumber(previewData, "discountRate", "95.00");
        assertEquals(Long.toString(SKU_A), previewData.getJSONArray("items").getJSONObject(0).get("skuId"));

        MvcResult created = mvc.perform(post("/quotation/create").with(authentication(editor))
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andReturn();
        JSONObject reference = this.json(created).getJSONObject("data");
        this.assertJsonNumber(reference, "totalAmount", "316.18");
        assertEquals(2, reference.getJSONArray("items").size());
        assertInstanceOf(String.class, reference.get("id"));
        Long quotationId = reference.getLong("id");
        jdbc.update("UPDATE quotation SET contact_email = ? WHERE id = ?", "demo@example.test", quotationId);
        MvcResult detail = mvc.perform(get("/quotation/detail").with(authentication(editor)).param("id", reference.getString("id")))
                .andExpect(header().string("Cache-Control", containsString("no-store"))).andReturn();
        JSONObject data = this.json(detail).getJSONObject("data");
        assertEquals(reference.getString("id"), data.get("id"));
        assertEquals("演示客户", data.getString("customerName"));
        this.assertJsonNumber(data, "totalAmount", "316.18");
        this.assertJsonNumber(data, "originalAmount", "378.69");
        this.assertJsonNumber(data, "subtotalAmount", "332.82");
        assertFalse(data.containsKey("lineDiscountAmount"));
        assertFalse(data.containsKey("orderDiscountAmount"));
        for (String field : List.of("gmtCreate", "gmtModified", "createUser", "updateUser", "isDeleted",
                "ownerUserId", "numberDate", "dailySequence")) {
            assertTrue(data.containsKey(field), field);
        }
        for (String field : List.of("form", "document", "skuIds", "customer", "issuer", "discount", "totals",
                "contactEmail")) {
            assertFalse(data.containsKey(field), field);
        }
        JSONObject firstItem = data.getJSONArray("items").getJSONObject(0);
        assertEquals(Long.toString(SKU_A), firstItem.get("skuId"));
        assertInstanceOf(String.class, firstItem.get("id"));
        this.assertJsonNumber(firstItem, "quantity", "2");
        this.assertJsonNumber(firstItem, "unitPrice", "129.345");
        this.assertJsonNumber(firstItem, "lineAmount", "232.82");
        assertEquals("展厅储物设备", firstItem.getString("productName"));
        assertTrue(firstItem.containsKey("gmtCreate"));
        assertTrue(firstItem.containsKey("gmtModified"));
        assertTrue(firstItem.containsKey("createUser"));
        assertFalse(firstItem.containsKey("discountAmount"));
        assertFalse(firstItem.containsKey("discount"));

        MvcResult list = mvc.perform(get("/quotation/list").with(authentication(editor))
                .param("pageNum", "1").param("pageSize", "1")).andReturn();
        JSONObject page = this.json(list).getJSONObject("data");
        assertEquals(1, page.getLongValue("total"));
        JSONObject listed = page.getJSONArray("records").getJSONObject(0);
        assertEquals(reference.getString("id"), listed.get("id"));
        assertEquals("演示客户", listed.getString("customerName"));
        this.assertJsonNumber(listed, "totalAmount", "316.18");
        assertEquals("展厅储物设备报价", listed.getString("title"));
        assertEquals("0900-000-000", listed.getString("contactPhone"));
        for (String field : List.of("items", "form", "document", "skuIds", "contactEmail")) {
            assertFalse(listed.containsKey(field), field);
        }

        MvcResult copy = mvc.perform(post("/quotation/copy").with(authentication(editor)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"" + reference.getString("id") + "\",\"clearCustomer\":false}"))
                .andExpect(header().string("Cache-Control", containsString("no-store"))).andReturn();
        JSONObject copied = this.json(copy).getJSONObject("data");
        assertEquals("演示客户", copied.getString("customerName"));
        assertEquals(reference.getString("id"), copied.get("sourceId"));
        assertEquals(0, copied.getLongValue("sourceVersion"));
        assertEquals(firstItem.get("id"), copied.getJSONArray("items").getJSONObject(0).get("id"));
        for (String field : List.of("id", "version", "orderGenerated", "quoteNumber", "form", "document", "skuIds", "contactEmail")) {
            assertFalse(copied.containsKey(field), field);
        }

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

        QuotationVO editable = data.toJavaObject(QuotationVO.class);
        QuotationVO updateForm = editable;
        String updatePayload = JSON.toJSONString(updateForm);
        MvcResult update = mvc.perform(post("/quotation/update").with(authentication(editor))
                .contentType(MediaType.APPLICATION_JSON).content(updatePayload)).andReturn();
        JSONObject updated = this.json(update).getJSONObject("data");
        assertEquals(reference.getString("id"), updated.get("id"));
        assertEquals(1, updated.getLongValue("version"));
        this.assertJsonNumber(updated, "totalAmount", "316.18");
        assertEquals(firstItem.get("id"), updated.getJSONArray("items").getJSONObject(0).get("id"));
        assertEquals(updated.get("id"), updated.getJSONArray("items").getJSONObject(0).get("quotationId"));
        assertFalse(updated.containsKey("form"));
        assertFalse(updated.containsKey("document"));
    }

    @Test
    void flatRequestsValidateFieldsAndIgnoreServerManagedValues() throws Exception {
        Authentication editor = this.user(1L, QUOTE_CREATE, QUOTE_QUERY, QUOTE_UPDATE);
        JSONObject request = JSON.parseObject(JSON.toJSONString(this.sampleForm()));
        request.put("id", "99");
        request.put("version", 99);
        request.put("ownerUserId", "2");
        request.put("contactEmail", "injected@example.test");
        request.put("orderGenerated", true);
        request.put("quoteNumber", "FORGED");
        request.put("currency", "TWD");
        request.put("totalAmount", "0");
        request.put("issuerName", "伪造开单方");
        JSONObject item = request.getJSONArray("items").getJSONObject(0);
        item.put("snapshotId", "99");
        item.put("unitPrice", "0");
        item.put("lineAmount", "0");
        item.put("productName", "伪造商品");

        MvcResult created = mvc.perform(post("/quotation/create").with(authentication(editor))
                .contentType(MediaType.APPLICATION_JSON).content(request.toJSONString())).andReturn();
        JSONObject saved = this.json(created).getJSONObject("data");
        this.assertJsonNumber(saved, "totalAmount", "316.18");
        assertEquals("CNY", saved.getString("currency"));
        assertEquals(0, saved.getLongValue("version"));
        assertFalse(saved.getBooleanValue("orderGenerated"));
        assertNotEquals("99", saved.getString("id"));
        assertNotEquals("FORGED", saved.getString("quoteNumber"));
        assertNotEquals("伪造开单方", saved.getString("issuerName"));
        JSONObject savedItem = saved.getJSONArray("items").getJSONObject(0);
        this.assertJsonNumber(savedItem, "unitPrice", "129.345");
        assertEquals("展厅储物设备", savedItem.getString("productName"));
        assertNotEquals("99", savedItem.getString("snapshotId"));
        Long id = saved.getLong("id");
        assertEquals(1L, jdbc.queryForObject("SELECT owner_user_id FROM quotation WHERE id = ?", Long.class, id));
        assertNull(jdbc.queryForObject("SELECT contact_email FROM quotation WHERE id = ?", String.class, id));

        JSONObject missingVersion = JSON.parseObject(saved.toJSONString());
        missingVersion.remove("version");
        MvcResult invalidUpdate = mvc.perform(post("/quotation/update").with(authentication(editor))
                .contentType(MediaType.APPLICATION_JSON).content(missingVersion.toJSONString())).andReturn();
        BusinessException versionError = assertInstanceOf(BusinessException.class, invalidUpdate.getResolvedException());
        assertEquals("quotation.version-invalid", versionError.getI18nCode());

        for (String invalid : List.of("null", "{\"items\":[]}", "{\"items\":[null]}",
                "{\"sourceId\":\"1\",\"items\":[]}",
                "{\"items\":[{\"skuId\":\"1\",\"productVersion\":0,\"skuVersion\":0}]}",
                "{\"discountRate\":\"95\",\"discountedAmount\":\"10\",\"items\":[]}")) {
            MvcResult rejected = mvc.perform(post("/quotation/preview").with(authentication(editor))
                    .contentType(MediaType.APPLICATION_JSON).content(invalid)).andReturn();
            assertFalse(this.json(rejected).getBooleanValue("success"), invalid);
        }
        assertEquals(1, this.count("quotation"));
    }

    @Test
    void defaultSecurityRequiresLoginForPublicTemplatesAndCreateAuthorityForApply() throws Exception {
        List<QuotationTemplateItemVO> items = List.of(this.templateItem(SKU_A, null, null));
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
        assertEquals(1L, template.getLongValue("ownerUserId"));
        assertTrue(template.containsKey("gmtCreate"));
        this.assertJsonNumber(template, "discountRate", "95.55");
        assertEquals(remark, template.getString("remark"));
        assertEquals(remark, template.getJSONArray("items").getJSONObject(0).getString("remark"));
        this.assertJsonNumber(template.getJSONArray("items").getJSONObject(0), "discountRate", "0");
        this.assertJsonNumber(template.getJSONArray("items").getJSONObject(1), "discountRate", "100");

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
        assertEquals(Set.of("title", "currency", "discountRate", "remark", "items"), application.keySet());
        assertEquals("演示报价", application.getString("title"));
        JSONObject appliedItem = application.getJSONArray("items").getJSONObject(0);
        assertEquals(Long.toString(SKU_A), appliedItem.get("skuId"));
        assertEquals(0, appliedItem.getLongValue("productVersion"));
        assertFalse(appliedItem.containsKey("id"));
        assertFalse(appliedItem.containsKey("product"));
        this.assertJsonNumber(appliedItem, "quantity", "1");

        MvcResult listed = mvc.perform(get("/quotation/template/public/list").with(authentication(this.user(2L, PUBLIC_QUERY)))).andReturn();
        JSONObject summary = this.json(listed).getJSONObject("data").getJSONArray("records").getJSONObject(0);
        assertEquals(Set.of("id", "name", "title", "scope", "version", "ownedByCurrentUser",
                "gmtCreate", "gmtModified", "createUser", "updateUser", "isDeleted", "ownerUserId",
                "usable", "issueCount", "discountRate", "remark"), summary.keySet());
        assertEquals(Boolean.FALSE, summary.getBoolean("ownedByCurrentUser"));

        String update = """
                {"id":"%s","version":0,"name":"改名","title":"新标题","items":[{"skuId":"%s"}]}
                """.formatted(id, SKU_B);
        Authentication otherMaintainer = this.user(2L, publicUpdate);
        MvcResult updated = mvc.perform(post("/quotation/template/public/update").with(authentication(otherMaintainer))
                .contentType(MediaType.APPLICATION_JSON).content(update)).andReturn();
        assertEquals("新标题", this.json(updated).getJSONObject("data").getString("title"));
        assertEquals(0, this.count("quotation"));
    }

    @Test
    void onlyCreatorsWithScopePermissionCanDeleteQuotationTemplates() throws Exception {
        List<QuotationTemplateItemVO> items = List.of(this.templateItem(SKU_A, null, null));
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
            String request = "{\"id\":\"%s\",\"version\":0}".formatted(id);

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

            String staleRequest = "{\"id\":\"%s\",\"version\":1}".formatted(id);
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
            Long id, Long version, String name, String title, BigDecimal rate, String remark,
            List<QuotationTemplateItemVO> items
    ) {
        QuotationTemplateVO form = new QuotationTemplateVO();
        form.setId(id);
        form.setVersion(version);
        form.setName(name);
        form.setTitle(title);
        form.setDiscountRate(rate);
        form.setRemark(remark);
        form.setItems(items);
        return form;
    }

    private QuotationTemplateItemVO templateItem(Long skuId, BigDecimal rate, String remark) {
        QuotationTemplateItemVO item = new QuotationTemplateItemVO();
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

    /**
     * 验证接口返回 JSON 数值及其业务金额，不依赖小数尾零格式。
     */
    private void assertJsonNumber(JSONObject data, String field, String expected) {
        assertInstanceOf(Number.class, data.get(field), field);
        BigDecimal actual = data.getBigDecimal(field);
        BigDecimal expectedValue = new BigDecimal(expected);
        assertEquals(0, expectedValue.compareTo(actual), field);
    }

    private QuotationVO sampleForm() {
        QuotationItemVO first = this.line(SKU_A, "2.00", new BigDecimal("90"), "贴标签");
        QuotationItemVO second = this.line(SKU_B, "1.500", null, null);
        second.setDiscountedAmount(new BigDecimal("100"));
        QuotationVO form = this.form(first, second);
        form.setTitle("展厅储物设备报价");
        form.setCustomerName("演示客户");
        form.setContactPhone("0900-000-000");
        form.setShippingAddress("演示地址 1 号");
        form.setRemark("交期另行确认");
        form.setDiscountRate(new BigDecimal("95.00"));
        return form;
    }

    private QuotationVO form(QuotationItemVO... items) {
        QuotationVO form = new QuotationVO();
        form.setQuotedAt(QUOTED_AT);
        form.setItems(List.of(items));
        return form;
    }

    private QuotationItemVO line(long sku, String quantity, BigDecimal rate, String remark) {
        QuotationItemVO item = new QuotationItemVO();
        item.setSkuId(sku);
        item.setProductVersion(0L);
        item.setSkuVersion(0L);
        item.setQuantity(new BigDecimal(quantity));
        item.setDiscountRate(rate);
        item.setRemark(remark);
        return item;
    }

    private QuotationVO versioned(Long id, Long version) {
        QuotationVO request = new QuotationVO();
        request.setId(id);
        request.setVersion(version);
        return request;
    }

    private QuotationVO copyRequest(Long id, boolean clearCustomer) {
        QuotationVO request = new QuotationVO();
        request.setId(id);
        request.setClearCustomer(clearCustomer);
        return request;
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
    @ComponentScan({"com.muzhi.minierp.vo.product", "com.muzhi.minierp.vo.sales",
            "com.muzhi.minierp.entity.product", "com.muzhi.minierp.entity.sales"})
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
