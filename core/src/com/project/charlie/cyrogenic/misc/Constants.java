package com.project.charlie.cyrogenic.misc;

import com.badlogic.gdx.math.Vector2;

public class Constants {

    /* Application Constants */
    public static final float PIXELS_TO_METRES = 100f;

    public static final boolean DEBUG = false;

    static final float WORLD_TO_BOX = 0.032f; // Pixels * 0.032 to get metres
    public static final float BOX_TO_WORLD = 32; // Metres * 32 to get pixels


    public static float ConvertToBox(float n) {
        return n * WORLD_TO_BOX;
    }

    public static float ConvertToScreen(float n) {
        return Constants.BOX_TO_WORLD * n;
    }

    public static final int APP_WIDTH = 800;
    public static final int APP_HEIGHT = 480;
    public static final String BACKGROUND_ASSET_ID = "background";
    public static final String BACKGROUND_IMAGE_PATH = "images/backgrounds/background2.png";
    public static final String BACKGROUND_SPACE_ASSET_ID = "background_space";
    public static final String BACKGROUND_SPACE_IMAGE_PATH = "images/backgrounds/background.jpg";
    public static final int TOP = 1;
    public static final int BOTTOM = 0;

    public static final int UP = 0;
    public static final int DOWN = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;

    public static final Vector2 GRAVITY = new Vector2(0, -10);


    /* Game Modes */
    public static final int GAMEMODE_NORMAL = 0;
    public static final int GAMEMODE_OBSTACLES = 1;
    public static final int GAMEMODE_CREATOR = 3;
    public static final int GAMEMODE_MAP = 4;
    public static final int GAMEMODE_NOTHING = 5;

    /* Masks */
    public static final short BULLET_ENTITY = 0x1;    // 0001
    public static final short TURRET_BULLET_ENTITY = 0x5;    // 0005
    public static final short PLAYER_ENTITY = 0x6;    // 0001
    public static final short ASTERIODENTITY = 0x9;    // 0002
    public static final short COLLIDE_BOUNDARY_ENTITY = 0x3;    // 0003
    public static final short BOUNDARY_ENTITY = 0x1 << 1; // 0010 or 0x2 in hex
    public static final short DEFAULT_ENTITY = 0x3 << 1; // 0010 or 0x2 in hex


    /* Player  */
    public static final String PLAYER_ASSET_ID = "player";
    public static final String PLAYER_IMAGE_PATH = "images/player/plr_ship_trans.png";
    public static final int PLAYER_X = 1;
    public static final int PLAYER_Y = 5 + 1; // GROUND y + ground height
    public static final float PLAYER_GRAVITY = 0f;
    public static final float PLAYER_WIDTH = 1.3f;
    public static final float PLAYER_HEIGHT = 1.5f;
    public static final float PLAYER_DENSITY = 0.5f;


    /* Defaults */
    public static final float DEFAULT_DENSITY = 0.5f;
    public static final float DEFUALT_GRAVITY = 0f;

    /* Turret */
    public static final String TURRET_ASSET_ID = "turret";
    public static final String TURRET_IMAGE_PATH = "images/turrets/turret_new.png";
    public static final int TURRET_X = 2;
    public static final int TURRET_Y = 7;
    public static final float TURRET_WIDTH = 1.5f;
    public static final float TURRET_HEIGHT = 1.7f;
    public static final float TURRET_DENSITY = 4.5f;

    /* Bullet */
    public static final String BULLET_ASSET_ID = "bullet";
    public static final String BULLET_IMAGE_PATH = "images/misc/bullet.png";
    public static final float BULLET_WIDTH = 0.5f;
    public static final float BULLET_HEIGHT = 0.5f;
    public static final float BULLET_SPEED = 8f;

    /* Laser */
    public static final String LASER_ASSET_ID = "laser";
    public static final String LASER_IMAGE_PATH = "images/misc/laser.png";
    public static final float LASER_WIDTH = 19.5f;
    public static final float LASER_HEIGHT = 0.5f;
    public static final float LASER_DELAY = 7f;

    /* Tesla */

    public static final String TESLA_ASSET_ID = "tesla";
    public static final String TESLA_IMAGE_PATH = "images/misc/shock_one.png";
    public static final float TESLA_WIDTH = 8f;
    public static final float TESLA_HEIGHT = 1f;
    public static final float TESLA_DELAY = 2f;


    /* Misc */
    public static final String BLOCK_ASSET_ID = "block";
    public static final String BLOCK_ASSET_PATH = "images/misc/block.png";

    public static final String BOX_ASSET_ID = "box";
    public static final String BOX_ASSET_PATH = "images/misc/boundingBox.png";

    public static final String BUTTONS_SKIN_PATH = "data/uiskin.json";

    /* Asteroid */
    public static final String ASTEROID_ASSET_ID = "asteroid";
    public static final String ASTEROID_IMAGE_PATH = "images/asteroids/asteroid.png";
    public static final float ASTEROID_WIDTH = 1.2f;
    public static final float ASTEROID_HEIGHT = 1.2f;
    public static final float ASTEROID_DENSITY = 3f;


    public static final String TOUCH_BG_PATH = "images/misc/touchBackground_new.png";
    public static final String TOUCH_KNOB_PATH = "images/misc/touchKnob_new.png";


    public static final String PLANET_DESERT_ASSET_ID = "planet_desert";
    public static final String PLANET_DESERT_IMAGE_PATH = "images/planets/planet_desert.png";

    public static final String PLANET_WATER_ASSET_ID = "planet_water";
    public static final String PLANET_WATER_IMAGE_PATH = "images/planets/planet_water.png";

    public enum TurretType {
        MACHINE_GUN(0.5f, 2f, 100), LASER(10.5f, 3f, 150), BURST(1.3f, 7f, 50), TESLA(2.5f, 12.5f, 100);
        private final float fireRate;
        private final float damage;
        private final float health;

        TurretType(float fireRate, float damage, float health) {
            this.fireRate = fireRate;
            this.damage = damage;
            this.health = health;
        }

        public float getFireRate() {
            return fireRate;
        }

        public float getDamage() {
            return damage;
        }

        public float getHealth() {
            return health;
        }
    }




}
