package com.project.charlie.cryogenic.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.project.charlie.cryogenic.actors.*;
import com.project.charlie.cryogenic.data.*;
import com.project.charlie.cryogenic.handlers.*;
import com.project.charlie.cryogenic.managers.AssetsManager;
import com.project.charlie.cryogenic.managers.FileManager;
import com.project.charlie.cryogenic.misc.Constants;
import com.project.charlie.cryogenic.ui.GameLabel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Charlie on 12/02/2016.
 */
public class GameStage extends Stage implements ContactListener {
    private Box2DDebugRenderer renderer;
    OrthographicCamera camera;
    World world;

    private GameHandler gameHandler;
    private ObstacleGameHandler obstacleGameHandler;
    private CreatorHandler creatorHandler;
    private StarMapHandler mapHandler;
    private FileManager fileManager;
    private PlanetHandler planetHandler;
    private Boundary boundaryBottom;
    private Boundary boundaryTop;
    private Boundary boundaryLeft;
    private Boundary boundaryRight;


    private ArrayList<Turret> turrets = new ArrayList<Turret>();
    private ArrayList<Bullet> bullets = new ArrayList<>();
    private ArrayList<Explosion> explosions = new ArrayList<>();
    private HashMap<Object, String> projectiles = new HashMap<>();
    private HashMap<String, Planet> planets = new HashMap<String, Planet>();
    public HashMap<String, GameLabel> labels = new HashMap<String, GameLabel>();

    private int bulletsToCreate;
    private float accumulator = 0f;
    private final float TIME_STEP = 1 / 300f;

    static int VIEWPORT_WIDTH = Constants.APP_WIDTH;
    static int VIEWPORT_HEIGHT = Constants.APP_HEIGHT;
    private Vector3 touchPoint;

    ArrayList<Asteroid> asteroids = new ArrayList<Asteroid>();

    private ArrayList<Body> dead = new ArrayList<Body>();


    String infoLabelString = "Player HP: %hp%\n%tatkspd%";
    Cryogenic cryogenic;

    FitnessHandler fitnessHandler;
    PlayerHandler playerHandler;
    FPSLogger fpsLogger;

    /**
     * todo
     * fade out background to show moving stages
     * REMEMBER TO IMPLEMENT LIVES YOU TWAT.
     */


    public GameStage(Cryogenic cryogenic) {
        super(new ScalingViewport(Scaling.stretch, VIEWPORT_WIDTH, VIEWPORT_HEIGHT,
                new OrthographicCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)));

        Gdx.input.setInputProcessor(this);
        Gdx.input.setCatchBackKey(true);

        touchPoint = new Vector3();
        playerHandler = new PlayerHandler(this);
        gameHandler = new GameHandler(this);
        obstacleGameHandler = new ObstacleGameHandler(this);
        creatorHandler = new CreatorHandler(this);
        mapHandler = new StarMapHandler(this);
        fitnessHandler = new FitnessHandler(this);
        this.cryogenic = cryogenic;
        fpsLogger = new FPSLogger();

        AssetsManager.music.play();

        setupFitness();
        setUpMenu();

        if (Constants.DEBUG)
            renderer = new Box2DDebugRenderer();

        // todo audio & animation
    }

    public void setupFitness() {
        fitnessHandler.setCryogenic(cryogenic);
        if (fitnessHandler.connectToApi()) {
            playerHandler.addCurrency((int) fitnessHandler.calculateCurrencyGain());
            playerHandler.addCurrency((!fitnessHandler.achieved && fitnessHandler.hasAchievedTarget()) ? 5000 : 0);
        }
        gameHandler.setPlayerHandler(playerHandler);
        obstacleGameHandler.setPlayerHandler(playerHandler);
    }

    public void setUpFitnessMenu() {
        if (fitnessHandler.connectToApi()) {
            clear();
            setUpStage(4, planetHandler);
            Gdx.app.log("SS", "Connected. Setting up text..");
            fitnessHandler.setUpFitnessText();
        } else {
            Gdx.app.log("SS", "Signing in...");
            fitnessHandler.signIn();
        }
    }

    public void setUpMenu() {
        gameHandler.gameMode = Constants.GAMEMODE_NOTHING;
        for (Timer.Task timer : gameHandler.timers)
            timer.cancel();
        if (gameHandler.pickupTimer != null)
            gameHandler.pickupTimer.cancel();
        if (obstacleGameHandler.obstacleTimer != null)
            obstacleGameHandler.obstacleTimer.cancel();

        clear();
        setUpPreChoice();

        creatorHandler.setUpCreatorButton();
        mapHandler.setUpMapButton();
        fitnessHandler.setUpFitnessButton();
        playerHandler.savePlayer();
    }

    public void setUpPreChoice() {
        setUpCamera();
        setUpStage(3, planetHandler);
    }

    public void setUpPlanet(String planet) {
        clear();
        gameHandler.gameMode = Constants.GAMEMODE_OBSTACLES;
        planetHandler = FileManager.loadPlanet(planet);
        setUpStage(0, planetHandler);
        obstacleGameHandler.setUpPlayer();
        setUpBoundaries();
        obstacleGameHandler.setUpLabels(planet);
        obstacleGameHandler.setUpControls();
        obstacleGameHandler.setUpInfoText();
        obstacleGameHandler.setUpAsteroids();
        obstacleGameHandler.setUpHPBar();
        Gdx.app.log("GS", "Width: " + getCamera().viewportWidth);

    }

    public void scheduleRemoval(final Body body, float time) {
        gameHandler.timers.add(new Timer().scheduleTask(new Timer.Task() {
            @Override
            public void run() {
                if (body != null && body.getUserData() != null) {
                    addDead(body);
                    ((ActorData) body.getUserData()).isRemoved = true;
                }
            }
        }, time));
    }

    public void scheduleExplosionRemoval(final Body body, float time, final Explosion explosion) {
        gameHandler.timers.add(new Timer().scheduleTask(new Timer.Task() {
            @Override
            public void run() {
                if (body != null) {
                    try {
                        explosion.addAction(Actions.removeActor());
                        world.destroyBody(body);
                    } catch (Exception e) {
                        Gdx.app.log("Exception", "Exception caught attempting to remove explosion actor.");
                        e.printStackTrace();
                    }
                }
            }
        }, time));

    }

    public void setUpNormalLevel() {
        gameHandler.gameMode = Constants.GAMEMODE_NORMAL;
        for (Timer.Task timer : gameHandler.timers)
            timer.cancel();

        clear();
        if (planetHandler == null)
            loadPlanet();
        setUpStage(1, planetHandler);
        gameHandler.setUpPlayer(); // todo carry over stats + health from previous stage?
        gameHandler.setUpTurrets(planetHandler);
        setUpBoundaries();
        gameHandler.setUpControls();
        gameHandler.setUpStageCompleteLabel();
        gameHandler.setUpInfoText();
        gameHandler.setUpHPBar();
        gameHandler.setUpTargetBar();
        gameHandler.setUpPickupDrops();
        Gdx.app.log("GS", "Width: " + getCamera().viewportWidth);

    }

    public void setUpMap() {
        gameHandler.gameMode = Constants.GAMEMODE_MAP;
        clear();
        setUpStage(2, planetHandler);
        mapHandler.createPlanets(0);
    }

    public void setUpStageCreator() {
        gameHandler.gameMode = Constants.GAMEMODE_CREATOR;
        clear();
        setUpStage(4, planetHandler);
        setUpBoundaries();
        creatorHandler.setUpLabels();
        creatorHandler.setUpButtons();
    }


    public void loadPlanet() {
        planetHandler = FileManager.parsePlanet(world);
    }

    public GameLabel createLabel(String text, Vector3 bounds, float width, float height, int fadeOut, float size) {
        Rectangle rect = new Rectangle(bounds.x, bounds.y, width, height);
        GameLabel toReturn = new GameLabel(rect, text, size);
        addActor(toReturn);
        if (fadeOut > 0)
            toReturn.addAction(Actions.sequence(Actions.fadeOut(fadeOut), Actions.hide()));
        return toReturn;
    }

    public String getDebugText() {
//        String text = infoLabelString;
//        if (Constants.DEBUG) {
//            if (gameHandler.gameMode == Constants.GAMEMODE_NORMAL && turrets.size() > 0) {
//                String tempText = "";
//                for (Turret turret : turrets) {
//                    tempText = tempText + String.format("\nTurret %d HP: %f SPD: %f", turrets.indexOf(turret),
//                            turret.getActorData().getHealth(), turret.getActorData().getTurretType().getFireRate());
//
//                }
//                text = text.replace("%tatkspd%", tempText);
//            } else
//                text = text.replace("%tatkspd%", "");
//        } else
//            text = text.replace("%tatkspd%", "");
        String text = "";
        if (gameHandler.getPlayer() != null)
            text = "X: " + gameHandler.getPlayer().getX() + "\n Y: " + gameHandler.getPlayer().getY();
        else if (obstacleGameHandler.getPlayer() != null)
            text = "X: " + obstacleGameHandler.getPlayer().getX() + "\n Y: " + obstacleGameHandler.getPlayer().getY();

//            text = text.replace("%hp%", gameHandler.getPlayer().getActorData().getHealth() + "");
        return text;
    }

    public void setUpStage(int stage, PlanetHandler planetHandler) {
        setUpWorld();
        setUpBackground(stage, planetHandler);
    }

    public void setUpWorld() {
        world = WorldHandler.createWorld();
        world.setContactListener(this);
    }

    public void setUpBoundaries() {
        boundaryTop = new Boundary(WorldHandler.createBoundary(world, Constants.TOP));
        boundaryBottom = new Boundary(WorldHandler.createBoundary(world, Constants.BOTTOM));
        boundaryLeft = new Boundary(WorldHandler.createHorizontalBoundary(world, 0));
        boundaryRight = new Boundary(WorldHandler.createHorizontalBoundary(world, 1));
        addActor(boundaryTop);
        addActor(boundaryBottom);
        addActor(boundaryLeft);
        addActor(boundaryRight);
    }

    public void setUpBackground(int stage, PlanetHandler planetHandler) {
        addActor(new Background(stage, planetHandler));
    }

    public void setUpCamera() {
        camera = new OrthographicCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0f);
        camera.update();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        // if paused -> return

        createBullets(); // force all actor creation to occur before stepping

        // Fixed timestep
        accumulator += delta;
        while (accumulator >= delta) {
            world.step(TIME_STEP, 6, 2);
            accumulator -= TIME_STEP;
        }

        if (Constants.DEBUG)
            renderer.render(world, camera.combined);

//        fpsLogger.log();


        checkBounds();
        removeDeadBodies();
        checkEndGame();
        if (gameHandler.gameMode == Constants.GAMEMODE_NORMAL)
            gameHandler.updateLabel();
        if (gameHandler.gameMode == Constants.GAMEMODE_OBSTACLES)
            obstacleGameHandler.updateLabel();
    }

    private void createBullets() {
        if (gameHandler.gameMode != Constants.GAMEMODE_CREATOR) {
            if (gameHandler.gameMode == Constants.GAMEMODE_OBSTACLES)
                obstacleGameHandler.createBullets(bulletsToCreate);
            else if (gameHandler.gameMode == Constants.GAMEMODE_NORMAL)
                gameHandler.createBullets(bulletsToCreate);
        }
    }

    private void checkBounds() {
        for (Map.Entry<Object, String> cursor : projectiles.entrySet()) {
            if (cursor.getValue().equals(Constants.BULLET_ASSET_ID)) {
                Bullet bullet = (Bullet) cursor.getKey();
                if ((bullet.getX() + bullet.getWidth() > VIEWPORT_WIDTH)
                        || bullet.getX() + bullet.getWidth() < 0) {
                    dead.add(bullet.getBody());
                    bullet.getActorData().isRemoved = true;
                }
            }
        }
        for (Asteroid asteroid : asteroids) {
            boolean leftBounds = (asteroid.getX() + asteroid.getWidth() < 0);
            boolean bottomBounds = (asteroid.getY() + asteroid.getHeight() < 0);
            boolean topBounds = (asteroid.getY() + asteroid.getHeight() > VIEWPORT_HEIGHT);
            if (leftBounds || bottomBounds || topBounds) {
                dead.add(asteroid.getBody());
                asteroid.getActorData().isRemoved = true;
            }
        }
        // todo pickups
    }

    private void removeDeadBodies() { // todo this needs sorting out
        ArrayList<Body> removed = new ArrayList<Body>();
        for (int j = 0; j < dead.size(); j++) {
            Body body = dead.get(j);
            if (body != null && !world.isLocked()) {
                if (WorldHandler.isBullet(body)) {
                    BulletActorData b_data = (BulletActorData) body.getUserData();
                    if (b_data != null && b_data.isRemoved) {
                        removed.add(body);
                        if (projectiles.containsKey(b_data.bullet))
                            projectiles.remove(b_data.bullet);
                        b_data.bullet.addAction(Actions.removeActor());
                        world.destroyBody(body);
                        body.setUserData(null);
                        body = null;
                    }
                } else if (WorldHandler.isLaser(body)) {
                    LaserActorData l_data = (LaserActorData) body.getUserData();
                    if (l_data != null && l_data.isRemoved) {
                        removed.add(body);
                        if (projectiles.containsKey(l_data.getLaser())) {
                            projectiles.remove(l_data.getLaser());
                        }
                        l_data.laser.addAction(Actions.removeActor());
                        world.destroyBody(body);
                        body.setUserData(null);
                        body = null;
                    }
                } else if (WorldHandler.isTesla(body)) {
                    TeslaActorData t_data = (TeslaActorData) body.getUserData();
                    if (t_data != null && t_data.isRemoved) {
                        removed.add(body);
                        if (projectiles.containsKey(t_data.getTesla())) {
                            projectiles.remove(t_data.getTesla());
                        }
                        t_data.getTesla().addAction(Actions.removeActor());
                        try {
                            world.destroyBody(body);
                        } catch (Exception ignored) {
                        }
                        body.setUserData(null);
                        body = null;
                    }
                } else if (WorldHandler.isTurret(body)) {
                    TurretActorData t_data = (TurretActorData) body.getUserData();
                    if (t_data != null && t_data.isRemoved && turrets.contains(t_data.turret)) {
                        removed.add(body);
                        Gdx.app.log("Dead", "Removed turret. Remaining: " + turrets.size());
                        turrets.remove(t_data.turret);
                        t_data.turret.addAction(Actions.removeActor());
                        world.destroyBody(body);
                        body.setUserData(null);
                        body = null;
                    }
                } else if (WorldHandler.isAsteroid(body)) {
                    AsteroidActorData a_data = (AsteroidActorData) body.getUserData();
                    if (a_data != null && a_data.isRemoved && asteroids.contains(a_data.asteroid)) {
                        removed.add(body);
                        asteroids.remove(a_data.asteroid);
                        a_data.asteroid.addAction(Actions.removeActor());
                        world.destroyBody(body);
                        body.setUserData(null);
                        body = null;
                    }
                } else if (WorldHandler.isPickup(body)) {
                    PickupData pData = (PickupData) body.getUserData();
                    if (pData != null && pData.isRemoved && pickups.contains(pData.pickup)) {
                        removed.add(body);
                        pickups.remove(pData.pickup);
                        pData.pickup.addAction(Actions.removeActor());
                        world.destroyBody(body);
                        body.setUserData(null);
                        body = null;
                    }
                }
            }
        }
        dead.removeAll(removed);
    }

    public void checkEndGame() {
        if (gameHandler.gameMode == Constants.GAMEMODE_NORMAL && turrets.isEmpty()) {
            Gdx.app.log("GS", "Turrets dead.");
            setUpMenu();
        }
    }

//    // todo implement rotate
//    angle = (float) Math.atan2(touchPoint.y - player.getY(), touchPoint.x - player.getX());
//    angle = (float) (angle * (180/Math.PI));


    public Vector3 translateScreenToWorldCoordinates(int x, int y) {
        touchPoint.set(getCamera().unproject(new Vector3(x, y, 0)));
        return touchPoint;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        translateScreenToWorldCoordinates(x, y);
        switch (gameHandler.gameMode) {
            case Constants.GAMEMODE_CREATOR:
                creatorHandler.touchDown(touchPoint.x, touchPoint.y);
                break;
            case Constants.GAMEMODE_NORMAL:
                gameHandler.touchDown(touchPoint.x, touchPoint.y);
                break;
            case Constants.GAMEMODE_OBSTACLES:
                obstacleGameHandler.touchDown(touchPoint.x, touchPoint.y);
                break;
//            default:
//                gameHandler.touchDown(touchPoint.x, touchPoint.y);
//                break;
        }
        return super.touchDown(x, y, pointer, button);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        translateScreenToWorldCoordinates(screenX, screenY);
        switch (gameHandler.gameMode) {
            case Constants.GAMEMODE_CREATOR:
                creatorHandler.touchDragged(touchPoint.x, touchPoint.y);
                break;
            case Constants.GAMEMODE_NORMAL:
                gameHandler.touchDragged(touchPoint.x, touchPoint.y);
                break;
            case Constants.GAMEMODE_OBSTACLES:
                obstacleGameHandler.touchDragged(touchPoint.x, touchPoint.y);
                break;
        }
        return super.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public void beginContact(Contact contact) {
        Body a = contact.getFixtureA().getBody();
        Body b = contact.getFixtureB().getBody();

        if (WorldHandler.getDataType(a) == null ||
                WorldHandler.getDataType(b) == null)
            return;

        switch (gameHandler.gameMode) {
            case Constants.GAMEMODE_OBSTACLES:
                obstacleGameHandler.handleContact(a, b);
                break;
            case Constants.GAMEMODE_NORMAL:
                gameHandler.handleContact(a, b);
                break;
            case Constants.GAMEMODE_CREATOR:
                break;
        }
    }


    @Override
    public void endContact(Contact contact) {
        if (contact.getFixtureA() == null && contact.getFixtureB() == null)
            return;

        Body a = contact.getFixtureA().getBody();
        Body b = contact.getFixtureB().getBody();

        if (WorldHandler.getDataType(a) == null ||
                WorldHandler.getDataType(b) == null)
            return;

        switch (gameHandler.gameMode) {
            case Constants.GAMEMODE_OBSTACLES:
                obstacleGameHandler.endContact(a, b);
                break;
            case Constants.GAMEMODE_NORMAL:
                gameHandler.endContact(a, b);
                break;
            case Constants.GAMEMODE_CREATOR:
                break;
        }

    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }

    @Override
    public boolean keyDown(int keyCode) {
        if (keyCode == Input.Keys.BACK) {
            playerHandler.savePlayer();
            dead.clear();
            setUpMenu();
        }
        return false;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public PlanetHandler getPlanetHandler() {
        return planetHandler;
    }


    public void addBullet() {
        bulletsToCreate++;
    }

    public World getWorld() {
        return world;
    }

    public void addTurret(Turret turret) {
        turrets.add(turret);
        addActor(turret);
    }

    public void addAsteroid(Asteroid asteroid) {
        asteroids.add(asteroid);
        addActor(asteroid);
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public void addDead(Body body) {
        dead.add(body);
    }

    public void addProjectile(String type, Object projectile) {
        projectiles.put(projectile, type);
        switch (type) {
            case Constants.BULLET_ASSET_ID:
                addActor((Bullet) projectile);
                break;
            case Constants.LASER_ASSET_ID:
                addActor((Laser) projectile);
                break;
            case Constants.TESLA_ASSET_ID:
                addActor((Tesla) projectile);
                break;
        }
    }

    public void setBulletsToCreate(int bulletsToCreate) {
        this.bulletsToCreate = bulletsToCreate;
    }

    public ArrayList<Turret> getTurrets() {
        return turrets;
    }

    public ArrayList<Bullet> getBullets() {
        return bullets;
    }

    public void addPlanet(Planet planet, String name) {
        planets.put(name, planet);
        addActor(planet);
    }

    public void addLabel(String key, GameLabel label) {
        labels.put(key, label);
        addActor(label);
    }

    public GameLabel getLabel(String key) {
        return labels.get(key);
    }

    public PlayerHandler getPlayerHandler() {
        return playerHandler;
    }

    ArrayList<Pickup> pickups = new ArrayList<>();

    public void addPickup(Pickup pickup) {
        pickups.add(pickup);
        addActor(pickup);
    }

    public void addExplosion(Explosion explosion) {
        explosions.add(explosion);
        addActor(explosion);
    }
}
