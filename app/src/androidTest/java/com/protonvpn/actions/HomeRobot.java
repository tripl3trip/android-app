/*
 * Copyright (c) 2018 Proton Technologies AG
 *
 * This file is part of ProtonVPN.
 *
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.protonvpn.actions;

import com.protonvpn.MockSwitch;
import com.protonvpn.android.R;
import com.protonvpn.results.ConnectionResult;
import com.protonvpn.results.HomeResult;
import com.protonvpn.results.LogoutResult;
import com.protonvpn.results.ProfilesResult;
import com.protonvpn.testsHelper.ServiceTestHelper;
import com.protonvpn.testsHelper.UIActionsTestHelper;

import androidx.annotation.NonNull;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static com.protonvpn.testsHelper.UICustomViewActions.waitObjectWithId;
import static org.hamcrest.Matchers.endsWith;

public class HomeRobot extends UIActionsTestHelper {

    ServiceTestHelper serviceTestHelper;

    public HomeRobot(){
        if(MockSwitch.mockedConnectionUsed){
            serviceTestHelper = new ServiceTestHelper();
        }
    }

    public HomeRobot disableSecureCore() {
        setStateOfSecureCoreSwitch(false);
        return this;
    }

    public void openDrawer() {
        if (!isDrawerOpened()) {
            clickOnObjectWithContentDescription(R.string.hamburgerMenu);
        }
    }

    public boolean isDrawerOpened() {
        return isObjectWithIdVisible(R.id.navigationDrawer);
    }

    public SettingsRobot openSettings() {
        clickOnObjectWithContentDescription(R.string.hamburgerMenu);
        clickOnObjectWithText("Settings");
        return new SettingsRobot();
    }

    public CountriesRobot clickOnCountriesTab() {
        clickOnObjectChildWithinChildWithIdAndPosition(R.id.tabs, 0, 0);
        return new CountriesRobot();
    }

    public MapRobot clickOnMapViewTab() {
        clickOnObjectChildWithinChildWithIdAndPosition(R.id.tabs, 0, 1);
        return new MapRobot();
    }

    public ProfilesResult clickOnProfilesTab() {
        clickOnObjectChildWithinChildWithIdAndPosition(R.id.tabs, 0, 2);
        return new ProfilesResult();
    }

    public ConnectionResult connectThroughQuickConnect() {
        return connectThroughQuickConnect("Fastest");
    }

    public ConnectionResult connectThroughQuickConnect(@NonNull String profileName) {
        // The last "FloatingActionButton" is the main one.
        longClickOnLastChildWithId(R.id.fabQuickConnect, withClassName(endsWith("FloatingActionButton")));
        clickOnObjectWithText(profileName);
        if (!MockSwitch.mockedConnectionUsed) {
            allowToUseVpn();
        }
        return new ConnectionResult();
    }

    public ConnectionResult connectThroughQuickConnectWithoutVPNHandling() {
        longClickOnLastChildWithId(R.id.fabQuickConnect, withClassName(endsWith("FloatingActionButton")));
        handleQuickConnectLongClick();
        return new ConnectionResult();
    }

    public ConnectionResult disconnectThroughQuickConnect() {
        clickOnLastChildWithId(R.id.fabQuickConnect, withClassName(endsWith("FloatingActionButton")));
        return new ConnectionResult();
    }

    public void allowToUseVpn() {
        allowVpnToBeUsed(isAllowVpnRequestVisible());
    }

    public HomeRobot handleQuickConnectLongClick() {
        clickOnRandomButtonFromQuickConnectMenu(isLongClickOnQuickConnect());
        return this;
    }

    public LogoutResult logout() {
        openDrawer();
        clickOnObjectWithIdAndText(R.id.drawerButtonLogout, R.string.menuActionSignOut);
        return new LogoutResult();
    }

    public LogoutResult logoutAfterWarning() {
        clickOnObjectWithText(R.string.logoutConfirmDialogButton);
        return new LogoutResult();
    }

    public LogoutResult cancelLogoutAfterWarning() {
        clickOnObjectWithText(R.string.cancel);
        return new LogoutResult();
    }

    public HomeResult enableSecureCore() {
        setStateOfSecureCoreSwitch(true);
        return new HomeResult();
    }

    public HomeResult clickButtonGotIt() {
        clickOnObjectWithId(R.id.buttonGotIt);
        return new HomeResult();
    }

    public HomeResult clickButtonCancel() {
        clickOnObjectWithId(R.id.md_buttonDefaultNegative);
        return new HomeResult();
    }

    public HomeResult clickButtonUpgrade() {
        clickOnObjectWithId(R.id.md_buttonDefaultPositive);
        return new HomeResult();
    }

    protected void setStateOfSecureCoreSwitch(boolean state) {
        onView(isRoot()).perform(waitObjectWithId(R.id.switchSecureCore));
        if (state != serviceTestHelper.isSecureCoreEnabled()) {
            clickOnObjectWithId(R.id.switchSecureCore);
        }
    }
}
