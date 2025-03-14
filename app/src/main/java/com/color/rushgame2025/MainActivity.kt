package com.color.rushgame2025

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class MainActivity : Activity() {
    private lateinit var gameView: BounceMasterView
    private lateinit var uiManager: UIManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the app fullscreen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Create main layout
        val mainLayout = FrameLayout(this)

        // Create and add game view
        gameView = BounceMasterView(this)
        mainLayout.addView(gameView)

        // Create and add UI manager
        uiManager = UIManager(this, gameView)
        mainLayout.addView(uiManager)

        // Set content view
        setContentView(mainLayout)
    }

    override fun onPause() {
        super.onPause()
        gameView.pauseGame()
    }

    override fun onResume() {
        super.onResume()
        if (!gameView.isGameOver) {
            gameView.resumeGame()
        }
    }
}

// UI Manager - handles all UI elements overlaid on the game
class UIManager(context: Context, private val gameView: BounceMasterView) : FrameLayout(context) {
    // UI elements
    private val scoreTextPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = 60f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(5f, 3f, 3f, Color.BLACK)
    }

    private val highScoreTextPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = 30f
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 2f, 2f, Color.BLACK)
    }

    private val comboTextPaint = Paint().apply {
        isAntiAlias = true
        color = Color.YELLOW
        textSize = 40f
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 2f, 2f, Color.BLACK)
    }

    private val buttonPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
    }

    private val buttonTextPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val gameOverPanelPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#CC000000")
        style = Paint.Style.FILL
    }

    private val gameOverCardPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(10f, 0f, 5f, Color.parseColor("#66000000"))
    }

    private val titleTextPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#FF5252")
        textSize = 70f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val infoTextPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#333333")
        textSize = 35f
        textAlign = Paint.Align.CENTER
    }

    // Game state
    private var score = 0
    private var highScore = 0
    private var combo = 1
    private var isGameOverShowing = false

    // UI layout variables
    private var buttonRect = RectF()
    private var gameOverCardRect = RectF()
    private var screenWidth = 0f
    private var screenHeight = 0f

    // Animation values
    private var gameOverAlpha = 0f
    private var scoreScale = 1f
    private var scoreScaleAnimator: ValueAnimator? = null
    private var comboPulseAnimator: ValueAnimator? = null
    private var comboScale = 1f

    init {
        // Make this view not block touches to views behind it
        setWillNotDraw(false)

        // Load high score
        val prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        highScore = prefs.getInt("high_score", 0)

        // Set up game callbacks
        gameView.setScoreUpdateListener { newScore, newCombo ->
            if (newScore > score) {
                // Pulse score animation
                scoreScaleAnimator?.cancel()
                scoreScaleAnimator = ValueAnimator.ofFloat(1.3f, 1f).apply {
                    duration = 300
                    interpolator = OvershootInterpolator()
                    addUpdateListener {
                        scoreScale = it.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }

            // Update combo animation
            if (newCombo > combo) {
                comboPulseAnimator?.cancel()
                comboPulseAnimator = ValueAnimator.ofFloat(1.5f, 1f).apply {
                    duration = 400
                    interpolator = OvershootInterpolator()
                    addUpdateListener {
                        comboScale = it.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }

            score = newScore
            combo = newCombo
            invalidate()
        }

        gameView.setGameOverListener { finalScore ->
            // Update high score if needed
            if (finalScore > highScore) {
                highScore = finalScore
                val prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("high_score", highScore).apply()
            }

            // Show game over UI with animation
            isGameOverShowing = true
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 500
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    gameOverAlpha = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w.toFloat()
        screenHeight = h.toFloat()

        // Calculate game over card position
        val cardWidth = screenWidth * 0.8f
        val cardHeight = screenHeight * 0.4f
        val cardLeft = (screenWidth - cardWidth) / 2
        val cardTop = (screenHeight - cardHeight) / 2
        gameOverCardRect.set(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight)

        // Calculate replay button position
        val buttonWidth = cardWidth * 0.6f
        val buttonHeight = 120f
        val buttonLeft = screenWidth / 2 - buttonWidth / 2
        val buttonTop = cardTop + cardHeight - buttonHeight - 50f
        buttonRect.set(buttonLeft, buttonTop, buttonLeft + buttonWidth, buttonTop + buttonHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw score
        canvas.save()
        canvas.scale(scoreScale, scoreScale, screenWidth / 2, 80f)
        canvas.drawText(score.toString(), screenWidth / 2, 80f, scoreTextPaint)
        canvas.restore()

        // Draw high score
        canvas.drawText("Best: $highScore", screenWidth / 2, 140f, highScoreTextPaint)

        // Draw combo if active
        if (combo > 1) {
            canvas.save()
            canvas.scale(comboScale, comboScale, screenWidth / 2, 200f)
            canvas.drawText("x$combo COMBO!", screenWidth / 2, 200f, comboTextPaint)
            canvas.restore()
        }

        // Draw game over UI if showing
        if (isGameOverShowing) {
            // Semi-transparent background
            gameOverPanelPaint.alpha = (gameOverAlpha * 200).toInt()
            canvas.drawRect(0f, 0f, screenWidth, screenHeight, gameOverPanelPaint)

            // Game over card
            gameOverCardPaint.alpha = (gameOverAlpha * 255).toInt()
            canvas.drawRoundRect(gameOverCardRect, 30f, 30f, gameOverCardPaint)

            // Title
            titleTextPaint.alpha = (gameOverAlpha * 255).toInt()
            canvas.drawText("GAME OVER", screenWidth / 2, gameOverCardRect.top + 80f, titleTextPaint)

            // Score info
            infoTextPaint.alpha = (gameOverAlpha * 255).toInt()
            canvas.drawText("Score: $score", screenWidth / 2, gameOverCardRect.top + 160f, infoTextPaint)

            // High score
            val highScoreText = if (score >= highScore) "NEW BEST SCORE!" else "Best: $highScore"
            canvas.drawText(highScoreText, screenWidth / 2, gameOverCardRect.top + 220f, infoTextPaint)

            // Replay button
            buttonPaint.alpha = (gameOverAlpha * 255).toInt()
            buttonTextPaint.alpha = (gameOverAlpha * 255).toInt()
            canvas.drawRoundRect(buttonRect, 20f, 20f, buttonPaint)
            canvas.drawText("PLAY AGAIN", screenWidth / 2, buttonRect.centerY() + 15f, buttonTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isGameOverShowing && event.action == MotionEvent.ACTION_DOWN) {
            if (buttonRect.contains(event.x, event.y)) {
                // Restart the game
                isGameOverShowing = false
                score = 0
                combo = 1
                gameView.restartGame()
                invalidate()
                return true
            }
        }

        // Pass touch events to the game view
        return gameView.onTouchEvent(event) || super.onTouchEvent(event)
    }
}

// The main game view
class BounceMasterView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    // Game thread and state
    private var gameThread: Thread? = null
    private var isRunning = false
    private var isPaused = false
    var isGameOver = false
        private set

    // Game objects
    private val player = Player()
    private val platforms = mutableListOf<Platform>()
    private val collectibles = mutableListOf<Collectible>()
    private val obstacles = mutableListOf<Obstacle>()
    private val particles = mutableListOf<Particle>()
    private val powerups = mutableListOf<PowerUp>()

    // Game state
    private var score = 0
    private var comboMultiplier = 1
    private var comboTimer = 0f
    private var gameSpeed = 1f
    private var distanceTraveled = 0f
    private var platformTypes = mutableListOf(PlatformType.NORMAL)
    private var currentLevel = 1
    private var invincibleTimer = 0f
    private var powerUpActive = PowerUpType.NONE
    private var powerUpTimer = 0f

    // Graphics
    private val paint = Paint().apply { isAntiAlias = true }
    private val particlePaint = Paint().apply { isAntiAlias = true }

    // Screen dimensions
    private var screenWidth = 0f
    private var screenHeight = 0f

    // Companion object for static properties
    companion object {
        var screenWidth = 0f
    }

    // Colors
    private val colors = listOf(
        Color.parseColor("#FF5252"),  // Red
        Color.parseColor("#FFEB3B"),  // Yellow
        Color.parseColor("#4CAF50"),  // Green
        Color.parseColor("#2196F3"),  // Blue
        Color.parseColor("#9C27B0")   // Purple
    )

    // Game timing
    private var lastFrameTime = System.currentTimeMillis()
    private var platformSpawnTimer = 0f
    private var collectibleSpawnTimer = 0f
    private var obstacleSpawnTimer = 0f
    private var powerUpSpawnTimer = 0f

    // Animations
    private var screenShake = 0f
    private var screenShakeDecay = 0.9f
    private var backgroundOffset = 0f

    // Callbacks
    private var scoreUpdateListener: ((Int, Int) -> Unit)? = null
    private var gameOverListener: ((Int) -> Unit)? = null

    // Touch handling
    private var lastTouchX = 0f

    init {
        holder.addCallback(this)
        isFocusable = true

        // Initialize game
        initializeGame()
    }

    fun setScoreUpdateListener(listener: (Int, Int) -> Unit) {
        scoreUpdateListener = listener
    }

    fun setGameOverListener(listener: (Int) -> Unit) {
        gameOverListener = listener
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
        // Update static screenWidth
        BounceMasterView.screenWidth = screenWidth
        if (!isPaused) {
            startGame()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
        // Update static screenWidth
        BounceMasterView.screenWidth = screenWidth
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pauseGame()
    }

    fun startGame() {
        if (gameThread == null) {
            // Start game loop
            isRunning = true
            gameThread = Thread(this)
            gameThread?.start()
        }
    }

    fun pauseGame() {
        isPaused = true
        isRunning = false
        try {
            gameThread?.join()
            gameThread = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun resumeGame() {
        if (isPaused) {
            isPaused = false
            startGame()
        }
    }

    fun restartGame() {
        pauseGame()
        initializeGame()
        startGame()
    }

    private fun initializeGame() {
        // Reset game state
        score = 0
        isGameOver = false
        comboMultiplier = 1
        comboTimer = 0f
        gameSpeed = 1f
        distanceTraveled = 0f
        currentLevel = 1
        invincibleTimer = 0f
        powerUpActive = PowerUpType.NONE
        powerUpTimer = 0f

        // Reset platform types based on level
        platformTypes.clear()
        platformTypes.add(PlatformType.NORMAL)

        // Initialize player
        player.x = screenWidth / 2
        player.y = screenHeight * 0.3f
        player.color = colors[Random.nextInt(colors.size)]
        player.radius = screenWidth * 0.06f
        player.velocityY = 0f

        // Clear game objects
        platforms.clear()
        collectibles.clear()
        obstacles.clear()
        particles.clear()
        powerups.clear()

        // Create initial platform only if screen dimensions are valid
        if (screenWidth > 0 && screenHeight > 0) {
            val initialPlatform = Platform().apply {
                x = screenWidth / 2
                y = screenHeight * 0.7f
                width = screenWidth * 0.4f
                height = screenHeight * 0.05f
                color = player.color
                type = PlatformType.NORMAL
                speed = 0f
            }
            platforms.add(initialPlatform)

            // Create some initial platforms
            for (i in 1..3) {
                spawnPlatform()
            }
        }

        // Update score UI
        scoreUpdateListener?.invoke(score, comboMultiplier)
    }

    override fun run() {
        while (isRunning) {
            // Calculate delta time
            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastFrameTime) / 1000f
            lastFrameTime = currentTime

            // Update game state
            if (!isGameOver && screenWidth > 0 && screenHeight > 0) {
                update(deltaTime)
            }

            // Draw frame
            try {
                draw()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Control frame rate
            try {
                Thread.sleep(16) // ~60 FPS
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun update(deltaTime: Float) {
        // Update game speed based on score
        gameSpeed = min(3f, 1f + score / 200f)

        // Update distance traveled and check level progression
        distanceTraveled += deltaTime * 10f * gameSpeed
        checkLevelProgression()

        // Update background offset for parallax effect
        backgroundOffset = (backgroundOffset + deltaTime * 50f * gameSpeed) % 100f

        // Update combo timer
        if (comboTimer > 0) {
            comboTimer -= deltaTime
            if (comboTimer <= 0) {
                comboMultiplier = 1
                scoreUpdateListener?.invoke(score, comboMultiplier)
            }
        }

        // Update invincibility timer
        if (invincibleTimer > 0) {
            invincibleTimer -= deltaTime
        }

        // Update power-up timer
        if (powerUpTimer > 0) {
            powerUpTimer -= deltaTime
            if (powerUpTimer <= 0) {
                powerUpActive = PowerUpType.NONE
            }
        }

        // Update screen shake
        if (screenShake > 0.1f) {
            screenShake *= screenShakeDecay
        } else {
            screenShake = 0f
        }

        // Update player
        player.velocityY += 9.8f * 4f * deltaTime // Gravity

        // Apply magnetic power-up effect
        if (powerUpActive == PowerUpType.MAGNET) {
            // Pull nearby collectibles toward the player
            for (collectible in collectibles) {
                val dx = player.x - collectible.x
                val dy = player.y - collectible.y
                val distance = Math.sqrt(dx * dx + dy * dy.toDouble()).toFloat()

                if (distance < screenWidth * 0.4f) {
                    collectible.x += dx * deltaTime * 5f
                    collectible.y += dy * deltaTime * 5f
                }
            }
        }

        player.y += player.velocityY * deltaTime * 60f

        // Check platform collisions
        if (player.velocityY > 0) { // Only check when falling
            for (platform in platforms) {
                if (player.intersects(platform)) {
                    // Special platform effects
                    when (platform.type) {
                        PlatformType.NORMAL -> {
                            // Normal bounce
                            player.velocityY = -15f
                        }
                        PlatformType.BOUNCY -> {
                            // Extra bouncy
                            player.velocityY = -25f
                            createParticles(player.x, player.y, Color.WHITE, 20)
                            addScreenShake(5f)
                        }
                        PlatformType.FRAGILE -> {
                            // Breaks after landing
                            player.velocityY = -15f
                            platform.breaking = true
                            createParticles(platform.x, platform.y, platform.color, 25)
                        }
                        PlatformType.MOVING -> {
                            // Standard bounce
                            player.velocityY = -15f
                        }
                    }

                    // Color mechanics
                    if (player.color == platform.color) {
                        // Matched color - increase combo
                        comboMultiplier = min(10, comboMultiplier + 1)
                        comboTimer = 3f
                        score += 5 * comboMultiplier

                        // Create particles for visual feedback
                        createParticles(player.x, player.y, player.color, 15)
                    } else if (!platform.breaking) {
                        // Mismatched color - reset combo but still get points
                        comboMultiplier = 1
                        comboTimer = 0f
                        score += 1

                        // Change player color to platform color
                        player.changeColor(platform.color)
                    }

                    // Update score UI
                    scoreUpdateListener?.invoke(score, comboMultiplier)
                    break
                }
            }
        }

        // Check collectible collisions
        val collectibleIterator = collectibles.iterator()
        while (collectibleIterator.hasNext()) {
            val collectible = collectibleIterator.next()
            if (player.intersectsCollectible(collectible)) {
                // Collected!
                collectibleIterator.remove()

                // Add score based on collectible type
                when (collectible.type) {
                    CollectibleType.COIN -> {
                        score += 10 * comboMultiplier
                        createParticles(collectible.x, collectible.y, Color.YELLOW, 10)
                    }
                    CollectibleType.GEM -> {
                        score += 25 * comboMultiplier
                        createParticles(collectible.x, collectible.y, Color.CYAN, 15)
                    }
                    CollectibleType.COLOR_CHANGE -> {
                        // Change player color
                        val newColor = colors.filter { it != player.color }.random()
                        player.changeColor(newColor)
                        createParticles(collectible.x, collectible.y, newColor, 20)
                    }
                }

                // Update score UI
                scoreUpdateListener?.invoke(score, comboMultiplier)
            }
        }

        // Check powerup collisions
        val powerUpIterator = powerups.iterator()
        while (powerUpIterator.hasNext()) {
            val powerUp = powerUpIterator.next()
            if (player.intersectsCollectible(powerUp)) {
                // Collected!
                powerUpIterator.remove()

                // Apply power-up effect
                powerUpActive = powerUp.type
                powerUpTimer = 10f // 10 seconds

                when (powerUp.type) {
                    PowerUpType.SHIELD -> {
                        invincibleTimer = 10f
                        createParticles(player.x, player.y, Color.WHITE, 30)
                    }
                    PowerUpType.MAGNET -> {
                        createParticles(player.x, player.y, Color.MAGENTA, 30)
                    }
                    PowerUpType.COIN_MULTIPLIER -> {
                        comboMultiplier = min(10, comboMultiplier + 3)
                        comboTimer = 10f
                        createParticles(player.x, player.y, Color.YELLOW, 30)
                    }
                    PowerUpType.NONE -> { /* Do nothing */ }
                }
            }
        }

        // Check obstacle collisions if not invincible
        if (invincibleTimer <= 0) {
            for (obstacle in obstacles) {
                if (player.intersectsObstacle(obstacle)) {
                    // Hit obstacle!
                    if (powerUpActive == PowerUpType.SHIELD) {
                        // Shield protects - just break the obstacle
                        createParticles(obstacle.x, obstacle.y, Color.RED, 30)
                        obstacles.remove(obstacle)
                        addScreenShake(10f)
                        break
                    } else {
                        // Game over
                        isGameOver = true
                        createParticles(player.x, player.y, player.color, 50)
                        addScreenShake(20f)
                        gameOverListener?.invoke(score)
                        break
                    }
                }
            }
        }

        // Update game objects
        updatePlatforms(deltaTime)
        updateCollectibles(deltaTime)
        updateObstacles(deltaTime)
        updatePowerUps(deltaTime)
        updateParticles(deltaTime)

        // Spawn game objects
        handleObjectSpawning(deltaTime)

        // Check game over conditions
        if (player.y > screenHeight + player.radius) {
            isGameOver = true
            gameOverListener?.invoke(score)
        }
    }

    private fun updatePlatforms(deltaTime: Float) {
        val iterator = platforms.iterator()
        while (iterator.hasNext()) {
            val platform = iterator.next()

            if (platform.breaking) {
                platform.breakingTimer += deltaTime
                if (platform.breakingTimer >= platform.breakingDuration) {
                    iterator.remove()
                    continue
                }
            }

            platform.update(deltaTime, gameSpeed)

            // Remove platforms that are off-screen
            if (platform.y > screenHeight + platform.height) {
                iterator.remove()
            }
        }
    }

    private fun updateCollectibles(deltaTime: Float) {
        val iterator = collectibles.iterator()
        while (iterator.hasNext()) {
            val collectible = iterator.next()
            collectible.update(deltaTime, gameSpeed)

            // Remove collectibles that are off-screen
            if (collectible.y > screenHeight + collectible.radius) {
                iterator.remove()
            }
        }
    }

    private fun updateObstacles(deltaTime: Float) {
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            obstacle.update(deltaTime, gameSpeed)

            // Remove obstacles that are off-screen
            if (obstacle.y > screenHeight + obstacle.height / 2) {
                iterator.remove()
            }
        }
    }

    private fun updatePowerUps(deltaTime: Float) {
        val iterator = powerups.iterator()
        while (iterator.hasNext()) {
            val powerUp = iterator.next()
            powerUp.update(deltaTime, gameSpeed)

            // Remove power-ups that are off-screen
            if (powerUp.y > screenHeight + powerUp.radius) {
                iterator.remove()
            }
        }
    }

    private fun updateParticles(deltaTime: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.update(deltaTime)

            // Remove dead particles
            if (particle.lifetime <= 0) {
                iterator.remove()
            }
        }
    }

    private fun handleObjectSpawning(deltaTime: Float) {
        // Spawn platforms
        platformSpawnTimer -= deltaTime
        if (platformSpawnTimer <= 0) {
            spawnPlatform()
            platformSpawnTimer = (1.0f + Random.nextFloat() * 0.5f) / gameSpeed
        }

        // Spawn collectibles
        collectibleSpawnTimer -= deltaTime
        if (collectibleSpawnTimer <= 0) {
            if (Random.nextFloat() < 0.7f) {
                spawnCollectible()
            }
            collectibleSpawnTimer = (2.0f + Random.nextFloat()) / gameSpeed
        }

        // Spawn obstacles (only after level 2)
        if (currentLevel >= 2) {
            obstacleSpawnTimer -= deltaTime
            if (obstacleSpawnTimer <= 0) {
                if (Random.nextFloat() < 0.5f * (currentLevel - 1) / 5) {
                    spawnObstacle()
                }
                obstacleSpawnTimer = (3.0f + Random.nextFloat() * 2f) / gameSpeed
            }
        }

        // Spawn power-ups (rarely)
        powerUpSpawnTimer -= deltaTime
        if (powerUpSpawnTimer <= 0) {
            if (Random.nextFloat() < 0.2f) {
                spawnPowerUp()
            }
            powerUpSpawnTimer = 10f + Random.nextFloat() * 5f
        }
    }

    private fun checkLevelProgression() {
        // Check if we should advance to next level
        val newLevel = (distanceTraveled / 1000).toInt() + 1
        if (newLevel > currentLevel) {
            currentLevel = newLevel

            // Add new platform types as level increases
            when (currentLevel) {
                2 -> platformTypes.add(PlatformType.MOVING)
                3 -> platformTypes.add(PlatformType.BOUNCY)
                4 -> platformTypes.add(PlatformType.FRAGILE)
            }
        }
    }

    private fun spawnPlatform() {
        // Only spawn if screen dimensions are valid
        if (screenWidth <= 0 || screenHeight <= 0) return
        
        val platform = Platform().apply {
            width = screenWidth * (0.2f + Random.nextFloat() * 0.3f)
            height = screenHeight * 0.05f
            x = Random.nextFloat() * (screenWidth - width) + width / 2
            y = -height

            // Determine platform type based on current level
            type = platformTypes.random()

            // Determine platform color
            val sameColorChance = max(0.3f, 1f - score / 300f)
            color = if (Random.nextFloat() < sameColorChance) {
                player.color
            } else {
                colors.filter { it != player.color }.random()
            }

            // Configure platform based on type
            when (type) {
                PlatformType.MOVING -> {
                    speed = (Random.nextFloat() * 2f - 1f) * 4f * gameSpeed
                }
                PlatformType.BOUNCY -> {
                    // Bouncy platforms are smaller
                    width *= 0.8f
                }
                PlatformType.FRAGILE -> {
                    // Fragile platforms are smaller
                    width *= 0.7f
                }
                PlatformType.NORMAL -> {
                    // Normal platforms are just normal
                }
            }
        }
        platforms.add(platform)
    }

    private fun spawnCollectible() {
        // Only spawn if screen dimensions are valid
        if (screenWidth <= 0 || screenHeight <= 0) return
        
        val collectible = Collectible().apply {
            x = Random.nextFloat() * screenWidth
            y = -100f
            radius = screenWidth * 0.03f

            // Random collectible type based on rarity
            type = when {
                Random.nextFloat() < 0.1f -> CollectibleType.GEM
                Random.nextFloat() < 0.2f -> CollectibleType.COLOR_CHANGE
                else -> CollectibleType.COIN
            }

            // Set color based on type
            color = when (type) {
                CollectibleType.COIN -> Color.YELLOW
                CollectibleType.GEM -> Color.CYAN
                CollectibleType.COLOR_CHANGE -> colors.random()
            }
        }
        collectibles.add(collectible)
    }

    private fun spawnObstacle() {
        // Only spawn if screen dimensions are valid
        if (screenWidth <= 0 || screenHeight <= 0) return
        
        val obstacle = Obstacle().apply {
            width = screenWidth * (0.1f + Random.nextFloat() * 0.15f)
            height = width
            x = Random.nextFloat() * (screenWidth - width) + width / 2
            y = -height
            color = Color.parseColor("#FF5252") // Red

            // Moving obstacles at higher levels
            if (currentLevel >= 3 && Random.nextFloat() < 0.5f) {
                speed = (Random.nextFloat() * 2f - 1f) * 3f * gameSpeed
            }
        }
        obstacles.add(obstacle)
    }

    private fun spawnPowerUp() {
        // Only spawn if screen dimensions are valid
        if (screenWidth <= 0 || screenHeight <= 0) return
        
        val powerUp = PowerUp().apply {
            x = Random.nextFloat() * screenWidth
            y = -100f
            radius = screenWidth * 0.04f

            // Random power-up type
            type = PowerUpType.values().filter { it != PowerUpType.NONE }.random()

            // Set color based on type
            color = when (type) {
                PowerUpType.SHIELD -> Color.WHITE
                PowerUpType.MAGNET -> Color.MAGENTA
                PowerUpType.COIN_MULTIPLIER -> Color.YELLOW
                PowerUpType.NONE -> Color.WHITE // Should never happen
            }
        }
        powerups.add(powerUp)
    }

    private fun createParticles(x: Float, y: Float, color: Int, count: Int) {
        for (i in 0 until count) {
            val particle = Particle().apply {
                this.x = x
                this.y = y
                this.color = color
                this.size = 3f + Random.nextFloat() * 8f
                this.velocityX = (Random.nextFloat() * 2f - 1f) * 100f
                this.velocityY = (Random.nextFloat() * 2f - 1f) * 100f
                this.lifetime = 0.5f + Random.nextFloat() * 0.5f
            }
            particles.add(particle)
        }
    }

    private fun addScreenShake(amount: Float) {
        screenShake = amount
    }

    private fun draw() {
        // Get canvas from holder and lock it
        val canvas = holder.lockCanvas() ?: return

        try {
            // Apply screen shake
            canvas.save()
            if (screenShake > 0) {
                val shakeX = (Random.nextFloat() * 2f - 1f) * screenShake
                val shakeY = (Random.nextFloat() * 2f - 1f) * screenShake
                canvas.translate(shakeX, shakeY)
            }

            // Draw game background
            drawBackground(canvas)

            // Draw platforms
            for (platform in platforms) {
                drawPlatform(canvas, platform)
            }

            // Draw player
            drawPlayer(canvas)

            // Draw collectibles
            for (collectible in collectibles) {
                drawCollectible(canvas, collectible)
            }

            // Draw obstacles
            for (obstacle in obstacles) {
                drawObstacle(canvas, obstacle)
            }

            // Draw power-ups
            for (powerUp in powerups) {
                drawPowerUp(canvas, powerUp)
            }

            // Draw particles (on top of everything else)
            for (particle in particles) {
                drawParticle(canvas, particle)
            }

            // Restore canvas after shake
            canvas.restore()

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Unlock canvas and post
            try {
                holder.unlockCanvasAndPost(canvas)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun drawBackground(canvas: Canvas) {
        // Clear to background color
        canvas.drawColor(Color.parseColor("#1A237E"))

        // Draw parallax stars
        paint.color = Color.WHITE
        paint.alpha = 100

        for (i in 0 until 50) {
            val x = (i * 37 + 17) % screenWidth
            val y = (i * 23 + 19 + backgroundOffset) % screenHeight
            val size = 2f + (i % 3)
            canvas.drawCircle(x, y, size, paint)
        }
    }

    private fun drawPlayer(canvas: Canvas) {
        // Draw player shadow
        paint.color = Color.BLACK
        paint.alpha = 100
        canvas.drawCircle(player.x + 5, player.y + 5, player.radius, paint)

        // Draw player body
        paint.color = player.color
        paint.alpha = 255
        canvas.drawCircle(player.x, player.y, player.radius, paint)

        // Draw shield effect if invincible
        if (invincibleTimer > 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            paint.color = Color.WHITE
            paint.alpha = (Math.cos(invincibleTimer * 10.0) * 100 + 155).toInt()
            canvas.drawCircle(player.x, player.y, player.radius + 10, paint)
            paint.style = Paint.Style.FILL
        }

        // Draw eyes
        paint.color = Color.WHITE
        val eyeSize = player.radius * 0.3f
        canvas.drawCircle(player.x - player.radius * 0.3f, player.y - player.radius * 0.2f, eyeSize, paint)
        canvas.drawCircle(player.x + player.radius * 0.3f, player.y - player.radius * 0.2f, eyeSize, paint)

        // Draw pupils
        paint.color = Color.BLACK
        val pupilSize = eyeSize * 0.6f
        val pupilOffset = if (player.velocityY < 0) -2f else 2f
        canvas.drawCircle(player.x - player.radius * 0.3f, player.y - player.radius * 0.2f + pupilOffset, pupilSize, paint)
        canvas.drawCircle(player.x + player.radius * 0.3f, player.y - player.radius * 0.2f + pupilOffset, pupilSize, paint)

        // Draw mouth
        val mouthPath = Path()
        if (player.velocityY < 0) {
            // Happy mouth when going up
            mouthPath.moveTo(player.x - player.radius * 0.3f, player.y + player.radius * 0.3f)
            mouthPath.quadTo(player.x, player.y + player.radius * 0.5f, player.x + player.radius * 0.3f, player.y + player.radius * 0.3f)
        } else {
            // Worried mouth when falling
            mouthPath.moveTo(player.x - player.radius * 0.3f, player.y + player.radius * 0.4f)
            mouthPath.quadTo(player.x, player.y + player.radius * 0.2f, player.x + player.radius * 0.3f, player.y + player.radius * 0.4f)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.BLACK
        canvas.drawPath(mouthPath, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawPlatform(canvas: Canvas, platform: Platform) {
        // Draw platform shadow
        paint.color = Color.BLACK
        paint.alpha = 80
        val shadowRect = RectF(
            platform.x - platform.width / 2 + 3,
            platform.y - platform.height / 2 + 3,
            platform.x + platform.width / 2 + 3,
            platform.y + platform.height / 2 + 3
        )
        canvas.drawRoundRect(shadowRect, 10f, 10f, paint)

        // Draw platform
        paint.color = platform.color
        paint.alpha = if (platform.breaking) {
            (255 * (1 - platform.breakingTimer / platform.breakingDuration)).toInt()
        } else {
            255
        }

        val rect = RectF(
            platform.x - platform.width / 2,
            platform.y - platform.height / 2,
            platform.x + platform.width / 2,
            platform.y + platform.height / 2
        )
        canvas.drawRoundRect(rect, 10f, 10f, paint)

        // Draw platform type indicator
        when (platform.type) {
            PlatformType.BOUNCY -> {
                // Draw springs
                paint.color = Color.WHITE
                for (i in 0 until 3) {
                    val springX = platform.x - platform.width * 0.3f + platform.width * 0.3f * i
                    val springY = platform.y - platform.height * 0.2f
                    val springWidth = platform.width * 0.1f
                    canvas.drawRoundRect(
                        RectF(
                            springX - springWidth / 2,
                            springY,
                            springX + springWidth / 2,
                            platform.y + platform.height / 2 - 5
                        ),
                        5f, 5f, paint
                    )
                }
            }
            PlatformType.FRAGILE -> {
                // Draw cracks
                paint.color = Color.BLACK
                paint.alpha = 100
                paint.strokeWidth = 2f
                paint.style = Paint.Style.STROKE

                for (i in 0 until 3) {
                    val startX = platform.x - platform.width * 0.3f + platform.width * 0.3f * i
                    val startY = platform.y - platform.height * 0.3f
                    val endY = platform.y + platform.height * 0.3f

                    val path = Path()
                    path.moveTo(startX, startY)
                    path.lineTo(startX + 5, startY + 5)
                    path.lineTo(startX - 5, endY - 5)
                    path.lineTo(startX, endY)

                    canvas.drawPath(path, paint)
                }

                paint.style = Paint.Style.FILL
                paint.alpha = 255
            }
            PlatformType.MOVING -> {
                // Draw arrows indicating movement direction
                paint.color = Color.WHITE

                val arrowPath = Path()
                val arrowSize = platform.width * 0.1f

                if (platform.speed > 0) {
                    // Right arrow
                    arrowPath.moveTo(platform.x + arrowSize, platform.y)
                    arrowPath.lineTo(platform.x, platform.y - arrowSize / 2)
                    arrowPath.lineTo(platform.x, platform.y + arrowSize / 2)
                    arrowPath.close()
                } else {
                    // Left arrow
                    arrowPath.moveTo(platform.x - arrowSize, platform.y)
                    arrowPath.lineTo(platform.x, platform.y - arrowSize / 2)
                    arrowPath.lineTo(platform.x, platform.y + arrowSize / 2)
                    arrowPath.close()
                }

                canvas.drawPath(arrowPath, paint)
            }
            PlatformType.NORMAL -> {
                // No special indicators for normal platforms
            }
        }
    }

    private fun drawCollectible(canvas: Canvas, collectible: Collectible) {
        // Draw shadow
        paint.color = Color.BLACK
        paint.alpha = 80
        canvas.drawCircle(collectible.x + 3, collectible.y + 3, collectible.radius, paint)

        // Draw collectible
        paint.color = collectible.color
        paint.alpha = 255

        when (collectible.type) {
            CollectibleType.COIN -> {
                canvas.drawCircle(collectible.x, collectible.y, collectible.radius, paint)

                // Draw coin details
                paint.color = Color.parseColor("#FFD700") // Darker gold
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                canvas.drawCircle(collectible.x, collectible.y, collectible.radius * 0.7f, paint)
                paint.style = Paint.Style.FILL
            }
            CollectibleType.GEM -> {
                // Draw gem as a diamond shape
                val path = Path()
                path.moveTo(collectible.x, collectible.y - collectible.radius)
                path.lineTo(collectible.x + collectible.radius, collectible.y)
                path.lineTo(collectible.x, collectible.y + collectible.radius)
                path.lineTo(collectible.x - collectible.radius, collectible.y)
                path.close()
                canvas.drawPath(path, paint)

                // Draw gem sparkle
                paint.color = Color.WHITE
                paint.alpha = 200
                canvas.drawCircle(
                    collectible.x - collectible.radius * 0.3f,
                    collectible.y - collectible.radius * 0.3f,
                    collectible.radius * 0.15f,
                    paint
                )
            }
            CollectibleType.COLOR_CHANGE -> {
                // Draw as a star
                val outerRadius = collectible.radius
                val innerRadius = collectible.radius * 0.5f
                val path = Path()

                for (i in 0 until 10) {
                    val angle = Math.PI * 2 * i / 10
                    val radius = if (i % 2 == 0) outerRadius else innerRadius
                    val x = collectible.x + (radius * Math.cos(angle)).toFloat()
                    val y = collectible.y + (radius * Math.sin(angle)).toFloat()

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                path.close()
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun drawObstacle(canvas: Canvas, obstacle: Obstacle) {
        // Draw shadow
        paint.color = Color.BLACK
        paint.alpha = 80
        val shadowRect = RectF(
            obstacle.x - obstacle.width / 2 + 5,
            obstacle.y - obstacle.height / 2 + 5,
            obstacle.x + obstacle.width / 2 + 5,
            obstacle.y + obstacle.height / 2 + 5
        )
        canvas.drawRoundRect(shadowRect, 8f, 8f, paint)

        // Draw obstacle
        paint.color = obstacle.color
        paint.alpha = 255
        val rect = RectF(
            obstacle.x - obstacle.width / 2,
            obstacle.y - obstacle.height / 2,
            obstacle.x + obstacle.width / 2,
            obstacle.y + obstacle.height / 2
        )
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        // Draw spikes
        paint.color = Color.WHITE
        val path = Path()

        for (i in 0 until 3) {
            val startX = obstacle.x - obstacle.width * 0.25f + obstacle.width * 0.25f * i
            val startY = obstacle.y + obstacle.height * 0.1f

            path.moveTo(startX, startY)
            path.lineTo(startX - obstacle.width * 0.1f, startY + obstacle.height * 0.2f)
            path.lineTo(startX + obstacle.width * 0.1f, startY + obstacle.height * 0.2f)
            path.close()
        }

        canvas.drawPath(path, paint)
    }

    private fun drawPowerUp(canvas: Canvas, powerUp: PowerUp) {
        // Draw shadow
        paint.color = Color.BLACK
        paint.alpha = 80
        canvas.drawCircle(powerUp.x + 3, powerUp.y + 3, powerUp.radius, paint)

        // Draw power-up
        paint.color = powerUp.color
        paint.alpha = 255
        canvas.drawCircle(powerUp.x, powerUp.y, powerUp.radius, paint)

        // Draw icon based on type
        paint.color = Color.WHITE
        when (powerUp.type) {
            PowerUpType.SHIELD -> {
                // Draw shield icon
                canvas.drawCircle(powerUp.x, powerUp.y, powerUp.radius * 0.6f, paint)
                paint.color = powerUp.color
                canvas.drawCircle(powerUp.x, powerUp.y, powerUp.radius * 0.4f, paint)
            }
            PowerUpType.MAGNET -> {
                // Draw magnet icon
                val path = Path()
                val width = powerUp.radius * 0.6f
                path.moveTo(powerUp.x - width, powerUp.y - width * 0.5f)
                path.lineTo(powerUp.x - width, powerUp.y + width * 0.5f)
                path.lineTo(powerUp.x, powerUp.y + width * 0.5f)
                path.lineTo(powerUp.x, powerUp.y - width * 0.5f)
                path.close()

                canvas.drawPath(path, paint)

                path.reset()
                path.moveTo(powerUp.x, powerUp.y - width * 0.5f)
                path.lineTo(powerUp.x, powerUp.y + width * 0.5f)
                path.lineTo(powerUp.x + width, powerUp.y + width * 0.5f)
                path.lineTo(powerUp.x + width, powerUp.y - width * 0.5f)
                path.close()

                paint.color = Color.RED
                canvas.drawPath(path, paint)
            }
            PowerUpType.COIN_MULTIPLIER -> {
                // Draw x2 text
                paint.textSize = powerUp.radius * 1.2f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("x2", powerUp.x, powerUp.y + powerUp.radius * 0.4f, paint)
            }
            PowerUpType.NONE -> {
                // Should never happen
            }
        }
    }

    private fun drawParticle(canvas: Canvas, particle: Particle) {
        particlePaint.color = particle.color
        particlePaint.alpha = (255 * (particle.lifetime / particle.maxLifetime)).toInt()
        canvas.drawCircle(particle.x, particle.y, particle.size, particlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isGameOver) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - lastTouchX
                player.x += deltaX

                // Keep player within screen bounds
                player.x = max(player.radius, min(screenWidth - player.radius, player.x))

                lastTouchX = event.x
                return true
            }
        }

        return super.onTouchEvent(event)
    }
}

// Game objects
class Player {
    var x = 0f
    var y = 0f
    var radius = 30f
    var color = Color.RED
    var velocityY = 0f

    fun intersects(platform: Platform): Boolean {
        // Check if player is colliding with platform from above
        return y + radius > platform.y - platform.height / 2 &&
                y + radius < platform.y &&
                x > platform.x - platform.width / 2 &&
                x < platform.x + platform.width / 2
    }

    fun intersectsCollectible(collectible: Collectible): Boolean {
        val dx = x - collectible.x
        val dy = y - collectible.y
        val distance = Math.sqrt(dx * dx + dy * dy.toDouble()).toFloat()
        return distance < radius + collectible.radius
    }

    fun intersectsCollectible(powerUp: PowerUp): Boolean {
        val dx = x - powerUp.x
        val dy = y - powerUp.y
        val distance = Math.sqrt(dx * dx + dy * dy.toDouble()).toFloat()
        return distance < radius + powerUp.radius
    }

    fun intersectsObstacle(obstacle: Obstacle): Boolean {
        // Check if player overlaps with the obstacle's rectangle
        return x + radius > obstacle.x - obstacle.width / 2 &&
                x - radius < obstacle.x + obstacle.width / 2 &&
                y + radius > obstacle.y - obstacle.height / 2 &&
                y - radius < obstacle.y + obstacle.height / 2
    }

    fun changeColor(newColor: Int) {
        color = newColor
    }
}

class Platform {
    var x = 0f
    var y = 0f
    var width = 100f
    var height = 20f
    var color = Color.GREEN
    var type = PlatformType.NORMAL
    var speed = 0f
    var breaking = false
    var breakingTimer = 0f
    var breakingDuration = 0.5f

    fun update(deltaTime: Float, gameSpeed: Float) {
        // Move platform down
        y += 120f * deltaTime * gameSpeed

        // Move horizontally if it's a moving platform
        if (type == PlatformType.MOVING) {
            x += speed * deltaTime

            // Bounce off edges
            if (x - width / 2 < 0 || x + width / 2 > BounceMasterView.screenWidth) {
                speed *= -1
            }
        }
    }
}

class Collectible {
    var x = 0f
    var y = 0f
    var radius = 20f
    var color = Color.YELLOW
    var type = CollectibleType.COIN

    fun update(deltaTime: Float, gameSpeed: Float) {
        // Move collectible down
        y += 100f * deltaTime * gameSpeed
    }
}

class Obstacle {
    var x = 0f
    var y = 0f
    var width = 50f
    var height = 50f
    var color = Color.RED
    var speed = 0f

    fun update(deltaTime: Float, gameSpeed: Float) {
        // Move obstacle down
        y += 150f * deltaTime * gameSpeed

        // Move horizontally if it has speed
        if (speed != 0f) {
            x += speed * deltaTime

            // Bounce off edges
            if (x - width / 2 < 0 || x + width / 2 > BounceMasterView.screenWidth) {
                speed *= -1
            }
        }
    }
}

class PowerUp {
    var x = 0f
    var y = 0f
    var radius = 25f
    var color = Color.WHITE
    var type = PowerUpType.NONE

    fun update(deltaTime: Float, gameSpeed: Float) {
        // Move power-up down
        y += 80f * deltaTime * gameSpeed
    }
}

class Particle {
    var x = 0f
    var y = 0f
    var velocityX = 0f
    var velocityY = 0f
    var color = Color.WHITE
    var size = 5f
    var lifetime = 1f
    var maxLifetime = 1f

    init {
        maxLifetime = lifetime
    }

    fun update(deltaTime: Float) {
        x += velocityX * deltaTime
        y += velocityY * deltaTime
        velocityY += 100f * deltaTime // Gravity
        lifetime -= deltaTime
    }
}

// Enums
enum class PlatformType {
    NORMAL,
    BOUNCY,
    FRAGILE,
    MOVING
}

enum class CollectibleType {
    COIN,
    GEM,
    COLOR_CHANGE
}

enum class PowerUpType {
    NONE,
    SHIELD,
    MAGNET,
    COIN_MULTIPLIER
}