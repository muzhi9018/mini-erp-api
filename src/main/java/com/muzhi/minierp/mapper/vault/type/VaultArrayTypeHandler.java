package com.muzhi.minierp.mapper.vault.type;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.*;

/**
 * <p>
 * {@code VaultArrayTypeHandler}：认证器传输提示的 PostgreSQL text 数组映射
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public class VaultArrayTypeHandler extends BaseTypeHandler<String[]> {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, String[] value, JdbcType type) throws SQLException {
        Array array = statement.getConnection().createArrayOf("text", value);
        statement.setArray(index, array);
    }

    private String[] read(Array array) throws SQLException {
        if (array == null) {
            return new String[0];
        }
        try {
            return (String[]) array.getArray();
        } finally {
            array.free();
        }
    }

    @Override
    public String[] getNullableResult(ResultSet result, String column) throws SQLException {
        Array array = result.getArray(column);
        return this.read(array);
    }

    @Override
    public String[] getNullableResult(ResultSet result, int column) throws SQLException {
        Array array = result.getArray(column);
        return this.read(array);
    }

    @Override
    public String[] getNullableResult(CallableStatement statement, int column) throws SQLException {
        Array array = statement.getArray(column);
        return this.read(array);
    }
}
