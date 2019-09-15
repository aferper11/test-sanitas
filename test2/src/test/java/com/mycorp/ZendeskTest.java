package com.mycorp;

import org.junit.Test;

import junit.framework.TestCase;

public class ZendeskTest extends TestCase {

	@Test
	public void testNull() {
		String URL_ZENDESK = "http://localhost:80/zendesk";
		String ZENDESK_USER = "user";
		String TOKEN_ZENDESK = "token";
		Zendesk zendesk = new Zendesk.Builder(URL_ZENDESK).setUsername(ZENDESK_USER).setToken(TOKEN_ZENDESK).build();

		try {
			zendesk.createTicket(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}