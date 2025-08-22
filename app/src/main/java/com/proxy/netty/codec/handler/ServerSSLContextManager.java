package com.proxy.netty.codec.handler;

import com.proxy.setting.Settings;
import com.proxy.ssl.PrivateKeyAndCertChain;

import android.content.Context;
import com.proxy.ssl.KeyStoreGenerator;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import com.proxy.utils.Networks;
import com.proxy.exception.SSLContextException;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;
import java.nio.file.Path;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;

/**
 * Hold current root cert and cert generator
 *
 */
public class ServerSSLContextManager {

	private String rootKeyStorePath;
	private KeyStoreGenerator keyStoreGenerator;
	private BigInteger lastRootCertSN;
	// ssl context cache
	private final ConcurrentHashMap<String, SslContext> sslContextCache = new ConcurrentHashMap<>();
	// guard for set new root cert
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	public ServerSSLContextManager(Context context, String rootKeyStorePath, char[] keyStorePassword) {
		this.rootKeyStorePath = rootKeyStorePath;
		long start = System.currentTimeMillis();
		KeyStoreGenerator keyStoreGenerator;
		try {
			keyStoreGenerator = new KeyStoreGenerator(context, rootKeyStorePath, keyStorePassword);
		} catch (Exception e) {
			throw new SSLContextException(e);
		}

		BigInteger rootCertSN = keyStoreGenerator.getRootCertSN();

		lock.writeLock().lock();
		try {
			if (rootCertSN.equals(lastRootCertSN)) {
				// do nothing
				return;
			}
			this.keyStoreGenerator = keyStoreGenerator;
			this.lastRootCertSN = rootCertSN;
			this.sslContextCache.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Create ssl context for the host
	 */
	public SslContext createSSlContext(String host, boolean useH2) {
		String finalHost = Networks.wildcardHost(host);
		lock.readLock().lock();
		try {
			return sslContextCache.computeIfAbsent(host + ":" + useH2, key -> {
				try {
					return getNettySslContextInner(finalHost, useH2);
				} catch (Exception e) {
					throw new SSLContextException(e);
				}
			});
		} finally {
			lock.readLock().unlock();
		}
	}

	private SslContext getNettySslContextInner(String host, boolean useH2) throws Exception {
		long start = System.currentTimeMillis();
		PrivateKeyAndCertChain keyAndCertChain = keyStoreGenerator.generateCertChain(host, Settings.certValidityDays);

		SslContextBuilder builder = SslContextBuilder.forServer(keyAndCertChain.privateKey(),
				keyAndCertChain.certificateChain());
		
		if (useH2) {
			//                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
			builder.applicationProtocolConfig(new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
					SelectorFailureBehavior.NO_ADVERTISE, SelectedListenerFailureBehavior.ACCEPT,
					ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1));
		}
		return builder.build();
	}

	public String getRootKeyStorePath() {
		return rootKeyStorePath;
	}

	public KeyStoreGenerator getKeyStoreGenerator() {
		return keyStoreGenerator;
	}
}