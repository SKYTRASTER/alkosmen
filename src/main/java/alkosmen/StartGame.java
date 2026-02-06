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
        try (var in = StartGame.class.getResourceAsStream("/alkosmen/config.properties")) {
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

            // ⬇️ если пользователь жмёт крестик
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    midi.stop();
                }
            });

            frame.setContentPane(new alkosmen.service.BackgroundPanel());
            frame.getContentPane().setLayout(null);
            frame.setSize(Constants.Width, Constants.Height);
            frame.setResizable(false);

            alkosmen.ui.PixelButton start = new alkosmen.ui.PixelButton("Начать движуху");
            alkosmen.ui.PixelButton end   = new alkosmen.ui.PixelButton("Выйти");

            start.setBounds(Constants.Width / 2 - 180, Constants.Height / 2 - 20, 360, 70);
            end.setBounds(Constants.Width / 2 - 180, Constants.Height / 2 + 70, 360, 70);

            // ▶ START
            start.addActionListener(e -> {
                midi.stop();       // 🔥 остановить музыку
                frame.dispose();  // закрыть меню
                newGame();        // запустить игру
            });

            // ❌ EXIT
            end.addActionListener(e -> {
                midi.stop();      // 🔥 остановить музыку
                frame.dispose(); // закрыть окно
            });

            frame.getContentPane().add(start);
            frame.getContentPane().add(end);

            frame.setVisible(true);

        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.ERROR, "Start error", e);
            e.printStackTrace();
        }
    }


    private static void newGame() {
        JFrame frame = new JFrame(" ");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setLayout(new BorderLayout());
        frame.setResizable(false);

        Game g = new Game();
        frame.add(g, BorderLayout.CENTER);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                Game.stopGame(); // остановить цикл
            }
        });

        g.requestFocusInWindow();
        g.start();
    }


}
