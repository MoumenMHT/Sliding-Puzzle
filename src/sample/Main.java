package sample;

import javafx.animation.TranslateTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    private static final int GRID_SIZE = 3;
    private static final int TILE_SIZE = 60;
    private static final int MAX_MOVES = 50;

    private static int levelIndex = 0;
    private static boolean isWin = false;

    private final Label lblMoves = new Label("Moves: 0");
    private final Label lblScore = new Label("Score: 0");
    private final Label lblBestScore = new Label("Best Score: 0");
    private final Label lblMovesLeft = new Label("Moves Left: 20");
    private final Label lblLevel = new Label();
    private final Label lblTime = new Label("Time: 0s");  // Label to show time

    private final Button[][] tiles = new Button[GRID_SIZE][GRID_SIZE];
    private final GridPane gridPane = new GridPane();

    private final String[] configurations = {
            "123456708", "073214568", "124857063", "204153876",
            "624801753", "670132584", "781635240", "280163547"
    };

    private int movesCount = 0;
    private int score = 0;
    private int BestScore = 0;
    private String currentConfig = "";

    private Timeline timer;  // Timer to track time
    private int elapsedTime = 0;  // Time in seconds

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setId("root");

        HBox topControls = createTopControls();
        HBox statsPanel = createStatsPanel();
        StackPane puzzleContainer = createPuzzleGrid();

        root.getChildren().addAll(topControls, statsPanel, puzzleContainer);

        Scene scene = new Scene(root, 600, 320);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.setTitle("Sliding Puzzle");
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();

        loadLevel();
    }

    private HBox createTopControls() {
        Button btnNewGame = new Button("New Game");
        Button btnRestart = new Button("Restart Level");

        btnNewGame.setPrefWidth(100);
        btnRestart.setPrefWidth(106);
        btnNewGame.setId("red");
        btnRestart.setId("yellow");

        btnNewGame.setOnAction(e -> {
            if (!isWin) {
                levelIndex = (levelIndex + 1) % configurations.length;
                loadLevel();
            }
        });

        btnRestart.setOnAction(e -> {
            if (!isWin) {
                applyConfiguration(currentConfig);
            }
        });

        HBox controls = new HBox(350, btnNewGame, btnRestart);
        controls.setPadding(new Insets(0, 0, 5, 10));
        return controls;
    }

    private HBox createStatsPanel() {
        lblMoves.setId("label");
        lblScore.setId("label");
        lblBestScore.setId("label");
        lblMovesLeft.setId("label");
        lblLevel.setId("label");
        lblTime.setId("label");  // Add the time label to the stats panel

        HBox stats = new HBox(25, lblMoves, lblScore, lblBestScore, lblMovesLeft, lblLevel, lblTime);
        stats.setPadding(new Insets(0, 0, 0, 10));
        return stats;
    }

    private StackPane createPuzzleGrid() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Button tile = new Button();
                tile.setPrefSize(TILE_SIZE, TILE_SIZE);
                final int r = row, c = col;
                tile.setOnAction(e -> attemptMove(r, c));
                tiles[row][col] = tile;
                gridPane.add(tile, col, row);
            }
        }
    
        // Set the horizontal and vertical gaps
        gridPane.setVgap(10);
        gridPane.setHgap(10);
    
        // StackPane to center the GridPane
        StackPane centerPane = new StackPane(gridPane);
        centerPane.setAlignment(Pos.CENTER);  // This will center the grid in the StackPane
        centerPane.setTranslateX(170);
    
        // Ensuring that StackPane will expand to take the full space of the parent
        centerPane.setPrefWidth(Region.USE_COMPUTED_SIZE); 
        centerPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
    
        return centerPane;
    }

    private void loadLevel() {
        isWin = false;
        movesCount = 0;
        currentConfig = configurations[levelIndex];

        lblLevel.setText("Level: " + (levelIndex + 1));
        lblMoves.setText("Moves: 0");
        lblScore.setText("Score: " + score);
        lblBestScore.setText("Best Score: " + BestScore);
        lblMovesLeft.setText("Moves Left: " + MAX_MOVES);

        applyConfiguration(currentConfig);

        // Start the timer when a new level is loaded
        startTimer();
    }

    private void applyConfiguration(String config) {
        movesCount = 0;
        lblMoves.setText("Moves: 0");
        lblMovesLeft.setText("Moves Left: " + MAX_MOVES);

        for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
            int row = i / GRID_SIZE;
            int col = i % GRID_SIZE;
            char val = config.charAt(i);
            Button tile = tiles[row][col];

            if (val == '0') {
                tile.setText("");
                tile.setId("khaki");
            } else {
                tile.setText(String.valueOf(val));
                tile.setId("coral");
            }
        }
    }

    private void attemptMove(int row, int col) {
        if (isWin || movesCount >= MAX_MOVES) return;

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
        tt.setByX((c2 - c1) * TILE_SIZE);
        tt.setByY((r2 - r1) * TILE_SIZE);
        tt.setOnFinished(e -> {
            from.setTranslateX(0);
            from.setTranslateY(0);
            to.setText(from.getText());
            to.setId("coral");
            from.setText("");
            from.setId("khaki");

            lblMoves.setText("Moves: " + (++movesCount));
            lblMovesLeft.setText("Moves Left: " + (MAX_MOVES - movesCount));
            
            

            if (checkWin()) showWinDialog();
            else if (movesCount >= MAX_MOVES) showLoseDialog();
        });
        tt.play();
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
        score = 10 * ( MAX_MOVES - movesCount);
        return true;
    }

    private void showWinDialog() {
        isWin = true;
        stopTimer();  // Stop the timer when the player wins
        Stage winStage = new Stage();
        VBox box = createDialogBox("🎉 You Win With in " + elapsedTime + "s!" , "Next Level", "yellow", () -> {
            winStage.close();
            levelIndex = (levelIndex + 1) % configurations.length;
            loadLevel();
        });
        lblTime.setText("Time: " + elapsedTime + "s");
        showDialog(winStage, box, "Level Completed");
    }

    private void showLoseDialog() {
        stopTimer();  // Stop the timer when the player loses
        Stage loseStage = new Stage();
        VBox box = createDialogBox("💀 You Lose!", "Retry Level", "red", () -> {
            loseStage.close();
            applyConfiguration(currentConfig);
        });
        
        lblScore.setText("Score: " + score);
        if (score > BestScore) {
            lblBestScore.setText("Best Score: " + score);
        }
        score = 0;
        lblTime.setText("Time: " + elapsedTime + "s");  // Display the time when the user loses
        showDialog(loseStage, box, "Game Over");
    }

    private VBox createDialogBox(String message, String buttonText, String buttonId, Runnable action) {
        Label label = new Label(message);
        label.setId("label");

        Button button = new Button(buttonText);
        button.setId(buttonId);
        button.setOnAction(e -> action.run());

        VBox box = new VBox(10, label, button);
        box.setAlignment(Pos.CENTER);
        box.setId("root");

        return box;
    }

    private void showDialog(Stage stage, VBox content, String title) {
        Scene scene = new Scene(content, 200, 110);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void startTimer() {
        elapsedTime = 0;  // Reset time at the start of each level
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            elapsedTime++;
            lblTime.setText("Time: " + elapsedTime + "s");  // Update the time display
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
