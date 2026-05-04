package com.example.stackoverflow;

import org.openqa.selenium.By;

final class StackOverflowLocators {
    static final By PAGE_HEADING = By.xpath("//h1[normalize-space()]");
    static final By MAIN_SEARCH = By.xpath("//form[contains(@action,'/search')]//input[(contains(@name,'q') or contains(@aria-label,'Search')) and not(@type='hidden')]");
    static final By QUESTION_SUMMARIES = By.xpath("//div[contains(@class,'s-post-summary') or contains(@class,'question-summary')]");
    static final By FIRST_QUESTION_LINK = By.xpath("(//div[contains(@class,'s-post-summary') or contains(@class,'question-summary')]//h3//a[contains(@href,'/questions/') and normalize-space()])[1]");
    static final By QUESTION_TITLE = By.xpath("//h1[normalize-space()]");
    static final By QUESTION_BODY = By.xpath("//div[contains(@class,'question')]//div[contains(@class,'s-prose') or @itemprop='text'] | //div[contains(@class,'s-prose') and normalize-space()]");
    static final By POST_TAGS = By.xpath("//a[contains(@class,'post-tag') and normalize-space()]");
    static final By ASK_QUESTION = By.xpath("//a[contains(@href,'/questions/ask') and contains(normalize-space(.),'Ask Question')]");
    static final By LOGIN_REQUIRED_MESSAGE = By.xpath("//*[contains(normalize-space(.),'You must be logged in to ask a question') or contains(normalize-space(.),'Log in below')]");
    static final By TAGS_NAVIGATION = By.xpath("//a[contains(@href,'/tags') and contains(normalize-space(.),'Tags')]");
    static final By TAG_SEARCH_INPUT = By.xpath("//input[contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'filter') and contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'tag') or contains(translate(@aria-label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'filter by tag') or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'tag')]");
    static final By PYTHON_TAG_LINK = By.xpath("//a[normalize-space(.)='python' and (contains(@href,'/questions/tagged/python') or contains(@href,'/tags/python'))]");
    static final By ACTIVE_TAB = By.xpath("//a[contains(@href,'tab=Active') and normalize-space()='Active']");
    static final By ACTIVE_HEADING = By.xpath("//*[self::h1 or self::h2][contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'active')]");
    static final By SEARCH_RESULTS = By.xpath("//a[contains(@href,'/questions/') and not(contains(@href,'/questions/ask')) and not(contains(@href,'/questions/tagged')) and normalize-space()]");
    static final By COOKIE_BUTTON = By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'accept') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'necessary')]");

    private StackOverflowLocators() {
    }
}
