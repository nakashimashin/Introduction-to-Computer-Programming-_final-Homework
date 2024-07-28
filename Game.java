import java.io.*;
import java.util.Random;
import java.util.ArrayList;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class Game extends JFrame implements KeyListener {
    public static void main(String args[]) {
        Game game = new Game();
    }

    static Random random = new Random();

    Map map;
    ArrayList<Actor> actors;
    final static int PLAYER_ID = 0;
    final static int NUM_OF_MONSTERS = 4;

    Game() {
        super("Dungeon Game");
        
        map = new Map(this);
        actors = new ArrayList<Actor>();
        actors.add(new Player(this, "Taro"));
        for (int i = 0; i < NUM_OF_MONSTERS; i++) {
            if (random.nextInt(2) == 0) {
                actors.add(new Monster(this));
            } 
            else {
                actors.add(new RedMonster(this));
            }
        }

        setBounds(100, 100, Cell.SIZE * Map.XSIZE + 5 + 200, Cell.SIZE * Map.YSIZE + 25 + 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(map, BorderLayout.NORTH);

        JPanel actorsPanel = new JPanel();
        actorsPanel.setLayout(new GridLayout(10, 1));
        for (Actor a : actors) { actorsPanel.add(a); }
        add(actorsPanel, BorderLayout.SOUTH);

        setVisible(true);

        addKeyListener(this);
    }

    void print() { repaint(); }

    void turn() {
        for (Actor a : actors) { a.action(); }
        print();
    }

    static char keyChar;
    
    static char getKeyChar() { return keyChar; }
    
    static void setKeyChar(char c) { keyChar = c; }
    
    @Override
    public void keyPressed(KeyEvent e) {
        setKeyChar(e.getKeyChar());
        turn();
    }
    
    @Override
    public void keyReleased(KeyEvent e){}
    
    @Override
    public void keyTyped(KeyEvent e){}
}

class Map extends JPanel {
    static final int XSIZE = 13;
    static final int YSIZE = 11;

    Game game;
    Cell data[][] = new Cell[XSIZE][YSIZE];

    Map(Game game) {
        this.game = game;
        for (int x = 0; x < XSIZE; x++) {
            for (int y = 0; y < YSIZE; y++) {
                if ((x == 0)||(x == XSIZE-1)||
                    (y == 0)||(y == YSIZE-1)||
                    ((Math.abs(x-XSIZE/2) <= 1)&&
                     (Math.abs(y-YSIZE/2) <= 1))) {
                    data[x][y] = new RockCell();
                }
                else {
                    switch (Game.random.nextInt(5)) {
                    case 0: data[x][y] = new PotionCell();
                             break;
                    case 1: data[x][y] = new PoisonCell();
                             break;
                    default: data[x][y] = new EmptyCell();
                    }
                }
            }
        }
        data[XSIZE-2][YSIZE-2] = new GoalCell();

        setPreferredSize(new Dimension(Cell.SIZE*XSIZE, Cell.SIZE*YSIZE));
    }

    Cell getAt(Int2 position) { return data[position.x][position.y]; }

    void setAt(Int2 position, Cell cell) {
        data[position.x][position.y] = cell;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int y = 0; y < YSIZE; y++) {
            for (int x = 0; x < XSIZE; x++) {
                Int2 position = new Int2(x, y);
                getAt(position).paintAt(g, position);
                for (Actor a : game.actors) {
                    a.paintAt(g, position);
                }
            }
        }
    }

    Int2 getRandomCellPosition() {
        while (true) {
            Int2 position = new Int2(Game.random.nextInt(XSIZE),
                                     Game.random.nextInt(YSIZE));

            if (!getAt(position).isRockCell()) { return position; }
        }
    }
}

abstract class Actor extends JLabel {
    Game game;
    Int2 position;
    int hp;

    Actor(Game game, Int2 position, int hp) {
        super(" ");
        this.game = game;
        this.position = position;
        this.hp = hp;
    }

    Actor(Game game, int hp) {
        super(" ");
        this.game = game;
        this.hp = hp;
        while_loop: while (true) {
            position = game.map.getRandomCellPosition();
            for (Actor a : game.actors) {
                if (position.equal(a.position)) {
                    continue while_loop;
                }
            }
            return;
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        setText(toString());
    }

    void paintAt(Graphics g, Int2 position) {
        if (this.position.equal(position)) {
            g.drawImage(getImage(),
                        position.x*Cell.SIZE + Cell.SIZE*2/24,
                        position.y*Cell.SIZE + Cell.SIZE*2/24,
                        Cell.SIZE*20/24,
                        Cell.SIZE*20/24,
                        game.map);
        }
    }

    abstract Image getImage();

    boolean moveTo(Int2 direction) {
        Int2 target = position.add(direction);

        Cell cell = game.map.getAt(target);
        if (cell.isRockCell()) { return false; }

        for (Actor a : game.actors) {
            if (target.equal(a.position)) { return false; }
        }

        Cell newCell = cell.affectOn(this);
        game.map.setAt(target, newCell);
        position = target;
        return true;
    }

    abstract void action();

    void attackTo(Int2 direction) {
        Int2 target = position.add(direction);
        for (Actor a : game.actors) {
            if (target.equal(a.position)) {
                a.takeDamageFrom(this);
                return;
            }
        }
    }

    abstract void takeDamageFrom(Actor a);
    void giveDamageTo(Player p) {}
    void giveDamageTo(Monster m) {}
    void giveDamageTo(RedMonster r) {}
}

class Player extends Actor {
    static final int INIT_HP = 100;
    String name;

    Player(Game game, String name) {
        super(game, new Int2(1, 1), INIT_HP);
        this.name = name;
    }

    Player(Game game) { this(game, "Player"); }

    public String toString() { return name+" : "+position+" "+hp; }

    @Override
    void takeDamageFrom(Actor a) { a.giveDamageTo(this); }

    @Override
    void giveDamageTo(Monster m) { m.hp -= 20; }

    @Override
    void giveDamageTo(RedMonster r) { r.hp -= 15; }

    @Override
    void action() {
        Int2 direction;
        switch (Game.getKeyChar()) {
            case 'z': direction = new Int2(-1, +1); break;
            case 'x': direction = new Int2( 0, +1); break;
            case 'c': direction = new Int2(+1, +1); break;
            case 'a': direction = new Int2(-1,  0); break;
            case 'd': direction = new Int2(+1,  0); break;
            case 'q': direction = new Int2(-1, -1); break;
            case 'w': direction = new Int2( 0, -1); break;
            case 'e': direction = new Int2(+1, -1); break;
            default: direction = new Int2(0, 0);
        }

        if (!moveTo(direction)) { attackTo(direction); }
    }

    static final Image image1 = Toolkit.getDefaultToolkit().getImage("Player1.png");
    static final Image image2 = Toolkit.getDefaultToolkit().getImage("Player2.png");

    @Override
    Image getImage() {
        if (hp > INIT_HP/2) { return image1; }
        else { return image2; }
    }
}

class Monster extends Actor {
    static final int INIT_HP = 150;
    protected Image image = Toolkit.getDefaultToolkit().getImage("Monster.png");

    Monster(Game game) { super(game, INIT_HP); }

    @Override
    public String toString() { return "Monster : "+position+" "+hp; }

    @Override
    void takeDamageFrom(Actor a) { a.giveDamageTo(this); }

    @Override
    void giveDamageTo(Player p) { p.hp -= 30; }

    @Override
    void giveDamageTo(Monster m) { m.hp -= 5; }

    @Override
    void action() {
        Actor player = game.actors.get(game.PLAYER_ID);
        Int2 direction = player.position.sub(position).signum();

        if (!moveTo(direction)) {
            attackTo(direction);
        }
    }

    @Override
    Image getImage() { return image; }
}

class RedMonster extends Monster {
    private boolean isTransformed = false;

    RedMonster(Game game) {
        super(game);
        this.hp = INIT_HP;
    }

    @Override
    public String toString() {
        return "Red" + super.toString();
    }

    @Override
    void takeDamageFrom(Actor a) {
        super.takeDamageFrom(a);
        if (this.hp <= INIT_HP * 2 / 3 && !isTransformed) {
            this.isTransformed = true;
            this.image = Toolkit.getDefaultToolkit().getImage("RedMonster.png");
        }
        else if (this.hp <= INIT_HP / 3) {
            this.isTransformed = false;
            this.image = Toolkit.getDefaultToolkit().getImage("Monster.png");
        }
    }

    @Override
    void giveDamageTo(Player p) {
        if (isTransformed) {
            p.hp -= 80;
        } else {
            p.hp -= 60;
        }
    }

    @Override
    void giveDamageTo(Monster m) { m.hp -= 7; }
}


class Int2 {
    int x, y;

    Int2(int x, int y) { this.x = x; this.y = y; }
    Int2 add(Int2 a) { return new Int2(x + a.x, y + a.y); }
    Int2 sub(Int2 a) { return new Int2(x - a.x, y - a.y); }
    Int2 signum() { return new Int2(Integer.signum(x), Integer.signum(y)); }
    boolean equal(Int2 a) { return (x == a.x) && (y == a.y); }
    
    public String toString() { return "(" + x + "," + y + ")"; }
}

abstract class Cell {
    static final int SIZE = 48;

    boolean isRockCell() { return false; }

    abstract Cell affectOn(Actor a);

    abstract void paintAt(Graphics g, Int2 position);

    void drawCell(Graphics g, Int2 position) {
        g.drawRect(position.x*SIZE + SIZE/24,
                   position.y*SIZE + SIZE/24,
                   SIZE*22/24,
                   SIZE*22/24);
    }

    void fillCell(Graphics g, Int2 position) {
        g.fillRect(position.x*SIZE + SIZE/24,
                   position.y*SIZE + SIZE/24,
                   SIZE*22/24,
                   SIZE*22/24);
    }
}

abstract class SimpleCell extends Cell {
    @Override
    Cell affectOn(Actor a) { return this; }
}

abstract class TrapCell extends Cell {
    boolean visible = false;

    @Override
    Cell affectOn(Actor a) {
        visible = true;
        return this;
    }
}

class RockCell extends SimpleCell {
    @Override
    void paintAt(Graphics g, Int2 position) {
        g.setColor(new Color(100, 20, 20));
        fillCell(g, position);
    }

    @Override
    boolean isRockCell() { return true; }
}

class EmptyCell extends SimpleCell {
    @Override
    void paintAt(Graphics g, Int2 position) {
        g.setColor(new Color(128, 128, 128));
        drawCell(g, position);
    }
}

class PotionCell extends SimpleCell {
    @Override
    void paintAt(Graphics g, Int2 position) {
        g.setColor(new Color(0, 255, 255));
        fillCell(g, position);
    }

    @Override
    Cell affectOn(Actor a) {
        a.hp += 50;
        return new EmptyCell();
    }
}

class GoalCell extends SimpleCell {
    @Override
    void paintAt(Graphics g, Int2 position) {
        g.setColor(new Color(255, 255, 0));
        fillCell(g, position);
    }

    @Override
    Cell affectOn(Actor a) {
        if (a instanceof Player) {
            System.out.println("YOU WIN");
            System.exit(0);
        }
        return this;
    }
}

class PoisonCell extends TrapCell {
    @Override
    void paintAt(Graphics g, Int2 position) {
        if (visible) {
            g.setColor(new Color(0, 128, 0));
            fillCell(g, position);
        } else {
            g.setColor(new Color(128, 128, 128));
            drawCell(g, position);
        }
    }

    @Override
    Cell affectOn(Actor a) {
        a.hp -= 10;
        return super.affectOn(a);
    }
}


