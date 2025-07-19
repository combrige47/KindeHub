package com.example.kindle.service.book;

import com.example.kindle.service.book.EbookProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component // 让 Spring 能够扫描并管理这个 bean
public class EbookProcessorFactory {

    private final Map<String, EbookProcessor> processorsMap;

    // Spring 会自动注入所有 EbookProcessor 的实现类
    public EbookProcessorFactory(List<EbookProcessor> ebookProcessors) {
        // 将注入的处理器列表转换为一个 Map，键是它支持的文件扩展名，值是处理器实例
        processorsMap = ebookProcessors.stream()
                .collect(Collectors.toMap(
                        processor -> processor.getClass().getSimpleName().replace("Processor", "").toLowerCase(), // 从类名推断扩展名，例如 EpubProcessor -> epub
                        Function.identity()
                ));
        // 更好的方式是让 EbookProcessor 接口提供一个 getSupportedExtensions() 方法来返回列表
        // 但为了简化当前步骤，我们先用这种方式
    }

    /**
     * 根据文件扩展名获取对应的电子书处理器。
     * @param fileExtension 文件扩展名 (不带点，例如 "epub", "pdf")
     * @return 对应的 EbookProcessor 实例，如果找不到则返回 Optional.empty()
     */
    public Optional<EbookProcessor> getProcessor(String fileExtension) {
        // 这里需要改进，直接通过类名推断扩展名不够健壮。
        // 实际应该遍历processorsMap的值，调用每个processor.supports(fileExtension)
        // 或者在processorsMap中直接存储支持的扩展名作为key。
        // 为了下一步的快速迭代，我们先简单处理。
        for (EbookProcessor processor : processorsMap.values()) {
            if (processor.supports(fileExtension)) {
                return Optional.of(processor);
            }
        }
        return Optional.empty();
    }
}