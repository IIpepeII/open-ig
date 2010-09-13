/*
 * Copyright 2008-2011, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */
package hu.openig.model;

import hu.openig.core.RoadType;
import hu.openig.utils.XML;
import hu.openig.core.ResourceLocator;
import hu.openig.core.ResourceType;
import hu.openig.core.Tile;
import hu.openig.core.ResourceLocator.ResourcePlace;
import hu.openig.model.BuildingType.Resource;
import hu.openig.model.BuildingType.TileSet;
import hu.openig.model.BuildingType.Upgrade;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

/**
 * The building models.
 * @author karnokd
 */
public class BuildingModel {
	/** The list of all building types. Maps from building ID to building type definition. */
	public final Map<String, BuildingType> buildings = new LinkedHashMap<String, BuildingType>();
	/** The road tile map from tech id to road type to tile. */
	public final Map<String, Map<RoadType, Tile>> roadTiles = new HashMap<String, Map<RoadType, Tile>>();
	/** The road tile to road type reverse lookup table. */
	public final Map<String, Map<Tile, RoadType>> tileRoads = new HashMap<String, Map<Tile, RoadType>>();
	/**
	 * Process the contents of the buildings definition.
	 * @param data the buildings definition
	 * @param rl the resource locator
	 * @param language the language
	 */
	public void processBuildings(ResourceLocator rl, String language, String data) {
		Element buildings = rl.getXML(language, data);
		for (Element building : XML.childrenWithName(buildings, "building")) {
			BuildingType b = new BuildingType();
			
			b.id = building.getAttribute("id");
			b.label = building.getAttribute("label");
			b.description = b.label + ".desc";
			
			Element gfx = XML.childElement(building, "graphics");
			String pattern = gfx.getAttribute("base");
			for (Element r : XML.childrenWithName(gfx, "tech")) {
				TileSet ts = new TileSet();
				
				String rid = r.getAttribute("id");
				int width = Integer.parseInt(r.getAttribute("width"));
				int height = Integer.parseInt(r.getAttribute("height"));
				
				String normalImg = String.format(pattern, rid);
				String normalLight = normalImg + "_lights";
				String damagedImg = normalImg + "_damaged";
				
				BufferedImage lightMap = null;
				ResourcePlace rp = rl.get(language, normalLight, ResourceType.IMAGE);
				if (rp != null) {
					lightMap = rl.getImage(language, normalLight);
				}
				ts.normal = new Tile(width, height, rl.getImage(language, normalImg), lightMap);
				ts.damaged = new Tile(width, height, rl.getImage(language, damagedImg), null); // no lightmap for damaged building
				b.tileset.put(rid, ts);
			}
			Element bld = XML.childElement(building, "build");
			b.cost = Integer.parseInt(bld.getAttribute("cost"));
			b.kind = bld.getAttribute("kind");
			String limit = bld.getAttribute("limit");
			if ("*".equals(limit)) {
				b.limit = Integer.MAX_VALUE;
			} else {
				b.limit = Integer.parseInt(limit);
			}
			b.research = bld.getAttribute("research");
			String except = bld.getAttribute("except");
			if (except != null && !except.isEmpty()) {
				b.except.addAll(Arrays.asList(except.split("\\s*,\\s*")));
			}
			Element op = XML.childElement(building, "operation");
			b.percentable = "true".equals(op.getAttribute("percent"));
			for (Element re : XML.childrenWithName(op, "resource")) {
				Resource res = new Resource();
				res.type = re.getAttribute("type");
				res.amount = Float.parseFloat(re.getTextContent());
				b.resources.put(res.type, res);
				if ("true".equals(re.getAttribute("display"))) {
					b.primary = res;
				}
			}
			
			Element ug = XML.childElement(building , "upgrades");
			for (Element u : XML.childrenWithName(ug, "upgrade")) {
				Upgrade upg = new Upgrade();
				upg.description = u.getAttribute("desc");
				for (Element re : XML.childrenWithName(op, "resource")) {
					Resource res = new Resource();
					res.type = re.getAttribute("type");
					res.amount = Float.parseFloat(re.getTextContent());
					upg.resources.put(res.type, res);
				}
				b.upgrades.add(upg);
			}
			
			this.buildings.put(b.id, b);
		}
		Element roads = XML.childElement(buildings, "roads");
		Element graph = XML.childElement(roads, "graphics");
		String roadBase = graph.getAttribute("base");
		List<String> techs = new ArrayList<String>();
		for (Element e : XML.childrenWithName(graph, "tech")) {
			techs.add(e.getAttribute("id"));
		}
		for (Element e : XML.childrenWithName(roads, "layout")) {
			int index = Integer.parseInt(e.getAttribute("index"));
			String id = e.getAttribute("id");
			for (String rid : techs) {
				String normalImg = String.format(roadBase, rid, id);
				String normalLight = normalImg + "_lights";
				
				BufferedImage lightMap = null;
				ResourcePlace rp = rl.get(language, normalLight, ResourceType.IMAGE);
				if (rp != null) {
					lightMap = rl.getImage(language, normalLight);
				}
				Tile t = new Tile(1, 1, rl.getImage(language, normalImg), lightMap);
				RoadType rt = RoadType.getByIndex(index);
				addRoadType(rid, rt, t);
			}
		}
	}
	/**
	 * Add a road type - tile entry.
	 * @param rid the race id
	 * @param rt the road type
	 * @param tile the tile entry
	 */
	void addRoadType(String rid, RoadType rt, Tile tile) {
		Map<RoadType, Tile> tiles = roadTiles.get(rid);
		if (tiles == null) {
			tiles = new HashMap<RoadType, Tile>();
			roadTiles.put(rid, tiles);
		}
		tiles.put(rt, tile);
		
		Map<Tile, RoadType> roads = tileRoads.get(rid);
		if (roads == null) {
			roads = new HashMap<Tile, RoadType>();
			tileRoads.put(rid, roads);
		}
		roads.put(tile, rt);
	}
}
