import java.util.*;
import greenfoot.*;

/**
 * 
 */
public class cop extends Actor
{
  int ax;

    /**
     * Act - do whatever the cop wants to do. This method is called whenever the 'Act' or 'Run' button gets pressed in the environment.
     */
    public void act()
    {
       
       copsmove();
      
       
       getWorld().showText("  "+ax +" " ,450,15);
    }
    public void copsmove()
    {
         alk alx = new alk() ;
      //  alk aly = new alk();
         alx.getXY();
         ax=alx.ax;
      // move(4);
   //   if (Greenfoot.getRandomNumber(getX())<10)
   //     {
       // turn(Greenfoot.getRandomNumber(90));
    //    setLocation(getX() + 2, getY());
    // }
     // if (getY()<50)
     //   {
       // turn(Greenfoot.getRandomNumber(90));
       //  setLocation(getX()-1, getY());
     // }
   //   if(getX()==ax)
      {
    //    setLocation(getX()+2, getY());  
  //      move(2);
    }
     
      
        
    }
    
}
