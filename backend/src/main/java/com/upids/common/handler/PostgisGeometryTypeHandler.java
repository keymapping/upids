package com.upids.common.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostGIS Geometry 类型处理器
 * 处理 WKT 格式与 PostGIS geometry 类型的转换
 */
@MappedTypes(String.class)
public class PostgisGeometryTypeHandler extends BaseTypeHandler<String> {

    private static final int SRID = 4326;

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("geometry");
        pgObject.setValue(wktToEWKB(parameter));
        ps.setObject(i, pgObject);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return extractWkt(rs.getObject(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return extractWkt(rs.getObject(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return extractWkt(cs.getObject(columnIndex));
    }

    private String extractWkt(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof PGobject) {
            String value = ((PGobject) obj).getValue();
            if (value == null) return null;
            // 检测是否为十六进制 EWKB 格式
            if (isHexEWKB(value)) {
                return parseHexEWKB(value);
            }
            return value;
        }
        return obj.toString();
    }

    /**
     * 判断是否为十六进制 EWKB 格式
     * EWKB 格式以十六进制字符串表示，长度至少为 18 个字符（头信息）
     */
    private boolean isHexEWKB(String value) {
        if (value.length() < 18) return false;
        // 检查是否全部为十六进制字符
        for (char c : value.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解析十六进制 EWKB 格式为 WKT
     * EWKB 布局（小端序）：
     * - 字节 0: 字节序 (01 = 小端序)
     * - 字节 1-4: 几何类型 (01000020 = Point with SRID)
     * - 字节 5-8: SRID (E6100000 = 4326)
     * - 字节 9-16: X 坐标 (double)
     * - 字节 17-24: Y 坐标 (double)
     */
    private String parseHexEWKB(String hex) {
        try {
            // 检查字节序
            boolean littleEndian = hex.substring(0, 2).equalsIgnoreCase("01");

            // 解析几何类型（跳过字节序）
            String typeHex = hex.substring(2, 10);
            int geomType = littleEndian ? (int) littleEndianHexToLong(typeHex) : Integer.parseUnsignedInt(typeHex, 16);

            int offset = 10;
            // 检查是否有 SRID 标志 (0x20000000)
            boolean hasSRID = (geomType & 0x20000000) != 0;
            if (hasSRID) {
                offset += 8; // 跳过 SRID (4 bytes = 8 hex chars)
            }

            // 解析 Point 坐标
            if ((geomType & 0xFF) == 1) { // Point
                String xHex = hex.substring(offset, offset + 16);
                String yHex = hex.substring(offset + 16, offset + 32);

                double x, y;
                if (littleEndian) {
                    x = Double.longBitsToDouble(littleEndianHexToLong(xHex));
                    y = Double.longBitsToDouble(littleEndianHexToLong(yHex));
                } else {
                    x = Double.longBitsToDouble(Long.parseUnsignedLong(xHex, 16));
                    y = Double.longBitsToDouble(Long.parseUnsignedLong(yHex, 16));
                }
                return String.format("POINT(%f %f)", x, y);
            }

            // 解析 LineString 坐标
            if ((geomType & 0xFF) == 2) { // LineString
                String numPointsHex = hex.substring(offset, offset + 8);
                int numPoints;
                if (littleEndian) {
                    numPoints = (int) littleEndianHexToLong(numPointsHex);
                } else {
                    numPoints = Integer.parseUnsignedInt(numPointsHex, 16);
                }
                offset += 8;

                StringBuilder wkt = new StringBuilder("LINESTRING(");
                for (int i = 0; i < numPoints; i++) {
                    if (i > 0) wkt.append(", ");
                    String xHex = hex.substring(offset, offset + 16);
                    String yHex = hex.substring(offset + 16, offset + 32);
                    double x, y;
                    if (littleEndian) {
                        x = Double.longBitsToDouble(littleEndianHexToLong(xHex));
                        y = Double.longBitsToDouble(littleEndianHexToLong(yHex));
                    } else {
                        x = Double.longBitsToDouble(Long.parseUnsignedLong(xHex, 16));
                        y = Double.longBitsToDouble(Long.parseUnsignedLong(yHex, 16));
                    }
                    wkt.append(String.format("%f %f", x, y));
                    offset += 32;
                }
                wkt.append(")");
                return wkt.toString();
            }

            return hex; // 未知几何类型，返回原始值
        } catch (Exception e) {
            return hex; // 解析失败，返回原始值
        }
    }

    /**
     * 将小端序十六进制字符串转换为 long
     */
    private static long littleEndianHexToLong(String hex) {
        StringBuilder sb = new StringBuilder();
        for (int i = hex.length() - 2; i >= 0; i -= 2) {
            sb.append(hex, i, i + 2);
        }
        return Long.parseUnsignedLong(sb.toString(), 16);
    }

    /**
     * 将 WKT 格式转换为 EWKB 十六进制字符串
     * PostgreSQL 接受 EWKB hex 格式作为 geometry 类型输入
     */
    private static String wktToEWKB(String wkt) {
        if (wkt == null) return null;
        // 去掉可能的 SRID 前缀
        if (wkt.startsWith("SRID=")) {
            wkt = wkt.substring(wkt.indexOf(';') + 1);
        }
        wkt = wkt.trim().toUpperCase();

        if (wkt.startsWith("POINT(")) {
            return pointToEWKB(wkt);
        } else if (wkt.startsWith("LINESTRING(")) {
            return lineStringToEWKB(wkt);
        }
        return wkt; // 未知格式，原样返回
    }

    private static String pointToEWKB(String wkt) {
        String coords = wkt.replace("POINT(", "").replace(")", "").trim();
        String[] parts = coords.split("\\s+");
        double x = Double.parseDouble(parts[0]); // lng
        double y = Double.parseDouble(parts[1]); // lat

        ByteBuffer buf = ByteBuffer.allocate(25);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 1); // little-endian
        buf.putInt(0x20000001); // Point with SRID flag
        buf.putInt(SRID);
        buf.putDouble(x);
        buf.putDouble(y);

        return bytesToHex(buf.array());
    }

    private static String lineStringToEWKB(String wkt) {
        String coords = wkt.replace("LINESTRING(", "").replace(")", "").trim();
        String[] pointStrs = coords.split(",");
        int numPoints = pointStrs.length;

        ByteBuffer buf = ByteBuffer.allocate(13 + 16 * numPoints);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 1); // little-endian
        buf.putInt(0x20000002); // LineString with SRID flag
        buf.putInt(SRID);
        buf.putInt(numPoints);

        for (String pt : pointStrs) {
            String[] parts = pt.trim().split("\\s+");
            buf.putDouble(Double.parseDouble(parts[0]));
            buf.putDouble(Double.parseDouble(parts[1]));
        }

        return bytesToHex(buf.array());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
