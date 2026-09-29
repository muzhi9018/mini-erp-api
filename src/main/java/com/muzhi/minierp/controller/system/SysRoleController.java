package com.muzhi.minierp.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.entity.system.SysRole;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.system.ISysRoleService;
import com.muzhi.minierp.vo.SysMenuVO;
import com.muzhi.minierp.vo.SysRoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 系统角色信息 前端控制器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-08-13
 */
@Slf4j
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final ISysRoleService sysRoleService;

    @PostMapping("/addRole")
    public JsonResult<Boolean> addRole(@RequestBody SysRole sysRole) {
        sysRoleService.addRole(sysRole);
        return JsonResult.success(true);
    }

    @PostMapping("/delRole")
    public JsonResult<Boolean> delRole(@RequestBody SysRole sysRole) {
        sysRoleService.delRole(sysRole);
        return JsonResult.success(true);
    }

    @PostMapping("/editRole")
    public JsonResult<Boolean> editRole(@RequestBody SysRole sysRole) {
        sysRoleService.editRole(sysRole);
        return JsonResult.success(true);
    }

    @GetMapping("/findByPage")
    public JsonResult<IPage<SysRoleVO>> findByPage(@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "15") Integer pageSize, SysRoleVO query) {
        IPage<SysRoleVO> page = sysRoleService.findByPage(pageNum, pageSize, query);
        return JsonResult.success(page);
    }

    /**
     * 查询全部角色，供用户授权时选择。
     *
     * @return 角色列表
     */
    @GetMapping("/listAll")
    public JsonResult<List<SysRole>> listAll() {
        List<SysRole> roles = sysRoleService.listAll();
        return JsonResult.success(roles);
    }

    @GetMapping("/roleMenuDetail")
    public JsonResult<?> roleMenuDetail(@RequestParam Long id) {
        List<SysMenuVO> list = sysRoleService.roleMenuDetail(id);
        return JsonResult.success(list);
    }

    @PostMapping("/roleAuthorize")
    public JsonResult<?> roleAuthorize(@RequestBody SysRoleVO sysRole) {
        sysRoleService.roleAuthorize(sysRole);
        return JsonResult.success();
    }
}
