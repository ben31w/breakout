/*
 * This program is a remake of the classic arcade game 'Breakout.'
 * Your goal is to destroy all the bricks at the top of the screen,
 * and you have three lives to accomplish this. Move the paddle with 
 * the mouse and click once to start the ball.
 * 
 * In this version, combos have a profound effect on the player's 
 * score. Each brick is normally worth 100 points. However, if the 
 * ball destroys two bricks before it touches the paddle again, the 
 * player will earn 200 points for the second brick instead of 100.
 * If the ball destroys three bricks before touching the paddle, the 
 * player will earn 200 points for the second brick and 300 points for 
 * the third brick.
 * 
 * Try to rack up large combos to max out your score! 
 * 
 * TIP: If you have 2+ lives and think the ball is bouncing in a way 
 * where it is impossible to achieve a large combo, you can intentionally 
 * lose that ball, spawn a new ball, and hope for a better bounce! There 
 * is some strategy to this game!
 * */
import acm.graphics.*;
import acm.program.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * The entire Breakout game. Just run to play.
 * 
 * @author ben31w
 * @version 2021.01.08
 */
public class Breakout extends GraphicsProgram {

    /** width of game display */
    private static final int WIDTH = 390;

    /** height of game display */
    private static final int HEIGHT = 625;

    /** width of paddle */
    private static final int PADDLE_WIDTH = 60;

    /** height of paddle */
    private static final int PADDLE_HEIGHT = 10;

    /** offset of paddle and bottom of screen in pixels */
    private static final int PADDLE_Y_OFFSET = 60;

    /** number of bricks per row */
    private static final int NBRICKS_PER_ROW = 10;

    /** number of rows of bricks */
    private static final int NBRICK_ROWS = 10;

    /** pixel separation between bricks */
    private static final int BRICK_SEP = 4;

    /** width of each brick in pixels */
    private static final int BRICK_WIDTH = WIDTH / NBRICKS_PER_ROW - BRICK_SEP;

    /** height of each brick in pixels */
    private static final int BRICK_HEIGHT = 8;

    /** radius of ball in pixels */
    private static final int BALL_RADIUS = 6;

    /** offset of the top brick row from top of screen */
    private static final int BRICK_Y_OFFSET = 70;

    /** number of bricks left */
    private int nbricks = NBRICKS_PER_ROW * NBRICK_ROWS;

    /** player's lives */
    private int lives = 3;
    
    /** player's score */
    private int score = 0;

    /** the player's combo (number of bricks hit before touching the paddle) */
    private int combo = 1;

    /** the highest combo the player has reached */
    private int maxCombo = 1;

    /** scores for each ball */
    private int ball1Score, ball2Score, ball3Score;
    
    /** the game's high score */
    private int highScore;
    
    /** used for mouse events (only moves the paddle every 5th mouse move) */
    private int toggle = 0;
    
    /** ball velocity in x- and y-direction */
    private double vx, vy;

    /** records the last x position of the mouse (see mouseMoved method) */
    private double lastX;    

    /** used for mouse events to check if the ball has been started yet */
    private boolean ballStarted = false;
    
    /** label displaying lives */
    private GLabel livesLabel;
    
    /** label displaying score */
    private GLabel scoreLabel;
    
    /** label displaying high score */
    private GLabel highScoreLabel;

    /** labels displaying ball scores (displayed at end of the game) */
    private GLabel ball1Label, ball2Label, ball3Label;

    /** background image */
    private GImage background;

    /** the paddle */
    private GRect paddle;

    /** the ball */
    private GOval ball;

    /**
     * Called when the program is run.
     * 
     * @param args
     *          not used here
     */
    public static void main(String[] args) {
        String[] sizeArgs = { "width=" + WIDTH, "height=" + HEIGHT };
        new Breakout().start(sizeArgs);
    }

    
    /**
     * Called indirectly from the main method. Prepare the game window.
     */
    public void run() {
        setup();
        waitForClick();
        play();
    }

    
    /**
     * Initialize game elements for play.
     */
    public void setup() {
        createBackground();
        createBricks();
        createPaddle();
        createBall();
        addMouseListeners();
    }

    
    /** 
     * Add the background and basic labels to the screen. 
     * */
    public void createBackground() {
        background = new GImage("theVoid.png");
        add(background);
        
        // visible labels
        scoreLabel = new GLabel("Score: " + score, 0, 20);
        scoreLabel.setColor(Color.RED);
        add(scoreLabel);

        livesLabel = new GLabel("Lives: " + lives);
        livesLabel.setColor(Color.RED);
        livesLabel.setLocation(WIDTH / 2 - livesLabel.getWidth() / 2, 20);
        add(livesLabel);
        
        highScore = getHighScore("highScore.txt");
        highScoreLabel = new GLabel("Hi Score: " + highScore);
        highScoreLabel.setColor(Color.RED);
        highScoreLabel.setLocation(WIDTH - highScoreLabel.getWidth(), 20);
        add(highScoreLabel);

        // invisible labels (become visible at end of game)
        ball1Label = new GLabel("Ball 1 Score: " + ball1Score);
        ball1Label.setColor(Color.RED);
        ball1Label.setLocation(0, scoreLabel.getY() + scoreLabel.getHeight());
        ball1Label.setVisible(false);
        add(ball1Label);

        ball2Label = new GLabel("Ball 2 Score: " + ball2Score);
        ball2Label.setColor(Color.RED);
        ball2Label.setLocation(0, scoreLabel.getY() + scoreLabel.getHeight()*2);
        ball2Label.setVisible(false);
        add(ball2Label);

        ball3Label = new GLabel("Ball 3 Score: " + ball3Score);
        ball3Label.setColor(Color.RED);
        ball3Label.setLocation(0, scoreLabel.getY() + scoreLabel.getHeight()*3);
        ball3Label.setVisible(false);
        add(ball3Label);
    }

    /**
     * Add the bricks to the screen.
     */
    public void createBricks() {
        Color gradient = new Color(255, 0, 0);
        
        // Make the bricks.
        for (int x = 0; x < NBRICK_ROWS; x++) {
            for (int y = 0; y < NBRICKS_PER_ROW; y++) {
                GRect brick = 
                        new GRect((y * BRICK_WIDTH) + BRICK_SEP*y + BRICK_SEP/2, 
                        BRICK_Y_OFFSET + (BRICK_HEIGHT * x) + BRICK_SEP*x, 
                        BRICK_WIDTH, 
                        BRICK_HEIGHT);
                brick.setFilled(true);
                brick.setFillColor(gradient);
                add(brick);
                
                // Update the color gradient.
                gradient = new Color(gradient.getRed() - 2, 
                        gradient.getGreen() + 2, gradient.getBlue() + 2);
            }
        }
    }

    
    /**
     * Create the paddle and add it to the screen.
     */
    public void createPaddle() {
        paddle = new GRect(0, HEIGHT - PADDLE_Y_OFFSET, PADDLE_WIDTH, PADDLE_HEIGHT);
        paddle.setFilled(true);
        paddle.setFillColor(Color.WHITE);
        add(paddle);
    }

    
    /**
     * Create the ball and add it to the screen.
     */
    public void createBall() {
        ball = new GOval(paddle.getX() + PADDLE_WIDTH/2 - BALL_RADIUS, 
                paddle.getY() - BALL_RADIUS*2.1, 
                BALL_RADIUS*2, 
                BALL_RADIUS*2);
        ball.setFilled(true);
        ball.setFillColor(Color.WHITE);
        add(ball);

        ballStarted = false;
    }
    
    
    /**
     * Reads and returns the high score in a text file.
     * 
     * @param filePath
     *              a text file to be read
     * @return the first integer in the text file
     */
    public int getHighScore(String filePath) {
        try {
            Scanner fin = new Scanner(new File(filePath));
            int highScore = fin.nextInt();
            fin.close();
            return highScore;
        }
        catch (FileNotFoundException e) {
            updateHighScore(0, filePath);
            return 0;
        }
    }
    
    
    /**
     * Update the game's high score.
     * 
     * @param score
     *              the new high score
     * @param filePath
     *              a text file to write the score to
     */
    public void updateHighScore(int score, String filePath) {
        try {
            PrintWriter fout = new PrintWriter(new File(filePath));
            fout.println(score);
            fout.println(maxCombo); // also add the max combo
            fout.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("Failed to create " + filePath);
        }
    }

    
    /**
     * Start play.
     */
    public void play() {
        startTheBall();
        playBall();
    }

    
    /**
     * Start the ball. Initialize the ball's x- and y-velocity.
     */
    public void startTheBall() {
        ballStarted = true;

        // Ball should have a random x-velocity and direction.
        vx = Math.random() / 3 + .3;
        if (Math.random() < .5) {
            vx = -vx;
        }

        vy = 0.75;
    }

    
    /**
     * Keep playing ball. Move the ball and check for contact with bricks, the 
     * paddle, walls, etc.
     */
    public void playBall() {
        // continuous loop
        while (true) {
            // move the ball
            ball.move(vx, vy);
            // pause
            pause(1);

            // Check for contact along the outer walls.
            // If the ball hits the bottom wall, loseALife.
            if (ball.getY() >= HEIGHT) {
                loseALife();
                break;
            }
            // Otherwise, reverse the ball's x or y-direction velocity.
            else if (ball.getY() <= 0) {
                vy = -vy;
            }
            if (ball.getX() >= WIDTH) {
                vx = -vx;
            }
            else if (ball.getX() <= 0) {
                vx = -vx;
            }

            // Check for collisions with bricks or paddle.
            GObject collider = getCollidingObject();
            
            // If the ball collides with the paddle, reverse the y-velocity and 
            // reset the combo.
            if (collider  ==  paddle) {
                vy = -vy;
                combo = 1;
            }
            // If the ball collides with a brick, reverse the y-velocity and 
            // update the player's score.
            else if (collider instanceof GRect) {
                vy = -vy;
                
                score += 100 * combo;
                scoreLabel.setLabel("Score: " + score);
                ++combo;
                
                // Check if the player's current combo is greater than the 
                // previous maximum combo.
                if (combo > maxCombo) {
                    maxCombo = combo - 1;
                }
                
                // Remove the brick. If there are no bricks left, the player 
                // has won.
                remove(collider);
                --nbricks;
                if (nbricks == 0) {
                    win();
                    break;
                }
            }
        }
    }

    
    /**
     * Subtract a life from the player. If the player still has lives remaining,
     * prepare a new ball. Otherwise, it's game over. This method is called when
     * the ball touches the bottom wall.
     */
    public void loseALife() {
        --lives;
        livesLabel.setLabel("Lives: " + lives);

        // Track scores for each ball.
        if (lives == 2) {
            ball1Score = score;
        }
        else if (lives == 1) {
            ball2Score = score - ball1Score; 
        }
        // If the player runs out of lives, it's game over. Remove the ball and 
        // display stats.
        else if (lives == 0) {
            remove(ball);

            GLabel loss = new GLabel("GAME OVER"); 
            loss.setColor(Color.RED);
            loss.setFont(new Font("calibri", Font.BOLD, 30));
            loss.setLocation(WIDTH / 2 - loss.getWidth() / 2, 
                    HEIGHT / 2 - loss.getHeight());
            add(loss);

            GLabel maxComboLabel = new GLabel("MAX COMBO: " + maxCombo);
            maxComboLabel.setColor(Color.RED);
            maxComboLabel.setFont(new Font("calibri", Font.BOLD, 30));
            maxComboLabel.setLocation(WIDTH / 2 - maxComboLabel.getWidth() / 2, 
                    HEIGHT / 2);
            add(maxComboLabel);

            ball3Score = score - ball2Score - ball1Score;
            displayBallScores();
            
            // Check for a new high score.
            if (score > highScore) {
                updateHighScore(score, "highScore.txt");
            }

            resetGame();
        }
        
        // If the player still has lives, create a new ball and keep playing.
        if (lives != 0) {
            createBall();
            waitForClick();
            play();
        }      
    }
    
    
    /**
     * Congratulate the player for winning!!:)
     */
    public void win() {
        remove(ball);

        GLabel victory = new GLabel("YOU WIN");
        victory.setColor(Color.RED);
        victory.setFont(new Font("calibri", Font.BOLD, 30));
        victory.setLocation(WIDTH / 2 - victory.getWidth() / 2, 
                HEIGHT / 2 - victory.getHeight());
        add(victory);

        GLabel maxComboLabel = new GLabel("MAX COMBO: " + maxCombo); 
        maxComboLabel.setColor(Color.RED);
        maxComboLabel.setFont(new Font("calibri", Font.BOLD, 30));
        maxComboLabel.setLocation(WIDTH / 2 - maxComboLabel.getWidth() / 2, 
                HEIGHT / 2);
        add(maxComboLabel);

        // Calculate and display ball scores.
        if (lives == 3) {
            ball1Score = score;
        }
        else if (lives == 2) {
            ball2Score = score - ball1Score;
        }
        else if (lives == 1) {
            ball3Score = score - ball2Score - ball1Score;
        }
        displayBallScores();
        
        // Check for a new high score.
        if (score > highScore) {
            updateHighScore(score, "highScore.txt");
        }
        
        resetGame();
    }

    
    /**
     * Display the ball scores.
     */
    public void displayBallScores() {
        ball1Label.setLabel("Ball 1 Score: " + ball1Score);
        ball2Label.setLabel("Ball 2 Score: " + ball2Score);
        ball3Label.setLabel("Ball 3 Score: " + ball3Score);

        ball1Label.setVisible(true);
        ball2Label.setVisible(true);
        ball3Label.setVisible(true);
    }
    
    
    /**
     * Reset the game for a new round.
     */
    public void resetGame() {
        GLabel playAgainLabel = new GLabel("Click anywhere to play again.");
        playAgainLabel.setColor(Color.RED);
        playAgainLabel.setLocation(WIDTH / 2 - playAgainLabel.getWidth() / 2, 
                HEIGHT / 2 + playAgainLabel.getHeight() * 2);
        add(playAgainLabel);
        
        lives = 3;
        score = 0;
        nbricks = NBRICKS_PER_ROW * NBRICK_ROWS;
        maxCombo = 1;
        waitForClick();
        run();
    }
    

    // getCollidingObject -- called from the playBall method
    // discovers and returns the object that the ball collided with
    private GObject getCollidingObject() {
        if (getElementAt(ball.getX(), ball.getY()) != null)
            return getElementAt(ball.getX(), ball.getY());
        else if (getElementAt(ball.getX()+BALL_RADIUS*2, ball.getY()) != null)
            return getElementAt(ball.getX()+BALL_RADIUS*2, ball.getY());
        else if (getElementAt(ball.getX()+BALL_RADIUS*2, ball.getY()+BALL_RADIUS*2) != null)
            return getElementAt(ball.getX()+BALL_RADIUS*2, ball.getY()+BALL_RADIUS*2);
        else if (getElementAt(ball.getX(), ball.getY()+BALL_RADIUS*2) != null)
            return getElementAt(ball.getX(), ball.getY()+BALL_RADIUS*2);
        else  
            return null;
    }

    // mouseMoved method -- called by the mouseListener when the mouse is moved
    // anywhere within the boundaries of the run window
    public void mouseMoved(MouseEvent e) {
        // only move the paddle every 5th mouse event 
        // otherwise the play slows every time the mouse moves
        if (toggle == 5) {
            // get the x-coordinate of the mouse
            double eX = e.getX();

            // if the mouse moved to the right
            if (eX - lastX > 0) {
                // if paddle is not already at the right wall
                if (paddle.getX() < WIDTH - PADDLE_WIDTH) {
                    // move to the right
                    paddle.move(eX - lastX, 0);
                    // if the ball has not been started yet, move the ball to the right
                    if (!ballStarted) {
                        ball.move(eX - lastX, 0);
                    }
                }
            }
            // if the mouse moved to the left
            else {
                // if paddle is not already at the left wall
                if (paddle.getX() > 0) {
                    // move to the left
                    paddle.move(eX - lastX, 0);
                    // if the ball has not been started yet, move the ball to the left
                    if (!ballStarted) {
                        ball.move(eX - lastX, 0);
                    }
                }
            }

            // record this mouse x position for next mouse event           
            GPoint last = new GPoint(e.getPoint());
            lastX = last.getX();

            // reset toggle to 1 
            toggle = 1;
        }
        else {
            // increment toggle by 1
            // (when toggle gets to 5 the code will move the paddle 
            //  and reset toggle back to 1)
            toggle++;
        }
    }
}