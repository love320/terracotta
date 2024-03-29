/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.wicket;

import java.util.Collections;
import java.util.Date;

import junit.framework.Test;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WicketServlet;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.TIMUtil;
import com.tc.util.runtime.Vm;

/**
 * An in-container smoke test for Wicket framework
 * 
 * @author Eugene Kuleshov
 */
public class ContainerWicketTest extends AbstractTwoServerDeploymentTest {
	private static final String CONFIG_FILE_FOR_TEST = "/tc-config-files/wicket-tc-config.xml";

	public static Test suite() {
		return new ContainerWicketTestSetup();
	}

	public ContainerWicketTest() {
		if (Vm.isIBM()) {
			this.disableAllUntil(new Date(Long.MAX_VALUE));
		}
	}

	public boolean shouldDisable() {
		return super.shouldDisable() || Vm.isIBM();
	}

	public void testWicketInitialization() throws Exception {

		WebConversation webConversation1 = new WebConversation();

		WebResponse response1 = request(server0, webConversation1, "");
		WebLink[] links1 = response1.getLinks();
		assertEquals(1, links1.length);
		assertEquals("Action link clicked 0 times", links1[0].getText());

		WebResponse response2 = request(server0, webConversation1, links1[0]
				.getURLString());
		WebLink[] links2 = response2.getLinks();
		assertEquals(1, links2.length);
		assertEquals("Action link clicked 1 times", links2[0].getText());

		WebResponse response3 = request(server1, webConversation1, "?"
				+ response2.getURL().getQuery());
		WebLink[] links3 = response3.getLinks();
		assertEquals(1, links3.length);
		assertEquals("Action link clicked 1 times", links3[0].getText());

		WebResponse response4 = request(server1, webConversation1, links3[0]
				.getURLString());
		WebLink[] links4 = response4.getLinks();
		assertEquals(1, links4.length);
		assertEquals("Action link clicked 2 times", links4[0].getText());

		WebResponse response5 = request(server0, webConversation1, "?"
				+ response4.getURL().getQuery());
		WebLink[] links5 = response5.getLinks();
		assertEquals(1, links5.length);
		assertEquals("Action link clicked 2 times", links5[0].getText());

	}

	private WebResponse request(WebApplicationServer server,
			WebConversation conversation, String params) throws Exception {
		return server.ping("/wicket-test/clickcounter/" + params, conversation);
	}

	private static class ContainerWicketTestSetup extends TwoServerTestSetup {
		private ContainerWicketTestSetup() {
			super(ContainerWicketTest.class, CONFIG_FILE_FOR_TEST,
					"wicket-test");
		}

		protected void configureWar(DeploymentBuilder builder) {
			// builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
			// builder.addRemoteService(REMOTE_SERVICE_NAME, "singleton",
			// ISingleton.class);

			builder.addDirectoryOrJARContainingClass(Page.class); // wicket*.jar
			builder.addDirectoryOrJARContainingClass(Logger.class); // log4j
			builder.addDirectoryOrJARContainingClass(LogFactory.class); // common-loggings
			builder
					.addDirectoryContainingResource("/com/tctest/wicket/ContainerWicketTest$ClickCounter.html");

			builder.addServlet("ClickCounter", "/clickcounter/*",
					WicketServlet.class, Collections.singletonMap(
							"applicationClassName", ClickCounterApp.class
									.getName()), true);
		}

		protected void configureTcConfig(TcConfigBuilder clientConfig) {
			clientConfig.addModule(TIMUtil.WICKET_1_3, TIMUtil
					.getVersion(TIMUtil.WEBSPHERE_6_1_0_7));
		}

	}

	public static final class ClickCounterApp extends WebApplication {
		public Class getHomePage() {
			return ClickCounter.class;
		}
	}

	public static class ClickCounter extends WebPage {
		private static final long serialVersionUID = 1L;

		private int linkClickCount;

		public ClickCounter() {
			final Link actionLink = new Link("actionLink") {
				private static final long serialVersionUID = 1L;

				public void onClick() {
					linkClickCount++;
				}
			};
			actionLink.add(new Label("linkClickCount", new PropertyModel(this,
					"linkClickCount")));
			add(actionLink);
		}
	}

}
