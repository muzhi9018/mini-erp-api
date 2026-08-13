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

    /**
     * 填充字段创建时间
     */
    private static final String FILL_FIELD_GMT_CREATE = "gmtCreate";

    /**
     * 填充字段更新时间
     */
    private static final String FILL_FIELD_GMT_MODIFIED = "gmtModified";

    /**
     * 填充字段创建人
     */
    private static final String FILL_FIELD_CREATE_USER = "createUser";

    /**
     * 填充字段更新人
     */
    private static final String FILL_FIELD_UPDATE_USER = "updateUser";



    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, FILL_FIELD_GMT_CREATE, LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, FILL_FIELD_GMT_MODIFIED, LocalDateTime::now, LocalDateTime.class);
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            this.strictInsertFill(metaObject, FILL_FIELD_CREATE_USER, currentUser::getId, Long.class);
            this.strictInsertFill(metaObject, FILL_FIELD_UPDATE_USER, currentUser::getId, Long.class);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, FILL_FIELD_GMT_MODIFIED, LocalDateTime::now, LocalDateTime.class);
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            this.strictInsertFill(metaObject, FILL_FIELD_UPDATE_USER, currentUser::getId, Long.class);
        }
    }

}
