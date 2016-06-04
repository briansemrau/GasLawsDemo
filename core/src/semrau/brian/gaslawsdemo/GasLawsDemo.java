package semrau.brian.gaslawsdemo;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.ArrayList;

public class GasLawsDemo extends ApplicationAdapter {

    // GUI

    private Stage stage;
    private Skin skin;

    private Table equationTable;
    private Table specialControlsTable;

    private Slider molSlider; // mol
    private TextField molField;
    private Slider vSlider; // mL
    private TextField vField;
    private Slider tSlider; // K
    private TextField tField;
    private Slider pSlider; // atm
    private TextField pField;

    private boolean fixingValues;

    private TextButton stpButton;

    private ButtonGroup<TextButton> lockGroup;
    private TextButton pvLock;
    private TextButton ptLock;
    private TextButton vtLock;

    // Rendering

    private OrthographicCamera camera;
    private Box2DDebugRenderer b2renderer;

    private ShapeRenderer shapeRenderer;

    // Particle Simulation

    private World b2world;
    private float timeToStep;
    private final float stepTime = 1.0f / 60.0f;
    private Body walls;
    private ArrayList<Body> particles;
    private ArrayList<Body> toRemove;
    private final float particleRadius = 3.0f / 16.0f;
    private final float wallThickness = 5;

    private final int MOLE = 20;
    private final float R = 0.082057f; // L atm mol-1 K-1

    // Performance measuring

    private double guiRender;
    private double simRender;
    private double simStep;
    private double relocateParticles;

    @Override
    public void create() {
        createGUI();
        initRendering();
        createSim();

        Gdx.input.setInputProcessor(stage);
    }

    private void createGUI() {
        // Create stage and skin
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("visui/uiskin.json"));

        // Create tables
        equationTable = new Table(skin);
        equationTable.pad(10);
        equationTable.setFillParent(true);
        equationTable.align(Align.center | Align.top);

        specialControlsTable = new Table(skin);
        specialControlsTable.pad(10);
        specialControlsTable.setFillParent(true);
        specialControlsTable.align(Align.right | Align.top);

        // Create components

        // ######################
        // Sliders
        // ######################

        molSlider = new Slider(0, 10, 0.01f, true, skin);
        molSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (fixingValues) return;
                setMoles(molSlider.getValue());
            }
        });

        vSlider = new Slider(1, 100, 0.01f, true, skin); // TODO tune
        vSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (fixingValues) return;
                setVolume(vSlider.getValue());
            }
        });

        tSlider = new Slider(0.1f, 2000, 0.01f, true, skin);
        tSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float a = tSlider.getValue() / tSlider.getMaxValue();
                tSlider.setColor(
                        a,
                        0.5f,
                        1 - a,
                        1);

                if (fixingValues) return;

                setTemp(tSlider.getValue());
            }
        });

        pSlider = new Slider(0, 100, 0.01f, true, skin);
        pSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (fixingValues) return;
                setPressure(pSlider.getValue());
            }
        });

        // ######################
        // Text Fields
        // ######################

        TextField.TextFieldFilter filter = new TextField.TextFieldFilter() {
            @Override
            public boolean acceptChar(TextField textField, char c) {
                return "1234567890.,".contains("" + c);
            }
        };

        molField = new TextField("" + getMoles(), skin);
        molField.setTextFieldFilter(filter);
        molField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (fixingValues) return;
                molField.setColor(Color.YELLOW);
            }
        });
        molField.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    setMoles(Float.parseFloat(molField.getText()));
                    molField.setColor(Color.WHITE);
                    return true;
                }
                return false;
            }
        });

        vField = new TextField("" + getVolume(), skin);
        vField.setTextFieldFilter(filter);
        vField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (fixingValues) return;
                vField.setColor(Color.YELLOW);
            }
        });
        vField.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    setVolume(Float.parseFloat(vField.getText()));
                    vField.setColor(Color.WHITE);
                    return true;
                }
                return false;
            }
        });

        tField = new TextField("" + getTemp(), skin);
        tField.setTextFieldFilter(filter);
        tField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (fixingValues) return;
                tField.setColor(Color.YELLOW);
            }
        });
        tField.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    setTemp(Float.parseFloat(tField.getText()));
                    tField.setColor(Color.WHITE);
                    return true;
                }
                return false;
            }
        });

        pField = new TextField("" + getPressure(), skin);
        pField.setTextFieldFilter(filter);
        pField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (fixingValues) return;
                pField.setColor(Color.YELLOW);
            }
        });
        pField.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    setPressure(Float.parseFloat(pField.getText()));
                    pField.setColor(Color.WHITE);
                    return true;
                }
                return false;
            }
        });

        fixValues(22.414f, 273.15f, 1.0f, 1);

        // ######################
        // Buttons
        // ######################

        stpButton = new TextButton("Set to STP", skin);
        stpButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                fixValues(22.414f, 273.15f, 1.0f, 1);
            }
        });

        pvLock = new TextButton("P vs V", skin, "toggle");
        pvLock.addListener(new ClickListener() {

            @Override
            public void clicked(InputEvent event, float x, float y) {
                tSlider.setVisible(false);

                pSlider.setVisible(true);
                vSlider.setVisible(true);
            }
        });
        ptLock = new TextButton("P vs T", skin, "toggle");
        ptLock.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                vSlider.setVisible(false);

                pSlider.setVisible(true);
                tSlider.setVisible(true);
            }
        });
        vtLock = new TextButton("V vs T", skin, "toggle");
        vtLock.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                pSlider.setVisible(false);

                tSlider.setVisible(true);
                vSlider.setVisible(true);
            }
        });
        lockGroup = new ButtonGroup<TextButton>(pvLock, ptLock, vtLock);

        ptLock.setChecked(true);
        vSlider.setVisible(false);

        // Build equationTable
        equationTable.add(new Label("P", skin, "large"));
        equationTable.add(new Label("V", skin, "large"));
        equationTable.add(new Label("=", skin, "large"));
        equationTable.add(new Label("n", skin, "large"));
        equationTable.add(new Label("R", skin, "large"));
        equationTable.add(new Label("T", skin, "large"));
        equationTable.row();

        equationTable.add(pSlider);
        equationTable.add(vSlider);
        equationTable.add(new Label("", skin)).width(50).padRight(10);
        equationTable.add(molSlider);
        equationTable.add(new Label("", skin)).width(50).padRight(10);
        equationTable.add(tSlider);
        equationTable.row();

        equationTable.add(pField).width(50).padLeft(5).padRight(5);
        equationTable.add(vField).width(50).padLeft(5).padRight(5);
        equationTable.add(new Label("=", skin, "large"));
        equationTable.add(molField).width(50).padLeft(5).padRight(5);

        equationTable.add(new Label("R", skin, "large"));
        equationTable.add(tField).width(50).padLeft(5).padRight(5);
        equationTable.row();

        equationTable.add(new Label("atm", skin));
        equationTable.add(new Label("L", skin));
        equationTable.add(new Label("", skin));
        equationTable.add(new Label("mol", skin));
        equationTable.add(new Label("", skin));
        equationTable.add(new Label("K", skin));

        stage.addActor(equationTable);

        // Build special controls table
        specialControlsTable.add(stpButton);
        specialControlsTable.row();
        specialControlsTable.add(pvLock).padTop(10);
        specialControlsTable.row();
        specialControlsTable.add(ptLock).padTop(10);
        specialControlsTable.row();
        specialControlsTable.add(vtLock).padTop(10);

        stage.addActor(specialControlsTable);
    }

    private void initRendering() {
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = 0.1f;
        b2renderer = new Box2DDebugRenderer(true, true, false, true, false, false);
//        b2renderer.SHAPE_AWAKE.set(Color.WHITE);
//        b2renderer.SHAPE_STATIC.set(Color.WHITE);

        shapeRenderer = new ShapeRenderer();
    }

    private void createSim() {
        b2world = new World(new Vector2(), false);
        World.setVelocityThreshold(1);

        particles = new ArrayList<Body>();
        toRemove = new ArrayList<Body>();

        createWalls();

        setMoles(getMoles());
    }

    private void createWalls() {
        if (walls != null)
            b2world.destroyBody(walls);

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        walls = b2world.createBody(bodyDef);

        float thick = wallThickness / 2;
        float size = wallSize() / 2;

        FixtureDef wall = new FixtureDef();
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(size + wallThickness, thick, new Vector2(0, -size - thick), 0);
        wall.shape = shape;
        wall.friction = 0;
        wall.restitution = 1;
        walls.createFixture(wall);

        shape.setAsBox(size + wallThickness, thick, new Vector2(0, size + thick), 0);
        walls.createFixture(wall);

        shape.setAsBox(thick, size + wallThickness, new Vector2(size + thick, 0), 0);
        walls.createFixture(wall);

        shape.setAsBox(thick, size + wallThickness, new Vector2(-size - thick, 0), 0);
        walls.createFixture(wall);
    }

    private void createParticles(int count) {
        while (count < particles.size()) {
            b2world.destroyBody(particles.get(0));
            particles.remove(0);
        }

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.restitution = 1;
        fixtureDef.friction = 0;
        fixtureDef.density = 1;
        fixtureDef.shape = new CircleShape();
        fixtureDef.shape.setRadius(particleRadius);

        float size = wallSize() / 2;
        float thick = wallThickness / 2;

        float vel = (float) Math.sqrt(getTemp() * 2); // KE = 1/2 m v^2

        for (int i = particles.size(); i < count; i++) {
            float x = MathUtils.random(-size + thick, size - thick);
            float y = MathUtils.random(-size + thick, size - thick);
            bodyDef.position.set(x, y);
            bodyDef.linearVelocity.set(MathUtils.random() * 2 - 1, MathUtils.random() * 2 - 1);
            bodyDef.linearVelocity.setLength(vel);

            Body particle = b2world.createBody(bodyDef);
            particle.createFixture(fixtureDef);

            particles.add(particle);
        }
    }

    private float wallSize() {
        return ((float) Math.sqrt(getVolume())) * 5;
    }

    private float getPressure() {
        return pSlider.getValue();
    }

    private float getVolume() {
        return vSlider.getValue();
    }

    private float getTemp() {
        return tSlider.getValue();
    }

    private float getMoles() {
        return molSlider.getValue();
    }

    private void setPressure(float p) {
        if (ptLock.isChecked()) {
            // Const V, update T; T = PV/nR
            fixValues(-1, p * getVolume() / (getMoles() * R), p, -1);
        } else if (pvLock.isChecked()) {
            // Const T, update V; V = nRT/P
            fixValues(getMoles() * R * getTemp() / p, -1, p, -1);
        } else {
            fixValues(-1, -1, p, -1);
        }
    }

    private void setVolume(float v) {
        if (pvLock.isChecked()) {
            // Const T, update P; P = nRT/V
            fixValues(v, -1, getMoles() * R * getTemp() / v, -1);
        } else if (vtLock.isChecked()) {
            // Const P, update T; T = PV/nR
            fixValues(v, getPressure() * v / (getMoles() * R), -1, -1);
        } else {
            fixValues(v, -1, -1, -1);
        }
    }

    private void setTemp(float k) {
        if (ptLock.isChecked()) {
            // Const V, update P; P = nRT/V
            fixValues(-1, k, getMoles() * R * k / getVolume(), -1);
        } else if (vtLock.isChecked()) {
            // Const P, update V; V = nRT/P
            fixValues(getMoles() * R * k / getPressure(), k, -1, -1);
        } else {
            fixValues(-1, k, -1, -1);
        }
    }

    private void setMoles(float m) {
        // Don't change volume
        if (ptLock.isChecked() || pvLock.isChecked()) {
            // Const T, update P; P = nRT/V
            fixValues(-1, -1, m * R * getTemp() / getVolume(), m);
        } else if (vtLock.isChecked()) {
            // Const P, update T; T = PV/nR
            fixValues(-1, getPressure() * getVolume() / (m * R), -1, m);
        } else {
            fixValues(-1, -1, -1, m);
        }
    }

    private String ezFormat(float val) {
        return "" + (Math.round(val * 100.0f) / 100.0f);
    }

    private void fixValues(float v, float t, float p, float m) {
        fixingValues = true;
        if (v != -1) {
            vSlider.setValue(v);
            vField.setText(ezFormat(vSlider.getValue()));
            vField.setColor(Color.WHITE);
            if (b2world != null)
                createWalls();
        }
        if (t != -1) {
            tSlider.setValue(t);
            tField.setText(ezFormat(tSlider.getValue()));
            tField.setColor(Color.WHITE);

//            if (particles != null) {
//                float vel = (float) Math.sqrt(getTemp() * 2); // KE = 1/2 m v^2
//                for (Body b : particles) {
//                    b.setLinearVelocity(b.getLinearVelocity().setLength(vel));
//                }
//            }
        }
        if (p != -1) {
            pSlider.setValue(p);
            pField.setText(ezFormat(pSlider.getValue()));
            pField.setColor(Color.WHITE);
        }
        if (m != -1) {
            molSlider.setValue(m);
            molField.setText(ezFormat(molSlider.getValue()));
            molField.setColor(Color.WHITE);
            if (b2world != null)
                createParticles((int) (getMoles() * MOLE));
        }
        fixingValues = false;
    }

    private void update(float delta) {

        timeToStep += delta;

        if (timeToStep >= stepTime) {
            // Destroy and recreate all particles found outside the box
            float size = wallSize() / 2 + wallThickness;
            for (Body b : particles) {
                if (b.getPosition().x < -size || b.getPosition().x > size || b.getPosition().y < -size || b.getPosition().y > size) {
                    b2world.destroyBody(b);
                    toRemove.add(b);
                }
            }
            int count = particles.size();
            particles.removeAll(toRemove);
            toRemove.clear();
            createParticles(count);

            // Make sure average temperature stays where it's set
            float vel2 = 0;
            for (Body b : particles) {
                vel2 += b.getLinearVelocity().len2();
            }
            vel2 /= particles.size();
            float scale = (float) Math.sqrt(getTemp() / vel2);
            for (Body b : particles) {
                b.setLinearVelocity(b.getLinearVelocity().scl(scale));
            }

            // Step
            b2world.step(stepTime, 3, 6);
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        update(delta);

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        // Draw simulation
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Particles
        for (Body p : particles) {
            float a = p.getLinearVelocity().len2() / tSlider.getMaxValue();
            shapeRenderer.setColor(
                    a,
                    0.5f,
                    1 - a,
                    1);
            shapeRenderer.circle(p.getPosition().x, p.getPosition().y, particleRadius + 0.1f, 7);
        }
        // Walls
        float size = wallSize() / 2;
        float thick = 0.5f;
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(-size - thick, -size - thick, size * 2 + thick * 2, thick);
        shapeRenderer.rect(-size - thick, size, size * 2 + thick * 2, thick);
        shapeRenderer.rect(-size - thick, -size - thick, thick, size * 2 + thick * 2);
        shapeRenderer.rect(size, -size - thick, thick, size * 2 + thick * 2);
        shapeRenderer.end();

//        b2renderer.render(b2world, camera.combined);

        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

}
