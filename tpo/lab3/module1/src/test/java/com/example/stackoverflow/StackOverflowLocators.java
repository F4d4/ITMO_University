package com.example.stackoverflow;

import org.openqa.selenium.By;

final class StackOverflowLocators {
    static final By PAGE_HEADING = By.xpath("//h1[normalize-space()]");
    static final By MAIN_SEARCH = By.xpath("//form[contains(@action,'/search')]//input[(contains(@name,'q') or contains(@aria-label,'Search')) and not(@type='hidden')]");
    static final By QUESTION_SUMMARIES = By.xpath("//div[contains(@class,'s-post-summary') or contains(@class,'question-summary')]");
    static final By FIRST_QUESTION_LINK = By.xpath("(//div[contains(@class,'s-post-summary') or contains(@class,'question-summary')]//h3//a[contains(@href,'/questions/') and normalize-space()])[1]");
    static final By QUESTION_TITLE = By.xpath("//h1[@itemprop='name'] | //h1[contains(@class,'fs-headline1') and normalize-space()] | //div[contains(@class,'question-header')]//h1[normalize-space()] | //h1//a[contains(@class,'question-hyperlink')]");
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

    static final By USER_AVATAR = By.xpath("//header//a[contains(@href,'/users/') and (.//img[contains(@class,'gravatar') or contains(@class,'avatar')] or contains(@class,'my-profile') or contains(@class,'s-user-card'))]");
    static final By LOGIN_LINK = By.xpath("//header//a[contains(@href,'/users/login') and (normalize-space()='Log in' or normalize-space()='Sign in')]");
    static final By REPUTATION_BADGE = By.xpath("//header//*[contains(@class,'reputation') or contains(@class,'-reputation')][normalize-space()]");

    static final By ASK_TITLE_INPUT = By.xpath("//input[@type='text' and (@name='post-title' or @label='Title' or contains(@aria-label,'Title') or contains(@placeholder,'title') or (@data-min-length and @data-max-length))]");
    static final By ASK_TAGS_INPUT = By.xpath("//input[@name='tagnames' or contains(@placeholder,'tag') or contains(translate(@aria-label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'tag')]");
    static final By POST_QUESTION_BUTTON = By.xpath("//button[normalize-space()='Post Your Question' or normalize-space()='Post your question' or normalize-space()='Review your question' or normalize-space()='Submit to Staging Ground' or normalize-space()='Submit your question']");
    static final By DEPLOY_TO_STACKOVERFLOW_RADIO = By.xpath("//label[contains(normalize-space(.),'Post question on Stack Overflow now') or contains(normalize-space(.),\"I'm sure that my question\")] | //input[@type='radio' and (contains(@id,'stackOverflowOptIn') or contains(@value,'stack'))]");

    static final By VOTE_UP_BUTTON = By.xpath("//button[contains(@class,'js-vote-up-btn') or contains(translate(@aria-label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'up vote') or contains(translate(@aria-label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'upvote') or contains(@class,'js-vote-arrow')] | //div[contains(@class,'js-vote-up') or contains(@class,'vote-up')]");
    static final By ANSWER_FORM_CONTAINER = By.xpath("//form[@id='post-form' or contains(@action,'/answer/submit')] | //div[contains(@class,'js-add-answer-component')]");
    static final By ANSWER_BACKING_TEXTAREA = By.xpath("//form[@id='post-form' or contains(@action,'/answer/submit')]//textarea[@name='post-text'] | //textarea[@name='post-text']");
    static final By ANSWER_EDITOR_TEXTBOX = By.xpath("//form[@id='post-form' or contains(@action,'/answer/submit')]//div[@role='textbox'] | //div[contains(@class,'js-add-answer-component')]//div[@role='textbox' or @contenteditable='true']");
    static final By POST_ANSWER_BUTTON = By.xpath("//button[normalize-space()='Post Your Answer' or normalize-space()='Post your answer']");
    static final By ANSWER_BLOCK = By.xpath("//div[(contains(@class,'answer') or @id='answers') and .//div[contains(@class,'s-prose')]][not(contains(@class,'answers-subheader'))]");
    static final By ANSWER_BODY_TEXT = By.xpath("//div[contains(@class,'answer')]//div[contains(@class,'s-prose') and @itemprop='text']");

    static final By ASK_BODY_BACKING_TEXTAREA = By.xpath("//textarea[@name='post-text']");
    static final By ASK_BODY_EDITOR_TEXTBOX = By.xpath("//div[@role='textbox'] | //div[contains(@class,'js-stacks-editor')]//div[@contenteditable='true']");

    static final By LOGIN_FORM_EMAIL = By.xpath("//form[@id='login-form']//input[@name='email']");
    static final By LOGIN_FORM_PASSWORD = By.xpath("//form[@id='login-form']//input[@name='password']");
    static final By LOGIN_FORM_SUBMIT = By.xpath("//form[@id='login-form']//button[@name='submit-button' or normalize-space()='Log in']");

    static final By QUESTION_DELETE_LINK = By.xpath("//div[@id='question' or contains(@class,'js-question') or contains(@class,'question')]//button[contains(@class,'js-delete-post') and not(contains(@class,'deleted-post'))] | //div[@id='question' or contains(@class,'js-question')]//a[normalize-space()='Delete']");
    static final By ANSWER_DELETE_LINK = By.xpath("//div[contains(@class,'answer') and not(contains(@class,'answers-subheader'))]//button[contains(@class,'js-delete-post') and not(contains(@class,'deleted-post'))] | //div[contains(@class,'answer')]//a[normalize-space()='Delete']");
    static final By DELETE_CONFIRM_BUTTON = By.xpath("//div[contains(@class,'s-modal') or contains(@class,'popup')]//button[normalize-space()='Delete' or contains(@class,'js-popup-submit') or contains(@class,'js-modal-submit')]");
    static final By DELETED_BADGE = By.xpath("//*[contains(normalize-space(.),'has been deleted') or contains(normalize-space(.),'Question deleted') or contains(normalize-space(.),'Page not found') or contains(normalize-space(.),'has been removed')]");

    private StackOverflowLocators() {
    }
}
