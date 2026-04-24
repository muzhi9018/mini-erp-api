package com.muzhi.minierp.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.muzhi.minierp.model.User;
import com.muzhi.minierp.security.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * <p>
 * {@code FieldFillHandler}: Mybatis Plus 字段填充处理器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2023/8/25 16:36
 */
public class FieldFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "gmtCreate", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "gmtModified", LocalDateTime::now, LocalDateTime.class);
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            this.strictInsertFill(metaObject, "createUser", currentUser::getId, Long.class);
            this.strictInsertFill(metaObject, "updateUser", currentUser::getId, Long.class);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "gmtModified", LocalDateTime::now, LocalDateTime.class);
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            this.strictInsertFill(metaObject, "updateUser", currentUser::getId, Long.class);
        }
    }

}
