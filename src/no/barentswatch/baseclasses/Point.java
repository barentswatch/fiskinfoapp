package no.barentswatch.baseclasses;

public class Point implements java.io.Serializable{
	public double lat;
	public double lon;
	
	public Point() {
		
	}
	
	public Point(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}
	
	public void setNewPointValues(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}
	
	public void printPointValues() {
		System.out.println("lat= " + this.lat + " lon= " + this.lon);
	}
	
	public boolean checkDistanceBetweenTwoPoints(Point point, double unacceptableDistance) {
		double earthMeanRadius = 6371; //Kilometer
		double phiLatThis = deg2rad(this.lat);
		double phiLatUserPoision = deg2rad(point.lat);
		double deltaPhi = deg2rad(point.lat - this.lat);
		double deltaLambda = deg2rad(point.lon - this.lon);
		
		
		double haversine = (Math.sin((deltaPhi / 2)) * Math.sin((deltaPhi / 2)) ) + Math.cos(phiLatThis) * Math.cos(phiLatUserPoision) * Math.sin((deltaLambda/2)) * Math.sin(deltaLambda / 2);
		double c = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
		
		double distanceInKilometers = earthMeanRadius * c;
		double distanceInMeter = distanceInKilometers * 1000;
		double distanceInNauticalMiles = distanceInMeter / 1852;
		if (distanceInNauticalMiles - unacceptableDistance < 0) {
			return true;
		}
		return false;
		
	}
	
	public double deg2rad(double degree) {
		double conversionFactor = (2 * Math.PI) / 360;
		return (degree * conversionFactor);
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3L;
	
}
