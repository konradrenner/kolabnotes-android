package org.kore.kolabnotes.android;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.MenuItem;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onData;

import static android.support.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;

/**
 * Test Main Activity
 */
@RunWith(AndroidJUnit4.class)
public class TestSearchViewUI {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    /**
     * Method to use the custom matcher
     * @param title title of the menu item
     * @return      return the matcher for the item
     */
    static MenuItemTitleMatcher withTitle(String title) {
        return new MenuItemTitleMatcher(title);
    }

    /**
     * Perform the search view clicked UI test
     */
    @Test
    public void testSearchMenuItemClick() {
        openContextualActionModeOverflowMenu();
        final String searchViewTitle = mActivityRule.getActivity().getString(
                R.string.title_dialog_search);
        onData(allOf(instanceOf(MenuItem.class), withTitle(searchViewTitle))).perform(click());
    }
}


/**
 * On View not working with the view inside adapter view need to create this Matcher in order to use
 * with on Data
 *
 * @see <a href="http://stackoverflow.com/questions/25408530/android-espresso-long-options-menu-clicking-on-a-option-menu-item-that-is">
 *          StackOverFlow answer
 *     </a>
 */
class MenuItemTitleMatcher extends BaseMatcher<Object> {
    private final String title;
    public MenuItemTitleMatcher(String title) { this.title = title; }

    @Override public boolean matches(Object o) {
        if (o instanceof MenuItem) {
            return ((MenuItem) o).getTitle().equals(title);
        }
        return false;
    }
    @Override public void describeTo(Description description) { }
}