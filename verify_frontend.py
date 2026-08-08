"""前端联调验证：流式对话与会话侧边栏"""
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 900})
    page.goto("http://localhost:5175/ai-advice")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(3000)

    # 1. 检查会话侧边栏是否显示数据
    items = page.locator("aside button:has(div.text-xs)").count()
    print("SIDEBAR_SESSIONS:", items)
    page.screenshot(path="d:/javacode/agent_demo/shot_sidebar.png", full_page=True)

    # 2. 发起对话并等待流式回复
    page.fill("input[placeholder*='和习惯助手']", "今天我做了什么")
    page.click("button.btn-grad")
    print("SENT")

    # 通过 ai 角色消息的最大长度判断流式输出进展
    page.wait_for_function(
        """() => {
            const aiMsgs = document.querySelectorAll('.whitespace-pre-wrap');
            for (const el of aiMsgs) {
                if (el.textContent.length > 50 && el.textContent.includes('你的') || el.textContent.length > 100) {
                    return true;
                }
            }
            return false;
        }""",
        timeout=60000
    )
    print("SSE_OK")

    page.wait_for_timeout(3000)
    page.screenshot(path="d:/javacode/agent_demo/shot_reply_final.png", full_page=True)

    # 3. 再次检查侧边栏（对话后应多一条）
    items_after = page.locator("aside button:has(div.text-xs)").count()
    print("SIDEBAR_AFTER:", items_after)

    browser.close()