package com.muzhi.minierp.model.vault;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * <p>
 * {@code VaultSnapshot}：只包含密文的临时快照，读取结束或失败后删除
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-16
 */
public final class VaultSnapshot implements AutoCloseable {

    /**
     * 服务器生成的临时路径，不从请求接收，不写入响应
     */
    private final Path path;

    private VaultSnapshot(Path path) {
        this.path = path;
    }

    public static VaultSnapshot create() throws IOException {
        Path directory = Path.of(System.getProperty("java.io.tmpdir"));
        Path file;
        if (Files.getFileStore(directory).supportsFileAttributeView("posix")) {
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
            FileAttribute<Set<PosixFilePermission>> attributes = PosixFilePermissions.asFileAttribute(permissions);
            file = Files.createTempFile(directory, "mini-erp-vault-snapshot-", ".json", attributes);
        } else {
            file = Files.createTempFile(directory, "mini-erp-vault-snapshot-", ".json");
        }
        return new VaultSnapshot(file);
    }

    public OutputStream output() throws IOException {
        return Files.newOutputStream(this.path);
    }

    public long contentLength() throws IOException {
        return Files.size(this.path);
    }

    public void transferTo(OutputStream output) throws IOException {
        Files.copy(this.path, output);
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(this.path);
    }
}
