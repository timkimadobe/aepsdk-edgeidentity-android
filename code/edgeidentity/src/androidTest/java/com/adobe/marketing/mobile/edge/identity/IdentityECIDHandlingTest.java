/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.identity;

import static com.adobe.marketing.mobile.edge.identity.util.IdentityFunctionalTestUtil.*;
import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.edge.identity.util.TestHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IdentityECIDHandlingTest {

	@Rule
	public RuleChain rule = RuleChain
		.outerRule(new TestHelper.SetupCoreRule())
		.around(new TestHelper.RegisterMonitorExtensionRule());

	@Test
	public void testECID_autoGeneratedWhenBooted() throws InterruptedException {
		// setup
		registerEdgeIdentityExtension();

		// verify ECID is not null
		verifyPrimaryECIDNotNull();
	}

	@Test
	public void testECID_loadedFromPersistence() throws Exception {
		// setup
		setEdgeIdentityPersistence(
			createXDMIdentityMap(new TestItem("ECID", "primaryECID"), new TestItem("ECID", "secondaryECID"))
		);
		registerEdgeIdentityExtension();

		// verify
		verifyPrimaryECID("primaryECID");
		verifySecondaryECID("secondaryECID");
	}

	@Test
	public void testECID_edgePersistenceTakesPreferenceOverDirectExtension() throws Exception {
		// setup
		setIdentityDirectPersistedECID("legacyECID");
		setEdgeIdentityPersistence(createIdentityMap("ECID", "edgeECID").asXDMMap());
		registerEdgeIdentityExtension();

		// verify
		verifyPrimaryECID("edgeECID");
		verifySecondaryECID(null);
	}

	@Test
	public void testECID_loadsIdentityDirectECID() throws Exception {
		// This will happen when EdgeIdentity extension is installed after Identity direct extension
		// setup
		setIdentityDirectPersistedECID("legacyECID");
		registerEdgeIdentityExtension();

		// verify
		verifyPrimaryECID("legacyECID");
	}

	@Test
	public void testECID_whenBothExtensionRegistered_install() throws Exception {
		// setup
		registerBothIdentityExtensions(); // no ECID exists before this step

		String directECID = getIdentityDirectECIDSync();
		String edgeECID = getExperienceCloudIdSync();

		// verify ECID
		verifyPrimaryECID(directECID);
		verifySecondaryECID(null);
		assertEquals(directECID, edgeECID);
	}

	@Test
	public void testECID_whenBothExtensionRegistered_migrationPath() throws Exception {
		// setup
		String existingECID = "legacyECID";
		setIdentityDirectPersistedECID(existingECID);
		registerBothIdentityExtensions();

		String directECID = getIdentityDirectECIDSync();
		String edgeECID = getExperienceCloudIdSync();

		// verify ECID
		verifyPrimaryECID(directECID);
		verifySecondaryECID(null);
		assertEquals(directECID, edgeECID);
		assertEquals(existingECID, edgeECID);
	}

	@Test
	public void testECID_onResetClearsOldECID() throws Exception {
		// setup
		setEdgeIdentityPersistence(
			createXDMIdentityMap(new TestItem("ECID", "primaryECID"), new TestItem("ECID", "secondaryECID"))
		);
		registerEdgeIdentityExtension();

		// test
		MobileCore.resetIdentities();
		String newECID = getExperienceCloudIdSync();

		// verify the new ecid is not the same as old one and the secondary ECID is cleared
		assertNotNull(newECID);
		assertNotEquals("primaryECID", newECID);
		verifyPrimaryECID(newECID);
		verifySecondaryECID(null);
	}

	@Test
	public void testECID_AreDifferentAfterPrivacyChange() throws Exception {
		/// Test Edge Identity and IdentityDirect have same ECID on bootup, and after privacy change ECIDs are different
		setIdentityDirectPersistedECID("legacyECID");
		registerBothIdentityExtensions();
		TestHelper.waitForThreads(2000);

		// verify ECID for both extensions are same
		String directECID = getIdentityDirectECIDSync();
		String edgeECID = getExperienceCloudIdSync();
		assertEquals("legacyECID", edgeECID);
		assertEquals(directECID, edgeECID);

		//  Toggle privacy
		togglePrivacyStatus();
		directECID = getIdentityDirectECIDSync();

		// verify legacy ECID added to IdentityMap
		verifyPrimaryECID(edgeECID);
		verifySecondaryECID(directECID);
	}

	@Test
	public void testECID_AreDifferentAfterResetIdentitiesAndPrivacyChange() throws Exception {
		/// Test Edge Identity and IdentityDirect have same ECID on bootup, and after resetIdentities and privacy change ECIDs are different

		// 1) Register Identity then Edge Identity and verify both have same ECID
		setIdentityDirectPersistedECID("legacyECID");
		registerBothIdentityExtensions();
		TestHelper.waitForThreads(2000);

		// verify ECID for both extensions are same
		String directECID = getIdentityDirectECIDSync();
		String edgeECID = getExperienceCloudIdSync();
		assertEquals("legacyECID", edgeECID);
		assertEquals(directECID, edgeECID);

		// 2) Reset identities and toggle privacy and verify legacy ECID added to IdentityMap
		MobileCore.resetIdentities();
		togglePrivacyStatus();

		// verify
		directECID = getIdentityDirectECIDSync();
		edgeECID = getExperienceCloudIdSync();
		verifyPrimaryECID(edgeECID);
		verifySecondaryECID(directECID);
	}

	@Test
	public void testECID_DirectEcidIsRemovedOnPrivacyOptOut() throws Exception {
		// setup
		setIdentityDirectPersistedECID("legacyECID");
		setEdgeIdentityPersistence(createIdentityMap("ECID", "edgeECID").asXDMMap());
		registerBothIdentityExtensions();

		// verify ECID
		verifyPrimaryECID("edgeECID");
		verifySecondaryECID("legacyECID");

		// Set privacy opted-out
		MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_OUT);
		TestHelper.waitForThreads(2000);

		// verify that the secondary ECID is removed
		verifyPrimaryECID("edgeECID");
		verifySecondaryECID(null);
	}

	// --------------------------------------------------------------------------------------------
	// private helpers methods
	// --------------------------------------------------------------------------------------------

	private void togglePrivacyStatus() {
		MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_OUT);
		MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_IN);
		TestHelper.waitForThreads(2000);
	}
}
