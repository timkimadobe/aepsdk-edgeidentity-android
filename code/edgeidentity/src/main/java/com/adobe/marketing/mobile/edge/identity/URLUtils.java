/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.identity;

import static com.adobe.marketing.mobile.edge.identity.IdentityConstants.LOG_TAG;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.StringUtils;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

class URLUtils {

	private static final String LOG_SOURCE = "URLUtils";

	/**
	 * Helper function to generate url variables in format acceptable by the AEP web SDKs
	 *
	 * @param ts timestamp {@link String} denoting time when url variables request was made
	 * @param ecid Experience Cloud identifier {@link String} generated by the SDK
	 * @param orgId Experience Cloud Org identifier {@link String} set in the configuration
	 * @return {@link String} formatted with the visitor id payload
	 */
	static String generateURLVariablesPayload(final String ts, final String ecid, final String orgId) {
		final StringBuilder urlFragment = new StringBuilder();

		// construct the adobe_mc string
		try {
			// append timestamp
			String theIdString = appendKVPToVisitorIdString(null, IdentityConstants.UrlKeys.TS, ts);

			// append ecid
			theIdString = appendKVPToVisitorIdString(theIdString, IdentityConstants.UrlKeys.EXPERIENCE_CLOUD_ID, ecid);

			// add Experience Cloud Org ID
			theIdString =
				appendKVPToVisitorIdString(theIdString, IdentityConstants.UrlKeys.EXPERIENCE_CLOUD_ORG_ID, orgId);

			// after the adobe_mc string is created, encode the idString before adding it to the url
			urlFragment.append(IdentityConstants.UrlKeys.PAYLOAD);
			urlFragment.append("=");

			if (StringUtils.isNullOrEmpty(theIdString)) {
				// No need to encode
				urlFragment.append("null");
			} else {
				urlFragment.append(URLEncoder.encode(theIdString, StandardCharsets.UTF_8.toString()));
			}
		} catch (UnsupportedEncodingException e) {
			Log.debug(LOG_TAG, LOG_SOURCE, String.format("Failed to encode urlVariable string: %s", e));
		}
		return urlFragment.toString();
	}

	/**
	 * Takes in a key-value pair and appends it to the source string
	 * <p>
	 * This method <b>does not</b> URL encode the provided {@code value} on the resulting string.
	 * If encoding is needed, make sure that the values are encoded before being passed into this function.
	 *
	 * @param originalString {@link String} to append the key and value to
	 * @param key key to append
	 * @param value value to append
	 *
	 * @return a new string with the key and value appended, or {@code originalString}
	 *         if {@code key} or {@code value} are null or empty
	 */
	static String appendKVPToVisitorIdString(final String originalString, final String key, final String value) {
		// quickly return original string if key or value are empty
		if (StringUtils.isNullOrEmpty(key) || StringUtils.isNullOrEmpty(value)) {
			return originalString;
		}

		// get the value for the new variable
		final String newUrlVariable = String.format("%s=%s", key, value);

		// if the original string is not empty, we need to append a pipe before we return
		if (StringUtils.isNullOrEmpty(originalString)) {
			return newUrlVariable;
		} else {
			return String.format("%s|%s", originalString, newUrlVariable);
		}
	}
}
