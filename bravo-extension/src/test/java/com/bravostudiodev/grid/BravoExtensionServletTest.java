package com.bravostudiodev.grid;

import com.google.gson.Gson;
import io.sterodium.rmi.protocol.MethodInvocationDto;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.seleniumhq.jetty9.server.AbstractNetworkConnector;
import org.seleniumhq.jetty9.server.Server;
import org.seleniumhq.jetty9.servlet.ServletContextHandler;
import org.seleniumhq.jetty9.servlet.ServletHolder;

import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.net.URLEncoder;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BravoExtensionServletTest {

    private Server sikuliServer;
    private HttpHost serverHost;
    private String basePath;

    @Before
    public void setUp() throws Exception {
        basePath = "/" + BravoExtensionServlet.class.getSimpleName() + "/";
        sikuliServer = startServerForServlet(new BravoExtensionServlet(), basePath + "*");

        serverHost = new HttpHost("localhost", ((AbstractNetworkConnector) sikuliServer.getConnectors()[0]).getLocalPort());
    }

    @After
    public void tearDown() throws Exception {
        sikuliServer.stop();
    }

    @Test
    public void shouldMakeSimpleRemoteInvocation() throws IOException {
        String invocationJsonString = invocationJsonString();

        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPost request = new HttpPost(basePath + "/" + URLEncoder.encode("screen", "UTF-8"));
        request.setEntity(new StringEntity(invocationJsonString));

        HttpResponse httpResponse = httpClient.execute(serverHost, request);

        int statusCode = httpResponse.getStatusLine().getStatusCode();

        assertThat(statusCode, is(HttpStatus.SC_OK));
        assertThat(EntityUtils.toString(httpResponse.getEntity()), is("{\"value\":\"true\",\"type\":\"java.lang.Boolean\"}"));
    }

    @Test
    public void shouldReturnErrorInformation() throws IOException {
        String invocationJsonString = failingInvocationJsonString();

        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPost request = new HttpPost(basePath + "/" + URLEncoder.encode("screen", "UTF-8"));
        request.setEntity(new StringEntity(invocationJsonString));

        HttpResponse httpResponse = httpClient.execute(serverHost, request);

        int statusCode = httpResponse.getStatusLine().getStatusCode();

        assertThat(statusCode, is(HttpStatus.SC_OK));
        assertThat(EntityUtils.toString(httpResponse.getEntity()), containsString("notExistingMethod not found"));
    }

    @Test
    public void shouldFailIfObjectIdNotPresent() throws IOException {
        String invocationJsonString = failingInvocationJsonString();


        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPost request = new HttpPost(basePath + "/");
        request.setEntity(new StringEntity(invocationJsonString));

        HttpResponse httpResponse = httpClient.execute(serverHost, request);

        int statusCode = httpResponse.getStatusLine().getStatusCode();

        assertThat(statusCode, is(HttpStatus.SC_BAD_REQUEST));
    }

    private String invocationJsonString() {
        String[] argClasses = {String.class.getName(), String.class.getName()};
        String[] arguments = {"targetName", "b64PNG"};
        MethodInvocationDto invocation = new MethodInvocationDto("addTarget", argClasses, arguments);

        return new Gson().toJson(invocation);
    }

    private String failingInvocationJsonString() {
        String[] argClasses = {String.class.getName()};
        String[] arguments = {"value"};
        MethodInvocationDto invocation = new MethodInvocationDto("notExistingMethod", argClasses, arguments);

        return new Gson().toJson(invocation);
    }

    protected Server startServerForServlet(HttpServlet servlet, String path) throws Exception {
        Server server = new Server(0);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(servlet), path);
        server.start();

        return server;
    }
}
