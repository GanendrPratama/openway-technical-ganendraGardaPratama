package com.qa.gmail;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GmailUnreadEmailTest {

    private static final Logger logger = LoggerFactory.getLogger(GmailUnreadEmailTest.class);

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String GMAIL_URL = "https://mail.google.com/mail/";
    private static final String TEST_EMAIL = System.getProperty("test.email", "");
    private static final String TEST_PASSWORD = System.getProperty("test.password", "");
    private static final String WEBDRIVER_URL = System.getProperty("webdriver.url", "");
    private static final int TIMEOUT_SECONDS = 30;

    @BeforeClass
    public void setUp() throws MalformedURLException {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--headless");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments(
                "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        options.setExperimentalOption("excludeSwitches", new String[] { "enable-automation" });
        options.setExperimentalOption("useAutomationExtension", false);

        if (WEBDRIVER_URL != null && !WEBDRIVER_URL.isEmpty()) {
            logger.info("Connecting to remote WebDriver at: {}", WEBDRIVER_URL);
            driver = new RemoteWebDriver(new URL(WEBDRIVER_URL), options);
        } else {
            logger.info("Using local WebDriver");
            WebDriverManager.chromedriver().setup();
            driver = new org.openqa.selenium.chrome.ChromeDriver(options);
        }

        // Remove webdriver flag to reduce bot detection
        ((JavascriptExecutor) driver).executeScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);

        wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SECONDS));
    }

    @Test
    public void testLoginAndGetLastUnreadEmailTitle() {
        logger.info("Starting Gmail test - login and retrieve last unread email title");
        logger.info("Test email: {}", TEST_EMAIL);
        logger.info("WebDriver URL: {}", WEBDRIVER_URL);

        driver.get(GMAIL_URL);
        logger.info("Navigated to Gmail login page");

        waitForPageToLoad();

        try {
            enterCredentialsAndLogin();

            handleOTPVerification();

            waitForInboxToLoad();

            String lastUnreadEmailTitle = getLastUnreadEmailTitle();
            logger.info("Last unread email title: {}", lastUnreadEmailTitle);

        } catch (Exception e) {
            logger.error("Test failed with exception: {}", e.getMessage(), e);
            captureDebugInfo("test_failure");
            throw e;
        }
    }

    private void enterCredentialsAndLogin() {
        logger.info("Entering email: {}", TEST_EMAIL);

        // Log current page info for debugging
        logger.info("Current URL before email entry: {}", driver.getCurrentUrl());
        logger.info("Current page title: {}", driver.getTitle());

        WebElement emailInput = wait.until(new ExpectedCondition<WebElement>() {
            public WebElement apply(WebDriver d) {
                // Try multiple selector strategies for the email field
                List<WebElement> candidates = d.findElements(By.cssSelector("input[type='email']"));
                candidates.addAll(d.findElements(By.xpath("//input[@name='identifier']")));
                candidates.addAll(d.findElements(By.xpath("//input[contains(@id, 'identifier')]")));
                for (WebElement input : candidates) {
                    if (input.isDisplayed() && input.isEnabled()) {
                        return input;
                    }
                }
                return null;
            }
        });

        emailInput.clear();
        emailInput.sendKeys(TEST_EMAIL);

        logger.info("Email entered, clicking Next button");

        // Click the "Next" button instead of pressing ENTER for more reliable flow
        try {
            WebElement nextButton = driver.findElement(By.xpath(
                    "//button[contains(text(),'Next')] | " +
                            "//span[contains(text(),'Next')]/ancestor::button | " +
                            "//div[@id='identifierNext'] | " +
                            "//div[@id='identifierNext']//button"));
            nextButton.click();
        } catch (Exception e) {
            logger.info("Next button not found, falling back to ENTER key");
            emailInput.sendKeys(Keys.ENTER);
        }

        // Wait for the page transition (Google animates between email → password views)
        logger.info("Waiting for page transition to password view...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Log page state after transition for debugging
        logger.info("URL after email submission: {}", driver.getCurrentUrl());
        logger.info("Page title after email submission: {}", driver.getTitle());

        // Check if Google blocked the login attempt
        checkForLoginBlockPage();

        captureDebugInfo("after_email_entry");

        logger.info("Waiting for password field...");

        WebDriverWait passwordWait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SECONDS));

        WebElement passwordInput = passwordWait.until(new ExpectedCondition<WebElement>() {
            public WebElement apply(WebDriver d) {
                // Google's password page uses various selectors; try multiple approaches
                List<WebElement> candidates = d.findElements(By.cssSelector("input[type='password']"));
                candidates.addAll(d.findElements(By.xpath("//input[@name='Passwd']")));
                candidates.addAll(d.findElements(By.xpath("//input[@name='password']")));
                candidates.addAll(d.findElements(By.xpath("//input[contains(@id, 'password')]")));
                candidates.addAll(d.findElements(By.xpath("//input[@aria-label='Enter your password']")));
                for (WebElement input : candidates) {
                    try {
                        if (input.isDisplayed() && input.isEnabled()) {
                            return input;
                        }
                    } catch (Exception ex) {
                        // Element may have become stale
                    }
                }
                return null;
            }
        });

        logger.info("Password field found, entering password");
        passwordInput.clear();
        passwordInput.sendKeys(TEST_PASSWORD);

        // Click the password "Next" button
        try {
            WebElement nextButton = driver.findElement(By.xpath(
                    "//div[@id='passwordNext'] | " +
                            "//div[@id='passwordNext']//button | " +
                            "//button[contains(text(),'Next')] | " +
                            "//span[contains(text(),'Next')]/ancestor::button"));
            nextButton.click();
        } catch (Exception e) {
            logger.info("Password Next button not found, falling back to ENTER key");
            passwordInput.sendKeys(Keys.ENTER);
        }

        logger.info("Credentials submitted");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check if Google is showing a "blocked" or "couldn't sign you in" page
     * and log helpful debug info.
     */
    private void checkForLoginBlockPage() {
        try {
            String pageSource = driver.getPageSource();

            if (pageSource.contains("Couldn't sign you in") ||
                    pageSource.contains("couldn't sign you in")) {
                logger.error("Google blocked the sign-in attempt: 'Couldn't sign you in' page detected");
                captureDebugInfo("login_blocked");
            }

            if (pageSource.contains("This browser or app may not be secure") ||
                    pageSource.contains("browser or app may not be secure")) {
                logger.error("Google rejected browser: 'This browser or app may not be secure'");
                captureDebugInfo("browser_not_secure");
            }

            if (pageSource.contains("Verify it's you") ||
                    pageSource.contains("verify it's you")) {
                logger.warn("Google is requesting additional verification");
                captureDebugInfo("verification_required");
            }

            if (pageSource.contains("Try again") && pageSource.contains("Wrong email")) {
                logger.error("Google reported wrong email address");
                captureDebugInfo("wrong_email");
            }

        } catch (Exception e) {
            logger.warn("Could not check for login block page: {}", e.getMessage());
        }
    }

    private void handleOTPVerification() {
        logger.info("Checking for OTP/verification requirement");

        try {
            WebDriverWait otpWait = new WebDriverWait(driver, Duration.ofSeconds(10));

            WebElement otpInput = otpWait.until(new ExpectedCondition<WebElement>() {
                public WebElement apply(WebDriver d) {
                    List<WebElement> inputs = d.findElements(By.xpath("//input[@type='tel']"));
                    inputs.addAll(d.findElements(By.xpath("//input[contains(@aria-label, 'code')]")));
                    for (WebElement input : inputs) {
                        if (input.isDisplayed()) {
                            return input;
                        }
                    }
                    return null;
                }
            });

            logger.warn("OTP verification detected. Please enter the verification code manually.");
            logger.info("Waiting for manual OTP entry...");

            otpWait.withTimeout(Duration.ofMinutes(5));

            otpWait.until(new ExpectedCondition<WebElement>() {
                public WebElement apply(WebDriver d) {
                    try {
                        WebElement confirmBtn = d
                                .findElement(By.xpath("//div[@role='button' and contains(text(), 'Next')]"));
                        return confirmBtn.isDisplayed() ? null : confirmBtn;
                    } catch (Exception e) {
                        return null;
                    }
                }
            });

        } catch (Exception e) {
            logger.info("No OTP verification required or verification completed");
        }
    }

    private void waitForInboxToLoad() {
        logger.info("Waiting for inbox to load");

        // Give the interstitial page time to try auto-redirect first
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String currentUrl = driver.getCurrentUrl();
        logger.info("Current URL after login: {}", currentUrl);
        logger.info("Current title after login: {}", driver.getTitle());
        captureDebugInfo("before_inbox_nav");

        // If stuck on the interstitial page, try multiple strategies
        if (!currentUrl.contains("mail.google.com/mail/")) {

            // Strategy 1: Extract the continue URL from the interstitial page URL
            if (currentUrl.contains("continue=") || currentUrl.contains("continue%3D")) {
                try {
                    String continueUrl = null;
                    if (currentUrl.contains("continue=")) {
                        continueUrl = currentUrl.split("continue=")[1].split("&")[0];
                    } else if (currentUrl.contains("continue%3D")) {
                        continueUrl = currentUrl.split("continue%3D")[1].split("&")[0];
                        continueUrl = java.net.URLDecoder.decode(continueUrl, "UTF-8");
                    }
                    if (continueUrl != null && continueUrl.contains("mail.google.com")) {
                        logger.info("Following continue URL from interstitial: {}", continueUrl);
                        driver.get(continueUrl);
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to extract continue URL: {}", e.getMessage());
                }
            }

            // Strategy 2: Try executing the interstitial's JS redirect
            currentUrl = driver.getCurrentUrl();
            if (!currentUrl.contains("mail.google.com/mail/")) {
                try {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    // Try to get the redirect URL from the page's config
                    Object redirectUrl = js.executeScript(
                            "try { " +
                                    "  if (window.WIZ_global_data && window.WIZ_global_data.HAZvpc) { " +
                                    "    return window.WIZ_global_data.HAZvpc; " +
                                    "  } " +
                                    "  var links = document.querySelectorAll('a[href*=\"mail.google.com\"]'); " +
                                    "  if (links.length > 0) return links[0].href; " +
                                    "  return null; " +
                                    "} catch(e) { return null; }");
                    if (redirectUrl != null) {
                        logger.info("Found redirect URL via JS: {}", redirectUrl);
                        driver.get(redirectUrl.toString());
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    logger.warn("JS redirect extraction failed: {}", e.getMessage());
                }
            }

            // Strategy 3: Direct navigation as final fallback
            currentUrl = driver.getCurrentUrl();
            if (!currentUrl.contains("mail.google.com/mail/")) {
                logger.info("All redirect strategies failed, trying direct navigation");
                driver.get(GMAIL_URL);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        currentUrl = driver.getCurrentUrl();
        String title = driver.getTitle();
        logger.info("Final URL: {}, Title: {}", currentUrl, title);
        captureDebugInfo("final_page_state");

        // Wait for inbox to be ready
        WebDriverWait inboxWait = new WebDriverWait(driver, Duration.ofSeconds(60));

        inboxWait.until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                String url = d.getCurrentUrl();
                String pageTitle = d.getTitle();
                logger.info("Inbox check: URL={}, Title={}", url, pageTitle);

                // If we're on the Gmail marketing page, login session was lost
                if (url.contains("workspace.google.com") ||
                        pageTitle.contains("Google Workspace")) {
                    logger.warn("Landed on Gmail marketing page, not authenticated inbox");
                    // Try navigating to Gmail again
                    d.get(GMAIL_URL);
                    return false;
                }

                boolean isGmailUrl = url.contains("mail.google.com/mail/");
                boolean hasGmailTitle = pageTitle.contains("Gmail") || pageTitle.contains("Inbox");
                boolean notSignIn = !pageTitle.contains("Sign in") && !pageTitle.contains("Google Accounts");

                return isGmailUrl && notSignIn && (hasGmailTitle || pageTitle.isEmpty());
            }
        });

        // Wait for Gmail's AJAX content to load
        waitForPageToLoad();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Inbox loaded successfully");
        captureDebugInfo("inbox_loaded");
    }

    /**
     * Handle Google's interstitial page that appears between login and Gmail
     * redirect.
     * This page (doritos/forward/success) sometimes requires clicking a "Continue"
     * button.
     */
    private void handleInterstitialPage() {
        try {
            String currentUrl = driver.getCurrentUrl();
            logger.info("Checking for interstitial page, URL: {}", currentUrl);

            if (currentUrl.contains("accounts.google.com")) {
                // Try clicking various "Continue" / "Yes" / confirmation buttons
                List<WebElement> buttons = driver.findElements(By.xpath(
                        "//button | //div[@role='button'] | //input[@type='submit']"));

                for (WebElement button : buttons) {
                    try {
                        String text = button.getText().toLowerCase();
                        if (text.contains("continue") || text.contains("yes") ||
                                text.contains("confirm") || text.contains("next") ||
                                text.contains("i agree") || text.contains("accept")) {
                            logger.info("Clicking interstitial button: {}", button.getText());
                            button.click();
                            Thread.sleep(3000);
                            break;
                        }
                    } catch (Exception e) {
                        // Continue trying other buttons
                    }
                }

                // Also try using JavaScript to trigger any pending redirects
                try {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    // Check if there's a redirect URL in the page
                    Object result = js.executeScript(
                            "var meta = document.querySelector('meta[http-equiv=\"refresh\"]'); " +
                                    "if (meta) return meta.getAttribute('content'); " +
                                    "return null;");
                    if (result != null) {
                        logger.info("Found meta refresh: {}", result);
                    }
                } catch (Exception e) {
                    // Ignore
                }

                // Wait a bit for auto-redirect
                Thread.sleep(5000);
                logger.info("After interstitial handling, URL: {}", driver.getCurrentUrl());
                captureDebugInfo("after_interstitial");
            }
        } catch (Exception e) {
            logger.warn("Error handling interstitial page: {}", e.getMessage());
        }
    }

    private String getLastUnreadEmailTitle() {
        logger.info("Searching for last unread email");

        waitForPageToLoad();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Strategy 1: Standard unread email selectors
        List<WebElement> unreadEmails = driver.findElements(By.xpath(
                "//div[@class='zA yO' and not(contains(@class, 'zE'))]//span[@class='bog']"));

        if (unreadEmails.isEmpty()) {
            // Strategy 2: tr-based layout
            unreadEmails = driver.findElements(By.xpath(
                    "//tr[contains(@class, 'zA') and not(contains(@class, 'zE'))]//span[@class='bog']"));
        }

        if (unreadEmails.isEmpty()) {
            // Strategy 3: div-based layout with subject span
            unreadEmails = driver.findElements(By.xpath(
                    "//div[contains(@class, 'zA') and not(contains(@class, 'zE'))]//div[contains(@class, 'y6')]/span"));
        }

        if (unreadEmails.isEmpty()) {
            // Strategy 4: Bold (unread) email subjects
            unreadEmails = driver.findElements(By.xpath(
                    "//tr[contains(@class, 'zE')]//span[@class='bog'] | " +
                            "//div[contains(@class, 'zE')]//span[@class='bog']"));
        }

        if (unreadEmails.isEmpty()) {
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("document.querySelector('[data-group=\"unread\"]').click();");
                Thread.sleep(1000);
            } catch (Exception e) {
                logger.info("Clicking unread filter");
            }

            unreadEmails = driver.findElements(By.xpath(
                    "//div[@class='zA yO']//span[@class='bog']"));
        }

        if (!unreadEmails.isEmpty()) {
            int lastIndex = unreadEmails.size() - 1;
            WebElement lastUnreadEmail = unreadEmails.get(lastIndex);
            String title = lastUnreadEmail.getText();
            logger.info("Found unread email at position {}, title: {}", lastIndex, title);
            return title;
        }

        logger.warn("No unread emails found. Trying to find any email in inbox.");

        List<WebElement> anyEmails = driver.findElements(By.xpath(
                "//div[@class='zA yO']//span[@class='bog']"));

        if (!anyEmails.isEmpty()) {
            int lastIndex = anyEmails.size() - 1;
            String title = anyEmails.get(lastIndex).getText();
            logger.info("Using last email (not unread specific), title: {}", title);
            return title;
        }

        logger.warn("No emails found in inbox");
        return "No emails found";
    }

    private void waitForPageToLoad() {
        wait.until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                JavascriptExecutor js = (JavascriptExecutor) d;
                return js.executeScript("return document.readyState").equals("complete");
            }
        });
    }

    /**
     * Capture a screenshot and page source for debugging.
     */
    private void captureDebugInfo(String label) {
        try {
            Path outputDir = Paths.get("/app/test-output");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // Save screenshot
            if (driver instanceof TakesScreenshot) {
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                Path dest = outputDir.resolve("screenshot_" + label + ".png");
                Files.copy(screenshot.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Screenshot saved to: {}", dest);
            }

            // Save page source
            String pageSource = driver.getPageSource();
            Path htmlDest = outputDir.resolve("page_source_" + label + ".html");
            Files.write(htmlDest, pageSource.getBytes());
            logger.info("Page source saved to: {}", htmlDest);

        } catch (Exception e) {
            logger.warn("Failed to capture debug info: {}", e.getMessage());
        }
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            logger.info("Closing browser");
            driver.quit();
        }
    }
}