import java.util.*;
import greenfoot.*;
/**
 * 
 */
public class alk extends Actor
{
public double score=0.5;
public int ax;
public int ay;
    /**
     * Act - do whatever the alk wants to do. This method is called whenever the 'Act' or 'Run' button gets pressed in the environment.
     */
    public void act()
    {
      move ();
      drink();

       
    }
    public void move()
    {
        getWorld().showText("  "+ax +" "+ay ,300,15);
       if(Greenfoot.isKeyDown("down"))
       {
          setLocation(getX(), getY() + 1);
         getXY();
           if(getY()%8!=0)      
         {
               
             setImage("alkdown" + 0 +".png");
            }
            else{setImage("alkdown" + 1 +".png");}
        }
        if(Greenfoot.isKeyDown("up"))
       {
          setLocation(getX(), getY() - 1);
                  getXY();
            if(getY()%8!=0)      
         {
               
             setImage("alkup" + 0 +".png");
            }
            else{setImage("alkup" + 1 +".png");}
        }
          if(Greenfoot.isKeyDown("left"))
       {
          setLocation(getX()-1, getY());
                   getXY();
           if(getX()%8!=0)      
         {
               
             setImage("alkleft" + 0 +".png");
            }
            else{setImage("alkleft" + 1 +".png");}
            
        }
        
        if(Greenfoot.isKeyDown("right"))
       {
          setLocation(getX()+1, getY());
                  getXY();
          if(getX()%8!=0)      
         {
               
             setImage("alkright" + 0 +".png");
            }

            else{
             setImage("alkright" + 1 +".png");

             {
             
            }
        
    }
        }
       
    }
    public void drink()
    {
        Actor bottle;
        bottle = getOneObjectAtOffset(0,0,bottle.class);
        if (bottle != null)
        {
            World world;
            world = getWorld();
            world.removeObject(bottle);
            getWorld().showText("  "+score + "Л" ,200,15);

            score=score+0.5;
        }
    }
   public void getXY()
   {
      ax=getX();
      ay=getY();  
    }
    
}
    