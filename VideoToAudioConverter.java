import com.formdev.flatlaf.FlatLightLaf; // 导入现代化皮肤
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class VideoToAudioConverter extends JFrame {

    private JTextField inputField;
    private JTextField startTimeField;
    private JTextField endTimeField;
    private JTextArea logArea;
    private JButton convertButton;
    private JProgressBar progressBar; // 增加进度条，看起来更高级

    private static final String FFMPEG_PATH = "ffmpeg";

    public VideoToAudioConverter() {
        // 设置窗口标题和图标
        setTitle("✨ 视频提取音频工具 Pro");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // 初始化UI
        initUI();
    }

    private void initUI() {
        // 主面板，使用 BorderLayout，边距设大一点，显得大气
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20)); // 四周留白 20像素

        // --- 顶部区域：标题 ---
        JLabel titleLabel = new JLabel("视频转音频转换器", JLabel.CENTER);
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 24));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // --- 中间区域：表单 ---
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

        // 1. 文件选择区域
        JPanel filePanel = createRowPanel();
        JLabel fileLabel = new JLabel("视频源文件:");
        styleLabel(fileLabel);
        inputField = new JTextField();
        inputField.putClientProperty("JTextField.placeholderText", "请选择要转换的视频..."); // 占位符
        JButton selectBtn = new JButton("浏览...");
        selectBtn.addActionListener(e -> chooseFile());

        filePanel.add(fileLabel);
        filePanel.add(Box.createHorizontalStrut(10));
        filePanel.add(inputField);
        filePanel.add(Box.createHorizontalStrut(10));
        filePanel.add(selectBtn);

        // 2. 时间裁剪区域
        JPanel timePanel = createRowPanel();
        JLabel startLabel = new JLabel("开始时间:");
        styleLabel(startLabel);
        startTimeField = new JTextField();
        startTimeField.putClientProperty("JTextField.placeholderText", "00:00:00");
        
        JLabel endLabel = new JLabel("结束时间:");
        styleLabel(endLabel);
        endTimeField = new JTextField();
        endTimeField.putClientProperty("JTextField.placeholderText", "留空则到结尾");

        timePanel.add(startLabel);
        timePanel.add(Box.createHorizontalStrut(10));
        timePanel.add(startTimeField);
        timePanel.add(Box.createHorizontalStrut(20));
        timePanel.add(endLabel);
        timePanel.add(Box.createHorizontalStrut(10));
        timePanel.add(endTimeField);

        // 3. 进度条
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("准备就绪");
        
        // 添加到表单面板，增加垂直间距
        formPanel.add(filePanel);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(timePanel);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(progressBar);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // --- 底部区域：按钮和日志 ---
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        
        convertButton = new JButton("开始转换");
        convertButton.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 16));
        convertButton.setBackground(new Color(60, 140, 240)); // 类似 iOS 的蓝色按钮
        convertButton.setForeground(Color.WHITE);
        convertButton.setFocusPainted(false);
        convertButton.setPreferredSize(new Dimension(100, 45));
        convertButton.addActionListener(e -> startConversion());

        logArea = new JTextArea(6, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBorder(BorderFactory.createTitledBorder("运行日志"));
        JScrollPane scrollPane = new JScrollPane(logArea);

        bottomPanel.add(convertButton, BorderLayout.NORTH);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    // 辅助方法：创建行容器
    private JPanel createRowPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    // 辅助方法：统一标签样式
    private void styleLabel(JLabel label) {
        label.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
    }

    private void chooseFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            inputField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void startConversion() {
        String inputFile = inputField.getText();
        if (inputFile.isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠️ 请先选择视频文件！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String start = startTimeField.getText().trim();
        String end = endTimeField.getText().trim();
        String outputFile = inputFile.substring(0, inputFile.lastIndexOf(".")) + "_output.mp3";

        new Thread(() -> {
            SwingUtilities.invokeLater(() -> {
                convertButton.setEnabled(false);
                progressBar.setIndeterminate(true); // 开启滚动动画
                progressBar.setString("转换中，请稍候...");
                logArea.setText("");
            });

            try {
                executeFFmpeg(inputFile, outputFile, start, end);
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    progressBar.setString("转换完成！");
                    log("✅ 成功：文件已保存至 " + outputFile);
                    JOptionPane.showMessageDialog(this, "转换成功！\n" + outputFile);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setString("发生错误");
                    log("❌ 错误: " + e.getMessage());
                });
                e.printStackTrace();
            } finally {
                SwingUtilities.invokeLater(() -> convertButton.setEnabled(true));
            }
        }).start();
    }

    private void executeFFmpeg(String input, String output, String start, String end) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(FFMPEG_PATH);
        
        // 1. 设置输入
        command.add("-i");
        command.add(input);

        // 2. 设置裁剪 (如果需要)
        if (!start.isEmpty()) { command.add("-ss"); command.add(start); }
        if (!end.isEmpty()) { command.add("-to"); command.add(end); }

        // 3. 关键修改：不再强制 -vn，而是自动复制音频
        // 如果源文件没声音，mp3 编码器会自动报错，我们需要捕获这个错误
        command.add("-map");
        command.add("0:a"); // 显式告诉 FFmpeg：我要音频流！
        
        command.add("-acodec");
        command.add("libmp3lame");
        command.add("-q:a");
        command.add("2");
        command.add("-y");
        command.add(output);

        String cmdStr = String.join(" ", command);
        System.out.println("DEBUG: " + cmdStr); 

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // --- 修复乱码的关键点 ---
        // Windows 中文系统的 CMD 输出通常是 GBK，而不是 UTF-8
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 过滤掉无关紧要的进度条信息，只打关键日志
                if(!line.startsWith("size=") && !line.startsWith("frame=")) {
                    log(line);
                    System.out.println(line);
                }
                
                // 实时检测有没有报错说“找不到音频”
                if (line.contains("Stream map '0:a' matches no streams")) {
                    throw new Exception("转换失败：源视频文件里【没有音频轨道】！\n请先播放视频确认是否有声音。");
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("FFmpeg 异常退出 (代码 " + exitCode + ")。");
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        // --- 核心改动：启用 FlatLaf 皮肤 ---
        try {
            // 这里选择了 Light 主题，如果你想要深色模式，换成 FlatDarkLaf
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 10); // 设置按钮圆角
            UIManager.put("Component.arc", 10); // 设置组件圆角
            UIManager.put("TextComponent.arc", 10); // 输入框圆角
        } catch (Exception ex) {
            System.err.println("皮肤加载失败");
        }
        
        SwingUtilities.invokeLater(() -> new VideoToAudioConverter().setVisible(true));
    }
}