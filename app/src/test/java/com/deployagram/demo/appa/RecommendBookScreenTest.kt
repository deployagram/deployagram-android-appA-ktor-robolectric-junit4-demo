package com.deployagram.demo.appa

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withText
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Tests AppA's "Recommend book" screen on the JVM, with Robolectric. AppA's Ktor client sends its request to a
 * MockEngine double, so no network and no HTTP server are used.
 *
 * The test never sends an HTTP request itself. It types an email and presses the button, and AppA makes the
 * request to BookOverflow. The double records the requests it receives, and the test checks them and the screen.
 */
@RunWith(RobolectricTestRunner::class)
class RecommendBookScreenTest {

    private val defaultEngine = BookOverflowConfig.engine
    private lateinit var bookOverflowAnswers: SuggestionAnswers
    private lateinit var enteredEmail: String
    private var screen: ActivityScenario<MainActivity>? = null

    @Before
    fun startBookOverflow() {
        bookOverflowAnswers = SuggestionAnswers()
        enteredEmail = "reader-${UUID.randomUUID()}@example.com"
        BookOverflowConfig.engine = MockEngine(bookOverflowAnswers.handler())
    }

    @After
    fun stopBookOverflow() {
        screen?.close()
        screen = null
        BookOverflowConfig.engine = defaultEngine
    }

    @Test
    fun pressingRecommendBookAsksBookOverflowForASuggestionAndShowsIt() {
        givenBookOverflowHasASuggestion()

        whenTheRecommendBookButtonIsPressed()

        thenTheSuggestionIsShown()
        thenBookOverflowWasAskedForTheEnteredEmail()
    }

    @Test
    fun pressingRecommendBookWithNoEmailAsksBookOverflowWhichRefusesAndAsksForAValidEmail() {
        givenBookOverflowRefusesARequestWithNoEmail()
        givenNoEmailIsEntered()

        whenTheRecommendBookButtonIsPressed()

        thenTheInvalidEmailPromptIsShown()
        thenBookOverflowWasCalledWithoutAnEmailAddress()
    }

    // Given

    private fun givenBookOverflowHasASuggestion() {
        bookOverflowAnswers.suggestionJson = """
            {
              "name": "Refactoring",
              "edition": "1st",
              "format": "paperback",
              "authors": ["Fowler Martin", "Beck Kent"]
            }
        """.trimIndent()
    }

    private fun givenBookOverflowRefusesARequestWithNoEmail() {
        bookOverflowAnswers.refusesRequestsWithNoEmail = true
    }

    private fun givenNoEmailIsEntered() {
        enteredEmail = ""
    }

    // When

    private fun whenTheRecommendBookButtonIsPressed() {
        screen = ActivityScenario.launch(MainActivity::class.java)
        onView(withHint("Email address"))
            .perform(replaceText(enteredEmail), closeSoftKeyboard())
        onView(withText("Recommend book"))
            .perform(click())
    }

    // Then

    private fun thenTheSuggestionIsShown() {
        // AppA makes the request in the background, so the screen is checked until the result appears.
        waitUntilShown(withText(containsString("Refactoring")))
    }

    private fun thenTheInvalidEmailPromptIsShown() {
        waitUntilShown(withText("You must enter a valid email!"))
    }

    private fun thenBookOverflowWasAskedForTheEnteredEmail() {
        val url = bookOverflowAnswers.nextRequest()
            ?: error("BookOverflow was not asked for a suggestion for \"$enteredEmail\"")
        check(url.encodedPath == "/BookOverflow/suggestion") { "asked at $url" }
        check(url.parameters["email"] == enteredEmail) { "asked for $url" }
    }

    private fun thenBookOverflowWasCalledWithoutAnEmailAddress() {
        val url = bookOverflowAnswers.nextRequest()
            ?: error("BookOverflow was not asked for a suggestion")
        check(url.encodedPath == "/BookOverflow/suggestion") { "asked at $url" }
        check(url.parameters["email"] == "") { "asked for $url" }
    }

    /**
     * Checks the screen until the matcher passes, running the main looper between checks so that the
     * background response can update the screen.
     */
    private fun waitUntilShown(matcher: org.hamcrest.Matcher<android.view.View>) {
        val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10)
        while (true) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return
            } catch (failure: NoMatchingViewException) {
                if (System.currentTimeMillis() > deadline) throw failure
            } catch (failure: AssertionError) {
                if (System.currentTimeMillis() > deadline) throw failure
            }
            ShadowLooper.idleMainLooper()
            Thread.sleep(50)
        }
    }
}

/**
 * What BookOverflow answers, as a Ktor MockEngine. A test sets what it wants with the given steps.
 * Until then, BookOverflow has no suggestion and answers 404. Each request it receives is queued for the test.
 */
class SuggestionAnswers {
    @Volatile
    var suggestionJson: String? = null

    // When true, a request without an email is refused with 400, as BookOverflow refuses it.
    @Volatile
    var refusesRequestsWithNoEmail = false

    private val receivedUrls = LinkedBlockingQueue<Url>()

    fun handler(): MockRequestHandler = { request -> with(this@SuggestionAnswers) { answer(request) } }

    fun engine(): HttpClientEngine = MockEngine(handler())

    fun nextRequest(): Url? = receivedUrls.poll(5, TimeUnit.SECONDS)

    fun MockRequestHandleScope.answer(request: HttpRequestData): HttpResponseData {
        receivedUrls.add(request.url)
        if (refusesRequestsWithNoEmail && request.url.parameters["email"].isNullOrBlank()) {
            return respond(content = "", status = HttpStatusCode.BadRequest)
        }
        val json = suggestionJson ?: return respond(content = "", status = HttpStatusCode.NotFound)
        return respond(
            content = json,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }
}
