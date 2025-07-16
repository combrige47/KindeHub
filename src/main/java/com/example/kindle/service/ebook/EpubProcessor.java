package com.example.kindle.service.ebook;

import nl.siegmann.epublib.domain.Book; // 导入 epublib 库的 Book 类，代表一个电子书对象
import nl.siegmann.epublib.epub.EpubReader; // 导入 epublib 库的 EpubReader，用于读取 EPUB 文件
import org.springframework.stereotype.Component; // Spring 注解，将此类标记为 Spring 组件

import java.io.IOException; // IO 异常处理
import java.io.InputStream; // 输入流
import java.nio.file.Files; // 文件操作工具类
import java.nio.file.Path; // 路径对象
import java.nio.file.StandardCopyOption; // 文件复制选项
import java.util.HashMap; // Map 实现
import java.util.List; // 列表
import java.util.Map; // 映射
import java.util.stream.Collectors; // 流操作工具

@Component // 核心注解：告诉 Spring 框架，这是一个组件（Bean），Spring 会自动管理它的生命周期
public class EpubProcessor implements EbookProcessor { // 实现 EbookProcessor 接口

    /**
     * 检查此处理器是否支持给定的文件扩展名。
     *
     * @param fileExtension 文件扩展名 (不带点，例如 "epub", "pdf")
     * @return 如果支持则返回 true，否则返回 false
     */
    @Override
    public boolean supports(String fileExtension) {
        // 判断传入的文件扩展名是否是 "epub"（不区分大小写），如果是则表示此处理器支持该格式
        return "epub".equalsIgnoreCase(fileExtension);
    }

    /**
     * 处理电子书文件，提取元数据并保存文件。
     *
     * @param inputStream      电子书文件的输入流
     * @param originalFilename 原始文件名 (例如 "mybook.epub")
     * @param saveDirectory    保存文件的目录路径
     * @return 包含提取的元数据（如标题、作者）和保存的文件路径的 Map。
     * 键可以是 "title", "author", "filePath" 等。
     * @throws IOException 如果文件处理失败
     */
    @Override
    public Map<String, String> process(InputStream inputStream, String originalFilename, Path saveDirectory) throws IOException {
        Map<String, String> metadata = new HashMap<>(); // 用于存储提取到的元数据

        // 1. 确保保存目录存在
        if (!Files.exists(saveDirectory)) {
            // 如果目录不存在，则创建所有必需的父目录
            Files.createDirectories(saveDirectory);
        }

        // 2. 生成唯一的文件名，保留原始扩展名
        // 使用当前时间戳作为前缀，结合原始文件名，确保文件名的唯一性，避免覆盖现有文件
        String uniqueFilename = System.currentTimeMillis() + "_" + originalFilename;
        Path filePath = saveDirectory.resolve(uniqueFilename); // 构建完整的保存路径

        // 3. 保存电子书文件
        // 将输入流中的电子书内容复制到目标文件路径。
        // StandardCopyOption.REPLACE_EXISTING 表示如果文件已存在则替换
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

        // 4. 使用 epublib 读取电子书元数据
        // 使用 try-with-resources 确保流在操作完成后自动关闭
        try (InputStream bookInputStream = Files.newInputStream(filePath)) { // 重新打开已保存的文件流来读取EPUB内容
            EpubReader epubReader = new EpubReader(); // 创建 EpubReader 实例
            Book epubBook = epubReader.readEpub(bookInputStream); // 读取 EPUB 文件并解析成 epublib 的 Book 对象

            // 4.1 提取标题
            String title = epubBook.getTitle(); // 获取 EPUB 文件的标题
            if (title == null || title.isEmpty()) {
                // 如果 EPUB 文件内部没有明确的标题，则尝试从原始文件名中截取作为标题
                title = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
            }
            metadata.put("title", title); // 将标题存入元数据 Map

            // 4.2 提取作者
            List<String> authors = epubBook.getMetadata().getAuthors().stream()
                    // 遍历所有作者信息，拼接成 "FirstName LastName" 的格式
                    .map(author -> author.getFirstname() + " " + author.getLastname().trim())
                    .collect(Collectors.toList());
            String author = String.join(", ", authors); // 将多个作者用逗号连接成一个字符串
            if (author.trim().isEmpty()) {
                author = "未知作者"; // 如果没有作者信息，则设为 "未知作者"
            }
            metadata.put("author", author); // 将作者存入元数据 Map

            // 4.3 保存文件路径
            metadata.put("filePath", filePath.toString()); // 将电子书在服务器上的实际存储路径存入元数据 Map

            // TODO: 考虑提取封面图片并保存
            // 这一部分是未来可以扩展的功能。EPUB 文件中通常包含封面图片，
            // 我们可以提取它并保存为独立的图片文件，然后将图片路径存入 Book 实体。
            // if (epubBook.getCoverImage() != null) {
            //     // 处理封面图片逻辑，保存到封面目录，并更新 Book 实体中的 coverPath
            // }

        } catch (Exception e) {
            // 5. 错误处理：如果解析 EPUB 失败 (例如文件损坏或不是有效的 EPUB)，
            // 删除已经保存到服务器的文件，避免留下无效文件
            Files.deleteIfExists(filePath);
            // 抛出 IOException，告知调用者处理失败，并包含原始异常信息
            throw new IOException("解析EPUB文件失败: " + e.getMessage(), e);
        }

        return metadata; // 返回包含提取元数据和文件路径的 Map
    }
}