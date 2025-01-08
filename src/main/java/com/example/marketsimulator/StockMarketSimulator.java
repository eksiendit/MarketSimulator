package com.example.marketsimulator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockMarketSimulator extends Application {

    private double balance = 10000;  // Начальный баланс пользователя
    private Label balanceLabel;
    private TableView<Stock> stockTable;
    private TextArea transactionHistory;
    private LineChart<Number, Number> lineChart;
    private int timeStep = 0;  // Время для оси X на графике

    private Map<String, List<PricePoint>> historyData = new HashMap<>();

    // Храним текущую выбранную акцию и её серию,
    // чтобы при обновлении просто добавлять новые точки
    private String currentlyDisplayedStock = null;
    private XYChart.Series<Number, Number> displayedSeries = null;

    // Создадим экземпляр WebSocket-клиента
    private BinanceWebSocketClient binanceClient = new BinanceWebSocketClient();
    private BinanceWebSocketClient binanceClient1 = new BinanceWebSocketClient();
    private BinanceWebSocketClient binanceClient2 = new BinanceWebSocketClient();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Stock (Crypto) Market Simulator");

        balanceLabel = new Label("Balance: $" + balance);

        stockTable = new TableView<>();
        stockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Stock, String> nameColumn = new TableColumn<>("Stock Name");
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());

        TableColumn<Stock, Double> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(data -> data.getValue().priceProperty().asObject());

        stockTable.getColumns().addAll(nameColumn, priceColumn);

        Button buyButton = new Button("Buy");
        Button sellButton = new Button("Sell");

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");

        transactionHistory = new TextArea();
        transactionHistory.setEditable(false);
        transactionHistory.setPrefHeight(150);

        HBox actionBox = new HBox(10, new Label("Amount:"), amountField, buyButton, sellButton);

        lineChart = createChart();

        VBox layout = new VBox(10, balanceLabel, stockTable, actionBox, lineChart, transactionHistory);
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Инициализация с "фиктивными" акциями (на деле будут криптопары)
        initialize();

        // --- ВАЖНО! ---
        // Теперь НЕ запускаем Timer с рандомом, а подключаемся к Binance WebSocket
        // Для теста подпишемся только на BTCUSDT
        // @ticker - 24-часовые данные: в JSON получаем поле "c" (текущая цена)
        binanceClient.connect("btcusdt@ticker", newPrice -> {
            // Здесь код обновления цены в UI
            updatePriceInTable("BTC\\USDT", newPrice);
        });
        binanceClient1.connect("ethusdt@ticker", newPrice -> {
            // Здесь код обновления цены в UI
            updatePriceInTable("ETH\\USDT", newPrice);
        });
        binanceClient2.connect("dogeusdt@ticker", newPrice -> {
            // Здесь код обновления цены в UI
            updatePriceInTable("DOGE\\USDT", newPrice);
        });

        // При выборе "акции" отображаем её историю на графике
        stockTable.getSelectionModel().selectedItemProperty().addListener((obs, oldv, newv) -> {
            if (newv != null) {
                currentlyDisplayedStock = newv.getName();
                rebuildSeriesForSelectedStock(currentlyDisplayedStock);
            }
        });

        // Логика покупки/продажи
        buyButton.setOnAction(e -> processTransaction("buy", amountField));
        sellButton.setOnAction(e -> processTransaction("sell", amountField));

        // При закрытии приложения закрываем WebSocket
        primaryStage.setOnCloseRequest(event -> {
            binanceClient.close();
            Platform.exit();
            System.exit(0);
        });
    }

    private void initialize() {
        // Допустим, мы хотим BTCUSDT отобразить в таблице
        stockTable.getItems().add(new Stock("BTC\\USDT", 0.0));
        stockTable.getItems().add(new Stock("ETH\\USDT", 0.0));
        stockTable.getItems().add(new Stock("DOGE\\USDT", 0.0));

        // Инициализируем историю для "BTCUSDT"
        historyData.put("BTC\\USDT", new ArrayList<>());
        historyData.put("ETH\\USDT", new ArrayList<>());
        historyData.put("DOGE\\USDT", new ArrayList<>());
    }

    /**
     * Этот метод вызывается при получении новой цены для соответствующей "акции"
     */
    private void updatePriceInTable(String stockName, double newPrice) {
        // Ищем нужный элемент в таблице
        for (Stock stock : stockTable.getItems()) {
            if (stock.getName().equals(stockName)) {
                stock.setPrice(newPrice);

                // Записываем в историю
                List<PricePoint> dataList = historyData.get(stockName);
                dataList.add(new PricePoint(timeStep, newPrice));

                // Если на графике сейчас выбрана эта акция, добавляем точку
                if (currentlyDisplayedStock != null
                        && currentlyDisplayedStock.equals(stockName)
                        && displayedSeries != null) {
                    displayedSeries.getData().add(new XYChart.Data<>(timeStep, newPrice));
                }
                timeStep++;
                break;
            }
        }
    }

    private void processTransaction(String type, TextField amountField) {
        try {
            int amount = Integer.parseInt(amountField.getText());
            Stock selectedStock = stockTable.getSelectionModel().getSelectedItem();

            if (selectedStock == null) {
                transactionHistory.appendText("No stock selected.\n");
                return;
            }

            double totalPrice = selectedStock.getPrice() * amount;
            if ("buy".equals(type)) {
                if (balance >= totalPrice) {
                    balance -= totalPrice;
                    transactionHistory.appendText(
                            "Bought " + amount + " of " + selectedStock.getName()
                                    + " at $" + selectedStock.getPrice() + " each. Total: $" + totalPrice + "\n");
                } else {
                    transactionHistory.appendText("Insufficient balance to buy.\n");
                }
            } else if ("sell".equals(type)) {
                // Очень упрощённая логика
                balance += totalPrice;
                transactionHistory.appendText(
                        "Sold " + amount + " of " + selectedStock.getName()
                                + " at $" + selectedStock.getPrice() + " each. Total: $" + totalPrice + "\n");
            }

            balanceLabel.setText("Balance: $" + balance);
            amountField.clear();

        } catch (NumberFormatException ex) {
            transactionHistory.appendText("Invalid amount.\n");
        }
    }

    private LineChart<Number, Number> createChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Price");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Stock Prices");
        return chart;
    }

    private void rebuildSeriesForSelectedStock(String stockName) {
        lineChart.getData().clear();
        displayedSeries = new XYChart.Series<>();
        displayedSeries.setName(stockName);

        List<PricePoint> points = historyData.get(stockName);
        if (points != null) {
            for (PricePoint point : points) {
                displayedSeries.getData().add(new XYChart.Data<>(point.time, point.price));
            }
        }

        lineChart.getData().add(displayedSeries);
    }

    public static void main(String[] args) {
        Application.launch(StockMarketSimulator.class, args);
    }
}
