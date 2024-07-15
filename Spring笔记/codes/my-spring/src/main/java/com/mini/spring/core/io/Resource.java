package com.mini.spring.core.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Author: wujie
 * @Date: 2024/7/13 14:49
 */
public interface Resource {

    InputStream getInputStream() throws IOException;

}
