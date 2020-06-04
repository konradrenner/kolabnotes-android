package org.kore.kolabnotes.android;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.action.ViewActions;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.kore.kolabnotes.android.IsEqualTrimmingAndIgnoringCase.equalToTrimmingAndIgnoringCase;
import static org.kore.kolabnotes.android.VisibleViewMatcher.isVisible;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void mainActivityTest() {
        ViewInteraction root = onView(isRoot());
        root.perform(getSwipeAction(540, 897, 540, 1794));

        waitToScrollEnd();

        ViewInteraction root2 = onView(isRoot());
        root2.perform(getSwipeAction(540, 897, 540, 1794));

        waitToScrollEnd();

        ViewInteraction android_widget_ImageButton =
                onView(
                        allOf(
                                withContentDescription(equalToTrimmingAndIgnoringCase("Navigate up")),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.toolbar),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.activity_main_frame),
                                                                isDescendantOfA(withId(R.id.activity_main))))))));
        android_widget_ImageButton.perform(getClickAction());

        ViewInteraction androidx_appcompat_widget_LinearLayoutCompat =
                onView(
                        allOf(
                                classOrSuperClassesName(is("androidx.appcompat.widget.LinearLayoutCompat")),
                                isVisible(),
                                hasDescendant(
                                        allOf(
                                                withId(R.id.design_menu_item_text),
                                                withTextOrHint(equalToTrimmingAndIgnoringCase("Notes from all accounts")))),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.design_navigation_view),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.navigation_view),
                                                                isDescendantOfA(withId(R.id.activity_main))))))));
        androidx_appcompat_widget_LinearLayoutCompat.perform(getClickAction());

        ViewInteraction root3 = onView(isRoot());
        root3.perform(getSwipeAction(540, 897, 540, 1794));

        waitToScrollEnd();

        ViewInteraction android_widget_ImageView =
                onView(
                        allOf(
                                withContentDescription(equalToTrimmingAndIgnoringCase("More options")),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.toolbar),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.activity_main_frame),
                                                                isDescendantOfA(withId(R.id.activity_main))))))));
        android_widget_ImageView.perform(getClickAction());

        ViewInteraction android_widget_LinearLayout =
                onView(
                        allOf(
                                classOrSuperClassesName(is("android.widget.LinearLayout")),
                                isVisible(),
                                hasDescendant(
                                        allOf(
                                                withId(R.id.content),
                                                hasDescendant(
                                                        allOf(
                                                                withId(R.id.title),
                                                                withTextOrHint(
                                                                        equalToTrimmingAndIgnoringCase("New Notebook"))))))));
        android_widget_LinearLayout.perform(getClickAction());

        ViewInteraction android_widget_EditText =
                onView(
                        allOf(
                                withId(R.id.dialog_text_input_field),
                                withTextOrHint(equalToTrimmingAndIgnoringCase("Summary")),
                                isVisible()));
        android_widget_EditText.perform(replaceText("leucoplast"));

        Espresso.pressBackUnconditionally();

        ViewInteraction android_widget_TextView =
                onView(
                        allOf(
                                withId(R.id.action_search),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.toolbar),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.activity_main_frame),
                                                                isDescendantOfA(withId(R.id.activity_main))))))));
        android_widget_TextView.perform(getClickAction());

        ViewInteraction android_widget_TextView2 =
                onView(
                        allOf(
                                withId(R.id.tag_list),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.toolbar),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.activity_main_frame),
                                                                isDescendantOfA(withId(R.id.activity_main))))))));
        android_widget_TextView2.perform(getClickAction());

        ViewInteraction android_widget_ImageButton2 =
                onView(
                        allOf(
                                withContentDescription(equalToTrimmingAndIgnoringCase("Navigate up")),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.toolbar_tag_list),
                                                isDescendantOfA(withId(R.id.activity_tag_list))))));
        android_widget_ImageButton2.perform(getClickAction());

        Espresso.pressBackUnconditionally();

        ViewInteraction android_widget_ImageView2 =
                onView(
                        allOf(
                                withContentDescription(equalToTrimmingAndIgnoringCase("More options")),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.toolbar),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.activity_main_frame),
                                                                isDescendantOfA(withId(R.id.activity_main))))))));
        android_widget_ImageView2.perform(getClickAction());

        ViewInteraction android_widget_LinearLayout2 =
                onView(
                        allOf(
                                classOrSuperClassesName(is("android.widget.LinearLayout")),
                                isVisible(),
                                hasDescendant(
                                        allOf(
                                                withId(R.id.content),
                                                hasDescendant(
                                                        allOf(
                                                                withId(R.id.title),
                                                                withTextOrHint(
                                                                        equalToTrimmingAndIgnoringCase("Export notebook"))))))));
        android_widget_LinearLayout2.perform(getClickAction());

        ViewInteraction root4 = onView(isRoot());
        root4.perform(getSwipeAction(540, 897, 540, 1794));

        waitToScrollEnd();

        ViewInteraction root5 = onView(isRoot());
        root5.perform(getSwipeAction(540, 897, 540, 1794));

        waitToScrollEnd();

        ViewInteraction android_widget_ImageButton3 =
                onView(
                        allOf(
                                withId(R.id.fab_button),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.coordinator_overview),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.overview_fragment),
                                                                isDescendantOfA(
                                                                        allOf(
                                                                                withId(R.id.activity_main_frame),
                                                                                isDescendantOfA(withId(R.id.activity_main))))))))));
        android_widget_ImageButton3.perform(getClickAction());

        Espresso.pressBackUnconditionally();

        ViewInteraction android_widget_EditText2 =
                onView(
                        allOf(
                                withId(R.id.search_src_text),
                                withTextOrHint(equalToTrimmingAndIgnoringCase("   Note summary")),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.search_plate),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.search_edit_frame),
                                                                isDescendantOfA(
                                                                        allOf(
                                                                                withId(R.id.search_bar),
                                                                                isDescendantOfA(
                                                                                        allOf(
                                                                                                withId(R.id.action_search),
                                                                                                isDescendantOfA(
                                                                                                        allOf(
                                                                                                                withId(R.id.toolbar),
                                                                                                                isDescendantOfA(
                                                                                                                        allOf(
                                                                                                                                withId(R.id.activity_main_frame),
                                                                                                                                isDescendantOfA(
                                                                                                                                        withId(
                                                                                                                                                R.id
                                                                                                                                                        .activity_main))))))))))))))));
        android_widget_EditText2.perform(replaceText("florescent"));

        ViewInteraction android_widget_TextView3 =
                onView(
                        allOf(
                                withId(R.id.tag_list),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.toolbar),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.activity_main_frame),
                                                                isDescendantOfA(withId(R.id.activity_main))))))));
        android_widget_TextView3.perform(getClickAction());

        ViewInteraction android_widget_ImageButton4 =
                onView(
                        allOf(
                                withContentDescription(equalToTrimmingAndIgnoringCase("Navigate up")),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.toolbar_tag_list),
                                                isDescendantOfA(withId(R.id.activity_tag_list))))));
        android_widget_ImageButton4.perform(getClickAction());

        ViewInteraction android_widget_ImageView3 =
                onView(
                        allOf(
                                withContentDescription(equalToTrimmingAndIgnoringCase("More options")),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.toolbar),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.activity_main_frame),
                                                                isDescendantOfA(withId(R.id.activity_main))))))));
        android_widget_ImageView3.perform(getClickAction());

        ViewInteraction android_widget_LinearLayout3 =
                onView(
                        allOf(
                                classOrSuperClassesName(is("android.widget.LinearLayout")),
                                isVisible(),
                                hasDescendant(
                                        allOf(
                                                withId(R.id.content),
                                                hasDescendant(
                                                        allOf(
                                                                withId(R.id.title),
                                                                withTextOrHint(
                                                                        equalToTrimmingAndIgnoringCase("New Notebook"))))))));
        android_widget_LinearLayout3.perform(getClickAction());

        Espresso.pressBackUnconditionally();

        ViewInteraction android_widget_ImageView4 =
                onView(
                        allOf(
                                withId(R.id.search_close_btn),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.search_plate),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.search_edit_frame),
                                                                isDescendantOfA(
                                                                        allOf(
                                                                                withId(R.id.search_bar),
                                                                                isDescendantOfA(
                                                                                        allOf(
                                                                                                withId(R.id.action_search),
                                                                                                isDescendantOfA(
                                                                                                        allOf(
                                                                                                                withId(R.id.toolbar),
                                                                                                                isDescendantOfA(
                                                                                                                        allOf(
                                                                                                                                withId(R.id.activity_main_frame),
                                                                                                                                isDescendantOfA(
                                                                                                                                        withId(
                                                                                                                                                R.id
                                                                                                                                                        .activity_main))))))))))))))));
        android_widget_ImageView4.perform(getClickAction());

        ViewInteraction android_widget_ImageButton5 =
                onView(
                        allOf(
                                withContentDescription(equalToTrimmingAndIgnoringCase("Collapse")),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.toolbar),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.activity_main_frame),
                                                                isDescendantOfA(withId(R.id.activity_main))))))));
        android_widget_ImageButton5.perform(getClickAction());

        ViewInteraction android_widget_ImageView5 =
                onView(
                        allOf(
                                withContentDescription(equalToTrimmingAndIgnoringCase("More options")),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.toolbar),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.activity_main_frame),
                                                                isDescendantOfA(withId(R.id.activity_main))))))));
        android_widget_ImageView5.perform(getLongClickAction());

        ViewInteraction android_widget_ImageView6 =
                onView(
                        allOf(
                                withContentDescription(equalToTrimmingAndIgnoringCase("More options")),
                                isVisible(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.toolbar),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.activity_main_frame),
                                                                isDescendantOfA(withId(R.id.activity_main))))))));
        android_widget_ImageView6.perform(getClickAction());

        ViewInteraction android_widget_LinearLayout4 =
                onView(
                        allOf(
                                classOrSuperClassesName(is("android.widget.LinearLayout")),
                                isVisible(),
                                hasDescendant(
                                        allOf(
                                                withId(R.id.content),
                                                hasDescendant(
                                                        allOf(
                                                                withId(R.id.title),
                                                                withTextOrHint(
                                                                        equalToTrimmingAndIgnoringCase("Import notebook"))))))));
        android_widget_LinearLayout4.perform(getClickAction());
    }

    private static Matcher<View> classOrSuperClassesName(final Matcher<String> classNameMatcher) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Class name or any super class name ");
                classNameMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                Class<?> clazz = view.getClass();
                String canonicalName;

                do {
                    canonicalName = clazz.getCanonicalName();
                    if (canonicalName == null) {
                        return false;
                    }

                    if (classNameMatcher.matches(canonicalName)) {
                        return true;
                    }

                    clazz = clazz.getSuperclass();
                    if (clazz == null) {
                        return false;
                    }
                } while (!"java.lang.Object".equals(canonicalName));

                return false;
            }
        };
    }

    private static Matcher<View> withTextOrHint(final Matcher<String> stringMatcher) {
        return anyOf(withText(stringMatcher), withHint(stringMatcher));
    }

    private ViewAction getSwipeAction(
            final int fromX, final int fromY, final int toX, final int toY) {
        return ViewActions.actionWithAssertions(
                new GeneralSwipeAction(
                        Swipe.SLOW,
                        new CoordinatesProvider() {
                            @Override
                            public float[] calculateCoordinates(View view) {
                                float[] coordinates = {fromX, fromY};
                                return coordinates;
                            }
                        },
                        new CoordinatesProvider() {
                            @Override
                            public float[] calculateCoordinates(View view) {
                                float[] coordinates = {toX, toY};
                                return coordinates;
                            }
                        },
                        Press.FINGER));
    }

    private void waitToScrollEnd() {
        SystemClock.sleep(500);
    }

    private ClickWithoutDisplayConstraint getClickAction() {
        return new ClickWithoutDisplayConstraint(
                Tap.SINGLE,
                GeneralLocation.VISIBLE_CENTER,
                Press.FINGER,
                InputDevice.SOURCE_UNKNOWN,
                MotionEvent.BUTTON_PRIMARY);
    }

    private ClickWithoutDisplayConstraint getLongClickAction() {
        return new ClickWithoutDisplayConstraint(
                Tap.LONG,
                GeneralLocation.CENTER,
                Press.FINGER,
                InputDevice.SOURCE_UNKNOWN,
                MotionEvent.BUTTON_PRIMARY);
    }
}
