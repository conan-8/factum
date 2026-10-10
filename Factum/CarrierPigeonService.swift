//
//  CarrierPigeonService.swift
//  Pigeon
//
//  Curated city landmarks and carrier pigeon flight generation
//  for the Carrier Pigeon study timer mode.
//

import Foundation
import CoreLocation

// MARK: - City Landmark

struct CityLandmark: Identifiable, Sendable {
    let id: String
    let name: String
    let latitude: Double
    let longitude: Double
    
    var location: CLLocation {
        CLLocation(latitude: latitude, longitude: longitude)
    }
}

// MARK: - Pigeon Flight

struct PigeonFlight: Identifiable, Sendable {
    let id = UUID()
    let origin: CityLandmark
    let destination: CityLandmark
    let distanceKm: Double
    let estimatedSeconds: Int   // base estimate shown to user ("est ~2h 30m")
    let actualSeconds: Int      // real countdown with variance (could be ±15%)
    
    var estimatedTimeFormatted: String {
        let hours = estimatedSeconds / 3600
        let minutes = (estimatedSeconds % 3600) / 60
        if hours > 0 {
            return minutes > 0 ? "est ~\(hours)h \(minutes)m" : "est ~\(hours)h"
        }
        return "est ~\(minutes)m"
    }
    
    var routeLabel: String {
        "\(origin.name) → \(destination.name)"
    }
    
    var distanceFormatted: String {
        if distanceKm >= 100 {
            return "\(Int(distanceKm)) km"
        }
        return String(format: "%.1f km", distanceKm)
    }
}

// MARK: - Carrier Pigeon Service

enum CarrierPigeonService {
    
    /// Average carrier pigeon cruising speed in km/h.
    static let pigeonSpeedKmh: Double = 70.0
    
    /// Minimum flight duration: 30 minutes.
    static let minFlightSeconds: Int = 30 * 60
    
    /// Maximum flight duration: 5 hours.
    static let maxFlightSeconds: Int = 5 * 3600
    
    /// Variance range for actual flight time relative to estimate.
    /// e.g. 0.85...1.15 means ±15% of the base time.
    static let varianceRange: ClosedRange<Double> = 0.85...1.15
    
    // MARK: - Available Cities
    
    static let availableCities: [String] = curatedCities.keys.sorted()
    
    // MARK: - Flight Generation
    
    /// Generate random pigeon flights for a city.
    /// - Parameters:
    ///   - city: The city name (must match a key in `curatedCities`).
    ///   - count: Number of flights to generate (default 5).
    /// - Returns: Array of `PigeonFlight` sorted by estimated time (shortest first).
    static func generateFlights(for city: String, count: Int = 5) -> [PigeonFlight] {
        guard let landmarks = curatedCities[city], landmarks.count >= 2 else {
            return []
        }
        
        var flights: [PigeonFlight] = []
        var attempts = 0
        let maxAttempts = 80
        
        while flights.count < count && attempts < maxAttempts {
            attempts += 1
            let shuffled = landmarks.shuffled()
            guard shuffled.count >= 2 else { continue }
            let origin = shuffled[0]
            let destination = shuffled[1]
            guard origin.id != destination.id else { continue }
            
            // Straight-line distance (pigeons fly, not drive)
            let distanceMeters = origin.location.distance(from: destination.location)
            let distanceKm = distanceMeters / 1000.0
            
            // Base time at pigeon speed
            let timeHours = distanceKm / pigeonSpeedKmh
            let baseSeconds = Int(timeHours * 3600)
            
            // Apply variance for actual flight time
            let variance = Double.random(in: varianceRange)
            let actualSeconds = Int(Double(baseSeconds) * variance)
            
            // Filter to valid range
            guard actualSeconds >= minFlightSeconds && actualSeconds <= maxFlightSeconds else {
                continue
            }
            
            // Avoid duplicate routes (same pair in either direction)
            let isDuplicate = flights.contains {
                ($0.origin.id == origin.id && $0.destination.id == destination.id) ||
                ($0.origin.id == destination.id && $0.destination.id == origin.id)
            }
            guard !isDuplicate else { continue }
            
            flights.append(PigeonFlight(
                origin: origin,
                destination: destination,
                distanceKm: distanceKm,
                estimatedSeconds: baseSeconds,
                actualSeconds: actualSeconds
            ))
        }
        
        return flights.sorted { $0.estimatedSeconds < $1.estimatedSeconds }
    }
    
    // MARK: - Curated City Data
    
    static let curatedCities: [String: [CityLandmark]] = [
        
        // ─── Toronto ───────────────────────────────────────────────
        "Toronto": [
            CityLandmark(id: "union-station", name: "Union Station", latitude: 43.6453, longitude: -79.3806),
            CityLandmark(id: "sheppard-yonge", name: "Sheppard-Yonge", latitude: 43.7617, longitude: -79.4111),
            CityLandmark(id: "bloor-yonge", name: "Bloor-Yonge", latitude: 43.6709, longitude: -79.3857),
            CityLandmark(id: "finch-station", name: "Finch Station", latitude: 43.7808, longitude: -79.4148),
            CityLandmark(id: "kipling-station", name: "Kipling Station", latitude: 43.6373, longitude: -79.5363),
            CityLandmark(id: "kennedy-station", name: "Kennedy Station", latitude: 43.7325, longitude: -79.2638),
            CityLandmark(id: "scarborough-town", name: "Scarborough Town Centre", latitude: 43.7735, longitude: -79.2577),
            CityLandmark(id: "yorkdale", name: "Yorkdale Mall", latitude: 43.7254, longitude: -79.4528),
            CityLandmark(id: "cn-tower", name: "CN Tower", latitude: 43.6426, longitude: -79.3871),
            CityLandmark(id: "robarts-library", name: "Robarts Library", latitude: 43.6644, longitude: -79.3997),
            CityLandmark(id: "rom", name: "Royal Ontario Museum", latitude: 43.6677, longitude: -79.3948),
            CityLandmark(id: "high-park", name: "High Park", latitude: 43.6465, longitude: -79.4637),
            CityLandmark(id: "pearson-airport", name: "Pearson Airport", latitude: 43.6777, longitude: -79.6248),
            CityLandmark(id: "square-one", name: "Square One", latitude: 43.5931, longitude: -79.6443),
            CityLandmark(id: "vaughan-metro", name: "Vaughan Metropolitan Centre", latitude: 43.7943, longitude: -79.5273),
            CityLandmark(id: "exhibition", name: "Exhibition Place", latitude: 43.6353, longitude: -79.4178),
            CityLandmark(id: "distillery", name: "Distillery District", latitude: 43.6503, longitude: -79.3596),
            CityLandmark(id: "casa-loma", name: "Casa Loma", latitude: 43.6780, longitude: -79.4094),
            CityLandmark(id: "utm", name: "UTM Campus", latitude: 43.5489, longitude: -79.6625),
            CityLandmark(id: "utsc", name: "UTSC Campus", latitude: 43.7838, longitude: -79.1874),
            CityLandmark(id: "york-university", name: "York University", latitude: 43.7735, longitude: -79.5019),
            CityLandmark(id: "mcmaster", name: "McMaster University", latitude: 43.2609, longitude: -79.9192),
            CityLandmark(id: "niagara-falls", name: "Niagara Falls", latitude: 43.0896, longitude: -79.0849),
            CityLandmark(id: "oshawa-centre", name: "Oshawa Centre", latitude: 43.8975, longitude: -78.8580),
            CityLandmark(id: "brampton-civic", name: "Brampton Civic Centre", latitude: 43.6878, longitude: -79.7605),
        ],
        
        // ─── New York ──────────────────────────────────────────────
        "New York": [
            CityLandmark(id: "grand-central", name: "Grand Central Terminal", latitude: 40.7527, longitude: -73.9772),
            CityLandmark(id: "penn-station", name: "Penn Station", latitude: 40.7506, longitude: -73.9935),
            CityLandmark(id: "times-square", name: "Times Square", latitude: 40.7580, longitude: -73.9855),
            CityLandmark(id: "central-park", name: "Central Park", latitude: 40.7829, longitude: -73.9654),
            CityLandmark(id: "brooklyn-bridge", name: "Brooklyn Bridge", latitude: 40.7061, longitude: -73.9969),
            CityLandmark(id: "columbia", name: "Columbia University", latitude: 40.8075, longitude: -73.9626),
            CityLandmark(id: "nyu", name: "NYU", latitude: 40.7295, longitude: -73.9965),
            CityLandmark(id: "jfk", name: "JFK Airport", latitude: 40.6413, longitude: -73.7781),
            CityLandmark(id: "laguardia", name: "LaGuardia Airport", latitude: 40.7769, longitude: -73.8740),
            CityLandmark(id: "coney-island", name: "Coney Island", latitude: 40.5749, longitude: -73.9859),
            CityLandmark(id: "statue-liberty", name: "Statue of Liberty", latitude: 40.6892, longitude: -74.0445),
            CityLandmark(id: "yankee-stadium", name: "Yankee Stadium", latitude: 40.8296, longitude: -73.9262),
            CityLandmark(id: "met-museum", name: "The Met", latitude: 40.7794, longitude: -73.9632),
            CityLandmark(id: "wall-street", name: "Wall Street", latitude: 40.7074, longitude: -74.0113),
            CityLandmark(id: "prospect-park", name: "Prospect Park", latitude: 40.6602, longitude: -73.9690),
            CityLandmark(id: "flushing", name: "Flushing", latitude: 40.7596, longitude: -73.8317),
            CityLandmark(id: "staten-island-ferry", name: "Staten Island Ferry", latitude: 40.6437, longitude: -74.0738),
            CityLandmark(id: "harlem", name: "Harlem", latitude: 40.8116, longitude: -73.9465),
            CityLandmark(id: "hudson-yards", name: "Hudson Yards", latitude: 40.7538, longitude: -74.0011),
            CityLandmark(id: "barclays-center", name: "Barclays Center", latitude: 40.6826, longitude: -73.9754),
            CityLandmark(id: "newark-airport", name: "Newark Airport", latitude: 40.6895, longitude: -74.1745),
            CityLandmark(id: "nypl", name: "NY Public Library", latitude: 40.7532, longitude: -73.9822),
            CityLandmark(id: "fordham", name: "Fordham University", latitude: 40.8615, longitude: -73.8854),
            CityLandmark(id: "whitestone", name: "Whitestone", latitude: 40.7946, longitude: -73.8094),
        ],
        
        // ─── London ────────────────────────────────────────────────
        "London": [
            CityLandmark(id: "kings-cross", name: "King's Cross", latitude: 51.5309, longitude: -0.1233),
            CityLandmark(id: "paddington", name: "Paddington", latitude: 51.5154, longitude: -0.1755),
            CityLandmark(id: "waterloo", name: "Waterloo Station", latitude: 51.5031, longitude: -0.1132),
            CityLandmark(id: "big-ben", name: "Big Ben", latitude: 51.5007, longitude: -0.1246),
            CityLandmark(id: "british-museum", name: "British Museum", latitude: 51.5194, longitude: -0.1270),
            CityLandmark(id: "tower-of-london", name: "Tower of London", latitude: 51.5081, longitude: -0.0759),
            CityLandmark(id: "greenwich", name: "Greenwich Observatory", latitude: 51.4769, longitude: -0.0005),
            CityLandmark(id: "canary-wharf", name: "Canary Wharf", latitude: 51.5054, longitude: -0.0235),
            CityLandmark(id: "camden-town", name: "Camden Town", latitude: 51.5392, longitude: -0.1426),
            CityLandmark(id: "wembley", name: "Wembley Stadium", latitude: 51.5560, longitude: -0.2796),
            CityLandmark(id: "heathrow", name: "Heathrow Airport", latitude: 51.4700, longitude: -0.4543),
            CityLandmark(id: "stratford", name: "Stratford", latitude: 51.5415, longitude: -0.0034),
            CityLandmark(id: "imperial-college", name: "Imperial College", latitude: 51.4988, longitude: -0.1749),
            CityLandmark(id: "ucl", name: "UCL", latitude: 51.5246, longitude: -0.1340),
            CityLandmark(id: "buckingham-palace", name: "Buckingham Palace", latitude: 51.5014, longitude: -0.1419),
            CityLandmark(id: "london-eye", name: "London Eye", latitude: 51.5033, longitude: -0.1196),
            CityLandmark(id: "hyde-park", name: "Hyde Park", latitude: 51.5073, longitude: -0.1657),
            CityLandmark(id: "brixton", name: "Brixton", latitude: 51.4613, longitude: -0.1145),
            CityLandmark(id: "wimbledon", name: "Wimbledon", latitude: 51.4340, longitude: -0.2141),
            CityLandmark(id: "richmond-park", name: "Richmond Park", latitude: 51.4430, longitude: -0.2747),
            CityLandmark(id: "crystal-palace", name: "Crystal Palace", latitude: 51.4178, longitude: -0.0731),
            CityLandmark(id: "gatwick", name: "Gatwick Airport", latitude: 51.1537, longitude: -0.1821),
            CityLandmark(id: "oxford", name: "Oxford", latitude: 51.7520, longitude: -1.2577),
            CityLandmark(id: "cambridge", name: "Cambridge", latitude: 52.2053, longitude: 0.1218),
            CityLandmark(id: "brighton", name: "Brighton", latitude: 50.8225, longitude: -0.1372),
        ],
        
        // ─── Los Angeles ───────────────────────────────────────────
        "Los Angeles": [
            CityLandmark(id: "lax", name: "LAX Airport", latitude: 33.9425, longitude: -118.4081),
            CityLandmark(id: "ucla", name: "UCLA", latitude: 34.0689, longitude: -118.4452),
            CityLandmark(id: "usc", name: "USC", latitude: 34.0224, longitude: -118.2851),
            CityLandmark(id: "santa-monica-pier", name: "Santa Monica Pier", latitude: 34.0090, longitude: -118.4973),
            CityLandmark(id: "hollywood-sign", name: "Hollywood Sign", latitude: 34.1341, longitude: -118.3215),
            CityLandmark(id: "la-union-station", name: "Union Station", latitude: 34.0561, longitude: -118.2365),
            CityLandmark(id: "griffith-observatory", name: "Griffith Observatory", latitude: 34.1184, longitude: -118.3004),
            CityLandmark(id: "long-beach", name: "Long Beach", latitude: 33.7701, longitude: -118.1937),
            CityLandmark(id: "pasadena", name: "Pasadena City Hall", latitude: 34.1478, longitude: -118.1445),
            CityLandmark(id: "venice-beach", name: "Venice Beach", latitude: 33.9850, longitude: -118.4695),
            CityLandmark(id: "dodger-stadium", name: "Dodger Stadium", latitude: 34.0739, longitude: -118.2400),
            CityLandmark(id: "sofi-stadium", name: "SoFi Stadium", latitude: 33.9535, longitude: -118.3392),
            CityLandmark(id: "getty-center", name: "Getty Center", latitude: 34.0781, longitude: -118.4741),
            CityLandmark(id: "disneyland", name: "Disneyland", latitude: 33.8121, longitude: -117.9190),
            CityLandmark(id: "beverly-hills", name: "Beverly Hills", latitude: 34.0736, longitude: -118.4004),
            CityLandmark(id: "malibu", name: "Malibu", latitude: 34.0259, longitude: -118.7798),
            CityLandmark(id: "burbank-airport", name: "Burbank Airport", latitude: 34.1975, longitude: -118.3585),
            CityLandmark(id: "caltech", name: "Caltech", latitude: 34.1377, longitude: -118.1253),
            CityLandmark(id: "huntington-beach", name: "Huntington Beach", latitude: 33.6595, longitude: -117.9988),
            CityLandmark(id: "pomona", name: "Cal Poly Pomona", latitude: 34.0565, longitude: -117.8215),
            CityLandmark(id: "santa-barbara", name: "Santa Barbara", latitude: 34.4208, longitude: -119.6982),
            CityLandmark(id: "san-diego", name: "San Diego", latitude: 32.7157, longitude: -117.1611),
            CityLandmark(id: "palm-springs", name: "Palm Springs", latitude: 33.8303, longitude: -116.5453),
            CityLandmark(id: "torrance", name: "Torrance", latitude: 33.8358, longitude: -118.3406),
        ],
        
        // ─── Tokyo ─────────────────────────────────────────────────
        "Tokyo": [
            CityLandmark(id: "tokyo-station", name: "Tokyo Station", latitude: 35.6812, longitude: 139.7671),
            CityLandmark(id: "shibuya", name: "Shibuya Crossing", latitude: 35.6595, longitude: 139.7004),
            CityLandmark(id: "shinjuku", name: "Shinjuku Station", latitude: 35.6896, longitude: 139.7006),
            CityLandmark(id: "akihabara", name: "Akihabara", latitude: 35.7023, longitude: 139.7745),
            CityLandmark(id: "asakusa", name: "Asakusa", latitude: 35.7148, longitude: 139.7967),
            CityLandmark(id: "ueno", name: "Ueno Park", latitude: 35.7146, longitude: 139.7714),
            CityLandmark(id: "todai", name: "University of Tokyo", latitude: 35.7126, longitude: 139.7620),
            CityLandmark(id: "roppongi", name: "Roppongi Hills", latitude: 35.6605, longitude: 139.7292),
            CityLandmark(id: "ginza", name: "Ginza", latitude: 35.6717, longitude: 139.7649),
            CityLandmark(id: "odaiba", name: "Odaiba", latitude: 35.6267, longitude: 139.7769),
            CityLandmark(id: "narita", name: "Narita Airport", latitude: 35.7720, longitude: 140.3929),
            CityLandmark(id: "haneda", name: "Haneda Airport", latitude: 35.5494, longitude: 139.7798),
            CityLandmark(id: "meiji-shrine", name: "Meiji Shrine", latitude: 35.6764, longitude: 139.6993),
            CityLandmark(id: "ikebukuro", name: "Ikebukuro", latitude: 35.7295, longitude: 139.7109),
            CityLandmark(id: "skytree", name: "Tokyo Skytree", latitude: 35.7101, longitude: 139.8107),
            CityLandmark(id: "tokyo-tower", name: "Tokyo Tower", latitude: 35.6586, longitude: 139.7454),
            CityLandmark(id: "tsukiji", name: "Tsukiji Market", latitude: 35.6654, longitude: 139.7707),
            CityLandmark(id: "yokohama", name: "Yokohama Station", latitude: 35.4664, longitude: 139.6226),
            CityLandmark(id: "kawasaki", name: "Kawasaki", latitude: 35.5309, longitude: 139.7030),
            CityLandmark(id: "machida", name: "Machida", latitude: 35.5429, longitude: 139.4459),
            CityLandmark(id: "chiba", name: "Chiba", latitude: 35.6074, longitude: 140.1065),
            CityLandmark(id: "mt-takao", name: "Mount Takao", latitude: 35.6251, longitude: 139.2437),
            CityLandmark(id: "kamakura", name: "Kamakura", latitude: 35.3192, longitude: 139.5467),
            CityLandmark(id: "hakone", name: "Hakone", latitude: 35.2326, longitude: 139.1070),
        ],
    ]
}
