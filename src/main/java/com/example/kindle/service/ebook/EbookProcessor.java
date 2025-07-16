package com.example.kindle.service.ebook; // 创建一个新的 ebook 子包

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

public interface EbookProcessor {

    /**
     * 检查此处理器是否支持给定的文件扩展名。
     * @param fileExtension 文件扩展名 (不带点，例如 "epub", "pdf")
     * @return 如果支持则返回 true，否则返回 false
     */
    boolean supports(String fileExtension);

    /**
     * 处理电子书文件，提取元数据并保存文件。
     * @param inputStream 电子书文件的输入流
     * @param originalFilename 原始文件名
     * @param saveDirectory 保存文件的目录路径
     * @return 包含提取的元数据（如标题、作者）和保存的文件路径的 Map。
     * 键可以是 "title", "author", "filePath" 等。
     * @throws IOException 如果文件处理失败
     */
    Map<String, String> process(InputStream inputStream, String originalFilename, Path saveDirectory) throws IOException;
}