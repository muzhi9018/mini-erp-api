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
import com.muzhi.minierp.entity.sales.Quotation;
import com.muzhi.minierp.entity.sales.QuotationItem;
import com.muzhi.minierp.enums.CurrencyEnum;
import com.muzhi.minierp.mapper.product.ProductSnapshotMapper;
import com.muzhi.minierp.mapper.sales.QuotationItemMapper;
import com.muzhi.minierp.mapper.sales.QuotationMapper;
import com.muzhi.minierp.model.LoginUser;
import com.muzhi.minierp.model.sales.QuotationCalculation;
import com.muzhi.minierp.model.sales.QuotationLine;
import com.muzhi.minierp.model.sales.QuotationSkuSelection;
import com.muzhi.minierp.security.SecurityUtils;
import com.muzhi.minierp.service.product.IProductSnapshotService;
import com.muzhi.minierp.service.sales.IQuotationItemService;
import com.muzhi.minierp.service.sales.IQuotationService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.product.ProductOptionVO;
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

    private final IProductSnapshotService snapshotService;

    private final IQuotationItemService quotationItemService;

    private final QuotationIssuerProperties issuerProperties;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public QuotationDocumentVO preview(QuotationFormVO form) {
        Quotation source = this.copySource(form.reference(), false);
        QuotationCalculation result = this.calculate(form, source, false, false);
        return this.document(result.header(), result.lines());
    }

    @Override
    @Retryable(includes = PessimisticLockingFailureException.class, maxRetries = 1, delay = 50)
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public QuotationSummaryVO create(QuotationFormVO form) {
        Quotation source = this.copySource(form.reference(), true);
        QuotationCalculation result = this.calculate(form, source, false, true);
        Quotation header = result.header();
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
        for (QuotationLine line : result.lines()) {
            QuotationItem item = line.item();
            item.setId(IdWorker.getId());
            item.setQuotationId(header.getId());
            itemMapper.insert(item);
        }
        QuotationSummaryVO reference = new QuotationSummaryVO();
        reference.setId(header.getId());
        reference.setQuoteNumber(quoteNumber);
        return reference;
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
    public IPage<QuotationSummaryVO> list(QuotationQueryVO query, Integer pageNum, Integer pageSize) {
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
        return result.convert(quote -> {
            QuotationSummaryVO summary = BeanConvertUtils.convert(quote, QuotationSummaryVO.class);
            summary.setQuotedAt(this.businessTime(summary.getQuotedAt()));
            summary.setTotalAmount(this.roundAmount(summary.getTotalAmount()));
            return summary;
        });
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public QuotationDetailVO detail(Long id) {
        Quotation header = this.owned(id, false);
        List<QuotationLine> lines = this.readLines(id);
        return this.detail(header, lines);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public QuotationDocumentVO previewUpdate(QuotationFormVO form) {
        Quotation source = this.editable(form.reference(), false);
        QuotationCalculation result = this.calculate(form, source, true, false);
        return this.document(result.header(), result.lines());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public QuotationDetailVO update(QuotationFormVO form) {
        Quotation source = this.editable(form.reference(), true);
        QuotationCalculation result = this.calculate(form, source, true, true);
        Quotation header = result.header();
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

        List<QuotationLine> lines = result.lines();
        this.replaceItems(header.getId(), lines);
        return this.detail(header, lines);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void delete(QuotationVersionedId reference) {
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
    public QuotationDetailVO copy(QuotationCopyRequest request) {
        Quotation source = this.owned(request.id(), false);
        List<QuotationLine> lines = this.readLines(source.getId());
        if (Boolean.TRUE.equals(request.clearCustomer())) {
            source.setCustomerName(null);
            source.setContactName(null);
            source.setContactPhone(null);
            source.setShippingAddress(null);
        }
        source.setQuoteNumber(null);
        QuotationFormVO form = this.form(source, lines);
        QuotationDocumentVO document = this.document(source, lines);
        List<Long> skuIds = this.skuIds(lines);
        return new QuotationDetailVO(null, null, null, form, document, skuIds);
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

    private Quotation copySource(QuotationVersionedId reference, boolean forWrite) {
        if (reference == null) {
            return null;
        }
        LoginUser user = SecurityUtils.getCurrentUser();
        boolean canRead = user != null && user.getAuthorities().stream()
                .anyMatch(authority -> "sales-manager:quotation:query".equals(authority.getAuthority()));
        if (!canRead) {
            throw new AccessDeniedException("quotation.read-source-denied");
        }
        Quotation source = this.owned(reference.id(), forWrite);
        this.checkVersion(source, reference);
        return source;
    }

    private Quotation editable(QuotationVersionedId reference, boolean forWrite) {
        Quotation quotation = this.owned(reference.id(), forWrite);
        Assert.isTrue(Boolean.TRUE.equals(quotation.getOrderGenerated()), "quotation.order-generated", "已生成订单的报价不能修改或删除");
        this.checkVersion(quotation, reference);
        return quotation;
    }

    private void checkVersion(Quotation source, QuotationVersionedId reference) {
        boolean conflict = !Objects.equals(source.getVersion(), reference.expectedVersion());
        Assert.isTrue(conflict, "quotation.version-conflict", "报价已更新，请重新读取后确认");
    }

    /**
     * 新选商品先批量核验，沿用行只使用原快照和单价。
     */
    private QuotationCalculation calculate(QuotationFormVO form, Quotation source, boolean editing, boolean forWrite) {
        Map<Long, QuotationLine> originals = Map.of();
        if (source != null && form.items().stream().anyMatch(item -> item.sourceItemId() != null)) {
            List<QuotationLine> lines = this.readLines(source.getId());
            originals = lines.stream().collect(Collectors.toMap(line -> line.item().getId(), Function.identity()));
        }
        List<Long> newSkuIds = form.items().stream().map(QuotationFormVO.Item::skuId).filter(Objects::nonNull).toList();
        Map<Long, QuotationSkuSelection> selections = quotationItemService.loadSkuSelections(newSkuIds, forWrite);
        List<QuotationLine> lines = new ArrayList<>();
        String currency = editing ? source.getCurrency() : null;
        BigDecimal originalAmount = BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;
        for (QuotationFormVO.Item input : form.items()) {
            int position = lines.size() + 1;
            QuotationLine line;
            if (input.sourceItemId() != null) {
                QuotationLine original = originals.get(input.sourceItemId());
                Assert.isNull(original, "quotation.source-item-not-found", "第 {0} 行引用的原报价明细不存在", position);
                QuotationItem originalItem = original.item();
                BigDecimal unitPrice = originalItem.getUnitPrice();
                String lineCurrency = originalItem.getCurrency();
                QuotationItem pricedItem = this.priceLine(input, original.quantityScale(), unitPrice, lineCurrency, position);
                pricedItem.setSnapshotId(originalItem.getSnapshotId());
                if (editing) {
                    pricedItem.setId(originalItem.getId());
                }
                line = original.withItem(pricedItem);
            } else {
                QuotationSkuSelection selection = selections.get(input.skuId());
                line = this.priceSelectedLine(input, selection, position);
            }
            QuotationItem item = line.item();
            if (currency == null) {
                currency = item.getCurrency();
            }
            Assert.isTrue(!currency.equals(item.getCurrency()), "quotation.currency-conflict",
                    "第 {0} 行的币种必须与报价币种一致", position);
            originalAmount = originalAmount.add(item.getOriginalAmount());
            subtotal = subtotal.add(item.getLineAmount());
            lines.add(line);
        }
        this.checkAmountRange(originalAmount);
        BigDecimal total = this.afterDiscount(subtotal, form.discount(), 0);
        if (forWrite) {
            this.freezeSelectedItems(form.items(), lines);
        }

        Quotation header = this.header(form, source, editing);
        header.setCurrency(currency);
        header.setOriginalAmount(originalAmount);
        header.setSubtotalAmount(subtotal);
        header.setTotalAmount(total);
        return new QuotationCalculation(header, lines);
    }

    private QuotationLine priceSelectedLine(QuotationFormVO.Item input, QuotationSkuSelection selection, int position) {
        boolean unavailable = selection == null || !selection.issues().isEmpty();
        Assert.isTrue(unavailable, "quotation.sku-unavailable",
                "第 {0} 行的商品或 SKU 不可用，请刷新或替换", position);

        ProductOptionVO option = selection.option();
        boolean changed = !Objects.equals(input.productVersion(), option.getProductVersion())
                || !Objects.equals(input.skuVersion(), option.getSkuVersion());
        Assert.isTrue(changed, "quotation.sku-version-conflict",
                "第 {0} 行的商品资料已变更，请刷新该行", position);

        BigDecimal unitPrice = option.getReferenceSalePrice();
        String currency = option.getReferenceSaleCurrency();
        QuotationItem item = this.priceLine(input, option.getQuantityScale(), unitPrice, currency, position);
        return QuotationLine.fromOption(item, option);
    }

    private QuotationItem priceLine(
            QuotationFormVO.Item input, int quantityScale, BigDecimal unitPrice, String currency, int position
    ) {
        BigDecimal quantity = input.quantity().stripTrailingZeros();
        Assert.isTrue(quantity.scale() > quantityScale, "quotation.quantity-scale",
                "第 {0} 行的数量最多支持 {1} 位小数", position, quantityScale);
        BigDecimal unrounded = unitPrice.multiply(quantity);
        BigDecimal original = this.roundAmount(unrounded);
        this.checkAmountRange(original);
        BigDecimal amount = this.afterDiscount(original, input.discount(), position);

        QuotationItem item = new QuotationItem();
        item.setSortOrder(position - 1);
        item.setCurrency(currency);
        item.setUnitPrice(unitPrice);
        item.setQuantity(quantity);
        item.setOriginalAmount(original);
        item.setLineAmount(amount);
        item.setRemark(input.remark());
        if (input.discount() != null) {
            item.setDiscountRate(input.discount().discountRate());
            item.setDiscountedAmount(input.discount().discountedAmount());
        }
        return item;
    }

    /**
     * 计算优惠后的金额，位置为 0 时表示整单。
     */
    private BigDecimal afterDiscount(BigDecimal before, QuotationFormVO.Discount discount, int position) {
        if (discount == null) {
            return before;
        }
        if (discount.discountRate() != null) {
            BigDecimal discounted = before.multiply(discount.discountRate()).movePointLeft(2);
            return this.roundAmount(discounted);
        }
        BigDecimal fixed = discount.discountedAmount();
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

    private String decimalText(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String moneyText(BigDecimal value) {
        return this.roundAmount(value).toPlainString();
    }

    /**
     * 所有新选商品已按固定顺序取得行锁，重复 SKU 共用快照。
     */
    private void freezeSelectedItems(List<QuotationFormVO.Item> inputs, List<QuotationLine> lines) {
        Map<Long, ProductSnapshot> snapshots = new HashMap<>();
        for (int index = 0; index < inputs.size(); index++) {
            QuotationFormVO.Item input = inputs.get(index);
            if (input.skuId() == null) {
                continue;
            }
            ProductSnapshot snapshot = snapshots.get(input.skuId());
            if (snapshot == null) {
                snapshot = snapshotService.getOrCreate(input.skuId(), input.productVersion(), input.skuVersion());
                snapshots.put(input.skuId(), snapshot);
            }
            QuotationItem item = lines.get(index).item();
            item.setSnapshotId(snapshot.getId());
            QuotationLine frozenLine = QuotationLine.fromSnapshot(item, snapshot);
            lines.set(index, frozenLine);
        }
    }

    private Quotation header(QuotationFormVO form, Quotation source, boolean editing) {
        Quotation header = new Quotation();
        if (editing) {
            header.setId(source.getId());
            header.setQuoteNumber(source.getQuoteNumber());
            header.setOrderGenerated(source.getOrderGenerated());
            header.setVersion(source.getVersion());
        }
        header.setTitle(form.title() == null ? "报价单" : form.title().strip());
        OffsetDateTime quotedAt = form.quotedAt();
        if (quotedAt == null) {
            quotedAt = OffsetDateTime.now(BUSINESS_ZONE);
        }
        header.setQuotedAt(quotedAt);
        header.setIssuerName(source == null ? issuerProperties.getName() : source.getIssuerName());
        header.setIssuerContact(source == null ? issuerProperties.getContact() : source.getIssuerContact());
        header.setRemark(form.remark());
        QuotationCustomerVO customer = form.customer();
        if (customer != null) {
            header.setCustomerName(customer.getCustomerName());
            header.setContactName(customer.getContactName());
            header.setContactPhone(customer.getContactPhone());
            header.setShippingAddress(customer.getShippingAddress());
        }
        if (form.discount() != null) {
            header.setDiscountRate(form.discount().discountRate());
            header.setDiscountedAmount(form.discount().discountedAmount());
        }
        return header;
    }

    /**
     * 更新保留行的业务字段，按提交顺序新增与移除其他行。
     */
    private void replaceItems(Long quotationId, List<QuotationLine> lines) {
        List<Long> retainedIds = lines.stream().map(line -> line.item().getId()).filter(Objects::nonNull).toList();
        LambdaUpdateWrapper<QuotationItem> removed = Wrappers.lambdaUpdate();
        removed.eq(QuotationItem::getQuotationId, quotationId);
        removed.notIn(!retainedIds.isEmpty(), QuotationItem::getId, retainedIds);
        removed.set(QuotationItem::getIsDeleted, true);
        QuotationItem audit = new QuotationItem();
        itemMapper.update(audit, removed);

        for (QuotationLine line : lines) {
            QuotationItem item = line.item();
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
    private List<QuotationLine> readLines(Long quotationId) {
        LambdaQueryWrapper<QuotationItem> query = Wrappers.lambdaQuery();
        query.eq(QuotationItem::getQuotationId, quotationId);
        query.orderByAsc(QuotationItem::getSortOrder, QuotationItem::getId);
        List<QuotationItem> items = itemMapper.selectList(query);
        if (items.isEmpty()) {
            return List.of();
        }

        List<Long> ids = items.stream().map(QuotationItem::getSnapshotId).distinct().toList();
        List<ProductSnapshot> snapshots = snapshotMapper.selectByIds(ids);
        Map<Long, ProductSnapshot> byId = snapshots.stream().collect(Collectors.toMap(ProductSnapshot::getId, Function.identity()));
        List<QuotationLine> lines = new ArrayList<>();
        for (QuotationItem item : items) {
            ProductSnapshot snapshot = byId.get(item.getSnapshotId());
            Assert.isNull(snapshot, "quotation.snapshot-missing", "原商品快照不存在");
            QuotationLine line = QuotationLine.fromSnapshot(item, snapshot);
            lines.add(line);
        }
        return lines;
    }

    private QuotationDetailVO detail(Quotation header, List<QuotationLine> lines) {
        QuotationFormVO form = this.form(header, lines);
        QuotationDocumentVO document = this.document(header, lines);
        List<Long> skuIds = this.skuIds(lines);
        return new QuotationDetailVO(header.getId(), header.getVersion(), header.getOrderGenerated(), form, document, skuIds);
    }

    private List<Long> skuIds(List<QuotationLine> lines) {
        return lines.stream().map(QuotationLine::skuId).toList();
    }

    private QuotationFormVO form(Quotation header, List<QuotationLine> lines) {
        List<QuotationFormVO.Item> items = new ArrayList<>();
        for (QuotationLine line : lines) {
            QuotationItem item = line.item();
            QuotationFormVO.Discount discount = this.discount(item.getDiscountRate(), item.getDiscountedAmount());
            BigDecimal quantity = item.getQuantity().stripTrailingZeros();
            items.add(new QuotationFormVO.Item(item.getId(), null, null, null, quantity, discount, item.getRemark()));
        }
        QuotationVersionedId reference = new QuotationVersionedId(header.getId(), header.getVersion());
        QuotationCustomerVO customer = BeanConvertUtils.convert(header, QuotationCustomerVO.class);
        QuotationFormVO.Discount discount = this.discount(header.getDiscountRate(), header.getDiscountedAmount());
        OffsetDateTime quotedAt = this.businessTime(header.getQuotedAt());
        return new QuotationFormVO(reference, header.getTitle(), quotedAt, customer, header.getRemark(), discount, items);
    }

    private QuotationDocumentVO document(Quotation header, List<QuotationLine> lines) {
        QuotationDocumentVO.Issuer issuer = new QuotationDocumentVO.Issuer(
                header.getIssuerName(), header.getIssuerContact(), issuerProperties.getLogoUrl()
        );
        QuotationCustomerVO customer = BeanConvertUtils.convert(header, QuotationCustomerVO.class);
        QuotationFormVO.Discount discount = this.discount(header.getDiscountRate(), header.getDiscountedAmount());
        BigDecimal lineDiscount = header.getOriginalAmount().subtract(header.getSubtotalAmount());
        BigDecimal orderDiscount = header.getSubtotalAmount().subtract(header.getTotalAmount());
        QuotationDocumentVO.Totals totals = new QuotationDocumentVO.Totals(
                this.moneyText(header.getOriginalAmount()), this.moneyText(lineDiscount),
                this.moneyText(header.getSubtotalAmount()), this.moneyText(orderDiscount),
                this.moneyText(header.getTotalAmount())
        );
        List<QuotationDocumentVO.Item> items = lines.stream().map(this::documentItem).toList();
        OffsetDateTime quotedAt = this.businessTime(header.getQuotedAt());
        return new QuotationDocumentVO(header.getQuoteNumber(), header.getTitle(), quotedAt, header.getCurrency(),
                issuer, customer, header.getRemark(), discount, totals, items);
    }

    private QuotationDocumentVO.Item documentItem(QuotationLine line) {
        QuotationItem item = line.item();
        QuotationFormVO.Discount discount = this.discount(item.getDiscountRate(), item.getDiscountedAmount());
        BigDecimal discountAmount = item.getOriginalAmount().subtract(item.getLineAmount());
        return new QuotationDocumentVO.Item(
                line.skuCode(), line.productName(), line.model(), line.specificationText(), line.unitName(), line.quantityScale(),
                this.decimalText(item.getQuantity()), this.decimalText(item.getUnitPrice()), discount,
                this.moneyText(item.getOriginalAmount()), this.moneyText(discountAmount),
                this.moneyText(item.getLineAmount()), item.getRemark()
        );
    }

    private OffsetDateTime businessTime(OffsetDateTime quotedAt) {
        return quotedAt.atZoneSameInstant(BUSINESS_ZONE).toOffsetDateTime();
    }

    private QuotationFormVO.Discount discount(BigDecimal rate, BigDecimal amount) {
        if (rate == null && amount == null) {
            return null;
        }
        BigDecimal scaledAmount = amount == null ? null : this.roundAmount(amount);
        return new QuotationFormVO.Discount(rate, scaledAmount);
    }
}
