package com.proxy.netty;

import com.proxy.setting.ProxySetting;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;

import java.net.InetSocketAddress;
import io.netty.handler.proxy.ProxyHandler;
import java.util.function.Supplier;

/**
 * `ProxyHandlerSupplier` is a class that provides a `ProxyHandler` instance
 * based on the provided `ProxySetting`. It implements the `Supplier` interface
 * This class handles the creation of different types of proxy handlers (HTTP, SOCKS4, SOCKS5) based
 * on the configuration.
 */
public class ProxyHandlerSupplier implements Supplier<ProxyHandler> {
	private final ProxySetting proxySetting;
	private final InetSocketAddress address;

	public ProxyHandlerSupplier(ProxySetting proxySetting) {
		this.proxySetting = proxySetting;
		// Java InetSocketAddress always do dns resolver.
		// we just create InetSocketAddress now so dns resolve will not block netty event loop.
		this.address = new InetSocketAddress(proxySetting.host(), proxySetting.port());
	}

	@Override
	public ProxyHandler get() {
		if (!proxySetting.use()) {
			return null;
		}
		ProxyHandler proxyHandler = newProxyHandler();
		proxyHandler.setConnectTimeoutMillis(NettySettings.CONNECT_TIMEOUT);
		return proxyHandler;
	}

	/**
	 * Creates a new `ProxyHandler` instance based on the proxy settings.
	 * This method handles the logic for creating different types of proxy
	 * handlers (HTTP, SOCKS4, SOCKS5) and setting the appropriate
	 * authentication details.
	 *
	 * @return A new `ProxyHandler` instance.
	 * @throws RuntimeException If an unknown proxy type is specified.
	 */
	public ProxyHandler newProxyHandler() {
		switch (proxySetting.type()) {
		case ProxySetting.TYPE_HTTP:
			if (proxySetting.user().isEmpty()) {
				return new HttpProxyHandler(address);
			} else {
				return new HttpProxyHandler(address, proxySetting.user(), proxySetting.password());
			}

		case ProxySetting.TYPE_SOCKS5:
			if (proxySetting.user().isEmpty()) {
				return new Socks5ProxyHandler(address);
			} else {
				return new Socks5ProxyHandler(address, proxySetting.user(), proxySetting.password());
			}
		case ProxySetting.TYPE_SOCKS4:
			if (proxySetting.user().isEmpty()) {
				return new Socks4ProxyHandler(address);
			} else {
				return new Socks4ProxyHandler(address, proxySetting.user());
			}
		default:
			throw new RuntimeException("unknown proxy type: " + proxySetting.type());
		}
	}
}