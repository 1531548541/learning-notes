package com.mini.spring.core.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Author: wujie
 * @Date: 2024/7/13 14:58
 */
public class FileSystemResource implements Resource {

    private File file;

    public FileSystemResource(String path) {
        this(new File(path));
    }

    public FileSystemResource(File file) {
        this.file = file;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }
}
