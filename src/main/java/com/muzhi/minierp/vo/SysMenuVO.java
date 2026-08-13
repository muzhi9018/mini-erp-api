package com.muzhi.minierp.vo;

import com.muzhi.minierp.dto.SysMenuDTO;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * <p>
 * {@code SysMenuVO}: 系统菜单视图对象
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2023/12/20 17:30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMappers(@AutoMapper(target = SysMenuDTO.class))
public class SysMenuVO extends SysMenuDTO {

    @Serial
    private static final long serialVersionUID = -4011747186629680606L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 父级id (顶级菜单为 0)
     */
    private Long parentId;

    /**
     * 子节点
     */
    private List<SysMenuVO> children;

    /**
     * 是否选中
     */
    private boolean selected;

}
