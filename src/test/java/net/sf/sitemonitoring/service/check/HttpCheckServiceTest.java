package net.sf.sitemonitoring.service.check;

import com.google.common.eventbus.EventBus;
import net.sf.sitemonitoring.entity.Check;
import net.sf.sitemonitoring.entity.Check.CheckCondition;
import net.sf.sitemonitoring.entity.Check.CheckType;
import net.sf.sitemonitoring.entity.Check.HttpMethod;
import net.sf.sitemonitoring.entity.Credentials;
import net.sf.sitemonitoring.service.check.util.PagingServlet;
import net.sf.sitemonitoring.service.check.util.ProxyServerUtil;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.littleshoot.proxy.HttpProxyServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * This class tests all checks, which perform http requests
 * 
 * @author pinkas
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class HttpCheckServiceTest {

	private SinglePageCheckService singlePageCheckService;

	private SitemapCheckThread sitemapCheckThread;

	private SpiderCheckThread spiderCheckThread;

	public static String TEST_JETTY_HTTP = "http://localhost:8081/";

	private static final int timeout = 10000;


	@Before
	public void before() {
		singlePageCheckService = new SinglePageCheckService();
		singlePageCheckService.setEventBus(new EventBus());
		sitemapCheckThread = new SitemapCheckThread(singlePageCheckService, null);
		// created in AbstractCheckThread.run(), that's why I have to create it here.
		sitemapCheckThread.httpClient = HttpClients.createDefault();
		spiderCheckThread = new SpiderCheckThread(singlePageCheckService, null);
		spiderCheckThread.httpClient = HttpClients.createDefault();
	}

	@After
	public void after() throws IOException {
		sitemapCheckThread.httpClient.close();
		spiderCheckThread.httpClient.close();
	}

	@Test
	public void testPerformCheckSinglePageContains() throws Exception {
		Check check = new Check();
		check.setCondition("</html>");
		check.setReturnHttpCode(200);
		check.setType(CheckType.SINGLE_PAGE);
		check.setConditionType(CheckCondition.CONTAINS);
		check.setUrl(TEST_JETTY_HTTP + "index.html");
		check.setCheckBrokenLinks(false);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		check.setHttpMethod(HttpMethod.GET);

		assertNull(singlePageCheckService.performCheck(check));
	}

	@Test
	public void testPerformCheckSinglePageWeirdBrokenLink() throws Exception {
		Check check = new Check();
		check.setCondition("</html>");
		check.setReturnHttpCode(200);
		check.setType(CheckType.SINGLE_PAGE);
		check.setConditionType(CheckCondition.CONTAINS);
		check.setUrl(TEST_JETTY_HTTP + "contains-weird-broken-link.html");
		check.setCheckBrokenLinks(true);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		check.setHttpMethod(HttpMethod.GET);

		assertEquals("http://localhost:8081/contains-weird-broken-link.html has error: java.net.URISyntaxException: Illegal character in fragment at index 19: http://github.com/#{github}",
				singlePageCheckService.performCheck(check));
	}

	@Test
	public void testPerformCheckSinglePageContainsWithProxy() throws Exception {
		HttpProxyServer proxyServer = ProxyServerUtil.start();
		Check check = new Check();
		check.setCondition("</html>");
		check.setReturnHttpCode(200);
		check.setType(CheckType.SINGLE_PAGE);
		check.setConditionType(CheckCondition.CONTAINS);
		check.setUrl(TEST_JETTY_HTTP + "index.html");
		check.setCheckBrokenLinks(false);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		check.setHttpProxyServer("localhost");
		check.setHttpProxyPort(8089);
		check.setHttpProxyUsername("test");
		check.setHttpProxyPassword("works");
		check.setHttpMethod(HttpMethod.GET);

		assertNull(singlePageCheckService.performCheck(check));
		ProxyServerUtil.stop(proxyServer);
	}

	@Test
	public void testPerformCheckSinglePageDoNotFollow() throws Exception {
		Check check = new Check();
		check.setType(CheckType.SINGLE_PAGE);
		check.setReturnHttpCode(200);
		check.setConditionType(CheckCondition.CONTAINS);
		check.setCondition("</html>");
		check.setDoNotFollowUrls("*do-not-follow*\r\n*twitter.com");
		check.setUrl(TEST_JETTY_HTTP + "test-do-not-follow.html");
		check.setCheckBrokenLinks(true);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		check.setHttpMethod(HttpMethod.GET);

		assertNull(singlePageCheckService.performCheck(check));
	}

	@Test
	public void testPerformCheckSitemapPageDoNotFollow() throws Exception {
		Check check = new Check();
		check.setType(CheckType.SITEMAP);
		check.setReturnHttpCode(200);
		check.setConditionType(CheckCondition.CONTAINS);
		check.setCondition("</html>");
		check.setDoNotFollowUrls("*do-not-follow*\r\n*twitter.com");
		check.setExcludedUrls("*pdf");
		check.setUrl(TEST_JETTY_HTTP + "local-sitemap.xml");
		check.setCheckBrokenLinks(true);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		check.setHttpMethod(HttpMethod.GET);

		sitemapCheckThread.check = check;
		sitemapCheckThread.performCheck();
		assertNull(sitemapCheckThread.output);
	}

	@Test
	public void testPerformCheckSinglePageDoesntContain() throws Exception {
		Check check = new Check();
		check.setCondition("not there");
		check.setReturnHttpCode(200);
		check.setType(CheckType.SINGLE_PAGE);
		check.setConditionType(CheckCondition.DOESNT_CONTAIN);
		check.setUrl(TEST_JETTY_HTTP + "index.html");
		check.setCheckBrokenLinks(false);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		check.setHttpMethod(HttpMethod.GET);

		assertNull(singlePageCheckService.performCheck(check));
	}

	@Test
	public void testPerformCheckSinglePageUnexpectedDoesntExist() throws Exception {
		Check check = new Check();
		check.setReturnHttpCode(200);
		check.setType(CheckType.SINGLE_PAGE);
		check.setUrl(TEST_JETTY_HTTP + "not-exists.html");
		check.setCheckBrokenLinks(false);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		check.setHttpMethod(HttpMethod.GET);

		assertEquals("Invalid status: http://localhost:8081/not-exists.html required: 200, received: 500 ", singlePageCheckService.performCheck(check));
	}

	@Test
	public void testPerformCheckSinglePageExpectedDoesntExist() throws Exception {
		Check check = new Check();
		check.setReturnHttpCode(500);
		check.setType(CheckType.SINGLE_PAGE);
		check.setUrl(TEST_JETTY_HTTP + "not-exists.html");
		check.setCheckBrokenLinks(false);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		check.setHttpMethod(HttpMethod.HEAD);

		assertNull(singlePageCheckService.performCheck(check));
	}

	@Test
	public void testPerformCheckBadUrl() throws Exception {
		Check check = new Check();
		check.setReturnHttpCode(404);
		check.setType(CheckType.SINGLE_PAGE);
		check.setUrl("http://");
		check.setCheckBrokenLinks(false);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		check.setHttpMethod(HttpMethod.HEAD);

		assertEquals("http:// has error: incorrect URL", singlePageCheckService.performCheck(check));
	}

	@Test
	public void testPerformCheckPdfExists() throws Exception {
		Check check = new Check();
		check.setReturnHttpCode(200);
		check.setType(CheckType.SINGLE_PAGE);
		check.setUrl(TEST_JETTY_HTTP + "test.pdf");
		check.setCheckBrokenLinks(false);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		check.setHttpMethod(HttpMethod.GET);

		assertNull(singlePageCheckService.performCheck(check));
	}

	@Test
	public void testDownloadSitemap() throws Exception {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClients.createDefault();
			String sitemapXml = sitemapCheckThread.downloadSitemap(httpClient, TEST_JETTY_HTTP + "sitemap.xml");
			assertTrue(sitemapXml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
		} finally {
			if (httpClient != null) {
				httpClient.close();
			}
		}
	}

	@Test
	public void testCheckSitemap() throws Exception {
		Check check = new Check();
		check.setCondition("</html>");
		check.setReturnHttpCode(200);
		check.setType(CheckType.SITEMAP);
		check.setConditionType(CheckCondition.CONTAINS);
		check.setUrl(TEST_JETTY_HTTP + "sitemap.xml");
		check.setExcludedUrls("*html\r\nhttp://www.sqlvids.com/\r\n*pdf");
		check.setCheckBrokenLinks(false);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);

		sitemapCheckThread.check = check;
		sitemapCheckThread.performCheck();
		assertNull(sitemapCheckThread.output);
	}

	@Test(expected = IOException.class)
	public void testDownloadSitemapError() throws Exception {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClients.createDefault();
			sitemapCheckThread.downloadSitemap(httpClient, TEST_JETTY_HTTP + "sitemap.notexists.xml");
		} finally {
			if (httpClient != null) {
				httpClient.close();
			}
		}
	}

	@Test
	public void testSpiderWithoutBrokenLinks() {
		Check check = new Check();
		check.setReturnHttpCode(200);
		check.setType(CheckType.SPIDER);
		// note: URL must be base URL (directory!), not some web page
		check.setUrl(TEST_JETTY_HTTP + "spider/");
		check.setCheckBrokenLinks(false);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		spiderCheckThread.check = check;
		spiderCheckThread.performCheck();
		assertEquals(
				"http://localhost:8081/spider/ has error: Invalid status: http://localhost:8081/spider/broken-link.html required: 200, received: 500 <br />http://localhost:8081/spider/contains-broken-links.html has error: Invalid status: http://localhost:8081/spider/doesnt-exist required: 200, received: 500 <br />http://localhost:8081/spider/page?id=9 has error: Invalid status: http://localhost:8081/spider/not-found.html required: 200, received: 500 <br />",
				spiderCheckThread.output);

	}

	@Test
	public void testSpiderWithBrokenLinks() {
		Check check = new Check();
		check.setReturnHttpCode(200);
		check.setType(CheckType.SPIDER);
		check.setUrl(TEST_JETTY_HTTP + "spider/");
		check.setCheckBrokenLinks(true);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		spiderCheckThread.check = check;
		spiderCheckThread.performCheck();
		assertEquals(
				"http://localhost:8081/spider/ has error: Invalid status: http://localhost:8081/spider/broken-link.html required: 200, received: 500 <br />http://localhost:8081/spider/ has error: http://localhost:8081/spider/contains-broken-links.html has error: Invalid status: http://localhost:8081/spider/doesnt-exist required: 200, received: 500 <br /><br />http://localhost:8081/spider/ has error: Invalid status: http://localhost:8081/spider/broken-link.html required: 200, received: 500 <br />http://localhost:8081/spider/contains-broken-links.html has error: Invalid status: http://localhost:8081/spider/doesnt-exist required: 200, received: 500 <br />http://localhost:8081/spider/page?id=8 has error: http://localhost:8081/spider/page?id=9 has error: Invalid status: http://localhost:8081/spider/not-found.html required: 200, received: 500 <br /><br />http://localhost:8081/spider/page?id=9 has error: Invalid status: http://localhost:8081/spider/not-found.html required: 200, received: 500 <br />",
				spiderCheckThread.output);
	}

	@Test
	public void testPerformCheckSitemapWithErrorsNoBrokenLinks() throws Exception {
		Check check = new Check();
		check.setType(CheckType.SITEMAP);
		check.setReturnHttpCode(200);
		check.setConditionType(CheckCondition.CONTAINS);
		check.setCondition("</html>");
		check.setExcludedUrls("*pdf");
		check.setUrl(TEST_JETTY_HTTP + "local-sitemap-with-errors.xml");
		check.setCheckBrokenLinks(false);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		check.setHttpMethod(HttpMethod.GET);

		sitemapCheckThread.check = check;
		sitemapCheckThread.performCheck();
		assertEquals("Invalid status: http://localhost:8081/doesnt-exist required: 200, received: 500 <br />", sitemapCheckThread.output);
	}

	@Test
	public void testPerformCheckSitemapWithErrorsAndBrokenLinks() throws Exception {
		Check check = new Check();
		check.setType(CheckType.SITEMAP);
		check.setReturnHttpCode(200);
		check.setConditionType(CheckCondition.CONTAINS);
		check.setCondition("</html>");
		check.setExcludedUrls("*pdf");
		check.setUrl(TEST_JETTY_HTTP + "local-sitemap-with-errors.xml");
		check.setCheckBrokenLinks(true);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		check.setHttpMethod(HttpMethod.GET);
		check.setFollowOutboundBrokenLinks(true);

		sitemapCheckThread.check = check;
		sitemapCheckThread.performCheck();
		assertEquals(
				"Invalid status: http://localhost:8081/doesnt-exist required: 200, received: 500 <br />http://localhost:8081/contains-broken-links.html has error: Invalid status: http://localhost:8081/doesnt-exist required: 200, received: 500 <br />http://localhost:8081/contains-broken-links.html has error: http://www.doesntexist93283893289292947987498.com/: Unknown host: www.doesntexist93283893289292947987498.com<br /><br />",
				sitemapCheckThread.output);
	}

	@Test
	public void testPerformCheckSitemapWithErrorsAndBrokenLinksDoNotFollowOutboundUnspecified() throws Exception {
		Check check = new Check();
		check.setType(CheckType.SITEMAP);
		check.setReturnHttpCode(200);
		check.setConditionType(CheckCondition.CONTAINS);
		check.setCondition("</html>");
		check.setExcludedUrls("*pdf");
		check.setUrl(TEST_JETTY_HTTP + "local-sitemap-with-errors.xml");
		check.setCheckBrokenLinks(true);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		check.setHttpMethod(HttpMethod.GET);
		check.setFollowOutboundBrokenLinks(null);

		sitemapCheckThread.check = check;
		sitemapCheckThread.performCheck();
		assertEquals(
				"Invalid status: http://localhost:8081/doesnt-exist required: 200, received: 500 <br />http://localhost:8081/contains-broken-links.html has error: Invalid status: http://localhost:8081/doesnt-exist required: 200, received: 500 <br /><br />",
				sitemapCheckThread.output);
	}

	@Test
	public void testPerformCheckSitemapWithErrorsAndBrokenLinksDoNotFollowOutboundFalse() throws Exception {
		Check check = new Check();
		check.setType(CheckType.SITEMAP);
		check.setReturnHttpCode(200);
		check.setConditionType(CheckCondition.CONTAINS);
		check.setCondition("</html>");
		check.setExcludedUrls("*pdf");
		check.setUrl(TEST_JETTY_HTTP + "local-sitemap-with-errors.xml");
		check.setCheckBrokenLinks(true);
		check.setSocketTimeout(timeout);
		check.setConnectionTimeout(timeout);
		check.setHttpMethod(HttpMethod.GET);
		check.setFollowOutboundBrokenLinks(false);

		sitemapCheckThread.check = check;
		sitemapCheckThread.performCheck();
		assertEquals(
				"Invalid status: http://localhost:8081/doesnt-exist required: 200, received: 500 <br />http://localhost:8081/contains-broken-links.html has error: Invalid status: http://localhost:8081/doesnt-exist required: 200, received: 500 <br /><br />",
				sitemapCheckThread.output);
	}

//	@Test
//	public void testSingleCheckBasicAuthentication() {
//		Check check = new Check();
//		check.setCondition("this is protected using BasicAuthenticationFilter.java");
//		check.setReturnHttpCode(200);
//		check.setType(CheckType.SINGLE_PAGE);
//		check.setConditionType(CheckCondition.CONTAINS);
//		check.setUrl(TEST_JETTY_HTTP + "security/basic.html");
//		check.setCheckBrokenLinks(false);
//		check.setSocketTimeout(timeout);
//		check.setConnectionTimeout(timeout);
//		check.setHttpMethod(HttpMethod.GET);
//
//		Credentials credentials = new Credentials();
//		credentials.setUsername("admin");
//		credentials.setPassword("admin");
//		check.setCredentials(credentials);
//
//		assertNull(singlePageCheckService.performCheck(check));
//	}
//
//	@Test
//	public void testSingleCheckBasicAuthenticationWithProxy() throws Exception {
//		HttpProxyServer proxyServer = ProxyServerUtil.start();
//		Check check = new Check();
//		check.setCondition("this is protected using BasicAuthenticationFilter.java");
//		check.setReturnHttpCode(200);
//		check.setType(CheckType.SINGLE_PAGE);
//		check.setConditionType(CheckCondition.CONTAINS);
//		check.setUrl(TEST_JETTY_HTTP + "security/basic.html");
//		check.setCheckBrokenLinks(false);
//		check.setSocketTimeout(timeout);
//		check.setConnectionTimeout(timeout);
//		check.setHttpProxyServer("localhost");
//		check.setHttpProxyPort(8089);
//		check.setHttpProxyUsername("test");
//		check.setHttpProxyPassword("works");
//		check.setHttpMethod(HttpMethod.GET);
//
//		Credentials credentials = new Credentials();
//		credentials.setUsername("admin");
//		credentials.setPassword("admin");
//		check.setCredentials(credentials);
//
//		assertNull(singlePageCheckService.performCheck(check));
//		ProxyServerUtil.stop(proxyServer);
//	}

}
