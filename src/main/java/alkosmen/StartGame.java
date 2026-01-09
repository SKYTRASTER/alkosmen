package alkosmen;

import alkosmen.audio.MidiPlayer;

import java.io.IOException;
import java.util.Properties;
import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;

public class StartGame {
    static final System.Logger LOGGER = System.getLogger(StartGame.class.getName());

    public static void main(String[] args) {
        LOGGER.log(System.Logger.Level.INFO, "Запуск игры....");

        Properties aProperties = new Properties();
        try (var in = StartGame.class.getResourceAsStream("/alkosmen/config.properties")) { // <-- проверь имя
            if (in == null) throw new RuntimeException("Config not found: /alkosmen/config.properties");
            aProperties.load(in);

            Constants.Height = Integer.parseInt(aProperties.getProperty("Height"));
            Constants.Width  = Integer.parseInt(aProperties.getProperty("Width"));
            Constants.Size   = Integer.parseInt(aProperties.getProperty("Size"));
            Constants.Font   = aProperties.getProperty("Font");
            MidiPlayer midi = new MidiPlayer();
            midi.playLoop("/alkosmen/sounds/Golden-Brown.mid");




            JFrame frame = new JFrame(Constants.Title);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // 1) СНАЧАЛА фон
            frame.setContentPane(new alkosmen.service.BackgroundPanel());

            // 2) абсолютное позиционирование, чтобы setBounds работал
            frame.getContentPane().setLayout(null);

            frame.setSize(Constants.Width, Constants.Height);
            frame.setResizable(false);

            alkosmen.ui.PixelButton start = new alkosmen.ui.PixelButton("Начать движуху");
            alkosmen.ui.PixelButton end   = new alkosmen.ui.PixelButton("Выйти");

            start.setBounds(Constants.Width / 2 - 180, Constants.Height / 2 - 20, 360, 70);
            end.setBounds(Constants.Width / 2 - 180, Constants.Height / 2 + 70, 360, 70);



            start.addActionListener(e -> newGame());
            end.addActionListener(e -> frame.dispose());

            frame.getContentPane().add(start);
            frame.getContentPane().add(end);

            // 3) ПОСЛЕ всего показываем
            frame.setVisible(true);

        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.ERROR, "Start error", e);
            e.printStackTrace();
        }
    }

    private static void newGame() {
        JFrame frame = new JFrame(" ");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(250, 250);
        frame.setResizable(false);
        frame.setVisible(true);

        Game g = new Game();
        g.setPreferredSize(new Dimension(Constants.Width, Constants.Height));
        frame.add(g);
        frame.pack();
        g.start();
    }
}
