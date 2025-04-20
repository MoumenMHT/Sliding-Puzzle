package sample;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

    private final String[] configurations = {
            "123456708", "073214568", "124857063", "204153876",
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
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e);");

        root.setTop(createTopControls());
        root.setCenter(createPuzzleGrid());
        root.setBottom(createStatsPanel());

        Scene scene = new Scene(root, 450, 580); // Adjusted height back to 580
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.setTitle("Sliding Puzzle Game");
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();

        loadLevel();
    }

    private BorderPane createTopControls() {
        Button btnNewGame = new Button("New Game");
        Button btnRestart = new Button("Restart");
        Button btnRandomLevel = new Button("Random Level");
        Button btnPause = new Button("| |");
        ComboBox<String> levelSelector = new ComboBox<>();

        // Set uniform width for buttons and ComboBox (except pause)
        double buttonWidth = 180;
        btnNewGame.setPrefWidth(buttonWidth);
        btnRestart.setPrefWidth(buttonWidth);
        btnRandomLevel.setPrefWidth(buttonWidth);
        levelSelector.setPrefWidth(buttonWidth);
        btnPause.setPrefSize(40, 40); // Smaller size for icon button

        // Populate level selector
        for (int i = 0; i < configurations.length; i++) {
            levelSelector.getItems().add("Level " + (i + 1));
        }
        levelSelector.setValue("Level 1");
        levelSelector.setOnAction(e -> {
            if (!isWin && !isPaused) {
                levelIndex = levelSelector.getSelectionModel().getSelectedIndex();
                loadLevel();
            }
        });

        btnNewGame.setOnAction(e -> {
            if (!isWin && !isPaused) {
                levelIndex = (levelIndex + 1) % configurations.length;
                levelSelector.setValue("Level " + (levelIndex + 1));
                loadLevel();
            }
        });

        btnRestart.setOnAction(e -> {
            if (!isWin && !isPaused) {
                applyConfiguration(currentConfig);
            }
        });

        btnRandomLevel.setOnAction(e -> {
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

        // Center other controls in a VBox
        VBox centerControls = new VBox(10, btnNewGame, btnRestart, btnRandomLevel, levelSelector);
        centerControls.setAlignment(Pos.CENTER);
        centerControls.setPadding(new Insets(10));

        // Place pause button in top-left corner
        HBox pauseContainer = new HBox(btnPause);
        pauseContainer.setAlignment(Pos.TOP_LEFT);
        pauseContainer.setPadding(new Insets(5));

        // Combine in a BorderPane
        BorderPane topControls = new BorderPane();
        topControls.setLeft(pauseContainer);
        topControls.setCenter(centerControls);

        return topControls;
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

        StackPane pane = new StackPane(gridPane);
        pane.setPadding(new Insets(15));
        return pane;
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

            lblMoves.setText("Moves: " + (++movesCount));
            lblMovesLeft.setText("Moves Left: " + (MAX_MOVES - movesCount));

            if (checkWin()) showWinDialog();
            else if (movesCount >= MAX_MOVES) showLoseDialog();
        });
        pt.play();
    }

    private boolean checkWin() {
        int k = 1;
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++, k++) {
                String text = tiles[i][j].getText();
                if (k == GRID_SIZE * GRID_SIZE) {
                    if (!text.isEmpty()) return false;
                } else {
                    if (!text.equals(String.valueOf(k))) return false;
                }
            }
        }
        score = 10 * (MAX_MOVES - movesCount);
        return true;
    }

    private void showWinDialog() {
        isWin = true;
        stopTimer();
        Stage winStage = new Stage();
        VBox box = createDialogBox("\uD83C\uDFC6 You Win in " + elapsedTime + "s!", "Next Level", "btn-next", () -> {
            winStage.close();
            levelIndex = (levelIndex + 1) % configurations.length;
            loadLevel();
        });
        showDialog(winStage, box, "Level Completed");
    }

    private void showLoseDialog() {
        stopTimer();
        Stage loseStage = new Stage();
        VBox box = createDialogBox("\uD83D\uDC80 Game Over!", "Retry Level", "btn-retry", () -> {
            loseStage.close();
            applyConfiguration(currentConfig);
        });

        lblScore.setText("Score: " + score);
        if (score > bestScore) {
            bestScore = score;
            lblBestScore.setText("Best: " + bestScore);
        }
        score = 0;
        showDialog(loseStage, box, "Game Over");
    }

    private VBox createDialogBox(String message, String buttonText, String buttonId, Runnable action) {
        Label label = new Label(message);
        label.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 18px; -fx-text-fill: white;");

        Button button = new Button(buttonText);
        button.setId(buttonId);
        button.setOnAction(e -> action.run());

        VBox box = new VBox(15, label, button);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-background-radius: 10;");
        return box;
    }

    private void showDialog(Stage stage, VBox content, String title) {
        Scene scene = new Scene(content, 300, 150);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
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