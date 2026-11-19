package jp.kthrlab.pianorollsample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 視線データ (gaze_sender.py から UDP 5005 で送信される座標) の受信および
 * 演奏ログ・視線ログを CSV 形式で記録するクラス。
 */
public class GazeReceiver {

    public static final int DEFAULT_PORT = 5005;
    private static final String DEFAULT_PYTHON_SCRIPT = "gaze_sender.py";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 視線データのスナップショットを保持するイミュータブルクラス
     */
    public static class GazeData {
        public final double x;
        public final double y;
        public final boolean valid;
        public final long timestampMs;

        public GazeData(double x, double y, boolean valid, long timestampMs) {
            this.x = x;
            this.y = y;
            this.valid = valid;
            this.timestampMs = timestampMs;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "GazeData(x=%.4f, y=%.4f, valid=%s, time=%d)", x, y, valid, timestampMs);
        }
    }

    // UDP 受信設定
    private final int port;
    private DatagramSocket socket;
    private Thread udpThread;
    private volatile boolean running = false;

    // 視線データ (スレッドセーフ保持)
    private volatile double gazeX = 0.5;
    private volatile double gazeY = 0.5;
    private volatile boolean gazeValid = false;
    private final AtomicReference<GazeData> currentGaze =
            new AtomicReference<>(new GazeData(0.5, 0.5, false, 0));

    // Python プロセス管理
    private Process pythonProcess;
    private Thread pythonOutputThread;

    // 実験ログ用属性
    private volatile String subjectId = "default";
    private volatile String condition = "default";
    private volatile String phase = "default";
    private File logDirectory = new File("logs");

    // FRAME ログ記録時の MIDI 列プレースホルダー (デフォルトは空文字)
    private String frameMidiNote = "";
    private String frameMidiVelocity = "";

    // CSV 書き込み用
    private File currentCsvFile;
    private BufferedWriter csvWriter;
    private final Object writeLock = new Object();

    // JVM 終了時の安全終了フック
    private Thread shutdownHook;

    /**
     * デフォルト設定 (ポート 5005, subjectId="default", condition="default") で生成
     */
    public GazeReceiver() {
        this(DEFAULT_PORT, "default", "default");
    }

    /**
     * 被験者IDと条件を指定して生成
     */
    public GazeReceiver(String subjectId, String condition) {
        this(DEFAULT_PORT, subjectId, condition);
    }

    /**
     * 被験者ID (int) と条件を指定して生成
     */
    public GazeReceiver(int subjectId, String condition) {
        this(DEFAULT_PORT, String.valueOf(subjectId), condition);
    }

    /**
     * ポート番号、被験者ID、条件を指定して生成
     */
    public GazeReceiver(int port, String subjectId, String condition) {
        this.port = port;
        this.subjectId = (subjectId != null) ? subjectId : "default";
        this.condition = (condition != null) ? condition : "default";
    }

    // ==========================================
    // ライフサイクル管理 (start / stop)
    // ==========================================

    /**
     * 視線受信およびログ記録を開始する。
     *
     * @param autoStartPython true の場合、gaze_sender.py を子プロセスとして自動起動する
     * @param useMock         true の場合 --mock、false の場合 --tobii 引数を渡す
     */
    public synchronized void start(boolean autoStartPython, boolean useMock) {
        if (this.running) {
            System.out.println("[GazeReceiver] 既に起動しています。");
            return;
        }

        this.running = true;

        // 1. CSV ファイルの初期化
        initCsvLog();

        // 2. UDP 受信スレッドの開始
        startUdpReceiver();

        // 3. Python スクリプトの自動起動 (要求時)
        if (autoStartPython) {
            startPythonProcess(useMock);
        }

        // 4. シャットダウンフックの登録
        registerShutdownHook();

        System.out.println("[GazeReceiver] 開始完了 (Port: " + port +
                ", Subject: " + subjectId + ", Condition: " + condition + ")");
    }

    /**
     * subjectId と condition を更新して開始するオーバーロード
     */
    public synchronized void start(String subjectId, String condition, boolean autoStartPython, boolean useMock) {
        setSubjectId(subjectId);
        setCondition(condition);
        start(autoStartPython, useMock);
    }

    /**
     * subjectId (int) と condition を更新して開始するオーバーロード
     */
    public synchronized void start(int subjectId, String condition, boolean autoStartPython, boolean useMock) {
        setSubjectId(subjectId);
        setCondition(condition);
        start(autoStartPython, useMock);
    }

    /**
     * 受信とロギングを停止し、CSV ファイルを保存完了し、起動した Python プロセスを終了する。
     */
    public synchronized void stop() {
        if (!this.running) {
            return;
        }
        this.running = false;

        // 1. UDP ソケットを閉じる
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (udpThread != null) {
            try {
                udpThread.join(1000);
            } catch (InterruptedException ignored) {
            }
            udpThread = null;
        }

        // 2. Python プロセスを終了
        stopPythonProcess();

        // 3. CSV ライターのフラッシュ & クローズ
        synchronized (writeLock) {
            if (csvWriter != null) {
                try {
                    csvWriter.flush();
                    csvWriter.close();
                    if (currentCsvFile != null) {
                        System.out.println("[GazeReceiver] CSVログ保存完了: " + currentCsvFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    System.err.println("[GazeReceiver] CSVクローズ中にエラー: " + e.getMessage());
                } finally {
                    csvWriter = null;
                }
            }
        }

        // シャットダウンフック解除
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // JVM 終了中は解除できないので無視
            }
            shutdownHook = null;
        }

        System.out.println("[GazeReceiver] 停止完了");
    }

    // ==========================================
    // ログ記録メソッド
    // ==========================================

    /**
     * 毎フレーム呼び出すログ記録メソッド。
     * event_type="FRAME" として最新の視線座標を記録する。
     */
    public void logFrame() {
        GazeData gaze = currentGaze.get();
        writeRow("FRAME", gaze.x, gaze.y, gaze.valid ? 1 : 0, frameMidiNote, frameMidiVelocity);
    }

    /**
     * MIDI 打鍵/離鍵時に呼び出すログ記録メソッド。
     *
     * @param eventType イベント種別 (例: "NOTE_ON", "NOTE_OFF")
     * @param note      MIDI ノート番号 (0〜127)
     * @param velocity  ベロシティ (0〜127)
     */
    public void logMidiEvent(String eventType, int note, int velocity) {
        GazeData gaze = currentGaze.get();
        writeRow(eventType, gaze.x, gaze.y, gaze.valid ? 1 : 0, String.valueOf(note), String.valueOf(velocity));
    }

    /**
     * フェーズを変更する (例: "practice", "test", "demo")。
     *
     * @param phase フェーズ名
     */
    public void setPhase(String phase) {
        this.phase = (phase != null) ? phase : "";
    }

    public String getPhase() {
        return this.phase;
    }

    // ==========================================
    // ゲッター / セッター
    // ==========================================

    /**
     * 最新の視線 X 座標 (0.0〜1.0) を取得
     */
    public double getGazeX() {
        return currentGaze.get().x;
    }

    /**
     * 最新の視線 Y 座標 (0.0〜1.0) を取得
     */
    public double getGazeY() {
        return currentGaze.get().y;
    }

    /**
     * 視線が有効トラッキング中か取得
     */
    public boolean isGazeValid() {
        return currentGaze.get().valid;
    }

    /**
     * 視線有効フラグを整数 (1 or 0) で取得
     */
    public int getGazeValid() {
        return currentGaze.get().valid ? 1 : 0;
    }

    /**
     * 最新の視線データスナップショットを取得
     */
    public GazeData getGazeData() {
        return currentGaze.get();
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = (subjectId != null) ? subjectId : "default";
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = String.valueOf(subjectId);
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = (condition != null) ? condition : "default";
    }

    public File getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(File logDirectory) {
        if (logDirectory != null) {
            this.logDirectory = logDirectory;
        }
    }

    public void setLogDirectory(String logDirectoryPath) {
        if (logDirectoryPath != null && !logDirectoryPath.trim().isEmpty()) {
            this.logDirectory = new File(logDirectoryPath);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public File getCurrentCsvFile() {
        return currentCsvFile;
    }

    /**
     * FRAME ログ記録時の MIDI 列プレースホルダーを設定する (空文字 "" や "-1" など)。
     */
    public void setFrameMidiPlaceholders(String notePlaceholder, String velocityPlaceholder) {
        this.frameMidiNote = (notePlaceholder != null) ? notePlaceholder : "";
        this.frameMidiVelocity = (velocityPlaceholder != null) ? velocityPlaceholder : "";
    }

    // ==========================================
    // 内部実装: CSV 初期化 & 書き込み
    // ==========================================

    private void initCsvLog() {
        File logDir = (logDirectory != null) ? logDirectory : new File("logs");
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (!created && !logDir.exists()) {
                System.err.println("[GazeReceiver] ログディレクトリの作成に失敗しました: " + logDir.getAbsolutePath());
            }
        }

        String nowStr = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String filename = String.format("%s_%s_%s.csv", subjectId, condition, nowStr);
        currentCsvFile = new File(logDir, filename);

        synchronized (writeLock) {
            try {
                csvWriter = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(currentCsvFile, false), StandardCharsets.UTF_8));
                // ヘッダー行の書き込み
                csvWriter.write("timestamp_ms,subject_id,condition,phase,event_type,gaze_norm_x,gaze_norm_y,gaze_valid,midi_note,midi_velocity");
                csvWriter.newLine();
                csvWriter.flush();
                System.out.println("[GazeReceiver] ログファイルを作成しました: " + currentCsvFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("[GazeReceiver] CSVファイル初期化エラー: " + e.getMessage());
            }
        }
    }

    private void writeRow(String eventType, double x, double y, int valid, String noteStr, String velStr) {
        if (csvWriter == null) {
            return;
        }

        long now = System.currentTimeMillis();
        String safeEventType = (eventType != null) ? eventType : "";

        String line = String.format(Locale.US, "%d,%s,%s,%s,%s,%.4f,%.4f,%d,%s,%s",
                now,
                subjectId,
                condition,
                phase,
                safeEventType,
                x,
                y,
                valid,
                (noteStr != null ? noteStr : ""),
                (velStr != null ? velStr : "")
        );

        synchronized (writeLock) {
            if (csvWriter != null) {
                try {
                    csvWriter.write(line);
                    csvWriter.newLine();
                } catch (IOException e) {
                    System.err.println("[GazeReceiver] CSV行書き込みエラー: " + e.getMessage());
                }
            }
        }
    }

    // ==========================================
    // 内部実装: UDP 受信
    // ==========================================

    private void startUdpReceiver() {
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new java.net.InetSocketAddress(port));
            udpThread = new Thread(this::receiveLoop, "GazeReceiver-UDP");
            udpThread.setDaemon(true);
            udpThread.start();
            System.out.println("[GazeReceiver] UDP受信スレッドを開始しました (Port: " + port + ")");
        } catch (SocketException e) {
            System.err.println("[GazeReceiver] UDPソケットのオープンに失敗しました (Port: " + port + "): " + e.getMessage());
        }
    }

    private void receiveLoop() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (running && socket != null && !socket.isClosed()) {
            try {
                socket.receive(packet);
                String text = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.US_ASCII).trim();
                if (!text.isEmpty()) {
                    parseGazePacket(text);
                }
            } catch (SocketException e) {
                // socket.close() 呼び出しによる停止
                break;
            } catch (Exception e) {
                if (running) {
                    System.err.println("[GazeReceiver] UDP受信処理エラー: " + e.getMessage());
                }
            }
        }
    }

    private void parseGazePacket(String text) {
        try {
            String[] parts = text.split(",");
            if (parts.length >= 3) {
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                int validInt = Integer.parseInt(parts[2].trim());
                boolean valid = (validInt == 1);

                this.gazeX = x;
                this.gazeY = y;
                this.gazeValid = valid;
                this.currentGaze.set(new GazeData(x, y, valid, System.currentTimeMillis()));
            }
        } catch (Exception ignored) {
            // 受信パケットの形式不正は読み飛ばす
        }
    }

    // ==========================================
    // 内部実装: Python プロセス管理
    // ==========================================

    private void startPythonProcess(boolean useMock) {
        File scriptFile = findPythonScript();
        if (scriptFile == null || !scriptFile.exists()) {
            System.err.println("[GazeReceiver] 警告: " + DEFAULT_PYTHON_SCRIPT + " が見つかりませんでした。Pythonプロセスの自動起動をスキップします。");
            return;
        }

        String modeArg = useMock ? "--mock" : "--tobii";
        ProcessBuilder pb = new ProcessBuilder("python", scriptFile.getAbsolutePath(), modeArg);
        pb.directory(scriptFile.getParentFile());
        pb.redirectErrorStream(true);

        try {
            pythonProcess = pb.start();
        } catch (IOException e) {
            // Windows 環境で 'python' ではなく 'py' が必要な場合のフォールバック
            try {
                ProcessBuilder fallbackPb = new ProcessBuilder("py", scriptFile.getAbsolutePath(), modeArg);
                fallbackPb.directory(scriptFile.getParentFile());
                fallbackPb.redirectErrorStream(true);
                pythonProcess = fallbackPb.start();
            } catch (IOException ex) {
                System.err.println("[GazeReceiver] Python プロセスの起動に失敗しました: " + ex.getMessage());
                return;
            }
        }

        System.out.println("[GazeReceiver] Python プロセスを起動しました: " + scriptFile.getName() + " " + modeArg);

        // 標準出力を非同期で読み捨ててバッファ詰まりを防止
        pythonOutputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(pythonProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // デバッグ用: 必要に応じて出力を確認
                    // System.out.println("[gaze_sender] " + line);
                }
            } catch (IOException ignored) {
            }
        }, "GazeReceiver-PythonOutput");
        pythonOutputThread.setDaemon(true);
        pythonOutputThread.start();
    }

    private void stopPythonProcess() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            System.out.println("[GazeReceiver] Python プロセスを終了中...");
            pythonProcess.destroy();
            try {
                if (!pythonProcess.waitFor(2, TimeUnit.SECONDS)) {
                    pythonProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                pythonProcess.destroyForcibly();
            }
            pythonProcess = null;
        }
    }

    private File findPythonScript() {
        // 1. カレントディレクトリ
        File f1 = new File(DEFAULT_PYTHON_SCRIPT);
        if (f1.exists()) {
            return f1.getAbsoluteFile();
        }

        // 2. user.dir
        File f2 = new File(System.getProperty("user.dir"), DEFAULT_PYTHON_SCRIPT);
        if (f2.exists()) {
            return f2.getAbsoluteFile();
        }

        // 3. 親ディレクトリ方向の探索
        File current = new File(".").getAbsoluteFile();
        while (current != null) {
            File check = new File(current, DEFAULT_PYTHON_SCRIPT);
            if (check.exists()) {
                return check.getAbsoluteFile();
            }
            current = current.getParentFile();
        }

        return f1.getAbsoluteFile();
    }

    private void registerShutdownHook() {
        shutdownHook = new Thread(() -> {
            try {
                stop();
            } catch (Exception ignored) {
            }
        }, "GazeReceiver-ShutdownHook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
}
