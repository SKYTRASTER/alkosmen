package src.main.java.alkosmen.objects;


import src.main.java.alkosmen.Point;

/**
 * 
 */
public class GameMap {

    private boolean[][] walls;
    private boolean[][] grass;

    private int length;

    /**
     * Constructor for objects of class world.
     */
    public GameMap() {
        // length = Constants.LenOfMap;
        // food = new boolean[][length];
        walls = new boolean[100][100];
        grass = new boolean[100][100];

        // heroes = new byte[length][length];
    }

    /**
     * Prepare the world for the start of the program.
     * That is: create the initial objects and add them to the world.
     */
    public void setGrassAt(Point p) {
        grass[p.y][p.x] = true;
    }

    private void prepare() {
        /* Alkobot alkobot = new Alkobot(); */
        /* addObject(alkobot, 41, 558); */
        /* Bottle bottle = new Bottle(); */
        /* addObject(bottle, 164, 173); */
        /* Bottle bottle2 = new Bottle(); */
        /* addObject(bottle2, 265, 368); */
        /* Bottle bottle3 = new Bottle(); */
        /* addObject(bottle3, 334, 359); */
        /* Bottle bottle4 = new Bottle(); */
        /* addObject(bottle4, 450, 105); */
        /* Bottle bottle5 = new Bottle(); */
        /* addObject(bottle5, 239, 90); */
        /* Bottle bottle6 = new Bottle(); */
        /* addObject(bottle6, 499, 475); */
        /* Bottle bottle7 = new Bottle(); */
        /* addObject(bottle7, 176, 485); */
        /* Bottle bottle8 = new Bottle(); */
        /* addObject(bottle8, 178, 286); */
        /* Bottle bottle9 = new Bottle(); */
        /* addObject(bottle9, 396, 223); */

        /* Cop cop = new Cop(); */
        /* addObject(cop, 86, 68); */
        /* // cop cop2 = new cop(); */
        /* // addObject(cop2,388,65); */
        /* // cop cop3 = new cop(); */
        /* // addObject(cop3,476,328); */
    }
}
