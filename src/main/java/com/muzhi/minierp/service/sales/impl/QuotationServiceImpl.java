package com.muzhi.minierp.service.sales.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.config.QuotationIssuerProperties;
import com.muzhi.minierp.entity.product.ProductSnapshot;
import com.muzhi.minierp.entity.product.ProductSnapshotImage;
import com.muzhi.minierp.entity.product.ProductSnapshotSpec;
import com.muzhi.minierp.entity.sales.Quotation;
import com.muzhi.minierp.entity.sales.QuotationItem;
import com.muzhi.minierp.enums.CurrencyEnum;
import com.muzhi.minierp.enums.ProductEnum;
import com.muzhi.minierp.mapper.product.ProductSnapshotImageMapper;
import com.muzhi.minierp.mapper.product.ProductSnapshotMapper;
import com.muzhi.minierp.mapper.product.ProductSnapshotSpecMapper;
import com.muzhi.minierp.mapper.sales.QuotationItemMapper;
import com.muzhi.minierp.mapper.sales.QuotationMapper;
import com.muzhi.minierp.model.LoginUser;
import com.muzhi.minierp.model.sales.QuotationSkuSelection;
import com.muzhi.minierp.security.SecurityUtils;
import com.muzhi.minierp.service.product.IProductSnapshotService;
import com.muzhi.minierp.service.sales.IQuotationItemService;
import com.muzhi.minierp.service.sales.IQuotationService;
import com.muzhi.minierp.service.system.IAttachmentService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.product.ProductOptionSpecVO;
import com.muzhi.minierp.vo.product.ProductOptionVO;
import com.muzhi.minierp.vo.product.ProductSnapshotImageVO;
import com.muzhi.minierp.vo.sales.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 报价计价、正式取号及历史报价服务。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Service
@RequiredArgsConstructor
public class QuotationServiceImpl extends ServiceImpl<QuotationMapper, Quotation> implements IQuotationService {

    /**
     * 报价业务时间及正式单号的取号时区。
     */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Taipei");

    private final QuotationItemMapper itemMapper;

    private final ProductSnapshotMapper snapshotMapper;

    private final ProductSnapshotImageMapper snapshotImageMapper;

    private final ProductSnapshotSpecMapper snapshotSpecMapper;

    private final IAttachmentService attachmentService;

    private final IProductSnapshotService snapshotService;

    private final IQuotationItemService quotationItemService;

    private final QuotationIssuerProperties issuerProperties;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public QuotationVO preview(QuotationVO form) {
        Quotation source = this.copySource(form, false);
        QuotationVO result = this.calculate(form, source, false, false);
        QuotationVO preview = this.detail(result);
        preview.setSourceId(form.getSourceId());
        preview.setSourceVersion(form.getSourceVersion());
        return preview;
    }

    @Override
    @Retryable(includes = PessimisticLockingFailureException.class, maxRetries = 1, delay = 50)
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public QuotationVO create(QuotationVO form) {
        Quotation source = this.copySource(form, true);
        QuotationVO header = this.calculate(form, source, false, true);
        LocalDate numberDate = LocalDate.now(BUSINESS_ZONE);
        super.baseMapper.lockDailySequence(numberDate);
        Integer maximum = super.baseMapper.selectMaxDailySequence(numberDate);
        short sequence = this.nextDailySequence(maximum);
        CurrencyEnum currency = CurrencyEnum.ofCode(header.getCurrency());
        String quoteNumber = QuotationServiceImpl.formatQuoteNumber(numberDate, sequence, currency);

        header.setId(IdWorker.getId());
        header.setOwnerUserId(this.currentUserId());
        header.setQuoteNumber(quoteNumber);
        header.setNumberDate(numberDate);
        header.setDailySequence(sequence);
        header.setOrderGenerated(false);
        header.setVersion(0L);
        super.baseMapper.insert(header);
        for (QuotationItemVO item : header.getItems()) {
            item.setId(IdWorker.getId());
            item.setQuotationId(header.getId());
            itemMapper.insert(item);
        }
        return this.detail(header);
    }

    /**
     * 在取得当日取号锁后根据已发流水计算下一个号码。
     */
    private short nextDailySequence(Integer maximum) {
        int dailyLimit = 999;
        Assert.isTrue(maximum != null && maximum >= dailyLimit, "quotation.number-exhausted", "今日正式报价单号已用完");
        return maximum == null ? 1 : (short) (maximum + 1);
    }

    /**
     * 按报价币种格式化台北日期和流水号。
     */
    static String formatQuoteNumber(LocalDate date, short sequence, CurrencyEnum currency) {
        if (currency == CurrencyEnum.TWD) {
            int republicYear = date.getYear() - 1911;
            return String.format(Locale.ROOT, "QT-%d%02d%02d-%03d", republicYear,
                    date.getMonthValue(), date.getDayOfMonth(), sequence);
        }
        return String.format(Locale.ROOT, "QT-%04d%02d%02d-%03d", date.getYear(),
                date.getMonthValue(), date.getDayOfMonth(), sequence);
    }

    @Override
    public IPage<QuotationVO> list(QuotationVO query, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Quotation> criteria = Wrappers.lambdaQuery();
        Long ownerId = this.currentUserId();
        criteria.eq(Quotation::getOwnerUserId, ownerId);
        String keyword = StringUtils.trimToNull(query.getKeyword());
        if (keyword != null) {
            criteria.and(search -> search.like(Quotation::getQuoteNumber, keyword)
                    .or().like(Quotation::getTitle, keyword)
                    .or().like(Quotation::getCustomerName, keyword));
        }
        String quoteNumber = StringUtils.trimToNull(query.getQuoteNumber());
        String title = StringUtils.trimToNull(query.getTitle());
        String customer = StringUtils.trimToNull(query.getCustomerName());
        criteria.like(quoteNumber != null, Quotation::getQuoteNumber, quoteNumber);
        criteria.like(title != null, Quotation::getTitle, title);
        criteria.like(customer != null, Quotation::getCustomerName, customer);
        criteria.eq(query.getOrderGenerated() != null, Quotation::getOrderGenerated, query.getOrderGenerated());
        if (query.getQuotedFrom() != null) {
            OffsetDateTime from = query.getQuotedFrom().atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
            criteria.ge(Quotation::getQuotedAt, from);
        }
        if (query.getQuotedTo() != null) {
            LocalDate nextDay = query.getQuotedTo().plusDays(1);
            OffsetDateTime until = nextDay.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
            criteria.lt(Quotation::getQuotedAt, until);
        }
        criteria.orderByDesc(Quotation::getQuotedAt, Quotation::getId);
        Page<Quotation> page = new Page<>(pageNum, pageSize);
        Page<Quotation> result = super.baseMapper.selectPage(page, criteria);
        return result.convert(this::quotationView);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public QuotationVO detail(Long id) {
        Quotation header = this.owned(id, false);
        QuotationVO result = this.quotationView(header);
        List<QuotationItemVO> items = this.readItems(id);
        result.setItems(items);
        return this.detail(result);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public QuotationVO previewUpdate(QuotationVO form) {
        Quotation source = this.editable(form, false);
        QuotationVO result = this.calculate(form, source, true, false);
        return this.detail(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public QuotationVO update(QuotationVO form) {
        Quotation source = this.editable(form, true);
        QuotationVO header = this.calculate(form, source, true, true);
        header.setVersion(Math.incrementExact(source.getVersion()));
        LambdaUpdateWrapper<Quotation> update = Wrappers.lambdaUpdate();
        update.eq(Quotation::getId, source.getId());
        update.set(Quotation::getVersion, header.getVersion());
        update.set(Quotation::getTitle, header.getTitle());
        update.set(Quotation::getQuotedAt, header.getQuotedAt());
        update.set(Quotation::getCustomerName, header.getCustomerName());
        update.set(Quotation::getContactName, header.getContactName());
        update.set(Quotation::getContactPhone, header.getContactPhone());
        update.set(Quotation::getShippingAddress, header.getShippingAddress());
        update.set(Quotation::getRemark, header.getRemark());
        update.set(Quotation::getOriginalAmount, header.getOriginalAmount());
        update.set(Quotation::getSubtotalAmount, header.getSubtotalAmount());
        update.set(Quotation::getDiscountRate, header.getDiscountRate());
        update.set(Quotation::getDiscountedAmount, header.getDiscountedAmount());
        update.set(Quotation::getTotalAmount, header.getTotalAmount());
        Quotation audit = new Quotation();
        super.baseMapper.update(audit, update);

        this.replaceItems(header.getId(), header.getItems());
        return this.detail(header);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void delete(QuotationVO reference) {
        Quotation source = this.editable(reference, true);
        Long nextVersion = Math.incrementExact(source.getVersion());
        LambdaUpdateWrapper<Quotation> update = Wrappers.lambdaUpdate();
        update.eq(Quotation::getId, source.getId());
        update.set(Quotation::getVersion, nextVersion);
        update.set(Quotation::getIsDeleted, true);
        Quotation audit = new Quotation();
        super.baseMapper.update(audit, update);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public QuotationVO copy(QuotationVO request) {
        Quotation source = this.owned(request.getId(), false);
        List<QuotationItemVO> items = this.readItems(source.getId());
        if (Boolean.TRUE.equals(request.getClearCustomer())) {
            source.setCustomerName(null);
            source.setContactName(null);
            source.setContactPhone(null);
            source.setShippingAddress(null);
        }
        QuotationVO result = this.quotationView(source);
        result.setItems(items);
        this.detail(result);
        result.setSourceId(source.getId());
        result.setSourceVersion(source.getVersion());
        result.setId(null);
        result.setVersion(null);
        result.setOrderGenerated(null);
        result.setQuoteNumber(null);
        return result;
    }

    /**
     * 从指定归属记录读取来源，写入使用的锁也供后续订单事务使用。
     */
    private Quotation owned(Long id, boolean forWrite) {
        LambdaQueryWrapper<Quotation> query = Wrappers.lambdaQuery();
        query.eq(Quotation::getId, id);
        Long ownerId = this.currentUserId();
        query.eq(Quotation::getOwnerUserId, ownerId);
        query.last(forWrite, "FOR UPDATE");
        Quotation quotation = super.baseMapper.selectOne(query);
        Assert.isNull(quotation, "quotation.not-found", "报价不存在或已删除");
        return quotation;
    }

    private Long currentUserId() {
        LoginUser user = SecurityUtils.getCurrentUser();
        if (user == null || user.getId() == null) {
            throw new AccessDeniedException("quotation.authentication-required");
        }
        return user.getId();
    }

    private Quotation copySource(QuotationVO reference, boolean forWrite) {
        if (reference.getSourceId() == null) {
            return null;
        }
        LoginUser user = SecurityUtils.getCurrentUser();
        boolean canRead = user != null && user.getAuthorities().stream()
                .anyMatch(authority -> "sales-manager:quotation:query".equals(authority.getAuthority()));
        if (!canRead) {
            throw new AccessDeniedException("quotation.read-source-denied");
        }
        Quotation source = this.owned(reference.getSourceId(), forWrite);
        this.checkVersion(source, reference.getSourceVersion());
        return source;
    }

    private Quotation editable(QuotationVO reference, boolean forWrite) {
        Quotation quotation = this.owned(reference.getId(), forWrite);
        Assert.isTrue(Boolean.TRUE.equals(quotation.getOrderGenerated()), "quotation.order-generated", "已生成订单的报价不能修改或删除");
        this.checkVersion(quotation, reference.getVersion());
        return quotation;
    }

    private void checkVersion(Quotation source, Long version) {
        boolean conflict = !Objects.equals(source.getVersion(), version);
        Assert.isTrue(conflict, "quotation.version-conflict", "报价已更新，请重新读取后确认");
    }

    /**
     * 新选商品先批量核验，沿用行只使用原快照和单价。
     */
    private QuotationVO calculate(QuotationVO form, Quotation source, boolean editing, boolean forWrite) {
        Map<Long, QuotationItemVO> originals = Map.of();
        if (source != null && form.getItems().stream().anyMatch(item -> item.getId() != null)) {
            List<QuotationItemVO> items = this.readItems(source.getId());
            originals = items.stream().collect(Collectors.toMap(QuotationItemVO::getId, Function.identity()));
        }
        List<Long> newSkuIds = form.getItems().stream()
                .filter(item -> item.getId() == null)
                .map(QuotationItemVO::getSkuId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, QuotationSkuSelection> selections = quotationItemService.loadSkuSelections(newSkuIds, forWrite);
        List<QuotationItemVO> items = new ArrayList<>();
        String currency = editing ? source.getCurrency() : null;
        BigDecimal originalAmount = BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;
        for (QuotationItemVO input : form.getItems()) {
            int position = items.size() + 1;
            QuotationItemVO item;
            if (input.getId() != null) {
                QuotationItemVO original = originals.get(input.getId());
                Assert.isNull(original, "quotation.source-item-not-found", "第 {0} 行引用的原报价明细不存在", position);
                // 同一历史行允许复制到多个位置，各位置独立重新计价。
                item = BeanConvertUtils.convert(original, QuotationItemVO.class);
                this.priceLine(input, item, position);
                // 预览保留来源明细 ID，正式新建时重新生成主键。
                if (editing || !forWrite) {
                    item.setId(original.getId());
                }
            } else {
                QuotationSkuSelection selection = selections.get(input.getSkuId());
                item = this.priceSelectedItem(input, selection, position);
            }
            if (currency == null) {
                currency = item.getCurrency();
            }
            Assert.isTrue(!currency.equals(item.getCurrency()), "quotation.currency-conflict",
                    "第 {0} 行的币种必须与报价币种一致", position);
            originalAmount = originalAmount.add(item.getOriginalAmount());
            subtotal = subtotal.add(item.getLineAmount());
            items.add(item);
        }
        this.checkAmountRange(originalAmount);
        BigDecimal total = this.afterDiscount(subtotal, form.getDiscountRate(), form.getDiscountedAmount(), 0);
        if (forWrite) {
            this.freezeSelectedItems(form.getItems(), items);
        }

        Quotation header = this.header(form, source, editing);
        header.setCurrency(currency);
        header.setOriginalAmount(originalAmount);
        header.setSubtotalAmount(subtotal);
        header.setTotalAmount(total);
        QuotationVO result = this.quotationView(header);
        result.setItems(items);
        return result;
    }

    private QuotationItemVO priceSelectedItem(QuotationItemVO input, QuotationSkuSelection selection, int position) {
        boolean unavailable = selection == null || !selection.issues().isEmpty();
        Assert.isTrue(unavailable, "quotation.sku-unavailable",
                "第 {0} 行的商品或 SKU 不可用，请刷新或替换", position);

        ProductOptionVO option = selection.option();
        boolean changed = !Objects.equals(input.getProductVersion(), option.getProductVersion())
                || !Objects.equals(input.getSkuVersion(), option.getSkuVersion());
        Assert.isTrue(changed, "quotation.sku-version-conflict",
                "第 {0} 行的商品资料已变更，请刷新该行", position);

        QuotationItemVO item = QuotationItemVO.fromOption(option);
        this.priceLine(input, item, position);
        return item;
    }

    private void priceLine(QuotationItemVO input, QuotationItemVO item, int position) {
        BigDecimal quantity = input.getQuantity().stripTrailingZeros();
        int quantityScale = item.getQuantityScale();
        Assert.isTrue(quantity.scale() > quantityScale, "quotation.quantity-scale",
                "第 {0} 行的数量最多支持 {1} 位小数", position, quantityScale);
        BigDecimal unrounded = item.getUnitPrice().multiply(quantity);
        BigDecimal original = this.roundAmount(unrounded);
        this.checkAmountRange(original);
        BigDecimal amount = this.afterDiscount(original, input.getDiscountRate(), input.getDiscountedAmount(), position);

        item.setId(null);
        item.setQuotationId(null);
        item.setGmtCreate(null);
        item.setGmtModified(null);
        item.setCreateUser(null);
        item.setUpdateUser(null);
        item.setIsDeleted(null);
        item.setSortOrder(position - 1);
        item.setQuantity(quantity);
        item.setOriginalAmount(original);
        item.setLineAmount(amount);
        item.setRemark(input.getRemark());
        item.setDiscountRate(input.getDiscountRate());
        item.setDiscountedAmount(input.getDiscountedAmount());
    }

    /**
     * 计算优惠后的金额，位置为 0 时表示整单。
     */
    private BigDecimal afterDiscount(BigDecimal before, BigDecimal rate, BigDecimal fixed, int position) {
        if (rate != null) {
            BigDecimal discounted = before.multiply(rate).movePointLeft(2);
            return this.roundAmount(discounted);
        }
        if (fixed == null) {
            return before;
        }
        if (position == 0) {
            Assert.isTrue(fixed.compareTo(before) > 0, "quotation.order-discount-exceeds-amount",
                    "整单优惠后金额不能超过整单优惠前金额");
        } else {
            Assert.isTrue(fixed.compareTo(before) > 0, "quotation.line-discount-exceeds-amount",
                    "第 {0} 行的优惠后金额不能超过该行原价", position);
        }
        return this.roundAmount(fixed);
    }

    private BigDecimal roundAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 数据库金额上界为 14 位整数。
     */
    private void checkAmountRange(BigDecimal amount) {
        BigDecimal upperExclusive = new BigDecimal("100000000000000");
        Assert.isTrue(amount.compareTo(upperExclusive) >= 0, "quotation.amount-overflow", "报价金额超出可保存范围");
    }

    /**
     * 所有新选商品已按固定顺序取得行锁，重复 SKU 共用快照。
     */
    private void freezeSelectedItems(List<QuotationItemVO> inputs, List<QuotationItemVO> items) {
        Map<Long, ProductSnapshot> snapshots = new HashMap<>();
        for (int index = 0; index < inputs.size(); index++) {
            QuotationItemVO input = inputs.get(index);
            if (input.getId() != null) {
                continue;
            }
            ProductSnapshot snapshot = snapshots.get(input.getSkuId());
            if (snapshot == null) {
                snapshot = snapshotService.getOrCreate(input.getSkuId(), input.getProductVersion(), input.getSkuVersion());
                snapshots.put(input.getSkuId(), snapshot);
            }
            QuotationItemVO item = items.get(index);
            this.applySnapshot(item, snapshot);
        }
    }

    /**
     * 使用已校验的快照填充来源和展示字段，成交数据由计价流程保留。
     */
    private void applySnapshot(QuotationItemVO item, ProductSnapshot snapshot) {
        item.setSnapshotId(snapshot.getId());
        item.setSkuId(snapshot.getSourceSkuId());
        item.setSkuCode(snapshot.getSkuCode());
        item.setProductName(snapshot.getProductName());
        item.setModel(snapshot.getModel());
        item.setSpecificationText(snapshot.getSpecificationText());
        item.setUnitName(snapshot.getUnitName());
        item.setQuantityScale(snapshot.getQuantityScale());
        item.setProductVersion(snapshot.getVersion());
        item.setSkuVersion(snapshot.getSourceSkuVersion());
        item.setImages(List.of());
        item.setSpecs(List.of());
    }

    private Quotation header(QuotationVO form, Quotation source, boolean editing) {
        Quotation header = new Quotation();
        if (editing) {
            header.setId(source.getId());
            header.setQuoteNumber(source.getQuoteNumber());
            header.setOrderGenerated(source.getOrderGenerated());
            header.setVersion(source.getVersion());
        }
        header.setTitle(form.getTitle() == null ? "报价单" : form.getTitle().strip());
        OffsetDateTime quotedAt = form.getQuotedAt();
        if (quotedAt == null) {
            quotedAt = OffsetDateTime.now(BUSINESS_ZONE);
        }
        header.setQuotedAt(quotedAt);
        header.setIssuerName(source == null ? issuerProperties.getName() : source.getIssuerName());
        header.setIssuerContact(source == null ? issuerProperties.getContact() : source.getIssuerContact());
        header.setRemark(form.getRemark());
        header.setCustomerName(form.getCustomerName());
        header.setContactName(form.getContactName());
        header.setContactPhone(form.getContactPhone());
        header.setShippingAddress(form.getShippingAddress());
        header.setDiscountRate(form.getDiscountRate());
        header.setDiscountedAmount(form.getDiscountedAmount());
        return header;
    }

    /**
     * 更新保留行的业务字段，按提交顺序新增与移除其他行。
     */
    private void replaceItems(Long quotationId, List<QuotationItemVO> items) {
        List<Long> retainedIds = items.stream().map(QuotationItemVO::getId).filter(Objects::nonNull).toList();
        LambdaUpdateWrapper<QuotationItem> removed = Wrappers.lambdaUpdate();
        removed.eq(QuotationItem::getQuotationId, quotationId);
        removed.notIn(!retainedIds.isEmpty(), QuotationItem::getId, retainedIds);
        removed.set(QuotationItem::getIsDeleted, true);
        QuotationItem audit = new QuotationItem();
        itemMapper.update(audit, removed);

        for (QuotationItemVO item : items) {
            if (item.getId() == null) {
                item.setId(IdWorker.getId());
                item.setQuotationId(quotationId);
                itemMapper.insert(item);
            } else {
                LambdaUpdateWrapper<QuotationItem> update = Wrappers.lambdaUpdate();
                update.eq(QuotationItem::getId, item.getId());
                update.eq(QuotationItem::getQuotationId, quotationId);
                update.set(QuotationItem::getSortOrder, item.getSortOrder());
                update.set(QuotationItem::getQuantity, item.getQuantity());
                update.set(QuotationItem::getRemark, item.getRemark());
                update.set(QuotationItem::getDiscountRate, item.getDiscountRate());
                update.set(QuotationItem::getDiscountedAmount, item.getDiscountedAmount());
                update.set(QuotationItem::getOriginalAmount, item.getOriginalAmount());
                update.set(QuotationItem::getLineAmount, item.getLineAmount());
                QuotationItem lineAudit = new QuotationItem();
                itemMapper.update(lineAudit, update);
            }
        }
    }

    /**
     * 历史数据直接批量读取已保存的商品快照。
     */
    private List<QuotationItemVO> readItems(Long quotationId) {
        LambdaQueryWrapper<QuotationItem> query = Wrappers.lambdaQuery();
        query.eq(QuotationItem::getQuotationId, quotationId);
        query.orderByAsc(QuotationItem::getSortOrder, QuotationItem::getId);
        List<QuotationItem> entities = itemMapper.selectList(query);
        if (entities.isEmpty()) {
            return List.of();
        }

        List<Long> ids = entities.stream().map(QuotationItem::getSnapshotId).distinct().toList();
        List<ProductSnapshot> snapshots = snapshotMapper.selectByIds(ids);
        Map<Long, ProductSnapshot> byId = snapshots.stream().collect(Collectors.toMap(ProductSnapshot::getId, Function.identity()));
        List<QuotationItemVO> items = BeanConvertUtils.mapList(entities, QuotationItemVO.class);
        for (QuotationItemVO item : items) {
            ProductSnapshot snapshot = byId.get(item.getSnapshotId());
            Assert.isNull(snapshot, "quotation.snapshot-missing", "原商品快照不存在");
            this.applySnapshot(item, snapshot);
        }
        return items;
    }

    private QuotationVO quotationView(Quotation header) {
        QuotationVO result = BeanConvertUtils.convert(header, QuotationVO.class);
        OffsetDateTime quotedAt = this.businessTime(header.getQuotedAt());
        result.setQuotedAt(quotedAt);
        result.setIssuerLogoUrl(issuerProperties.getLogoUrl());
        BigDecimal originalAmount = this.roundAmount(header.getOriginalAmount());
        BigDecimal subtotalAmount = this.roundAmount(header.getSubtotalAmount());
        BigDecimal totalAmount = this.roundAmount(header.getTotalAmount());
        result.setOriginalAmount(originalAmount);
        result.setSubtotalAmount(subtotalAmount);
        result.setTotalAmount(totalAmount);
        if (header.getDiscountedAmount() != null) {
            BigDecimal discountedAmount = this.roundAmount(header.getDiscountedAmount());
            result.setDiscountedAmount(discountedAmount);
        }
        return result;
    }

    private QuotationVO detail(QuotationVO result) {
        List<QuotationItemVO> items = result.getItems();
        Map<Long, List<ProductSnapshotImageVO>> imagesBySnapshot = this.snapshotImages(items);
        Map<Long, List<ProductOptionSpecVO>> specsBySnapshot = this.snapshotSpecs(items);
        for (QuotationItemVO item : items) {
            item.setQuotationId(result.getId());
            BigDecimal quantity = item.getQuantity().stripTrailingZeros();
            BigDecimal unitPrice = item.getUnitPrice().stripTrailingZeros();
            BigDecimal originalAmount = this.roundAmount(item.getOriginalAmount());
            BigDecimal lineAmount = this.roundAmount(item.getLineAmount());
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setOriginalAmount(originalAmount);
            item.setLineAmount(lineAmount);
            if (item.getDiscountedAmount() != null) {
                BigDecimal discountedAmount = this.roundAmount(item.getDiscountedAmount());
                item.setDiscountedAmount(discountedAmount);
            }
            if (item.getSnapshotId() != null) {
                List<ProductSnapshotImageVO> images = imagesBySnapshot.getOrDefault(item.getSnapshotId(), List.of());
                item.setImages(images);
                item.setSpecs(specsBySnapshot.getOrDefault(item.getSnapshotId(), List.of()));
            }
            String productImageUrl = item.getImages().stream()
                    .filter(image -> Boolean.TRUE.equals(image.getIsCover()))
                    .min(Comparator.comparing(image -> !ProductEnum.ImageType.SKU.getCode().equals(image.getImageSource())))
                    .map(ProductSnapshotImageVO::getUrl)
                    .orElse(null);
            item.setProductImageUrl(productImageUrl);
        }
        return result;
    }

    /**
     * 批量读取冻结规格，复用选品规格视图并保留快照顺序、名称、值和单位。
     */
    private Map<Long, List<ProductOptionSpecVO>> snapshotSpecs(List<QuotationItemVO> items) {
        List<Long> snapshotIds = items.stream().map(QuotationItemVO::getSnapshotId)
                .filter(Objects::nonNull).distinct().toList();
        if (snapshotIds.isEmpty()) {
            return Map.of();
        }

        LambdaQueryWrapper<ProductSnapshotSpec> query = Wrappers.lambdaQuery();
        query.in(ProductSnapshotSpec::getSnapshotId, snapshotIds);
        query.orderByAsc(ProductSnapshotSpec::getSnapshotId, ProductSnapshotSpec::getSortOrder, ProductSnapshotSpec::getId);
        List<ProductSnapshotSpec> specs = snapshotSpecMapper.selectList(query);
        List<ProductOptionSpecVO> views = BeanConvertUtils.mapList(specs, ProductOptionSpecVO.class);
        Map<Long, List<ProductOptionSpecVO>> specsBySnapshot = new HashMap<>();
        for (int index = 0; index < specs.size(); index++) {
            ProductSnapshotSpec spec = specs.get(index);
            ProductOptionSpecVO view = views.get(index);
            view.setSpecId(spec.getSourceSpecId());
            view.setSpecValueId(spec.getSourceSpecValueId());
            specsBySnapshot.computeIfAbsent(spec.getSnapshotId(), id -> new ArrayList<>()).add(view);
        }
        return specsBySnapshot;
    }

    /**
     * 批量读取冻结图片并重新生成授权地址，避免历史报价受当前商品图片变化影响。
     */
    private Map<Long, List<ProductSnapshotImageVO>> snapshotImages(List<QuotationItemVO> items) {
        List<Long> snapshotIds = items.stream().map(QuotationItemVO::getSnapshotId)
                .filter(Objects::nonNull).distinct().toList();
        if (snapshotIds.isEmpty()) {
            return Map.of();
        }

        LambdaQueryWrapper<ProductSnapshotImage> query = Wrappers.lambdaQuery();
        query.in(ProductSnapshotImage::getSnapshotId, snapshotIds);
        query.orderByAsc(
                ProductSnapshotImage::getImageSource,
                ProductSnapshotImage::getSortOrder,
                ProductSnapshotImage::getId
        );
        List<ProductSnapshotImage> images = snapshotImageMapper.selectList(query);
        List<ProductSnapshotImageVO> views = BeanConvertUtils.mapList(images, ProductSnapshotImageVO.class);
        List<Long> attachmentIds = images.stream().map(ProductSnapshotImage::getAttachmentId).distinct().toList();
        Map<Long, String> urls = attachmentService.getAuthorizedUrls(attachmentIds);
        Map<Long, List<ProductSnapshotImageVO>> imagesBySnapshot = new HashMap<>();
        for (int index = 0; index < images.size(); index++) {
            ProductSnapshotImage image = images.get(index);
            ProductSnapshotImageVO view = views.get(index);
            view.setUrl(urls.get(image.getAttachmentId()));
            imagesBySnapshot.computeIfAbsent(image.getSnapshotId(), id -> new ArrayList<>()).add(view);
        }
        return imagesBySnapshot;
    }

    private OffsetDateTime businessTime(OffsetDateTime quotedAt) {
        return quotedAt.atZoneSameInstant(BUSINESS_ZONE).toOffsetDateTime();
    }

}
