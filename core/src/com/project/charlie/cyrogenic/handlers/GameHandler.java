package com.project.charlie.cyrogenic.handlers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Timer;
import com.project.charlie.cyrogenic.actors.*;
import com.project.charlie.cyrogenic.data.*;
import com.project.charlie.cyrogenic.game.GameStage;
import com.project.charlie.cyrogenic.misc.Constants;
import com.project.charlie.cyrogenic.objects.TurretJSON;
import com.project.charlie.cyrogenic.ui.GameLabel;

import java.util.Random;

/**
 * Created by Charlie on 05/03/2016.
 */
public class GameHandler {

    GameStage stage;
    public int gameMode;
    private Player player;
    private Touchpad touchPad;
    GameLabel infoLabel;
    Random random;
    Bar playerHPBar;
    Bar targetBar;
    Timer.Task damageTimer;
    PlayerHandler playerHandler;

    public GameHandler(GameStage stage) {
        this.stage = stage;
        random = new Random();
        gameMode = Constants.GAMEMODE_NOTHING;
    }

    public void setUpControls() {
        Skin touchpadSkin = new Skin();
        touchpadSkin.add("touchBackground", new Texture(Constants.TOUCH_BG_PATH));
        touchpadSkin.add("touchKnob", new Texture(Constants.TOUCH_KNOB_PATH));
        // Style
        Touchpad.TouchpadStyle touchpadStyle = new Touchpad.TouchpadStyle();
        // create drawables
        Drawable touchBackground = touchpadSkin.getDrawable("touchBackground");
        Drawable touchKnob = touchpadSkin.getDrawable("touchKnob");
        // Apply drawables
        touchpadStyle.background = touchBackground;
        touchpadStyle.knob = touchKnob;

        touchPad = new Touchpad(10, touchpadStyle);
        touchPad.setBounds(10, 10, 120, 120);
        stage.addActor(touchPad);
    }

    public void setPlayerHandler(PlayerHandler playerHandler) {
        this.playerHandler = playerHandler;
    }

    public void setUpPlayer() {
        // null check
        player = new Player(WorldHandler.createPlayer(stage.getWorld()));
        stage.addActor(player);
    }

    public void setUpTurrets(PlanetHandler planetHandler) { // TODO: handle stages
        if (planetHandler == null)
            planetHandler = stage.getPlanetHandler();

        for (TurretJSON turret : planetHandler.getTurrets()) {
            Constants.TurretType type = Constants.TurretType.valueOf(turret.getType());
            Turret temp = new Turret(WorldHandler.createTurret(stage.getWorld(), turret.getX(), turret.getY(), type.getWidth(), type.getHeight()), type); // TODO "warp in" animation
            temp.getActorData().turret = temp;
            stage.addTurret(temp);
        }
    }

    public void setUpInfoText() {
        Rectangle bounds = new Rectangle(stage.getCamera().viewportWidth * 0.10f, stage.getCamera().viewportHeight * 0.88f, 100, 20);
        infoLabel = new GameLabel(bounds, "Currency: " + playerHandler.getCurrentCurrency(), 0.7f);
        stage.addActor(infoLabel);
    }

    public void updateLabel() {
        infoLabel.setText("Currency: " + playerHandler.getCurrentCurrency());
    }

    public void setUpStageCompleteLabel() {
        stage.addLabel("StageComplete", stage.createLabel("Planet Entry completed", new Vector3(stage.getCamera().viewportWidth / 3,
                stage.getCamera().viewportHeight - 10, 0), 100, 20, 10, 0.3f));
    }

    public void setUpNormalButton() {
        TextButton normalButton = new TextButton("Stage Two: Planet Assault", new Skin(Gdx.files.internal(Constants.BUTTONS_SKIN_PATH)), "default");
        normalButton.setPosition(stage.getCamera().viewportWidth / 3, stage.getCamera().viewportHeight / 2 - 60f);
        normalButton.setBounds(stage.getCamera().viewportWidth / 3, stage.getCamera().viewportHeight / 2 - 60f, 250, 40);
        normalButton.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                stage.setUpNormalLevel();
                return true;
            }
        });
        stage.addActor(normalButton);
    }

    public void setUpHPBar() {
        GameLabel hpLabel = new GameLabel(new Rectangle(0, stage.getCamera().viewportHeight, 140, 20), "Player HP", 0.5f);
        stage.addActor(hpLabel);

        playerHPBar = new Bar(WorldHandler.createBar(stage.getWorld(), 20, stage.getCamera().viewportHeight * 0.95f, 100), Color.RED);
        stage.addActor(playerHPBar);
        Gdx.app.log("GH", "Bar created");
    }

    public void setUpTargetBar() {
        GameLabel targetLabel = new GameLabel(new Rectangle(stage.getCamera().viewportWidth * 0.90f, stage.getCamera().viewportHeight, 100, 20), "", 0.5f);
        stage.addLabel("TargetLabel", targetLabel);

        targetBar = new Bar(WorldHandler.createBar(stage.getWorld(), stage.getCamera().viewportWidth * 0.90f, stage.getCamera().viewportHeight * 0.95f, 100), Color.PINK);
        stage.addActor(targetBar);
        targetBar.addAction(Actions.hide());
    }

    public void handleContact(Body a, Body b) {
        boolean bulletHitTurret = ((WorldHandler.isProjectile(a) && WorldHandler.isTurret(b)) ||
                (WorldHandler.isTurret(a) && WorldHandler.isProjectile(b)));

        boolean bulletHitPlayer = ((WorldHandler.isProjectile(a) && WorldHandler.isPlayer(b)) ||
                WorldHandler.isPlayer(a) && WorldHandler.isProjectile(b));

        final Body bullet;
        Body turret;
        Body player;

        if (bulletHitTurret) {
            if (WorldHandler.isProjectile(a)) {
                bullet = a;
                turret = b;
            } else {
                bullet = b;
                turret = a;
            }
            ActorData data = (ActorData) bullet.getUserData();
            TurretActorData t_data = (TurretActorData) turret.getUserData();

            if (data.getShotBy().equals(Constants.PLAYER_ASSET_ID)) {
                if (WorldHandler.isBullet(bullet)) {
                    data.isRemoved = true;
                    stage.addDead(bullet);
                }

                if (t_data.subHealth(WorldHandler.getProjectileDamage(bullet))) {
                    t_data.isRemoved = true;
                    t_data.turret.die();
                    stage.addDead(turret);
                    // death animation and stuff
                }

                if (!targetBar.isVisible())
                    targetBar.addAction(Actions.show());

                playerHandler.addCurrency(1);
                targetBar.setBarWidth((int) t_data.getHealth());
                stage.getLabel("TargetLabel").setText(t_data.getTurretType().name());
                Gdx.app.log("TURRET", "Turret HP:" + t_data.getHealth());
            }
        } else if (bulletHitPlayer) {
            if (a.isBullet()) {
                bullet = a;
                player = b;
            } else {
                player = a;
                bullet = b;
            }
            ActorData b_data = (ActorData) bullet.getUserData();

            PlayerActorData p_data = (PlayerActorData) player.getUserData();
            if (WorldHandler.isBullet(bullet)) {
                b_data.isRemoved = true;
                stage.addDead(bullet);
            }

            if (p_data.subHealth(WorldHandler.getProjectileDamage(bullet)) <= 0) {
                Gdx.app.log("PLAYER", "You died!");
            }
            if (WorldHandler.isConstantlyDamaging(bullet)) {
                damageTimer = new Timer().scheduleTask(new Timer.Task() {
                    @Override
                    public void run() {
                        damageTarget(bullet, WorldHandler.getProjectileDamage(bullet));
                    }
                }, 0, 750);
            }
            playerHPBar.setBarWidth((int) p_data.getHealth());
        }
    }

    public void endContact(Body a, Body b) {
        boolean bulletHitPlayer = ((WorldHandler.isProjectile(a) && WorldHandler.isPlayer(b)) ||
                WorldHandler.isPlayer(a) && WorldHandler.isProjectile(b));
        if (bulletHitPlayer) {
            Body bullet;
            if (WorldHandler.isProjectile(a))
                bullet = a;
            else
                bullet = b;

            if (WorldHandler.isConstantlyDamaging(bullet)) {
                damageTimer.cancel();
            }
        }
    }

    public void damageTarget(Body body, float damage) {
        if (WorldHandler.isPlayer(body)) {
            PlayerActorData pData = (PlayerActorData) body.getUserData();
            pData.subHealth(damage);
        }
    }

    public void createBullets(int bulletsToCreate) {
        if (gameMode != Constants.GAMEMODE_CREATOR) {
            if(playerHandler.getMultiShot())
                bulletsToCreate *= 2;
            for (int i = 0; i < bulletsToCreate; i++) {
                Bullet bullet = new Bullet(WorldHandler.createBullet(stage.getWorld(), player.getBodyX() + 1,
                        player.getBodyY(), Constants.PLAYER_ASSET_ID));
                BulletActorData data = bullet.getActorData();
                data.setDamage(playerHandler.getDamage());
                data.bullet = bullet;
                stage.addProjectile(Constants.BULLET_ASSET_ID, bullet);
            }
            stage.setBulletsToCreate(0);
            if (stage.getTurrets().size() > 0) {
                for (final Turret turret : stage.getTurrets()) {

                    final Constants.TurretType type = turret.getActorData().getTurretType();
                    boolean canFire = ((System.currentTimeMillis() - turret.getLastFiretime()) / 1000) > type.getFireRate();

                    if (random.nextFloat() > 0.3 && canFire) {
                        turret.setLastFiretime(System.currentTimeMillis());
                        switch (type) {
                            case MACHINE_GUN:
                                Bullet bullet = new Bullet(WorldHandler.createBullet(stage.getWorld(),
                                        turret.getBodyX() - 2, turret.getBodyY(), Constants.TURRET_ASSET_ID));
                                BulletActorData b_data = bullet.getActorData();
                                b_data.bullet = bullet;
                                b_data.setDamage(type.getDamage());
                                stage.addProjectile(Constants.BULLET_ASSET_ID, bullet);
                                break;
                            case LASER:
                                Laser laser = new Laser(WorldHandler.createLaser(stage.getWorld(),
                                        turret.getBodyX() - Constants.LASER_WIDTH * 0.51f,
                                        turret.getBodyY(), Constants.TURRET_ASSET_ID));
                                LaserActorData l_data = laser.getActorData();
                                l_data.laser = laser;
                                l_data.setDamage(type.getDamage());
                                stage.addProjectile(Constants.LASER_ASSET_ID, laser);
                                stage.scheduleRemoval(laser.getBody(), Constants.LASER_DELAY);
                                break;
                            case BURST:
                                new Timer().scheduleTask(new Timer.Task() {
                                    @Override
                                    public void run() {
                                        Bullet burstBullet = new Bullet(WorldHandler.createBullet(stage.getWorld(),
                                                turret.getBodyX() - 2, turret.getBodyY(), Constants.TURRET_ASSET_ID));
                                        BulletActorData burstData = burstBullet.getActorData();
                                        burstData.bullet = burstBullet;
                                        burstData.setDamage(type.getDamage());
                                        stage.addProjectile(Constants.BULLET_ASSET_ID, burstBullet);
                                    }
                                }, 0.3f, 0.3f, 2);
                                break;
                            case TESLA:
                                for (int x = 0; x < 2; x++) {
                                    Tesla tesla = new Tesla(WorldHandler.createTesla(stage.getWorld(),
                                            turret.getBodyX() + 2f,
                                            turret.getBodyY() + turret.getBodyHeight() / 2, Constants.TURRET_ASSET_ID, x),
                                            turret.getBodyX() + 2f,
                                            turret.getBodyY() + turret.getBodyHeight() / 2);
                                    TeslaActorData teslaData = tesla.getActorData();
                                    teslaData.setTesla(tesla);
                                    teslaData.setDamage(type.getDamage());
                                    stage.addProjectile(Constants.TESLA_ASSET_ID, tesla);
                                    stage.scheduleRemoval(tesla.getBody(), Constants.TESLA_DELAY);
                                }
                                break;
                        }
                    }
                }
            }
        }
    }


    public void touchDown(float x, float y) {
        int activeTouch = 0;
        for (int i = 0; i < 2; i++) {
            if (Gdx.input.isTouched(i)) activeTouch++;
        }

        if (player != null) {
            player.applyForce(touchPad.getKnobPercentX() * playerHandler.getSpeed(),
                    touchPad.getKnobPercentY() * playerHandler.getSpeed());
            if (activeTouch > 1) {
                stage.addBullet();
            }
        }
    }

    public void touchDragged(float x, float y) {
        if (player != null) {
            player.applyForce(touchPad.getKnobPercentX() * 10,
                    touchPad.getKnobPercentY() * 10);
        }
    }

    public Player getPlayer() {
        return player;
    }
}

