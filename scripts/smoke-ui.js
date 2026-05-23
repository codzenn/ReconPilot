const { chromium } = require("playwright");

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1366, height: 820 } });
  await page.goto("http://localhost:8080", { waitUntil: "networkidle" });
  await page.waitForSelector(".metric strong");

  const title = await page.locator("h1").innerText();
  const metrics = await page.locator(".metric strong").allInnerTexts();

  await page.click("[data-view='cases']");
  await page.waitForSelector("#casesView.active-view .case-item");
  const caseCount = await page.locator("#casesView .case-item").count();

  await page.click("[data-view='transactions']");
  await page.waitForSelector("#transactionsView.active-view #txnRows tr");
  const txnCount = await page.locator("#txnRows tr").count();

  await page.click("[data-view='dashboard']");
  await page.waitForSelector("#dashboardView.active-view .metric");
  await page.screenshot({ path: "docs/dashboard-smoke.png", fullPage: true });
  await browser.close();

  console.log(JSON.stringify({ title, metrics, caseCount, txnCount }, null, 2));
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
