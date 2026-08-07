// =============================================================
// 生活习惯助手 Agent · 知识库初始化脚本
// -------------------------------------------------------------
// 功能：读取 sql/knowledge/ 下的 Markdown 科普文档，按文件名前缀
//       映射 docType（sleep / exercise / diet），对长文档分块后，
//       调用后端 /api/embedding 接口批量写入 MongoDB 向量库
//       （集合 habit_knowledge），供 RAG 语义检索增强。
//
// 前置条件：
//   1. 后端 Spring Boot 已启动（默认 http://localhost:8080）
//   2. application.yml 已配置 MongoDB 向量库连接与 embedding 模型
//   3. mongo-init.js 已执行（含 habit_knowledge 向量索引）
//
// 运行方式：
//   node sql/knowledge/init-knowledge.mjs
//   BASE_URL=http://localhost:8080 node sql/knowledge/init-knowledge.mjs
// =============================================================

import { readdir, readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join, basename } from "node:path";

const __dirname = dirname(fileURLToPath(import.meta.url));
const KNOWLEDGE_DIR = __dirname;
const BASE_URL = process.env.BASE_URL || "http://localhost:8080";
const ENDPOINT = `${BASE_URL}/api/embedding`;
const CHUNK_SIZE = 400; // 每块约 400 字，避免单文档过长影响检索精度

// 文件名前缀 -> docType 映射（与 PRD 4.x / KnowledgeTools 一致）
const PREFIX_TO_DOCTYPE = {
  "sleep": "sleep",
  "exercise": "exercise",
  "diet": "diet",
};

function resolveDocType(filename) {
  const key = filename.split("-")[0].toLowerCase();
  return PREFIX_TO_DOCTYPE[key] || "diet";
}

// 简单按段落 + 字数的分块：先按空行分段，再在超限时硬切
function chunkText(text, size = CHUNK_SIZE) {
  const paragraphs = text.split(/\n\s*\n/).map((p) => p.trim()).filter(Boolean);
  const chunks = [];
  let buffer = "";
  for (const para of paragraphs) {
    if ((buffer + "\n\n" + para).length > size && buffer) {
      chunks.push(buffer);
      buffer = para;
    } else {
      buffer = buffer ? buffer + "\n\n" + para : para;
    }
  }
  if (buffer) chunks.push(buffer);
  return chunks;
}

async function postEmbedding(messages) {
  const url = `${ENDPOINT}?${messages.map((m) => `messages=${encodeURIComponent(m)}`).join("&")}`;
  const res = await fetch(url, { method: "POST" });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status} ${res.statusText}`);
  }
}

async function main() {
  const files = (await readdir(KNOWLEDGE_DIR))
    .filter((f) => f.toLowerCase().endsWith(".md"))
    .filter((f) => f !== "README.md" && f !== "init-knowledge.mjs");

  if (files.length === 0) {
    console.warn("未找到任何知识文档（.md）。");
    return;
  }

  let total = 0;
  for (const file of files.sort()) {
    const abs = join(KNOWLEDGE_DIR, file);
    const raw = await readFile(abs, "utf8");
    const docType = resolveDocType(file);
    const title = basename(file, ".md");
    const chunks = chunkText(raw);

    // 为每块拼接 docType / 标题元信息，提升检索可解释性
    const payloads = chunks.map((c, i) => `[${docType}]《${title}》第${i + 1}段：\n${c}`);

    try {
      await postEmbedding(payloads);
      total += payloads.length;
      console.log(`✓ ${file} -> docType=${docType}, 分块=${chunks.length}`);
    } catch (e) {
      console.error(`✗ ${file} 写入失败：${e.message}`);
    }
  }

  console.log(`\n知识库初始化完成：共写入 ${total} 个文档片段（来源 ${files.length} 篇）。`);
  console.log(`向量库集合：habit_knowledge；验证：GET ${BASE_URL}/api/rag/documents`);
}

main().catch((e) => {
  console.error("初始化脚本异常：", e);
  process.exit(1);
});
