package sample;

import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main extends Application {

    private static final int GRID_SIZE = 3;
    private static final double BASE_TILE_SIZE = 80;
    private static final int MAX_MOVES = 50;
    private static final String SAVE_FILE = "game_save.dat";

    private static int levelIndex = 0;
    private static boolean isWin = false;
    private boolean isPaused = false;
    private boolean isAnimating = false; // Added to prevent overlapping animations

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
    private HBox topControls;

    private final String[] configurations = {
            "123864705", "073214568", "124857063", "204153876",
            "624801753", "670132584", "781635240", "280163547"
    };

    private int movesCount = 0;
    private int score = 0;
    private int bestScore = 0;
    private String currentConfig = configurations[0];
    private String initialConfig = configurations[0];
    private Timeline timer;
    private int elapsedTime = 0;

    // Variables to track stage properties
    private boolean isFullScreen = false;
    private double windowWidth = 600;
    private double windowHeight = 580;
    private double windowX = -1; // -1 indicates default/centered position
    private double windowY = -1; // -1 indicates default/centered position

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

        gameScene = new Scene(root, 600, 580);
        gameScene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(500);

        loadGameState();

        Scene startMenuScene = createStartMenu();

        primaryStage.setTitle("Sliding Puzzle Game");
        primaryStage.setScene(startMenuScene);
        primaryStage.setOnCloseRequest(e -> saveGameState());
        primaryStage.show();

        // Save initial stage properties
        saveStageProperties();
    }

    // Helper methods to save and restore stage properties
    private void saveStageProperties() {
        isFullScreen = primaryStage.isFullScreen();
        if (!isFullScreen) {
            windowWidth = primaryStage.getWidth();
            windowHeight = primaryStage.getHeight();
            windowX = primaryStage.getX();
            windowY = primaryStage.getY();
        }
    }

    private void restoreStageProperties() {
        primaryStage.setFullScreen(isFullScreen);
        if (!isFullScreen) {
            primaryStage.setWidth(windowWidth);
            primaryStage.setHeight(windowHeight);
            if (windowX != -1 && windowY != -1) {
                // Basic validation to ensure window is on screen
                if (windowX >= 0 && windowY >= 0) {
                    primaryStage.setX(windowX);
                    primaryStage.setY(windowY);
                } else {
                    primaryStage.centerOnScreen();
                }
            }
        }
    }

    private static class GameState implements Serializable {
        private static final long serialVersionUID = 1L;
        String currentConfig;
        String initialConfig;
        int levelIndex;
        int score;
        int movesCount;
        int elapsedTime;
        int bestScore;

        GameState(String currentConfig, String initialConfig, int levelIndex, int score, int movesCount, int elapsedTime, int bestScore) {
            this.currentConfig = currentConfig;
            this.initialConfig = initialConfig;
            this.levelIndex = levelIndex;
            this.score = score;
            this.movesCount = movesCount;
            this.elapsedTime = elapsedTime;
            this.bestScore = bestScore;
        }
    }

    private void saveGameState() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            saveStageProperties();
            GameState state = new GameState(currentConfig, initialConfig, levelIndex, score, movesCount, elapsedTime, bestScore);
            oos.writeObject(state);
        } catch (IOException e) {
            System.err.println("Error saving game state: " + e.getMessage());
        }
    }

    private void loadGameState() {
        File saveFile = new File(SAVE_FILE);
        if (saveFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))) {
                GameState state = (GameState) ois.readObject();
                if (state.currentConfig != null && state.currentConfig.length() == GRID_SIZE * GRID_SIZE &&
                    state.initialConfig != null && state.initialConfig.length() == GRID_SIZE * GRID_SIZE) {
                    currentConfig = state.currentConfig;
                    initialConfig = state.initialConfig;
                    levelIndex = state.levelIndex;
                    score = state.score;
                    movesCount = state.movesCount;
                    elapsedTime = state.elapsedTime;
                    bestScore = state.bestScore;
                    lblScore.setText("Score: " + score);
                    lblBestScore.setText("Best: " + bestScore);
                    restoreStageProperties();
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading game state: " + e.getMessage());
            }
        }
    }

    private Scene createStartMenu() {
        StackPane menuPane = new StackPane();
        menuPane.setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e);");
        menuPane.setPadding(new Insets(20));

        Label title = new Label("Sliding Puzzle Game");
        title.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 36px; -fx-text-fill: white; -fx-font-weight: bold;");
        title.setEffect(new DropShadow(5, Color.gray(0.4)));

        Label bestScoreLabel = new Label("Best Score: " + bestScore);
        bestScoreLabel.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 20px; -fx-text-fill: white;");

        Button btnStart = new Button("Start Game");
        btnStart.setPrefWidth(200);
        btnStart.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 16px; -fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5;");
        btnStart.setOnAction(e -> {
            saveStageProperties();
            levelIndex = 0;
            initialConfig = configurations[levelIndex];
            currentConfig = initialConfig;
            score = 0;
            movesCount = 0;
            elapsedTime = 0;
            saveGameState();
            primaryStage.setScene(gameScene);
            loadLevel();
            restoreStageProperties();
        });

        Button btnContinue = new Button("Continue");
        btnContinue.setPrefWidth(200);
        btnContinue.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 16px; -fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5;");
        btnContinue.setOnAction(e -> {
            saveStageProperties();
            primaryStage.setScene(gameScene);
            loadLevel();
            restoreStageProperties();
        });
        btnContinue.setDisable(!new File(SAVE_FILE).exists() || currentConfig == null || currentConfig.length() != GRID_SIZE * GRID_SIZE);

        Button btnSelectLevel = new Button("Select Level");
        btnSelectLevel.setPrefWidth(200);
        btnSelectLevel.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 16px; -fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5;");
        btnSelectLevel.setOnAction(e -> {
            saveStageProperties();
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
                saveStageProperties();
                levelIndex = levelSelector.getSelectionModel().getSelectedIndex();
                initialConfig = configurations[levelIndex];
                currentConfig = initialConfig;
                score = 0;
                movesCount = 0;
                elapsedTime = 0;
                saveGameState();
                primaryStage.setScene(gameScene);
                loadLevel();
                restoreStageProperties();
            });

            Button btnBack = new Button("Back");
            btnBack.setPrefWidth(200);
            btnBack.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 16px; -fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
            btnBack.setOnAction(e2 -> {
                saveStageProperties();
                primaryStage.setScene(createStartMenu());
                restoreStageProperties();
            });

            levelMenu.getChildren().addAll(selectTitle, levelSelector, btnConfirm, btnBack);
            primaryStage.setScene(new Scene(levelMenu, 450, 580));
            restoreStageProperties();
        });

        Button btnExit = new Button("Exit");
        btnExit.setPrefWidth(200);
        btnExit.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 16px; -fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
        btnExit.setOnAction(e -> {
            saveGameState();
            primaryStage.close();
        });

        VBox menuBox = new VBox(20, title, bestScoreLabel, btnStart, btnContinue, btnSelectLevel, btnExit);
        menuBox.setAlignment(Pos.CENTER);
        menuPane.getChildren().add(menuBox);

        return new Scene(menuPane, 450, 580);
    }

    private HBox createTopControls() {
        Button btnPause = new Button("| |");
        Button btnNewGame = new Button("New Game");
        Button btnRestart = new Button("Restart");
        Button btnRandomLevel = new Button("Random Level");
        Button btnMenu = new Button("Menu");
        Button btnFullScreen = new Button("⛶");

        btnPause.setPrefSize(40, 30);
        btnNewGame.setPrefSize(120, 30);
        btnRestart.setPrefSize(80, 30);
        btnRandomLevel.setPrefSize(120, 30);
        btnMenu.setPrefSize(80, 30);
        btnFullScreen.setPrefSize(40, 30);

        String buttonStyle = "-fx-font-family: 'Arial'; -fx-font-size: 12px; -fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5;";
        btnPause.setStyle(buttonStyle);
        btnNewGame.setStyle(buttonStyle);
        btnRestart.setStyle(buttonStyle);
        btnRandomLevel.setStyle(buttonStyle);
        btnMenu.setStyle(buttonStyle);
        btnFullScreen.setId("btn-fullscreen");

        btnNewGame.setOnAction(e -> {
            if (overlay != null && root.getCenter() == overlay) {
                removeOverlay();
            }
            if (!isWin && !isPaused) {
                saveStageProperties();
                levelIndex = (levelIndex + 1) % configurations.length;
                initialConfig = configurations[levelIndex];
                currentConfig = initialConfig;
                movesCount = 0;
                score = 0;
                elapsedTime = 0;
                saveGameState();
                loadLevel();
                restoreStageProperties();
            }
        });

        btnRestart.setOnAction(e -> {
            removeOverlay();
            isWin = false;
            isPaused = false;
            movesCount = 0;
            score = 0;
            elapsedTime = 0;
            currentConfig = initialConfig;
            enableControlButtons();
            enableTiles();
            saveGameState();
            saveStageProperties();
            loadLevel();
            restoreStageProperties();
        });

        btnRandomLevel.setOnAction(e -> {
            if (overlay != null && root.getCenter() == overlay) {
                removeOverlay();
            }
            if (!isWin && !isPaused) {
                saveStageProperties();
                initialConfig = generateRandomConfiguration();
                currentConfig = initialConfig;
                levelIndex = -1;
                movesCount = 0;
                elapsedTime = 0;
                saveGameState();
                loadLevel();
                restoreStageProperties();
            }
        });

        btnPause.setOnAction(e -> {
            if (!isWin) {
                isPaused = !isPaused;
                btnPause.setText(isPaused ? "▶" : "| |");
                if (isPaused) {
                    stopTimer();
                    disableTiles();
                    saveGameState();
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
            saveGameState();
            saveStageProperties();
            primaryStage.setScene(createStartMenu());
            restoreStageProperties();
        });

        btnFullScreen.setOnAction(e -> {
            isFullScreen = !primaryStage.isFullScreen();
            primaryStage.setFullScreen(isFullScreen);
            btnFullScreen.setText(isFullScreen ? "🗗" : "⛶");
            saveStageProperties();
        });

        HBox topControls = new HBox(10, btnPause, btnNewGame, btnRestart, btnRandomLevel, btnMenu, btnFullScreen);
        topControls.setAlignment(Pos.CENTER);
        topControls.setPadding(new Insets(10));
        return topControls;
    }

    private void disableControlButtons() {
        if (topControls != null) {
            for (Node node : topControls.getChildren()) {
                if (node instanceof Button) {
                    node.setDisable(true);
                }
            }
        }
    }

    private void enableControlButtons() {
        if (topControls != null) {
            for (Node node : topControls.getChildren()) {
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
                tile.prefWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.min(root.widthProperty().get() - 100, root.heightProperty().get() - 200) / GRID_SIZE,
                    root.widthProperty(), root.heightProperty()
                ));
                tile.prefHeightProperty().bind(tile.prefWidthProperty());
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

        // Add listener to stage size changes
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> updateTileSizes());
        primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> updateTileSizes());

        return puzzleGridPane;
    }

    private void updateTileSizes() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                tiles[row][col].requestLayout();
            }
        }
        gridPane.requestLayout();
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

        lblLevel.setText(levelIndex >= 0 ? "Level: " + (levelIndex + 1) : "Level: Random");
        lblMoves.setText("Moves: " + movesCount);
        lblScore.setText("Score: " + score);
        lblBestScore.setText("Best: " + bestScore);
        lblMovesLeft.setText("Moves Left: " + (MAX_MOVES - movesCount));
        lblTime.setText("Time: " + elapsedTime + "s");

        stopTimer();
        removeOverlay();
        applyConfiguration(currentConfig);
        enableTiles();
        enableControlButtons();
        if (!isPaused && !isWin) {
            startTimer();
        }
        restoreStageProperties();
    }

    private void applyConfiguration(String config) {
        lblMoves.setText("Moves: " + movesCount);
        lblMovesLeft.setText("Moves Left: " + (MAX_MOVES - movesCount));
        lblTime.setText("Time: " + elapsedTime + "s");

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
        if (isWin || isPaused || movesCount >= MAX_MOVES || isAnimating) return;

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
        isAnimating = true; // Prevent new moves during animation
        Button from = tiles[r1][c1];
        Button to = tiles[r2][c2];

        // Bring the moving tile to the front
        from.toFront();

        // Animation setup
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), from);
        ScaleTransition st = new ScaleTransition(Duration.millis(100), from);
        Glow glow = new Glow(0.8);
        Timeline glowTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(glow.levelProperty(), 0.8)),
            new KeyFrame(Duration.millis(100), new KeyValue(glow.levelProperty(), 0))
        );

        // Configure translation
        double tileSize = tiles[0][0].getWidth();
        tt.setByX((c2 - c1) * (tileSize + 8));
        tt.setByY((r2 - r1) * (tileSize + 8));
        tt.setInterpolator(Interpolator.EASE_BOTH);

        // Configure scale
        st.setToX(1.15);
        st.setToY(1.15);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.setInterpolator(Interpolator.EASE_BOTH);

        // Apply glow effect
        from.setEffect(glow);

        // Combine animations
        ParallelTransition highlight = new ParallelTransition(st, glowTimeline);
        SequentialTransition animation = new SequentialTransition(highlight, tt);

        // Handle completion
        animation.setOnFinished(e -> {
            // Reset transformations and effects
            from.setTranslateX(0);
            from.setTranslateY(0);
            from.setScaleX(1.0);
            from.setScaleY(1.0);
            from.setEffect(new DropShadow(5, Color.gray(0.4))); // Restore original effect

            // Swap tile content
            to.setText(from.getText());
            to.setId("tile");
            from.setText("");
            from.setId("empty-tile");

            // Update game state
            movesCount++;
            score += 10;
            lblMoves.setText("Moves: " + movesCount);
            lblMovesLeft.setText("Moves Left: " + (MAX_MOVES - movesCount));
            lblScore.setText("Score: " + score);
            if (score > bestScore) {
                bestScore = score;
                lblBestScore.setText("Best: " + bestScore);
            }

            StringBuilder newConfig = new StringBuilder();
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    String text = tiles[i][j].getText();
                    newConfig.append(text.isEmpty() ? '0' : text.charAt(0));
                }
            }
            currentConfig = newConfig.toString();
            saveGameState();

            isAnimating = false; // Allow new moves

            if (checkWin()) showWinDialog();
            else if (movesCount >= MAX_MOVES) showLoseDialog();
        });

        animation.play();
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

        int baseScore = 1000;
        int timeBonus = Math.max(0, 500 - elapsedTime * 5);
        score += (baseScore + timeBonus);

        if (score > bestScore) {
            bestScore = score;
            lblBestScore.setText("Best: " + bestScore);
        }
        lblScore.setText("Score: " + score);
        saveGameState();

        saveStageProperties();
        overlay = createOverlay(
                "\uD83C\uDFC6 You Win in " + elapsedTime + "s!\nScore: " + (baseScore + timeBonus),
                "Next Level",
                "btn-next",
                () -> {
                    removeOverlay();
                    levelIndex = (levelIndex + 1) % configurations.length;
                    initialConfig = configurations[levelIndex];
                    currentConfig = initialConfig;
                    movesCount = 0;
                    elapsedTime = 0;
                    saveGameState();
                    loadLevel();
                    restoreStageProperties();
                }
        );
        BorderPane.setAlignment(overlay, Pos.CENTER);
        root.setCenter(overlay);
        restoreStageProperties();
    }

    private void showLoseDialog() {
        stopTimer();
        disableControlButtons();

        score = Math.max(0, score - 200);
        if (score > bestScore) {
            bestScore = score;
            lblBestScore.setText("Best: " + bestScore);
        }
        lblScore.setText("Score: " + score);
        saveGameState();

        saveStageProperties();
        overlay = createOverlay(
                "\uD83D\uDC80 Game Over!",
                "Retry Level",
                "btn-retry",
                () -> {
                    removeOverlay();
                    movesCount = 0;
                    elapsedTime = 0;
                    currentConfig = initialConfig;
                    saveGameState();
                    loadLevel();
                    restoreStageProperties();
                }
        );
        BorderPane.setAlignment(overlay, Pos.CENTER);
        root.setCenter(overlay);
        restoreStageProperties();
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
        background.prefWidthProperty().bind(root.widthProperty());
        background.prefHeightProperty().bind(root.heightProperty());

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
        lblTime.setText("Time: " + elapsedTime + "s");
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