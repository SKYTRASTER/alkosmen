package alkosmen;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.scene.canvas.Canvas;


public final class Game extends Canvas implements Runnable {
    private static int Height;
    private static int Width;
    private static boolean running;
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        LOGGER.info("Запуск игры....");
        // String rootPath =
        // Thread.currentThread().getContextClassLoader().getResource("").getPath();
        // String appConfig = "config";
        Properties aProperties = new Properties();
        try {

            aProperties.load(Game.class.getResourceAsStream("config"));
            LOGGER.info("Ширина " + aProperties.getProperty("Width") + " ,высота " + aProperties.getProperty("Height"));

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

    public void start() {
        running = true;
        new Thread(this).start();
    }
}
