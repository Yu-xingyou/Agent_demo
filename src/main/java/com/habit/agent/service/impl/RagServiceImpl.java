package com.habit.agent.service.impl;

import com.habit.agent.common.exception.BusinessException;
import com.habit.agent.service.RagService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 知识库服务实现
 * <p>
 * 文档分块 → MongoDBAtlasVectorStore.add() 自动向量化（DashScope text-embedding-v3 1024 维）→ 存入远程 Atlas habit_knowledge。
 * 向量库不可用（远程连接失败）时抛出业务异常提示。
 */
@Slf4j
@Service
public class RagServiceImpl implements RagService {

    private static final String COLLECTION = "habit_knowledge";

    /** 预设文档清单（classpath:rag-docs/） */
    private static final Map<String, String> PRESET_DOCS = Map.of(
            "sleep-health.md", "sleep",
            "exercise-health.md", "exercise",
            "diet-health.md", "diet"
    );

    private static final String ATLAS_DATABASE = "habit_agent";

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final String remoteMongoUri;
    /** 懒初始化的远程 Atlas MongoTemplate（不注册为 Spring Bean，避免抑制本地自动配置） */
    private volatile MongoTemplate atlasMongoTemplate;

    public RagServiceImpl(ObjectProvider<VectorStore> vectorStoreProvider,
                          @Value("${REMOTE_MONGO_URI:}") String remoteMongoUri) {
        this.vectorStoreProvider = vectorStoreProvider;
        this.remoteMongoUri = remoteMongoUri;
    }

    private MongoTemplate atlasTemplate() {
        if (atlasMongoTemplate == null) {
            synchronized (this) {
                if (atlasMongoTemplate == null) {
                    if (!StringUtils.hasText(remoteMongoUri)) {
                        return null;
                    }
                    try {
                        MongoClient client = MongoClients.create(remoteMongoUri);
                        atlasMongoTemplate = new MongoTemplate(client, ATLAS_DATABASE);
                    } catch (Exception e) {
                        log.error("创建远程 Atlas MongoTemplate 失败", e);
                        return null;
                    }
                }
            }
        }
        return atlasMongoTemplate;
    }

    @Override
    public Map<String, Object> importPresetDocs() {
        VectorStore vectorStore = requireVectorStore();
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        int totalDocs = 0;
        int totalChunks = 0;

        // 已入库的 source 集合（幂等）
        Set<String> existingSources = new HashSet<>(listDocuments(null).stream()
                .map(doc -> String.valueOf(doc.getOrDefault("source", "")))
                .toList());

        for (Map.Entry<String, String> entry : PRESET_DOCS.entrySet()) {
            String fileName = entry.getKey();
            String docType = entry.getValue();
            if (existingSources.contains(fileName)) {
                log.info("预设文档已存在，跳过：{}", fileName);
                continue;
            }
            try {
                ClassPathResource resource = new ClassPathResource("rag-docs/" + fileName);
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                int chunkCount = addDocument(fileName, docType, content, vectorStore);
                totalDocs++;
                totalChunks += chunkCount;
            } catch (IOException e) {
                log.error("读取预设文档失败：{}", fileName, e);
                errors.add(fileName);
            }
        }

        result.put("totalDocs", totalDocs);
        result.put("totalChunks", totalChunks);
        result.put("errors", errors);
        return result;
    }

    @Override
    public Map<String, Object> uploadDocument(String fileName, byte[] content) {
        VectorStore vectorStore = requireVectorStore();
        String docType = inferDocType(fileName);
        String text = new String(content, StandardCharsets.UTF_8);
        int chunks = addDocument(fileName, docType, text, vectorStore);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalChunks", chunks);
        result.put("source", fileName);
        result.put("docType", docType);
        return result;
    }

    @Override
    public List<Map<String, Object>> search(String query, int topK) {
        VectorStore vectorStore = requireVectorStore();
        int k = (topK <= 0) ? 3 : topK;
        return vectorStore.similaritySearch(SearchRequest.builder()
                        .query(query)
                        .topK(k)
                        .build())
                .stream()
                .map(RagServiceImpl::toDocMap)
                .toList();
    }

    @Override
    public List<Map<String, Object>> listDocuments(String docType) {
        MongoTemplate template = atlasTemplate();
        if (template == null) {
            return List.of();
        }
        Query query = new Query();
        if (StringUtils.hasText(docType)) {
            query.addCriteria(Criteria.where("metadata.docType").is(docType));
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Document doc : template.findAll(Document.class, COLLECTION)) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", doc.getString("id"));
            map.put("content", doc.getString("content"));
            Document metadata = doc.get("metadata", Document.class);
            if (metadata != null) {
                map.put("source", metadata.getString("source"));
                map.put("docType", metadata.getString("docType"));
                map.put("chunkIndex", metadata.getInteger("chunkIndex"));
            }
            list.add(map);
        }
        // 按 source + chunkIndex 排序，保证列表稳定
        list.sort((a, b) -> {
            int c = String.valueOf(a.get("source")).compareTo(String.valueOf(b.get("source")));
            if (c != 0) return c;
            return Integer.compare(toInt(a.get("chunkIndex")), toInt(b.get("chunkIndex")));
        });
        return list;
    }

    @Override
    public void deleteDocument(String id) {
        VectorStore vectorStore = requireVectorStore();
        vectorStore.delete(List.of(id));
        log.info("删除知识片段：id={}", id);
    }

    /**
     * 分块并向量化入库，返回分块数
     */
    private int addDocument(String source, String docType, String text, VectorStore vectorStore) {
        List<String> chunks = chunk(text);
        List<org.springframework.ai.document.Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", source);
            metadata.put("docType", docType);
            metadata.put("chunkIndex", i);
            documents.add(new org.springframework.ai.document.Document(UUID.randomUUID().toString(), chunks.get(i), metadata));
        }
        vectorStore.add(documents);
        return documents.size();
    }

    /**
     * 简单分块：按空行分段，过滤过短段落
     */
    private List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        for (String para : text.split("\r?\n\\s*\r?\n")) {
            String trimmed = para.replaceAll("^#+\\s*", "").trim();
            if (trimmed.length() >= 15) {
                chunks.add(trimmed);
            }
        }
        return chunks;
    }

    private String inferDocType(String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase();
        if (name.contains("sleep")) return "sleep";
        if (name.contains("exercise") || name.contains("sport") || name.contains("run")) return "exercise";
        if (name.contains("diet") || name.contains("food") || name.contains("water")) return "diet";
        return "custom";
    }

    private VectorStore requireVectorStore() {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new BusinessException(40001, "向量库不可用，请检查远程 Atlas MongoDB 连接配置（REMOTE_MONGO_URI）");
        }
        return vectorStore;
    }

    private static Map<String, Object> toDocMap(org.springframework.ai.document.Document doc) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", doc.getId());
        map.put("content", doc.getText());
        map.put("score", doc.getScore());
        Map<String, Object> metadata = doc.getMetadata();
        if (metadata != null) {
            map.put("source", metadata.getOrDefault("source", ""));
            map.put("docType", metadata.getOrDefault("docType", "custom"));
            map.put("chunkIndex", metadata.getOrDefault("chunkIndex", 0));
        }
        return map;
    }

    private static int toInt(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }
}
