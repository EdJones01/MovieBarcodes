import libraries.MMCQ;
import libraries.ColorThief;
import org.apache.commons.lang3.time.StopWatch;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MainPanel extends JPanel {
    private enum ColorMode {
        MOST_PROMINENT, AVERAGE_RGB
    }

    private ColorMode colorMode = ColorMode.AVERAGE_RGB;

    private String name = "";
    private String saveFileDirectory = null;
    private String moviePath = null;

    private int numOfFrames = 400;
    private int frameSize = 1;
    private double widthToHeightRatio = 3;

    private JProgressBar progressBar;
    private JLabel taskLabel;
    private JTextArea detailsArea;
    private final ButtonGroup buttonGroup = new ButtonGroup();

    public MainPanel() {
        setupUI();
        updateDetails();
    }

    private void setupUI() {
        setLayout(null);

        JLabel lblFrameSize = new JLabel("Frame size:");
        lblFrameSize.setBounds(342, 64, 67, 14);
        add(lblFrameSize);

        JLabel lblNewLabel = new JLabel("Number of fames:");
        lblNewLabel.setBounds(10, 64, 211, 14);
        add(lblNewLabel);

        JLabel widthRatioLabel = new JLabel("Width to height ratio:");
        widthRatioLabel.addKeyListener(new MyKeyAdapter());
        widthRatioLabel.setBounds(162, 64, 131, 14);
        add(widthRatioLabel);

        taskLabel = new JLabel();
        taskLabel.setBounds(10, 268, 430, 14);
        add(taskLabel);

        JLabel lblColorMode = new JLabel("Color mode:");
        lblColorMode.setBounds(10, 95, 100, 14);
        add(lblColorMode);

        JRadioButton averageRadioButton = new JRadioButton("Average RGB");
        buttonGroup.add(averageRadioButton);
        averageRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                colorMode = ColorMode.AVERAGE_RGB;
            }
        });
        averageRadioButton.setSelected(true);
        averageRadioButton.setBounds(121, 91, 109, 23);
        add(averageRadioButton);

        JRadioButton prominentRadioButton = new JRadioButton("Most prominent color");
        buttonGroup.add(prominentRadioButton);
        prominentRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                colorMode = ColorMode.MOST_PROMINENT;
            }
        });
        prominentRadioButton.setBounds(239, 91, 170, 23);
        add(prominentRadioButton);

        JButton generateButton = new JButton("Generate poster");
        generateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                generatePoster();
            }
        });
        generateButton.setBounds(10, 202, 460, 23);
        generateButton.setEnabled(false);
        add(generateButton);

        JButton selectMovieButton = new JButton("Select movie file");
        selectMovieButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                String oldMoviePath = moviePath;
                moviePath = getMoviePath();
                if (moviePath == null)
                    moviePath = oldMoviePath;
                updateDetails();
                generateButton.setEnabled(moviePath != null && saveFileDirectory != null);
            }
        });
        selectMovieButton.setBounds(10, 11, 211, 42);
        add(selectMovieButton);

        JButton openSaveDirectoryButton = new JButton("Open Save Directory");
        openSaveDirectoryButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File(saveFileDirectory));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Unable to open given directory.");
            }
        });
        openSaveDirectoryButton.setBounds(310, 268, 160, 23);
        openSaveDirectoryButton.setEnabled(false);
        add(openSaveDirectoryButton);

        JButton selectDirectoryButton = new JButton("Select save directory");
        selectDirectoryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String oldSaveFileDirectory = saveFileDirectory;
                saveFileDirectory = getSaveDirectory();
                if (saveFileDirectory.equals(null + "\\"))
                    saveFileDirectory = oldSaveFileDirectory;
                updateDetails();
                generateButton.setEnabled(moviePath != null && saveFileDirectory != null);
                openSaveDirectoryButton.setEnabled(saveFileDirectory != null);
            }
        });
        selectDirectoryButton.setBounds(259, 11, 211, 42);
        add(selectDirectoryButton);

        JTextField frameNumBox = new JTextField();
        frameNumBox.addKeyListener(new MyKeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (((c < '0') || (c > '9')) && (c != KeyEvent.VK_BACK_SPACE)) {
                    e.consume();
                } else {
                    try {
                        numOfFrames = Integer.parseInt(frameNumBox.getText() + c);
                    } catch (Exception ex) {
                        numOfFrames = 1;
                    }
                    updateDetails();
                }
            }
        });
        frameNumBox.setText("" + numOfFrames);
        frameNumBox.addKeyListener(new MyKeyAdapter());
        frameNumBox.setBounds(113, 62, 44, 20);
        add(frameNumBox);
        frameNumBox.setColumns(10);

        detailsArea = new JTextArea();
        detailsArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(detailsArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setBounds(10, 120, 460, 80);
        add(scrollPane);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setBounds(10, 232, 460, 31);
        add(progressBar);

        JSpinner ratioSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        ratioSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent arg0) {
                widthToHeightRatio = Double.parseDouble("" + (int) ratioSpinner.getValue());
                updateDetails();
            }
        });
        ratioSpinner.setBounds(288, 61, 44, 20);
        add(ratioSpinner);

        JSpinner frameSizeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        frameSizeSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent arg0) {
                frameSize = (int) frameSizeSpinner.getValue();
                updateDetails();
            }
        });
        frameSizeSpinner.setBounds(426, 61, 44, 20);
        add(frameSizeSpinner);
    }

    private void generatePoster() {
        String[] pathArray = moviePath.split("\\\\");
        name = pathArray[pathArray.length - 1].substring(0, pathArray[pathArray.length - 1].length() - 4);
        progressBar.setMinimum(0);
        progressBar.setMaximum(numOfFrames);
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    taskLabel.setText("Tasks Completed 0/2");
                    createFrameFiles();
                    taskLabel.setText("Tasks Completed 1/2");
                    progressBar.setValue(0);
                    createPoster();
                    taskLabel.setText("Tasks Completed 2/2 - Poster saved");
                    clearWorkingDirs();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        t1.start();
    }

    private void createFrameFiles() {
        try {
            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(moviePath);

            frameGrabber.start();

            int length = frameGrabber.getLengthInVideoFrames();

            File framePath = new File("./src/frames");
            if (!framePath.exists())
                framePath.mkdirs();

            int numOfCPUs = Runtime.getRuntime().availableProcessors();
            int framesPerThread = numOfFrames / numOfCPUs;
            Thread[] threads = new Thread[numOfCPUs];

            progressBar.setString("Generating frames across " + numOfCPUs + " threads.");
            progressBar.setValue(0);

            for (int i = 0; i < numOfCPUs; i++) {
                FFmpegFrameGrabber clone = new FFmpegFrameGrabber(moviePath);
                Java2DFrameConverter frameConverter = new Java2DFrameConverter();
                clone.start();
                int startFrame = i * framesPerThread;
                int endFrame = (i == numOfCPUs - 1) ? numOfFrames : startFrame + framesPerThread;
                threads[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int j = startFrame; j < endFrame; j++) {
                            try {
                                clone.setFrameNumber(j * (length / numOfFrames));
                                BufferedImage frameImg = frameConverter.convert(clone.grabImage());
                                ImageIO.write(frameImg, "png", new File("./src/frames/" + j + ".png"));
                            } catch (Exception ignored) {
                            }
                        }
                    }
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            frameGrabber.stop();
            frameGrabber.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatSeconds(double d) {
        int minutes = (int) (d / 60);
        int seconds = (int) (d % 60);
        if (minutes > 0)
            return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    private void createPoster() {
        BufferedImage poster = new BufferedImage(numOfFrames * frameSize, (int) ((numOfFrames * frameSize) / widthToHeightRatio),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = poster.createGraphics();

        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        stopwatch.split();
        long[] times = new long[numOfFrames];

        for (int i = 0; i < numOfFrames; i++) {
            BufferedImage img = null;
            try {
                img = ImageIO.read(new File("./src/frames/" + i + ".png"));

                int[] rgb = null;
                if (colorMode == ColorMode.MOST_PROMINENT) {
                    MMCQ.CMap result = ColorThief.getColorMap(img, 5);
                    MMCQ.VBox dominantColor = result.vboxes.get(0);
                    rgb = dominantColor.avg(false);
                }

                if (colorMode == ColorMode.AVERAGE_RGB)
                    rgb = averageRGB(getPixels(img));

                graphics.setStroke(new BasicStroke(1));
                graphics.setColor(new Color(rgb[0], rgb[1], rgb[2]));
                graphics.fillRect(i * frameSize, 0, frameSize, poster.getHeight());
            } catch (IOException e) {
                e.printStackTrace();
            }
            times[i] = ((stopwatch.getTime() - stopwatch.getSplitTime()));
            stopwatch.split();
            progressBar.setValue(i + 1);
            int percentage = (int) (((double) i / (double) numOfFrames) * 100.0);
            progressBar.setString(percentage + "% - Estimated time remaining: " + formatSeconds((average(times) / 1000.0) * (numOfFrames - i)));
        }
        try {
            ImageIO.write(poster, "png", new File(saveFileDirectory + name + "poster" + numOfFrames + ".png"));
            progressBar.setString("Poster generation successful.");
        } catch (IOException e) {
            progressBar.setString("Poster generation failed.");

            e.printStackTrace();
        }
    }

    private long average(long[] times) {
        long total = 0;
        int validNums = 0;
        for (int i = 0; i < times.length; i++) {
            if (times[i] != 0) {
                total += times[i];
                validNums++;
            }
        }
        return (total / validNums);
    }

    private int[] averageRGB(Pixel[][] pixels) {
        int[] totals = new int[3];
        int numOfPixels = pixels.length * pixels[0].length;
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[0].length; j++) {
                totals[0] += pixels[i][j].getR();
                totals[1] += pixels[i][j].getG();
                totals[2] += pixels[i][j].getB();
            }
        }
        return new int[]{totals[0] / numOfPixels, totals[1] / numOfPixels, totals[2] / numOfPixels};
    }

    private Pixel[][] getPixels(BufferedImage img) {
        Pixel[][] pixels = new Pixel[img.getWidth()][img.getHeight()];
        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                int color = img.getRGB(i, j);
                int r = (color & 0x00ff0000) >> 16;
                int g = (color & 0x0000ff00) >> 8;
                int b = color & 0x000000ff;
                pixels[i][j] = new Pixel(r, g, b);
            }
        }
        return pixels;
    }

    private void clearWorkingDirs() {
        File framePath = new File("./src/frames");
        File[] frameFiles = framePath.listFiles();
        for (int i = 0; i < frameFiles.length; i++) {
            frameFiles[i].delete();
        }
    }

    private void updateDetails() {
        detailsArea
                .setText("Movie path: " + moviePath + "\nSave file directory: " + saveFileDirectory + "\nImage size: "
                        + numOfFrames * frameSize + "x" + (int) ((numOfFrames * frameSize) / widthToHeightRatio));
    }

    private String getMoviePath() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileFilter() {
            public String getDescription() {
                return ".mp4";
            }

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    return f.getName().toLowerCase().endsWith(".mp4");
                }
            }
        });
        String path = null;
        if (fc.showOpenDialog(fc) == JFileChooser.APPROVE_OPTION)
            path = (fc.getSelectedFile().getAbsolutePath());
        return path;
    }

    private String getSaveDirectory() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String path = null;
        if (fc.showOpenDialog(fc) == JFileChooser.APPROVE_OPTION)
            path = (fc.getSelectedFile().getAbsolutePath());
        return path + "\\";
    }
}

class MyKeyAdapter extends KeyAdapter {
    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if (((c < '0') || (c > '9')) && (c != KeyEvent.VK_BACK_SPACE)) {
            e.consume();
        }
    }
}

class Pixel {
    private int r, g, b;

    public Pixel(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int getR() {
        return r;
    }

    public int getG() {
        return g;
    }

    public int getB() {
        return b;
    }
}