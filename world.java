import java.util.*;
import greenfoot.*;

/**
 * 
 */
public class world extends World
{

    /**
     * Constructor for objects of class world.
     */
    public world()
    {
        super(600, 600, 1);
        prepare();
        showText("Выпито пивца:  ",100,15);

    }

    /**
     * Prepare the world for the start of the program.
     * That is: create the initial objects and add them to the world.
     */
    private void prepare()
    {
        bottle bottle = new bottle();
        addObject(bottle,164,173);
        bottle bottle2 = new bottle();
        addObject(bottle2,265,368);
        bottle bottle3 = new bottle();
        addObject(bottle3,334,359);
        bottle bottle4 = new bottle();
        addObject(bottle4,450,105);
        bottle bottle5 = new bottle();
        addObject(bottle5,239,90);
        bottle bottle6 = new bottle();
        addObject(bottle6,499,475);
        bottle bottle7 = new bottle();
        addObject(bottle7,176,485);
        bottle bottle8 = new bottle();
        addObject(bottle8,178,286);
        bottle bottle9 = new bottle();
        addObject(bottle9,396,223);
        alk alk = new alk();
        addObject(alk,41,558);
        cop cop = new cop();
        addObject(cop,86,68);
        cop cop2 = new cop();
        addObject(cop2,388,65);
        cop cop3 = new cop();
        addObject(cop3,476,328);
    }
}
