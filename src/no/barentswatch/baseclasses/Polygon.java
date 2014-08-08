package no.barentswatch.baseclasses;

import java.util.LinkedList;
import java.util.List;

public class Polygon implements java.io.Serializable {
	List<Point> vertices;
	
	public Polygon() {
		vertices = new LinkedList<Point>();
	}
	
	public Polygon(List<Point> Vertices) {
		vertices = Vertices;
	}
	
	public void setVertices(List<Point> Vertices) {
		for(Point vert : Vertices) {
			vertices.add(vert);
		}
	}
	
	public void addVertex(Point point) {
		vertices.add(point);
	}
	
	public List<Point> getVertices() {
		return vertices;
	}
	
	public void printPolygon() {
		for(Point point : vertices) {
			point.printPointValues();
		}
	}
	
	private static final long serialVersionUID = 9L;
}
