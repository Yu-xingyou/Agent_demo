package com.habit.agent.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.habit.agent.common.vo.ImportResultVO;
import com.habit.agent.common.vo.RagDocumentVO;
import com.habit.agent.common.vo.RagSearchResultVO;

/**
 * RAG 知识库业务逻辑接口（阶段八）。
 *
 * <p>底层依托 MongoDB Atlas Vector Search，文档经 TokenTextSplitter 分块后
 * 由 DashScope text-embedding-v3 向量化存入 habit_knowledge 集合。
 */
public interface RagService {

    /**
     * 导入 classpath:rag-docs/ 下的预设知识文档。
     *
     * <p>幂等：按 metadata.source 先删除同名文件的旧片段再写入，重复调用不会造成知识库膨胀。
     * 单篇文档失败不中断整体流程，失败原因收集在返回结果的 errors 中。
     */
    ImportResultVO importPresetDocs();

    /**
     * 上传自定义知识文档入库（支持 .md / .txt，UTF-8 编码，大小不超过 2MB）。
     */
    ImportResultVO uploadDocument(MultipartFile file);

    /**
     * 语义检索知识库。
     *
     * @param query 检索问题
     * @param topK  返回条数，取值范围 [1, 10]
     */
    List<RagSearchResultVO> search(String query, int topK);

    /**
     * 查询已入库的文档片段列表。
     *
     * @param docType 知识类型过滤，传 {@code null} 或空串表示不过滤
     */
    List<RagDocumentVO> listDocuments(String docType);

    /**
     * 按片段 ID 删除知识库文档。
     */
    void deleteDocument(String id);
}
