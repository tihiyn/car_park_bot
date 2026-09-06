package com.tihiyn.car_park_bot;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

public final class TelegramProxy {
    private static final String TELEGRAM_HOST = "api.telegram.org";
    private static final int DEFAULT_SOCKS_PORT = 1080;

    private TelegramProxy() {
    }

    public static void install() {
        String host = System.getenv("PROXY_SOCKS_HOST");
        if (host == null || host.isBlank()) {
            return;
        }
        int port = parsePort(System.getenv("PROXY_SOCKS_PORT"));
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));

        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                String target = uri == null ? null : uri.getHost();
                return TELEGRAM_HOST.equalsIgnoreCase(target) ? List.of(proxy) : List.of(Proxy.NO_PROXY);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress address, IOException failure) {
            }
        });
        System.out.printf("Трафик к %s направлен через SOCKS-прокси %s:%d%n", TELEGRAM_HOST, host, port);
    }

    private static int parsePort(String value) {
        try {
            return value == null || value.isBlank() ? DEFAULT_SOCKS_PORT : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("PROXY_SOCKS_PORT должен быть числом, получено: " + value, e);
        }
    }
}
