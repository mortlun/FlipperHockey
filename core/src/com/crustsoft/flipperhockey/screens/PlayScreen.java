package com.crustsoft.flipperhockey.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;

import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.crustsoft.flipperhockey.game.FHGame;
import com.crustsoft.flipperhockey.gameobjects.FieldContainer;
import com.crustsoft.flipperhockey.gameobjects.FlipperLeft;
import com.crustsoft.flipperhockey.gameobjects.FlipperRight;
import com.crustsoft.flipperhockey.gameobjects.Puck;
import com.crustsoft.flipperhockey.gameobjects.ScoreLineSensor;
import com.crustsoft.flipperhockey.helpers.AssetLoader;
import com.crustsoft.flipperhockey.helpers.B2DContactListener;
import com.crustsoft.flipperhockey.helpers.InputHandler;

import java.util.Random;

/**
 * Created by Morten on 28.02.2016.
 */
public class PlayScreen implements Screen {
    float screenWidth = Gdx.graphics.getWidth();
    float screenHeight = Gdx.graphics.getHeight();
    private int scoreLimit = 5;
    private int scorePlayerBot = 0;
    private int scorePlayerTop = 0;
    boolean isGoal = false;
    public boolean pauseGame = false;
    public boolean scoreBottom = false;
    public boolean scoreTop = false;
    private float gravitation = 2f;

    private InputHandler inputHandler;
    public World world;
    private Box2DDebugRenderer box2DDebugRenderer;
    private FHGame fhGame;
    private Screen parent;
    private OrthographicCamera camera, cameraStage;
    public Viewport viewport, stageViewport;
    private FlipperLeft flipperLeftBottom, flipperLeftTop;
    private FlipperRight flipperRightBottom, flipperRightTop;
    private Puck puck;
    private FieldContainer fieldContainer;
    Rectangle rectangle, rectangle2;
    ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    Texture laser;
    Texture laser2;
    Texture markers;
    Texture rect;
    public boolean toggle = true;
    private ScoreLineSensor scoreLineSensorBottom, scoreLineSensorTop;
    Random random;
    public float rand;
    public PlayScreenUI playScreenUI;
    InputMultiplexer inputMultiplexer;
    public static Sound cling, flipper;
    private float speed = 3f;
    public int counter = 0;
    ParticleEffect particleEffectBottom, particleEffectTop;

    public PlayScreen(final FHGame fhGame, Screen parent) {
        this.parent = parent;
        this.fhGame = fhGame;
        world = new World(new Vector2(0, 0), true);
        spriteBatch = fhGame.spriteBatch;
        playScreenUI = new PlayScreenUI(spriteBatch, this);
        box2DDebugRenderer = new Box2DDebugRenderer();

        //Particles
        initParticles();

        //Camera, Stage and Viewport
        this.camera = new OrthographicCamera();
        viewport = new ExtendViewport(fhGame.LOGICAL_V_WIDTH / FHGame.PPM, fhGame.LOGICAL_V_HEIGHT / FHGame.PPM, camera);
        viewport.apply();
        camera.position.set((fhGame.LOGICAL_V_WIDTH / FHGame.PPM) / 2, (fhGame.LOGICAL_V_HEIGHT / FHGame.PPM) / 2, 0);
        //Random start angle for puck
        getRandomAngle();
        //Sounds
        //cling = Gdx.audio.newSound(Gdx.files.internal("cling.wav"));
        // flipper = Gdx.audio.newSound(Gdx.files.internal("flipp.wav"));

        //Setup input for menu and game
        inputHandler = new InputHandler(this, screenWidth / FHGame.V_WIDTH, screenHeight / FHGame.V_HEIGHT);
        inputMultiplexer = new InputMultiplexer(playScreenUI.stage, inputHandler);
        Gdx.input.setInputProcessor(inputMultiplexer);

        //Scorelinesensors, positioned top and bottom, not visible on 4:3 only 16:9.
        scoreLineSensorBottom = new ScoreLineSensor(this, false, FHGame.LOGICAL_V_WIDTH / 2, -(fhGame.V_HEIGHT - fhGame.LOGICAL_V_HEIGHT) / 4);
        scoreLineSensorTop = new ScoreLineSensor(this, true, FHGame.LOGICAL_V_WIDTH / 2, (fhGame.V_HEIGHT - fhGame.LOGICAL_V_HEIGHT) / 4 + fhGame.LOGICAL_V_HEIGHT);

        //Setup flippers & puck
        flipperLeftBottom = new FlipperLeft(this, 147, 145 - 82, -speed, true);
        flipperLeftTop = new FlipperLeft(this, 147, 995 - 98, speed, true);

        flipperRightBottom = new FlipperRight(this, 493, 145 - 82, speed, false);
        flipperRightTop = new FlipperRight(this, 493, 995 - 98, -speed, false);

        puck = new Puck(this, FHGame.LOGICAL_V_WIDTH / 2, fhGame.LOGICAL_V_HEIGHT / 2);

        fieldContainer = new FieldContainer(this);

        rectangle = new Rectangle(0, 0, 640, 960);
        rectangle2 = new Rectangle(-40, -90, 720, 1140);

        shapeRenderer = new ShapeRenderer();
        box2DDebugRenderer.SHAPE_STATIC.add(Color.BLUE);
        world.setContactListener(new B2DContactListener());

        markers = new Texture("markers.png");
        markers.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        laser = new Texture("laser.png");
        laser2 = new Texture("laserOverlayStatic.png");
        laser2.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        laser.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        rect = new Texture("rect.png");
        rect.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }


    public void update(float delta) {

        if (puck.bodyPuck.getPosition().y > (FHGame.LOGICAL_V_HEIGHT / 2) / FHGame.PPM) {
            world.setGravity(new Vector2(0, gravitation));

        }
        if (puck.bodyPuck.getPosition().y < (FHGame.LOGICAL_V_HEIGHT / 2) / FHGame.PPM) {
            world.setGravity(new Vector2(0, -gravitation));

        }
        if (counter < 50) {
            if (toggle && counter % 3 == 0) {
                puck.getPuckBody().setLinearVelocity(2.0f, 0.0f);

            } else if (counter % 3 == 0) {
                puck.getPuckBody().setLinearVelocity(-2.0f, 0.0f);

            }
            toggle = !toggle;
        }

        counter++;

        if (counter > 50 && toggle) {
            // puck.setDynamic();

            puck.getPuckBody().setType(BodyDef.BodyType.DynamicBody);
            puck.getPuckBody().setLinearVelocity(new Vector2(MathUtils.cos(rand) * 5, MathUtils.sin(rand) * 5));
            toggle = false;
        }


        world.step(fhGame.STEP, 6, 2);

        puck.update(delta);
        flipperLeftBottom.update();
        flipperRightBottom.update();
        flipperRightTop.update();
        flipperLeftTop.update();
        scoreLineSensorBottom.update();
        scoreLineSensorTop.update();
        playScreenUI.update(delta);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.position.set((fhGame.LOGICAL_V_WIDTH / FHGame.PPM) / 2, (fhGame.LOGICAL_V_HEIGHT / FHGame.PPM) / 2, 0);
        playScreenUI.stage.getViewport().update(width, height);
        // playScreenUI.stage.getCamera().position.set((fhGame.LOGICAL_V_WIDTH )/2, (fhGame.LOGICAL_V_HEIGHT )/2,0);
    }


    @Override
    public void render(float delta) {


        if (!pauseGame) {
            update(delta);
        }
        camera.update();
        // playScreenUI.stage.getCamera().update();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapeRenderer.setProjectionMatrix(camera.combined);
        spriteBatch.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(rectangle2.getX() / FHGame.PPM, rectangle2.getY() / FHGame.PPM, rectangle2.getWidth() / FHGame.PPM, rectangle2.getHeight() / FHGame.PPM);
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(rectangle2.getX() / FHGame.PPM, rectangle2.getY() / FHGame.PPM, rectangle2.getWidth() / FHGame.PPM, rectangle2.getHeight() / FHGame.PPM);
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(rectangle.getX() / FHGame.PPM, rectangle.getY() / FHGame.PPM, rectangle.getWidth() / FHGame.PPM, rectangle.getHeight() / FHGame.PPM);
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.end();


        //Markers
        spriteBatch.begin();
        spriteBatch.draw(markers, 58 / FHGame.PPM, 151.5f / FHGame.PPM, markers.getWidth() / FHGame.PPM, markers.getHeight() / fhGame.PPM);
        spriteBatch.end();
        spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        spriteBatch.begin();

        //Glow
        spriteBatch.setColor(Color.BLUE);
        spriteBatch.draw(laser, -36.5f / FHGame.PPM, 7.5f / FHGame.PPM, (laser.getWidth()) / FHGame.PPM, (laser.getHeight()) / FHGame.PPM);

        //Puck
        puck.setTextureGlow();
        puck.setColor(Color.BLUE);
        puck.draw(spriteBatch);

        //Scorelines
        scoreLineSensorBottom.setTextureGlow();
        scoreLineSensorBottom.setColor(Color.GREEN);
        scoreLineSensorBottom.draw(spriteBatch);

        scoreLineSensorTop.setTextureGlow();
        scoreLineSensorTop.setColor(Color.RED);
        scoreLineSensorTop.draw(spriteBatch);

        //Flipper
        flipperLeftBottom.setTextureGlow();
        flipperLeftBottom.setColor(Color.GREEN);
        flipperLeftBottom.draw(spriteBatch);

        flipperRightBottom.setTextureGlow();
        flipperRightBottom.setColor(Color.GREEN);
        flipperRightBottom.draw(spriteBatch);

        flipperLeftTop.setTextureGlow();
        flipperLeftTop.setColor(Color.RED);
        flipperLeftTop.draw(spriteBatch);

        flipperRightTop.setTextureGlow();
        flipperRightTop.setColor(Color.RED);
        flipperRightTop.draw(spriteBatch);
        //Overlay
        spriteBatch.setColor(Color.WHITE);
        spriteBatch.draw(rect, -36.5f / FHGame.PPM, 7.5f / FHGame.PPM, (rect.getWidth()) / FHGame.PPM, (rect.getHeight()) / FHGame.PPM);


        //Flipper
        flipperLeftBottom.setTextureFlipper();
        flipperLeftBottom.setColor(Color.WHITE);
        flipperLeftBottom.draw(spriteBatch);

        flipperRightBottom.setTextureFlipper();
        flipperRightBottom.setColor(Color.WHITE);
        flipperRightBottom.draw(spriteBatch);

        flipperLeftTop.setTextureFlipper();
        flipperLeftTop.setColor(Color.WHITE);
        flipperLeftTop.draw(spriteBatch);

        flipperRightTop.setTextureFlipper();
        flipperRightTop.setColor(Color.WHITE);
        flipperRightTop.draw(spriteBatch);


        //Puck
        puck.setTexturePuck();
        puck.setColor(Color.WHITE);
        puck.draw(spriteBatch);

        //Scorelines
        scoreLineSensorBottom.setTextureScoreLine();
        scoreLineSensorBottom.setColor(Color.WHITE);
        scoreLineSensorBottom.draw(spriteBatch);

        scoreLineSensorTop.setTextureScoreLine();
        scoreLineSensorTop.setColor(Color.WHITE);
        scoreLineSensorTop.draw(spriteBatch);

        //spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        particleEffectBottom.draw(spriteBatch, delta);

        particleEffectTop.draw(spriteBatch, delta);
        spriteBatch.end();
        //box2DDebugRenderer.render(world, camera.combined);
        spriteBatch.setProjectionMatrix(playScreenUI.stage.getCamera().combined);

        playScreenUI.stage.act(delta);
        playScreenUI.stage.draw();
    }

    public void pauseGame() {
        pauseGame = true;
    }

    public void resumeGame() {

        pauseGame = false;

    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);

    }

    @Override
    public void dispose() {

    }


    public void addScorePlayerBot() {
        AssetLoader.airhorn.play();
        scorePlayerBot++;
        if (scorePlayerBot == scoreLimit) {
            if (particleEffectTop.isComplete()) {
                particleEffectTop.reset();
            }
            particleEffectTop.start();
            playScreenUI.showWinBottomPlayer();
            playScreenUI.showLoseTopPlayer();

        } else {
            playScreenUI.showGoalBottomPlayer();
            if (particleEffectTop.isComplete()) {
                particleEffectTop.reset();
            }
            particleEffectTop.start();
            isGoal = true;


        }

    }

    public void addScorePlayerTop() {
        AssetLoader.airhorn.play();

        scorePlayerTop++;
        if (scorePlayerTop == scoreLimit) {
            if (particleEffectBottom.isComplete()) {
                particleEffectBottom.reset();
            }
            particleEffectBottom.start();
            playScreenUI.showWinTopPlayer();
            playScreenUI.showLoseBottomPlayer();

        } else {
            playScreenUI.showGoalTopPlayer();
            if (particleEffectBottom.isComplete()) {
                particleEffectBottom.reset();
            }
            particleEffectBottom.start();
            isGoal = true;
        }
    }

    public void reset() {
        world.clearForces();
        counter = 0;
        getPuck().resetPuck();
        isGoal = false;
        toggle = true;
        getRandomAngle();
    }

    public void getRandomAngle() {
        int i = MathUtils.random(0, 1);
        switch (i) {
            case 0:
                rand = MathUtils.random(-60, 60) * MathUtils.degreesToRadians;
                break;
            case 1:
                rand = MathUtils.random(120, 240) * MathUtils.degreesToRadians;
                break;
        }

    }

    private void initParticles() {
        particleEffectBottom = new ParticleEffect();
        particleEffectBottom.load(Gdx.files.internal("particleBottom.p"), Gdx.files.internal(""));
        particleEffectBottom.setPosition((fhGame.LOGICAL_V_WIDTH / 2) / FHGame.PPM, (-(fhGame.V_HEIGHT - fhGame.LOGICAL_V_HEIGHT) / 4) / FHGame.PPM);

        particleEffectTop = new ParticleEffect();
        particleEffectTop.load(Gdx.files.internal("particleTop.p"), Gdx.files.internal(""));
        particleEffectTop.setPosition((fhGame.LOGICAL_V_WIDTH / 2) / FHGame.PPM, ((fhGame.V_HEIGHT - fhGame.LOGICAL_V_HEIGHT) / 4 + fhGame.LOGICAL_V_HEIGHT) / FHGame.PPM);
    }

    public PlayScreen(Viewport viewport) {
        this.viewport = viewport;
    }

    public Viewport getViewport() {
        return viewport;
    }

    public FlipperLeft getFlipperLeftBottom() {
        return flipperLeftBottom;
    }

    public FlipperLeft getFlipperLeftTop() {
        return flipperLeftTop;
    }

    public FlipperRight getFlipperRightBottom() {
        return flipperRightBottom;
    }

    public FlipperRight getFlipperRightTop() {
        return flipperRightTop;
    }

    public Puck getPuck() {
        return puck;
    }

    public FieldContainer getFieldContainer() {
        return fieldContainer;
    }

    public int getScorePlayerTop() {
        return scorePlayerTop;
    }

    public int getScorePlayerBot() {
        return scorePlayerBot;
    }

    public Screen getParent() {
        return parent;
    }

    public FHGame getFhGame() {
        return fhGame;
    }
}
