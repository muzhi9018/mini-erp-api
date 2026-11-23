package com.muzhi.minierp.service.sales.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.sales.QuotationTemplate;
import com.muzhi.minierp.entity.sales.QuotationTemplateItem;
import com.muzhi.minierp.enums.QuotationEnum.Scope;
import com.muzhi.minierp.mapper.sales.QuotationTemplateItemMapper;
import com.muzhi.minierp.mapper.sales.QuotationTemplateMapper;
import com.muzhi.minierp.model.LoginUser;
import com.muzhi.minierp.model.sales.QuotationSkuSelection;
import com.muzhi.minierp.security.SecurityUtils;
import com.muzhi.minierp.service.sales.IQuotationItemService;
import com.muzhi.minierp.service.sales.IQuotationTemplateService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.product.ProductOptionVO;
import com.muzhi.minierp.vo.sales.QuotationTemplateVO;
import com.muzhi.minierp.vo.sales.QuotationVersionedId;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 个人及公共报价模板服务。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Service
@RequiredArgsConstructor
public class QuotationTemplateServiceImpl extends ServiceImpl<QuotationTemplateMapper, QuotationTemplate> implements IQuotationTemplateService {

    /**
     * 模板明细的售价币种不一致。
     */
    private static final String CURRENCY_CONFLICT = "CURRENCY_CONFLICT";

    private final QuotationTemplateItemMapper itemMapper;

    private final IQuotationItemService quotationItemService;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public IPage<QuotationTemplateVO> list(Scope scope, String keyword, Integer pageNum, Integer pageSize) {
        Long userId = this.currentUserId();
        LambdaQueryWrapper<QuotationTemplate> criteria = this.visible(scope, userId, false);
        String search = StringUtils.trimToNull(keyword);
        if (search != null) {
            criteria.and(query -> query.like(QuotationTemplate::getName, search).or().like(QuotationTemplate::getTitle, search));
        }
        criteria.orderByDesc(QuotationTemplate::getGmtModified, QuotationTemplate::getId);
        Page<QuotationTemplate> page = new Page<>(pageNum, pageSize);
        Page<QuotationTemplate> result = super.baseMapper.selectPage(page, criteria);

        List<Long> ids = result.getRecords().stream().map(QuotationTemplate::getId).toList();
        List<QuotationTemplateItem> items = this.readItems(ids);
        Map<Long, List<QuotationTemplateItem>> byTemplate = items.stream().collect(Collectors.groupingBy(QuotationTemplateItem::getTemplateId));
        Map<Long, QuotationSkuSelection> selections = this.selectItems(items, false);
        return result.convert(template -> {
            List<QuotationTemplateItem> templateItems = byTemplate.getOrDefault(template.getId(), List.of());
            List<QuotationTemplateVO.Item> details = this.inspectItems(templateItems, selections);
            return this.summary(template, details, userId);
        });
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public QuotationTemplateVO detail(Scope scope, Long id) {
        QuotationTemplate template = this.find(scope, id, false);
        List<QuotationTemplateVO.Item> details = this.inspectTemplate(id);
        return this.detail(template, details);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuotationTemplateVO create(Scope scope, QuotationTemplateVO form) {
        Long id = IdWorker.getId();
        List<QuotationTemplateItem> items = this.formItems(id, form.getItems());
        List<QuotationTemplateVO.Item> details = this.inspectForWrite(items);

        QuotationTemplate template = new QuotationTemplate();
        template.setId(id);
        template.setOwnerUserId(this.currentUserId());
        template.setScope(scope.getCode());
        template.setVersion(0L);
        template.setName(form.getName().strip());
        template.setTitle(form.getTitle().strip());
        template.setDiscountRate(form.getDiscountRate());
        template.setRemark(form.getRemark());
        super.baseMapper.insert(template);
        for (QuotationTemplateItem item : items) {
            itemMapper.insert(item);
        }
        return this.detail(template, details);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuotationTemplateVO update(Scope scope, QuotationTemplateVO form) {
        QuotationTemplate template = this.find(scope, form.getId(), true);
        this.checkVersion(template, form.getExpectedVersion());
        List<QuotationTemplateItem> items = this.formItems(template.getId(), form.getItems());
        List<QuotationTemplateVO.Item> details = this.inspectForWrite(items);

        Long nextVersion = Math.incrementExact(template.getVersion());
        LambdaUpdateWrapper<QuotationTemplate> update = Wrappers.lambdaUpdate();
        update.eq(QuotationTemplate::getId, template.getId());
        update.set(QuotationTemplate::getName, form.getName().strip());
        update.set(QuotationTemplate::getTitle, form.getTitle().strip());
        update.set(QuotationTemplate::getDiscountRate, form.getDiscountRate());
        update.set(QuotationTemplate::getRemark, form.getRemark());
        update.set(QuotationTemplate::getVersion, nextVersion);
        QuotationTemplate audit = new QuotationTemplate();
        super.baseMapper.update(audit, update);
        this.replaceItems(template.getId(), items);

        QuotationTemplate saved = this.find(scope, template.getId(), false);
        return this.detail(saved, details);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Scope scope, QuotationVersionedId reference) {
        Long userId = this.currentUserId();
        LambdaQueryWrapper<QuotationTemplate> query = this.visible(scope, userId, true);
        query.eq(QuotationTemplate::getId, reference.id());
        query.last("FOR UPDATE");
        QuotationTemplate template = super.baseMapper.selectOne(query);
        Assert.isNull(template, "quotation.template.not-found", "报价模板不存在或已删除");
        this.checkVersion(template, reference.expectedVersion());

        Long nextVersion = Math.incrementExact(template.getVersion());
        LambdaUpdateWrapper<QuotationTemplate> remove = Wrappers.lambdaUpdate();
        remove.eq(QuotationTemplate::getId, template.getId());
        remove.set(QuotationTemplate::getVersion, nextVersion);
        remove.set(QuotationTemplate::getIsDeleted, true);
        QuotationTemplate audit = new QuotationTemplate();
        super.baseMapper.update(audit, remove);
        this.deleteItems(template.getId());
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public QuotationTemplateVO apply(Scope scope, Long id) {
        QuotationTemplate template = this.find(scope, id, false);
        List<QuotationTemplateVO.Item> details = this.inspectTemplate(id);
        boolean unavailable = details.isEmpty() || this.countIssues(details) > 0;
        Assert.isTrue(unavailable, "quotation.template.unusable", "当前模板有不可用的商品明细，请修复后再套用");
        List<QuotationTemplateVO.Item> items = new ArrayList<>();
        for (QuotationTemplateVO.Item item : details) {
            QuotationTemplateVO.Item appliedItem = new QuotationTemplateVO.Item();
            appliedItem.setProduct(item.getProduct());
            appliedItem.setQuantity("1");
            appliedItem.setDiscountRate(item.getDiscountRate());
            appliedItem.setRemark(item.getRemark());
            items.add(appliedItem);
        }

        QuotationTemplateVO applied = new QuotationTemplateVO();
        applied.setTitle(template.getTitle());
        BigDecimal discountRate = this.displayDiscountRate(template.getDiscountRate());
        applied.setDiscountRate(discountRate);
        applied.setRemark(template.getRemark());
        applied.setItems(items);
        return applied;
    }

    private LambdaQueryWrapper<QuotationTemplate> visible(Scope scope, Long userId, boolean creatorOnly) {
        LambdaQueryWrapper<QuotationTemplate> query = Wrappers.lambdaQuery();
        query.eq(QuotationTemplate::getScope, scope.getCode());
        query.eq(creatorOnly || scope == Scope.PERSONAL, QuotationTemplate::getOwnerUserId, userId);
        return query;
    }

    private QuotationTemplate find(Scope scope, Long id, boolean forWrite) {
        Long userId = this.currentUserId();
        LambdaQueryWrapper<QuotationTemplate> query = this.visible(scope, userId, false);
        query.eq(QuotationTemplate::getId, id);
        query.last(forWrite, "FOR UPDATE");
        QuotationTemplate template = super.baseMapper.selectOne(query);
        Assert.isNull(template, "quotation.template.not-found", "报价模板不存在或已删除");
        return template;
    }

    private void checkVersion(QuotationTemplate template, Long expectedVersion) {
        boolean conflict = !Objects.equals(template.getVersion(), expectedVersion);
        Assert.isTrue(conflict, "quotation.template.version-conflict", "报价模板已更新，请重新读取后确认");
    }

    private Long currentUserId() {
        LoginUser user = SecurityUtils.getCurrentUser();
        if (user == null || user.getId() == null) {
            throw new AccessDeniedException("quotation.authentication-required");
        }
        return user.getId();
    }

    /**
     * 全量替换有效行，让同一 SKU 的不同位置保留各自的备注和优惠。
     */
    private void replaceItems(Long templateId, List<QuotationTemplateItem> items) {
        this.deleteItems(templateId);
        for (QuotationTemplateItem item : items) {
            itemMapper.insert(item);
        }
    }

    private void deleteItems(Long templateId) {
        LambdaUpdateWrapper<QuotationTemplateItem> remove = Wrappers.lambdaUpdate();
        remove.eq(QuotationTemplateItem::getTemplateId, templateId);
        remove.set(QuotationTemplateItem::getIsDeleted, true);
        QuotationTemplateItem audit = new QuotationTemplateItem();
        itemMapper.update(audit, remove);
    }

    private List<QuotationTemplateItem> formItems(Long templateId, List<QuotationTemplateVO.Item> inputs) {
        List<QuotationTemplateItem> items = new ArrayList<>();
        for (QuotationTemplateVO.Item input : inputs) {
            QuotationTemplateItem item = new QuotationTemplateItem();
            item.setId(IdWorker.getId());
            item.setTemplateId(templateId);
            item.setSkuId(input.getSkuId());
            item.setSortOrder(items.size());
            item.setDiscountRate(input.getDiscountRate());
            item.setRemark(input.getRemark());
            items.add(item);
        }
        return items;
    }

    private List<QuotationTemplateItem> readItems(List<Long> templateIds) {
        if (templateIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<QuotationTemplateItem> query = Wrappers.lambdaQuery();
        query.in(QuotationTemplateItem::getTemplateId, templateIds);
        query.orderByAsc(QuotationTemplateItem::getTemplateId, QuotationTemplateItem::getSortOrder);
        return itemMapper.selectList(query);
    }

    private Map<Long, QuotationSkuSelection> selectItems(List<QuotationTemplateItem> items, boolean forWrite) {
        List<Long> skuIds = items.stream().map(QuotationTemplateItem::getSkuId).toList();
        return quotationItemService.loadSkuSelections(skuIds, forWrite);
    }

    private List<QuotationTemplateVO.Item> inspectTemplate(Long id) {
        List<Long> ids = List.of(id);
        List<QuotationTemplateItem> items = this.readItems(ids);
        Map<Long, QuotationSkuSelection> selections = this.selectItems(items, false);
        return this.inspectItems(items, selections);
    }

    private List<QuotationTemplateVO.Item> inspectForWrite(List<QuotationTemplateItem> items) {
        Map<Long, QuotationSkuSelection> selections = this.selectItems(items, true);
        List<QuotationTemplateVO.Item> details = this.inspectItems(items, selections);
        for (QuotationTemplateVO.Item item : details) {
            int position = item.getSortOrder() + 1;
            Assert.isTrue(item.getIssues().contains(CURRENCY_CONFLICT), "quotation.template.currency-conflict",
                    "第 {0} 行的售价币种与其他模板明细不一致", position);
            Assert.isTrue(!item.getIssues().isEmpty(), "quotation.template.invalid-item",
                    "第 {0} 行的 SKU 当前不可用，请替换或移除", position);
        }
        return details;
    }

    /**
     * 每个模板单独检测币种分化，返回全部受影响行的实时资料。
     */
    private List<QuotationTemplateVO.Item> inspectItems(
            List<QuotationTemplateItem> items, Map<Long, QuotationSkuSelection> selections
    ) {
        Set<String> currencies = new HashSet<>();
        for (QuotationTemplateItem item : items) {
            QuotationSkuSelection selection = selections.get(item.getSkuId());
            if (selection != null && selection.issues().isEmpty()) {
                currencies.add(selection.referenceSaleCurrency());
            }
        }

        List<QuotationTemplateVO.Item> details = new ArrayList<>();
        for (QuotationTemplateItem item : items) {
            QuotationSkuSelection selection = selections.get(item.getSkuId());
            List<String> issues = new ArrayList<>();
            ProductOptionVO option = null;
            String currency = null;
            if (selection == null) {
                issues.add("SKU_MISSING");
            } else {
                issues.addAll(selection.issues());
                if (issues.isEmpty() && currencies.size() > 1) {
                    issues.add(CURRENCY_CONFLICT);
                }
                option = selection.option();
                currency = selection.referenceSaleCurrency();
            }
            QuotationTemplateVO.Item detail = BeanConvertUtils.convert(item, QuotationTemplateVO.Item.class);
            BigDecimal discountRate = this.displayDiscountRate(item.getDiscountRate());
            detail.setDiscountRate(discountRate);
            detail.setReferenceSaleCurrency(currency);
            detail.setProduct(option);
            detail.setIssues(issues);
            details.add(detail);
        }
        return details;
    }

    private QuotationTemplateVO summary(QuotationTemplate template, List<QuotationTemplateVO.Item> items, Long userId) {
        QuotationTemplateVO summary = BeanConvertUtils.convert(template, QuotationTemplateVO.class);
        boolean ownedByCurrentUser = Objects.equals(template.getOwnerUserId(), userId);
        summary.setOwnedByCurrentUser(ownedByCurrentUser);
        int issueCount = this.countIssues(items);
        summary.setIssueCount(issueCount);
        summary.setUsable(!items.isEmpty() && issueCount == 0);
        return summary;
    }

    private QuotationTemplateVO detail(QuotationTemplate template, List<QuotationTemplateVO.Item> items) {
        Long userId = this.currentUserId();
        QuotationTemplateVO detail = this.summary(template, items, userId);
        BigDecimal discountRate = this.displayDiscountRate(template.getDiscountRate());
        detail.setDiscountRate(discountRate);
        detail.setRemark(template.getRemark());
        detail.setItems(items);
        return detail;
    }

    private int countIssues(List<QuotationTemplateVO.Item> items) {
        long count = items.stream().filter(item -> !item.getIssues().isEmpty()).count();
        return (int) count;
    }

    private BigDecimal displayDiscountRate(BigDecimal rate) {
        if (rate == null) {
            return null;
        }
        BigDecimal normalized = rate.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
    }
}
