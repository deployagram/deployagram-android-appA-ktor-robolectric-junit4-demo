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
import com.deployagram.junit4.DeployagramRule
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Tests AppA's "Load Config" button, which calls a second back-end, AliceMessager. Both back-ends are doubles behind
 * one Ktor MockEngine, so no network is used.
 *
 * The button sends "Hello" when a book has already been recommended, and an empty payload when it has not.
 */
@RunWith(RobolectricTestRunner::class)
class LoadConfigButtonTest {

    @get:Rule
    val deployagram = DeployagramRule()

    private val defaultBookOverflowEngine = BookOverflowConfig.engine
    private val defaultAliceMessagerEngine = AliceMessagerConfig.engine
    private lateinit var backEnds: BackEnds
    private lateinit var enteredEmail: String
    private var screen: ActivityScenario<MainActivity>? = null

    @Before
    fun startBackEnds() {
        backEnds = BackEnds()
        enteredEmail = "reader-${UUID.randomUUID()}@example.com"
        // Deployagram records every exchange with the back-end it was sent to.
        BookOverflowConfig.engine = backEnds.recordedEngine()
        AliceMessagerConfig.engine = backEnds.recordedEngine()
    }

    @After
    fun stopBackEnds() {
        screen?.close()
        screen = null
        BookOverflowConfig.engine = defaultBookOverflowEngine
        AliceMessagerConfig.engine = defaultAliceMessagerEngine
    }

    @Test
    fun pressingLoadConfigAfterARecommendationSendsHelloAndShowsWorld() {
        givenBookOverflowHasASuggestion()
        givenABookHasBeenRecommended()

        whenTheLoadConfigButtonIsPressed()

        thenTheReplyIsShown("World")
        thenAliceMessagerWasSentTheConfigRequest(AliceMessagerAnswers.HELLO)
    }

    @Test
    fun pressingLoadConfigWithNoRecommendationSendsAnEmptyPayloadAndShowsTheReply() {
        givenTheScreenIsOpen()

        whenTheLoadConfigButtonIsPressed()

        thenTheReplyIsShown("Is someone there?")
        thenAliceMessagerWasSentTheConfigRequest(null)
    }

    // Given

    private fun givenBookOverflowHasASuggestion() {
        backEnds.bookOverflow.suggestionJson = """
            {
              "name": "Refactoring",
              "edition": "1st",
              "format": "paperback",
              "authors": ["Fowler Martin", "Beck Kent"]
            }
        """.trimIndent()
    }

    private fun givenTheScreenIsOpen() {
        screen = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun givenABookHasBeenRecommended() {
        // The recommendation is made through the screen, as a user would make it.
        givenTheScreenIsOpen()
        onView(withHint("Email address")).perform(replaceText(enteredEmail), closeSoftKeyboard())
        onView(withText("Recommend book")).perform(click())
        waitUntilShown(withText(containsString("Refactoring")))
    }

    // When

    private fun whenTheLoadConfigButtonIsPressed() {
        onView(withText("Load Config")).perform(click())
    }

    // Then

    private fun thenTheReplyIsShown(reply: String) {
        waitUntilShown(withText(reply))
    }

    private fun thenAliceMessagerWasSentTheConfigRequest(expectedBody: String?) {
        val sent = backEnds.aliceMessagerApp.nextRequest() ?: error("AliceMessager was not sent a request")
        check(sent.path == "/AliceMessager/config") { "sent to ${sent.path}" }
        check(sent.body == expectedBody) { "sent body ${sent.body}" }
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
