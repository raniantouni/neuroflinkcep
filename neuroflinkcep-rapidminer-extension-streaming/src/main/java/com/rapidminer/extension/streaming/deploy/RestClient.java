/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import com.google.common.collect.Maps;
import com.rapidminer.tools.LogService;


/**
 * Client for REST API calls (support: HTTP)
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class RestClient {

	private static final Logger LOGGER = LogService.getRoot();

	private static final String ROOT_URL_TEMPLATE = "%s://%s:%s";

	private final ExecutorService executor = Executors.newFixedThreadPool(10);

	private final String proto;

	private final String host;

	private final String port;

	private final HttpClient httpClient;

	private Map<String,String> headers = Maps.newHashMap();

	/**
	 * Constructs the client for the given endpoint (host:port), with "http" as protocol
	 *
	 * @param host
	 * @param port
	 */
	public RestClient(String host, String port) {
		this(false, false, host, port);
	}

	/**
	 * Constructs the client for the given endpoint configuration
	 *
	 * @param secure (protocol --> https)
	 * @param allowSelfSignedCert whether to allow self-signed certificates
	 * @param host
	 * @param port
	 */
	public RestClient(boolean secure, boolean allowSelfSignedCert, String host, String port) {
		this.proto = secure ? "https" : "http";
		this.host = host;
		this.port = port;

		HttpClientBuilder clientBuilder = HttpClients.custom();
		try {
			if (secure && allowSelfSignedCert) {
				HostnameVerifier hostNameVerifier = NoopHostnameVerifier.INSTANCE;
				SSLContext sslContext = new SSLContextBuilder()
					.loadTrustMaterial(null, new TrustSelfSignedStrategy())
					.build();

				clientBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext, hostNameVerifier));
			}
		} catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
			LOGGER.log(Level.SEVERE, "Self-signed certificates will not be allowed due to initialization error", e);
		} finally {
			httpClient = clientBuilder.build();
		}
	}

	/**
	 * Sends a HTTP POST request with the given URL to the endpoint using the body in the request asynchronously
	 *
	 * @param url API URL specification (e.g.: "/api/jobs/123")
	 * @param body of the request
	 * @param err error message to use if the communication was not successful (not 2xx)
	 * @return pair of request (to potentially abort) and response future
	 */
	public ImmutablePair<HttpUriRequest, Future<HttpEntity>> postAsync(String url,
																	   HttpEntity body,
																	   String err) {
		// Build request
		RequestBuilder request = RequestBuilder
			.post(rootURL() + "/" + StringUtils.stripStart(url, "/"))
			.setEntity(body);

		// Send request asynchronously
		return sendRequestAsync(request, err);
	}

	/**
	 * Sends a HTTP POST request with the given URL to the endpoint using the body in the request
	 *
	 * @param url  API URL specification (e.g.: "/api/jobs/123")
	 * @param body of the request
	 * @param err  error message to use if the communication was not successful (not 2xx)
	 * @return response if the communication was successful
	 * @throws IOException
	 */
	public HttpEntity post(String url, HttpEntity body, String err) throws IOException {
		try {
			return postAsync(url, body, err).getRight().get();
		} catch (InterruptedException | ExecutionException e) {
			throw new IOException("POST request could not succeed", e.getCause());
		}
	}

	/**
	 * Sends a HTTP GET request with the given URL to the endpoint
	 *
	 * @param url API URL specification (e.g.: "/api/jobs/123")
	 * @param err error message to use if the communication was not successful (not 2xx)
	 * @return response
	 * @throws IOException
	 */
	public HttpEntity get(String url, String err) throws IOException {
		// Build request
		RequestBuilder request = RequestBuilder
			.get(rootURL() + "/" + StringUtils.stripStart(url, "/"));

		// Send request
		return sendRequest(request, err);
	}

	/**
	 * Sends a HTTP DELETE request with the given URL to the endpoint
	 *
	 * @param url API URL specification (e.g.: "/api/jobs/123")
	 * @param err error message to use if the communication was not successful (not 2xx)
	 * @throws IOException
	 */
	public void delete(String url, String err) throws IOException {
		// Build request
		RequestBuilder request = RequestBuilder
			.delete(rootURL() + "/" + StringUtils.stripStart(url, "/"));

		// Send request
		sendRequest(request, err);
	}

	/**
	 * Sends a HTTP PATCH request with the given URL to the endpoint
	 *
	 * @param url API URL specification (e.g.: "/api/jobs/123")
	 * @param err error message to use if the communication was not successful (not 2xx)
	 * @throws IOException
	 */
	public void patch(String url, String err) throws IOException {
		// Build request
		RequestBuilder request = RequestBuilder
			.patch(rootURL() + "/" + StringUtils.stripStart(url, "/"));

		// Send request
		sendRequest(request, err);
	}

	/**
	 * Takes the response body and returns a UTF-8 string representation of it
	 *
	 * @param response
	 * @return see above
	 * @throws IOException
	 */
	public String getResponseBody(HttpEntity response) throws IOException {
		StringWriter writer = new StringWriter();
		IOUtils.copy(response.getContent(), writer, StandardCharsets.UTF_8);
		return writer.toString();
	}

	/**
	 * Adds a header to the internal collection that will be appended to every request this client initiates
	 * @param key Header field key
	 * @param value Header field value
	 */
	public void setHeader(String key, String value) {
		headers.put(key, value);
	}

	/**
	 * Decides if the HTTP code should be considered successful or not
	 *
	 * @param httpCode
	 * @return true if the http-code is 2XX, false otherwise
	 */
	private boolean isSuccessful(int httpCode) {
		return (httpCode / 100) == 2;
	}

	/**
	 * "Executes" the request, i.e. actual HTTP communication takes place
	 *
	 * @param request executable object, represents request
	 * @param err     error message to use if the communication was not successful (not 2xx)
	 * @return response object
	 * @throws IOException
	 */
	private HttpEntity sendRequest(RequestBuilder request, String err) throws IOException {
		try {
			return sendRequestAsync(request, err).getRight().get();
		} catch (InterruptedException | ExecutionException e) {
			throw new IOException("Sending request failed", e.getCause());
		}
	}

	/**
	 * "Executes" the request asynchronously, i.e. actual HTTP communication takes place
	 *
	 * @param requestBuilder executable object, represents request
	 * @param err error message to use if the communication was not successful (not 2xx)
	 * @return pair of request (to potentially abort) and response future
	 */
	private ImmutablePair<HttpUriRequest, Future<HttpEntity>> sendRequestAsync(RequestBuilder requestBuilder,
																			   String err) {
		// Add headers
		for (Map.Entry<String,String> entry : headers.entrySet()) {
			requestBuilder.addHeader(entry.getKey(), entry.getValue());
		}

		HttpUriRequest request = requestBuilder.build();
		Future<HttpEntity> response = executor.submit(() -> {
			// Send request
			HttpResponse httpResp = httpClient.execute(request);

			// Get response details and handle response
			StatusLine statusLine = httpResp.getStatusLine();

			if (isSuccessful(statusLine.getStatusCode())) {
				return httpResp.getEntity();
			} else {
				throw new IOException(err + " --> " + statusLine.getReasonPhrase());
			}
		});

		return ImmutablePair.of(request, response);
	}

	/**
	 * @return filled root-URL-template
	 */
	private String rootURL() {
		return String.format(ROOT_URL_TEMPLATE, proto, host, port);
	}

}