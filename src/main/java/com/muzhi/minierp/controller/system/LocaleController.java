package com.muzhi.minierp.controller.system;

import com.muzhi.minierp.enums.SysLocale;
import com.muzhi.minierp.model.JsonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * <p>
 * 国际化语言 前端控制器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-04
 */
@RestController
@RequestMapping("/locale")
public class LocaleController {


    @GetMapping("/systemLocals")
    public JsonResult<List<SysLocale.Bean>> systemLocals() {
        List<SysLocale.Bean> beans = SysLocale.beans();
        return JsonResult.success(beans);
    }

}
