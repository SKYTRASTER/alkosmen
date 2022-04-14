package alkosmen;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.fail;

import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Start {
    private static final Logger LOGGER = LogManager.getLogger(Start.class);

    public static void main(String[] args) {
        LOGGER.info("Запуск игры....");
        // String rootPath =
        // Thread.currentThread().getContextClassLoader().getResource("").getPath();
        // String appConfig = "config";
        Properties aProperties = new Properties();
        try {

            aProperties.load(Game.class.getResourceAsStream("config"));
            LOGGER.info("Ширина " + aProperties.getProperty("Width") + " ,высота " + aProperties.getProperty("Height"));
            Constants.Width = Integer.parseInt(aProperties.getProperty("Width"));
            Constants.Height = Integer.parseInt(aProperties.getProperty("Height"));

            JFrame frame = new JFrame(Constants.Title);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new GridBagLayout());
            frame.setSize(Constants.Width, Constants.Height);
            frame.setResizable(false);
            frame.setVisible(true);
            JButton start = new JButton("Начать путь алкаша");
            JButton end = new JButton("Выйти");
            start.setSize(150, 40);
            start.setLocation(frame.getWidth() / 2 - 75, frame.getHeight() / 2 - 70);
            start.setVisible(true);
            end.setSize(150, 40);
            end.setLocation(frame.getWidth() / 2 - 75, frame.getHeight() / 2 - 20);
            start.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    // TODO Auto-generated method stub

                }
            });
            end.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    closeWindow(frame);

                }

            });
            frame.getContentPane().add(start);
            frame.getContentPane().add(end);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private static void closeWindow(JFrame frame) {
        frame.dispose();
    }

}
