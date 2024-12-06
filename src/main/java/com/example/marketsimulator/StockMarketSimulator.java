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
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

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

    public void stop(Stage primaryStage) {
        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Stock Market Simulator");
        stop(primaryStage);
        // Баланс пользователя
        balanceLabel = new Label("Balance: $" + balance);

        // Таблица для акций
        stockTable = new TableView<>();
        stockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Stock, String> nameColumn = new TableColumn<>("Stock Name");
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());

        TableColumn<Stock, Double> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(data -> data.getValue().priceProperty().asObject());

        stockTable.getColumns().addAll(nameColumn, priceColumn);

        // Кнопки для покупки и продажи
        Button buyButton = new Button("Buy");
        Button sellButton = new Button("Sell");

        // Поле ввода для количества акций
        TextField amountField = new TextField();
        amountField.setPromptText("Amount");

        // История сделок
        transactionHistory = new TextArea();
        transactionHistory.setEditable(false);
        transactionHistory.setPrefHeight(150);

        HBox actionBox = new HBox(10, new Label("Amount:"), amountField, buyButton, sellButton);

        // Создаем график
        lineChart = createChart();

        // Лэйаут для отображения графиков и истории сделок
        VBox layout = new VBox(10, balanceLabel, stockTable, actionBox, lineChart, transactionHistory);
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Инициализация акций
        initialize();

        // Псевдослучайное изменение цен
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    try {
                        updateStockPrices();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }, 0, 2000); // обновление каждые 2 секунды

        // При выборе акции - перестраиваем график для неё
        stockTable.getSelectionModel().selectedItemProperty().addListener((obs, oldv, newv) -> {
            if (newv != null) {
                currentlyDisplayedStock = newv.getName();
                rebuildSeriesForSelectedStock(currentlyDisplayedStock);
            }
        });

        // Логика покупки/продажи
        buyButton.setOnAction(e -> processTransaction("buy", amountField));
        sellButton.setOnAction(e -> processTransaction("sell", amountField));
    }

    private void initialize() {
        stockTable.getItems().addAll(
                new Stock("AAPL", getRandomPrice()),
                new Stock("GOOGL", getRandomPrice()),
                new Stock("AMZN", getRandomPrice())
        );
        // Инициализируем пустые списки для исторических данных
        for (Stock stock : stockTable.getItems()) {
            historyData.put(stock.getName(), new ArrayList<>());
        }
    }

    private void updateStockPrices() throws InterruptedException {
        Random random = new Random();

        for (Stock stock : stockTable.getItems()) {
            double change = (random.nextDouble(3) - 1.5) * 2;  // случайное изменение цены
            double newPrice = stock.getPrice() * (1 + change / 100);
            stock.setPrice(newPrice);

            // Записываем данные
            List<PricePoint> dataList = historyData.get(stock.getName());
            dataList.add(new PricePoint(timeStep, newPrice));

            // Если сейчас отображается именно эта акция, добавляем новую точку прямо в график
            if (currentlyDisplayedStock != null && currentlyDisplayedStock.equals(stock.getName()) && displayedSeries != null) {
                // Добавляем только новую точку, не пересоздавая всю серию
                displayedSeries.getData().add(new XYChart.Data<>(timeStep, newPrice));
            }
        }

        timeStep++;
    }

    private double getRandomPrice() {
        Random random = new Random();
        return 50 + (random.nextDouble() * 100);  // цены в диапазоне 50-150$
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
                    transactionHistory.appendText("Bought " + amount + " of " + selectedStock.getName()
                            + " at $" + selectedStock.getPrice() + " each. Total: $" + totalPrice + "\n");
                } else {
                    transactionHistory.appendText("Insufficient balance to buy.\n");
                }
            } else if ("sell".equals(type)) {
                // Простая логика продажи
                balance += totalPrice;
                transactionHistory.appendText("Sold " + amount + " of " + selectedStock.getName()
                        + " at $" + selectedStock.getPrice() + " each. Total: $" + totalPrice + "\n");
            }

            balanceLabel.setText("Balance: $" + balance);
            amountField.clear();

        } catch (NumberFormatException ex) {
            transactionHistory.appendText("Invalid amount.\n");
        }
    }

    // Создание графика
    private LineChart<Number, Number> createChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Price");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Stock Prices");
        return chart;
    }

    // Перестроение серии для выбранной акции (вызывается при выборе новой акции)
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
