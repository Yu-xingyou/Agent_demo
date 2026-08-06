package com.habit.agent.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.exception.AiCallException;
import com.habit.agent.common.exception.BusinessException;
import com.habit.agent.common.vo.ImportResultVO;
import com.habit.agent.common.vo.RagDocumentVO;
import com.habit.agent.common.vo.RagSearchResultVO;
import com.habit.agent.service.RagService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RAG 知识库服务实现（阶段八）。
 *
 * <p>写入链路：文档 → TokenTextSplitter 分块 → 注入元数据 → VectorStore.add()
 * （内部批量调用 text-embedding-v3 向量化）→ MongoDB Atlas habit_knowledge 集合。
 *
 * <p>读取链路分两条：
 * <ul>
 *   <li><b>语义检索</b>走 {@link VectorStore#similaritySearch}，依赖 Atlas $vectorSearch 索引。</li>
 *   <li><b>文档列表</b>走 {@link MongoTemplate} 原生查询，因为 VectorStore 接口不具备"列出全部"能力；
 *       查询时通过 {@code exclude("embedding")} 排除 1024 维向量字段，单条响应体积从约 4KB 降至 500B。</li>
 * </ul>
 *
 * <p>所有向量库调用统一包装为 {@link AiCallException}，由 GlobalExceptionHandler 映射为 503。
 */
@Slf4j
@Service
public class RagServiceImpl implements RagService {

    /** 向量库中存放正文的字段名（Spring AI MongoDB Atlas Store 约定）。 */
    private static final String FIELD_CONTENT = "content";
    /** 向量库中存放元数据的字段名。 */
    private static final String FIELD_METADATA = "metadata";
    /** 向量字段名，列表查询时需排除。 */
    private static final String FIELD_EMBEDDING = "embedding";

    private static final String META_DOC_TYPE = "doc_type";
    private static final String META_SOURCE = "source";
    private static final String META_CHUNK_INDEX = "chunk_index";
    private static final String META_IMPORT_TIME = "import_time";

    /** 列表接口的内容预览长度。 */
    private static final int CONTENT_PREVIEW_LIMIT = 300;

    /** 允许上传的文件扩展名白名单。 */
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".md", ".txt");

    private final VectorStore vectorStore;
    @Qualifier("atlasMongoTemplate")
    private final MongoTemplate mongoTemplate;

    public RagServiceImpl(VectorStore vectorStore,
            @Qualifier("atlasMongoTemplate") MongoTemplate mongoTemplate) {
        this.vectorStore = vectorStore;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public ImportResultVO importPresetDocs() {
        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver()
                    .getResources(AgentConstants.RAG_PRESET_DOCS_PATTERN);
        } catch (IOException e) {
            throw new AiCallException(AgentConstants.CODE_RAG_SEARCH_ERROR,
                    "读取预设知识库文档失败: " + e.getMessage(), e);
        }
        if (resources.length == 0) {
            log.warn("[阶段八] 未找到任何预设知识库文档，请检查 classpath:rag-docs/ 目录");
            return ImportResultVO.builder()
                    .totalDocs(0).totalChunks(0).success(true).errors(List.of())
                    .build();
        }

        int docCount = 0;
        int chunkCount = 0;
        List<String> errors = new ArrayList<>();

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            try {
                String text = resource.getContentAsString(StandardCharsets.UTF_8);
                chunkCount += ingest(text, filename, resolveDocType(filename));
                docCount++;
            } catch (Exception e) {
                // 单篇失败不中断整体导入
                log.warn("[阶段八] 预设文档导入失败: {} - {}", filename, e.getMessage());
                errors.add(filename + ": " + e.getMessage());
            }
        }

        log.info("[阶段八] 预设知识库导入完成，成功 {} 篇 / {} 个片段，失败 {} 篇",
                docCount, chunkCount, errors.size());
        return ImportResultVO.builder()
                .totalDocs(docCount)
                .totalChunks(chunkCount)
                .success(errors.isEmpty())
                .errors(errors)
                .build();
    }

    @Override
    public ImportResultVO uploadDocument(MultipartFile file) {
        String filename = validateUpload(file);

        String text;
        try {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(AgentConstants.CODE_RAG_UPLOAD_ERROR,
                    "文件读取失败，请确认为 UTF-8 编码的文本文件");
        }
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(AgentConstants.CODE_RAG_UPLOAD_ERROR, "文件内容为空");
        }

        int chunks = ingest(text, filename, resolveDocType(filename));
        log.info("[阶段八] 用户文档上传入库成功: {} / {} 个片段", filename, chunks);
        return ImportResultVO.builder()
                .totalDocs(1)
                .totalChunks(chunks)
                .success(true)
                .errors(List.of())
                .build();
    }

    @Override
    public List<RagSearchResultVO> search(String query, int topK) {
        int limit = Math.clamp(topK, 1, AgentConstants.RAG_MAX_TOP_K);
        List<Document> documents;
        try {
            documents = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query)
                    .topK(limit)
                    .similarityThreshold(AgentConstants.RAG_SIMILARITY_THRESHOLD)
                    .build());
        } catch (Exception e) {
            log.error("[阶段八] 知识库检索失败: {}", e.getMessage(), e);
            throw new AiCallException(AgentConstants.CODE_RAG_SEARCH_ERROR,
                    "知识库检索失败，请检查 MongoDB Atlas 连接与向量索引配置: " + e.getMessage(), e);
        }
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream().map(this::toSearchResult).toList();
    }

    @Override
    public List<RagDocumentVO> listDocuments(String docType) {
        Query query = new Query();
        if (StringUtils.hasText(docType)) {
            query.addCriteria(Criteria.where(FIELD_METADATA + "." + META_DOC_TYPE).is(docType));
        }
        // 关键性能优化：排除 1024 维向量字段，避免每条多传约 4KB 数据
        query.fields().exclude(FIELD_EMBEDDING);

        List<org.bson.Document> rows;
        try {
            rows = mongoTemplate.find(query, org.bson.Document.class, AgentConstants.RAG_COLLECTION_NAME);
        } catch (Exception e) {
            log.error("[阶段八] 知识库文档列表查询失败: {}", e.getMessage(), e);
            throw new AiCallException(AgentConstants.CODE_RAG_SEARCH_ERROR,
                    "知识库文档查询失败: " + e.getMessage(), e);
        }
        return rows.stream().map(this::toDocumentVO).toList();
    }

    @Override
    public void deleteDocument(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(AgentConstants.CODE_PARAM_ERROR, "文档ID不能为空");
        }
        try {
            vectorStore.delete(List.of(id));
            log.info("[阶段八] 知识库片段已删除: {}", id);
        } catch (Exception e) {
            log.error("[阶段八] 知识库片段删除失败: {}", e.getMessage(), e);
            throw new AiCallException(AgentConstants.CODE_RAG_SEARCH_ERROR,
                    "知识库片段删除失败: " + e.getMessage(), e);
        }
    }

    // ===== 内部辅助 =====

    /**
     * 将文本分块、注入元数据并写入向量库。
     *
     * <p>幂等保障：写入前先按 {@code metadata.source} 删除同名文件的历史片段，
     * 避免重复导入导致知识库膨胀与检索结果重复。
     *
     * @return 实际入库的分块数
     */
    private int ingest(String text, String source, String docType) {
        deleteBySource(source);

        // 使用 Builder 而非 6 参构造：后者要求显式传入 punctuationMarks，传空列表会触发校验失败。
        // withMinChunkSizeChars 控制分块边界回溯范围，起到与 overlap 类似的语义连续性保障。
        List<Document> chunks = TokenTextSplitter.builder()
                .withChunkSize(AgentConstants.RAG_CHUNK_SIZE)
                .withMinChunkSizeChars(AgentConstants.RAG_CHUNK_OVERLAP)
                .withMinChunkLengthToEmbed(10)
                .withKeepSeparator(true)
                .build()
                .apply(List.of(new Document(text)));

        String importTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        List<Document> enriched = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put(META_DOC_TYPE, docType);
            metadata.put(META_SOURCE, source);
            metadata.put(META_CHUNK_INDEX, i);
            metadata.put(META_IMPORT_TIME, importTime);
            enriched.add(new Document(chunks.get(i).getText(), metadata));
        }

        try {
            // 批量提交，由 Spring AI 内部批处理 Embedding 调用，避免逐条 HTTP 往返
            vectorStore.add(enriched);
        } catch (Exception e) {
            throw new AiCallException(AgentConstants.CODE_RAG_SEARCH_ERROR,
                    "向量化入库失败: " + e.getMessage(), e);
        }
        return enriched.size();
    }

    /** 按来源文件名清理历史片段，保证导入幂等。 */
    private void deleteBySource(String source) {
        try {
            mongoTemplate.remove(
                    new Query(Criteria.where(FIELD_METADATA + "." + META_SOURCE).is(source)),
                    AgentConstants.RAG_COLLECTION_NAME);
        } catch (Exception e) {
            // 清理失败不阻断导入，仅可能造成少量重复片段
            log.warn("[阶段八] 清理旧片段失败（继续导入）: {} - {}", source, e.getMessage());
        }
    }

    /** 校验上传文件并返回文件名。 */
    private String validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(AgentConstants.CODE_RAG_UPLOAD_ERROR, "上传文件不能为空");
        }
        if (file.getSize() > AgentConstants.RAG_UPLOAD_MAX_SIZE) {
            throw new BusinessException(AgentConstants.CODE_RAG_UPLOAD_ERROR, "文件大小不能超过 2MB");
        }
        String filename = StringUtils.getFilename(file.getOriginalFilename());
        if (!StringUtils.hasText(filename)) {
            throw new BusinessException(AgentConstants.CODE_RAG_UPLOAD_ERROR, "文件名不合法");
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (ALLOWED_EXTENSIONS.stream().noneMatch(lower::endsWith)) {
            throw new BusinessException(AgentConstants.CODE_RAG_UPLOAD_ERROR,
                    "仅支持 .md / .txt 格式文件");
        }
        return filename;
    }

    /** 根据预设文件名推断知识类型，未匹配时归为 custom。 */
    private String resolveDocType(String filename) {
        if (filename == null) {
            return "custom";
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.contains("sleep")) {
            return "sleep";
        }
        if (lower.contains("exercise")) {
            return "exercise";
        }
        if (lower.contains("diet")) {
            return "diet";
        }
        return "custom";
    }

    private RagSearchResultVO toSearchResult(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();
        return RagSearchResultVO.builder()
                .id(doc.getId())
                .content(doc.getText())
                .score(doc.getScore())
                .docType(asString(metadata.get(META_DOC_TYPE)))
                .source(asString(metadata.get(META_SOURCE)))
                .build();
    }

    @SuppressWarnings("unchecked")
    private RagDocumentVO toDocumentVO(org.bson.Document row) {
        Object rawMetadata = row.get(FIELD_METADATA);
        Map<String, Object> metadata = rawMetadata instanceof Map
                ? (Map<String, Object>) rawMetadata
                : Map.of();
        Object chunkIndex = metadata.get(META_CHUNK_INDEX);
        return RagDocumentVO.builder()
                .id(asString(row.get("_id")))
                .content(truncate(asString(row.get(FIELD_CONTENT))))
                .docType(asString(metadata.get(META_DOC_TYPE)))
                .source(asString(metadata.get(META_SOURCE)))
                .chunkIndex(chunkIndex instanceof Number n ? n.intValue() : null)
                .importTime(asString(metadata.get(META_IMPORT_TIME)))
                .build();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= CONTENT_PREVIEW_LIMIT
                ? text
                : text.substring(0, CONTENT_PREVIEW_LIMIT) + "...";
    }
}
