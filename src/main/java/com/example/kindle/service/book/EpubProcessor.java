package com.example.kindle.service.book;

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

        Path ebookDir = saveDirectory.resolve("ebook");
        if (!Files.exists(ebookDir)) {
            Files.createDirectories(ebookDir);
        }

        String uniqueFilename = String.valueOf(System.currentTimeMillis());
        String uniqueEbookFilename = uniqueFilename + "_" + originalFilename;
        Path ebookPath = ebookDir.resolve(uniqueEbookFilename);

        // 3. 保存电子书文件
        Files.copy(inputStream, ebookPath, StandardCopyOption.REPLACE_EXISTING);

        // 4. 使用 epublib 读取电子书元数据
        try (InputStream bookInputStream = Files.newInputStream(ebookPath)) {
            EpubReader epubReader = new EpubReader();
            Book epubBook = epubReader.readEpub(bookInputStream);

            // 4.1 提取标题
            String title = epubBook.getTitle(); // 获取 EPUB 文件的标题
            if (title == null || title.isEmpty()) {

                title = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
            }
            metadata.put("title", title);

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
            metadata.put("filePath", ebookPath.toString()); // 将电子书在服务器上的实际存储路径存入元数据 Map

            //4.4 提取封面并保存
            Path coverDir = saveDirectory.resolve("cover");
            if (!Files.exists(coverDir)) {
                Files.createDirectories(coverDir);
            }
            if(epubBook.getCoverImage() != null) {
                try{
                byte[] coverImageData = epubBook.getCoverImage().getData();
//                String type = String.valueOf(epubBook.getCoverImage().getMediaType());
                String coverFileName = "cover_" + uniqueFilename + ".jpg" ;
                Path coverPath = coverDir.resolve(coverFileName);

                Files.write(coverPath, coverImageData);
                metadata.put("coverPath", coverPath.toString());
            } catch(IOException e){
                    System.out.println("处理封面图片失败"+e.getMessage());
                    metadata.put("coverPath", "default_cover.jpg");
                }

            }

        } catch (Exception e) {
            // 5. 错误处理：如果解析 EPUB 失败 (例如文件损坏或不是有效的 EPUB)，
            // 删除已经保存到服务器的文件，避免留下无效文件
            Files.deleteIfExists(ebookPath);
            // 抛出 IOException，告知调用者处理失败，并包含原始异常信息
            throw new IOException("解析EPUB文件失败: " + e.getMessage(), e);
        }

        return metadata; // 返回包含提取元数据和文件路径的 Map
    }
}