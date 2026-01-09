package alkosmen.objects;



import alkosmen.Point;
import alkosmen.Sprite;
import alkosmen.interfaces.IGameObject;

import java.awt.*;


public class Ground extends Texture implements IGameObject {

    public Ground(Point p, Image image) {
        super(p);
        this.sprite = new Sprite(image);   // sprite должен быть protected в Texture
    }

    @Override
    public void draw(Graphics g) {
        //sprite.draw(g, getPoint()); // или position, если так называется в Texture
    }

}
