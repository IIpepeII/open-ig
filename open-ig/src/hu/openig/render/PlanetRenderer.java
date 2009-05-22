/*
 * Copyright 2008-2009, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */
package hu.openig.render;

import hu.openig.core.Btn;
import hu.openig.core.BtnAction;
import hu.openig.core.Location;
import hu.openig.core.RoadType;
import hu.openig.core.Tile;
import hu.openig.core.TileFragment;
import hu.openig.model.GameBuilding;
import hu.openig.model.GameBuildingPrototype;
import hu.openig.model.GamePlanet;
import hu.openig.model.GameWorld;
import hu.openig.model.GameBuildingPrototype.BuildingImages;
import hu.openig.res.GameResourceManager;
import hu.openig.res.gfx.CommonGFX;
import hu.openig.res.gfx.PlanetGFX;
import hu.openig.res.gfx.TextGFX;
import hu.openig.sound.SoundFXPlayer;
import hu.openig.utils.PACFile.PACEntry;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Planet surface renderer class.
 * @author karnokd, 2009.01.16.
 * @version $Revision 1.0$
 */
public class PlanetRenderer extends JComponent implements MouseListener, MouseMotionListener, 
MouseWheelListener, ActionListener {
	/** */
	private static final long serialVersionUID = -2113448032455145733L;
	/** Contains the rectangle to highlight. */
	Rectangle tilesToHighlight;
	/** The rendering X offset. */
	int xoff = 56;
	/** The rendering Y offset. */
	int yoff = 27;
	/** Last mouse X coordinate. */
	int lastx;
	/** Last mouse Y coordinate. */
	int lasty;
	/** Panning the screen. */
	boolean panMode;
	/** The bytes of the current map. */
//	byte[] mapBytes;
	/** The surface variant. */
//	int surfaceVariant = 1;
	/** Zoom scale. */
	float scale = 1.0f;
	/** The surace type. */
//	int surfaceType = 1;
	/** Empty surface map array. */
	private static final byte[] EMPTY_SURFACE_MAP = new byte[65 * 65 * 2 + 4];
	/** The planet graphics. */
	private final PlanetGFX gfx;
	/** The common graphics. */
	private final CommonGFX cgfx;
	/** Rectangle for. */
	private Rectangle leftTopRect = new Rectangle();
	/** Rectangle for. */
	private Rectangle leftFillerRect = new Rectangle();
	/** Rectangle for. */
	private Rectangle leftBottomRect = new Rectangle();
	
	/** Rectangle for. */
	private Rectangle rightTopRect = new Rectangle();
	/** Rectangle for. */
	private Rectangle rightFillerRect = new Rectangle();
	/** Rectangle for. */
	private Rectangle rightBottomRect = new Rectangle();
	
	/** Button for buildable building list. */
	private Btn btnBuilding;
	/** Button for. */
	private Btn btnRadar;
	/** Button for. */
	private Btn btnBuildingInfo;
	/** Button for. */
	private Btn btnButtons;
	/** Button for. */
	private Btn btnColonyInfo;
	/** Button for. */
	private Btn btnPlanet;
	/** Button for. */
	private Btn btnStarmap;
	/** Button for. */
	private Btn btnBridge;
	/** The middle window for the surface drawing. */
	private Rectangle mainWindow = new Rectangle();
	
	/** Rectangle for. */
	private Rectangle buildPanelRect = new Rectangle();
	/** Rectangle for. */
	private Rectangle radarPanelRect = new Rectangle();
	/** Rectangle for. */
	private Rectangle buildingInfoPanelRect = new Rectangle();
	/** The last width. */
	private int lastWidth;
	/** The last height. */
	private int lastHeight;
	/** The left filler painter. */
	private TexturePaint leftFillerPaint;
	/** The right filler painter. */
	private TexturePaint rightFillerPaint;
	/** 
	 * The timer to scroll the building window if the user holds down the left mouse button on the
	 * up/down arrow.
	 */
	private Timer buildScroller;
	/** The scroll interval. */
	private static final int BUILD_SCROLL_INTERVAL = 500;
	/** Timer used to animate fade in-out. */
	private Timer fadeTimer;
	/** Fade timer interval. */
	private static final int FADE_INTERVAL = 25;
	/** The alpha difference to use when animating the fadeoff-fadein. */
	private static final float ALPHA_DELTA = 0.15f;
	/** THe fade direction is up (true) or down (false). */
	private boolean fadeDirection;
	/** The current darkening factor for the entire UI. 0=No darkness, 1=Full darkness. */
	private float darkness = 0f;
	/** The daylight factor for the planetary surface only. 1=No darkness, 0=Full darkness. */
	private float daylight = 0.5f;
	/** The text renderer. */
	private TextGFX text;
	/** The user interface sounds. */
	private SoundFXPlayer uiSound;
	/** Buttons which change state on click.*/
	private final List<Btn> toggleButtons = new ArrayList<Btn>();
	/** The press buttons. */
	private final List<Btn> pressButtons = new ArrayList<Btn>();
	/** The buttons which fire on mouse release. */
	private final List<Btn> releaseButtons = new ArrayList<Btn>();
	/** Event for starmap click. */
	private BtnAction onStarmapClicked;
	/** Event for information click. */
	private BtnAction onInformationClicked;
	/** Event for information click. */
	private BtnAction onListClicked;
	/** Event for bridge click. */
	private BtnAction onBridgeClicked;
	/** Event for planets click. */
	private BtnAction onPlanetsClicked;
	/** The game world. */
	private GameWorld gameWorld;
	/** The information bar renderer. */
	private InfobarRenderer infobarRenderer;
	/** The last rendering position. */
	private final AchievementRenderer achievementRenderer;
	/** Build image rectangle. */
	private final Rectangle buildImageRect = new Rectangle();
	/** Build image rectangle. */
	private final Rectangle buildNameRect = new Rectangle();
	/** Button for next building. */
	private Btn btnBuildNext;
	/** Button for previous building. */
	private Btn btnBuildPrev;
	/** Button for build. */
	private Btn btnBuild;
	/** Button for building list. */
	private Btn btnList;
	/** We are currently in build mode. */
	private boolean buildMode;
	/**
	 * Constructor, expecting the planet graphics and the common graphics objects.
	 * @param grm the game resource manager 
	 * @param uiSound the user interface sounds.
	 * @param infobarRenderer the information bar
	 * @param achievementRenderer the achievement renderer
	 */
	public PlanetRenderer(GameResourceManager grm, 
			SoundFXPlayer uiSound, InfobarRenderer infobarRenderer,
			AchievementRenderer achievementRenderer) {
		this.gfx = grm.planetGFX;
		this.cgfx = grm.commonGFX;
		this.text = cgfx.text;
		this.uiSound = uiSound;
		this.infobarRenderer = infobarRenderer;
		this.achievementRenderer = achievementRenderer;
		buildScroller = new Timer(BUILD_SCROLL_INTERVAL, this);
		buildScroller.setActionCommand("BUILD_SCROLLER");
		fadeTimer = new Timer(FADE_INTERVAL, this);
		fadeTimer.setActionCommand("FADE");
		initButtons();
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addMouseListener(this);
		setOpaque(true);
		
//		int w = Tile.toScreenX(33,-33) - Tile.toScreenX(-64, -64);
//		int h = Tile.toScreenY(1, 0) - Tile.toScreenY(-32, -96);
	}
	/**
	 * Returns the given surface map based on the type and variant.
	 * @param surfaceType the type index 1-7
	 * @param variant the variant index 1-9
	 * @return the pac entry for the surface or null if not existent
	 */
	private PACEntry getSurface(int surfaceType, int variant) {
		String mapName = "MAP_" + (char)('A' + (surfaceType - 1)) + variant + ".MAP";
		return gfx.getMap(mapName);
	}
	/** Rendering X coordinates. */
	static final int[] MAP_START_X = new int[97];
	/** Rendering Y coordinates. */
	static final int[] MAP_START_Y = new int[97];
	/** Rendering X end coordinates. */
	static final int[] MAP_END_X = new int[97];
	/** Rendering Y end coordinates. */
//	static int[] mapEndY = new int[97];
	static {
		// initialize map rendering stripe coordinates
		int idx = 0;
		for (int i = 1; i <= 32; i++) {
			MAP_START_X[idx] = i;
			MAP_START_Y[idx] = 1 - i;
			idx++;
		}
		int y = -32;
		for (int i = 32; i >= -32; i--) {
			MAP_START_X[idx] = i;
			MAP_START_Y[idx] = y;
			idx++;
			y--;
		}
		idx = 0;
		for (int i = 0; i >= -64; i--) {
			MAP_END_X[idx] = i;
//			mapEndY[idx] = i;
			idx++;
		}
		y = -65;
		for (int i = -63; i <= -32; i++) {
			MAP_END_X[idx] = i;
//			mapEndY[idx] = y;
			y--;
			idx++;
		}
	}
	/**
	 * Paints the entire planet window.
	 * @param g the graphics object
	 */
	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		int w = getWidth();
		int h = getHeight();
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, w, h);

		if (w != lastWidth || h != lastHeight) {
			lastWidth = w;
			lastHeight = h;
			// if the render window changes, re-zoom to update scrollbars
			updateRegions();
		}
		AffineTransform t = g2.getTransform();
		g2.scale(scale, scale);
		int k = 0;
		int j = 0;
		GamePlanet planet = gameWorld.player.selectedPlanet;
		if (planet == null) {
			planet = gameWorld.getOwnPlanetsInOrder().get(0);
			gameWorld.player.selectedPlanet = planet;
		}
		int surfaceType = planet.surfaceType.index;
		byte[] mapBytes = getSelectedPlanetSurface();
		// RENDER VERTICALLY
		Map<Integer, Tile> surface = gfx.getSurfaceTiles(surfaceType);
		for (int mi = 0; mi < MAP_START_X.length; mi++) {
			k = MAP_START_X[mi];
			j = MAP_START_Y[mi];
			int i = toMapOffset(k, j);
			int kmin = MAP_END_X[mi];
			while (i >= 0 && k >= kmin) {
				int tileId = (mapBytes[2 * i + 4] & 0xFF) - (surfaceType < 7 ? 41 : 84);
				int stripeId = mapBytes[2 * i + 5] & 0xFF;
				Tile tile = surface.get(tileId);
				Location l = Location.of(k, j);
				TileFragment tf = planet.map.get(l);
				if (tf != null && tf.fragment >= 0) {
					tile = tf.provider.getTile(l);
					// select appropriate stripe
					tile.createImage(daylight);
					stripeId = tf.fragment % tile.strips.length; // wrap around for safety
				}
				if (tile != null) {
					tile.createImage(daylight);
					// 1x1 tiles can be drawn from top to bottom
					if (tile.width == 1 && tile.height == 1) {
						int x = xoff + Tile.toScreenX(k, j);
						int y = yoff + Tile.toScreenY(k, j);
						if (x >= -tile.image.getWidth() && x <= (int)(getWidth() / scale)
								&& y >= -tile.image.getHeight() && y <= (int)(getHeight() / scale) + tile.image.getHeight()) {
							BufferedImage subimage = tile.strips[stripeId];
							g2.drawImage(subimage, x, y - tile.image.getHeight() + tile.heightCorrection, null);
						}
					} else 
					if (stripeId < 255) {
						// multi spanning tiles should be cut into small rendering piece for the current strip
						// ff value indicates the stripe count
						// the entire image would be placed using this bottom left coordinate
						int j1 = stripeId >= tile.width ? j + tile.width - 1 : j + stripeId;
						int k1 = stripeId >= tile.width ? k + (tile.width - 1 - stripeId) : k;
						int j2 = stripeId >= tile.width ? j : j - (tile.width - 1 - stripeId);
						int x = xoff + Tile.toScreenX(k1, j1);
						int y = yoff + Tile.toScreenY(k1, j2);
						// use subimage stripe
						int x0 = stripeId >= tile.width ? Tile.toScreenX(stripeId, 0) : Tile.toScreenX(0, -stripeId);
						BufferedImage subimage = tile.strips[stripeId];
						g2.drawImage(subimage, x + x0, y - tile.image.getHeight() + tile.heightCorrection, null);
					}
				}
				k--;
				i = toMapOffset(k, j);
			}
		}
		Composite comp = null;
//		Composite comp = g2.getComposite();
//		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, daylight));
//		g2.setColor(Color.BLACK);
//		g2.fill(mainWindow);
//		g2.setComposite(comp);
		
		if (tilesToHighlight != null) {
			drawIntoRect(g2, gfx.getFrame(0), tilesToHighlight);
		}
		g2.setTransform(t);
		// RENDER INFOBARS
		infobarRenderer.renderInfoBars(this, g2);
		// RENDER LEFT BUTTONS
		g2.drawImage(gfx.buildingButton, btnBuilding.rect.x, btnBuilding.rect.y, null);
		g2.setColor(Color.BLACK);
		g2.drawLine(btnBuilding.rect.width, btnBuilding.rect.y, btnBuilding.rect.width, btnBuilding.rect.y + btnBuilding.rect.height - 1);
		
		g2.drawImage(gfx.leftTop, leftTopRect.x, leftTopRect.y, null);
		if (leftFillerRect.height > 0) {
			Paint p = g2.getPaint();
			g2.setPaint(leftFillerPaint);
			g2.fill(leftFillerRect);
			g2.setPaint(p);
		}
		g2.drawImage(gfx.leftBottom, leftBottomRect.x, leftBottomRect.y, null);
		g2.drawLine(btnRadar.rect.width, btnRadar.rect.y, btnRadar.rect.width, 
				btnRadar.rect.y + btnRadar.rect.height - 1);
		g2.drawImage(gfx.radarButton, btnRadar.rect.x, btnRadar.rect.y, null);
		
		// RENDER RIGHT BUTTONS
		g2.drawImage(gfx.buildingInfoButton, btnBuildingInfo.rect.x, btnBuildingInfo.rect.y, null);
		g2.drawImage(gfx.rightTop, rightTopRect.x, rightTopRect.y, null);
		if (rightFillerRect.height > 0) {
			Paint p = g2.getPaint();
			g2.setPaint(rightFillerPaint);
			g2.fill(rightFillerRect);
			g2.setPaint(p);
		}
		g2.drawImage(gfx.rightBottom, rightBottomRect.x, rightBottomRect.y, null);
		g2.drawImage(gfx.screenButtons, btnButtons.rect.x, btnButtons.rect.y, null);
		
		if (btnColonyInfo.visible) {
			if (btnColonyInfo.down) {
				g2.drawImage(gfx.colonyInfoButtonDown, btnColonyInfo.rect.x, btnColonyInfo.rect.y, null);
			} else {
				g2.drawImage(gfx.colonyInfoButton, btnColonyInfo.rect.x, btnColonyInfo.rect.y, null);
			}
		}
		if (btnPlanet.visible) {
			if (btnPlanet.down) {
				g2.drawImage(gfx.planetButtonDown, btnPlanet.rect.x, btnPlanet.rect.y, null);
			} else {
				g2.drawImage(gfx.planetButton, btnPlanet.rect.x, btnPlanet.rect.y, null);
			}
		}
		if (btnStarmap.visible) {
			if (btnStarmap.down) {
				g2.drawImage(gfx.starmapButtonDown, btnStarmap.rect.x, btnStarmap.rect.y, null);
			} else {
				g2.drawImage(gfx.starmapButton, btnStarmap.rect.x, btnStarmap.rect.y, null);
			}
		}
		if (btnBridge.visible) {
			if (btnBridge.down) {
				g2.drawImage(gfx.bridgeButtonDown, btnBridge.rect.x, btnBridge.rect.y, null);
			} else {
				g2.drawImage(gfx.bridgeButton, btnBridge.rect.x, btnBridge.rect.y, null);
			}
		}
		if (btnBuilding.down) {
			renderBuildings(g2);
		}
		if (btnBuildingInfo.down) {
			g2.drawImage(gfx.buildingInfoPanel, buildingInfoPanelRect.x, buildingInfoPanelRect.y, null);
		}
		if (btnRadar.down) {
			g2.drawImage(gfx.radarPanel, radarPanelRect.x, radarPanelRect.y, null);
		}
		Shape sp = g2.getClip();
		g2.clip(infobarRenderer.topInfoArea);
		text.paintTo(g2, infobarRenderer.topInfoArea.x, infobarRenderer.topInfoArea.y + 1, 14, 0xFFFFFFFF, 
				planet != null ? planet.name : "");
		g2.setClip(sp);
		
		// now darken the entire screen
		if (darkness > 0.0f) {
			comp = g2.getComposite();
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, darkness));
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, w, h);
			g2.setComposite(comp);
		}
		achievementRenderer.renderAchievements(g2, this);
	}
	/**
	 * Returns the currently selected planet's surface base.
	 * @return the map bytes
	 */
	private byte[] getSelectedPlanetSurface() {
		byte[] mapBytes;
		PACEntry e = getSurface(gameWorld.player.selectedPlanet.surfaceType.index, gameWorld.player.selectedPlanet.surfaceVariant);
		if (e != null) {
			mapBytes = e.data;
		} else {
			mapBytes = EMPTY_SURFACE_MAP;
		}
		return mapBytes;
	}
	/** Initialize buttons. */
	private void initButtons() {
		btnPlanet = new Btn(new BtnAction() { public void invoke() { doPlanetClick(); } });
		releaseButtons.add(btnPlanet);
		btnColonyInfo = new Btn(new BtnAction() { public void invoke() { doColonyInfoClick(); } });
		releaseButtons.add(btnColonyInfo);
		btnStarmap = new Btn(new BtnAction() { public void invoke() { doStarmapRecClick(); } });
		releaseButtons.add(btnStarmap);
		btnBridge = new Btn(new BtnAction() { public void invoke() { doBridgeClick(); } });
		releaseButtons.add(btnBridge);
		
		btnBuilding = new Btn(new BtnAction() { public void invoke() { doBuildingClick(); } });
		toggleButtons.add(btnBuilding);
		btnRadar = new Btn(new BtnAction() { public void invoke() { doRadarClick(); } });
		toggleButtons.add(btnRadar);
		btnBuildingInfo = new Btn(new BtnAction() { public void invoke() { doBuildingInfoClick(); } });
		toggleButtons.add(btnBuildingInfo);
		btnButtons = new Btn(new BtnAction() { public void invoke() { doScreenClick(); } });
		toggleButtons.add(btnButtons);
		
		btnBuildNext = new Btn(new BtnAction() { public void invoke() { doBuildNext(); } });
		pressButtons.add(btnBuildNext);
		btnBuildPrev = new Btn(new BtnAction() { public void invoke() { doBuildPrev(); } });
		pressButtons.add(btnBuildPrev);
		btnBuild = new Btn(new BtnAction() { public void invoke() { doBuild(); } });
		toggleButtons.add(btnBuild);
		btnList = new Btn(new BtnAction() { public void invoke() { doList(); } });
		releaseButtons.add(btnList);

		btnBuilding.down = true;
		btnRadar.down = true;
		btnBuildingInfo.down = true;
		btnButtons.down = true;
	}
	/**
	 * Perform action on list button click.
	 */
	protected void doList() {
		uiSound.playSound("Buildings");
		if (getOnListClicked() != null) {
			getOnListClicked().invoke();
		}
		cancelBuildMode();
	}
	/**
	 * Perform action on the build button click.
	 */
	protected void doBuild() {
		GameBuildingPrototype bp = gameWorld.player.selectedBuildingPrototype;
		if (bp == null) {
			return;
		}
		BuildingImages bi = bp.images.get(getTechId());
		if (bi == null) {
			return;
		}
		buildMode = !buildMode;
		// if build mode, resize the selection rectangle
		if (buildMode) {
			tilesToHighlight = new Rectangle(0, 0, bi.regularTile.height + 2, bi.regularTile.width + 2);
		} else {
			cancelBuildMode();
		}
	}
	/**
	 * Perform action on the previous button click.
	 */
	protected void doBuildPrev() {
		List<GameBuildingPrototype> list = getBuildingList();
		GameBuildingPrototype bp = gameWorld.player.selectedBuildingPrototype;
		int idx = Math.max(0, list.indexOf(bp) - 1);
		if (idx < list.size()) {
			gameWorld.player.selectedBuildingPrototype = list.get(idx);
		}
		repaint(buildPanelRect);
		buildScroller.setActionCommand("SCROLL-UP");
		if (!buildScroller.isRunning()) {
			buildScroller.start();
		}
		cancelBuildMode();
	}
	/**
	 * Cancel the build mode.
	 */
	private void cancelBuildMode() {
		buildMode = false;
		tilesToHighlight = null;
		btnBuild.down = false;
		repaint(btnBuild.rect);
	}
	/**
	 * @return a list of buildings for the selected planet's race or the player's race.
	 */
	private List<GameBuildingPrototype> getBuildingList() {
		String techId = getTechId();
		List<GameBuildingPrototype> list = gameWorld.getTechIdBuildingPrototypes(techId);
		return list;
	}
	/**
	 * @return the technology id for the current planet buildings
	 */
	private String getTechId() {
		String techId;
		if (gameWorld.player.selectedPlanet != null && gameWorld.player.selectedPlanet.populationRace != null) {
			techId = gameWorld.player.selectedPlanet.populationRace.techId; 
		} else {
			techId = gameWorld.player.race.techId;
		}
		return techId;
	}
	/**
	 * Perform action on the building next button click.
	 */
	protected void doBuildNext() {
		List<GameBuildingPrototype> list = getBuildingList();
		GameBuildingPrototype bp = gameWorld.player.selectedBuildingPrototype;
		int idx = Math.min(list.size(), list.indexOf(bp) + 1);
		if (idx < list.size()) {
			gameWorld.player.selectedBuildingPrototype = list.get(idx);
		}
		repaint(buildPanelRect);
		buildScroller.setActionCommand("SCROLL-DOWN");
		if (!buildScroller.isRunning()) {
			buildScroller.start();
		}
		cancelBuildMode();
	}
	/**
	 * Update location of various interresting rectangles of objects.
	 */
	private void updateRegions() {
		
		infobarRenderer.updateRegions(this);
		
		btnBuilding.rect.x = 0;
		btnBuilding.rect.y = cgfx.top.left.getHeight();
		btnBuilding.rect.width = gfx.buildingButton.getWidth();
		btnBuilding.rect.height = gfx.buildingButton.getHeight();
		
		leftTopRect.x = 0;
		leftTopRect.y = btnBuilding.rect.y + btnBuilding.rect.height;
		leftTopRect.width = gfx.leftTop.getWidth();
		leftTopRect.height = gfx.leftTop.getHeight();
		
		btnRadar.rect.x = 0;
		btnRadar.rect.y = getHeight() - cgfx.bottom.left.getHeight() - gfx.radarButton.getHeight();
		btnRadar.rect.width = gfx.radarButton.getWidth();
		btnRadar.rect.height = gfx.radarButton.getHeight();
		
		leftBottomRect.x = 0;
		leftBottomRect.y = btnRadar.rect.y - gfx.leftBottom.getHeight();
		leftBottomRect.width = gfx.leftBottom.getWidth();
		leftBottomRect.height = gfx.leftBottom.getHeight();
		
		leftFillerRect.x = 0;
		leftFillerRect.y = leftTopRect.y + leftTopRect.height;
		leftFillerRect.width = gfx.leftFiller.getWidth();
		leftFillerRect.height = leftBottomRect.y - leftFillerRect.y;
		if (leftFillerPaint == null) {
			leftFillerPaint = new TexturePaint(gfx.leftFiller, leftFillerRect);
		}
		
		btnBuildingInfo.rect.x = getWidth() - gfx.buildingInfoButton.getWidth();
		btnBuildingInfo.rect.y = cgfx.top.left.getHeight();
		btnBuildingInfo.rect.width = gfx.buildingInfoButton.getWidth();
		btnBuildingInfo.rect.height = gfx.buildingInfoButton.getHeight();
		
		rightTopRect.x = btnBuildingInfo.rect.x;
		rightTopRect.y = btnBuildingInfo.rect.y + btnBuildingInfo.rect.height;
		rightTopRect.width = gfx.rightTop.getWidth();
		rightTopRect.height = gfx.rightTop.getHeight();
		
		btnButtons.rect.x = btnBuildingInfo.rect.x;
		btnButtons.rect.y = getHeight() - cgfx.bottom.left.getHeight() - gfx.screenButtons.getHeight();
		btnButtons.rect.width = gfx.screenButtons.getWidth();
		btnButtons.rect.height = gfx.screenButtons.getHeight();
		
		rightBottomRect.x = btnBuildingInfo.rect.x;
		rightBottomRect.y = btnButtons.rect.y - gfx.rightBottom.getHeight();
		rightBottomRect.width = gfx.rightBottom.getWidth();
		rightBottomRect.height = gfx.rightBottom.getHeight();
		
		rightFillerRect.x = btnBuildingInfo.rect.x;
		rightFillerRect.y = rightTopRect.y + gfx.rightTop.getHeight();
		rightFillerRect.width = gfx.rightFiller.getWidth();
		rightFillerRect.height = rightBottomRect.y - rightFillerRect.y;
		
		rightFillerPaint = new TexturePaint(gfx.rightFiller, rightFillerRect);
		
		// BOTTOM RIGHT CONTROL BUTTONS
		
		btnBridge.rect.x = getWidth() - gfx.rightBottom.getWidth() - gfx.bridgeButton.getWidth();
		btnBridge.rect.y = getHeight() - cgfx.bottom.right.getHeight() - gfx.bridgeButton.getHeight();
		btnBridge.rect.width = gfx.bridgeButton.getWidth();
		btnBridge.rect.height = gfx.bridgeButton.getHeight();
		
		btnStarmap.rect.x = btnBridge.rect.x - gfx.starmapButton.getWidth();
		btnStarmap.rect.y = btnBridge.rect.y;
		btnStarmap.rect.width = gfx.starmapButton.getWidth();
		btnStarmap.rect.height = gfx.starmapButton.getHeight();
		
		btnPlanet.rect.x = btnStarmap.rect.x - gfx.planetButton.getWidth();
		btnPlanet.rect.y = btnBridge.rect.y;
		btnPlanet.rect.width = gfx.planetButton.getWidth();
		btnPlanet.rect.height = gfx.planetButton.getHeight();

		btnColonyInfo.rect.x = btnPlanet.rect.x - gfx.colonyInfoButton.getWidth();
		btnColonyInfo.rect.y = btnBridge.rect.y;
		btnColonyInfo.rect.width = gfx.colonyInfoButton.getWidth();
		btnColonyInfo.rect.height = gfx.colonyInfoButton.getHeight();
		
		mainWindow.x = btnBuilding.rect.width + 1;
		mainWindow.y = btnBuilding.rect.y;
		mainWindow.width = btnBuildingInfo.rect.x - mainWindow.x;
		mainWindow.height = btnRadar.rect.y + btnRadar.rect.height - mainWindow.y;
		
		buildPanelRect.x = mainWindow.x - 1;
		buildPanelRect.y = mainWindow.y;
		buildPanelRect.width = gfx.buildPanel.getWidth();
		buildPanelRect.height = gfx.buildPanel.getHeight();
		
		buildingInfoPanelRect.x = mainWindow.x + mainWindow.width - gfx.buildingInfoPanel.getWidth();
		buildingInfoPanelRect.y = mainWindow.y;
		buildingInfoPanelRect.width = gfx.buildingInfoPanel.getWidth();
		buildingInfoPanelRect.height = gfx.buildingInfoPanel.getHeight();
		
		radarPanelRect.x = buildPanelRect.x;
		radarPanelRect.y = mainWindow.y + mainWindow.height - gfx.radarPanel.getHeight();
		radarPanelRect.width = gfx.radarPanel.getWidth();
		radarPanelRect.height = gfx.radarPanel.getHeight();
		
		buildImageRect.setBounds(26, buildPanelRect.y + 7, 140, 103);
		buildNameRect.setBounds(27, buildPanelRect.y + 117, 166, 18);
		btnList.setBounds(26, buildPanelRect.y + 142, 81, 21);
		btnBuild.setBounds(113, buildPanelRect.y + 142, 81, 21);
		btnBuildPrev.setBounds(172, buildPanelRect.y + 7, 22, 48);
		btnBuildNext.setBounds(172, buildPanelRect.y + 62, 22, 48);
	}
	/**
	 * Converts the tile x and y coordinates to map offset.
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 * @return the map offset
	 */
	public int toMapOffset(int x, int y) {
		return (x - y) * 65 + (x - y + 1) / 2 - x;
	}
	/**
	 * Fills the given rectangular tile area with the specified tile image.
	 * @param g2 the graphics object
	 * @param image the image to draw
	 * @param rect the target rectangle
	 */
	private void drawIntoRect(Graphics2D g2, BufferedImage image, Rectangle rect) {
		for (int j = rect.y; j >= rect.y - rect.height + 1; j--) {
			for (int k = rect.x; k < rect.x + rect.width; k++) {
				int x = xoff + Tile.toScreenX(k, j); 
				int y = yoff + Tile.toScreenY(k, j); 
				g2.drawImage(image, x - 1, y - image.getHeight(), null);
			}
		}
	}
	/** Light value change. */
	boolean lightMode;
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseDragged(MouseEvent e) {
		if (lightMode) {
			// adjust daylight value based on the vertical mouse position
			daylight = e.getY() / (float)getHeight();
			repaint();
		} else
		if (panMode) {
			xoff -= (lastx - e.getX());
			yoff -= (lasty - e.getY());
			lastx = e.getX();
			lasty = e.getY();
			repaint();
		}
	}
	/**
	 * Returns true if the mouse event is within the
	 * visible area of the main window (e.g not over
	 * the panels or buttons).
	 * @param e the mouse event
	 * @return true if the event was on the surface
	 */
	private boolean eventInMainWindow(MouseEvent e) {
		Point pt = e.getPoint();
		return mainWindow.contains(pt) 
		&& (!btnBuilding.down || !buildPanelRect.contains(pt))
		&& (!btnRadar.down || !radarPanelRect.contains(pt))
		&& (!btnBuildingInfo.down || !buildingInfoPanelRect.contains(pt))
		&& (!btnColonyInfo.test(pt)
				&& !btnPlanet.test(pt)
				&& !btnStarmap.test(pt)
				&& !btnBridge.test(pt)
		);
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseMoved(MouseEvent e) {
		if (eventInMainWindow(e)) {
			if (tilesToHighlight != null) {
				int x = e.getX() - xoff - 27;
				int y = e.getY() - yoff + 1;
				int a = (int)Math.floor(Tile.toTileX(x, y));
				int b = (int)Math.floor(Tile.toTileY(x, y));
				tilesToHighlight.x = a;
				tilesToHighlight.y = b;
				repaint();
			}
		}
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		Point pt = e.getPoint(); 
		if (e.getButton() == MouseEvent.BUTTON3 && eventInMainWindow(e)) {
			lastx = e.getX();
			lasty = e.getY();
			panMode = true;
		} else
		if (e.getButton() == MouseEvent.BUTTON2 && eventInMainWindow(e)) {
			daylight = e.getY() / (float)getHeight();
			lightMode = true;
			repaint();
		} else
		if (e.getButton() == MouseEvent.BUTTON1) {
			if (eventInMainWindow(e)) {
				doMainWindowClick(e);
			} else {
				for (Btn b : pressButtons) {
					if (b.test(pt)) {
						b.down = true;
						repaint(b.rect);
						b.click();
					}
				}
				for (Btn b : releaseButtons) {
					if (b.test(pt)) {
						b.down = true;
						repaint(b.rect);
					}
				}
				for (Btn b : toggleButtons) {
					if (b.test(pt)) {
						b.down = !b.down;
						b.click();
						repaint(b.rect);
					}
				}
			}
		}
	}
	/**
	 * Do action when the user clicks on the maon window.
	 * @param e the mouse event
	 */
	private void doMainWindowClick(MouseEvent e) {
		if (!buildMode) {
			int x = e.getX() - xoff - 27;
			int y = e.getY() - yoff + 1;
			int a = (int)Math.floor(Tile.toTileX(x, y));
			int b = (int)Math.floor(Tile.toTileY(x, y));
			int offs = this.toMapOffset(a, b);
			int val = offs >= 0 && offs < 65 * 65 ? getSelectedPlanetSurface()[offs * 2 + 4] & 0xFF : 0;
			System.out.printf("%d, %d -> %d, %d%n", a, b, offs, val);
		} else {
			// TODO test placeability first!
			// place the selected building onto the planet
			GamePlanet planet = gameWorld.player.selectedPlanet;
			GameBuildingPrototype bp = gameWorld.player.selectedBuildingPrototype;
			String techid = getTechId();
			BuildingImages bi = bp.images.get(techid);
			
			GameBuilding b = new GameBuilding();
			b.prototype = bp;
			b.images = bi;
			b.progress = 100;
			b.health = 100;
			b.x = tilesToHighlight.x + 1;
			b.y = tilesToHighlight.y - 1;
			
			// place roads around the base
			Map<RoadType, Tile> rts = gfx.roadTiles.get(techid);
			TileFragment tf = TileFragment.of(0, rts.get(RoadType.RIGHT_TO_BOTTOM));
			planet.map.put(Location.of(tilesToHighlight.x, tilesToHighlight.y), tf);
			tf = TileFragment.of(0, rts.get(RoadType.LEFT_TO_BOTTOM));
			planet.map.put(Location.of(tilesToHighlight.x + tilesToHighlight.width - 1, tilesToHighlight.y), tf);
			tf = TileFragment.of(0, rts.get(RoadType.TOP_TO_RIGHT));
			planet.map.put(Location.of(tilesToHighlight.x, tilesToHighlight.y - tilesToHighlight.height + 1), tf);
			tf = TileFragment.of(0, rts.get(RoadType.TOP_TO_LEFT));
			planet.map.put(Location.of(tilesToHighlight.x + tilesToHighlight.width - 1, tilesToHighlight.y - tilesToHighlight.height + 1), tf);
			// add linear segments
			Tile ht = rts.get(RoadType.HORIZONTAL);
			for (int i = tilesToHighlight.x + 1; i < tilesToHighlight.x + tilesToHighlight.width - 1; i++) {
				tf = TileFragment.of(0, ht);
				planet.map.put(Location.of(i, tilesToHighlight.y), tf);
				tf = TileFragment.of(0, ht);
				planet.map.put(Location.of(i, tilesToHighlight.y - tilesToHighlight.height + 1), tf);
			}
			Tile vt = rts.get(RoadType.VERTICAL);
			for (int i = tilesToHighlight.y - 1; i > tilesToHighlight.y - tilesToHighlight.height + 1; i--) {
				tf = TileFragment.of(0, vt);
				planet.map.put(Location.of(tilesToHighlight.x, i), tf);
				tf = TileFragment.of(0, vt);
				planet.map.put(Location.of(tilesToHighlight.x + tilesToHighlight.width - 1, i), tf);
			}
			// place the actual building fragments
			int fragment = 0;
			for (int i = tilesToHighlight.y - 1; i >= tilesToHighlight.y - tilesToHighlight.height + 2; i--) {
				tf = TileFragment.of(fragment, b);
				planet.map.put(Location.of(tilesToHighlight.x + 1, i), tf);
				fragment++;
			}
			for (int i = tilesToHighlight.x + 2; i < tilesToHighlight.x + tilesToHighlight.width - 1; i++) {
				tf = TileFragment.of(fragment, b);
				planet.map.put(Location.of(i, tilesToHighlight.y - tilesToHighlight.height + 2), tf);
				fragment++;
			}
			planet.addBuilding(b);
			repaint(mainWindow);
			// if not multiple placement
			if (!e.isShiftDown()) {
				cancelBuildMode();
			}
		}
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		Point pt = e.getPoint();
		if (e.getButton() == MouseEvent.BUTTON3) {
			panMode = false;
		} else
		if (e.getButton() == MouseEvent.BUTTON2) {
			lightMode = false;
		} else
		if (e.getButton() == MouseEvent.BUTTON1) {
			boolean needRepaint = buildScroller.isRunning();
			buildScroller.stop();
			for (Btn b : pressButtons) {
				needRepaint |= b.down;
				b.down = false;
			}
			for (Btn b : releaseButtons) {
				needRepaint |= b.down;
				b.down = false;
				if (b.test(pt)) {
					b.click();
				}
			}
			if (needRepaint) {
				repaint();
			}
		}
	}
	/** Execute once flag. */
	boolean once = true;
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
//		if (!e.isControlDown() && !e.isAltDown()) {
//			if (e.getWheelRotation() > 0 & surfaceVariant < 9) {
//				surfaceVariant++;
//			} else 
//			if (e.getWheelRotation() < 0 && surfaceVariant > 1) {
//				surfaceVariant--;
//			}
//			changeSurface();
//		} else 
//		if (e.isControlDown()) {
//			if (e.getWheelRotation() < 0 & scale < 32) {
//				scale *= 2;
//			} else 
//			if (e.getWheelRotation() > 0 && scale > 1f / 32) {
//				scale /= 2;
//			}
//		} else
//		if (e.isAltDown()) {
//			if (e.getWheelRotation() < 0 && surfaceType > 1) {
//				surfaceType--;
//			} else
//			if (e.getWheelRotation() > 0 && surfaceType < 7) {
//				surfaceType++;
//			}
//			changeSurface();
//		}
//		repaint();
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		// no op
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseEntered(MouseEvent e) {
		
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mouseExited(MouseEvent e) {
		
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if ("FADE".equals(e.getActionCommand())) {
			doFade();
		} else
		if ("SCROLL-UP".equals(e.getActionCommand())) {
			doBuildScroller(false);
		} else
		if ("SCROLL-DOWN".equals(e.getActionCommand())) {
			doBuildScroller(true);
		}
	}
	/** Execute the fade animation. */
	private void doFade() {
		if (!fadeDirection) {
			darkness = Math.max(0.0f, Math.min(1.0f, darkness + ALPHA_DELTA));
			if (darkness >= 0.999f) {
				fadeTimer.stop();
				doFadeCompleted();
			}
		} else {
			darkness = Math.max(0.0f, Math.min(1.0f, darkness - ALPHA_DELTA));
			if (darkness <= 0.001f) {
				fadeTimer.stop();
				doFadeCompleted();
			}
		}
		repaint();
	}
	/**
	 * Invoked when the fading operation is completed.
	 */
	private void doFadeCompleted() {
		if (onStarmapClicked != null) {
			onStarmapClicked.invoke();
		}
		darkness = 0f;
	}
	/** 
	 * Action for build list scrolling.
	 * @param direction true: down, false: up 
	 */
	private void doBuildScroller(boolean direction) {
		if (direction) {
			doBuildNext();
		} else {
			doBuildPrev();
		}
	}
	/** Perform action on bridge button click. */
	protected void doBridgeClick() {
		uiSound.playSound("Bridge");
		if (onBridgeClicked != null) {
			onBridgeClicked.invoke();
		}
	}
	/** Perform action on starmap button click. */
	protected void doStarmapRecClick() {
		uiSound.playSound("Starmap");
		fadeDirection = false;
		fadeTimer.start();
	}
	/** Perform colony button click. */
	protected void doColonyInfoClick() {
		uiSound.playSound("ColonyInformation");
		if (onInformationClicked != null) {
			onInformationClicked.invoke();
		}
	}
	/** Do planet click. */
	protected void doPlanetClick() {
		uiSound.playSound("Planets");
		if (onPlanetsClicked != null) {
			onPlanetsClicked.invoke();
		}
	}
	/** Do Screen buttons click. */
	protected void doScreenClick() {
		btnColonyInfo.visible = btnButtons.down;
		btnPlanet.visible = btnButtons.down;
		btnStarmap.visible = btnButtons.down;
		btnBridge.visible = btnButtons.down;
		repaint();
	}
	/** Do building info button click. */
	protected void doBuildingInfoClick() {
		repaint(buildingInfoPanelRect);
	}
	/** Do radar button click. */
	protected void doRadarClick() {
		repaint(radarPanelRect);
	}
	/** Do building button click. */
	protected void doBuildingClick() {
		repaint(buildPanelRect);
	}
	/**
	 * Sets the onStarmapClicked action.
	 * @param onStarmapClicked the onStarmapClick to set
	 */
	public void setOnStarmapClicked(BtnAction onStarmapClicked) {
		this.onStarmapClicked = onStarmapClicked;
	}
	/**
	 * @return the onStarmapClick
	 */
	public BtnAction getOnStarmapClicked() {
		return onStarmapClicked;
	}
	/**
	 * @param onInformationClicked the onInformationClick to set
	 */
	public void setOnInformationClicked(BtnAction onInformationClicked) {
		this.onInformationClicked = onInformationClicked;
	}
	/**
	 * @return the onInformationClick
	 */
	public BtnAction getOnInformationClicked() {
		return onInformationClicked;
	}
	/**
	 * @param onBridgeClicked the onBridgeClicked to set
	 */
	public void setOnBridgeClicked(BtnAction onBridgeClicked) {
		this.onBridgeClicked = onBridgeClicked;
	}
	/**
	 * @return the onBridgeClicked
	 */
	public BtnAction getOnBridgeClicked() {
		return onBridgeClicked;
	}
	/**
	 * @param onPlanetsClicked the onPlanetsClicked to set
	 */
	public void setOnPlanetsClicked(BtnAction onPlanetsClicked) {
		this.onPlanetsClicked = onPlanetsClicked;
	}
	/**
	 * @return the onPlanetsClicked
	 */
	public BtnAction getOnPlanetsClicked() {
		return onPlanetsClicked;
	}
	/**
	 * @param gameWorld the gameWorld to set
	 */
	public void setGameWorld(GameWorld gameWorld) {
		this.gameWorld = gameWorld;
	}
	/**
	 * @return the gameWorld
	 */
	public GameWorld getGameWorld() {
		return gameWorld;
	}
	/**
	 * Render buildings panel.
	 * @param g2 the graphics object
	 */
	private void renderBuildings(Graphics2D g2) {
		Shape sp = g2.getClip();
		g2.drawImage(gfx.buildPanel, buildPanelRect.x, buildPanelRect.y, null);
		List<GameBuildingPrototype> list = getBuildingList();
		GameBuildingPrototype bp = gameWorld.player.selectedBuildingPrototype;
		int idx = list.indexOf(bp);
		if (idx < 0 && list.size() > 0) {
			idx = 0;
			gameWorld.player.selectedBuildingPrototype = list.get(0);
			bp = list.get(0);
		}
		if (bp != null) {
			BuildingImages bi = bp.images.get(getTechId());
			if (bp != null && bi != null) {
				g2.setClip(buildImageRect);
				g2.drawImage(bi.thumbnail, buildImageRect.x + (buildImageRect.width - bi.thumbnail.getWidth()) / 2,
						buildImageRect.y + (buildImageRect.height - bi.thumbnail.getHeight()) / 2,  null);
				
				if (!gameWorld.isBuildableOnPlanet(bp)) {
					g2.setColor(Color.RED);
					g2.drawLine(buildImageRect.x, buildImageRect.y, 
							buildImageRect.x + buildImageRect.width - 1, 
							buildImageRect.y + buildImageRect.height - 1
					);
					g2.drawLine(buildImageRect.x + buildImageRect.width - 1, buildImageRect.y, 
							buildImageRect.x, 
							buildImageRect.y + buildImageRect.height - 1 
					);
				}
				
				String costStr = bp.cost + " " + gameWorld.getLabel("BuildingInfo.ProductionUnitFor.credit");
				int len0 = text.getTextWidth(7, costStr);
				text.paintTo(g2, buildImageRect.x + (buildImageRect.width - len0) - 2,
						buildImageRect.y + (buildImageRect.height - 10),
						7, TextGFX.YELLOW, costStr);
				
				g2.setClip(buildNameRect);
				int len = text.getTextWidth(10, bp.name);
				text.paintTo(g2, buildNameRect.x + (buildNameRect.width - len) / 2,
						buildNameRect.y + (buildNameRect.height - 10) / 2,
						10, TextGFX.YELLOW, bp.name);
			}
		}
		g2.setClip(sp);
		if (idx <= 0) {
			g2.drawImage(gfx.buildScrollNone, btnBuildPrev.rect.x, btnBuildPrev.rect.y, null);
		} else
		if (btnBuildPrev.down) {
			g2.drawImage(gfx.buildScrollUpDown, btnBuildPrev.rect.x, btnBuildPrev.rect.y, null);
		}
		if (idx >= list.size() - 1) {
			g2.drawImage(gfx.buildScrollNone, btnBuildNext.rect.x, btnBuildNext.rect.y, null);
		} else
		if (btnBuildNext.down) {
			g2.drawImage(gfx.buildScrollDownDown, btnBuildNext.rect.x, btnBuildNext.rect.y, null);
		}
		if (btnList.down) {
			g2.drawImage(gfx.listDown, btnList.rect.x, btnList.rect.y, null);
		}
		if (btnBuild.down) {
			g2.drawImage(gfx.buildDown, btnBuild.rect.x, btnBuild.rect.y, null);
		}
		g2.setClip(sp);
	}
	/**
	 * @param onListClicked the onListClicked to set
	 */
	public void setOnListClicked(BtnAction onListClicked) {
		this.onListClicked = onListClicked;
	}
	/**
	 * @return the onListClicked
	 */
	public BtnAction getOnListClicked() {
		return onListClicked;
	}
}
