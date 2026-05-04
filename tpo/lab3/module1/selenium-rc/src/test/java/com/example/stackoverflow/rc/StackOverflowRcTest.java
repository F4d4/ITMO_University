package com.example.stackoverflow.rc;

import com.thoughtworks.selenium.DefaultSelenium;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class StackOverflowRcTest {
    private static final String BASE_URL = "http://stackoverflow.com";
    private static final String TIMEOUT = "30000";

    private static final String PAGE_HEADING = "//h1[normalize-space()]";
    private static final String MAIN_SEARCH = "//form[contains(@action,'/search')]//input[(contains(@name,'q') or contains(@aria-label,'Search')) and not(@type='hidden')]";
    private static final String QUESTION_SUMMARIES = "//div[contains(@class,'s-post-summary') or contains(@class,'question-summary')]";
    private static final String FIRST_QUESTION_LINK = "(//div[contains(@class,'s-post-summary') or contains(@class,'question-summary')]//h3//a[contains(@href,'/questions/') and normalize-space()])[1]";
    private static final String QUESTION_BODY = "//div[contains(@class,'question')]//div[contains(@class,'s-prose') or @itemprop='text'] | //div[contains(@class,'s-prose') and normalize-space()]";
    private static final String POST_TAGS = "//a[contains(@class,'post-tag') and normalize-space()]";
    private static final String ASK_QUESTION = "//a[contains(@href,'/questions/ask') and contains(normalize-space(.),'Ask Question')]";
    private static final String LOGIN_REQUIRED_MESSAGE = "//*[contains(normalize-space(.),'You must be logged in to ask a question') or contains(normalize-space(.),'Log in below')]";
    private static final String TAGS_NAVIGATION = "//a[contains(@href,'/tags') and contains(normalize-space(.),'Tags')]";
    private static final String PYTHON_TAG_LINK = "//a[normalize-space(.)='python' and (contains(@href,'/questions/tagged/python') or contains(@href,'/tags/python'))]";
    private static final String ACTIVE_TAB = "//a[contains(@href,'tab=Active') and normalize-space()='Active']";
    private static final String ACTIVE_HEADING = "//*[self::h1 or self::h2][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'active')]";
    private static final String SEARCH_RESULTS = "//a[contains(@href,'/questions/') and not(contains(@href,'/questions/ask')) and not(contains(@href,'/questions/tagged')) and normalize-space()]";

    private DefaultSelenium selenium;

    @Before
    public void setUp() {
        String host = System.getProperty("rc.host", "localhost");
        int port = Integer.parseInt(System.getProperty("rc.port", "4444"));
        String browser = System.getProperty("rc.browser", "*firefox");

        selenium = new DefaultSelenium(host, port, browser, BASE_URL);
        selenium.start();
    }

    @After
    public void tearDown() {
        if (selenium != null) {
            selenium.stop();
        }
    }

    @Test
    public void newestQuestionsPageShowsQuestionListAndNavigation() throws Exception {
        open("/questions");

        waitForElement(PAGE_HEADING);
        assertTrue(selenium.isTextPresent("Newest Questions"));
        waitForElement(QUESTION_SUMMARIES);
        waitForElement(ASK_QUESTION);
        waitForElement(TAGS_NAVIGATION);
    }

    @Test
    public void visitorSearchesQuestionsByTextQuery() throws Exception {
        open("/questions");

        waitForElement(MAIN_SEARCH);
        selenium.type(xpath(MAIN_SEARCH), "selenium webdriver");
        selenium.keyPress(xpath(MAIN_SEARCH), "\\13");

        waitForLocation("/search");
        assertTrue(selenium.getLocation().contains("q="));
        waitForElement(SEARCH_RESULTS);
    }

    @Test
    public void visitorOpensQuestionAndSeesContent() throws Exception {
        open("/questions");

        waitForElement(FIRST_QUESTION_LINK);
        selenium.click(xpath(FIRST_QUESTION_LINK));

        waitForLocation("/questions/");
        waitForElement(PAGE_HEADING);
        waitForElement(QUESTION_BODY);
        waitForElement(POST_TAGS);
    }

    @Test
    public void visitorOpensPopularTagPage() throws Exception {
        open("/tags");

        waitForElement(PAGE_HEADING);
        assertTrue(selenium.isTextPresent("Tags"));
        waitForElement(PYTHON_TAG_LINK);
        selenium.click(xpath(PYTHON_TAG_LINK));

        waitForLocation("python");
        assertTrue(selenium.getLocation().contains("python"));
    }

    @Test
    public void visitorSwitchesQuestionsToActiveSorting() throws Exception {
        open("/questions");

        waitForElement(ACTIVE_TAB);
        selenium.click(xpath(ACTIVE_TAB));

        waitUntil(new Condition() {
            public boolean check() {
                return selenium.getLocation().toLowerCase().contains("tab=active")
                        || selenium.isElementPresent(xpath(ACTIVE_HEADING));
            }
        });
    }

    @Test
    public void anonymousVisitorIsRedirectedToLoginWhenAskingQuestion() throws Exception {
        open("/questions");

        waitForElement(ASK_QUESTION);
        selenium.click(xpath(ASK_QUESTION));

        waitUntil(new Condition() {
            public boolean check() {
                return selenium.getLocation().contains("/users/login")
                        || selenium.isElementPresent(xpath(LOGIN_REQUIRED_MESSAGE));
            }
        });
    }

    private void open(String path) {
        selenium.open(path);
        selenium.waitForPageToLoad(TIMEOUT);
    }

    private void waitForElement(final String expression) throws Exception {
        waitUntil(new Condition() {
            public boolean check() {
                return selenium.isElementPresent(xpath(expression));
            }
        });
    }

    private void waitForLocation(final String expected) throws Exception {
        waitUntil(new Condition() {
            public boolean check() {
                return selenium.getLocation().contains(expected);
            }
        });
    }

    private void waitUntil(Condition condition) throws Exception {
        long deadline = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < deadline) {
            if (isBlockedByVerification()) {
                throw new AssertionError("Stack Overflow returned Cloudflare verification instead of the tested interface");
            }
            if (condition.check()) {
                return;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Condition was not met. Current URL: " + selenium.getLocation());
    }

    private boolean isBlockedByVerification() {
        String body = selenium.getBodyText().toLowerCase();
        String title = selenium.getTitle().toLowerCase();
        return body.contains("human verification")
                || body.contains("are you a human")
                || body.contains("verifying you are human")
                || body.contains("security service to protect against malicious bots")
                || title.contains("just a moment");
    }

    private String xpath(String expression) {
        return "xpath=" + expression;
    }

    private interface Condition {
        boolean check();
    }
}
