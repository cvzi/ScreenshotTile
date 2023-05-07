package com.github.cvzi.screenshottile.activities

import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    companion object {
        init {
            BuildConfig.TESTING_MODE.value = true
        }
    }

    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()


    @Test
    fun mainActivityTest() {

        val scenario = activityRule.scenario

        var stringMoreSetting = ""
        var stringSettingPostActions = ""
        var stringFloatingButtonSettings = ""
        var stringButtonHistory = ""
        var stringTutorial = ""
        scenario.onActivity { activity ->
            stringMoreSetting = activity.getString(R.string.more_setting)
            stringSettingPostActions = activity.getString(R.string.setting_post_actions)
            stringFloatingButtonSettings = activity.getString(R.string.floating_button_settings)
            stringButtonHistory = activity.getString(R.string.button_history)
            stringTutorial = activity.getString(R.string.tutorial)
        }

        val materialButton = onView(
            allOf(
                withId(R.id.buttonSettings), withText(stringMoreSetting), childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.LinearLayout")), 1
                    ), 0
                ), isDisplayed()
            )
        )

        while (stringMoreSetting.isBlank()) {
            Thread.sleep(1000)
        }

        materialButton.perform(click())

        pressBack()

        onView(withId(R.id.buttonPostActions)).perform(scrollTo())

        val materialButtonPostSettings = onView(
            allOf(
                withId(R.id.buttonPostActions), withText(stringSettingPostActions), childAtPosition(
                    childAtPosition(
                        withClassName(`is`("androidx.cardview.widget.CardView")), 0
                    ), 2
                )
            )
        )
        materialButtonPostSettings.perform(click())

        pressBack()


        onView(withId(R.id.buttonFloatingButtonSettings)).perform(scrollTo())

        val materialButtonFloatingButtonSettings = onView(
            allOf(
                withId(R.id.buttonFloatingButtonSettings),
                withText(stringFloatingButtonSettings),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("androidx.cardview.widget.CardView")), 0
                    ), 2
                )
            )
        )
        materialButtonFloatingButtonSettings.perform(click())

        pressBack()


        onView(withId(R.id.buttonHistory)).perform(scrollTo())

        val materialButtonHistory = onView(
            allOf(
                withId(R.id.buttonHistory), withText(stringButtonHistory), childAtPosition(
                    childAtPosition(
                        withClassName(`is`("androidx.cardview.widget.CardView")), 0
                    ), 2
                )
            )
        )
        materialButtonHistory.perform(click())

        pressBack()


        val materialButtonTutorial = onView(
            allOf(
                withId(R.id.buttonTutorial), withText(stringTutorial), childAtPosition(
                    childAtPosition(
                        withClassName(`is`("androidx.cardview.widget.CardView")), 0
                    ), 2
                )
            )
        )
        materialButtonTutorial.perform(scrollTo(), click())

        val clickableImageView = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView.perform(click())

        val clickableImageView2 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView2.perform(click())

        val clickableImageView3 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView3.perform(click())

        val clickableImageView4 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView4.perform(click())

        val clickableImageView5 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView5.perform(click())

        val clickableImageView6 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView6.perform(click())

        val clickableImageView7 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView7.perform(click())

        val clickableImageView8 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView8.perform(click())

        val clickableImageView9 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView9.perform(click())

        val clickableImageView10 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView10.perform(click())

        val clickableImageView11 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView11.perform(click())

        val clickableImageView12 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView12.perform(click())

        val clickableImageView13 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView13.perform(click())

        val clickableImageView14 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView14.perform(click())

        val clickableImageView15 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView15.perform(click())

        val clickableImageView16 = onView(
            allOf(
                withParent(
                    allOf(
                        withId(R.id.viewPager), childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")), 2
                        )
                    )
                ), isDisplayed()
            )
        )
        clickableImageView16.perform(click())

        pressBack()


    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent) && view == parent.getChildAt(
                    position
                )
            }
        }
    }
}
