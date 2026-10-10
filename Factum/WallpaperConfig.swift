//
//  WallpaperConfig.swift
//  Pigeon
//
//  Data model describing a wallpaper selection for home feed or groups.
//

import SwiftUI

// MARK: - Wallpaper Type

enum WallpaperType: String, Codable, CaseIterable, Sendable {
    case `default`       // PigeonTheme.background (warm parchment)
    case gradient        // Built-in gradient preset
    case pattern         // Built-in pattern preset
    case solidColor      // Solid color pick
    case photo           // User's camera roll photo (local or uploaded)
}

// MARK: - Wallpaper Config

/// Describes a wallpaper selection. Codable for @AppStorage (home) and Supabase JSON (groups).
struct WallpaperConfig: Codable, Equatable, Sendable {
    var type: WallpaperType = .default
    var presetID: String? = nil        // ID for built-in gradient/pattern
    var solidColorHex: String? = nil   // Hex color string for solid color
    var imageURL: String? = nil        // Local file URL (home) or Supabase URL (group)
    var blur: Double = 0.0             // Blur amount (0-20)
    var darkenOverlay: Double = 0.0    // Dark overlay for readability (0-0.5)
    
    static let defaultConfig = WallpaperConfig()
    
    var isCustom: Bool {
        type != .default
    }
    
    // MARK: JSON Serialization
    
    var jsonString: String? {
        guard let data = try? JSONEncoder().encode(self) else { return nil }
        return String(data: data, encoding: .utf8)
    }
    
    static func from(json: String) -> WallpaperConfig? {
        guard let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(WallpaperConfig.self, from: data)
    }
}

// Color hex helpers are defined in StudySubject.swift (Color.init?(hex:) and Color.hexString)
