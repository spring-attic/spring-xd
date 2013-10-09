package org.springframework.integration.x.twitter;

import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.OAuth2Template;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.util.Assert;

/**
 * An extension of TwitterTemplate which can be configured with a consumerKey and consumerSecret and will use those as
 * the clientId and secret to obtain an OAuth2 bearer token.
 *
 * @author Luke Taylor
 */
public class OAuth2AuthenticatingTwitterTemplate extends TwitterTemplate {
	public OAuth2AuthenticatingTwitterTemplate(String consumerKey, String consumerSecret) {
		super(getBearerToken(consumerKey, consumerSecret));
	}

	private static String getBearerToken(String consumerKey, String consumerSecret) {
		Assert.hasText(consumerKey, "consumerKey must be supplied");
		Assert.hasText(consumerSecret, "consumerSecret must be supplied");
		OAuth2Template o2t = new OAuth2Template(consumerKey, consumerSecret, "http://notused", "http://notused",
				"https://api.twitter.com/oauth2/token");
		AccessGrant grant = o2t.authenticateClient();
		return grant.getAccessToken();
	}
}
