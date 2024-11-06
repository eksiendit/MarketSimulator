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

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class StockMarketSimulator extends Application {

    private double balance = 10000;  // Начальный баланс пользователя
    private Label balanceLabel;
    private TableView<Stock> stockTable;
    private TextArea transactionHistory;
    private LineChart<Number, Number> lineChart;
    private XYChart.Series<Number, Number> priceSeries;  // Сериес для графика
    private int timeStep = 0;  // Время для оси X на графике

    public void stop(Stage primaryStage) {
        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Stock Market Simulator");
        // Остановка работы приложения при закрытии основного окна
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
        initializeStocks();

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
        }, 0, 2000);  // обновление каждые 2 секунды

        // Логика покупки и продажи
        buyButton.setOnAction(e -> processTransaction("buy", amountField));
        sellButton.setOnAction(e -> processTransaction("sell", amountField));
    }

    private void initializeStocks() {
        stockTable.getItems().addAll(
                new Stock("AAPL", getRandomPrice()),
                new Stock("GOOGL", getRandomPrice()),
                new Stock("AMZN", getRandomPrice())
        );
    }

    private void updateStockPrices() throws InterruptedException {
        Random random = new Random();

        for (Stock stock : stockTable.getItems()) {
            double change = (random.nextDouble() - 1.5) * 2;  // изменение цены на [-3, 3]%
            stock.setPrice(stock.getPrice() * (1 + change / 100));
        }

        // Обновляем график
        timeStep++;

        Stock selectedStock = stockTable.getItems().get(0);  // Используем первую акцию для примера (AAPL)
        double currentPrice = selectedStock.getPrice();
        priceSeries.getData().add(new XYChart.Data<>(timeStep, currentPrice));
    }

    private double getRandomPrice() {
        Random random = new Random();
        return 50 + (random.nextDouble() * 100);  // цены в диапазоне 50-150$
    }

    // Обработка транзакции покупки или продажи
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
                    transactionHistory.appendText("Bought " + amount + " of " + selectedStock.getName() + " at $" + selectedStock.getPrice() + " each. Total: $" + totalPrice + "\n");
                } else {
                    transactionHistory.appendText("Insufficient balance to buy.\n");
                }
            } else if ("sell".equals(type)) {
                // Простая логика продажи (реализуйте свою логику количества акций)
                balance += totalPrice;
                transactionHistory.appendText("Sold " + amount + " of " + selectedStock.getName() + " at $" + selectedStock.getPrice() + " each. Total: $" + totalPrice + "\n");
            }

            balanceLabel.setText("Balance: $" + balance);
            amountField.clear();

        } catch (NumberFormatException ex) {
            transactionHistory.appendText("Invalid amount.\n");
        }
    }

    // Создание графиков с данными
    private LineChart<Number, Number> createChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Price");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Stock Prices");

        priceSeries = new XYChart.Series<>();
        priceSeries.setName("AAPL Stock Price");

        lineChart.getData().add(priceSeries);
        return lineChart;
    }


    public static void main(String[] args) {
        Application.launch(StockMarketSimulator.class, args);
    }
}