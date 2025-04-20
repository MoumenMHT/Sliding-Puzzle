package sample;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main extends Application {

    private static final int GRID_SIZE = 3;
    private static final int TILE_SIZE = 80;
    private static final int MAX_MOVES = 50;

    private static int levelIndex = 0;
    private static boolean isWin = false;
    private boolean isPaused = false;

    private final Label lblMoves = new Label("Moves: 0");
    private final Label lblScore = new Label("Score: 0");
    private final Label lblBestScore = new Label("Best: 0");
    private final Label lblMovesLeft = new Label("Moves Left: 50");
    private final Label lblLevel = new Label("Level: 1");
    private final Label lblTime = new Label("Time: 0s");

    private final Button[][] tiles = new Button[GRID_SIZE][GRID_SIZE];
    private final GridPane gridPane = new GridPane();
    private BorderPane root;
    private StackPane overlay;
    private StackPane puzzleGridPane;
    private Scene gameScene;
    private Stage primaryStage;
    private BorderPane topControls;

    private final String[] configurations = {
            "123864705", "073214568", "124857063", "204153876",
            "624801753", "670132584", "781635240", "280163547"
    };

    private int movesCount = 0;
    private int score = 0;
    private int bestScore = 0;
    private String currentConfig = "";
    private Timeline timer;
    private int elapsedTime = 0;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e);");

        topControls = createTopControls();
        root.setTop(topControls);
        root.setCenter(createPuzzleGrid());
        root.setBottom(createStatsPanel());

        gameScene = new Scene(root, 450, 580);
        gameScene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        Scene startMenuScene = createStartMenu();

        primaryStage.setTitle("Sliding Puzzle Game");
        primaryStage.setResizable(false);
        primaryStage.setScene(startMenuScene);
        primaryStage.show();
    }

    private Scene createStartMenu() {
        StackPane menuPane = new StackPane();
        menuPane.setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e);");
        menuPane.setPadding(new Insets(20));

        Label title = new Label("Sliding Puzzle Game");
        title.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 36px; -fx-text-fill: white; -fx-font-weight: bold;");
        title.setEffect(new DropShadow(5, Color.gray(0.4)));

        Button btnStart = new Button("Start Game");
        btnStart.setPrefWidth(200);
        btnStart.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 16px; -fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5;");
        btnStart.setOnAction(e -> {
            levelIndex = 0;
            primaryStage.setScene(gameScene);
            loadLevel();
        });

        Button btnSelectLevel = new Button("Select Level");
        btnSelectLevel.setPrefWidth(200);
        btnSelectLevel.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 16px; -fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5;");
        btnSelectLevel.setOnAction(e -> {
            VBox levelMenu = new VBox(10);
            levelMenu.setAlignment(Pos.CENTER);
            levelMenu.setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e);");
            levelMenu.setPadding(new Insets(20));

            Label selectTitle = new Label("Select Level");
            selectTitle.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 24px; -fx-text-fill: white;");

            ComboBox<String> levelSelector = new ComboBox<>();
            levelSelector.setPrefWidth(200);
            for (int i = 0; i < configurations.length; i++) {
                levelSelector.getItems().add("Level " + (i + 1));
            }
            levelSelector.setValue("Level 1");

            Button btnConfirm = new Button("Start");
            btnConfirm.setPrefWidth(200);
            btnConfirm.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 16px; -fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5;");
            btnConfirm.setOnAction(e2 -> {
                levelIndex = levelSelector.getSelectionModel().getSelectedIndex();
                primaryStage.setScene(gameScene);
                loadLevel();
            });

            Button btnBack = new Button("Back");
            btnBack.setPrefWidth(200);
            btnBack.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 16px; -fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
            btnBack.setOnAction(e2 -> primaryStage.setScene(createStartMenu()));

            levelMenu.getChildren().addAll(selectTitle, levelSelector, btnConfirm, btnBack);
            primaryStage.setScene(new Scene(levelMenu, 450, 580));
        });

        Button btnExit = new Button("Exit");
        btnExit.setPrefWidth(200);
        btnExit.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 16px; -fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
        btnExit.setOnAction(e -> primaryStage.close());

        VBox menuBox = new VBox(20, title, btnStart, btnSelectLevel, btnExit);
        menuBox.setAlignment(Pos.CENTER);
        menuPane.getChildren().add(menuBox);

        return new Scene(menuPane, 450, 580);
    }

    private BorderPane createTopControls() {
        Button btnNewGame = new Button("New Game");
        Button btnRestart = new Button("Restart");
        Button btnRandomLevel = new Button("Random Level");
        Button btnPause = new Button("| |");
        Button btnMenu = new Button("Menu");
        ComboBox<String> levelSelector = new ComboBox<>();

        double buttonWidth = 180;
        btnNewGame.setPrefWidth(buttonWidth);
        btnRestart.setPrefWidth(buttonWidth);
        btnRandomLevel.setPrefWidth(buttonWidth);
        levelSelector.setPrefWidth(buttonWidth);
        btnPause.setPrefSize(40, 40);
        btnMenu.setPrefWidth(buttonWidth);

        for (int i = 0; i < configurations.length; i++) {
            levelSelector.getItems().add("Level " + (i + 1));
        }
        levelSelector.setValue("Level " + (levelIndex + 1));
        levelSelector.setOnAction(e -> {
            if (overlay != null && root.getCenter() == overlay) {
                removeOverlay();
            }
            if (!isWin && !isPaused) {
                levelIndex = levelSelector.getSelectionModel().getSelectedIndex();
                loadLevel();
            }
        });

        btnNewGame.setOnAction(e -> {
            if (overlay != null && root.getCenter() == overlay) {
                removeOverlay();
            }
            if (!isWin && !isPaused) {
                levelIndex = (levelIndex + 1) % configurations.length;
                levelSelector.setValue("Level " + (levelIndex + 1));
                loadLevel();
            }
        });

        btnRestart.setOnAction(e -> {
            if (overlay != null && root.getCenter() == overlay) {
                removeOverlay();
            }
            if (!isWin && !isPaused) {
                applyConfiguration(currentConfig);
            }
        });

        btnRandomLevel.setOnAction(e -> {
            if (overlay != null && root.getCenter() == overlay) {
                removeOverlay();
            }
            if (!isWin && !isPaused) {
                currentConfig = generateRandomConfiguration();
                levelIndex = -1;
                levelSelector.setValue("Custom Level");
                loadLevel();
            }
        });

        btnPause.setOnAction(e -> {
            if (!isWin) {
                isPaused = !isPaused;
                btnPause.setText(isPaused ? "▶" : "| |");
                if (isPaused) {
                    stopTimer();
                    disableTiles();
                } else {
                    startTimer();
                    enableTiles();
                }
            }
        });

        btnMenu.setOnAction(e -> {
            if (overlay != null && root.getCenter() == overlay) {
                removeOverlay();
            }
            primaryStage.setScene(createStartMenu());
        });

        VBox centerControls = new VBox(10, btnNewGame, btnRestart, btnRandomLevel, levelSelector, btnMenu);
        centerControls.setAlignment(Pos.CENTER);
        centerControls.setPadding(new Insets(10));

        HBox pauseContainer = new HBox(btnPause);
        pauseContainer.setAlignment(Pos.TOP_LEFT);
        pauseContainer.setPadding(new Insets(5));

        BorderPane topControls = new BorderPane();
        topControls.setLeft(pauseContainer);
        topControls.setCenter(centerControls);

        return topControls;
    }

    private void disableControlButtons() {
        if (topControls != null) {
            VBox centerControls = (VBox) topControls.getCenter();
            for (Node node : centerControls.getChildren()) {
                if (node instanceof Button || node instanceof ComboBox) {
                    node.setDisable(true);
                }
            }
            HBox pauseContainer = (HBox) topControls.getLeft();
            for (Node node : pauseContainer.getChildren()) {
                if (node instanceof Button) {
                    node.setDisable(true);
                }
            }
        }
    }

    private void enableControlButtons() {
        if (topControls != null) {
            VBox centerControls = (VBox) topControls.getCenter();
            for (Node node : centerControls.getChildren()) {
                if (node instanceof Button || node instanceof ComboBox) {
                    node.setDisable(false);
                }
            }
            HBox pauseContainer = (HBox) topControls.getLeft();
            for (Node node : pauseContainer.getChildren()) {
                if (node instanceof Button) {
                    node.setDisable(false);
                }
            }
        }
    }

    private GridPane createStatsPanel() {
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(10);
        statsGrid.setPadding(new Insets(15));
        statsGrid.setAlignment(Pos.CENTER);
        statsGrid.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-background-radius: 10;");

        lblMoves.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 14px; -fx-text-fill: white;");
        lblScore.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 14px; -fx-text-fill: white;");
        lblBestScore.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 14px; -fx-text-fill: white;");
        lblMovesLeft.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 14px; -fx-text-fill: white;");
        lblLevel.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 14px; -fx-text-fill: white;");
        lblTime.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 14px; -fx-text-fill: white;");

        statsGrid.add(lblMoves, 0, 0);
        statsGrid.add(lblMovesLeft, 1, 0);
        statsGrid.add(lblScore, 0, 1);
        statsGrid.add(lblBestScore, 1, 1);
        statsGrid.add(lblLevel, 0, 2);
        statsGrid.add(lblTime, 1, 2);

        return statsGrid;
    }

    private StackPane createPuzzleGrid() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Button tile = new Button();
                tile.setPrefSize(TILE_SIZE, TILE_SIZE);
                tile.setFont(Font.font("Arial", 18));
                tile.setEffect(new DropShadow(5, Color.gray(0.4)));
                final int r = row, c = col;
                tile.setOnAction(e -> attemptMove(r, c));
                tiles[row][col] = tile;
                gridPane.add(tile, col, row);
            }
        }
        gridPane.setHgap(8);
        gridPane.setVgap(8);
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-background-radius: 10; -fx-padding: 10;");

        puzzleGridPane = new StackPane(gridPane);
        puzzleGridPane.setPadding(new Insets(15));
        return puzzleGridPane;
    }

    private void disableTiles() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                tiles[row][col].setDisable(true);
            }
        }
    }

    private void enableTiles() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                tiles[row][col].setDisable(false);
            }
        }
    }

    private void loadLevel() {
        isWin = false;
        isPaused = false;
        movesCount = 0;
        currentConfig = levelIndex >= 0 ? configurations[levelIndex] : currentConfig;

        lblLevel.setText(levelIndex >= 0 ? "Level: " + (levelIndex + 1) : "Level: Custom");
        lblMoves.setText("Moves: 0");
        lblScore.setText("Score: " + score);
        lblBestScore.setText("Best: " + bestScore);
        lblMovesLeft.setText("Moves Left: " + MAX_MOVES);

        stopTimer();
        removeOverlay();
        applyConfiguration(currentConfig);
        startTimer();
    }

    private void applyConfiguration(String config) {
        movesCount = 0;
        lblMoves.setText("Moves: 0");
        lblMovesLeft.setText("Moves Left: " + MAX_MOVES);

        stopTimer();
        elapsedTime = 0;
        lblTime.setText("Time: 0s");

        for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
            int row = i / GRID_SIZE;
            int col = i % GRID_SIZE;
            char val = config.charAt(i);
            Button tile = tiles[row][col];

            if (val == '0') {
                tile.setText("");
                tile.setId("empty-tile");
            } else {
                tile.setText(String.valueOf(val));
                tile.setId("tile");
            }
        }
        startTimer();
    }

    private String generateRandomConfiguration() {
        StringBuilder config = new StringBuilder("123456780");
        Random random = new Random();
        int emptyRow = 2, emptyCol = 2;

        int moves = random.nextInt(51) + 50;
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int i = 0; i < moves; i++) {
            List<int[]> validMoves = new ArrayList<>();
            for (int[] dir : directions) {
                int newRow = emptyRow + dir[0];
                int newCol = emptyCol + dir[1];
                if (isInside(newRow, newCol)) {
                    validMoves.add(new int[]{newRow, newCol});
                }
            }

            if (validMoves.isEmpty()) break;

            int[] move = validMoves.get(random.nextInt(validMoves.size()));
            int newRow = move[0], newCol = move[1];

            int emptyIndex = emptyRow * GRID_SIZE + emptyCol;
            int tileIndex = newRow * GRID_SIZE + newCol;
            char temp = config.charAt(tileIndex);
            config.setCharAt(tileIndex, '0');
            config.setCharAt(emptyIndex, temp);

            emptyRow = newRow;
            emptyCol = newCol;
        }

        return config.toString();
    }

    private void attemptMove(int row, int col) {
        if (isWin || isPaused || movesCount >= MAX_MOVES) return;

        if (canMoveTo(row - 1, col)) swapTiles(row, col, row - 1, col);
        else if (canMoveTo(row + 1, col)) swapTiles(row, col, row + 1, col);
        else if (canMoveTo(row, col - 1)) swapTiles(row, col, row, col - 1);
        else if (canMoveTo(row, col + 1)) swapTiles(row, col, row, col + 1);
    }

    private boolean canMoveTo(int row, int col) {
        return isInside(row, col) && tiles[row][col].getText().isEmpty();
    }

    private boolean isInside(int row, int col) {
        return row >= 0 && col >= 0 && row < GRID_SIZE && col < GRID_SIZE;
    }

    private void swapTiles(int r1, int c1, int r2, int c2) {
        Button from = tiles[r1][c1];
        Button to = tiles[r2][c2];

        TranslateTransition tt = new TranslateTransition(Duration.millis(150), from);
        ScaleTransition st = new ScaleTransition(Duration.millis(150), from);
        FadeTransition ft = new FadeTransition(Duration.millis(150), from);

        tt.setByX((c2 - c1) * (TILE_SIZE + 8));
        tt.setByY((r2 - r1) * (TILE_SIZE + 8));
        tt.setInterpolator(Interpolator.EASE_BOTH);

        st.setToX(1.05);
        st.setToY(1.05);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.setInterpolator(Interpolator.EASE_BOTH);

        ft.setFromValue(1.0);
        ft.setToValue(0.7);
        ft.setCycleCount(2);
        ft.setAutoReverse(true);
        ft.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition pt = new ParallelTransition(tt, st, ft);
        pt.setOnFinished(e -> {
            from.setTranslateX(0);
            from.setTranslateY(0);
            from.setOpacity(1.0);
            to.setText(from.getText());
            to.setId("tile");
            from.setText("");
            from.setId("empty-tile");

            movesCount++;
            score = Math.max(0, score - 10); // Deduct 10 points per move
            lblMoves.setText("Moves: " + movesCount);
            lblMovesLeft.setText("Moves Left: " + (MAX_MOVES - movesCount));
            lblScore.setText("Score: " + score);
            if (score > bestScore) {
                bestScore = score;
                lblBestScore.setText("Best: " + bestScore);
            }

            if (checkWin()) showWinDialog();
            else if (movesCount >= MAX_MOVES) showLoseDialog();
        });
        pt.play();
    }

    private boolean checkWin() {
        String targetConfig = "123804765";
        int index = 0;
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                String text = tiles[i][j].getText();
                char expected = targetConfig.charAt(index);
                if (expected == '0') {
                    if (!text.isEmpty()) return false;
                } else {
                    if (!text.equals(String.valueOf(expected))) return false;
                }
                index++;
            }
        }
        return true;
    }

    private void showWinDialog() {
        isWin = true;
        stopTimer();
        disableControlButtons();

        // Calculate score: base + time bonus
        int baseScore = 1000;
        int timeBonus = Math.max(0, 500 - elapsedTime * 5);
        score += (baseScore + timeBonus);

        lblScore.setText("Score: " + score);
        if (score > bestScore) {
            bestScore = score;
            lblBestScore.setText("Best: " + bestScore);
        }

        overlay = createOverlay(
                "\uD83C\uDFC6 You Win in " + elapsedTime + "s!\nScore: " + (baseScore + timeBonus),
                "Next Level",
                "btn-next",
                () -> {
                    removeOverlay();
                    levelIndex = (levelIndex + 1) % configurations.length;
                    loadLevel();
                }
        );
        BorderPane.setAlignment(overlay, Pos.CENTER);
        root.setCenter(overlay);
    }

    private void showLoseDialog() {
        stopTimer();
        disableControlButtons();

        // Apply penalty for losing
        score = Math.max(0, score - 200);
        lblScore.setText("Score: " + score);
        if (score > bestScore) {
            bestScore = score;
            lblBestScore.setText("Best: " + bestScore);
        }

        overlay = createOverlay(
                "\uD83D\uDC80 Game Over!",
                "Retry Level",
                "btn-retry",
                () -> {
                    removeOverlay();
                    applyConfiguration(currentConfig);
                }
        );
        BorderPane.setAlignment(overlay, Pos.CENTER);
        root.setCenter(overlay);
    }

    private void removeOverlay() {
        if (overlay != null && root.getCenter() == overlay) {
            root.setCenter(puzzleGridPane);
            overlay = null;
            enableControlButtons();
        }
    }

    private StackPane createOverlay(String message, String buttonText, String buttonId, Runnable action) {
        Region background = new Region();
        background.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        background.setPrefSize(450, 580);

        Label label = new Label(message);
        label.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 18px; -fx-text-fill: white;");

        Button button = new Button(buttonText);
        button.setId(buttonId);
        button.setOnAction(e -> action.run());

        VBox box = new VBox(15, label, button);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-background-radius: 10;");
        box.setMaxWidth(300);
        box.setMaxHeight(150);

        StackPane overlay = new StackPane(background, box);
        overlay.setAlignment(Pos.CENTER);
        return overlay;
    }

    private void startTimer() {
        stopTimer();
        elapsedTime = 0;
        lblTime.setText("Time: 0s");
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            elapsedTime++;
            lblTime.setText("Time: " + elapsedTime + "s");
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}