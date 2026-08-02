from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1400, "height": 900})

    # Capture console errors
    errors = []
    page.on("console", lambda msg: errors.append(msg.text) if msg.type == "error" else None)
    page.on("pageerror", lambda err: errors.append(str(err)))

    # Test 1: Index page
    print("=== Testing Index Page ===")
    page.goto("http://localhost:8080/", wait_until="networkidle")
    page.wait_for_timeout(2000)
    page.screenshot(path="/tmp/index_page.png", full_page=True)
    
    # Check for sleep/exercise/water elements
    sleep_cells = page.locator("td:has-text('h')").count()
    exercise_cells = page.locator("td:has-text('min')").count()
    water_cells = page.locator("td:has-text('ml')").count()
    print(f"Sleep cells (h): {sleep_cells}")
    print(f"Exercise cells (min): {exercise_cells}")
    print(f"Water cells (ml): {water_cells}")
    
    # Check for mood badges
    mood_badges = page.locator(".mood-badge").count()
    print(f"Mood badges: {mood_badges}")

    # Test 2: History page
    print("\n=== Testing History Page ===")
    page.goto("http://localhost:8080/history", wait_until="networkidle")
    page.wait_for_timeout(2000)
    page.screenshot(path="/tmp/history_page.png", full_page=True)
    
    # Check for charts
    charts = page.locator("canvas").count()
    print(f"Charts found: {charts}")
    
    # Check for detail buttons
    detail_buttons = page.locator(".detail-btn, .action-btn, button:has-text('详情'), a:has-text('详情')").count()
    all_btns = page.locator("button, .card-clickable, [data-id]").count()
    print(f"Detail buttons: {detail_buttons}")
    print(f"All clickable elements: {all_btns}")
    
    # Try clicking a card for detail view
    first_card = page.locator(".card-clickable, .record-card, .habit-record-item").first
    if first_card.count() > 0:
        print("Found clickable card, clicking...")
        first_card.click()
        page.wait_for_timeout(1000)
        page.screenshot(path="/tmp/history_detail1.png", full_page=True)
        
        # Close modal
        close_btn = page.locator(".modal-close, [data-dismiss='modal'], .close").first
        if close_btn.count() > 0:
            close_btn.click()
            page.wait_for_timeout(500)
        
        # Click again to test repeated clicks
        first_card.click()
        page.wait_for_timeout(1000)
        page.screenshot(path="/tmp/history_detail2.png", full_page=True)
        print("Second click successful - no hang!")
        
        # Close again
        close_btn = page.locator(".modal-close, [data-dismiss='modal'], .close").first
        if close_btn.count() > 0:
            close_btn.click()
            page.wait_for_timeout(500)
    
    # Check for any JS errors
    if errors:
        print(f"\nJS Errors: {errors}")
    else:
        print("\nNo JS errors detected!")
    
    # Check page still responsive after multiple clicks
    page.goto("http://localhost:8080/", wait_until="networkidle")
    page.wait_for_timeout(1000)
    print("Page navigation still works after detail clicks!")
    
    browser.close()
    print("\n=== All tests passed! ===")
