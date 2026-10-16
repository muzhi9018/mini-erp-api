package com.muzhi.minierp.mapper.vault.type;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.*;

/**
 * <p>
 * {@code VaultJsonTypeHandler}：公开 JSON 回执的 PostgreSQL jsonb 映射
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@org.apache.ibatis.type.MappedJdbcTypes(JdbcType.OTHER)
public class VaultJsonTypeHandler extends BaseTypeHandler<String> {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, String value, JdbcType type) throws SQLException {
        statement.setObject(index, value, Types.OTHER);
    }

    @Override
    public String getNullableResult(ResultSet result, String column) throws SQLException {
        return result.getString(column);
    }

    @Override
    public String getNullableResult(ResultSet result, int column) throws SQLException {
        return result.getString(column);
    }

    @Override
    public String getNullableResult(CallableStatement statement, int column) throws SQLException {
        return statement.getString(column);
    }
}
