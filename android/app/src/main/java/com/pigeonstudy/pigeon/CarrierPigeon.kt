package com.pigeonstudy.pigeon

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object CarrierPigeonService {

    data class Landmark(val name: String, val lat: Double, val lon: Double)
    data class City(val name: String, val landmarks: List<Landmark>)
    data class Flight(val origin: Landmark, val destination: Landmark, val distanceKm: Double, val estimatedSeconds: Int, val actualSeconds: Int)

    val cities = listOf(
        City("Toronto", listOf(
            Landmark("CN Tower", 43.6426, -79.3871), Landmark("Royal Ontario Museum", 43.6677, -79.3948),
            Landmark("Casa Loma", 43.6780, -79.4094), Landmark("Ripley's Aquarium", 43.6424, -79.3861),
            Landmark("Toronto Island", 43.6205, -79.3815), Landmark("High Park", 43.6532, -79.4632),
            Landmark("St. Lawrence Market", 43.6485, -79.3748), Landmark("Distillery District", 43.6503, -79.3596),
            Landmark("Yonge-Dundas Square", 43.6561, -79.3807), Landmark("Kensington Market", 43.6547, -79.3998),
            Landmark("Art Gallery of Ontario", 43.6531, -79.3925), Landmark("Union Station", 43.6453, -79.3807),
        )),
        City("New York", listOf(
            Landmark("Statue of Liberty", 40.6892, -74.0445), Landmark("Empire State Building", 40.7484, -73.9857),
            Landmark("Central Park", 40.7829, -73.9654), Landmark("Times Square", 40.7580, -73.9855),
            Landmark("Brooklyn Bridge", 40.7061, -73.9969), Landmark("Rockefeller Center", 40.7587, -73.9787),
            Landmark("The Met", 40.7794, -73.9632), Landmark("Grand Central", 40.7527, -73.9772),
            Landmark("Wall Street", 40.7069, -74.0113), Landmark("High Line", 40.7480, -74.0048),
            Landmark("Coney Island", 40.5749, -73.9857), Landmark("Yankee Stadium", 40.8296, -73.9262),
        )),
        City("London", listOf(
            Landmark("Big Ben", 51.5007, -0.1246), Landmark("Tower Bridge", 51.5055, -0.0754),
            Landmark("London Eye", 51.5033, -0.1196), Landmark("Buckingham Palace", 51.5014, -0.1419),
            Landmark("British Museum", 51.5194, -0.1270), Landmark("St Paul's Cathedral", 51.5138, -0.0984),
            Landmark("Hyde Park", 51.5073, -0.1657), Landmark("Trafalgar Square", 51.5080, -0.1281),
            Landmark("Shakespeare's Globe", 51.5081, -0.0972), Landmark("Camden Market", 51.5390, -0.1427),
            Landmark("Greenwich Observatory", 51.4769, -0.0005), Landmark("King's Cross", 51.5308, -0.1238),
        )),
        City("Los Angeles", listOf(
            Landmark("Hollywood Sign", 34.1341, -118.3215), Landmark("Griffith Observatory", 34.1184, -118.3004),
            Landmark("Santa Monica Pier", 34.0083, -118.4988), Landmark("Getty Center", 34.0780, -118.4741),
            Landmark("Venice Beach", 33.9850, -118.4695), Landmark("Dodger Stadium", 34.0739, -118.2400),
            Landmark("LACMA", 34.0639, -118.3592), Landmark("Griffith Park", 34.1366, -118.2942),
            Landmark("The Broad", 34.0537, -118.2504), Landmark("Staples Center", 34.0430, -118.2653),
            Landmark("Echo Park", 34.0786, -118.2600), Landmark("Malibu Pier", 34.0259, -118.7798),
        )),
        City("Tokyo", listOf(
            Landmark("Tokyo Tower", 35.6586, 139.7454), Landmark("Senso-ji Temple", 35.7148, 139.7967),
            Landmark("Shibuya Crossing", 35.6595, 139.7005), Landmark("Meiji Shrine", 35.6764, 139.6993),
            Landmark("Tokyo Skytree", 35.7101, 139.8107), Landmark("Ueno Park", 35.7156, 139.7745),
            Landmark("Akihabara", 35.6989, 139.7731), Landmark("Imperial Palace", 35.6852, 139.7528),
            Landmark("Shinjuku Gyoen", 35.6852, 139.7100), Landmark("Tsukiji Market", 35.6654, 139.7707),
            Landmark("Roppongi Hills", 35.6604, 139.7290), Landmark("Harajuku", 35.6700, 139.7050),
        )),
    )

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun generateFlights(count: Int): List<Flight> {
        val flights = mutableListOf<Flight>()
        val seen = mutableSetOf<String>()
        var attempts = 0
        while (flights.size < count && attempts < 100) {
            attempts++
            val city = cities.random()
            if (city.landmarks.size < 2) continue
            val origin = city.landmarks.random()
            val destination = city.landmarks.filter { it.name != origin.name }.random()
            val key = origin.name + "->" + destination.name
            if (key in seen) continue
            seen.add(key)
            val km = haversineKm(origin.lat, origin.lon, destination.lat, destination.lon)
            val estimate = (km / 70.0 * 3600).toInt() // pigeon speed 70 km/h
            if (estimate < 30 * 60 || estimate > 5 * 3600) continue
            val actual = (estimate * (0.85 + Random.nextDouble() * 0.30)).toInt()
            flights.add(Flight(origin, destination, km, estimate, actual))
        }
        return flights.sortedBy { it.estimatedSeconds }
    }
}
