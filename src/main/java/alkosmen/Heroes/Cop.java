package alkosmen.Heroes;

import java.util.*;
import greenfoot.*;

/**
 * 
 */
public class Cop extends Actor {
  int ax;

  /**
   * Act - do whatever the cop wants to do. This method is called whenever the
   * 'Act' or 'Run' button gets pressed in the environment.
   */
  public void act() {
    if (getWorld() != null) {
      move();
    }

    // getWorld().showText(" "+ax +" " ,450,15);
  }

  public void move() {
    Actor alkobot = getOneObjectAtOffset(0, 0, Alkobot.class);
    ;
    // alkobot.ax;
    // System.out.println( );
    // alk aly = new alk();
    // getWorld().showText(alkobot.getY());
    // ax = alkobot.getX();

    // ax=ax.ax;
    // move(4);
    // if (Greenfoot.getRandomNumber(getX())<10)
    // {
    // turn(Greenfoot.getRandomNumber(90));
    // setLocation(getX() + 2, getY());
    // }
    // if (getY()<50)
    // {
    // turn(Greenfoot.getRandomNumber(90));
    // setLocation(getX()-1, getY());
    // }
    // if(getX()==ax)
    {
      // setLocation(getX()+2, getY());
      // move(2);
    }

  }

}
