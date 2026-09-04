package com.muzhi.minierp.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.entity.system.SysMenu;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.system.ISysMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 系统菜单 前端控制器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-08-13
 */
@Slf4j
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final ISysMenuService sysMenuService;

    @GetMapping("/findTopLevelMenu")
    public JsonResult<?> findTopLevelMenu(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "15") Integer pageSize) {
        IPage<SysMenu> page = sysMenuService.findTopLevelMenu(current, pageSize);
        return JsonResult.success(page);
    }

    @GetMapping("/findByParentId")
    public JsonResult<?> findByParentId(@RequestParam Long parentId) {
        List<SysMenu> list = sysMenuService.findByParentId(parentId);
        return JsonResult.success(list);
    }

    @PostMapping("/addMenu")
    public JsonResult<?> addMenu(@RequestBody SysMenu sysMenu) {
        sysMenuService.addMenu(sysMenu);
        return JsonResult.success();
    }

    @PostMapping("/updateMenu")
    public JsonResult<?> updateMenu(@RequestBody SysMenu sysMenu) {
        sysMenuService.updateMenu(sysMenu);
        return JsonResult.success();
    }

    @PostMapping("/del")
    public JsonResult<?> del(@RequestBody SysMenu sysMenu) {
        sysMenuService.delById(sysMenu.getId());
        return JsonResult.success();
    }

}
