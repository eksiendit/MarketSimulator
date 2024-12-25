package com.example.marketsimulator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;



public class BinanceWebSocketClient {
    private static final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws";
    private WebSocket webSocket;
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Метод для подключения к Binance WebSocket по конкретному символу.
     * Например, "btcusdt@ticker" вернёт нам данные по цене BTC/USDT.
     */
    public void connect(String symbolEndpoint, PriceUpdateCallback callback) {
        Request request = new Request.Builder()
                .url(BINANCE_WS_URL + "/" + symbolEndpoint)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("WebSocket Opened for " + symbolEndpoint);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                // Парсим входящее сообщение в JSON

                JsonObject json = new JsonParser()
                        .parse(text)
                        .getAsJsonObject();

                // В разных стримах Binance разная структура JSON
                // Например, если мы используем stream @ticker:
                // {
                //   "e": "24hrTicker",  // Event type
                //   "E": 123456789,     // Event time
                //   "s": "BTCUSDT",     // Symbol
                //   "c": "46300.00",    // Current day's close price
                //   ...
                // }

                // Для упрощения возьмём поле "c" (current close price)
                String priceStr = json.get("c").getAsString();
                double price = Double.parseDouble(priceStr);

                // Вызываем колбэк, чтобы передать цену в JavaFX-приложение
                Platform.runLater(() -> {
                    callback.onPriceUpdate(price);
                });
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                System.out.println("WebSocket Failure: " + t.getMessage());
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                System.out.println("WebSocket Closed. Reason: " + reason);
            }
        });

        // В реальных условиях хорошо бы ещё предусмотреть логику переподключения,
        // если соединение внезапно оборвётся.
    }

    /**
     * Отключение WebSocket при завершении работы приложения
     */
    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Closing WebSocket");
        }
        client.dispatcher().executorService().shutdown();
    }

    /**
     * Функциональный интерфейс для колбэка
     */
    @FunctionalInterface
    public interface PriceUpdateCallback {
        void onPriceUpdate(double newPrice);
    }
}