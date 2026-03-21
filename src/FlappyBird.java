import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;

public class FlappyBird extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
    private static final Color LARANJA_PADRAO = new Color(251, 196, 2);
    private static final int BTN_LARGE = 220;
    private static final int BTN_MEDIUM = 140;
    private static final int BTN_SMALL = 120;
    private static final int BTN_HEIGHT = 50;
    private static final int BTN_HEIGHT_CONFIRM = 50;
    private static final int CONFIG_BOX_X = 30;
    private static final int CONFIG_BOX_Y = 190;
    private static final int CONFIG_BOX_WIDTH = 360 - 60;
    private static final int CONFIG_BOX_HEIGHT = 300;
    private static final int HIGHLIGHT_WIDTH = 320;
    private static final int HIGHLIGHT_HEIGHT = 50;
    private static final int SELECTOR_RADIUS = 8;
    private static final int RADIO_RADIUS = 10;
    private static final int VOLUME_KNOB_SIZE = 20;
    private static final int BAR_HEIGHT = 12;
    private static final int VOLUME_BAR_HIT_MARGIN_X = 14;
    private static final int VOLUME_BAR_HIT_MARGIN_Y = 18;
    private static final long VOLUME_REPAINT_DEBOUNCE_MS = 0;
    enum GameState {
        MENU_PRINCIPAL, NEW_GAME, CONTINUE, EDIT_PROFILE, CONFIRM_DELETE_PROFILE,
        CONFIRM_EDIT_PROFILE, CONFIRM_START_GAME, DIGITAR_NOME_PERFIL, JOGANDO,
        GAME_OVER, CONFIGURACOES, RECORDS
    }

    enum ControlMode {
        KEYBOARD_ONLY, MOUSE_ONLY, KEYBOARD_AND_MOUSE
    }

    private static class MenuOption {
        String text;
        int x, y, width, height;
        Runnable action;
        MenuOption(String text, int x, int y, int width, int height, Runnable action) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.action = action;
        }
        boolean contains(int mx, int my) {
            return mx >= x && mx <= x + width && my >= (y - height) && my <= y + 10;
        }
    }

    static class GlobalHighScore {
        String name;
        int score;
        GlobalHighScore(String name, int score, String mode) {
            this.name = name;
            this.score = score;
        }
    }

    static class PlayerProfile {
        String name = "";
        ArrayList<Integer> scores = new ArrayList<>();
        boolean isEmpty() {
            return name.trim().isEmpty();
        }
    }

    private boolean isEditingName = false;
    private GameState currentState = GameState.MENU_PRINCIPAL;
    private int selectedMenuItem = 0;
    private int selectedSlot = 0;
    private int selectedSlotNormal = 0;
    private int selectedSlotHard = 0;
    private boolean abaNormalAtiva = true;
    private String typingName = "";
    private int highScoreNormalGlobal = 0;
    private int highScoreHardGlobal = 0;
    private int selectedGameOverOption = 0;
    private int selectedConfigOption = 0;
    private ControlMode controlMode;
    private boolean paused = false;
    private boolean pauseMenuActive = false;
    private int selectedPauseOption = 0;
    private boolean cameFromPauseToConfig = false;
    private int scrollOffsetNormal = 0;
    private int scrollOffsetHard = 0;
    private int configScrollOffset = 0;
    private int recordsScrollOffset = 0;
    private final int SLOT_HEIGHT = 60;
    private final int VISIBLE_SLOTS = 3;
    private ControlMode tempControlMode;
    private float tempVolumeEffects = 1.0f;
    private float tempVolumeMusic = 1.0f;
    private float prevVolumeEffects = 1.0f;
    private float prevVolumeMusic = 1.0f;
    private int tempJumpKey;
    private int tempPauseKey;
    private boolean configFocusOnSettings = true;
    private int selectedButtonIndex = 0;
    private long lastVolumeRepaintTime = 0;
    private boolean recordsFocusOnTabs = true;
    private boolean slotsFocusOnBack = false;
    private boolean slotsFocusOnTabs = true;
    private Map<Integer, Long> keyPressTimestamps = new HashMap<>();
    private static final long KEY_FLASH_DURATION = 200;
    private boolean shiftActive = false;
    private boolean simbolosAtivo = false;
    private boolean physicalShiftHeld = false;
    private boolean capsLockActive = false;
    private boolean showOnlyCurrentProfileRecords = false;
    private boolean recordsVindoDoGameOver = false;
    private GameState previousStateBeforeRecords = null;
    private int lastPlayedSlot = -1;
    private boolean lastPlayedWasHard = false;
    private int lastMenuPrincipalIndex = -1;
    private int lastSlotsTabIndex = -1;
    private int lastSlotsBackIndex = -1;
    private int lastRecordsTabIndex = -1;
    private int lastConfigButtonIndex = -1;
    private int lastEditProfileIndex = -1;
    private int lastDigitarButtonIndex = -1;
    private int lastConfirmIndex = -1;
    private int lastPauseIndex = -1;
    private int lastGameOverIndex = -1;
    private ArrayList<PlayerProfile> profilesNormal = new ArrayList<>();
    private ArrayList<PlayerProfile> profilesHard = new ArrayList<>();
    private ArrayList<PlayerProfile> currentProfileList;
    private String statusMessage = "";
    private long statusMessageStartTime = 0;
    private static final long MESSAGE_DURATION_MS = 1000;
    private float volumeEffects = 1.0f;
    private float volumeMusic = 1.0f;
    private int jumpKey = KeyEvent.VK_SPACE;
    private int pauseKey = KeyEvent.VK_P;
    private boolean confirmStartFromContinue = false;
    private ArrayList<GlobalHighScore> topNormal = new ArrayList<>();
    private ArrayList<GlobalHighScore> topHard = new ArrayList<>();
    private boolean recordsAbaNormal = true;
    int boardWidth = 360;
    int boardHeight = 640;
    Image backgroundImg;
    Image[] birdImg = new Image[3];
    int currentWingFrame = 0;
    int animationCounter = 0;
    int animationSpeed = 6;
    Image topPipeImg;
    Image bottomPipeImg;
    Image bulletBillImg;
    Image firstPlaceMedal;
    Image secondPlaceMedal;
    Image thirdPlaceMedal;
    Image keyboardArrowDown;
    Image keyboardArrowUp;
    Image altoFalanteAtivo;
    Image altoFalanteSilenciado;
    private boolean keyboardVisible = true;
    private Clip[] wingClips = new Clip[5];
    private Clip[] pointClips = new Clip[4];
    private Clip[] hitClips = new Clip[3];
    private Clip[] dieClips = new Clip[3];
    private Clip[] swooshingClips = new Clip[4];
    private Clip[] buttonClips = new Clip[3];
    private Clip[] bulletCanonClips = new Clip[3];
    private Clip backgroundMusicClip;
    private Clip themeMusicClip;
    private int nextWing = 0;
    private int nextPoint = 0;
    private int nextHit = 0;
    private int nextDie = 0;
    private int nextSwooshing = 0;
    private int nextButton = 0;
    private int nextBulletCanon = 0;
    double birdRotation = 0;
    int countdown = -1;
    Timer countdownTimer;
    int showGoForFrames = 0;
    double backGround1 = 0.0;
    double backGround2;
    double backGround3;
    double backGroundSpeed = -2.0;
    private float menuBackgroundSpeed = -0.8f;
    int birdx = boardWidth / 8;
    int birdy = boardHeight / 2;
    int birdWidth = 34;
    int birdHeight = 24;
    int velocityY = 0;
    int gravity = 1;
    int jumpStrength = -9;
    ArrayList<Pipe> pipes = new ArrayList<>();
    ArrayList<BulletBill> bullets = new ArrayList<>();
    Random random = new Random();
    long lastUpdateTime = System.currentTimeMillis();
    long pipeInterval = 1400;
    long bulletInterval = 3000;
    long pipeAccumulator = 0;
    long bulletAccumulator = 0;
    Timer gameLoop;
    
    
    int score = 0;
    private static final int LIMIT_TOP = -420;
    private static final int LIMIT_BOTTOM = -180;
    private boolean modoDificil = false;
    private static final int PAUSE_FRAMES = 45;
    private final ArrayList<MenuOption> clickableOptions = new ArrayList<>();
    class Bird {
        int x = birdx;
        int y = birdy;
        int width = birdWidth;
        int height = birdHeight;
        Image img;
        Bird(Image img) { this.img = img; }
    }

    class Pipe {
        int x;
        int y = 0;
        int width = 64;
        int height = 512;
        Image img;
        boolean passed = false;
        private float verticalSpeed = 0f;
        private int direction = 0;
        private static final float BASE_SPEED = 1.9f;
        private float movementAccumulator = 0f;
        private boolean isPaused = false;
        private int pauseCounter = 0;
        Pipe(Image img, boolean isHardMode) {
            this.img = img;
            this.x = boardWidth + 80;
            if (isHardMode) {
                verticalSpeed = BASE_SPEED;
                direction = random.nextBoolean() ? 1 : -1;
                movementAccumulator = 0f;
                isPaused = false;
                pauseCounter = 0;
            }
        }
        void updateVertical() {
            if (verticalSpeed == 0f) return;
            if (isPaused) {
                pauseCounter--;
                if (pauseCounter <= 0) {
                    isPaused = false;
                    movementAccumulator = 0f;
                }
                return;
            }
            movementAccumulator += verticalSpeed * direction;
            int movementThisFrame = (int) movementAccumulator;
            y += movementThisFrame;
            movementAccumulator -= movementThisFrame;
            boolean reachedLimit = false;
            if (y <= LIMIT_TOP) {
                y = LIMIT_TOP;
                reachedLimit = true;
            } else if (y >= LIMIT_BOTTOM) {
                y = LIMIT_BOTTOM;
                reachedLimit = true;
            }
            if (reachedLimit) {
                float chance = random.nextFloat();
                if (chance < 0.35f) {
                    isPaused = true;
                    pauseCounter = PAUSE_FRAMES;
                } else if (chance < 0.35f + 0.325f) {
                    direction = -1;
                    movementAccumulator = 0f;
                } else {
                    direction = 1;
                    movementAccumulator = 0f;
                }
            }
        }
    }

    class BulletBill {
        int x;
        int y;
        int width = 68;
        int height = 48;
        Image img;
        BulletBill(int y) {
            this.img = bulletBillImg;
            this.x = boardWidth + 50;
            this.y = y;
        }
    }

    Bird bird;
    private int mouseX = 0;
    private int mouseY = 0;
    FlappyBird() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        requestFocusInWindow();
        loadConfigFromFile();
        loadRecordsFromFile();
        backgroundImg = new ImageIcon(getClass().getResource("/Image/flappybirdbg.png")).getImage();
        birdImg[0] = new ImageIcon(getClass().getResource("/Image/yellowbird-upflap.png")).getImage();
        birdImg[1] = new ImageIcon(getClass().getResource("/Image/yellowbird-midflap.png")).getImage();
        birdImg[2] = new ImageIcon(getClass().getResource("/Image/yellowbird-downflap.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("/Image/toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("/Image/bottompipe.png")).getImage();
        bulletBillImg = new ImageIcon(getClass().getResource("/Image/bulletbill.png")).getImage();
        firstPlaceMedal = new ImageIcon(getClass().getResource("/Image/firstPlace.png")).getImage();
        secondPlaceMedal = new ImageIcon(getClass().getResource("/Image/secondPlace.png")).getImage();
        thirdPlaceMedal = new ImageIcon(getClass().getResource("/Image/thirdPlace.png")).getImage();
        keyboardArrowDown = new ImageIcon(getClass().getResource("/Image/keyboardarrowdown.png")).getImage();
        keyboardArrowUp = new ImageIcon(getClass().getResource("/Image/keyboardarrowup.png")).getImage();
        altoFalanteAtivo = new ImageIcon(getClass().getResource("/Image/alto-falanteativo.png")).getImage();
        altoFalanteSilenciado = new ImageIcon(getClass().getResource("/Image/alto-falantesilenciado.png")).getImage();
        try {
            for (int i = 0; i < wingClips.length; i++) wingClips[i] = loadSound("/Sound/sfx_wing.wav");
            for (int i = 0; i < pointClips.length; i++) pointClips[i] = loadSound("/Sound/sfx_point.wav");
            for (int i = 0; i < hitClips.length; i++) hitClips[i] = loadSound("/Sound/sfx_hit.wav");
            for (int i = 0; i < dieClips.length; i++) dieClips[i] = loadSound("/Sound/sfx_die.wav");
            for (int i = 0; i < swooshingClips.length; i++) swooshingClips[i] = loadSound("/Sound/sfx_swooshing.wav");
            for (int i = 0; i < buttonClips.length; i++) buttonClips[i] = loadSound("/Sound/sfx_button.wav");
            for (int i = 0; i < bulletCanonClips.length; i++) bulletCanonClips[i] = loadSound("/Sound/sfx_bulletcanon.wav");
            backgroundMusicClip = loadSound("/Sound/background_music.wav");
            themeMusicClip = loadSound("/Sound/sfx_flappybirdtheme.wav");
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Failed to load sounds: " + e.getMessage());
        }
        applyVolumeToAllClips();
        bird = new Bird(birdImg[0]);
        backGround2 = boardWidth;
        backGround3 = boardWidth * 2;
        
        
        gameLoop = new Timer(1000 / 60, null);
        countdownTimer = new Timer(800, null);
    }

    public void initializeListenersAndStart() {
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        
        
        gameLoop.addActionListener(this);
        countdownTimer.addActionListener(e -> {
            countdown--;
            repaint();
            if (countdown <= 0) {
                countdownTimer.stop();
                pipes.clear();
                bullets.clear();
                currentState = GameState.JOGANDO;
                showGoForFrames = 60;
                
                
                startBackgroundMusic();
                stopThemeMusic();
                repaint();
            }
        });
        gameLoop.start();
        updateTopScores();
        startThemeMusic();
    }

    private Clip loadSound(String path) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        InputStream resourceStream = getClass().getResourceAsStream(path);
        if (resourceStream == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = resourceStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        resourceStream.close();

        byte[] audioData = baos.toByteArray();
        InputStream bufferedStream = new ByteArrayInputStream(audioData);

        try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(bufferedStream)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            return clip;
        } catch (Exception e) {
            System.err.println("Error opening clip for " + path + ": " + e.getMessage());
            return null;
        }
    }

    private boolean isGameActive() {
        return currentState == GameState.JOGANDO && !paused && countdown <= 0;
    }

    private void loadConfigFromFile() {
        String configPath = System.getProperty("user.dir") + "/flappy_config.txt";
        File file = new File(configPath);
        if (!file.exists()) {
            resetToDefaultConfig();
            saveConfigToFile();
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;
                String key = parts[0].trim();
                String val = parts[1].trim();
                try {
                    switch (key) {
                        case "controlMode" -> controlMode = ControlMode.valueOf(val);
                        case "volumeEffects" -> volumeEffects = Float.parseFloat(val);
                        case "volumeMusic" -> volumeMusic = Float.parseFloat(val);
                        case "jumpKey" -> jumpKey = Integer.parseInt(val);
                        case "pauseKey" -> pauseKey = Integer.parseInt(val);
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid value for key '" + key + "': " + val);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load configurations: " + e.getMessage());
        }
        if (controlMode == null) controlMode = ControlMode.KEYBOARD_AND_MOUSE;
        tempVolumeEffects = volumeEffects;
        tempVolumeMusic = volumeMusic;
        prevVolumeEffects = volumeEffects;
        prevVolumeMusic = volumeMusic;
    }

    private void resetToDefaultConfig() {
        controlMode = ControlMode.KEYBOARD_AND_MOUSE;
        volumeEffects = 1.0f;
        volumeMusic = 1.0f;
        jumpKey = KeyEvent.VK_SPACE;
        pauseKey = KeyEvent.VK_P;
        tempControlMode = controlMode;
        tempVolumeEffects = volumeEffects;
        tempVolumeMusic = volumeMusic;
        tempJumpKey = jumpKey;
        tempPauseKey = pauseKey;
        applyVolumeToAllClips();
    }

    private void saveConfigToFile() {
        String configPath = System.getProperty("user.dir") + "/flappy_config.txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(configPath))) {
            pw.println("# Flappy Bird Configurações");
            pw.println("controlMode=" + controlMode.name());
            pw.println("volumeEffects=" + volumeEffects);
            pw.println("volumeMusic=" + volumeMusic);
            pw.println("jumpKey=" + jumpKey);
            pw.println("pauseKey=" + pauseKey);
        } catch (IOException e) {
            System.err.println("Failed to save configurations: " + e.getMessage());
        }
    }

    private void loadRecordsFromFile() {
        String recordsPath = System.getProperty("user.dir") + "/flappy_records.txt";
        profilesNormal.clear();
        profilesHard.clear();
        for (int i = 0; i < 6; i++) {
            profilesNormal.add(new PlayerProfile());
            profilesHard.add(new PlayerProfile());
        }
        File file = new File(recordsPath);
        if (!file.exists()) {
            highScoreNormalGlobal = 0;
            highScoreHardGlobal = 0;
            saveRecordsToFile();
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;
                String key = parts[0].trim();
                String val = parts[1].trim();
                try {
                    if (key.equals("highNormalGlobal")) highScoreNormalGlobal = Integer.parseInt(val);
                    else if (key.equals("highHardGlobal")) highScoreHardGlobal = Integer.parseInt(val);
                    else if (key.startsWith("normal.")) parseProfile(key, val, profilesNormal);
                    else if (key.startsWith("hard.")) parseProfile(key, val, profilesHard);
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("Failed to load records: " + e.getMessage());
        }
    }

    private void parseProfile(String key, String value, ArrayList<PlayerProfile> list) {
        try {
            String[] k = key.split("\\.");
            int slot = Integer.parseInt(k[1]) - 1;
            if (slot < 0 || slot >= list.size()) return;
            PlayerProfile p = list.get(slot);
            if (key.endsWith(".name")) p.name = value;
            else if (key.endsWith(".scores")) {
                p.scores.clear();
                if (!value.isEmpty()) {
                    for (String s : value.split(",")) {
                        try {
                            p.scores.add(Integer.parseInt(s.trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {}
    }

    private void saveRecordsToFile() {
        String recordsPath = System.getProperty("user.dir") + "/flappy_records.txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(recordsPath))) {
            pw.println("# Flappy Bird Records e Perfis");
            pw.println("highNormalGlobal=" + highScoreNormalGlobal);
            pw.println("highHardGlobal=" + highScoreHardGlobal);
            pw.println();
            pw.println("# Normal Profiles");
            for (int i = 0; i < 6; i++) {
                PlayerProfile p = profilesNormal.get(i);
                pw.println("normal." + (i + 1) + ".name=" + p.name);
                if (!p.scores.isEmpty()) {
                    String scoresStr = String.join(",", p.scores.stream().map(String::valueOf).toList());
                    pw.println("normal." + (i + 1) + ".scores=" + scoresStr);
                }
            }
            pw.println("# Hard Profiles");
            for (int i = 0; i < 6; i++) {
                PlayerProfile p = profilesHard.get(i);
                pw.println("hard." + (i + 1) + ".name=" + p.name);
                if (!p.scores.isEmpty()) {
                    String scoresStr = String.join(",", p.scores.stream().map(String::valueOf).toList());
                    pw.println("hard." + (i + 1) + ".scores=" + scoresStr);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to save records: " + e.getMessage());
        }
        updateTopScores();
    }

    private void applyVolumeToAllClips() {
        for (Clip c : wingClips) applyVolume(c, volumeEffects);
        for (Clip c : pointClips) applyVolume(c, volumeEffects);
        for (Clip c : hitClips) applyVolume(c, volumeEffects);
        for (Clip c : dieClips) applyVolume(c, volumeEffects);
        for (Clip c : swooshingClips) applyVolume(c, volumeEffects);
        for (Clip c : buttonClips) applyVolume(c, volumeEffects);
        for (Clip c : bulletCanonClips) applyVolume(c, volumeEffects);
        applyVolume(backgroundMusicClip, volumeMusic);
        applyVolume(themeMusicClip, volumeMusic);
    }

    private void applyTempVolumesToAllClips() {
        for (Clip c : wingClips)     applyVolume(c, tempVolumeEffects);
        for (Clip c : pointClips)     applyVolume(c, tempVolumeEffects);
        for (Clip c : hitClips)       applyVolume(c, tempVolumeEffects);
        for (Clip c : dieClips)       applyVolume(c, tempVolumeEffects);
        for (Clip c : swooshingClips) applyVolume(c, tempVolumeEffects);
        for (Clip c : buttonClips)    applyVolume(c, tempVolumeEffects);
        for (Clip c : bulletCanonClips) applyVolume(c, tempVolumeEffects);
        applyVolume(backgroundMusicClip, tempVolumeMusic);
        applyVolume(themeMusicClip,   tempVolumeMusic);
    }

    private void applyVolume(Clip clip, float vol) {
        if (clip != null && clip.isOpen()) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            if (gain != null) {
                float range = gain.getMaximum() - gain.getMinimum();
                float gainValue = gain.getMinimum() + (range * vol);
                gain.setValue(gainValue);
            }
        }
    }

    private void playWing() {
        Clip clip = wingClips[nextWing];
        if (clip != null) {
            if (clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
        nextWing = (nextWing + 1) % wingClips.length;
    }

    private void playPoint() {
        Clip clip = pointClips[nextPoint];
        if (clip != null) {
            if (clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
        nextPoint = (nextPoint + 1) % pointClips.length;
    }

    private void playHit() {
        Clip clip = hitClips[nextHit];
        if (clip != null) {
            if (clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
        nextHit = (nextHit + 1) % hitClips.length;
    }

    private void playDie() {
        Clip clip = dieClips[nextDie];
        if (clip != null) {
            if (clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
        nextDie = (nextDie + 1) % dieClips.length;
    }

    private void playSwooshing() {
        Clip clip = swooshingClips[nextSwooshing];
        if (clip != null) {
            if (clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
        nextSwooshing = (nextSwooshing + 1) % swooshingClips.length;
    }

    private void playBulletCanon() {
        Clip clip = bulletCanonClips[nextBulletCanon];
        if (clip != null) {
            if (clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
        nextBulletCanon = (nextBulletCanon + 1) % bulletCanonClips.length;
    }

    private void playButtonSelect() {
        Clip clip = buttonClips[nextButton];
        if (clip != null) {
            if (clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
        nextButton = (nextButton + 1) % buttonClips.length;
    }

    private void startBackgroundMusic() {
        if (backgroundMusicClip != null) {
            backgroundMusicClip.setFramePosition(0);
            backgroundMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    private void startThemeMusic() {
        if (themeMusicClip != null && !themeMusicClip.isRunning()) {
            themeMusicClip.setFramePosition(0);
            themeMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            applyVolume(themeMusicClip, volumeMusic);
        }
    }

    private void stopThemeMusic() {
        if (themeMusicClip != null && themeMusicClip.isRunning()) {
            themeMusicClip.stop();
        }
    }

    public void placePipes() {
        int pipeImgHeight = 512;
        int groundY = boardHeight - 100;
        int minGapFromGround = 50;
        int maxBottomY = groundY - minGapFromGround;
        int gap = 180 + random.nextInt(60);
        int minTopY = -pipeImgHeight + 80;
        int maxTopY = maxBottomY - pipeImgHeight - gap;
        if (maxTopY < minTopY) maxTopY = minTopY;
        int randomPipeY = minTopY + random.nextInt(maxTopY - minTopY + 1);
        Pipe top = new Pipe(topPipeImg, modoDificil);
        top.y = randomPipeY;
        if (modoDificil) {
            final int MIN_ROOM_FOR_UP = 80;
            top.y = Math.max(LIMIT_TOP + MIN_ROOM_FOR_UP, Math.min(LIMIT_BOTTOM, top.y));
        }
        pipes.add(top);
        Pipe bottom = new Pipe(bottomPipeImg, false);
        bottom.y = top.y + pipeImgHeight + gap;
        pipes.add(bottom);
    }

    private void spawnBullet() {
        if (!isGameActive() || !modoDificil) return;
        int margin = (int) (boardHeight * 0.15);
        int minY = margin;
        int maxY = boardHeight - margin - bulletBillImg.getHeight(null);
        if (minY >= maxY) {
            minY = 100;
            maxY = boardHeight - 100 - 24;
        }
        int randomY = minY + random.nextInt(maxY - minY + 1);
        bullets.add(new BulletBill(randomY));
        playBulletCanon();
    }

    private void updateTopScores() {
        topNormal.clear();
        topHard.clear();
        for (PlayerProfile p : profilesNormal) {
            if (!p.isEmpty() && !p.scores.isEmpty()) {
                for (int s : p.scores) topNormal.add(new GlobalHighScore(p.name, s, "Normal"));
            }
        }
        topNormal.sort((a, b) -> Integer.compare(b.score, a.score));
        if (topNormal.size() > 20) topNormal = new ArrayList<>(topNormal.subList(0, 20));
        for (PlayerProfile p : profilesHard) {
            if (!p.isEmpty() && !p.scores.isEmpty()) {
                for (int s : p.scores) topHard.add(new GlobalHighScore(p.name, s, "Hard"));
            }
        }
        topHard.sort((a, b) -> Integer.compare(b.score, a.score));
        if (topHard.size() > 20) topHard = new ArrayList<>(topHard.subList(0, 20));
    }

    private boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width &&
               a.x + a.width > b.x &&
               a.y < b.y + b.height &&
               a.y + a.height > b.y;
    }

    private boolean collision(Bird a, BulletBill b) {
        return a.x < b.x + b.width &&
               a.x + a.width > b.x &&
               a.y < b.y + b.height &&
               a.y + a.height > b.y;
    }

    private void drawShadowedText(Graphics g, String text, int x, int y, Color color, Font font) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(font);
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(text, x + 1, y + 1);
        g2d.setColor(color);
        g2d.drawString(text, x, y);
    }

    private void showStatusMessage(String msg) {
        statusMessage = msg;
        statusMessageStartTime = System.currentTimeMillis();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
        if (!statusMessage.isEmpty()) {
            long now = System.currentTimeMillis();
            if (now - statusMessageStartTime < MESSAGE_DURATION_MS) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setFont(new Font("Lucida Sans", Font.BOLD, 18));
                FontMetrics fm = g2d.getFontMetrics();
                int msgWidth = fm.stringWidth(statusMessage);
                int msgHeight = fm.getHeight();
                int paddingX = 20;
                int paddingY = 12;
                int x = (boardWidth - msgWidth) / 2;
                int y = (boardHeight - msgHeight) / 2;
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.fillRoundRect(x - paddingX, y - msgHeight - paddingY + 5, msgWidth + paddingX * 2, msgHeight + paddingY * 2, 30, 30);
                g2d.setColor(new Color(180, 180, 180));
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(x - paddingX, y - msgHeight - paddingY + 5, msgWidth + paddingX * 2, msgHeight + paddingY * 2, 30, 30);
                g2d.setColor(new Color(0, 0, 0, 220));
                g2d.drawString(statusMessage, x + 2, y + 2);
                g2d.setColor(Color.WHITE);
                g2d.drawString(statusMessage, x, y);
                g2d.dispose();
            } else {
                statusMessage = "";
            }
        }
        if (currentState == GameState.CONFIGURACOES || currentState == GameState.RECORDS) {
            requestFocusInWindow();
        }
    }

    private void draw(Graphics g) {
        g.drawImage(backgroundImg, (int) backGround1, 0, boardWidth, boardHeight, null);
        g.drawImage(backgroundImg, (int) backGround2, 0, boardWidth, boardHeight, null);
        g.drawImage(backgroundImg, (int) backGround3, 0, boardWidth, boardHeight, null);
        boolean showMenuBird = currentState == GameState.MENU_PRINCIPAL ||
                currentState == GameState.NEW_GAME ||
                currentState == GameState.CONTINUE ||
                currentState == GameState.EDIT_PROFILE ||
                currentState == GameState.CONFIRM_DELETE_PROFILE ||
                currentState == GameState.CONFIRM_EDIT_PROFILE ||
                currentState == GameState.CONFIRM_START_GAME ||
                currentState == GameState.DIGITAR_NOME_PERFIL ||
                currentState == GameState.GAME_OVER ||
                currentState == GameState.CONFIGURACOES ||
                currentState == GameState.RECORDS;
        if (showMenuBird) {
            int birdMenuX = boardWidth / 2 - birdWidth;
            int birdMenuY = 60;
            g.drawImage(bird.img, birdMenuX, birdMenuY, birdWidth * 2, birdHeight * 2, null);
        }
        clickableOptions.clear();
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        switch (currentState) {
            case MENU_PRINCIPAL -> drawMenuPrincipal(g2d);
            case NEW_GAME, CONTINUE -> drawSlotsScreen(g2d);
            case EDIT_PROFILE -> drawEditProfile(g2d);
            case CONFIRM_DELETE_PROFILE -> drawConfirmDelete(g2d);
            case CONFIRM_EDIT_PROFILE -> drawConfirmEditProfile(g2d);
            case CONFIRM_START_GAME -> drawConfirmStartGame(g2d);
            case DIGITAR_NOME_PERFIL -> drawDigitarNomePerfil(g2d);
            case CONFIGURACOES -> drawConfiguracoes(g2d);
            case RECORDS -> drawRecords(g2d);
            case JOGANDO, GAME_OVER -> {
                if (currentState == GameState.JOGANDO) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    AffineTransform old = g2.getTransform();
                    double cx = bird.x + bird.width / 2.0;
                    double cy = bird.y + bird.height / 2.0;
                    g2.rotate(Math.toRadians(birdRotation), cx, cy);
                    g2.drawImage(bird.img, bird.x, bird.y, bird.width, bird.height, null);
                    g2.setTransform(old);
                    g2.dispose();
                }
                for (Pipe p : pipes) g.drawImage(p.img, p.x, p.y, p.width, p.height, null);
                for (BulletBill b : bullets) g.drawImage(b.img, b.x, b.y, b.width, b.height, null);
                if (countdown >= 1) {
                    g.setFont(new Font("FFF Forward", Font.BOLD, 54));
                    FontMetrics fm = g.getFontMetrics();
                    String txt = String.valueOf(countdown);
                    int x = (boardWidth - fm.stringWidth(txt)) / 2;
                    int y = boardHeight / 2;
                    drawShadowedText(g, txt, x, y, LARANJA_PADRAO, g.getFont());
                }
                if (showGoForFrames > 0) {
                    g.setFont(new Font("FFF Forward", Font.BOLD, 54));
                    FontMetrics fm = g.getFontMetrics();
                    String txt = "GO!";
                    int x = (boardWidth - fm.stringWidth(txt)) / 2;
                    int y = boardHeight / 2;
                    drawShadowedText(g, txt, x, y, LARANJA_PADRAO, g.getFont());
                }
                if (currentState == GameState.JOGANDO) {
                    g.setFont(new Font("FFF Forward", Font.BOLD, 36));
                    FontMetrics fm = g.getFontMetrics();
                    String s = "" + score;
                    int x = boardWidth / 2 - fm.stringWidth(s) / 2;
                    int y = 80;
                    drawShadowedText(g, s, x, y, LARANJA_PADRAO, g.getFont());
                }
                if (paused && currentState == GameState.JOGANDO) {
                    g.setFont(new Font("FFF Forward", Font.BOLD, 32));
                    FontMetrics fm = g.getFontMetrics();
                    String pauseText = "PAUSE";
                    int x = (boardWidth - fm.stringWidth(pauseText)) / 2;
                    int y = boardHeight / 2 - 140;
                    drawShadowedText(g, pauseText, x, y, LARANJA_PADRAO, g.getFont());
                    if (pauseMenuActive) {
                        String[] pauseOptions = {"Continue", "Config.", "Exit"};
                        int buttonWidth = 150;
                        int startY = boardHeight / 2 - 40;
                        int spacing = 70;
                        g.setFont(new Font("FFF Forward", Font.BOLD, 14));
                        for (int i = 0; i < pauseOptions.length; i++) {
                            final int index = i;
                            String text = pauseOptions[i];
                            int centerX = boardWidth / 2;
                            int centerY = startY + i * spacing;
                            drawRoundedButton((Graphics2D) g, text, centerX, centerY, g.getFont(), (i == selectedPauseOption), buttonWidth);
                            if (i == selectedPauseOption && i != lastPauseIndex) {
                                playButtonSelect();
                                lastPauseIndex = i;
                            }
                            clickableOptions.add(new MenuOption(text, centerX - buttonWidth / 2, centerY + 35, buttonWidth, 60, () -> {
                                selectedPauseOption = index;
                                handlePauseMenuEnter();
                            }));
                        }
                    }
                }
                if (currentState == GameState.GAME_OVER) {
                    g.setFont(new Font("FFF Forward", Font.BOLD, 36));
                    FontMetrics fm = g.getFontMetrics();
                    String over = "Game Over";
                    int x = (boardWidth - fm.stringWidth(over)) / 2;
                    int y = boardHeight / 2 - 120;
                    drawShadowedText(g, over, x, y, LARANJA_PADRAO, g.getFont());
                    g.setFont(new Font("FFF Forward", Font.BOLD, 24));
                    fm = g.getFontMetrics();
                    String scoreText = "Score: " + score;
                    x = (boardWidth - fm.stringWidth(scoreText)) / 2;
                    y = boardHeight / 2 - 40;
                    drawShadowedText(g, scoreText, x, y, LARANJA_PADRAO, g.getFont());
                    String[] gameOverOptions = {"Restart", "Records", "Exit"};
                    int buttonWidth = 180;
                    int startY = boardHeight / 2 + 40;
                    int spacing = 70;
                    g.setFont(new Font("FFF Forward", Font.BOLD, 14));
                    for (int i = 0; i < gameOverOptions.length; i++) {
                        final int index = i;
                        String text = gameOverOptions[i];
                        int centerX = boardWidth / 2;
                        int centerY = startY + i * spacing;
                        drawRoundedButton((Graphics2D) g, text, centerX, centerY, g.getFont(), (index == selectedGameOverOption), buttonWidth);
                        if (index == selectedGameOverOption && index != lastGameOverIndex) {
                            playButtonSelect();
                            lastGameOverIndex = index;
                        }
                        clickableOptions.add(new MenuOption(text, centerX - buttonWidth / 2, centerY + 35, buttonWidth, 60, () -> {
                            selectedGameOverOption = index;
                            handleGameOverNewEnter();
                        }));
                    }
                }
            }
        }
    }

    private void drawRoundedButton(Graphics2D g2d, String text, int centerX, int centerY, Font baseFont, boolean selected, int buttonWidth) {
        final int PADDING_X = 15;
        final int PADDING_Y = 15;
        final int ARC = 40;
        int offsetY = selected ? -5 : 0;
        Font currentFont = baseFont;
        g2d.setFont(currentFont);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        while (textWidth > buttonWidth - PADDING_X * 2 && currentFont.getSize() > 12) {
            currentFont = currentFont.deriveFont((float) (currentFont.getSize() - 1));
            g2d.setFont(currentFont);
            fm = g2d.getFontMetrics();
            textWidth = fm.stringWidth(text);
        }
        int textHeight = fm.getHeight();
        int rectWidth = buttonWidth;
        int rectHeight = textHeight + PADDING_Y * 2;
        int rectX = centerX - rectWidth / 2;
        int rectY = centerY - rectHeight / 2 + fm.getDescent() / 2 + offsetY;
        Color baseColor = selected
                ? new Color(LARANJA_PADRAO.getRed(), LARANJA_PADRAO.getGreen(), LARANJA_PADRAO.getBlue(), 225)
                : new Color(LARANJA_PADRAO.getRed(), LARANJA_PADRAO.getGreen(), LARANJA_PADRAO.getBlue(), 250);
        g2d.setColor(baseColor);
        g2d.fillRoundRect(rectX, rectY, rectWidth, rectHeight, ARC, ARC);
        if (selected) {
            g2d.setColor(LARANJA_PADRAO);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRoundRect(rectX, rectY, rectWidth, rectHeight, ARC, ARC);
        }
        int shadowHeight = 30;
        GradientPaint shadowGradient = new GradientPaint(
                rectX, rectY + rectHeight - shadowHeight, new Color(223, 136, 12, 0),
                rectX, rectY + rectHeight, new Color(223, 136, 12, 200)
        );
        g2d.setPaint(shadowGradient);
        g2d.fillRoundRect(rectX, rectY - 15 + rectHeight - shadowHeight, rectWidth, shadowHeight + ARC / 2, ARC, ARC);
        g2d.setColor(selected ? Color.WHITE : new Color(80, 80, 80));
        int textX = centerX - textWidth / 2;
        int textY = centerY + textHeight / 2 - fm.getDescent() + offsetY;
        if (selected) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.drawString(text, textX + 2, textY + 2);
        }
        g2d.setColor(selected ? Color.WHITE : new Color(80, 80, 80));
        g2d.drawString(text, textX, textY);
        g2d.setFont(baseFont);
    }

    private void drawMenuPrincipal(Graphics2D g) {
        g.setFont(new Font("FFF Forward", Font.BOLD, 18));
        ArrayList<String> options = new ArrayList<>();
        options.add("New Game");
        if (hasAnySave()) options.add("Continue");
        options.add("Configurations");
        options.add("Records");
        int buttonY = 220;
        int buttonSpacing = 80;
        for (int i = 0; i < options.size(); i++) {
            final int index = i;
            String text = options.get(index);
            int centerX = boardWidth / 2;
            int centerY = buttonY + i * buttonSpacing;
            drawRoundedButton(g, text, centerX, centerY, g.getFont(), index == selectedMenuItem, BTN_LARGE);
            if (index == selectedMenuItem && index != lastMenuPrincipalIndex) {
                playButtonSelect();
                lastMenuPrincipalIndex = index;
            }
            clickableOptions.add(new MenuOption(
                    text, centerX - BTN_LARGE / 2, (centerY + 56) - BTN_HEIGHT / 2, BTN_LARGE, BTN_HEIGHT,
                    () -> {
                        selectedMenuItem = index;
                        if (index == options.size() - 1) {
                            updateTopScores();
                            recordsAbaNormal = true;
                        }
                        handleMenuPrincipalEnter();
                    }
            ));
        }
    }

    private void drawSlotsScreen(Graphics2D g) {
        boolean isContinue = (currentState == GameState.CONTINUE);
        ArrayList<PlayerProfile> lista = abaNormalAtiva ? profilesNormal : profilesHard;
        String titulo = isContinue ? "Continue" : "New Game";
        g.setFont(new Font("FFF Forward", Font.BOLD, 36));
        FontMetrics fmTitulo = g.getFontMetrics();
        int titleX = (boardWidth - fmTitulo.stringWidth(titulo)) / 2;
        int titleY = 180;
        drawShadowedText(g, titulo, titleX, titleY, LARANJA_PADRAO, g.getFont());
        g.setFont(new Font("FFF Forward", Font.BOLD, 18));
        int abaY = 240;
        int btnWidth = BTN_MEDIUM;
        boolean normalHovered = mouseX >= 100 - btnWidth / 2 && mouseX <= 100 + btnWidth / 2 &&
                                mouseY >= abaY - 30 && mouseY <= abaY + 30;
        boolean hardHovered = mouseX >= 260 - btnWidth / 2 && mouseX <= 260 + btnWidth / 2 &&
                              mouseY >= abaY - 30 && mouseY <= abaY + 30;
        boolean normalSelected = (abaNormalAtiva && !hardHovered) || normalHovered;
        boolean hardSelected = (!abaNormalAtiva && !normalHovered) || hardHovered;
        drawRoundedButton(g, "Normal", 100, abaY, g.getFont(), normalSelected && slotsFocusOnTabs, BTN_MEDIUM);
        if (slotsFocusOnTabs && normalSelected && lastSlotsTabIndex != 0) {
            playButtonSelect();
            lastSlotsTabIndex = 0;
        }
        clickableOptions.add(new MenuOption("Normal", 100 - BTN_MEDIUM / 2, abaY + 30, BTN_MEDIUM, BTN_HEIGHT, () -> {
            abaNormalAtiva = true;
            currentProfileList = profilesNormal;
            selectedSlot = selectedSlotNormal;
            slotsFocusOnBack = false;
            slotsFocusOnTabs = true;
            adjustScrollToSelected();
            repaint();
        }));
        drawRoundedButton(g, "Hard", 260, abaY, g.getFont(), hardSelected && slotsFocusOnTabs, BTN_MEDIUM);
        if (slotsFocusOnTabs && hardSelected && lastSlotsTabIndex != 1) {
            playButtonSelect();
            lastSlotsTabIndex = 1;
        }
        clickableOptions.add(new MenuOption("Hard", 260 - BTN_MEDIUM / 2, abaY + 30, BTN_MEDIUM, BTN_HEIGHT, () -> {
            abaNormalAtiva = false;
            currentProfileList = profilesHard;
            selectedSlot = selectedSlotHard;
            slotsFocusOnBack = false;
            slotsFocusOnTabs = true;
            adjustScrollToSelected();
            repaint();
        }));
        int boxX = (boardWidth - 300) / 2;
        int boxY = 300;
        int boxWidth = 300;
        int boxHeight = VISIBLE_SLOTS * SLOT_HEIGHT;
        g.setColor(new Color(40, 40, 40, 180));
        g.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 20, 20);
        g.setColor(new Color(150, 150, 150));
        g.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 20, 20);
        int scrollOffset = abaNormalAtiva ? scrollOffsetNormal : scrollOffsetHard;
        int totalHeight = lista.size() * SLOT_HEIGHT;
        int maxScroll = Math.max(0, totalHeight - boxHeight);
        if (maxScroll > 0) {
            float scrollRatio = (float) scrollOffset / maxScroll;
            int thumbHeight = (int) (boxHeight * (boxHeight / (float) totalHeight));
            thumbHeight = Math.max(20, thumbHeight);
            int trackHeight = boxHeight - 30;
            int thumbY = boxY + 15 + (int) (scrollRatio * trackHeight);
            thumbY = Math.min(thumbY, boxY + boxHeight - thumbHeight - 15);
            g.setColor(new Color(180, 180, 180, 200));
            g.fillRoundRect(boxX + boxWidth - 12, thumbY, 8, thumbHeight, 8, 8);
        }
        Shape oldClip = g.getClip();
        g.setClip(boxX, boxY, boxWidth, boxHeight);
        g.setFont(new Font("Lucida Sans", Font.PLAIN, 24));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < lista.size(); i++) {
            int itemY = boxY + (i * SLOT_HEIGHT) - scrollOffset;
            if (itemY + SLOT_HEIGHT < boxY || itemY > boxY + boxHeight) continue;
            PlayerProfile p = lista.get(i);
            String text = (i + 1) + ". " + (p.isEmpty() ? "Empty" : p.name);
            boolean selected = (i == selectedSlot && selectedSlot >= 0);
            Color slotColor = selected ? LARANJA_PADRAO : (p.isEmpty() ? Color.GRAY : Color.WHITE);
            int textX = boxX + 20;
            int textY = itemY + SLOT_HEIGHT / 2 + fm.getAscent() / 2 - 5;
            drawShadowedText(g, text, textX, textY, slotColor, g.getFont());
        }
        g.setClip(oldClip);
        g.setFont(new Font("FFF Forward", Font.BOLD, 18));
        String voltarText = "Back";
        int voltarY = boardHeight - 100;
        int voltarCenterX = boardWidth / 2;
        boolean backSelected = slotsFocusOnBack;
        drawRoundedButton(g, voltarText, voltarCenterX, voltarY, g.getFont(), backSelected, BTN_SMALL);
        if (slotsFocusOnBack && backSelected && lastSlotsBackIndex != 1) {
            playButtonSelect();
            lastSlotsBackIndex = 1;
        } else if (!slotsFocusOnBack && lastSlotsBackIndex != -1) {
            lastSlotsBackIndex = -1;
        }
        clickableOptions.add(new MenuOption(
                voltarText,
                voltarCenterX - BTN_SMALL / 2,
                voltarY + 30,
                BTN_SMALL,
                BTN_HEIGHT,
                () -> {
                    playSwooshing();
                    saveRecordsToFile();
                    currentState = GameState.MENU_PRINCIPAL;
                    selectedMenuItem = 0;
                    slotsFocusOnBack = false;
                    slotsFocusOnTabs = true;
                    repaint();
                }
        ));
    }

    private void drawRecords(Graphics2D g) {
        g.setFont(new Font("FFF Forward", Font.BOLD, 32));
        FontMetrics fmTitulo = g.getFontMetrics();
        String title = "Records";
        int titleX = (boardWidth - fmTitulo.stringWidth(title)) / 2;
        int titleY = 180;
        drawShadowedText(g, title, titleX, titleY, LARANJA_PADRAO, g.getFont());
        int abaY = 240;
        int btnWidth = BTN_MEDIUM;
        g.setFont(new Font("FFF Forward", Font.BOLD, 18));
        boolean normalHovered = mouseX >= 100 - btnWidth / 2 && mouseX <= 100 + btnWidth / 2 &&
                                mouseY >= abaY - 30 && mouseY <= abaY + 30;
        boolean hardHovered = mouseX >= 260 - btnWidth / 2 && mouseX <= 260 + btnWidth / 2 &&
                              mouseY >= abaY - 30 && mouseY <= abaY + 30;
        if (recordsVindoDoGameOver) {
            String modoTexto = recordsAbaNormal ? "Normal" : "Hard";
            int centerX = boardWidth / 2;
            boolean abaHovered = mouseX >= centerX - btnWidth / 2 && mouseX <= centerX + btnWidth / 2 &&
                                 mouseY >= abaY - 30 && mouseY <= abaY + 30;
            boolean abaHighlighted = abaHovered || recordsFocusOnTabs;
            drawRoundedButton(g, modoTexto, centerX, abaY, g.getFont(), abaHighlighted, BTN_MEDIUM);
            if (abaHighlighted && lastRecordsTabIndex != 0) {
                playButtonSelect();
                lastRecordsTabIndex = 0;
            }
            clickableOptions.add(new MenuOption(
                    modoTexto,
                    centerX - btnWidth / 2,
                    abaY - BTN_HEIGHT / 2,
                    btnWidth,
                    BTN_HEIGHT,
                    () -> {
                        recordsAbaNormal = !recordsAbaNormal;
                        recordsScrollOffset = 0;
                        playButtonSelect();
                        repaint();
                    }
            ));
        } else {
            boolean normalHighlighted;
            boolean hardHighlighted;
            if (normalHovered) {
                normalHighlighted = true;
                hardHighlighted = false;
            } else if (hardHovered) {
                normalHighlighted = false;
                hardHighlighted = true;
            } else {
                normalHighlighted = recordsAbaNormal;
                hardHighlighted = !recordsAbaNormal;
            }
            drawRoundedButton(g, "Normal", 100, abaY, g.getFont(), normalHighlighted && recordsFocusOnTabs, BTN_MEDIUM);
            if (normalHighlighted && lastRecordsTabIndex != 0) {
                playButtonSelect();
                lastRecordsTabIndex = 0;
            }
            clickableOptions.add(new MenuOption(
                    "Normal",
                    100 - btnWidth / 2,
                    abaY - BTN_HEIGHT / 2 + 50,
                    btnWidth,
                    BTN_HEIGHT + 5,
                    () -> {
                        recordsAbaNormal = true;
                        recordsScrollOffset = 0;
                        playButtonSelect();
                        repaint();
                    }
            ));
            drawRoundedButton(g, "Hard", 260, abaY, g.getFont(), hardHighlighted && recordsFocusOnTabs, BTN_MEDIUM);
            if (hardHighlighted && lastRecordsTabIndex != 1) {
                playButtonSelect();
                lastRecordsTabIndex = 1;
            }
            clickableOptions.add(new MenuOption(
                    "Hard",
                    260 - btnWidth / 2,
                    abaY - BTN_HEIGHT / 2 + 50,
                    btnWidth,
                    BTN_HEIGHT + 5,
                    () -> {
                        recordsAbaNormal = false;
                        recordsScrollOffset = 0;
                        playButtonSelect();
                        repaint();
                    }
            ));
        }
        ArrayList<GlobalHighScore> listaAtual;
        if (showOnlyCurrentProfileRecords && currentProfileList != null && selectedSlot >= 0) {
            PlayerProfile currentProfile = currentProfileList.get(selectedSlot);
            if (!currentProfile.scores.isEmpty()) {
                ArrayList<Integer> perfilScores = new ArrayList<>(currentProfile.scores);
                perfilScores.sort(Collections.reverseOrder());
                if (perfilScores.size() > 20) perfilScores = new ArrayList<>(perfilScores.subList(0, 20));
                listaAtual = new ArrayList<>();
                String modoReal = (currentProfileList == profilesNormal) ? "Normal" : "Hard";
                for (int s : perfilScores) listaAtual.add(new GlobalHighScore(currentProfile.name, s, modoReal));
            } else {
                listaAtual = new ArrayList<>();
            }
        } else {
            listaAtual = recordsAbaNormal ? topNormal : topHard;
        }
        int boxX = (boardWidth - 340) / 2;
        int boxY = 300;
        int boxWidth = 340;
        int boxHeight = 180;
        g.setColor(new Color(40, 40, 40, 180));
        g.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 20, 20);
        g.setColor(new Color(150, 150, 150));
        g.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 20, 20);
        int totalContent = listaAtual.size() * 30;
        int maxScroll = Math.max(0, totalContent - boxHeight + 40);
        if (maxScroll > 0 && totalContent > 0) {
            float ratio = Math.min(1.0f, (float) recordsScrollOffset / maxScroll);
            int thumbHeight = Math.max(30, Math.min(boxHeight - 30, (int) (boxHeight * boxHeight / (float) totalContent)));
            int trackHeight = boxHeight - 30;
            int thumbY = boxY + 15 + (int) (ratio * trackHeight);
            thumbY = Math.min(thumbY, boxY + boxHeight - thumbHeight - 15);
            g.setColor(new Color(180, 180, 180, 220));
            g.fillRoundRect(boxX + boxWidth - 14, thumbY, 8, thumbHeight, 4, 4);
        }
        Shape oldClip = g.getClip();
        g.setClip(boxX + 10, boxY + 15, boxWidth - 30, boxHeight - 30);
        g.setFont(new Font("Lucida Sans", Font.PLAIN, 18));
        FontMetrics fm = g.getFontMetrics();
        if (listaAtual.isEmpty()) {
            String vazio = "Empty!";
            int vazioX = (boardWidth - fm.stringWidth(vazio)) / 2;
            int vazioY = boxY + boxHeight / 2 + 10;
            drawShadowedText(g, vazio, vazioX, vazioY, Color.WHITE, g.getFont());
        } else {
            int lineHeight = 30;
            int medalSize = 24;
            int leftMargin = boxX + 20;
            int nameX = leftMargin + medalSize + 15;
            int scoreX = boxX + boxWidth - 30;
            for (int i = 0; i < listaAtual.size(); i++) {
                GlobalHighScore hs = listaAtual.get(i);
                int textY = boxY + 40 + i * lineHeight - recordsScrollOffset;
                if (textY + lineHeight < boxY + 15 || textY > boxY + boxHeight - 15) continue;
                if (i == 0 && firstPlaceMedal != null) {
                    g.drawImage(firstPlaceMedal, leftMargin, textY - 12, medalSize, medalSize, null);
                } else if (i == 1 && secondPlaceMedal != null) {
                    g.drawImage(secondPlaceMedal, leftMargin, textY - 12, medalSize, medalSize, null);
                } else if (i == 2 && thirdPlaceMedal != null) {
                    g.drawImage(thirdPlaceMedal, leftMargin, textY - 12, medalSize, medalSize, null);
                } else {
                    String pos = (i + 1) + "°";
                    drawShadowedText(g, pos, leftMargin + 4, textY + 4, Color.WHITE, g.getFont());
                }
                drawShadowedText(g, hs.name, nameX, textY + 4, Color.WHITE, g.getFont());
                String scoreStr = String.valueOf(hs.score);
                int scoreWidth = fm.stringWidth(scoreStr);
                drawShadowedText(g, scoreStr, scoreX - scoreWidth, textY + 4, Color.WHITE, g.getFont());
            }
        }
        g.setClip(oldClip);
        g.setFont(new Font("FFF Forward", Font.BOLD, 18));
        boolean backFocused = !recordsFocusOnTabs;
        drawRoundedButton(g, "Back", boardWidth / 2, boardHeight - 100, g.getFont(), backFocused, BTN_SMALL);
        if (!recordsFocusOnTabs && backFocused && lastRecordsTabIndex != -1) {
            playButtonSelect();
            lastRecordsTabIndex = -1;
        }
        clickableOptions.add(new MenuOption(
                "Back",
                boardWidth / 2 - BTN_SMALL / 2,
                boardHeight - 100 - BTN_HEIGHT / 2 + 45,
                BTN_SMALL,
                BTN_HEIGHT,
                () -> {
                    playSwooshing();
                    currentState = previousStateBeforeRecords != null ? previousStateBeforeRecords : GameState.MENU_PRINCIPAL;
                    selectedMenuItem = 0;
                    recordsScrollOffset = 0;
                    recordsFocusOnTabs = true;
                    showOnlyCurrentProfileRecords = false;
                    recordsVindoDoGameOver = false;
                    previousStateBeforeRecords = null;
                    repaint();
                }
        ));
    }

    private void drawEditProfile(Graphics2D g) {
        g.setFont(new Font("FFF Forward", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        String title = "Profile Options";
        int titleX = (boardWidth - fm.stringWidth(title)) / 2;
        int titleY = 165;
        drawShadowedText(g, title, titleX, titleY, LARANJA_PADRAO, g.getFont());
        String[] ops = {"Edit", "Delete", "Back"};
        Font baseFont = new Font("FFF Forward", Font.PLAIN, 18);
        int centerX = boardWidth / 2;
        int buttonY = 220;
        int spacing = 70;
        int buttonWidth = BTN_MEDIUM;
        for (int i = 0; i < ops.length; i++) {
            final int index = i;
            String text = ops[index];
            int thisY = buttonY + i * spacing;
            boolean selected = (index == selectedMenuItem);
            drawRoundedButton(g, text, centerX, thisY, baseFont, selected, buttonWidth);
            if (selected && index != lastEditProfileIndex) {
                playButtonSelect();
                lastEditProfileIndex = index;
            }
            clickableOptions.add(new MenuOption(
                    text,
                    centerX - buttonWidth / 2,
                    thisY + 27,
                    buttonWidth,
                    BTN_HEIGHT,
                    () -> {
                        selectedMenuItem = index;
                        handleEditProfileEnter();
                    }
            ));
        }
    }

    private void drawConfirmDelete(Graphics2D g) {
        PlayerProfile p = currentProfileList.get(selectedSlot);
        g.setFont(new Font("FFF Forward", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        String title = "Delete?";
        int titleX = (boardWidth - fm.stringWidth(title)) / 2;
        int titleY = 180;
        drawShadowedText(g, title, titleX, titleY, LARANJA_PADRAO, g.getFont());
        g.setFont(new Font("Lucida Sans", Font.BOLD, 24));
        FontMetrics fmNome = g.getFontMetrics();
        String nome = p.name;
        int nomeX = (boardWidth - fmNome.stringWidth(nome)) / 2;
        int nomeY = 240;
        drawShadowedText(g, nome, nomeX, nomeY, Color.WHITE, g.getFont());
        String[] ops = {"Yes", "No"};
        Font baseFont = new Font("FFF Forward", Font.BOLD, 14);
        int centerX = boardWidth / 2;
        int buttonY = 320;
        int spacing = 140;
        int buttonWidth = BTN_SMALL;
        for (int i = 0; i < ops.length; i++) {
            final int index = i;
            String text = ops[index];
            int thisCenterX = centerX + (i == 0 ? -spacing / 2 : spacing / 2);
            boolean selected = (index == selectedMenuItem);
            drawRoundedButton(g, text, thisCenterX, buttonY, baseFont, selected, buttonWidth);
            if (selected && index != lastConfirmIndex) {
                playButtonSelect();
                lastConfirmIndex = index;
            }
            clickableOptions.add(new MenuOption(
                    text,
                    thisCenterX - buttonWidth / 2,
                    buttonY + 27,
                    buttonWidth,
                    BTN_HEIGHT_CONFIRM,
                    () -> {
                        selectedMenuItem = index;
                        if (index == 0) {
                            playDie();
                            playSwooshing();
                            PlayerProfile prof = currentProfileList.get(selectedSlot);
                            prof.name = "";
                            prof.scores.clear();
                            saveRecordsToFile();
                            currentState = GameState.NEW_GAME;
                            if (selectedSlot == lastPlayedSlot) {
                                lastPlayedSlot = -1;
                                lastPlayedWasHard = false;
                            }
                        } else {
                            playSwooshing();
                            currentState = GameState.EDIT_PROFILE;
                        }
                        repaint();
                    }
            ));
        }
    }

    private boolean nomePerfilJaExiste(String nome, PlayerProfile ignorarPerfil) {
        if (nome == null || nome.trim().isEmpty()) return false;
        String nomeLower = nome.trim().toLowerCase();
        ArrayList<PlayerProfile> listaAtual = currentProfileList;
        for (PlayerProfile p : listaAtual) {
            if (p != ignorarPerfil && !p.isEmpty() && p.name.trim().toLowerCase().equals(nomeLower)) {
                return true;
            }
        }
        return false;
    }

    private void drawConfirmEditProfile(Graphics2D g) {
        g.setFont(new Font("FFF Forward", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        String title = "Change Name?";
        int titleX = (boardWidth - fm.stringWidth(title)) / 2;
        int titleY = 250;
        drawShadowedText(g, title, titleX, titleY, LARANJA_PADRAO, g.getFont());
        String[] ops = {"Yes", "No"};
        Font baseFont = new Font("FFF Forward", Font.BOLD, 14);
        int centerX = boardWidth / 2;
        int buttonY = 320;
        int spacing = 140;
        int buttonWidth = BTN_SMALL;
        for (int i = 0; i < ops.length; i++) {
            final int index = i;
            String text = ops[index];
            int thisCenterX = centerX + (i == 0 ? -spacing / 2 : spacing / 2);
            boolean selected = (index == selectedMenuItem);
            drawRoundedButton(g, text, thisCenterX, buttonY, baseFont, selected, buttonWidth);
            if (selected && index != lastConfirmIndex) {
                playButtonSelect();
                lastConfirmIndex = index;
            }
            clickableOptions.add(new MenuOption(
                    text,
                    thisCenterX - buttonWidth / 2,
                    buttonY + 27,
                    buttonWidth,
                    BTN_HEIGHT_CONFIRM,
                    () -> {
                        selectedMenuItem = index;
                        if (index == 0) {
                            playPoint();
                            String novoNome = typingName.trim();
                            if (novoNome.isEmpty()) {
                                showStatusMessage("Enter a name!");
                                repaint();
                                return;
                            }
                            PlayerProfile perfilAtual = currentProfileList.get(selectedSlot);
                            if (nomePerfilJaExiste(novoNome, perfilAtual)) {
                                showStatusMessage("This name already exists!");
                                repaint();
                                return;
                            }
                            PlayerProfile p = currentProfileList.get(selectedSlot);
                            p.name = novoNome;
                            saveRecordsToFile();
                            currentState = GameState.NEW_GAME;
                        } else {
                            playSwooshing();
                            currentState = GameState.DIGITAR_NOME_PERFIL;
                        }
                        repaint();
                    }
            ));
        }
    }

    private void drawConfirmStartGame(Graphics2D g) {
        PlayerProfile p = currentProfileList.get(selectedSlot);
        g.setFont(new Font("FFF Forward", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        String title = "Start Game?";
        int titleX = (boardWidth - fm.stringWidth(title)) / 2;
        int titleY = 200;
        drawShadowedText(g, title, titleX, titleY, LARANJA_PADRAO, g.getFont());
        g.setFont(new Font("Lucida Sans", Font.BOLD, 24));
        FontMetrics fmTexto = g.getFontMetrics();
        String texto = p.name;
        int textoX = (boardWidth - fmTexto.stringWidth(texto)) / 2;
        int textoY = 250;
        drawShadowedText(g, texto, textoX, textoY, Color.WHITE, g.getFont());
        String[] ops = {"Yes", "No"};
        Font baseFont = new Font("FFF Forward", Font.BOLD, 14);
        int centerX = boardWidth / 2;
        int buttonY = 320;
        int spacing = 140;
        int buttonWidth = BTN_SMALL;
        for (int i = 0; i < ops.length; i++) {
            final int index = i;
            String text = ops[index];
            int thisCenterX = centerX + (i == 0 ? -spacing / 2 : spacing / 2);
            boolean selected = (index == selectedMenuItem);
            drawRoundedButton(g, text, thisCenterX, buttonY, baseFont, selected, buttonWidth);
            if (selected && index != lastConfirmIndex) {
                playButtonSelect();
                lastConfirmIndex = index;
            }
            clickableOptions.add(new MenuOption(
                    text,
                    thisCenterX - buttonWidth / 2,
                    buttonY + 27,
                    buttonWidth,
                    BTN_HEIGHT_CONFIRM,
                    () -> {
                        selectedMenuItem = index;
                        if (index == 0) {
                            playPoint();
                            modoDificil = !abaNormalAtiva;
                            lastPlayedSlot = selectedSlot;
                            lastPlayedWasHard = !abaNormalAtiva;
                            currentState = GameState.JOGANDO;
                            countdown = 3;
                            countdownTimer.start();
                            score = 0;
                            pipes.clear();
                            bullets.clear();
                            bird.x = birdx;
                            bird.y = birdy;
                            velocityY = 0;
                            birdRotation = 0;
                            statusMessage = "";
                            stopThemeMusic();
                        } else {
                            playSwooshing();
                            currentState = confirmStartFromContinue ? GameState.CONTINUE : GameState.NEW_GAME;
                            statusMessage = "";
                        }
                        repaint();
                    }
            ));
        }
    }

    private void drawDigitarNomePerfil(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(new Font("FFF Forward", Font.BOLD, 36));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String title = "Your Name";
        int titleX = (boardWidth - fmTitle.stringWidth(title)) / 2;
        int titleY = 170;
        drawShadowedText(g2d, title, titleX, titleY, LARANJA_PADRAO, g2d.getFont());
        int boxWidth = 280;
        int boxHeight = 60;
        int boxX = (boardWidth - boxWidth) / 2;
        int boxY = 190;
        g2d.setColor(new Color(30, 30, 40, 220));
        g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 16, 16);
        g2d.setColor(new Color(80, 80, 100));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 16, 16);
        Font textFont = new Font("Lucida Sans", Font.BOLD, 24);
        g2d.setFont(textFont);
        FontMetrics fm = g2d.getFontMetrics();
        String displayText;
        Color textColor;
        if (typingName.isEmpty()) {
            displayText = "Enter your name";
            textColor = new Color(140, 140, 160);
            g2d.setFont(new Font("Lucida Sans", Font.BOLD, 14));
            fm = g2d.getFontMetrics();
        } else {
            displayText = typingName;
            textColor = Color.WHITE;
            g2d.setFont(textFont);
            fm = g2d.getFontMetrics();
        }
        int textWidth = fm.stringWidth(displayText);
        int textX = boxX + (boxWidth - textWidth) / 2;
        int textY = boxY + (boxHeight + fm.getAscent() - fm.getDescent()) / 2;
        drawShadowedText(g2d, displayText, textX, textY, textColor, g2d.getFont());
        if (!typingName.isEmpty() && System.currentTimeMillis() % 800 < 400) {
            int cursorX = textX + textWidth + 2;
            int cursorYTop = textY - fm.getAscent() + 4;
            int cursorHeight = fm.getHeight() - 8;
            g2d.setColor(LARANJA_PADRAO);
            g2d.fillRect(cursorX, cursorYTop, 3, cursorHeight);
        }
        Font buttonFont = new Font("FFF Forward", Font.BOLD, 14);
        int buttonY = boxY + boxHeight + 40;
        int buttonWidth = 110;
        int spacing = 40;
        int totalButtonsWidth = buttonWidth * 2 + spacing;
        int startX = (boardWidth - totalButtonsWidth) / 2;
        boolean confirmSelected = (selectedButtonIndex == 0);
        drawRoundedButton(g2d, "Confirm", startX + buttonWidth / 2, buttonY, buttonFont, confirmSelected, buttonWidth);
        if (confirmSelected && selectedButtonIndex != lastDigitarButtonIndex) {
            playButtonSelect();
            lastDigitarButtonIndex = 0;
        }
        clickableOptions.add(new MenuOption(
                "Confirm",
                startX,
                buttonY + 27,
                buttonWidth,
                BTN_HEIGHT,
                () -> {
                    String novoNome = typingName.trim();
                    if (novoNome.isEmpty()) {
                        showStatusMessage("Enter a name!");
                        repaint();
                        return;
                    }
                    if (nomePerfilJaExiste(novoNome, null)) {
                        showStatusMessage("This name already exists!");
                        repaint();
                        return;
                    }
                    playPoint();
                    PlayerProfile p = currentProfileList.get(selectedSlot);
                    p.name = novoNome;
                    saveRecordsToFile();
                    currentState = GameState.CONFIRM_START_GAME;
                    repaint();
                }
        ));
        boolean cancelSelected = (selectedButtonIndex == 1);
        drawRoundedButton(g2d, "Cancel", startX + buttonWidth + spacing + buttonWidth / 2, buttonY, buttonFont, cancelSelected, buttonWidth);
        if (cancelSelected && selectedButtonIndex != lastDigitarButtonIndex) {
            playButtonSelect();
            lastDigitarButtonIndex = 1;
        }
        clickableOptions.add(new MenuOption(
                "Cancel",
                startX + buttonWidth + spacing,
                buttonY + 27,
                buttonWidth,
                BTN_HEIGHT,
                () -> {
                    if (isEditingName) {
                        playSwooshing();
                    } else {
                        playSwooshing();
                        playDie();
                    }
                    typingName = "";
                    currentState = isEditingName ? GameState.EDIT_PROFILE : GameState.NEW_GAME;
                    isEditingName = false;
                    repaint();
                }
        ));
        if (keyboardVisible) {
            drawVirtualKeyboard(g2d);
        }
        int arrowSize = 48;
        int arrowX = 20;
        int arrowY = boardHeight - arrowSize - 20;
        Image arrowImage = keyboardVisible ? keyboardArrowDown : keyboardArrowUp;
        g2d.drawImage(arrowImage, arrowX, arrowY, arrowSize, arrowSize, null);
        clickableOptions.add(new MenuOption(
                "ToggleKeyboard",
                arrowX,
                arrowY + 40,
                arrowSize,
                arrowSize,
                () -> {
                    keyboardVisible = !keyboardVisible;
                    repaint();
                }
        ));
    }

    private void drawVirtualKeyboard(Graphics2D g2d) {
        long now = System.currentTimeMillis();
        Font keyFont = new Font("Google Sans", Font.PLAIN, 14);
        int keySize = 32;
        int keySpacing = 3;
        int startY = 340;
        int keyboardX = (boardWidth - 45 - (10 * (keySize + keySpacing) - keySpacing)) / 2;
        int keyboardY = startY + 30;
        int keyboardWidth = 11 * (keySize + keySpacing) + keySpacing * 2;
        int keyboardHeight = 4 * (keySize + keySpacing) + keySpacing * 5 + 45;
       
        g2d.setColor(new Color(120, 120, 130, 160));
        g2d.fillRoundRect(keyboardX, keyboardY, keyboardWidth, keyboardHeight, 20, 20);
       
        Color keyBgNormal = new Color(240, 240, 245);
        Color keyBgPressed = new Color(200, 200, 210);
        Color textColor = new Color(30, 30, 40);
       
        String[][] rows;
        if (simbolosAtivo) {
            rows = new String[][] {
                {"1","2","3","4","5","6","7","8","9","0"},
                {"@","#","$","_","&","-","+","(",")","/"},
                {"\\","*","\"","'",";",":","!","?","BACK"},
                {"ABC",",","ESPAÇO",".","ENTER"}
            };
        } else {
            rows = new String[][] {
                {"q","w","e","r","t","y","u","i","o","p"},
                {"a","s","d","f","g","h","j","k","l","ç"},
                {"SHIFT","z","x","c","v","b","n","m","BACK"},
                {"&!",",","ESPAÇO",".","ENTER"}
            };
        }
        int currentY = startY + 50;
        for (String[] rowTeclas : rows) {
            int rowY = currentY;
            int currentX = keyboardX;
            int totalWidth = 0;
            for (String tecla : rowTeclas) {
                int w = keySize;
                switch (tecla) {
                    case "SHIFT", "BACK", "ABC", "&!" -> w = (int) (keySize * 1.5f + keySpacing);
                    case "ESPAÇO" -> w = keySize * 4 + keySpacing * 3;
                    case "ENTER" -> w = keySize * 2 + keySpacing;
                }
                totalWidth += w + keySpacing;
            }
            totalWidth -= keySpacing;
            currentX += (keyboardWidth - totalWidth) / 2;
            for (String tecla : rowTeclas) {
                String display = tecla;
                int width = keySize;
                String actionKey = tecla;
                switch (tecla) {
                    case "SHIFT", "BACK", "ABC", "&!" -> {
                        width = (int) (keySize * 1.5f + keySpacing);
                    }
                    case "ESPAÇO" -> {
                        display = "ESPAÇO";
                        width = keySize * 4 + keySpacing * 3;
                        actionKey = "ESPAÇO";
                    }
                    case "ENTER" -> {
                        display = "ENTER";
                        width = keySize * 2 + keySpacing;
                        actionKey = "ENTER";
                    }
                }
                int x = currentX;
                int y = rowY;
                boolean pressed = keyPressTimestamps.containsKey((int) display.charAt(0)) &&
                                 (now - keyPressTimestamps.get((int) display.charAt(0))) < KEY_FLASH_DURATION;
                boolean isShiftActiveVisual = shiftActive || physicalShiftHeld || capsLockActive;
                g2d.setColor(pressed ? keyBgPressed : keyBgNormal);
                if (actionKey.equals("SHIFT") && isShiftActiveVisual) {
                    g2d.setColor(keyBgPressed);
                }
                if (actionKey.equals("ENTER")) {
                    g2d.setColor(pressed ? keyBgPressed : new Color(80, 160, 240));
                }
                g2d.fillRoundRect(x, y, width, keySize, 12, 12);
                g2d.setColor(textColor);
                if (actionKey.equals("ENTER")) g2d.setColor(Color.WHITE);
                Font drawFont = keyFont;
                if (display.equals("⌫") || display.equals("SHIFT") || display.equals("ABC") || display.equals("&!")) {
                    drawFont = keyFont.deriveFont(16f);
                }
                g2d.setFont(drawFont);
                FontMetrics fm = g2d.getFontMetrics();
                String textToDraw = display;
                if (!simbolosAtivo && isShiftActiveVisual && display.length() == 1 && Character.isLetter(display.charAt(0))) {
                    textToDraw = String.valueOf(Character.toUpperCase(display.charAt(0)));
                }
                int tx = x + (width - fm.stringWidth(textToDraw)) / 2;
                int ty = y + (keySize + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(textToDraw, tx, ty);
                final String finalActionKey = actionKey;
                clickableOptions.add(new MenuOption(
                    finalActionKey,
                    x, y + 20, width, keySize,
                    () -> {
                        processarTeclaTecladoVirtual(finalActionKey);
                        keyPressTimestamps.put((int) finalActionKey.charAt(0), now);
                        repaint();
                    }
                ));
                currentX += width + keySpacing;
            }
            currentY += keySize + keySpacing + 2;
        }
    }

    private void processarTeclaTecladoVirtual(String key) {
        switch (key) {
            case "ESPAÇO" -> {
                if (typingName.length() < 20) {
                    typingName += " ";
                }
            }
            case "," -> {
                if (typingName.length() < 20) {
                    typingName += ",";
                }
            }
            case "." -> {
                if (typingName.length() < 20) {
                    typingName += ".";
                }
            }
            case "BACK" -> {
                if (typingName.length() > 0) {
                    typingName = typingName.substring(0, typingName.length() - 1);
                }
            }
            case "ENTER" -> {
                String novoNome = typingName.trim();
                if (novoNome.isEmpty()) {
                    showStatusMessage("Enter a name!");
                    return;
                }
                if (nomePerfilJaExiste(novoNome, null)) {
                    showStatusMessage("This name already exists!");
                    return;
                }
                playPoint();
                PlayerProfile p = currentProfileList.get(selectedSlot);
                p.name = novoNome;
                saveRecordsToFile();
                currentState = GameState.CONFIRM_START_GAME;
            }
            case "SHIFT" -> {
                shiftActive = !shiftActive;
            }
            case "&!" -> {
                simbolosAtivo = true;
                shiftActive = false;
            }
            case "ABC" -> {
                simbolosAtivo = false;
                shiftActive = false;
            }
            default -> {
                if (typingName.length() < 20) {
                    if (!simbolosAtivo && shiftActive && Character.isLetter(key.charAt(0))) {
                        typingName += key.toUpperCase();
                        shiftActive = false;
                    } else {
                        typingName += key;
                    }
                }
            }
        }
        repaint();
    }

    private boolean isValidBindableKey(int keyCode) {
        if (keyCode == KeyEvent.VK_ESCAPE ||
            keyCode == KeyEvent.VK_ENTER ||
            keyCode == KeyEvent.VK_SHIFT ||
            keyCode == KeyEvent.VK_CONTROL ||
            keyCode == KeyEvent.VK_ALT ||
            keyCode == KeyEvent.VK_WINDOWS ||
            (keyCode >= KeyEvent.VK_F1 && keyCode <= KeyEvent.VK_F12)) {
            return false;
        }
        return keyCode > 0 && keyCode < 600;
    }

    private boolean isKeyAlreadyUsed(int keyCode, int excludeOption) {
        if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_ENTER) return true;
        if (excludeOption != 3 && tempJumpKey == keyCode) return true;
        if (excludeOption != 4 && tempPauseKey == keyCode) return true;
        return false;
    }

    private void drawConfiguracoes(Graphics2D g) {
        g.setFont(new Font("FFF Forward", Font.BOLD, 32));
        FontMetrics fmTitulo = g.getFontMetrics();
        String title = "Configurations";
        int titleX = (boardWidth - fmTitulo.stringWidth(title)) / 2;
        int titleY = 160;
        drawShadowedText(g, title, titleX, titleY, LARANJA_PADRAO, g.getFont());
        int shadowHeight = 10;
        int shadowOpacity = 225;
        GradientPaint shadowTop = new GradientPaint(
                CONFIG_BOX_X, CONFIG_BOX_Y - shadowHeight, new Color(47, 146, 212, 0),
                CONFIG_BOX_X, CONFIG_BOX_Y, new Color(47, 146, 212, shadowOpacity)
        );
        g.setPaint(shadowTop);
        g.fillRoundRect(CONFIG_BOX_X, CONFIG_BOX_Y - shadowHeight, CONFIG_BOX_WIDTH, shadowHeight + 5, 20, 20);
        int clipStartY = CONFIG_BOX_Y + 5;
        int clipHeight = CONFIG_BOX_HEIGHT - 10;
        Shape oldClip = g.getClip();
        g.setClip(CONFIG_BOX_X, clipStartY, CONFIG_BOX_WIDTH, clipHeight);
        int topPadding = 45;
        int baseY = CONFIG_BOX_Y + topPadding - configScrollOffset;
        int leftMargin = CONFIG_BOX_X + 20;
        int childMargin = leftMargin + 10;
        int inputTitleY = baseY;
        g.setFont(new Font("FFF Forward", Font.BOLD, 24));
        drawShadowedText(g, "Input Mode", leftMargin, inputTitleY, Color.WHITE, g.getFont());
        int volumeTitleY = inputTitleY + 40 + 3 * 55 + 40;
        g.setFont(new Font("FFF Forward", Font.BOLD, 24));
        drawShadowedText(g, "Volume", leftMargin, volumeTitleY, Color.WHITE, g.getFont());
        int buttonsTitleY = volumeTitleY + 50 + 100 + 50 + 50;
        g.setFont(new Font("FFF Forward", Font.BOLD, 24));
        drawShadowedText(g, "Buttons", leftMargin, buttonsTitleY, Color.WHITE, g.getFont());
        if (configFocusOnSettings) {
            switch (selectedConfigOption) {
                case 0 -> {
                    g.setColor(new Color(LARANJA_PADRAO.getRed(), LARANJA_PADRAO.getGreen(), LARANJA_PADRAO.getBlue(), 60));
                    g.fillRoundRect(leftMargin - 15, inputTitleY - 35, HIGHLIGHT_WIDTH + 30, HIGHLIGHT_HEIGHT, 15, 15);
                    g.setColor(LARANJA_PADRAO);
                    g.fillOval(leftMargin - 20, inputTitleY - 20, SELECTOR_RADIUS * 2, SELECTOR_RADIUS * 2);
                }
                case 1 -> {
                    int effectsY = volumeTitleY + 50;
                    g.setColor(new Color(LARANJA_PADRAO.getRed(), LARANJA_PADRAO.getGreen(), LARANJA_PADRAO.getBlue(), 60));
                    g.fillRoundRect(leftMargin - 5, effectsY - 30, HIGHLIGHT_WIDTH + 30, HIGHLIGHT_HEIGHT - 10, 15, 15);
                    g.setColor(LARANJA_PADRAO);
                    g.fillOval(leftMargin - 10, effectsY - 20, SELECTOR_RADIUS * 2, SELECTOR_RADIUS * 2);
                }
                case 2 -> {
                    int musicY = volumeTitleY + 50 + 100;
                    g.setColor(new Color(LARANJA_PADRAO.getRed(), LARANJA_PADRAO.getGreen(), LARANJA_PADRAO.getBlue(), 60));
                    g.fillRoundRect(leftMargin - 5, musicY - 30, HIGHLIGHT_WIDTH + 30, HIGHLIGHT_HEIGHT - 10, 15, 15);
                    g.setColor(LARANJA_PADRAO);
                    g.fillOval(leftMargin - 10, musicY - 20, SELECTOR_RADIUS * 2, SELECTOR_RADIUS * 2);
                }
                case 3 -> {
                    int jumpY = buttonsTitleY + 50;
                    g.setColor(new Color(LARANJA_PADRAO.getRed(), LARANJA_PADRAO.getGreen(), LARANJA_PADRAO.getBlue(), 60));
                    g.fillRoundRect(leftMargin - 5, jumpY - 30, HIGHLIGHT_WIDTH + 30, HIGHLIGHT_HEIGHT - 5, 15, 15);
                    g.setColor(LARANJA_PADRAO);
                    g.fillOval(leftMargin - 10, jumpY - 20, SELECTOR_RADIUS * 2, SELECTOR_RADIUS * 2);
                }
                case 4 -> {
                    int pauseY = buttonsTitleY + 50 + 50;
                    g.setColor(new Color(LARANJA_PADRAO.getRed(), LARANJA_PADRAO.getGreen(), LARANJA_PADRAO.getBlue(), 60));
                    g.fillRoundRect(leftMargin - 5, pauseY - 30, HIGHLIGHT_WIDTH + 30, HIGHLIGHT_HEIGHT - 5, 15, 15);
                    g.setColor(LARANJA_PADRAO);
                    g.fillOval(leftMargin - 10, pauseY - 20, SELECTOR_RADIUS * 2, SELECTOR_RADIUS * 2);
                }
            }
        }
        int radioYStart = inputTitleY + 40;
        int radioX = childMargin;
        String[] modes = {"Keyboard Only", "Mouse Only", "Hybrid"};
        for (int i = 0; i < modes.length; i++) {
            int radioY = radioYStart + i * 55;
            boolean isSelected = (tempControlMode.ordinal() == i);
            g.setColor(Color.WHITE);
            g.drawOval(radioX, radioY - RADIO_RADIUS, RADIO_RADIUS * 2, RADIO_RADIUS * 2);
            if (isSelected && configFocusOnSettings) {
                g.setColor(LARANJA_PADRAO);
                g.fillOval(radioX + 4, radioY - RADIO_RADIUS + 4, RADIO_RADIUS * 2 - 8, RADIO_RADIUS * 2 - 8);
            }
            if (configFocusOnSettings && selectedConfigOption == 0 && isSelected) {
                g.setStroke(new BasicStroke(3f));
                g.setColor(LARANJA_PADRAO);
                g.drawOval(radioX - 1, radioY - RADIO_RADIUS - 1, RADIO_RADIUS * 2 + 2, RADIO_RADIUS * 2 + 2);
                g.setStroke(new BasicStroke(1f));
            }
            g.setColor(isSelected ? LARANJA_PADRAO : Color.WHITE);
            g.setFont(new Font("Lucida Sans", Font.BOLD, 18));
            String modeText = modes[i];
            drawShadowedText(g, modeText, radioX + 40, radioY + 8, isSelected ? LARANJA_PADRAO : Color.WHITE, g.getFont());
        }
        int volumeStartY = volumeTitleY + 50;
        int effectsY = volumeStartY;
        g.setFont(new Font("Lucida Sans", Font.BOLD, 18));
        drawShadowedText(g, "Effects:", childMargin, effectsY, Color.WHITE, g.getFont());
        int lineYEffects = effectsY + 30;
        int iconXEffects = childMargin;
        int iconSize = 32;
        Image effectsIcon = (tempVolumeEffects == 0) ? altoFalanteSilenciado : altoFalanteAtivo;
        g.drawImage(effectsIcon, iconXEffects, lineYEffects - iconSize / 2, iconSize, iconSize, null);
        int barXEffects = iconXEffects + iconSize + 8;
        int barWidthEffects = CONFIG_BOX_WIDTH - 150 - (iconSize + 8);
        int barYEffects = lineYEffects + BAR_HEIGHT / 2 - BAR_HEIGHT / 2;
        g.setColor(new Color(60, 60, 60));
        g.fillRoundRect(barXEffects, barYEffects, barWidthEffects, BAR_HEIGHT, 12, 12);
        g.setColor(LARANJA_PADRAO);
        int fillWidthEffects = (int)(barWidthEffects * tempVolumeEffects);
        g.fillRoundRect(barXEffects, barYEffects, fillWidthEffects, BAR_HEIGHT, 12, 12);
        g.setColor(Color.WHITE);
        g.drawRoundRect(barXEffects, barYEffects, barWidthEffects, BAR_HEIGHT, 12, 12);
        int knobXEffects = barXEffects + fillWidthEffects;
        int knobYEffects = barYEffects + BAR_HEIGHT / 2 - VOLUME_KNOB_SIZE / 2;
        g.setColor(new Color(255, 220, 100));
        g.fillOval(knobXEffects - VOLUME_KNOB_SIZE/2, knobYEffects, VOLUME_KNOB_SIZE, VOLUME_KNOB_SIZE);
        g.setColor(Color.WHITE);
        g.drawOval(knobXEffects - VOLUME_KNOB_SIZE/2, knobYEffects, VOLUME_KNOB_SIZE, VOLUME_KNOB_SIZE);
        g.setFont(new Font("Lucida Sans", Font.BOLD, 18));
        g.setColor(tempVolumeEffects == 0 ? Color.LIGHT_GRAY : Color.WHITE);
        String percentText = (int)(tempVolumeEffects * 100) + "%";
        drawShadowedText(g, percentText, barXEffects + barWidthEffects + 20, lineYEffects + 8, tempVolumeEffects == 0 ? Color.LIGHT_GRAY : Color.WHITE, g.getFont());
        int musicY = effectsY + 100;
        g.setFont(new Font("Lucida Sans", Font.BOLD, 18));
        drawShadowedText(g, "Music:", childMargin, musicY, Color.WHITE, g.getFont());
        int lineYMusic = musicY + 30;
        int iconXMusic = childMargin;
        Image musicIcon = (tempVolumeMusic == 0) ? altoFalanteSilenciado : altoFalanteAtivo;
        g.drawImage(musicIcon, iconXMusic, lineYMusic - iconSize / 2, iconSize, iconSize, null);
        int barXMusic = iconXMusic + iconSize + 8;
        int barWidthMusic = CONFIG_BOX_WIDTH - 150 - (iconSize + 8);
        int barYMusic = lineYMusic + BAR_HEIGHT / 2 - BAR_HEIGHT / 2;
        g.setColor(new Color(60, 60, 60));
        g.fillRoundRect(barXMusic, barYMusic, barWidthMusic, BAR_HEIGHT, 12, 12);
        g.setColor(LARANJA_PADRAO);
        int fillWidthMusic = (int)(barWidthMusic * tempVolumeMusic);
        g.fillRoundRect(barXMusic, barYMusic, fillWidthMusic, BAR_HEIGHT, 12, 12);
        g.setColor(Color.WHITE);
        g.drawRoundRect(barXMusic, barYMusic, barWidthMusic, BAR_HEIGHT, 12, 12);
        int knobXMusic = barXMusic + fillWidthMusic;
        int knobYMusic = barYMusic + BAR_HEIGHT / 2 - VOLUME_KNOB_SIZE / 2;
        g.setColor(new Color(255, 220, 100));
        g.fillOval(knobXMusic - VOLUME_KNOB_SIZE/2, knobYMusic, VOLUME_KNOB_SIZE, VOLUME_KNOB_SIZE);
        g.setColor(Color.WHITE);
        g.drawOval(knobXMusic - VOLUME_KNOB_SIZE/2, knobYMusic, VOLUME_KNOB_SIZE, VOLUME_KNOB_SIZE);
        g.setFont(new Font("Lucida Sans", Font.BOLD, 18));
        g.setColor(tempVolumeMusic == 0 ? Color.LIGHT_GRAY : Color.WHITE);
        String musicPercent = (int)(tempVolumeMusic * 100) + "%";
        drawShadowedText(g, musicPercent, barXMusic + barWidthMusic + 20, lineYMusic + 8, tempVolumeMusic == 0 ? Color.LIGHT_GRAY : Color.WHITE, g.getFont());
        int jumpY = buttonsTitleY + 50;
        String jumpText = "Jump: " + KeyEvent.getKeyText(tempJumpKey);
        Font lucida18 = new Font("Lucida Sans", Font.BOLD, 18);
        drawShadowedText(g, jumpText, childMargin, jumpY, Color.WHITE, lucida18);
        int pauseY = jumpY + 50;
        String pauseText = "Pause: " + KeyEvent.getKeyText(tempPauseKey);
        drawShadowedText(g, pauseText, childMargin, pauseY, Color.WHITE, lucida18);
        int infoY = pauseY + 50;
        g.setFont(new Font("Lucida Sans", Font.BOLD, 18));
        String confirmText = "Confirm: ENTER";
        drawShadowedText(g, confirmText, childMargin, infoY, Color.WHITE, g.getFont());
        String cancelText = "Cancel: ESC";
        drawShadowedText(g, cancelText, childMargin, infoY + 50, Color.WHITE, g.getFont());
        g.setClip(oldClip);
        int scrollbarStartY = CONFIG_BOX_Y + 15;
        int scrollbarFullHeight = clipHeight - 30;
        int totalContentHeight = 720;
        int maxScroll = Math.max(0, totalContentHeight - scrollbarFullHeight);
        if (maxScroll > 0) {
            float ratio = (float) configScrollOffset / maxScroll;
            int thumbHeight = Math.max(40, (int) (scrollbarFullHeight * scrollbarFullHeight / (float) totalContentHeight));
            int thumbY = scrollbarStartY + (int) ((scrollbarFullHeight - thumbHeight) * ratio);
            g.setColor(new Color(180, 180, 180, 220));
            g.fillRoundRect(CONFIG_BOX_X + CONFIG_BOX_WIDTH - 18, thumbY, 14, thumbHeight, 10, 10);
            g.setColor(new Color(220, 220, 220));
            g.drawRoundRect(CONFIG_BOX_X + CONFIG_BOX_WIDTH - 18, thumbY, 14, thumbHeight, 10, 10);
        }
        int btnY = boardHeight - 70;
        int saveWidth = 90;
        int resetWidth = 130;
        int backWidth = 90;
        int spacing = 20;
        int totalWidth = saveWidth + spacing + resetWidth + spacing + backWidth;
        int startX = (boardWidth - totalWidth) / 2;
        boolean saveSelected = !configFocusOnSettings && selectedButtonIndex == 0;
        boolean resetSelected = !configFocusOnSettings && selectedButtonIndex == 1;
        boolean backSelected = !configFocusOnSettings && selectedButtonIndex == 2;
        Font buttonFont = new Font("FFF Forward", Font.BOLD, 14);
        int saveCenterX = startX + saveWidth / 2;
        drawRoundedButton(g, "SAVE", saveCenterX, btnY, buttonFont, saveSelected, saveWidth);
        clickableOptions.add(new MenuOption("SAVE", startX, btnY - BTN_HEIGHT / 2 + 45, saveWidth, BTN_HEIGHT,
                () -> {
                    controlMode = tempControlMode;
                    volumeEffects = tempVolumeEffects;
                    volumeMusic = tempVolumeMusic;
                    jumpKey = tempJumpKey;
                    pauseKey = tempPauseKey;
                    applyVolumeToAllClips();
                    saveConfigToFile();
                    showStatusMessage("Saved Settings!");
                    repaint();
                }));
        int resetCenterX = startX + saveWidth + spacing + resetWidth / 2;
        drawRoundedButton(g, "RESET", resetCenterX, btnY, buttonFont, resetSelected, resetWidth);
        clickableOptions.add(new MenuOption("RESET", startX + saveWidth + spacing, btnY - BTN_HEIGHT / 2 + 45, resetWidth, BTN_HEIGHT,
                () -> {
                    resetToDefaultConfig();
                    tempControlMode = controlMode;
                    tempVolumeEffects = volumeEffects;
                    tempVolumeMusic = volumeMusic;
                    tempJumpKey = jumpKey;
                    tempPauseKey = pauseKey;
                    applyVolumeToAllClips();
                    showStatusMessage("Settings Reset!");
                    repaint();
                }));
        int backCenterX = startX + saveWidth + spacing + resetWidth + spacing + backWidth / 2;
        drawRoundedButton(g, "BACK", backCenterX, btnY, buttonFont, backSelected, backWidth);
        clickableOptions.add(new MenuOption("BACK", startX + saveWidth + spacing + resetWidth + spacing, btnY - BTN_HEIGHT / 2 + 45, backWidth, BTN_HEIGHT,
                () -> {
                    playSwooshing();
                    if (cameFromPauseToConfig) {
                        currentState = GameState.JOGANDO;
                        paused = true;
                        pauseMenuActive = true;
                        cameFromPauseToConfig = false;
                    } else {
                        currentState = GameState.MENU_PRINCIPAL;
                        selectedMenuItem = 0;
                        configScrollOffset = 0;
                        tempControlMode = controlMode;
                        tempVolumeEffects = volumeEffects;
                        tempVolumeMusic = volumeMusic;
                        tempJumpKey = jumpKey;
                        tempPauseKey = pauseKey;
                        configFocusOnSettings = true;
                    }
                    repaint();
                }));
        if (!configFocusOnSettings) {
            if (selectedButtonIndex != lastConfigButtonIndex) {
                playButtonSelect();
                lastConfigButtonIndex = selectedButtonIndex;
            }
        } else {
            lastConfigButtonIndex = -1;
        }
    }

    private void salvarScoreAtual() {
        if (currentProfileList == null || selectedSlot < 0) return;
        ArrayList<PlayerProfile> listaCorreta = modoDificil ? profilesHard : profilesNormal;
        if (currentProfileList != listaCorreta) currentProfileList = listaCorreta;
        PlayerProfile perfil = currentProfileList.get(selectedSlot);
        perfil.scores.add(score);
        perfil.scores.sort(Collections.reverseOrder());
        if (perfil.scores.size() > 20) perfil.scores = new ArrayList<>(perfil.scores.subList(0, 20));
        saveRecordsToFile();
    }

    private void move() {
        animationCounter++;
        if (animationCounter >= animationSpeed) {
            animationCounter = 0;
            currentWingFrame = (currentWingFrame + 1) % 3;
            bird.img = birdImg[currentWingFrame];
        }
        boolean showMenuBird = currentState == GameState.MENU_PRINCIPAL ||
                currentState == GameState.NEW_GAME ||
                currentState == GameState.CONTINUE ||
                currentState == GameState.EDIT_PROFILE ||
                currentState == GameState.CONFIRM_DELETE_PROFILE ||
                currentState == GameState.CONFIRM_EDIT_PROFILE ||
                currentState == GameState.CONFIRM_START_GAME ||
                currentState == GameState.DIGITAR_NOME_PERFIL ||
                currentState == GameState.GAME_OVER ||
                currentState == GameState.CONFIGURACOES ||
                currentState == GameState.RECORDS;
        if (showMenuBird) {
            backGround1 += menuBackgroundSpeed;
            backGround2 += menuBackgroundSpeed;
            backGround3 += menuBackgroundSpeed;
            if (backGround1 + boardWidth <= 0) backGround1 = Math.max(backGround2, backGround3) + boardWidth;
            if (backGround2 + boardWidth <= 0) backGround2 = Math.max(backGround1, backGround3) + boardWidth;
            if (backGround3 + boardWidth <= 0) backGround3 = Math.max(backGround1, backGround2) + boardWidth;
            double[] positions = {backGround1, backGround2, backGround3};
            java.util.Arrays.sort(positions);
            backGround1 = positions[0];
            backGround2 = positions[1];
            backGround3 = positions[2];
            if (backGround2 < backGround1 + boardWidth) backGround2 = backGround1 + boardWidth;
            if (backGround3 < backGround2 + boardWidth) backGround3 = backGround2 + boardWidth;
        }
        if (paused) return;
        if (currentState != GameState.JOGANDO) return;
        if (countdown > 0) {
            bird.y = birdy;
            velocityY = 0;
            birdRotation = 0;
            return;
        }
        birdRotation = velocityY * 3.0;
        birdRotation = Math.max(-30, Math.min(90, birdRotation));
        backGround1 += backGroundSpeed;
        backGround2 += backGroundSpeed;
        backGround3 += backGroundSpeed;
        if (backGround1 <= -boardWidth) backGround1 += boardWidth * 3;
        if (backGround2 <= -boardWidth) backGround2 += boardWidth * 3;
        if (backGround3 <= -boardWidth) backGround3 += boardWidth * 3;
        for (int i = 0; i < pipes.size(); i += 2) {
            Pipe top = pipes.get(i);
            Pipe bottom = pipes.get(i + 1);
            top.x += backGroundSpeed;
            bottom.x += backGroundSpeed;
            if (modoDificil) {
                top.updateVertical();
                bottom.y = top.y + 512 + 180;
            }
            if (!top.passed && bird.x > top.x + top.width) {
                top.passed = true;
                score++;
                playPoint();
            }
            if (collision(bird, top) || collision(bird, bottom)) {
                if (showGoForFrames <= 0) {
                    salvarScoreAtual();
                    currentState = GameState.GAME_OVER;
                    pipes.clear();
                    bullets.clear();
                    bird.x = (boardWidth - birdWidth) / 2;
                    bird.y = 100;
                    velocityY = 0;
                    birdRotation = 0;
                    playHit();
                    playDie();
                    stopThemeMusic();
                }
                break;
            }
        }
        pipes.removeIf(pipe -> pipe.x + pipe.width < 0);
        for (int i = bullets.size() - 1; i >= 0; i--) {
            BulletBill b = bullets.get(i);
            b.x += backGroundSpeed * 2;
            if (b.x + b.width < 0) {
                bullets.remove(i);
            } else if (collision(bird, b)) {
                if (showGoForFrames <= 0) {
                    salvarScoreAtual();
                    currentState = GameState.GAME_OVER;
                    pipes.clear();
                    bullets.clear();
                    bird.x = (boardWidth - birdWidth) / 2;
                    bird.y = 100;
                    velocityY = 0;
                    birdRotation = 0;
                    playHit();
                    playDie();
                    stopThemeMusic();
                }
                break;
            }
        }
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, -birdHeight);
        if (bird.y + birdHeight > boardHeight && showGoForFrames <= 0) {
            salvarScoreAtual();
            currentState = GameState.GAME_OVER;
            pipes.clear();
            bullets.clear();
            bird.x = (boardWidth - birdWidth) / 2;
            bird.y = 100;
            velocityY = 0;
            birdRotation = 0;
            playDie();
            stopThemeMusic();
        }
        if (showGoForFrames > 0) showGoForFrames--;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.currentTimeMillis();
        long delta = now - lastUpdateTime;
        lastUpdateTime = now;

        if (!paused && currentState == GameState.JOGANDO) {

            pipeAccumulator += delta;
            if (pipeAccumulator >= pipeInterval) {
                placePipes();
                pipeAccumulator -= pipeInterval;
            }

            if (modoDificil) {
                bulletAccumulator += delta;
                if (bulletAccumulator >= bulletInterval) {
                    spawnBullet();
                    bulletAccumulator -= bulletInterval;
                }
            }
        }

        move();
        repaint();

        if (currentState == GameState.GAME_OVER) {}
    }

    private void handlePauseMenuEnter() {
        switch (selectedPauseOption) {
            case 0:
                paused = false;
                pauseMenuActive = false;
                if (isGameActive()) {
                    
                    
                }
                break;
            case 1:
                playSwooshing();
                cameFromPauseToConfig = true;
                tempControlMode = controlMode;
                tempVolumeEffects = volumeEffects;
                tempVolumeMusic = volumeMusic;
                tempJumpKey = jumpKey;
                tempPauseKey = pauseKey;
                currentState = GameState.CONFIGURACOES;
                selectedConfigOption = 0;
                configScrollOffset = 0;
                configFocusOnSettings = true;
                pauseMenuActive = false;
                stopThemeMusic();
                break;
            case 2:
                paused = false;
                pauseMenuActive = false;
                currentState = GameState.MENU_PRINCIPAL;
                selectedMenuItem = 0;
                score = 0;
                pipes.clear();
                bullets.clear();
                bird.x = birdx;
                bird.y = birdy;
                velocityY = 0;
                birdRotation = 0;
                playSwooshing();
                if (lastPlayedSlot >= 0) {
                    abaNormalAtiva = !lastPlayedWasHard;
                    selectedSlotNormal = lastPlayedWasHard ? selectedSlotNormal : lastPlayedSlot;
                    selectedSlotHard = lastPlayedWasHard ? lastPlayedSlot : selectedSlotHard;
                    selectedSlot = lastPlayedSlot;
                    currentProfileList = abaNormalAtiva ? profilesNormal : profilesHard;
                    scrollOffsetNormal = 0;
                    scrollOffsetHard = 0;
                    slotsFocusOnBack = false;
                    slotsFocusOnTabs = true;
                    adjustScrollToSelected();
                }
                startThemeMusic();
                break;
        }
    }

    private void handleGameOverNewEnter() {
        switch (selectedGameOverOption) {
            case 0:
                score = 0;
                pipes.clear();
                bullets.clear();
                bird.x = birdx;
                bird.y = birdy;
                velocityY = 0;
                birdRotation = 0;
                countdown = 3;
                showGoForFrames = 0;
                countdownTimer.start();
                currentState = GameState.JOGANDO;
                selectedGameOverOption = 0;
                stopThemeMusic();
                break;
            case 1:
                playSwooshing();
                updateTopScores();
                recordsVindoDoGameOver = true;
                recordsAbaNormal = !lastPlayedWasHard;
                showOnlyCurrentProfileRecords = true;
                currentProfileList = lastPlayedWasHard ? profilesHard : profilesNormal;
                selectedSlot = lastPlayedSlot;
                recordsScrollOffset = 0;
                recordsFocusOnTabs = true;
                previousStateBeforeRecords = GameState.GAME_OVER;
                currentState = GameState.RECORDS;
                stopThemeMusic();
                break;
            case 2:
                playSwooshing();
                currentState = GameState.MENU_PRINCIPAL;
                selectedMenuItem = 0;
                selectedGameOverOption = 0;
                score = 0;
                pipes.clear();
                bullets.clear();
                bird.x = birdx;
                bird.y = birdy;
                velocityY = 0;
                birdRotation = 0;
                if (lastPlayedSlot >= 0) {
                    abaNormalAtiva = !lastPlayedWasHard;
                    selectedSlotNormal = lastPlayedWasHard ? selectedSlotNormal : lastPlayedSlot;
                    selectedSlotHard = lastPlayedWasHard ? lastPlayedSlot : selectedSlotHard;
                    selectedSlot = lastPlayedSlot;
                    currentProfileList = abaNormalAtiva ? profilesNormal : profilesHard;
                    scrollOffsetNormal = 0;
                    scrollOffsetHard = 0;
                    slotsFocusOnBack = false;
                    slotsFocusOnTabs = true;
                    adjustScrollToSelected();
                }
                startThemeMusic();
                break;
        }
        repaint();
    }

    private void handleMenuPrincipalEnter() {
        if (selectedMenuItem == 0) {
            playSwooshing();
            currentState = GameState.NEW_GAME;
            abaNormalAtiva = true;
            selectedSlotNormal = 0;
            selectedSlotHard = 0;
            selectedSlot = selectedSlotNormal;
            scrollOffsetNormal = 0;
            scrollOffsetHard = 0;
            slotsFocusOnBack = false;
            slotsFocusOnTabs = true;
        } else if (hasAnySave() && selectedMenuItem == 1) {
            playSwooshing();
            currentState = GameState.CONTINUE;
            abaNormalAtiva = true;
            selectedSlotNormal = 0;
            selectedSlotHard = 0;
            selectedSlot = selectedSlotNormal;
            scrollOffsetNormal = 0;
            scrollOffsetHard = 0;
            slotsFocusOnBack = false;
            slotsFocusOnTabs = true;
        } else if ((hasAnySave() && selectedMenuItem == 2) || (!hasAnySave() && selectedMenuItem == 1)) {
            playSwooshing();
            tempControlMode = controlMode;
            tempVolumeEffects = volumeEffects;
            tempVolumeMusic = volumeMusic;
            tempJumpKey = jumpKey;
            tempPauseKey = pauseKey;
            currentState = GameState.CONFIGURACOES;
            selectedConfigOption = 0;
            configScrollOffset = 0;
            configFocusOnSettings = true;
            requestFocusInWindow();
        } else if ((hasAnySave() && selectedMenuItem == 3) || (!hasAnySave() && selectedMenuItem == 2)) {
            playSwooshing();
            updateTopScores();
            recordsVindoDoGameOver = false;
            recordsAbaNormal = true;
            showOnlyCurrentProfileRecords = false;
            recordsFocusOnTabs = true;
            recordsScrollOffset = 0;
            currentState = GameState.RECORDS;
        }
        if ((currentState == GameState.NEW_GAME || currentState == GameState.CONTINUE) && lastPlayedSlot >= 0) {
            abaNormalAtiva = !lastPlayedWasHard;
            selectedSlotNormal = lastPlayedWasHard ? selectedSlotNormal : lastPlayedSlot;
            selectedSlotHard = lastPlayedWasHard ? lastPlayedSlot : selectedSlotHard;
            selectedSlot = lastPlayedSlot;
            currentProfileList = abaNormalAtiva ? profilesNormal : profilesHard;
            scrollOffsetNormal = 0;
            scrollOffsetHard = 0;
            slotsFocusOnBack = false;
            slotsFocusOnTabs = true;
            adjustScrollToSelected();
        }
        repaint();
    }

    private void handleEditProfileEnter() {
        if (selectedMenuItem == 0) {
            PlayerProfile p = currentProfileList.get(selectedSlot);
            typingName = p.name;
            playSwooshing();
            isEditingName = true;
            currentState = GameState.DIGITAR_NOME_PERFIL;
            selectedButtonIndex = 0;
        } else if (selectedMenuItem == 1) {
            playSwooshing();
            currentState = GameState.CONFIRM_DELETE_PROFILE;
        } else if (selectedMenuItem == 2) {
            playSwooshing();
            currentState = GameState.NEW_GAME;
        }
        repaint();
    }

    private boolean hasAnySave() {
        for (PlayerProfile p : profilesNormal) if (!p.isEmpty()) return true;
        for (PlayerProfile p : profilesHard) if (!p.isEmpty()) return true;
        return false;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (currentState == GameState.JOGANDO && paused && pauseMenuActive) {
            switch (key) {
                case KeyEvent.VK_UP, KeyEvent.VK_W -> {
                    int prev = selectedPauseOption;
                    selectedPauseOption = (selectedPauseOption - 1 + 3) % 3;
                    if (selectedPauseOption != prev) playButtonSelect();
                    repaint();
                    return;
                }
                case KeyEvent.VK_DOWN, KeyEvent.VK_S -> {
                    int prev = selectedPauseOption;
                    selectedPauseOption = (selectedPauseOption + 1) % 3;
                    if (selectedPauseOption != prev) playButtonSelect();
                    repaint();
                    return;
                }
                case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> {
                    handlePauseMenuEnter();
                    repaint();
                    return;
                }
                case KeyEvent.VK_ESCAPE -> {
                    paused = false;
                    pauseMenuActive = false;
                    if (isGameActive()) {
                        
                        
                    }
                    repaint();
                    return;
                }
            }
        }
        if (currentState == GameState.JOGANDO) {
            if (countdown > 0 || paused) return;
            if (key == jumpKey) {
                if (controlMode != ControlMode.MOUSE_ONLY) {
                    velocityY = jumpStrength;
                    playWing();
                }
            }
            if (key == pauseKey) {
                paused = !paused;
                if (paused) {
                    pauseMenuActive = true;
                    selectedPauseOption = 0;
                    cameFromPauseToConfig = false;
                } else {
                    if (isGameActive()) {
                        
                        
                    }
                    pauseMenuActive = false;
                }
                repaint();
                return;
            }
        }
        if (currentState == GameState.CONFIGURACOES) {
            if (key == KeyEvent.VK_TAB) {
                configFocusOnSettings = !configFocusOnSettings;
                if (configFocusOnSettings) {
                    selectedConfigOption = 0;
                    configScrollOffset = 0;
                } else {
                    selectedButtonIndex = 0;
                    configScrollOffset = 450;
                }
                repaint();
                return;
            }
            if (configFocusOnSettings) {
                int totalOptions = 5;
                boolean selectionChanged = false;
                switch (key) {
                    case KeyEvent.VK_UP -> {
                        if (selectedConfigOption > 0) {
                            selectedConfigOption--;
                            selectionChanged = true;
                        }
                    }
                    case KeyEvent.VK_DOWN -> {
                        if (selectedConfigOption < totalOptions - 1) {
                            selectedConfigOption++;
                            selectionChanged = true;
                        }
                    }
                    case KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT -> {
                        int delta = (key == KeyEvent.VK_LEFT) ? -1 : 1;
                        switch (selectedConfigOption) {
                            case 0 -> {
                                int idx = tempControlMode.ordinal();
                                idx = (idx + delta + 3) % 3;
                                tempControlMode = ControlMode.values()[idx];
                            }
                            case 1 -> {
                                tempVolumeEffects = Math.max(0.0f, Math.min(1.0f, tempVolumeEffects + delta * 0.1f));
                                applyTempVolumesToAllClips();
                            }
                            case 2 -> {
                                tempVolumeMusic = Math.max(0.0f, Math.min(1.0f, tempVolumeMusic + delta * 0.1f));
                                applyTempVolumesToAllClips();
                            }
                        }
                    }
                }
                if (key == KeyEvent.VK_ESCAPE) {
                    playSwooshing();
                    if (cameFromPauseToConfig) {
                        currentState = GameState.JOGANDO;
                        paused = true;
                        pauseMenuActive = true;
                        cameFromPauseToConfig = false;
                    } else {
                        currentState = GameState.MENU_PRINCIPAL;
                        selectedMenuItem = 0;
                        configScrollOffset = 0;
                        tempControlMode = controlMode;
                        tempVolumeEffects = volumeEffects;
                        tempVolumeMusic = volumeMusic;
                        tempJumpKey = jumpKey;
                        tempPauseKey = pauseKey;
                        configFocusOnSettings = true;
                    }
                    repaint();
                    return;
                }
                boolean isArrowKey = (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN ||
                                      key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT);
                if (selectedConfigOption == 3 && isValidBindableKey(key) && !isArrowKey) {
                    if (!isKeyAlreadyUsed(key, 3)) {
                        tempJumpKey = key;
                    }
                    repaint();
                    return;
                }
                if (selectedConfigOption == 4 && isValidBindableKey(key) && !isArrowKey) {
                    if (!isKeyAlreadyUsed(key, 4)) {
                        tempPauseKey = key;
                    }
                    repaint();
                    return;
                }
                if (selectionChanged) {
                    int[] sectionStartY = {
                        0, 220, 340, 520, 590
                    };
                    int contentTopPadding = 45;
                    int selectedItemTop = contentTopPadding + sectionStartY[selectedConfigOption];
                    int desiredVisiblePosition = 0;
                    int targetScroll = selectedItemTop - desiredVisiblePosition;
                    int maxScroll = 420;
                    targetScroll = Math.max(0, Math.min(maxScroll, targetScroll));
                    configScrollOffset = targetScroll;
                    repaint();
                }
                repaint();
            } else {
                switch (key) {
                    case KeyEvent.VK_LEFT -> {
                        int prev = selectedButtonIndex;
                        selectedButtonIndex = (selectedButtonIndex - 1 + 3) % 3;
                        if (selectedButtonIndex != prev) playButtonSelect();
                        repaint();
                    }
                    case KeyEvent.VK_RIGHT -> {
                        int prev = selectedButtonIndex;
                        selectedButtonIndex = (selectedButtonIndex + 1) % 3;
                        if (selectedButtonIndex != prev) playButtonSelect();
                        repaint();
                    }
                    case KeyEvent.VK_ENTER -> {
                        switch (selectedButtonIndex) {
                            case 0 -> {
                                controlMode = tempControlMode;
                                volumeEffects = tempVolumeEffects;
                                volumeMusic = tempVolumeMusic;
                                jumpKey = tempJumpKey;
                                pauseKey = tempPauseKey;
                                applyVolumeToAllClips();
                                saveConfigToFile();
                                showStatusMessage("Saved Settings!");
                                repaint();
                            }
                            case 1 -> {
                                resetToDefaultConfig();
                                tempControlMode = controlMode;
                                tempVolumeEffects = volumeEffects;
                                tempVolumeMusic = volumeMusic;
                                tempJumpKey = jumpKey;
                                tempPauseKey = pauseKey;
                                applyVolumeToAllClips();
                                showStatusMessage("Settings Reset!");
                            }
                            case 2 -> {
                                playSwooshing();
                                if (cameFromPauseToConfig) {
                                    currentState = GameState.JOGANDO;
                                    paused = true;
                                    pauseMenuActive = true;
                                    cameFromPauseToConfig = false;
                                } else {
                                    currentState = GameState.MENU_PRINCIPAL;
                                    selectedMenuItem = 0;
                                    configScrollOffset = 0;
                                    tempControlMode = controlMode;
                                    tempVolumeEffects = volumeEffects;
                                    tempVolumeMusic = volumeMusic;
                                    tempJumpKey = jumpKey;
                                    tempPauseKey = pauseKey;
                                    configFocusOnSettings = true;
                                }
                            }
                        }
                        repaint();
                    }
                    case KeyEvent.VK_ESCAPE -> {
                        playSwooshing();
                        if (cameFromPauseToConfig) {
                            currentState = GameState.JOGANDO;
                            paused = true;
                            pauseMenuActive = true;
                            cameFromPauseToConfig = false;
                        } else {
                            currentState = GameState.MENU_PRINCIPAL;
                            selectedMenuItem = 0;
                            configScrollOffset = 0;
                            tempControlMode = controlMode;
                            tempVolumeEffects = volumeEffects;
                            tempVolumeMusic = volumeMusic;
                            tempJumpKey = jumpKey;
                            tempPauseKey = pauseKey;
                            configFocusOnSettings = true;
                        }
                        repaint();
                    }
                }
            }
            return;
        }
        if (currentState == GameState.RECORDS) {
            ArrayList<GlobalHighScore> listaAtual = recordsAbaNormal ? topNormal : topHard;
            int totalContent = listaAtual.size() * 30;
            int boxHeight = 180;
            int maxScroll = Math.max(0, totalContent - boxHeight + 32);
            if (key == KeyEvent.VK_ESCAPE) {
                playSwooshing();
                currentState = previousStateBeforeRecords != null ? previousStateBeforeRecords : GameState.MENU_PRINCIPAL;
                selectedMenuItem = 0;
                recordsScrollOffset = 0;
                recordsFocusOnTabs = true;
                showOnlyCurrentProfileRecords = false;
                recordsVindoDoGameOver = false;
                previousStateBeforeRecords = null;
                repaint();
                return;
            }
            if (key == KeyEvent.VK_TAB) {
                recordsFocusOnTabs = !recordsFocusOnTabs;
                repaint();
                return;
            }
            if (recordsFocusOnTabs) {
                if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                    int prev = recordsAbaNormal ? 0 : 1;
                    recordsAbaNormal = !recordsAbaNormal;
                    if ((recordsAbaNormal ? 0 : 1) != prev) playButtonSelect();
                    recordsScrollOffset = 0;
                    repaint();
                    return;
                }
                if (key == KeyEvent.VK_UP) {
                    recordsScrollOffset -= 30;
                    recordsScrollOffset = Math.max(0, recordsScrollOffset);
                    repaint();
                    return;
                }
                if (key == KeyEvent.VK_DOWN) {
                    recordsScrollOffset += 30;
                    recordsScrollOffset = Math.min(maxScroll, recordsScrollOffset);
                    repaint();
                    return;
                }
            } else {
                if (key == KeyEvent.VK_ENTER) {
                    playSwooshing();
                    currentState = previousStateBeforeRecords != null ? previousStateBeforeRecords : GameState.MENU_PRINCIPAL;
                    selectedMenuItem = 0;
                    recordsScrollOffset = 0;
                    recordsFocusOnTabs = true;
                    showOnlyCurrentProfileRecords = false;
                    recordsVindoDoGameOver = false;
                    previousStateBeforeRecords = null;
                    repaint();
                    return;
                }
            }
            return;
        }
        if (currentState == GameState.DIGITAR_NOME_PERFIL) {
            if (key == KeyEvent.VK_ESCAPE) {
                if (isEditingName) {
                    playSwooshing();
                } else {
                    playSwooshing();
                    playDie();
                }
                typingName = "";
                currentState = isEditingName ? GameState.EDIT_PROFILE : GameState.NEW_GAME;
                isEditingName = false;
                repaint();
                return;
            }
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                int prev = selectedButtonIndex;
                selectedButtonIndex = (selectedButtonIndex + (key == KeyEvent.VK_RIGHT ? 1 : -1) + 2) % 2;
                if (selectedButtonIndex != prev) playButtonSelect();
                repaint();
                return;
            }
            if (key == KeyEvent.VK_ENTER) {
                if (selectedButtonIndex == 0) {
                    String novoNome = typingName.trim();
                    if (novoNome.isEmpty()) {
                        showStatusMessage("Enter a name!");
                        repaint();
                        return;
                    }
                    if (nomePerfilJaExiste(novoNome, null)) {
                        showStatusMessage("This name already exists!");
                        repaint();
                        return;
                    }
                    playPoint();
                    PlayerProfile p = currentProfileList.get(selectedSlot);
                    p.name = novoNome;
                    saveRecordsToFile();
                    currentState = GameState.CONFIRM_START_GAME;
                } else {
                    if (isEditingName) {
                        playSwooshing();
                    } else {
                        playSwooshing();
                        playDie();
                    }
                    typingName = "";
                    currentState = isEditingName ? GameState.EDIT_PROFILE : GameState.NEW_GAME;
                    isEditingName = false;
                }
                repaint();
                return;
            }
            if (key == KeyEvent.VK_BACK_SPACE) {
                if (typingName.length() > 0) {
                    typingName = typingName.substring(0, typingName.length() - 1);
                }
                keyPressTimestamps.put((int) '\b', System.currentTimeMillis());
                repaint();
                return;
            }
            if (key == KeyEvent.VK_SHIFT) {
                physicalShiftHeld = true;
                repaint();
            }
            if (key == KeyEvent.VK_CAPS_LOCK) {
                capsLockActive = !capsLockActive;
                repaint();
            }
            char c = e.getKeyChar();
            if (c != KeyEvent.CHAR_UNDEFINED && typingName.length() < 20) {
                if (shiftActive && Character.isLetter(c)) {
                    c = Character.toUpperCase(c);
                    shiftActive = false;
                }
                typingName += c;
                keyPressTimestamps.put((int) c, System.currentTimeMillis());
                repaint();
                return;
            }
        }
        if (currentState == GameState.EDIT_PROFILE) {
            if (key == KeyEvent.VK_DOWN) {
                int prev = selectedMenuItem;
                selectedMenuItem = (selectedMenuItem + 1) % 3;
                if (selectedMenuItem != prev) playButtonSelect();
                repaint();
                return;
            }
            if (key == KeyEvent.VK_UP) {
                int prev = selectedMenuItem;
                selectedMenuItem = (selectedMenuItem - 1 + 3) % 3;
                if (selectedMenuItem != prev) playButtonSelect();
                repaint();
                return;
            }
            if (key == KeyEvent.VK_ENTER) {
                handleEditProfileEnter();
                return;
            }
            if (key == KeyEvent.VK_ESCAPE) {
                playSwooshing();
                currentState = GameState.NEW_GAME;
                repaint();
                return;
            }
        }
        if (currentState == GameState.CONFIRM_DELETE_PROFILE ||
            currentState == GameState.CONFIRM_EDIT_PROFILE ||
            currentState == GameState.CONFIRM_START_GAME) {
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN ||
                key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                int prev = selectedMenuItem;
                selectedMenuItem = (selectedMenuItem + 1) % 2;
                if (selectedMenuItem != prev) playButtonSelect();
                repaint();
                return;
            }
            if (key == KeyEvent.VK_ENTER) {
                if (selectedMenuItem == 0) {
                    if (currentState == GameState.CONFIRM_DELETE_PROFILE) {
                        playDie();
                        playSwooshing();
                        PlayerProfile prof = currentProfileList.get(selectedSlot);
                        prof.name = "";
                        prof.scores.clear();
                        saveRecordsToFile();
                        currentState = GameState.NEW_GAME;
                        if (selectedSlot == lastPlayedSlot) {
                            lastPlayedSlot = -1;
                            lastPlayedWasHard = false;
                        }
                    } else if (currentState == GameState.CONFIRM_EDIT_PROFILE) {
                        String novoNome = typingName.trim();
                        if (novoNome.isEmpty()) {
                            showStatusMessage("Enter a name!");
                            repaint();
                            return;
                        }
                        PlayerProfile perfilAtual = currentProfileList.get(selectedSlot);
                        if (nomePerfilJaExiste(novoNome, perfilAtual)) {
                            showStatusMessage("This name already exists!");
                            repaint();
                            return;
                        }
                        perfilAtual.name = novoNome;
                        saveRecordsToFile();
                        currentState = GameState.NEW_GAME;
                    } else if (currentState == GameState.CONFIRM_START_GAME) {
                        modoDificil = !abaNormalAtiva;
                        lastPlayedSlot = selectedSlot;
                        lastPlayedWasHard = !abaNormalAtiva;
                        currentState = GameState.JOGANDO;
                        countdown = 3;
                        countdownTimer.start();
                        score = 0;
                        pipes.clear();
                        bullets.clear();
                        bird.x = birdx;
                        bird.y = birdy;
                        velocityY = 0;
                        birdRotation = 0;
                        statusMessage = "";
                        stopThemeMusic();
                    }
                } else {
                    playSwooshing();
                    if (currentState == GameState.CONFIRM_START_GAME) {
                        currentState = confirmStartFromContinue ? GameState.CONTINUE : GameState.NEW_GAME;
                        statusMessage = "";
                    } else if (currentState == GameState.CONFIRM_DELETE_PROFILE) {
                        currentState = GameState.EDIT_PROFILE;
                    } else if (currentState == GameState.CONFIRM_EDIT_PROFILE) {
                        currentState = GameState.EDIT_PROFILE;
                    }
                }
                repaint();
                return;
            }
            if (key == KeyEvent.VK_ESCAPE) {
                playSwooshing();
                if (currentState == GameState.CONFIRM_START_GAME) {
                    currentState = confirmStartFromContinue ? GameState.CONTINUE : GameState.NEW_GAME;
                    statusMessage = "";
                } else if (currentState == GameState.CONFIRM_DELETE_PROFILE) {
                    currentState = GameState.EDIT_PROFILE;
                } else if (currentState == GameState.CONFIRM_EDIT_PROFILE) {
                    currentState = GameState.EDIT_PROFILE;
                }
                repaint();
                return;
            }
        }
        if (controlMode == ControlMode.MOUSE_ONLY &&
            (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN ||
             key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT ||
             key == KeyEvent.VK_ENTER)) {
            return;
        }
        if (currentState == GameState.MENU_PRINCIPAL) {
            int max = hasAnySave() ? 3 : 2;
            if (key == KeyEvent.VK_DOWN) {
                int prev = selectedMenuItem;
                selectedMenuItem = (selectedMenuItem + 1) % (max + 1);
                if (selectedMenuItem != prev) playButtonSelect();
            } else if (key == KeyEvent.VK_UP) {
                int prev = selectedMenuItem;
                selectedMenuItem = (selectedMenuItem - 1 + max + 1) % (max + 1);
                if (selectedMenuItem != prev) playButtonSelect();
            } else if (key == KeyEvent.VK_ENTER) {
                handleMenuPrincipalEnter();
            }
            repaint();
            return;
        }
        if (currentState == GameState.NEW_GAME || currentState == GameState.CONTINUE) {
            boolean isContinue = (currentState == GameState.CONTINUE);
            ArrayList<PlayerProfile> lista = abaNormalAtiva ? profilesNormal : profilesHard;
            if (key == KeyEvent.VK_TAB) {
                slotsFocusOnTabs = !slotsFocusOnTabs;
                if (!slotsFocusOnTabs) {
                    slotsFocusOnBack = true;
                    selectedSlot = -1;
                } else {
                    slotsFocusOnBack = false;
                    if (abaNormalAtiva) {
                        selectedSlot = selectedSlotNormal;
                    } else {
                        selectedSlot = selectedSlotHard;
                    }
                    adjustScrollToSelected();
                }
                repaint();
                return;
            }
            if (slotsFocusOnTabs) {
                if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                    int prev = abaNormalAtiva ? 0 : 1;
                    abaNormalAtiva = !abaNormalAtiva;
                    if ((abaNormalAtiva ? 0 : 1) != prev) playButtonSelect();
                    if (abaNormalAtiva) {
                        selectedSlot = selectedSlotNormal;
                    } else {
                        selectedSlot = selectedSlotHard;
                    }
                    currentProfileList = abaNormalAtiva ? profilesNormal : profilesHard;
                    adjustScrollToSelected();
                    repaint();
                }
            }
            if (key == KeyEvent.VK_UP) {
                if (selectedSlot >= 0 && selectedSlot > 0) {
                    selectedSlot--;
                    int itemTop = selectedSlot * SLOT_HEIGHT;
                    int scrollOffset = abaNormalAtiva ? scrollOffsetNormal : scrollOffsetHard;
                    if (itemTop < scrollOffset) {
                        if (abaNormalAtiva) scrollOffsetNormal = itemTop;
                        else scrollOffsetHard = itemTop;
                    }
                }
                adjustScrollToSelected();
                repaint();
            } else if (key == KeyEvent.VK_DOWN) {
                if (selectedSlot >= 0 && selectedSlot < lista.size() - 1) {
                    selectedSlot++;
                    int itemBottom = (selectedSlot + 1) * SLOT_HEIGHT;
                    int boxHeight = VISIBLE_SLOTS * SLOT_HEIGHT;
                    int scrollOffset = abaNormalAtiva ? scrollOffsetNormal : scrollOffsetHard;
                    if (itemBottom > scrollOffset + boxHeight) {
                        if (abaNormalAtiva) scrollOffsetNormal = itemBottom - boxHeight;
                        else scrollOffsetHard = itemBottom - boxHeight;
                    }
                }
                adjustScrollToSelected();
                repaint();
            } else if (key == KeyEvent.VK_ENTER) {
                if (slotsFocusOnBack) {
                    playSwooshing();
                    saveRecordsToFile();
                    currentState = GameState.MENU_PRINCIPAL;
                    selectedMenuItem = 0;
                    slotsFocusOnBack = false;
                    slotsFocusOnTabs = true;
                    repaint();
                    return;
                }
                if (selectedSlot < 0) return;
                currentProfileList = abaNormalAtiva ? profilesNormal : profilesHard;
                PlayerProfile p = currentProfileList.get(selectedSlot);
                if (isContinue) {
                    if (p.isEmpty()) {
                        showStatusMessage("No game saved!");
                    } else {
                        playSwooshing();
                        confirmStartFromContinue = true;
                        currentState = GameState.CONFIRM_START_GAME;
                    }
                } else {
                    if (p.isEmpty()) {
                        playSwooshing();
                        isEditingName = false;
                        currentState = GameState.DIGITAR_NOME_PERFIL;
                        typingName = "";
                        selectedButtonIndex = 0;
                    } else {
                        playSwooshing();
                        currentState = GameState.EDIT_PROFILE;
                        selectedMenuItem = 0;
                    }
                }
                repaint();
                return;
            } else if (key == KeyEvent.VK_ESCAPE) {
                playSwooshing();
                currentState = GameState.MENU_PRINCIPAL;
                selectedMenuItem = 0;
                slotsFocusOnBack = false;
                slotsFocusOnTabs = true;
                repaint();
            }
            if (abaNormalAtiva) {
                selectedSlotNormal = selectedSlot;
            } else {
                selectedSlotHard = selectedSlot;
            }
            repaint();
            return;
        }
        if (currentState == GameState.GAME_OVER) {
            if (key == KeyEvent.VK_UP) {
                int prev = selectedGameOverOption;
                selectedGameOverOption = (selectedGameOverOption - 1 + 3) % 3;
                if (selectedGameOverOption != prev) playButtonSelect();
            } else if (key == KeyEvent.VK_DOWN) {
                int prev = selectedGameOverOption;
                selectedGameOverOption = (selectedGameOverOption + 1) % 3;
                if (selectedGameOverOption != prev) playButtonSelect();
            } else if (key == KeyEvent.VK_ENTER) {
                handleGameOverNewEnter();
            }
            repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (currentState == GameState.DIGITAR_NOME_PERFIL) {
            if (key == KeyEvent.VK_SHIFT) {
                physicalShiftHeld = false;
                repaint();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        int button = e.getButton();
        int mx = e.getX();
        int my = e.getY();
        if (button == MouseEvent.BUTTON3 && currentState == GameState.JOGANDO && countdown <= 0) {
            paused = !paused;
            if (paused) {
                pauseMenuActive = true;
                selectedPauseOption = 0;
                cameFromPauseToConfig = false;
            } else {
                if (isGameActive()) {
                    
                    
                }
                pauseMenuActive = false;
            }
            repaint();
            return;
        }
        if (button != MouseEvent.BUTTON1) return;
        boolean mouseAllowed = (controlMode == ControlMode.MOUSE_ONLY ||
                               controlMode == ControlMode.KEYBOARD_AND_MOUSE);
        if (!mouseAllowed) return;
        if (currentState == GameState.JOGANDO && !paused && countdown <= 0) {
            velocityY = jumpStrength;
            playWing();
            return;
        }
        for (MenuOption opt : clickableOptions) {
            if (opt.contains(mx, my)) {
                opt.action.run();
                return;
            }
        }
        if (currentState == GameState.CONFIGURACOES) {
            int scroll = configScrollOffset;
            int leftMargin = CONFIG_BOX_X + 20;
            int childMargin = leftMargin + 10;
            int visibleTop = CONFIG_BOX_Y + 5;
            int visibleBottom = CONFIG_BOX_Y + 5 + (CONFIG_BOX_HEIGHT - 10);
            if (my >= visibleTop && my <= visibleBottom) {
                int volumeTitleBaseY = (CONFIG_BOX_Y + 45 - scroll) + 40 + 200;
                int volumeStartY = volumeTitleBaseY + 50;
                int effectsY = volumeStartY;
                int lineYEffects = effectsY + 30;
                int iconXEffects = childMargin;
                int barXEffects = iconXEffects + 32 + 8;
                int barWidthEffects = CONFIG_BOX_WIDTH - 150 - (32 + 8);
                int barYEffects = lineYEffects + BAR_HEIGHT / 2 - BAR_HEIGHT / 2;
                int knobCenterYEffects = barYEffects + BAR_HEIGHT / 2;
                if (mx >= barXEffects - VOLUME_BAR_HIT_MARGIN_X &&
                    mx <= barXEffects + barWidthEffects + VOLUME_BAR_HIT_MARGIN_X &&
                    my >= knobCenterYEffects - VOLUME_BAR_HIT_MARGIN_Y &&
                    my <= knobCenterYEffects + VOLUME_BAR_HIT_MARGIN_Y) {
                    tempVolumeEffects = Math.max(0f, Math.min(1f, (mx - barXEffects) / (float) barWidthEffects));
                    if (tempVolumeEffects > 0) prevVolumeEffects = tempVolumeEffects;
                    applyTempVolumesToAllClips();
                    selectedConfigOption = 1;
                    configFocusOnSettings = true;
                    repaint();
                    return;
                }
                if (my >= lineYEffects - 20 && my <= lineYEffects + 15 &&
                    mx >= iconXEffects - 10 && mx <= iconXEffects + 35) {
                    if (tempVolumeEffects == 0) {
                        tempVolumeEffects = prevVolumeEffects > 0 ? prevVolumeEffects : 1.0f;
                    } else {
                        prevVolumeEffects = tempVolumeEffects;
                        tempVolumeEffects = 0f;
                    }
                    applyTempVolumesToAllClips();
                    selectedConfigOption = 1;
                    configFocusOnSettings = true;
                    repaint();
                    return;
                }
                int musicY = effectsY + 100;
                int lineYMusic = musicY + 30;
                int iconXMusic = childMargin;
                int barXMusic = iconXMusic + 32 + 8;
                int barWidthMusic = CONFIG_BOX_WIDTH - 150 - (32 + 8);
                int barYMusic = lineYMusic + BAR_HEIGHT / 2 - BAR_HEIGHT / 2;
                int knobCenterYMusic = barYMusic + BAR_HEIGHT / 2;
                if (mx >= barXMusic - VOLUME_BAR_HIT_MARGIN_X &&
                    mx <= barXMusic + barWidthMusic + VOLUME_BAR_HIT_MARGIN_X &&
                    my >= knobCenterYMusic - VOLUME_BAR_HIT_MARGIN_Y &&
                    my <= knobCenterYMusic + VOLUME_BAR_HIT_MARGIN_Y) {
                    tempVolumeMusic = Math.max(0f, Math.min(1f, (mx - barXMusic) / (float) barWidthMusic));
                    if (tempVolumeMusic > 0) prevVolumeMusic = tempVolumeMusic;
                    applyTempVolumesToAllClips();
                    selectedConfigOption = 2;
                    configFocusOnSettings = true;
                    repaint();
                    return;
                }
                if (my >= lineYMusic - 20 && my <= lineYMusic + 10 &&
                    mx >= iconXMusic - 10 && mx <= iconXMusic + 35) {
                    if (tempVolumeMusic == 0) {
                        tempVolumeMusic = prevVolumeMusic > 0 ? prevVolumeMusic : 1.0f;
                    } else {
                        prevVolumeMusic = tempVolumeMusic;
                        tempVolumeMusic = 0f;
                    }
                    applyTempVolumesToAllClips();
                    selectedConfigOption = 2;
                    configFocusOnSettings = true;
                    repaint();
                    return;
                }
                int radioYStartAdjusted = (CONFIG_BOX_Y + 45 - scroll) + 40;
                for (int i = 0; i < 3; i++) {
                    int radioY = radioYStartAdjusted + i * 55;
                    int radioHitX = childMargin - 10;
                    int radioHitWidth = 220;
                    int radioHitY = radioY - 25;
                    int radioHitHeight = 50;
                    if (mx >= radioHitX && mx <= radioHitX + radioHitWidth &&
                        my >= radioHitY && my <= radioHitY + radioHitHeight) {
                        tempControlMode = ControlMode.values()[i];
                        selectedConfigOption = 0;
                        configFocusOnSettings = true;
                        repaint();
                        return;
                    }
                }
            }
        }
        if (currentState == GameState.NEW_GAME || currentState == GameState.CONTINUE) {
            int boxX = (boardWidth - 300) / 2;
            int boxY = 300;
            int boxWidth = 300;
            int boxHeight = VISIBLE_SLOTS * SLOT_HEIGHT;
            if (mx >= boxX && mx <= boxX + boxWidth && my >= boxY && my <= boxY + boxHeight) {
                int scrollOffset = abaNormalAtiva ? scrollOffsetNormal : scrollOffsetHard;
                int relativeY = my - boxY + scrollOffset;
                int slotClicked = relativeY / SLOT_HEIGHT;
                ArrayList<PlayerProfile> lista = abaNormalAtiva ? profilesNormal : profilesHard;
                if (slotClicked >= 0 && slotClicked < lista.size()) {
                    selectedSlot = slotClicked;
                    if (abaNormalAtiva) selectedSlotNormal = selectedSlot;
                    else selectedSlotHard = selectedSlot;
                    currentProfileList = abaNormalAtiva ? profilesNormal : profilesHard;
                    slotsFocusOnBack = false;
                    slotsFocusOnTabs = false;
                    adjustScrollToSelected();
                    PlayerProfile p = currentProfileList.get(selectedSlot);
                    boolean isContinue = (currentState == GameState.CONTINUE);
                    if (isContinue) {
                        if (p.isEmpty()) {
                            showStatusMessage("No game saved!");
                        } else {
                            playSwooshing();
                            confirmStartFromContinue = true;
                            currentState = GameState.CONFIRM_START_GAME;
                        }
                    } else {
                        if (p.isEmpty()) {
                            playSwooshing();
                            isEditingName = false;
                            typingName = "";
                            selectedButtonIndex = 0;
                            currentState = GameState.DIGITAR_NOME_PERFIL;
                        } else {
                            playSwooshing();
                            currentState = GameState.EDIT_PROFILE;
                            selectedMenuItem = 0;
                        }
                    }
                    repaint();
                    return;
                }
            }
        }
        if (currentState == GameState.DIGITAR_NOME_PERFIL) {
            int boxHeight = 60;
            int boxY = 190;
            int buttonY = boxY + boxHeight + 40;
            int buttonWidth = 110;
            int spacing = 40;
            int totalButtonsWidth = buttonWidth * 2 + spacing;
            int startX = (boardWidth - totalButtonsWidth) / 2;
            if (mx >= startX && mx <= startX + buttonWidth &&
                my >= buttonY - 30 && my <= buttonY + 30) {
                String novoNome = typingName.trim();
                if (novoNome.isEmpty()) {
                    showStatusMessage("Enter a name!");
                    repaint();
                    return;
                }
                if (nomePerfilJaExiste(novoNome, null)) {
                    showStatusMessage("This name already exists!");
                    repaint();
                    return;
                }
                PlayerProfile p = currentProfileList.get(selectedSlot);
                p.name = novoNome;
                saveRecordsToFile();
                currentState = GameState.CONFIRM_START_GAME;
                repaint();
                return;
            }
            if (mx >= startX + buttonWidth + spacing &&
                mx <= startX + buttonWidth + spacing + buttonWidth &&
                my >= buttonY - 30 && my <= buttonY + 30) {
                if (isEditingName) {
                    playSwooshing();
                } else {
                    playSwooshing();
                    playDie();
                }
                typingName = "";
                currentState = isEditingName ? GameState.EDIT_PROFILE : GameState.NEW_GAME;
                isEditingName = false;
                repaint();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (currentState == GameState.CONFIGURACOES) {
            repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (currentState != GameState.CONFIGURACOES) return;
        int mx = e.getX();
        int my = e.getY();
        int scroll = configScrollOffset;
        int visibleTop = CONFIG_BOX_Y + 5;
        int visibleBottom = CONFIG_BOX_Y + 5 + (CONFIG_BOX_HEIGHT - 10);
        if (my < visibleTop || my > visibleBottom) return;
        int childMargin = CONFIG_BOX_X + 20 + 10;
        int iconSize = 32;
        int lineYEffects = (CONFIG_BOX_Y + 45 - scroll) + 40 + 200 + 50 + 30;
        int barXEffects = childMargin + iconSize + 8;
        int barWidthEffects = CONFIG_BOX_WIDTH - 150 - (iconSize + 8);
        int barYEffects = lineYEffects + BAR_HEIGHT / 2 - BAR_HEIGHT / 2;
        int knobCenterYEffects = barYEffects + BAR_HEIGHT / 2;
        if (mx >= barXEffects - VOLUME_BAR_HIT_MARGIN_X &&
            mx <= barXEffects + barWidthEffects + VOLUME_BAR_HIT_MARGIN_X &&
            my >= knobCenterYEffects - VOLUME_BAR_HIT_MARGIN_Y &&
            my <= knobCenterYEffects + VOLUME_BAR_HIT_MARGIN_Y) {
            float newVol = Math.max(0f, Math.min(1f, (mx - barXEffects) / (float) barWidthEffects));
            if (Math.abs(newVol - tempVolumeEffects) > 0.001f) {
                tempVolumeEffects = newVol;
                if (tempVolumeEffects > 0) prevVolumeEffects = tempVolumeEffects;
                applyTempVolumesToAllClips();
                repaintDebounced();
            }
            return;
        }
        int lineYMusic = lineYEffects + 100;
        int barXMusic = childMargin + iconSize + 8;
        int barWidthMusic = CONFIG_BOX_WIDTH - 150 - (iconSize + 8);
        int barYMusic = lineYMusic + BAR_HEIGHT / 2 - BAR_HEIGHT / 2;
        int knobCenterYMusic = barYMusic + BAR_HEIGHT / 2;
        if (mx >= barXMusic - VOLUME_BAR_HIT_MARGIN_X &&
            mx <= barXMusic + barWidthMusic + VOLUME_BAR_HIT_MARGIN_X &&
            my >= knobCenterYMusic - VOLUME_BAR_HIT_MARGIN_Y &&
            my <= knobCenterYMusic + VOLUME_BAR_HIT_MARGIN_Y) {
            float newVol = Math.max(0f, Math.min(1f, (mx - barXMusic) / (float) barWidthMusic));
            if (Math.abs(newVol - tempVolumeMusic) > 0.001f) {
                tempVolumeMusic = newVol;
                if (tempVolumeMusic > 0) prevVolumeMusic = tempVolumeMusic;
                applyTempVolumesToAllClips();
                repaintDebounced();
            }
        }
    }

    private void repaintDebounced() {
        long now = System.currentTimeMillis();
        if (now - lastVolumeRepaintTime >= VOLUME_REPAINT_DEBOUNCE_MS) {
            repaint();
            lastVolumeRepaintTime = now;
        }
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        updateHoverSelection(mouseX, mouseY);
    }

    private void updateHoverSelection(int mx, int my) {
        boolean found = false;
        for (MenuOption opt : clickableOptions) {
            if (opt.contains(mx, my)) {
                switch (currentState) {
                    case MENU_PRINCIPAL -> {
                        ArrayList<String> options = new ArrayList<>();
                        options.add("New Game");
                        if (hasAnySave()) options.add("Continue");
                        options.add("Configurations");
                        options.add("Records");
                        for (int i = 0; i < options.size(); i++) {
                            if (opt.text.equals(options.get(i))) {
                                int prev = selectedMenuItem;
                                selectedMenuItem = i;
                                if (selectedMenuItem != prev) playButtonSelect();
                                found = true;
                                break;
                            }
                        }
                    }
                    case JOGANDO -> {
                        if (pauseMenuActive) {
                            String[] pauseOptions = {"Continue", "Config.", "Exit"};
                            for (int i = 0; i < pauseOptions.length; i++) {
                                if (opt.text.equals(pauseOptions[i])) {
                                    int prev = selectedPauseOption;
                                    selectedPauseOption = i;
                                    if (selectedPauseOption != prev) playButtonSelect();
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    case GAME_OVER -> {
                        String[] gameOverOptions = {"Restart", "Records", "Exit"};
                        for (int i = 0; i < gameOverOptions.length; i++) {
                            if (opt.text.equals(gameOverOptions[i])) {
                                int prev = selectedGameOverOption;
                                selectedGameOverOption = i;
                                if (selectedGameOverOption != prev) playButtonSelect();
                                found = true;
                                break;
                            }
                        }
                    }
                    case CONFIGURACOES -> {
                        switch (opt.text) {
                            case "SAVE" -> {
                                int prev = selectedButtonIndex;
                                selectedButtonIndex = 0;
                                if (selectedButtonIndex != prev) playButtonSelect();
                                configFocusOnSettings = false;
                                found = true;
                            }
                            case "RESET" -> {
                                int prev = selectedButtonIndex;
                                selectedButtonIndex = 1;
                                if (selectedButtonIndex != prev) playButtonSelect();
                                configFocusOnSettings = false;
                                found = true;
                            }
                            case "BACK" -> {
                                int prev = selectedButtonIndex;
                                selectedButtonIndex = 2;
                                if (selectedButtonIndex != prev) playButtonSelect();
                                configFocusOnSettings = false;
                                found = true;
                            }
                        }
                    }
                    case NEW_GAME, CONTINUE -> {
                        if (opt.text.equals("Back")) {
                            slotsFocusOnBack = true;
                            slotsFocusOnTabs = false;
                            found = true;
                        }
                    }
                    case RECORDS -> {
                        if (opt.text.equals("Back")) {
                            recordsFocusOnTabs = false;
                            found = true;
                        }
                    }
                    case CONFIRM_DELETE_PROFILE, CONFIRM_EDIT_PROFILE, CONFIRM_START_GAME -> {
                        switch (opt.text) {
                            case "Yes" -> {
                                int prev = selectedMenuItem;
                                selectedMenuItem = 0;
                                if (selectedMenuItem != prev) playButtonSelect();
                                found = true;
                            }
                            case "No" -> {
                                int prev = selectedMenuItem;
                                selectedMenuItem = 1;
                                if (selectedMenuItem != prev) playButtonSelect();
                                found = true;
                            }
                        }
                    }
                    case EDIT_PROFILE -> {
                        switch (opt.text) {
                            case "Edit" -> {
                                int prev = selectedMenuItem;
                                selectedMenuItem = 0;
                                if (selectedMenuItem != prev) playButtonSelect();
                                found = true;
                            }
                            case "Delete" -> {
                                int prev = selectedMenuItem;
                                selectedMenuItem = 1;
                                if (selectedMenuItem != prev) playButtonSelect();
                                found = true;
                            }
                            case "Back" -> {
                                int prev = selectedMenuItem;
                                selectedMenuItem = 2;
                                if (selectedMenuItem != prev) playButtonSelect();
                                found = true;
                            }
                        }
                    }
                    case DIGITAR_NOME_PERFIL -> {
                        switch (opt.text) {
                            case "Confirm" -> {
                                int prev = selectedButtonIndex;
                                selectedButtonIndex = 0;
                                if (selectedButtonIndex != prev) playButtonSelect();
                                found = true;
                            }
                            case "Cancel" -> {
                                int prev = selectedButtonIndex;
                                selectedButtonIndex = 1;
                                if (selectedButtonIndex != prev) playButtonSelect();
                                found = true;
                            }
                        }
                    }
                }
                if (found) {
                    repaint();
                    return;
                }
            }
        }
        switch (currentState) {
            case CONFIGURACOES -> {
                int scroll = configScrollOffset;
                int leftMargin = CONFIG_BOX_X + 20;
                int childMargin = leftMargin + 10;
                int baseY = CONFIG_BOX_Y + 45 - scroll;
                int inputTitleY = baseY;
                if (my >= inputTitleY - 35 && my <= inputTitleY + 13 &&
                    mx >= leftMargin - 15 && mx <= leftMargin + HIGHLIGHT_WIDTH + 15) {
                    selectedConfigOption = 0;
                    configFocusOnSettings = true;
                    found = true;
                }
                int radioYStart = inputTitleY + 40;
                for (int i = 0; i < 3; i++) {
                    int radioY = radioYStart + i * 55;
                    if (my >= radioY - 25 && my <= radioY + 25 &&
                        mx >= childMargin - 10 && mx <= childMargin + 200) {
                        selectedConfigOption = 0;
                        configFocusOnSettings = true;
                        found = true;
                        break;
                    }
                }
                int volumeTitleY = radioYStart + 200;
                int volumeStartY = volumeTitleY + 50;
                int effectsY = volumeStartY;
                int lineYEffects = effectsY + 30;
                if (my >= effectsY - 30 && my <= lineYEffects + 30 &&
                    mx >= leftMargin - 5 && mx <= leftMargin + HIGHLIGHT_WIDTH + 25) {
                    selectedConfigOption = 1;
                    configFocusOnSettings = true;
                    found = true;
                }
                int musicY = effectsY + 100;
                int lineYMusic = musicY + 30;
                if (my >= musicY - 30 && my <= lineYMusic + 30 &&
                    mx >= leftMargin - 5 && mx <= leftMargin + HIGHLIGHT_WIDTH + 25) {
                    selectedConfigOption = 2;
                    configFocusOnSettings = true;
                    found = true;
                }
                int buttonsTitleY = lineYMusic + 80;
                int jumpY = buttonsTitleY + 50;
                if (my >= jumpY - 25 && my <= jumpY + 25 &&
                    mx >= leftMargin - 5 && mx <= leftMargin + HIGHLIGHT_WIDTH + 25) {
                    selectedConfigOption = 3;
                    configFocusOnSettings = true;
                    found = true;
                }
                int pauseY = jumpY + 50;
                if (my >= pauseY - 25 && my <= pauseY + 25 &&
                    mx >= leftMargin - 5 && mx <= leftMargin + HIGHLIGHT_WIDTH + 25) {
                    selectedConfigOption = 4;
                    configFocusOnSettings = true;
                    found = true;
                }
                int btnY = boardHeight - 70;
                int btnDetectTop = btnY - 35;
                int btnDetectBottom = btnY + 65;
                int saveWidth = 90;
                int resetWidth = 130;
                int backWidth = 90;
                int spacing = 20;
                int totalWidth = saveWidth + spacing + resetWidth + spacing + backWidth;
                int startX = (boardWidth - totalWidth) / 2;
                if (my >= btnDetectTop && my <= btnDetectBottom) {
                    if (mx >= startX - 10 && mx <= startX + saveWidth + 10) {
                        int prev = selectedButtonIndex;
                        selectedButtonIndex = 0;
                        if (selectedButtonIndex != prev) playButtonSelect();
                        configFocusOnSettings = false;
                        found = true;
                    } else if (mx >= startX + saveWidth + spacing - 10 &&
                               mx <= startX + saveWidth + spacing + resetWidth + 10) {
                        int prev = selectedButtonIndex;
                        selectedButtonIndex = 1;
                        if (selectedButtonIndex != prev) playButtonSelect();
                        configFocusOnSettings = false;
                        found = true;
                    } else if (mx >= startX + saveWidth + spacing + resetWidth + spacing - 10 &&
                               mx <= startX + saveWidth + spacing + resetWidth + spacing + backWidth + 10) {
                        int prev = selectedButtonIndex;
                        selectedButtonIndex = 2;
                        if (selectedButtonIndex != prev) playButtonSelect();
                        configFocusOnSettings = false;
                        found = true;
                    }
                }
            }
            case RECORDS -> {
                int abaY = 240;
                int btnWidth = BTN_MEDIUM;
                boolean normalHovered = mx >= 100 - btnWidth / 2 && mx <= 100 + btnWidth / 2 &&
                                        my >= abaY - 30 && my <= abaY + 30;
                boolean hardHovered = mx >= 260 - btnWidth / 2 && mx <= 260 + btnWidth / 2 &&
                                      my >= abaY - 30 && my <= abaY + 30;
                if (normalHovered || hardHovered) {
                    recordsFocusOnTabs = true;
                    found = true;
                }
                int centerX = boardWidth / 2;
                boolean abaHovered = mx >= centerX - btnWidth / 2 && mx <= centerX + btnWidth / 2 &&
                                     my >= abaY - 30 && my <= abaY + 30;
                if (abaHovered && recordsVindoDoGameOver) {
                    recordsFocusOnTabs = true;
                    found = true;
                }
                int backY = boardHeight - 100;
                int backDetectTop = backY - 35;
                int backDetectBottom = backY + 65;
                if (my >= backDetectTop && my <= backDetectBottom &&
                    mx >= boardWidth / 2 - BTN_SMALL / 2 - 15 &&
                    mx <= boardWidth / 2 + BTN_SMALL / 2 + 15) {
                    recordsFocusOnTabs = false;
                    found = true;
                }
            }
            case NEW_GAME, CONTINUE -> {
                int boxX = (boardWidth - 300) / 2;
                int boxY = 300;
                int boxWidth = 300;
                int boxHeight = VISIBLE_SLOTS * SLOT_HEIGHT;

                boolean hoveredOnSlots = mx >= boxX && mx <= boxX + boxWidth && my >= boxY && my <= boxY + boxHeight;

                if (hoveredOnSlots) {
                    int scrollOffset = abaNormalAtiva ? scrollOffsetNormal : scrollOffsetHard;
                    int relativeY = my - boxY + scrollOffset;
                    int slotHovered = relativeY / SLOT_HEIGHT;
                    ArrayList<PlayerProfile> lista = abaNormalAtiva ? profilesNormal : profilesHard;
                    if (slotHovered >= 0 && slotHovered < lista.size()) {
                        selectedSlot = slotHovered;
                        if (abaNormalAtiva) selectedSlotNormal = selectedSlot;
                        else selectedSlotHard = selectedSlot;
                        slotsFocusOnBack = false;
                        slotsFocusOnTabs = true;
                        found = true;
                    }
                }
            
                int abaY = 240;
                int btnWidth = BTN_MEDIUM;
                boolean normalHovered = mx >= 100 - btnWidth / 2 && mx <= 100 + btnWidth / 2 &&
                                        my >= abaY - 30 && my <= abaY + 30;
                boolean hardHovered = mx >= 260 - btnWidth / 2 && mx <= 260 + btnWidth / 2 &&
                                      my >= abaY - 30 && my <= abaY + 30;
            
                if (normalHovered || hardHovered) {
                    slotsFocusOnTabs = true;
                    slotsFocusOnBack = false;
                    found = true;
                }
            }
            case CONFIRM_DELETE_PROFILE, CONFIRM_EDIT_PROFILE, CONFIRM_START_GAME -> {
                int centerX = boardWidth / 2;
                int buttonY = 320;
                int spacing = 140;
                int buttonWidth = BTN_SMALL;
                int hitExtra = 10;
                boolean yesHovered = mx >= centerX - spacing/2 - buttonWidth/2 - hitExtra &&
                                     mx <= centerX - spacing/2 + buttonWidth/2 + hitExtra &&
                                     my >= buttonY - 35 && my <= buttonY + 65;
                boolean noHovered = mx >= centerX + spacing/2 - buttonWidth/2 - hitExtra &&
                                    mx <= centerX + spacing/2 + buttonWidth/2 + hitExtra &&
                                    my >= buttonY - 35 && my <= buttonY + 65;
                if (yesHovered) {
                    int prev = selectedMenuItem;
                    selectedMenuItem = 0;
                    if (selectedMenuItem != prev) playButtonSelect();
                    found = true;
                } else if (noHovered) {
                    int prev = selectedMenuItem;
                    selectedMenuItem = 1;
                    if (selectedMenuItem != prev) playButtonSelect();
                    found = true;
                }
            }
            case EDIT_PROFILE -> {
                int centerX = boardWidth / 2;
                int buttonY = 220;
                int spacing = 70;
                int buttonWidth = BTN_MEDIUM;
                int hitExtra = 10;
                boolean editHovered = mx >= centerX - buttonWidth/2 - hitExtra &&
                                      mx <= centerX + buttonWidth/2 + hitExtra &&
                                      my >= buttonY - 35 && my <= buttonY + 35;
                boolean deleteHovered = mx >= centerX - buttonWidth/2 - hitExtra &&
                                        mx <= centerX + buttonWidth/2 + hitExtra &&
                                        my >= buttonY + spacing - 35 && my <= buttonY + spacing + 35;
                boolean backHovered = mx >= centerX - buttonWidth/2 - hitExtra &&
                                      mx <= centerX + buttonWidth/2 + hitExtra &&
                                      my >= buttonY + 2 * spacing - 35 && my <= buttonY + 2 * spacing + 35;
                if (editHovered) {
                    int prev = selectedMenuItem;
                    selectedMenuItem = 0;
                    if (selectedMenuItem != prev) playButtonSelect();
                    found = true;
                } else if (deleteHovered) {
                    int prev = selectedMenuItem;
                    selectedMenuItem = 1;
                    if (selectedMenuItem != prev) playButtonSelect();
                    found = true;
                } else if (backHovered) {
                    int prev = selectedMenuItem;
                    selectedMenuItem = 2;
                    if (selectedMenuItem != prev) playButtonSelect();
                    found = true;
                }
            }
            case DIGITAR_NOME_PERFIL -> {
                int boxY = 190;
                int boxHeight = 60;
                int buttonY = boxY + boxHeight + 40;
                int buttonWidth = 110;
                int spacing = 40;
                int totalButtonsWidth = buttonWidth * 2 + spacing;
                int startX = (boardWidth - totalButtonsWidth) / 2;
                int hitExtra = 10;
                boolean confirmHovered = mx >= startX - hitExtra &&
                                         mx <= startX + buttonWidth + hitExtra &&
                                         my >= buttonY - 35 && my <= buttonY + 35;
                boolean cancelHovered = mx >= startX + buttonWidth + spacing - hitExtra &&
                                        mx <= startX + buttonWidth + spacing + buttonWidth + hitExtra &&
                                        my >= buttonY - 35 && my <= buttonY + 35;
                if (confirmHovered) {
                    int prev = selectedButtonIndex;
                    selectedButtonIndex = 0;
                    if (selectedButtonIndex != prev) playButtonSelect();
                    found = true;
                } else if (cancelHovered) {
                    int prev = selectedButtonIndex;
                    selectedButtonIndex = 1;
                    if (selectedButtonIndex != prev) playButtonSelect();
                    found = true;
                }
            }
        }
        if (found) repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int notches = e.getWheelRotation();
        if (currentState == GameState.NEW_GAME || currentState == GameState.CONTINUE) {
            ArrayList<PlayerProfile> lista = abaNormalAtiva ? profilesNormal : profilesHard;
            int scrollOffset = abaNormalAtiva ? scrollOffsetNormal : scrollOffsetHard;
            scrollOffset += notches * SLOT_HEIGHT * 2;
            int totalHeight = lista.size() * SLOT_HEIGHT;
            int boxHeight = VISIBLE_SLOTS * SLOT_HEIGHT;
            int maxScroll = Math.max(0, totalHeight - boxHeight);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
            if (abaNormalAtiva) {
                scrollOffsetNormal = scrollOffset;
            } else {
                scrollOffsetHard = scrollOffset;
            }
            repaint();
        } else if (currentState == GameState.CONFIGURACOES) {
            if (e.getX() >= CONFIG_BOX_X && e.getX() <= CONFIG_BOX_X + CONFIG_BOX_WIDTH &&
                e.getY() >= CONFIG_BOX_Y && e.getY() <= CONFIG_BOX_Y + CONFIG_BOX_HEIGHT) {
                configScrollOffset += notches * 35;
                int totalContentHeight = 720;
                int maxScroll = Math.max(0, totalContentHeight - CONFIG_BOX_HEIGHT + 40);
                configScrollOffset = Math.max(0, Math.min(maxScroll, configScrollOffset));
                repaint();
            }
        } else if (currentState == GameState.RECORDS) {
            recordsScrollOffset += notches * 30;
            ArrayList<GlobalHighScore> listaAtual = recordsAbaNormal ? topNormal : topHard;
            int totalHeight = listaAtual.size() * 30;
            int boxHeight = 180;
            int maxScroll = Math.max(0, totalHeight - boxHeight + 32);
            recordsScrollOffset = Math.max(0, Math.min(maxScroll, recordsScrollOffset));
            repaint();
        }
    }

    private void adjustScrollToSelected() {
        if (selectedSlot < 0) return;
        int boxHeight = VISIBLE_SLOTS * SLOT_HEIGHT;
        int itemTop = selectedSlot * SLOT_HEIGHT;
        int targetOffset = itemTop - (boxHeight - SLOT_HEIGHT) / 2;
        ArrayList<PlayerProfile> listaAtual = abaNormalAtiva ? profilesNormal : profilesHard;
        int totalHeight = listaAtual.size() * SLOT_HEIGHT;
        int maxScroll = Math.max(0, totalHeight - boxHeight);
        int novoOffset = Math.max(0, Math.min(maxScroll, targetOffset));
        if (abaNormalAtiva) {
            scrollOffsetNormal = novoOffset;
        } else {
            scrollOffsetHard = novoOffset;
        }
    }
}