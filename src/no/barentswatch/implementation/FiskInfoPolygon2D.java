package no.barentswatch.implementation;

import java.util.ArrayList;
import java.util.List;

import no.barentswatch.baseclasses.Line;
import no.barentswatch.baseclasses.Point;
import no.barentswatch.baseclasses.Polygon;

public class FiskInfoPolygon2D implements java.io.Serializable {
	List<Line> lines;
	List<Polygon> polygons;
	List<Point> points;
	
	public FiskInfoPolygon2D() {
		this.lines = new ArrayList<Line>();
		this.polygons = new ArrayList<Polygon>();
		this.points = new ArrayList<Point>();
	}

	public void addLine(Line line) {
		lines.add(line);
	}

	public void addPolygon(Polygon polygon) {
		polygons.add(polygon);
	}

	public void addPoint(Point point) {
		points.add(point);
	}

	public List<Line> getLines() {
		return lines;
	}

	public List<Polygon> getPolygons() {
		return polygons;
	}

	public List<Point> getPoints() {
		return points;
	}

	public boolean checkCollsionWithPoint(Point point, double distance) {
		for (Line line : lines) {
			if (line.checkDistanceWithLineAndReportStatus(point, distance)) { 
				return true;
			}
		}
		// Construct lines from polygons so we can do our
		for (Polygon polygon : polygons) {
			List<Point> vertices = polygon.getVertices();
			Point currPoint = vertices.get(0);
			Point prevPoint = null;
			for (int i = 1; i < vertices.size() - 1; i++) {
				prevPoint = currPoint;
				currPoint = vertices.get(i);
				Line currLine = new Line(prevPoint, currPoint);
				if (currLine.checkDistanceWithLineAndReportStatus(point, distance)) {
					return true;
				}
			}
		}
		// Points
		for (Point currentPoint : points) {
			if(currentPoint.checkDistanceBetweenTwoPoints(point, distance)) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Utility functions might/should be removed after debugging
	 */

	public void printPolygon() {
		System.out.println("All lines");
		for (Line line : lines) {
			line.printLine();
		}
		System.out.println("All polygons");
		for (Polygon polygon : polygons) {
			polygon.printPolygon();
		}
		System.out.println("All points!");
		for (Point point : points) {
			point.printPointValues();
		}
	}


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
